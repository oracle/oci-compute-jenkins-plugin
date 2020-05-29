package com.oracle.cloud.baremetal.jenkins.credentials;

import java.util.logging.Logger;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.model.BmcException;

import com.oracle.cloud.baremetal.jenkins.client.BaremetalCloudClient;
import com.oracle.cloud.baremetal.jenkins.client.BaremetalCloudClientFactory;
import com.oracle.cloud.baremetal.jenkins.client.SDKBaremetalCloudClient;
import com.oracle.cloud.baremetal.jenkins.client.SDKBaremetalCloudClientFactory;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.Secret;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public final class BaremetalCloudCredentialsImpl extends BaseStandardCredentials implements BaremetalCloudCredentials {
    private static final Logger LOGGER = Logger.getLogger(BaremetalCloudCredentials.class.getName());

    private final String fingerprint;
    private final String apikey;
    private final String passphrase;
    private final String tenantId;
    private final String userId;
    private final String regionId;
    private final boolean instancePrincipals;

  @DataBoundConstructor
    public BaremetalCloudCredentialsImpl(CredentialsScope scope,
            String id,
            String description,
            String fingerprint,
            String apikey,
            String passphrase,
            String tenantId,
            String userId,
            String regionId,
            boolean instancePrincipals) {
        super(scope, id, description);
        this.fingerprint = fingerprint;
        this.apikey = getEncryptedValue(apikey);
        this.passphrase = getEncryptedValue(passphrase);
        this.tenantId = tenantId;
        this.userId = userId;
        this.regionId = regionId;
        this.instancePrincipals = instancePrincipals;
    }

    @Override
    public String getFingerprint() {
        return fingerprint;
    }

    @Override
    public String getApikey() {
        return getPlainText(apikey);
    }

    @Override
    public String getPassphrase() {
        return getPlainText(passphrase);
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getRegionId() {
        return regionId;
    }

    @Override
    public boolean isInstancePrincipals() {
        return instancePrincipals;
    }

    protected String getEncryptedValue(String str) {
        return Secret.fromString(str).getEncryptedValue();
    }

    protected String getPlainText(String str) {
        if (str != null) {
            Secret secret = Secret.decrypt(str);
            if (secret != null) {
                return secret.getPlainText();
            }
        }
        return null;
    }

    @Extension
    public static class DescriptorImpl extends BaseStandardCredentials.BaseStandardCredentialsDescriptor {

        @Override
        public String getDisplayName() {
            return "Oracle Cloud Infrastructure Credentials";
        }

        public FormValidation doTestConnection(
                    @QueryParameter String fingerprint,
                    @QueryParameter String apikey,
                    @QueryParameter String passphrase,
                    @QueryParameter String tenantId,
                    @QueryParameter String userId,
                    @QueryParameter String regionId,
                    @QueryParameter boolean instancePrincipals) {
            if (!instancePrincipals) {
                SimpleAuthenticationDetailsProvider provider = SimpleAuthenticationDetailsProvider.builder()
                    .fingerprint(fingerprint)
                    .passPhrase(passphrase)
                    .privateKeySupplier(() ->  new ByteArrayInputStream(apikey.getBytes(StandardCharsets.UTF_8)))
                    .tenantId(tenantId)
                    .userId(userId)
                    .build();
                BaremetalCloudClient client = new SDKBaremetalCloudClient(provider, regionId, 50);
                try{
                    client.authenticate();
                    return FormValidation.ok(com.oracle.cloud.baremetal.jenkins.Messages.BaremetalCloud_testConnection_success());
                }catch(BmcException e){
                    LOGGER.log(Level.INFO, "Failed to connect to Oracle Cloud Infrastructure, Please verify all the credential informations enterred", e);
                    return FormValidation.error(com.oracle.cloud.baremetal.jenkins.Messages.BaremetalCloud_testConnection_unauthorized());
                }
            } else {
                InstancePrincipalsAuthenticationDetailsProvider provider = InstancePrincipalsAuthenticationDetailsProvider.builder().build(); 
                BaremetalCloudClient client = new SDKBaremetalCloudClient(provider, regionId, 50, tenantId);
                try{
                    client.authenticate();
                    return FormValidation.ok(com.oracle.cloud.baremetal.jenkins.Messages.BaremetalCloud_testConnection_success());
                }catch(BmcException e){
                    LOGGER.log(Level.INFO, "Failed to connect to Oracle Cloud Infrastructure, Please verify all the credential informations enterred", e);
                    return FormValidation.error(com.oracle.cloud.baremetal.jenkins.Messages.BaremetalCloud_testConnection_unauthorized());
                }
            }
        }
    }
}
