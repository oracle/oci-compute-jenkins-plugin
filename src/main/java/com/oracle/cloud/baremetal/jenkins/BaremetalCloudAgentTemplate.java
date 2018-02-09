package com.oracle.cloud.baremetal.jenkins;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.interfaces.RSAPublicKey;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.oracle.bmc.core.model.Image;
import com.oracle.bmc.core.model.Shape;
import com.oracle.bmc.core.model.Subnet;
import com.oracle.bmc.core.model.Vcn;
import com.oracle.bmc.identity.model.AvailabilityDomain;
import com.oracle.bmc.identity.model.Compartment;
import com.oracle.bmc.identity.responses.ListAvailabilityDomainsResponse;
import com.oracle.cloud.baremetal.jenkins.client.BaremetalCloudClient;
import com.oracle.cloud.baremetal.jenkins.client.BaremetalCloudClientFactory;
import com.oracle.cloud.baremetal.jenkins.client.SDKBaremetalCloudClientFactory;

import hudson.Extension;
import hudson.RelativePath;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.labels.LabelAtom;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.bouncycastle.api.PEMEncodable;

public class BaremetalCloudAgentTemplate implements Describable<BaremetalCloudAgentTemplate>{
    private static final Logger LOGGER = Logger.getLogger(BaremetalCloud.class.getName());
    static final int FAILURE_COUNT_LIMIT = 3;

    public final String compartmentId;
    public final String availableDomain;
    public final String vcnId;
    public final String subnetId;
    public final String imageId;
    public final String shape;
    public final String sshPublickey;
    public final String sshPrivatekey;

    public final String description;
    public final String labelString;
    public transient Collection<LabelAtom> labelAtoms;
    public final Node.Mode mode;
    public final String initScript;
    public final String numExecutors;
    public final String idleTerminationMinutes;
    public final int templateId;
    public final String remoteFS;
    public final String sshUser;
    public final String startTimeoutSeconds;
    public final String sshConnectTimeoutSeconds;
    public final String initScriptTimeoutSeconds;

    private transient int failureCount;
    private transient String disableCause;

    @DataBoundConstructor
    public BaremetalCloudAgentTemplate(
            final String compartmentId,
            final String availableDomain,
            final String vcnId,
            final String subnetId,
            final String imageId,
            final String shape,
            final String sshPublickey,
            final String sshPrivatekey,
            final String description,
            final String remoteFS,
            final String sshUser,
            final String numExecutors,
            Node.Mode mode,
            final String labelString,
            final String idleTerminationMinutes,
            final int templateId,
            final String initScript,
            final String sshConnectTimeoutSeconds,
            final String startTimeoutSeconds,
            final String initScriptTimeoutSeconds){
    	this.compartmentId = compartmentId;
        this.availableDomain = availableDomain;
        this.vcnId = vcnId;
        this.subnetId = subnetId;
        this.imageId = imageId;
        this.shape = shape;
        this.sshPublickey = sshPublickey;
        this.sshPrivatekey = sshPrivatekey;
        this.description = description;
        this.remoteFS = remoteFS;
        this.sshUser = sshUser;
        this.numExecutors = numExecutors;
        this.mode = mode;
        this.labelString = labelString;
        this.idleTerminationMinutes = idleTerminationMinutes;
        this.templateId = templateId;
        this.initScript = initScript;
        this.sshConnectTimeoutSeconds = sshConnectTimeoutSeconds;
        this.startTimeoutSeconds = startTimeoutSeconds;
        this.initScriptTimeoutSeconds = initScriptTimeoutSeconds;
    }



    public String getcompartmentId() {
        return compartmentId;
    }

    public String getAvailableDomain() {
        return availableDomain;
    }

    public String getVcn() {
        return vcnId;
    }

    public String getSubnet() {
        return subnetId;
    }

    public String getImage() {
        return imageId;
    }

    public String getShape() {
        return shape;
    }

    public String getSshPublickey() {
        return sshPublickey;
    }

    public String getSshPrivatekey() {
        return sshPrivatekey;
    }

    public String getDisplayName() {
        return String.valueOf(getDescription());
    }

    public String getDescription() {
        return description;
    }

    public String getRemoteFS() {
        return remoteFS;
    }

    public String getSshUser() {
        return sshUser;
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

    public String getStartTimeoutSeconds() {
        return startTimeoutSeconds;
    }

    public String getSshConnectTimeoutSeconds() {
        return sshConnectTimeoutSeconds;
    }

    private static FormValidationValue<Integer> checkStartTimeoutSeconds(String value) {
        return FormValidationValue.validateNonNegativeInteger(value, (int)TimeUnit.MINUTES.toSeconds(5));
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

    private static FormValidationValue<Integer> checkInitScriptTimeoutSeconds(String value){
        return FormValidationValue.validateNonNegativeInteger(value, 120);
    }


    @Override
    public Descriptor<BaremetalCloudAgentTemplate> getDescriptor() {
        // TODO Auto-generated method stub
        return JenkinsUtil.getDescriptorOrDie(getClass());
    }

    public synchronized void increaseFailureCount(String cause) {
        if (++failureCount >= FAILURE_COUNT_LIMIT) {
            LOGGER.warning("Agent template " + getDisplayName() + " disabled due to error: " + cause);
            disableCause = cause;
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

    static class ConfigMessages {
        static final DynamicResourceBundleHolder holder = DynamicResourceBundleHolder.get(BaremetalCloudAgentTemplate.class, "config");

        public static String sshPrivatekey() {
            return holder.format("sshPrivatekey");
        }
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<BaremetalCloudAgentTemplate> {

        @Override
        public String getHelpFile(String fieldName) {
            String p = super.getHelpFile(fieldName);
            if (p == null)
                p = JenkinsUtil.getJenkinsInstance().getDescriptor(BaremetalCloudAgent.class).getHelpFile(fieldName);

            return p;
        }

        public static int getDefaultNumExecutors() {
            return checkNumExecutors(null).getValue();
        }

        public static int getDefaultSshConnectTimeoutSeconds() {
            return checkSshConnectTimeoutSeconds(null).getValue();
        }

        public static int getDefaultStartTimeoutSeconds() {
            return checkStartTimeoutSeconds(null).getValue();
        }

        public FormValidation doCheckNumExecutors(@QueryParameter String value) {
            return checkNumExecutors(value).getFormValidation();
        }

        public FormValidation doCheckSshConnectTimeoutSeconds(@QueryParameter String value) {
            return checkSshConnectTimeoutSeconds(value).getFormValidation();
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

        private static BaremetalCloudClient getClient(String fingerprint, String apikey, String passphrase, String tenantId, String userId, String regionId){
            BaremetalCloudClientFactory factory = SDKBaremetalCloudClientFactory.INSTANCE;
            return factory.createClient(fingerprint, apikey, passphrase, tenantId, userId, regionId);
        }

        public ListBoxModel doFillCompartmentIdItems(
                @QueryParameter @RelativePath("..") String userId,
                @QueryParameter @RelativePath("..") String fingerprint,
                @QueryParameter @RelativePath("..") String tenantId,
                @QueryParameter @RelativePath("..") String apikey,
                @QueryParameter @RelativePath("..") String passphrase,
                @QueryParameter @RelativePath("..") String regionId)
                        throws IOException, ServletException {
            ListBoxModel model = new ListBoxModel();
            model.add("<Select a compartmentId>", "");

            if (anyRequiredFieldEmpty(userId, fingerprint, tenantId, apikey, regionId)) {
                return model;
            }

            BaremetalCloudClient client = getClient(fingerprint, apikey, passphrase, tenantId, userId, regionId);

            try{
                List<Compartment> compartmentIds = client.getCompartmentsList(tenantId);
                for (Compartment compartmentId : compartmentIds) {
                    model.add(compartmentId.getName() + "(" + compartmentId.getDescription() + ")", compartmentId.getId());
                }
            }catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to get compartment list", e);
            }

            return model;
        }

        public ListBoxModel doFillAvailableDomainItems(
                @QueryParameter @RelativePath("..") String fingerprint,
                @QueryParameter @RelativePath("..") String apikey,
                @QueryParameter @RelativePath("..") String passphrase,
                @QueryParameter @RelativePath("..") String tenantId,
                @QueryParameter @RelativePath("..") String userId,
                @QueryParameter @RelativePath("..") String regionId,
                @QueryParameter String compartmentId) {
            ListBoxModel items = new ListBoxModel();
            if (anyRequiredFieldEmpty(userId, fingerprint, tenantId, apikey, regionId, compartmentId)) {
                return items;
            }

            BaremetalCloudClient client = getClient(fingerprint, apikey, passphrase, tenantId, userId, regionId);

            try {
                ListAvailabilityDomainsResponse listAvailabilityDomainsResponse = client.getAvailabilityDomainsList(compartmentId);
                for (AvailabilityDomain goal : listAvailabilityDomainsResponse.getItems()) {
                    items.add(goal.getName());
                }
                return items;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to get AvailableDomain list", e);
                return items;
            }
        }

        public ListBoxModel doFillImageIdItems(
                @QueryParameter @RelativePath("..") String userId,
                @QueryParameter @RelativePath("..") String fingerprint,
                @QueryParameter @RelativePath("..") String tenantId,
                @QueryParameter @RelativePath("..") String apikey,
                @QueryParameter @RelativePath("..") String passphrase,
                @QueryParameter @RelativePath("..") String regionId,
                @QueryParameter String compartmentId) throws IOException, ServletException {
            ListBoxModel model = new ListBoxModel();
            model.add("<Select an Image>", "");
            if (anyRequiredFieldEmpty(userId, fingerprint, tenantId, apikey, regionId, compartmentId)) {
                return model;
            }

            BaremetalCloudClient client = getClient(fingerprint, apikey, passphrase, tenantId, userId, regionId);

            try {
                List<Image> list = client.getImagesList(compartmentId);
                for (Image imageId : list) {
                    model.add(imageId.getDisplayName(), imageId.getId());
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to get images list", e);
            }
            return model;
        }

        public ListBoxModel doFillShapeItems(
                @QueryParameter @RelativePath("..") String userId,
                @QueryParameter @RelativePath("..") String fingerprint,
                @QueryParameter @RelativePath("..") String tenantId,
                @QueryParameter @RelativePath("..") String apikey,
                @QueryParameter @RelativePath("..") String passphrase,
                @QueryParameter @RelativePath("..") String regionId,
                @QueryParameter String compartmentId,
                @QueryParameter String availableDomain,
                @QueryParameter String imageId)
                        throws IOException, ServletException {
            ListBoxModel model = new ListBoxModel();
            model.add("<First select 'Availablity Domain' and 'Image' above>", "");

            if (anyRequiredFieldEmpty(userId, fingerprint, tenantId, apikey, regionId, compartmentId, imageId)) {
                return model;
            }

            BaremetalCloudClient client = getClient(fingerprint, apikey, passphrase, tenantId, userId, regionId);

            try {
                List<Shape> list = client.getShapesList(compartmentId, availableDomain, imageId);
                for (Shape shape : list) {
                    model.add(shape.getShape(), shape.getShape());
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to get shapes list", e);
            }
            return model;
        }

        public ListBoxModel doFillVcnIdItems(
                @QueryParameter @RelativePath("..") String userId,
                @QueryParameter @RelativePath("..") String fingerprint,
                @QueryParameter @RelativePath("..") String tenantId,
                @QueryParameter @RelativePath("..") String apikey,
                @QueryParameter @RelativePath("..") String passphrase,
                @QueryParameter @RelativePath("..") String regionId,
                @QueryParameter String compartmentId)
                        throws IOException, ServletException {
            ListBoxModel model = new ListBoxModel();
            model.add("<Select a Virtual Cloud Network>", "");

            if (anyRequiredFieldEmpty(userId, fingerprint, tenantId, apikey, regionId, compartmentId)) {
                return model;
            }

            BaremetalCloudClient client = getClient(fingerprint, apikey, passphrase, tenantId, userId, regionId);

            try {
                List<Vcn> list = client.getVcnList(compartmentId);
                for (Vcn vcnId : list) {
                    model.add(vcnId.getDisplayName(), vcnId.getId());
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to get VCN list", e);
            }
            return model;
        }

        public ListBoxModel doFillSubnetIdItems(
                @QueryParameter @RelativePath("..") String userId,
                @QueryParameter @RelativePath("..") String fingerprint,
                @QueryParameter @RelativePath("..") String tenantId,
                @QueryParameter @RelativePath("..") String apikey,
                @QueryParameter @RelativePath("..") String passphrase,
                @QueryParameter @RelativePath("..") String regionId,
                @QueryParameter String compartmentId,
                @QueryParameter String vcnId)
                        throws IOException, ServletException {
            ListBoxModel model = new ListBoxModel();
            model.add("<First select 'Availablity Domain' and 'Virtual Cloud Network' above>", "");

            if (anyRequiredFieldEmpty(userId, fingerprint, tenantId, apikey, regionId, compartmentId, vcnId)) {
                return model;
            }

            BaremetalCloudClient client = getClient(fingerprint, apikey, passphrase, tenantId, userId, regionId);

            try {
                List<Subnet> list = client.getSubNetList(compartmentId, vcnId);
                for (Subnet subnetId : list) {
                    model.add(subnetId.getDisplayName(), subnetId.getId());
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to get subnet list", e);
            }
            return model;
        }

        PEMEncodable decodePEM(String pem) throws UnrecoverableKeyException, IOException {
            return PEMEncodable.decode(pem);
        }

        private FormValidationValue<RSAPublicKey> checkPrivateKey(String value, boolean withContext) {
            FormValidation fv = JenkinsUtil.validateRequired(value);
            if (fv.kind != FormValidation.Kind.OK) {
                return FormValidationValue.error(withContext ? BaremetalCloud.DescriptorImpl.withContext(fv, ConfigMessages.sshPrivatekey()) : fv);
            }

            PEMEncodable encodable;
            try {
                encodable = decodePEM(value);
            } catch (NullPointerException e) {
                // Workaround https://issues.jenkins-ci.org/browse/JENKINS-41978
                LOGGER.log(Level.FINE, "Failed to parse private key", e);
                return FormValidationValue.error(Messages.BaremetalCloudAgentTemplate_privateKey_invalid());
            } catch (UnrecoverableKeyException e) {
                LOGGER.log(Level.FINE, "Failed to parse private key", e);
                return FormValidationValue.error(Messages.BaremetalCloudAgentTemplate_privateKey_unable(e.toString()));
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Failed to parse private key", e);
                return FormValidationValue.error(Messages.BaremetalCloudAgentTemplate_privateKey_unable(e.getMessage()));
            }

            KeyPair keyPair = encodable.toKeyPair();
            if (keyPair == null) {
                LOGGER.log(Level.FINE, "toKeyPair returned null for {0}", encodable.getRawObject());
                return FormValidationValue.error(Messages.BaremetalCloudAgentTemplate_privateKey_invalid());
            }

            PublicKey publicKey = keyPair.getPublic();
            if (!(publicKey instanceof RSAPublicKey)) {
                LOGGER.log(Level.FINE, "getPublic returned non-RSAPublicKey {0} for {1}",
                        new Object[] { publicKey, encodable.getRawObject() });
                return FormValidationValue.error(Messages.BaremetalCloudAgentTemplate_privateKey_invalid());
            }

            RSAPublicKey rsaPublicKey = (RSAPublicKey)publicKey;
            return FormValidationValue.ok(rsaPublicKey);
        }

        public FormValidation doCheckSshPrivatekey(@QueryParameter String value) {
            return checkPrivateKey(value, false).getFormValidation();
        }

        private FormValidation newUnableToVerifySshKeyPairFormValidation(FormValidation fv) {
            return FormValidation.error(Messages.BaremetalCloudAgentTemplate_verifySshKeyPair_unable(JenkinsUtil.unescape(fv.getMessage())));
        }

        public FormValidation doVerifySshKeyPair(
                @QueryParameter String sshPublickey,
                @QueryParameter String sshPrivatekey) {
            String sshPrivatekeyValue = sshPublickey.trim();
            if (sshPrivatekeyValue.trim().isEmpty()) {
                return FormValidation.error(Messages.BaremetalCloudAgentTemplate_verifySshKeyPair_publicKeyEmpty());
            }

            FormValidationValue<RSAPublicKey> privateKeyValid = checkPrivateKey(sshPrivatekey, true);
            if (!privateKeyValid.isOk()) {
                return newUnableToVerifySshKeyPairFormValidation(privateKeyValid.getFormValidation());
            }

            String sshString = SshKeyUtil.toSshString(privateKeyValid.getValue());
            int lengh = sshString.length();
            if (!sshPrivatekeyValue.startsWith(sshString) || sshPrivatekeyValue.length() > lengh && sshPrivatekeyValue.charAt(lengh) != ' ') {
                return FormValidation.error(Messages.BaremetalCloudAgentTemplate_verifySshKeyPair_mismatch());
            }

            return FormValidation.ok(Messages.BaremetalCloudAgentTemplate_verifySshKeyPair_success());
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
    }
}
