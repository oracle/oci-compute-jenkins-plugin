package com.oracle.cloud.baremetal.jenkins.client;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider.InstancePrincipalsAuthenticationDetailsProviderBuilder;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.cloud.baremetal.jenkins.credentials.BaremetalCloudCredentials;
import hudson.security.ACL;
import java.io.IOException;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;

public class SDKBaremetalCloudClientFactory implements BaremetalCloudClientFactory {
    public static final BaremetalCloudClientFactory INSTANCE = new SDKBaremetalCloudClientFactory();
    private static final Logger LOGGER = Logger.getLogger(SDKBaremetalCloudClient.class.getName());

    private SDKBaremetalCloudClientFactory() {}

    @Override
    public BaremetalCloudClient createClient (String credentialsId, int maxAsyncThreads) {
        BaremetalCloudCredentials credentials = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(BaremetalCloudCredentials.class, Jenkins.getInstance(), ACL.SYSTEM, Collections.<DomainRequirement>emptyList()),
                CredentialsMatchers.withId(credentialsId));
        if (credentials != null) {
            if (!credentials.isInstancePrincipals()) {
            SimpleAuthenticationDetailsProvider provider = SimpleAuthenticationDetailsProvider.builder()
                    .fingerprint(credentials.getFingerprint())
                    .passPhrase(credentials.getPassphrase())
                    .privateKeySupplier(() ->  new ByteArrayInputStream(credentials.getApikey().getBytes(StandardCharsets.UTF_8)))
                    .tenantId(credentials.getTenantId())
                    .userId(credentials.getUserId())
                    .build();
                return new SDKBaremetalCloudClient(provider, credentials.getRegionId(), maxAsyncThreads);
            } else {
                try {
                    InstancePrincipalsAuthenticationDetailsProvider provider = InstancePrincipalsAuthenticationDetailsProvider.builder().build(); 
                    return new SDKBaremetalCloudClient(provider, credentials.getRegionId(), maxAsyncThreads, credentials.getTenantId());
                } catch (Exception e){
                    LOGGER.log(Level.INFO,"Failed to use Calling Services from an Instance");
                } 
            }
        }
        LOGGER.log(Level.INFO,"Failed to create client!");
        return null;
    }
}
