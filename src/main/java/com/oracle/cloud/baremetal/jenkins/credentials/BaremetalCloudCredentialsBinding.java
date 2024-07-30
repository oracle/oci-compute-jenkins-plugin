package com.oracle.cloud.baremetal.jenkins.credentials;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.credentialsbinding.BindingDescriptor;
import org.jenkinsci.plugins.credentialsbinding.MultiBinding;
import org.kohsuke.stapler.DataBoundConstructor;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;

public final class BaremetalCloudCredentialsBinding extends MultiBinding<BaremetalCloudCredentials> {
    public static final String ENV_PASSPHRASE = "OCI_PASSPHRASE";
    public static final String ENV_USER_ID = "OCI_USER_ID";
    public static final String ENV_FINGERPRINT = "OCI_FINGERPRINT";
    public static final String ENV_TENANT_ID = "OCI_TENANT_ID";
    public static final String ENV_REGION_ID = "OCI_REGION_ID";
    public static final String ENV_API_KEY = "OCI_API_KEY";
    public static final String ENV_CONFIG_FILE = "OCI_CONFIG_FILE";

    @DataBoundConstructor
    public BaremetalCloudCredentialsBinding(String credentialsId) {
        super(credentialsId);
    }

    @Override
    protected Class<BaremetalCloudCredentials> type() {
        return BaremetalCloudCredentials.class;
    }

    @Override
    public MultiEnvironment bind(
            @NonNull Run<?, ?> build,
            @NonNull FilePath workspace,
            @Nullable Launcher launcher,
            @NonNull TaskListener listener)
            throws IOException, InterruptedException {
        final BaremetalCloudCredentials credentials = getCredentials(build);
        final OciConfigWriter writer = new OciConfigWriter(
                credentials.getUserId(),
                credentials.getFingerprint(),
                credentials.getTenantId(),
                credentials.getRegionId(),
                credentials.getApikey());
        Map<String, String> secretValues = new HashMap<>();
        // Probably unsafe, but may be required. Passphrase is deprecated in OCI config, so will give it to user
        // if needed.
        secretValues.put(ENV_PASSPHRASE, credentials.getPassphrase());

        return new MultiEnvironment(secretValues, workspace.act(writer.asCallable()));
    }

    @Symbol("ociCredentials")
    @Extension public static class DescriptorImpl extends BindingDescriptor<BaremetalCloudCredentials> {
        @Override protected Class<BaremetalCloudCredentials> type() {
            return BaremetalCloudCredentials.class;
        }

        @Override
        public String getDisplayName() {
            return "OCI Credentials";
        }

        @Override public boolean requiresWorkspace() {
            return true;
        }
    }

}
