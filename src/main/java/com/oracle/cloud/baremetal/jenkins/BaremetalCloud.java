package com.oracle.cloud.baremetal.jenkins;

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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.oracle.bmc.core.model.Instance;
import com.oracle.bmc.model.BmcException;
import com.oracle.cloud.baremetal.jenkins.client.BaremetalCloudClient;
import com.oracle.cloud.baremetal.jenkins.client.BaremetalCloudClientFactory;
import com.oracle.cloud.baremetal.jenkins.client.SDKBaremetalCloudClientFactory;
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
import hudson.slaves.AbstractCloudImpl;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner.PlannedNode;
import hudson.util.FormValidation;
import hudson.util.HttpResponses;
import hudson.util.Secret;
import jenkins.model.Jenkins;



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

    private final String fingerprint;
    private final String apikey;
    private final String passphrase;
    private final String tenantId;
    private final String userId;
    private final String regionId;
    private final int nextTemplateId;
    private final List<? extends BaremetalCloudAgentTemplate> templates;

    @DataBoundConstructor
    public BaremetalCloud(
            String cloudName,
            String fingerprint,
            String apikey,
            String passphrase,
            String tenantId,
            String userId,
            String regionId,
            String instanceCapStr,
            int nextTemplateId,
            List<? extends BaremetalCloudAgentTemplate> templates) {
        super(cloudNameToName(cloudName), instanceCapStr);

        this.fingerprint = fingerprint;
        this.apikey = getEncryptedValue(apikey);
        this.passphrase = getEncryptedValue(passphrase);
        this.tenantId = tenantId;
        this.userId = userId;
        this.regionId = regionId;
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

    public String getFingerprint() {
        return fingerprint;
    }

    public String getApikey() {
        return getPlainText(apikey);
    }

    public String getPassphrase() {
        return getPlainText(passphrase);
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getUserId() {
        return userId;
    }

    public String getRegionId() {
        return regionId;
    }

    public int getNextTemplateId() {
        return nextTemplateId;
    }

    public List<? extends BaremetalCloudAgentTemplate> getTemplates() {
        return templates;
    }

    protected String getEncryptedValue(String str) {
        return Secret.fromString(str).getEncryptedValue();
    }

    protected String getPlainText(String str) {
        return  str == null ? null : Secret.decrypt(str).getPlainText();
    }

    ExecutorService getThreadPoolForRemoting() {
        return Computer.threadPoolForRemoting;
    }

    PlannedNode newPlannedNode(String displayName, Future<Node> future, int numExecutors, BaremetalCloudAgentTemplate template) {
        return new PlannedNode(displayName, future, numExecutors);
    }

    @Override
    public Collection<PlannedNode> provision(Label label, int excessWorkload) {
        final BaremetalCloudAgentTemplate t = getTemplate(label);
        if (t == null) {
            return Collections.emptyList();
        }

        // TODO: reuse existing nodes
        int numAgents = countCurrentBaremetalCloudAgents();

        List<PlannedNode> r = new ArrayList<>();
        for (; excessWorkload > 0 && numAgents < getInstanceCap(); numAgents++) {
            Provisioner provisioner = new Provisioner(t);
            String displayName = provisioner.getPlannedNodeDisplayName();
            Future<Node> future = getThreadPoolForRemoting().submit(provisioner);

            int numExecutors = provisioner.numExecutors;
            r.add(newPlannedNode(displayName, future, numExecutors, t));
            excessWorkload -= numExecutors;
        }
        return r;
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
            this.instanceName = INSTANCE_NAME_PREFIX + uuid;
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
                Ip = client.getInstancePublicIp(template, instance.getId());
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

    public void recycleCloudResources(String instanceId) throws IOException{
        BaremetalCloudClient client = getClient();
        try{
            client.terminateInstance(instanceId);
            client.waitForInstanceTerminationToComplete(instanceId);
        }catch(Exception e){
            throw new IOException(e);
        }


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
        return factory.createClient(fingerprint, getApikey(), getPassphrase(), tenantId, userId, regionId);
    }

    public int countCurrentBaremetalCloudAgents() {
        int r = 0;
        for (Node n : getNodes()){
	        	if(n instanceof BaremetalCloudAgent){
	            BaremetalCloudAgent agent = (BaremetalCloudAgent)n;
	            if (name.equals(agent.cloudName)) {
	                r++;
	            }
        	}
        }
        return r;
    }

    // make sure the instance if available before launch agent on it.
    private void awaitInstanceSshAvailable(String host, int connectTimeoutMillis, TimeoutHelper timeoutHelper) throws IOException, InterruptedException {
        SshConnector sshConnector = getSshConnector();
        do {
            Connection conn = sshConnector.createConnection(host);
            try {
                sshConnector.connect(conn, connectTimeoutMillis);
                return;
            } catch (IOException e) {
                LOGGER.log(Level.FINER, "Ignoring exception connecting to SSH during privision", e);
            } finally {
                conn.close();
            }
        } while (timeoutHelper.sleep());

        throw new IOException("Timed out connecting to SSH");
    }

    SshConnector getSshConnector() {
        return SshConnector.INSTANCE;
    }

    List<Node> getNodes() {
        return JenkinsUtil.getJenkinsInstance().getNodes();
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

        public static FormValidation withContext(FormValidation fv, String context) {
            return FormValidation.error(JenkinsUtil.unescape(fv.getMessage()) + ": " + context);
        }

        /**
         * Test connection from configuration page.
         * @param fingerprint fingerprint
         * @param apikey apikey
         * @param passphrase passphrase
         * @param tenantId tenant id
         * @param userId User credentials
         * @param regionId region Id
         * @return FormValidation
         */
        public FormValidation doTestConnection(
                @QueryParameter String fingerprint,
                @QueryParameter String apikey,
                @QueryParameter String passphrase,
                @QueryParameter String tenantId,
                @QueryParameter String userId,
                @QueryParameter String regionId) {

            BaremetalCloudClientFactory factory = SDKBaremetalCloudClientFactory.INSTANCE;
            BaremetalCloudClient client = factory.createClient(fingerprint, apikey, passphrase, tenantId, userId, regionId);
            try{
                client.authenticate();
                return FormValidation.ok(Messages.BaremetalCloud_testConnection_success());
            }catch(BmcException e){
                LOGGER.log(Level.FINE, "Failed to connect to Oracle Cloud Infrastructure, Please verify all the credential informations enterred", e);
                return FormValidation.error(Messages.BaremetalCloud_testConnection_unauthorized());
            }
        }
    }

}
