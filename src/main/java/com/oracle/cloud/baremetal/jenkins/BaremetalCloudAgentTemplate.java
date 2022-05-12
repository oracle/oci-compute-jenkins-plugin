package com.oracle.cloud.baremetal.jenkins;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.cloudbees.jenkins.plugins.sshcredentials.SSHAuthenticator;
import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;

import javax.servlet.ServletException;

import com.oracle.bmc.identity.model.Tenancy;
import hudson.EnvVars;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.oracle.bmc.core.model.Image;
import com.oracle.bmc.core.model.Shape;
import com.oracle.bmc.core.model.Subnet;
import com.oracle.bmc.core.model.Vcn;
import com.oracle.bmc.core.responses.GetSubnetResponse;
import com.oracle.bmc.identity.model.AvailabilityDomain;
import com.oracle.bmc.identity.model.Compartment;
import com.oracle.cloud.baremetal.jenkins.client.BaremetalCloudClient;
import com.oracle.cloud.baremetal.jenkins.client.BaremetalCloudClientFactory;
import com.oracle.cloud.baremetal.jenkins.client.SDKBaremetalCloudClientFactory;

import hudson.Extension;
import hudson.RelativePath;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.labels.LabelAtom;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;

import java.util.stream.IntStream;

import jenkins.model.Jenkins;
import org.kohsuke.stapler.AncestorInPath;

public class BaremetalCloudAgentTemplate implements Describable<BaremetalCloudAgentTemplate>{
    private static final Logger LOGGER = Logger.getLogger(BaremetalCloud.class.getName());
    static final int FAILURE_COUNT_LIMIT = 3;
    static final int DISABLE_FAILURE_COUNT_LIMIT = 20;
    public transient boolean templateSleep = false;

    public final String compartmentId;
    public final String availableDomain;
    public final String vcnCompartmentId;
    public final String vcnId;
    public final String subnetCompartmentId;
    public final String subnetId;
    public final List<BaremetalCloudNsgTemplate> nsgIds;
    public final String imageCompartmentId;
    public final String imageId;
    public final String shape;
    public final String sshCredentialsId;
    public final String description;
    public final String labelString;
    public transient Collection<LabelAtom> labelAtoms;
    public final Node.Mode mode;
    public final String jenkinsAgentUser;
    public final String initScript;
    public final Boolean exportJenkinsEnvVars;
    public final String numExecutors;
    public final String idleTerminationMinutes;
    public final int templateId;
    public final String remoteFS;
    public final Boolean assignPublicIP;
    public final Boolean usePublicIP;
    public final String startTimeoutSeconds;
    public final String sshConnectTimeoutSeconds;
    public final String initScriptTimeoutSeconds;
    public final String instanceCap;
    public final String numberOfOcpus;
    public final Boolean autoImageUpdate;
    public final Boolean stopOnIdle;
    public final List<BaremetalCloudTagsTemplate> tags;
    public final String instanceNamePrefix;
    public final String memoryInGBs;
    public final Boolean doNotDisable;
    public final String retryTimeoutMins;

    private transient int failureCount=0;
    private transient String disableCause;
    public transient long sleepStartTime = 0;

    @DataBoundConstructor
    public BaremetalCloudAgentTemplate(
            final String compartmentId,
            final String availableDomain,
            final String vcnCompartmentId,
            final String vcnId,
            final String subnetCompartmentId,
            final String subnetId,
            final List<BaremetalCloudNsgTemplate> nsgIds,
            final String imageCompartmentId,
            final String imageId,
            final String shape,
            final String sshCredentialsId,
            final String description,
            final String remoteFS,
            final Boolean assignPublicIP,
            final Boolean usePublicIP,
            final String numExecutors,
            Node.Mode mode,
            final String labelString,
            final String idleTerminationMinutes,
            final int templateId,
            final String jenkinsAgentUser,
            final String initScript,
            final Boolean exportJenkinsEnvVars,
            final String sshConnectTimeoutSeconds,
            final String startTimeoutSeconds,
            final String initScriptTimeoutSeconds,
            final String instanceCap,
            final String numberOfOcpus,
            final Boolean autoImageUpdate,
            final Boolean stopOnIdle,
            final List<BaremetalCloudTagsTemplate> tags,
            final String instanceNamePrefix,
            final String memoryInGBs,
            final Boolean doNotDisable,
            final String retryTimeoutMins){
    	this.compartmentId = compartmentId;
        this.availableDomain = availableDomain;
        this.vcnCompartmentId = vcnCompartmentId;
        this.vcnId = vcnId;
        this.subnetCompartmentId = subnetCompartmentId;
        this.subnetId = subnetId;
        this.nsgIds = nsgIds;
        this.imageCompartmentId = imageCompartmentId;
        this.imageId = imageId;
        this.shape = shape;
        this.sshCredentialsId = sshCredentialsId;
        this.description = description;
        this.remoteFS = remoteFS;
        this.assignPublicIP=assignPublicIP;
        this.usePublicIP=usePublicIP;
        this.numExecutors = numExecutors;
        this.mode = mode;
        this.labelString = labelString;
        this.idleTerminationMinutes = idleTerminationMinutes;
        this.templateId = templateId;
        this.jenkinsAgentUser = jenkinsAgentUser;
        this.initScript = initScript;
        this.exportJenkinsEnvVars = exportJenkinsEnvVars;
        this.sshConnectTimeoutSeconds = sshConnectTimeoutSeconds;
        this.startTimeoutSeconds = startTimeoutSeconds;
        this.initScriptTimeoutSeconds = initScriptTimeoutSeconds;
        this.instanceCap = instanceCap;
        this.numberOfOcpus = numberOfOcpus;
        this.autoImageUpdate = autoImageUpdate;
        this.stopOnIdle = stopOnIdle;
        this.tags = tags;
        this.instanceNamePrefix = instanceNamePrefix;
        this.memoryInGBs = memoryInGBs;
        this.doNotDisable = doNotDisable;
        this.retryTimeoutMins = retryTimeoutMins;
    }

    public String getCompartmentId() {
        return compartmentId;
    }

    public String getAvailableDomain() {
        return availableDomain;
    }

    public String getVcnCompartmentId() {
        return vcnCompartmentId;
    }

    public String getVcn() {
        return vcnId;
    }

    public String getSubnetCompartmentId() {
        return subnetCompartmentId;
    }

    public String getSubnet() {
        return subnetId;
    }

    public List<BaremetalCloudNsgTemplate> getNsgIds() {
        return nsgIds;
    }

    public String getImageCompartmentId() {
        return imageCompartmentId;
    }

    public String getImage() {
        return imageId;
    }

    public Boolean getAutoImageUpdate() {
        return autoImageUpdate == null ? Boolean.FALSE : autoImageUpdate;
    }

    public String getShape() {
        return shape;
    }

    public String getSshCredentialsId() {
        return sshCredentialsId;
    }

    public String getDisplayName() {
        return String.valueOf(getDescription());
    }

    public String getDescription() {
        return description;
    }

    public String getJenkinsAgentUser() {
        return jenkinsAgentUser;
    }

    public String getRemoteFS() {
        return remoteFS;
    }

    public Boolean getAssignPublicIP() {
    	return assignPublicIP;
    }

    public Boolean getUsePublicIP() {
    	return usePublicIP;
    }

    public int getNumExecutors() {
        try {
            return Math.max(Integer.parseInt(numExecutors), 1);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static FormValidationValue<Integer> checkNumExecutors(String value) {
        return FormValidationValue.validatePositiveInteger(value, 1);
    }

    public int getNumExecutorsValue() {
        return checkNumExecutors(numExecutors).getValue();
    }

    public Node.Mode getMode() {
        return mode;
    }

    public String getLabelString() {
        return labelString;
    }

    Collection<LabelAtom> parseLabels(String labels) {
        return Label.parse(labels);
    }

    public synchronized Collection<LabelAtom> getLabelAtoms() {
        Collection<LabelAtom> labelAtoms = this.labelAtoms;
        if (labelAtoms == null) {
            labelAtoms = parseLabels(labelString);
            this.labelAtoms = labelAtoms;
        }
        return labelAtoms;
    }

    public String getIdleTerminationMinutes() {
        return idleTerminationMinutes;
    }

    public int getTemplateId() {
        return templateId;
    }

    public String getInitScript() {
        return initScript;
    }

    public String getInitScriptEnvVarsVersion(){
        return !getExportJenkinsEnvVars() ? initScript : addJenkinsEnvVarsToInitScript(initScript);
    }

    public Boolean getExportJenkinsEnvVars() {
        // if condition is needed for upgrade compatibility
        return exportJenkinsEnvVars == null ? Boolean.FALSE : exportJenkinsEnvVars;
    }

    public String getStartTimeoutSeconds() {
        return startTimeoutSeconds;
    }

    public String getSshConnectTimeoutSeconds() {
        return sshConnectTimeoutSeconds;
    }

    private static FormValidationValue<Integer> checkStartTimeoutSeconds(String value) {
        return FormValidationValue.validateNonNegativeInteger(value, (int)TimeUnit.MINUTES.toSeconds(15));
    }

    public long getStartTimeoutNanos() {
        return TimeUnit.SECONDS.toNanos(checkStartTimeoutSeconds(startTimeoutSeconds).getValue());
    }

    private static FormValidationValue<Integer> checkSshConnectTimeoutSeconds(String value) {
        return FormValidationValue.validateNonNegativeInteger(value, 30);
    }

    public int getSshConnectTimeoutMillis() {
        return (int)TimeUnit.SECONDS.toMillis(checkSshConnectTimeoutSeconds(sshConnectTimeoutSeconds).getValue());
    }

    public int getInitScriptTimeoutSeconds() {
        return (int)TimeUnit.SECONDS.toSeconds(checkInitScriptTimeoutSeconds(initScriptTimeoutSeconds).getValue());
    }

    public String getInstanceCap() {
        return instanceCap;
    }

    public String getNumberOfOcpus() {
        return numberOfOcpus;
    }

    public String getMemoryInGBs() {
        // if condition is needed for upgrade compatibility
        return (memoryInGBs == null  && !numberOfOcpus.isEmpty()) ? Integer.toString(Integer.parseInt(numberOfOcpus)*16) : memoryInGBs;
    }

    public String getPublicKey() throws IOException {
        SSHUserPrivateKey sshCredentials = (SSHUserPrivateKey) BaremetalCloud.matchCredentials(SSHUserPrivateKey.class, this.sshCredentialsId);
        if (sshCredentials != null) {
            return SshKeyUtil.getPublicKey(sshCredentials.getPrivateKey(), Secret.toString(sshCredentials.getPassphrase()));
        } else {
            return null;
        }
    }

    public Boolean getStopOnIdle() {
        return stopOnIdle == null ? Boolean.FALSE : stopOnIdle;
    }

    public List<BaremetalCloudTagsTemplate> getTags() {
        return tags;
    }

    public String getInstanceNamePrefix() {
        return instanceNamePrefix;
    }

    private static FormValidationValue<Integer> checkInitScriptTimeoutSeconds(String value){
        return FormValidationValue.validateNonNegativeInteger(value, 120);
    }

    @Override
    public Descriptor<BaremetalCloudAgentTemplate> getDescriptor() {
        // TODO Auto-generated method stub
        return JenkinsUtil.getDescriptorOrDie(getClass());
    }

    public synchronized void increaseFailureCount(String cause) {
        ++failureCount;
        if(doNotDisable==null) {
            if (failureCount >= FAILURE_COUNT_LIMIT) {
                LOGGER.warning("Agent template " + getDisplayName() + " disabled due to error: " + cause);
                disableCause = cause;
            }
        }else if(failureCount >= FAILURE_COUNT_LIMIT && !doNotDisable){
            LOGGER.warning("Agent template " + getDisplayName() + " disabled due to error: " + cause);
            disableCause = cause;
        }else if(failureCount >= DISABLE_FAILURE_COUNT_LIMIT && doNotDisable){
            LOGGER.info("Agent template "+getDisplayName()+ " encountered " + failureCount +" failures till now. " +
                    "It is disabled due to error: " + cause);
            disableCause = cause;
        } else{
            LOGGER.info("Agent template "+getDisplayName()+ " encountered " + failureCount +" failures till now.");
        }
    }

    public synchronized void resetFailureCount() {
        if (failureCount > 0) {
            failureCount = 0;
            if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("Agent template " + getDisplayName() + " is reset");
        }
        if (disableCause != null) {
            disableCause = null;
            LOGGER.info("Agent template " + getDisplayName() + " is re-enabled");
        }
    }

    public synchronized String getDisableCause() {
        return disableCause;
    }

    private String addJenkinsEnvVarsToInitScript(String initScript) {

        EnvVars vars = JenkinsUtil.getJenkinsEnvVars();
        if (vars != null) {
            StringBuilder newInitScript = new StringBuilder();
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                newInitScript.append("export ").append(key).append("=").append(value).append("\n");
            }
            return newInitScript.append(initScript).toString();
        }
        return initScript;
    }

    public Boolean getDoNotDisable() { return doNotDisable; }

    public synchronized void setTemplateSleep(Boolean sleepvar){
        this.templateSleep=sleepvar;
    }

    public synchronized boolean isTemplateSleep() {
        return templateSleep;
    }

    private static FormValidationValue<Integer> checkRetryTimeoutMins(String value) {
        return FormValidationValue.validateNonNegativeInteger(value, 10);
    }

    public int getRetryTimeoutMins() {
        return (int)TimeUnit.MINUTES.toMinutes(checkRetryTimeoutMins(retryTimeoutMins).getValue());
    }

    public synchronized void setSleepStartTime(long sleepStartTime) {
        LOGGER.log(Level.FINE, "Setting the current time for sleep " + sleepStartTime);
        this.sleepStartTime = sleepStartTime;
    }

    public synchronized long getSleepStartTime() {
        LOGGER.log(Level.FINE, "Getting the set time for sleep " + this.sleepStartTime);
        return this.sleepStartTime;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<BaremetalCloudAgentTemplate> {

        @Override
        public String getHelpFile(String fieldName) {
            String p = super.getHelpFile(fieldName);
            if (p == null) {
                Jenkins jenkins = Jenkins.getInstanceOrNull();
                if (jenkins != null) {
                    Descriptor descriptor = jenkins.getDescriptor(BaremetalCloudAgent.class);
                    if (descriptor != null)
                        p = descriptor.getHelpFile(fieldName);
                }
            }
            return p;
        }

        public static int getDefaultNumExecutors() {
            return checkNumExecutors(null).getValue();
        }

        public FormValidation doCheckNumExecutors(@QueryParameter String value) {
            return checkNumExecutors(value).getFormValidation();
        }

        public FormValidation doCheckSshConnectTimeoutSeconds(@QueryParameter String value) {
            return checkSshConnectTimeoutSeconds(value).getFormValidation();
        }

        public FormValidation doCheckAssignPublicIP(
                @QueryParameter @RelativePath("..") String credentialsId,
                @QueryParameter @RelativePath("..") String maxAsyncThreads,
                @QueryParameter String subnetId,
                @QueryParameter Boolean assignPublicIP) {
               if (subnetId != null && !subnetId.equals("") && (assignPublicIP == null || assignPublicIP)) {
                   BaremetalCloudClient client = getClient(credentialsId, maxAsyncThreads);

                   try{
                       GetSubnetResponse subnetResponse = client.getSubNet(subnetId);
                       if (subnetResponse.getSubnet().getProhibitPublicIpOnVnic()) {
                           return FormValidation.error(Messages.BaremetalCloudAgentTemplate_assignPublicIP_unable());
                       }
                   }catch (Exception e) {
                       LOGGER.log(Level.WARNING, "Failed to get subnet: " + subnetId, e);
                   }
               }

               return FormValidation.ok();
        }

        public FormValidation doCheckDoNotDisable(
                @QueryParameter @RelativePath("..") String credentialsId,
                @QueryParameter @RelativePath("..") String maxAsyncThreads,
                @QueryParameter Boolean doNotDisable) {
            if (doNotDisable == null || doNotDisable) {
                return FormValidation.warning(Messages.BaremetalCloudAgentTemplate_doNotDisable_warningmsg());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckUsePublicIP(
                @QueryParameter Boolean assignPublicIP,
                @QueryParameter Boolean usePublicIP) {
               if (usePublicIP != null && assignPublicIP != null && usePublicIP && !assignPublicIP) {
                   return FormValidation.error(Messages.BaremetalCloudAgentTemplate_usePublicIP_unable());
               }
               return FormValidation.ok();
        }

        public FormValidation doCheckInstanceNamePrefix(@QueryParameter String instanceNamePrefix) {
            if(instanceNamePrefix.contains(" ")) {
                return FormValidation.error(Messages.BaremetalCloudAgentTemplate_prefix_contains_spaces());
            }
            return FormValidation.ok();
        }

        private static boolean anyRequiredFieldEmpty(String... fields) {
            for (String field : fields) {
                if (field == null || field.isEmpty()) {
                    return true;
                }
            }
            return false;
        }

        BaremetalCloud.DescriptorImpl getBaremetalCloudDescriptor() {
            return JenkinsUtil.getDescriptorOrDie(BaremetalCloud.class, BaremetalCloud.DescriptorImpl.class);
        }

        private static BaremetalCloudClient getClient(String credentialsId, String maxAsyncThreads){
            BaremetalCloudClientFactory factory = SDKBaremetalCloudClientFactory.INSTANCE;
            return factory.createClient(credentialsId, Integer.parseInt(maxAsyncThreads));
        }

        public ListBoxModel doFillCompartmentIdItems(
                @QueryParameter @RelativePath("..") String credentialsId,
                @QueryParameter @RelativePath("..") String maxAsyncThreads)
                        throws IOException, ServletException {
            ListBoxModel model = new ListBoxModel();
            model.add("<Select a compartmentId>", "");

            if (anyRequiredFieldEmpty(credentialsId)) {
                return model;
            }

            try{
                BaremetalCloudClient client = getClient(credentialsId, maxAsyncThreads);
                Tenancy tenant = client.getTenant();
                model.add(tenant.getName(), tenant.getId());
                for (Compartment compartmentId : client.getCompartmentsList()) {
                    model.add(compartmentId.getName(), compartmentId.getId());
                }
            }catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to get compartment list", e);
            }
            return model;
        }

        public ListBoxModel doFillAvailableDomainItems(
                @QueryParameter @RelativePath("..") String credentialsId,
                @QueryParameter @RelativePath("..") String maxAsyncThreads,
                @QueryParameter String compartmentId) {
            ListBoxModel model = new ListBoxModel();

            if (anyRequiredFieldEmpty(credentialsId, compartmentId)) {
                model.add("<First select 'Compartment'>", "");
                return model;
            }

            try {
                BaremetalCloudClient client = getClient(credentialsId, maxAsyncThreads);
                List<String>  lstDomain = new ArrayList<String>();
                for (AvailabilityDomain domain : client.getAvailabilityDomainsList(compartmentId)) {
                    if (!lstDomain.contains(domain.getName())) {
                        model.add(domain.getName(), domain.getName());
                        lstDomain.add(domain.getName());
                    }
                }
                return model;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to get AvailableDomain list", e);
                return model;
            }
        }

        public ListBoxModel doFillImageCompartmentIdItems(
                @QueryParameter @RelativePath("..") String credentialsId,
                @QueryParameter @RelativePath("..") String maxAsyncThreads,
                @QueryParameter String compartmentId) throws IOException, ServletException {
            ListBoxModel model = new ListBoxModel();

            if (anyRequiredFieldEmpty(credentialsId, compartmentId)) {
                model.add("<First select 'Compartment'>", "");
                return model;
            }

            try{
                BaremetalCloudClient client = getClient(credentialsId, maxAsyncThreads);
                Tenancy tenant = client.getTenant();
                model.add(tenant.getName(), tenant.getId());
                for (Compartment compartment : client.getCompartmentsList()) {
                        model.add(compartment.getName(), compartment.getId());
                    }
            }catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to get compartment list", e);
            }

            return model;
        }

        public ListBoxModel doFillImageIdItems(
                @QueryParameter @RelativePath("..") String credentialsId,
                @QueryParameter @RelativePath("..") String maxAsyncThreads,
                @QueryParameter String compartmentId,
                @QueryParameter String imageCompartmentId) throws IOException, ServletException {
            ListBoxModel model = new ListBoxModel();
            model.add("<Select an Image>", "");
            if (anyRequiredFieldEmpty(credentialsId, compartmentId)) {
                return model;
            }
            if (anyRequiredFieldEmpty(imageCompartmentId)) {
                imageCompartmentId = compartmentId;
            }

            try {
                BaremetalCloudClient client = getClient(credentialsId, maxAsyncThreads);
                List<String>  lstImage = new ArrayList<String>();

                for (Image imageId : client.getImagesList(imageCompartmentId)) {
                    if (!lstImage.contains(imageId.getId())) {
                        model.add(imageId.getDisplayName(), imageId.getId());
                        lstImage.add(imageId.getId());
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to get images list", e);
            }
            return model;
        }

        public ListBoxModel doFillShapeItems(
                @QueryParameter @RelativePath("..") String credentialsId,
                @QueryParameter @RelativePath("..") String maxAsyncThreads,
                @QueryParameter String compartmentId,
                @QueryParameter String availableDomain,
                @QueryParameter String imageId)
                        throws IOException, ServletException {
            ListBoxModel model = new ListBoxModel();
            model.add("<First select 'Availablity Domain' and 'Image' above>", "");

            if (anyRequiredFieldEmpty(credentialsId, compartmentId, imageId)) {
                return model;
            }

            try {
                BaremetalCloudClient client = getClient(credentialsId, maxAsyncThreads);
                List<String>  lstShape = new ArrayList<String>();

                for (Shape shape : client.getShapesList(compartmentId, availableDomain, imageId)) {
                    if (!lstShape.contains(shape.getShape())) {
                        model.add(shape.getShape(), shape.getShape());
                        lstShape.add(shape.getShape());
                    }
                }

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to get shapes list", e);
            }
            return model;
        }

        public ListBoxModel doFillNumberOfOcpusItems(
                @QueryParameter @RelativePath("..") String credentialsId,
                @QueryParameter @RelativePath("..") String maxAsyncThreads,
                @QueryParameter String compartmentId,
                @QueryParameter String availableDomain,
                @QueryParameter String imageId,
                @QueryParameter String shape)
                throws IOException, ServletException {
            ListBoxModel model = new ListBoxModel();

            if (anyRequiredFieldEmpty(credentialsId, compartmentId, availableDomain, imageId, shape)) {
                model.clear();
                model.add("<First select 'Availablity Domain' and 'Image' and 'Shape' above>","");
                return model;
            }

            if (!shape.contains("Flex")) {
                model.clear();
                model.add("<This field only takes effect for flexible shape if selected>","");
                return model;
            }

            try {
                model.clear();
                BaremetalCloudClient client = getClient(credentialsId, maxAsyncThreads);
                Integer[] ocpuOptions = client.getMinMaxOcpus(compartmentId, availableDomain, imageId, shape);
                IntStream.range(ocpuOptions[0], ocpuOptions[1]+1).forEach(n -> model.add(Integer.toString(n)));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to get ocpus options list", e);
            }
            return model;
        }

        public ListBoxModel doFillMemoryInGBsItems(
                @QueryParameter @RelativePath("..") String credentialsId,
                @QueryParameter @RelativePath("..") String maxAsyncThreads,
                @QueryParameter String compartmentId,
                @QueryParameter String availableDomain,
                @QueryParameter String imageId,
                @QueryParameter String shape)
                throws IOException, ServletException {
            ListBoxModel model = new ListBoxModel();

            if (anyRequiredFieldEmpty(credentialsId, compartmentId, availableDomain, imageId, shape)) {
                model.clear();
                model.add("<First select 'Availablity Domain' and 'Image' and 'Shape' above>","");
                return model;
            }

            if (!shape.contains("Flex")) {
                model.clear();
                model.add("<This field only takes effect for flexible shape if selected>","");
                return model;
            }

            try {
                model.clear();
                BaremetalCloudClient client = getClient(credentialsId, maxAsyncThreads);
                Integer[] memoryOptions = client.getMinMaxMemory(compartmentId, availableDomain, imageId, shape);
                IntStream.range(memoryOptions[0], memoryOptions[1]+1).forEach(n -> model.add(Integer.toString(n)));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to get memory options list", e);
            }
            return model;
        }

        public ListBoxModel doFillVcnCompartmentIdItems(
                @QueryParameter @RelativePath("..") String credentialsId,
                @QueryParameter @RelativePath("..") String maxAsyncThreads,
                @QueryParameter String compartmentId) throws IOException, ServletException {
            ListBoxModel model = new ListBoxModel();

            if (anyRequiredFieldEmpty(credentialsId, compartmentId)) {
                model.add("<First select 'Compartment'>", "");
                return model;
            }

            try{
                BaremetalCloudClient client = getClient(credentialsId, maxAsyncThreads);
                Tenancy tenant = client.getTenant();
                model.add(tenant.getName(), tenant.getId());
                for (Compartment compartment : client.getCompartmentsList()) {
                        model.add(compartment.getName(), compartment.getId());
                    }
            }catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to get compartment list", e);
            }

            return model;
        }

        public ListBoxModel doFillVcnIdItems(
                @QueryParameter @RelativePath("..") String credentialsId,
                @QueryParameter @RelativePath("..") String maxAsyncThreads,
                @QueryParameter String compartmentId,
                @QueryParameter String vcnCompartmentId) throws IOException, ServletException {
            ListBoxModel model = new ListBoxModel();
            model.add("<Select a Virtual Cloud Network>", "");

            if (anyRequiredFieldEmpty(credentialsId, compartmentId)) {
                return model;
            }

            if (anyRequiredFieldEmpty(vcnCompartmentId)) {
                vcnCompartmentId = compartmentId;
            }

            try {
                BaremetalCloudClient client = getClient(credentialsId, maxAsyncThreads);
                for (Vcn vcnId : client.getVcnList(vcnCompartmentId)) {
                    model.add(vcnId.getDisplayName(), vcnId.getId());
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to get VCN list", e);
            }
            return model;
        }

        public ListBoxModel doFillSubnetCompartmentIdItems(
                @QueryParameter @RelativePath("..") String credentialsId,
                @QueryParameter @RelativePath("..") String maxAsyncThreads,
                @QueryParameter String compartmentId) throws IOException, ServletException {
            ListBoxModel model = new ListBoxModel();

            if (anyRequiredFieldEmpty(credentialsId, compartmentId)) {
                model.add("<First select 'Compartment'>", "");
                return model;
            }

            try{
                BaremetalCloudClient client = getClient(credentialsId, maxAsyncThreads);
                Tenancy tenant = client.getTenant();
                model.add(tenant.getName(), tenant.getId());
                for (Compartment compartment : client.getCompartmentsList()) {
                    model.add(compartment.getName(), compartment.getId());
                }
            }catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to get compartment list", e);
            }

            return model;
        }

        public ListBoxModel doFillSubnetIdItems(
                @QueryParameter @RelativePath("..") String credentialsId,
                @QueryParameter @RelativePath("..") String maxAsyncThreads,
                @QueryParameter String availableDomain,
                @QueryParameter String vcnId,
                @QueryParameter String compartmentId,
                @QueryParameter String vcnCompartmentId,
                @QueryParameter String subnetCompartmentId) throws IOException, ServletException {
            ListBoxModel model = new ListBoxModel();
            model.add("<First select 'Availablity Domain' and 'Virtual Cloud Network' above>", "");

            if (anyRequiredFieldEmpty(credentialsId, availableDomain, vcnId, compartmentId)) {
                return model;
            }

            if (anyRequiredFieldEmpty(subnetCompartmentId)) {
                subnetCompartmentId = vcnCompartmentId;
            }

            try {
                BaremetalCloudClient client = getClient(credentialsId, maxAsyncThreads);
                for (Subnet subnet : client.getSubNetList(subnetCompartmentId, vcnId)) {
                    if (null == subnet.getAvailabilityDomain() || subnet.getAvailabilityDomain().equals(availableDomain)) {
                        model.add(subnet.getDisplayName(), subnet.getId());
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to get subnet list", e);
            }
            return model;
        }

        public ListBoxModel doFillSshCredentialsIdItems(
                @AncestorInPath Item context, 
                @QueryParameter String sshCredentialsId) {
            StandardListBoxModel result = new StandardListBoxModel();
            Jenkins instance = Jenkins.getInstanceOrNull();
            if (context == null) {
                if (instance != null && !instance.hasPermission(Jenkins.ADMINISTER)) {
                    return result.includeCurrentValue(sshCredentialsId);
                }
            } else {
                if (!context.hasPermission(Item.EXTENDED_READ) && !context.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return result.includeCurrentValue(sshCredentialsId);
                }
            }

            List<DomainRequirement> domainRequirements = new ArrayList<DomainRequirement>();
            return result.includeMatchingAs(ACL.SYSTEM, context, SSHUserPrivateKey.class, domainRequirements, SSHAuthenticator.matcher());
}

        public FormValidation doCheckLabelString(@QueryParameter String value, @QueryParameter Node.Mode mode) {
            if (mode == Node.Mode.EXCLUSIVE && (value == null || value.trim().isEmpty())) {
                return FormValidation.warning(Messages.BaremetalCloudAgentTemplate_labelString_exclusiveEmpty());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckIdleTerminationMinutes(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.ok();
            }
            return FormValidation.validateNonNegativeInteger(value);
        }

        public FormValidation doCheckStartTimeoutSeconds(@QueryParameter String value) {
            return checkStartTimeoutSeconds(value).getFormValidation();
        }

        public FormValidation doCheckInstanceCap(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.ok();
            }
            return FormValidation.validateNonNegativeInteger(value);
        }
    }
}
