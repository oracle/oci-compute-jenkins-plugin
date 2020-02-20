package com.oracle.cloud.baremetal.jenkins;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.anyOf;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.instanceOf;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.net.InetAddress;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.oracle.bmc.core.model.Instance;
import com.oracle.cloud.baremetal.jenkins.client.BaremetalCloudClient;
import com.oracle.cloud.baremetal.jenkins.client.BaremetalCloudClientFactory;
import com.oracle.cloud.baremetal.jenkins.client.SDKBaremetalCloudClientFactory;
import com.oracle.cloud.baremetal.jenkins.credentials.BaremetalCloudCredentials;
import com.oracle.cloud.baremetal.jenkins.retry.LinearRetry;
import com.oracle.cloud.baremetal.jenkins.retry.Retry;
import com.oracle.cloud.baremetal.jenkins.ssh.SshConnector;
import com.trilead.ssh2.Connection;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.ComputerSet;
import hudson.model.Descriptor;
import hudson.model.Failure;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Descriptor.FormException;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.slaves.AbstractCloudImpl;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner.PlannedNode;
import hudson.util.FormValidation;
import hudson.util.HttpResponses;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

import static java.lang.Math.toIntExact;
import org.kohsuke.stapler.AncestorInPath;


public class BaremetalCloud extends AbstractCloudImpl{

    private static final Logger LOGGER = Logger.getLogger(BaremetalCloud.class.getName());

    /**
     * The prefix to add to names provided by the user in the UI to ensure that
     * names of clouds in different plugins do not conflict.  We use the term
     * "name" to mean the full name with a prefix (to match Cloud.name), and we
     * use the term "cloud name" to be the short name without a prefix.
     *
     * @see #cloudNameToName
     * @see #nameToCloudName
     * @see #getCloudName
     * @see DescriptorImpl#doCheckCloudName
     */
    public static final String NAME_PREFIX = "oci-compute-";

    /** Time to sleep while polling if an orchestration has started. */
    private static final long START_POLL_SLEEP_MILLIS = TimeUnit.SECONDS.toMillis(5);

    static String cloudNameToName(String cloudName) {
        return NAME_PREFIX + cloudName.trim();
    }

    static String nameToCloudName(String name) {
        return name.substring(NAME_PREFIX.length());
    }

    /**
     * The prefix to add to the names of created instances.
     */
    public static final String INSTANCE_NAME_PREFIX = "jenkins-";

    /** The prefix with Jenkins Master IP to add to the names of created instances. */
    private static final String JENKINS_IP = getJenkinsIp();

    private final String credentialsId;
    private final String maxAsyncThreads;
    private final int nextTemplateId;
    private final List<? extends BaremetalCloudAgentTemplate> templates;

    @DataBoundConstructor
    public BaremetalCloud(
            String cloudName,
            String credentialsId,
            String instanceCapStr,
            String maxAsyncThreads,
            int nextTemplateId,
            List<? extends BaremetalCloudAgentTemplate> templates) {
        super(cloudNameToName(cloudName), instanceCapStr);

        this.credentialsId = credentialsId;
        this.maxAsyncThreads = maxAsyncThreads;
        this.nextTemplateId = nextTemplateId;
        if (templates == null) {
            this.templates = Collections.emptyList();
        } else {
            this.templates = templates;
        }
    }

    @Override
    public String getDisplayName() {
        return getCloudName();
    }

    public String getCloudName() {
    	return nameToCloudName(name);
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public String getMaxAsyncThreads() {
        return maxAsyncThreads;
    }
    public int getNextTemplateId() {
        return nextTemplateId;
    }

    public List<? extends BaremetalCloudAgentTemplate> getTemplates() {
        return templates;
    }

    ExecutorService getThreadPoolForRemoting() {
        return Computer.threadPoolForRemoting;
    }

    private String fmtLogMsg(String msg) {
        return "OCI cloud \"" + getCloudName() + "\": " + msg;
    }

    private static String getJenkinsIp() {
        String IP = "";
        try {
            IP = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e){}
        return IP;
    }

    @Override
    public synchronized Collection<PlannedNode> provision(Label label, int excessWorkload) {
        final BaremetalCloudAgentTemplate template = getTemplate(label);

        if (template == null) {
            return Collections.emptyList();
        }

        LOGGER.info(fmtLogMsg("requested Agent provision excessWorkload: " + excessWorkload));
        List<PlannedNode> plannedNodes = new ArrayList<>();

        boolean templateInstanceCapInvalid = false;

        if (template.getInstanceCap().isEmpty()) {
            templateInstanceCapInvalid = true;
        } else if (Integer.parseInt(template.getInstanceCap()) >= getInstanceCap()) {
            templateInstanceCapInvalid = true;
        }

        while (excessWorkload > 0
                && (plannedNodes.size() + getNodeCount() < getInstanceCap()
                    && (templateInstanceCapInvalid || plannedNodes.size() + getTemplateNodeCount(template.getTemplateId()) < Integer.parseInt(template.getInstanceCap())))) {
            Provisioner provisioner = new Provisioner(template);
            String displayName = provisioner.getPlannedNodeDisplayName();
            Future<Node> future = getThreadPoolForRemoting().submit(provisioner);

            int numExecutors = provisioner.numExecutors;
            plannedNodes.add(new BaremetalCloudPlannedNode(displayName, future, numExecutors, this.name, template.getTemplateId()));
            excessWorkload -= numExecutors;
        }

        LOGGER.info(fmtLogMsg(plannedNodes.size() + " FutureNodes to be added to provisioning queue"));
        return plannedNodes;
    }

    private class Provisioner implements Callable<Node> {
        final BaremetalCloudAgentTemplate template;
        final int numExecutors;
        final String name;
        final String instanceName;

        Provisioner(BaremetalCloudAgentTemplate template) {
            this.template = template;
            this.numExecutors = template.getNumExecutorsValue();

            UUID uuid = UUID.randomUUID();
            this.name = BaremetalCloud.NAME_PREFIX + uuid;
            this.instanceName = INSTANCE_NAME_PREFIX + JENKINS_IP + "-" + uuid;
        }

        public String getPlannedNodeDisplayName() {
            return instanceName;
        }

        @Override
        public Node call() throws Exception {
            return provision(name, template, instanceName);
        }
    }

    private BaremetalCloudAgent provision(String name, BaremetalCloudAgentTemplate template, String instanceName) throws Exception {
        LOGGER.info("Provisioning new cloud infrastructure instance");
        try {
            BaremetalCloudClient client = getClient();
            Instance instance = client.createInstance(instanceName, template);
            String Ip = "";

            TimeoutHelper timeoutHelper = new TimeoutHelper(getClock(), template.getStartTimeoutNanos(), START_POLL_SLEEP_MILLIS);
            try{
                client.waitForInstanceProvisioningToComplete(instance.getId());
                Ip = client.getInstanceIp(template, instance.getId());
                LOGGER.info("Provisioned instance " + instanceName + " with ip " + Ip);
                awaitInstanceSshAvailable(Ip, template.getSshConnectTimeoutMillis(), timeoutHelper);
                template.resetFailureCount();
            } catch(IOException | RuntimeException ex){
                try{
                    recycleCloudResources(instance.getId());
                    LOGGER.log(Level.WARNING, "Provision node: " + instanceName + " failed, and created resources have been recycled.", ex);
                } catch(IOException | RuntimeException e2) {
                    LOGGER.log(Level.WARNING, "Provision node: " + instanceName + " failed, and failed to recycle node " + instanceName, ex);
                }
                throw ex;
            }
            return newBaremetalCloudAgent(name, template, this.name, instance.getId(), Ip);
        } catch (IOException | RuntimeException e) {
            String message = e.getMessage();
            template.increaseFailureCount(message != null ? message : e.toString());
            throw e;
        }
    }

    /**
     * Used to distinguish BaremetalCloud PlannedNode scheduled in queue for provisioning
     */
    private class BaremetalCloudPlannedNode extends PlannedNode {

        private final String cloudName;
        private final int templateId;

        public BaremetalCloudPlannedNode(String displayName, Future<Node> future, int numExecutors, String cloudName, int templateId) {
            super(displayName, future, numExecutors);
            this.cloudName = cloudName;
            this.templateId = templateId;
        }

        public String getCloudName() {
            return cloudName;
        }
        
        public int getTemplateId() {
            return templateId;
        }
    }

    public synchronized void recycleCloudResources(String instanceId) throws IOException {
        BaremetalCloudClient client = getClient();
        Retry<String> retry = getTerminationRetry(() -> client.terminateInstance(instanceId));
        try{
            retry.run();
            client.waitForInstanceTerminationToComplete(instanceId);
        }catch(Exception e){
            throw new IOException(e);
        }
    }

    public Retry<String> getTerminationRetry(Callable<String> task) {
        return new LinearRetry<String>(task);
    }

    Clock getClock() {
        return Clock.INSTANCE;
    }

    BaremetalCloudAgent newBaremetalCloudAgent(
            final String name,
            final BaremetalCloudAgentTemplate template,
            final String cloudName,
            final String instanceId,
            final String host) throws IOException, FormException {
        return new BaremetalCloudAgent(name, template, cloudName, instanceId, host);
    }

    public BaremetalCloudAgentTemplate getTemplate(Label label) {
        for (BaremetalCloudAgentTemplate t : templates) {
            if (t.getDisableCause() != null) {
                continue;
            }
            if (t.getMode() == Node.Mode.NORMAL) {
                if (label == null || label.matches(t.getLabelAtoms())) {
                    return t;
                }
            } else if (t.getMode() == Node.Mode.EXCLUSIVE) {
                if (label != null && label.matches(t.getLabelAtoms())) {
                    return t;
                }
            }
        }
        return null;
    }

    static final String PROVISION_ATTR_AGENT_NAME = BaremetalCloud.class.getName() + ".name";
    static final String PROVISION_ATTR_NUM_EXECUTORS = BaremetalCloud.class.getName() + ".numExecutors";

    /**
     * Called by {@code computerSet.jelly} when explicitly provisioning a new
     * node via the nodes page.
     *
     * @param templateId template id
     * @param req request
     * @param rsp response
     *
     * @throws ServletException if a servlet exception occurs
     * @throws IOException if a IO error occurs
     */
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public void doProvision(
            @QueryParameter int templateId,
            StaplerRequest req,
            StaplerResponse rsp) throws ServletException, IOException {
        checkPermission(PROVISION);

        BaremetalCloudAgentTemplate template = getTemplateById(templateId);
        if (template == null) {
            sendError(Messages.BaremetalCloud_provision_templateNotFound(), req, rsp);
            return;
        }
        if (template.getDisableCause() != null) {
            sendError(Messages.BaremetalCloud_provision_templateDisabled(), req, rsp);
            return;
        }

        // Note that this will directly add a new node without involving
        // NodeProvisioner, so that class will not be aware that a node is being
        // provisioned until ExplicitProvisioner adds it.
        ExplicitProvisioner provisioner = new ExplicitProvisioner(template);
        getThreadPoolForRemoting().submit(provisioner);

        req.setAttribute(PROVISION_ATTR_AGENT_NAME, provisioner.name);
        req.setAttribute(PROVISION_ATTR_NUM_EXECUTORS, provisioner.numExecutors);
        req.getView(this, "provision").forward(req, rsp);
    }

    void addNode(Node node) throws IOException {
        JenkinsUtil.getJenkinsInstance().addNode(node);
    }

    private class ExplicitProvisioner extends Provisioner {
        ExplicitProvisioner(BaremetalCloudAgentTemplate template) {
            super(template);
        }

        @Override
        public Node call() throws Exception {
            // Simulate NodeProvisioner.update.
            String displayName = getPlannedNodeDisplayName();
            try {
                addNode(super.call());
                LOGGER.log(Level.INFO, "{0} provisioning successfully completed", displayName);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Provisioned slave " + displayName + " failed!", e);
            }

            // doProvision does not use the Future.
            return null;
        }
    }

    private BaremetalCloudAgentTemplate getTemplateById(int templateId) {
        for (BaremetalCloudAgentTemplate t : templates) {
            if (t.getTemplateId() == templateId) {
                return t;
            }
        }
        return null;
    }

    /**
     * Called by {@code provision.jelly} to show a sidepanel.
     *
     * @return provision side panel class
     */
    public Class<?> getProvisionSidePanelClass() {
        return ComputerSet.class;
    }

    /**
     * Display a provisioning message based on request attributes set by
     * {@link #doProvision}.
     *
     * @param req request
     *
     * @return provision started message
     */
    public String getProvisionStartedMessage(HttpServletRequest req) {
        String name = (String)req.getAttribute(PROVISION_ATTR_AGENT_NAME);
        Integer numExecutors = (Integer)req.getAttribute(PROVISION_ATTR_NUM_EXECUTORS);
        return Messages.BaremetalCloud_provision_started(name, numExecutors);
    }

    /**
     * The breadcrumb on the {@code provision.jelly} page contains a link to
     * this object.  We have no data to display, so redirect the user to the
     * computer set page.
     *
     * @return the http response
     *
     * @throws IOException if an IO error occurs
     */
    public HttpResponse doIndex() throws IOException {
        return HttpResponses.redirectTo("../../computer/");
    }

    public BaremetalCloudClient getClient(){
        BaremetalCloudClientFactory factory = SDKBaremetalCloudClientFactory.INSTANCE;
        return factory.createClient(credentialsId, Integer.parseInt(maxAsyncThreads));
    }

    private synchronized int getNodeCount() {
        long count = 0;
        Jenkins jenkins = JenkinsUtil.getJenkinsInstance();

        // Provisioned nodes
        count += jenkins.getNodes()
            .stream()
            .filter(n -> n instanceof BaremetalCloudAgent)
            .filter(n -> ((BaremetalCloudAgent) n).cloudName.equals(name))
            .peek(n -> LOGGER.fine(fmtLogMsg("Peeking provisioned nodes: " + n.getNodeName())))
            .count();

        // Nodes waiting in provisioning queue
        count += Stream.concat(
                // Nodes provisioning for no label
                jenkins.unlabeledNodeProvisioner.getPendingLaunches().stream(),
                // Nodes provisioning for specific label
                jenkins.getLabels().stream().flatMap(l -> l.nodeProvisioner.getPendingLaunches().stream())
        )
            .filter(pn -> pn instanceof BaremetalCloudPlannedNode)
            .filter(pn -> ((BaremetalCloudPlannedNode) pn).getCloudName().equals(name))
            .peek(pn -> LOGGER.fine(fmtLogMsg("Peeking provisioning nodes: " + ((BaremetalCloudPlannedNode) pn).displayName)))
            .count();

        LOGGER.info(fmtLogMsg("Found " + count + " provisioned or provisioning Nodes"));

        return toIntExact(count);
    }

    private synchronized int getTemplateNodeCount(int templateId) {
        long count = 0;
        Jenkins jenkins = JenkinsUtil.getJenkinsInstance();

        // Provisioned nodes
        count += jenkins.getNodes()
            .stream()
            .filter(n -> n instanceof BaremetalCloudAgent)
            .filter(n -> ((BaremetalCloudAgent) n).cloudName.equals(name))
            .filter(n -> ((BaremetalCloudAgent) n).templateId == templateId)
            .peek(n -> LOGGER.fine(fmtLogMsg("Peeking provisioned nodes: " + n.getNodeName())))
            .count();

        // Nodes waiting in provisioning queue
        count += Stream.concat(
                // Nodes provisioning for no label
                jenkins.unlabeledNodeProvisioner.getPendingLaunches().stream(),
                // Nodes provisioning for specific label
                jenkins.getLabels().stream().flatMap(l -> l.nodeProvisioner.getPendingLaunches().stream())
        )
            .filter(pn -> pn instanceof BaremetalCloudPlannedNode)
            .filter(pn -> ((BaremetalCloudPlannedNode) pn).getCloudName().equals(name))
            .filter(pn -> ((BaremetalCloudPlannedNode) pn).getTemplateId() == templateId)
            .peek(pn -> LOGGER.fine(fmtLogMsg("Peeking provisioning nodes: " + ((BaremetalCloudPlannedNode) pn).displayName)))
            .count();

        LOGGER.info(fmtLogMsg("Found " + count + " provisioned or provisioning Nodes for the template " + templateId));

        return toIntExact(count);
    }
    // make sure the instance if available before launch agent on it.
    private void awaitInstanceSshAvailable(String host, int connectTimeoutMillis, TimeoutHelper timeoutHelper) throws IOException, InterruptedException {
        do {
            Connection connnection = SshConnector.createConnection(host, 22);
            try {
                SshConnector.connect(connnection, connectTimeoutMillis);
                return;
            } catch (IOException e) {
                LOGGER.log(Level.FINER, "Ignoring exception connecting to SSH during privision", e);
            } finally {
                connnection.close();
            }
        } while (timeoutHelper.sleep());

        throw new IOException("Timed out connecting to SSH");
    }

    @Override
    public boolean canProvision(Label label) {
        return getTemplate(label) != null;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Cloud> {

        @Override
        public String getDisplayName() {
            return "Oracle Cloud Infrastructure Compute";
        }

        List<? extends Cloud> getClouds() {
            return JenkinsUtil.getJenkinsInstance().clouds;
        }

        public FormValidation doCheckCloudName(@QueryParameter String value) {
            value = value.trim();
            try {
                Jenkins.checkGoodName(value);
            } catch (Failure e) {
                return FormValidation.error(e.getMessage());
            }

            String name = cloudNameToName(value);
            int found = 0;
            for (Cloud c : getClouds()) {
                if (c.name.equals(name)) {
                    found++;
                }
            }

            // We don't know whether the user is adding a new cloud or updating
            // an existing one.  If they are adding a new cloud, then found==0,
            // but if they are updating an existing cloud, then found==1, and we
            // do not want to give an error, so we check found>1.  This means
            // this error is only given after the user has already saved a new
            // cloud with a duplicate name and has reopened the configuration.
            if (found > 1) {
                return FormValidation.error(Messages.BaremetalCloud_cloudName_duplicate(value));
            }

            return FormValidation.ok();
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item context, @QueryParameter String credentialsId) {
            StandardListBoxModel result = new StandardListBoxModel();
            Jenkins instance = Jenkins.getInstance();
            if (context != null && instance != null ) {
                if (!context.hasPermission(Item.EXTENDED_READ) && !context.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return result.includeCurrentValue(credentialsId);
                }
            } else {
                if (instance != null && !instance.hasPermission(Jenkins.ADMINISTER)) {
                    return result.includeCurrentValue(credentialsId);
                }
            }

            List<DomainRequirement> domainRequirements = new ArrayList<>();

            return result.includeMatchingAs(ACL.SYSTEM, context, BaremetalCloudCredentials.class, domainRequirements, anyOf(instanceOf(BaremetalCloudCredentials.class)));
        }

        public static FormValidation withContext(FormValidation fv, String context) {
            return FormValidation.error(JenkinsUtil.unescape(fv.getMessage()) + ": " + context);
        }
    }
}