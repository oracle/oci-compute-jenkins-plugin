package com.oracle.cloud.baremetal.jenkins.client;

import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.cloud.baremetal.jenkins.BaremetalCloud;
import com.oracle.cloud.baremetal.jenkins.credentials.BaremetalCloudCredentials;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SDKBaremetalCloudClientFactory implements BaremetalCloudClientFactory {
    public static final BaremetalCloudClientFactory INSTANCE = new SDKBaremetalCloudClientFactory();
    private static final Logger LOGGER = Logger.getLogger(SDKBaremetalCloudClient.class.getName());

    private SDKBaremetalCloudClientFactory() {}

    @Override
    public BaremetalCloudClient createClient (String credentialsId, int maxAsyncThreads) {
        BaremetalCloudCredentials credentials = (BaremetalCloudCredentials) BaremetalCloud.matchCredentials(BaremetalCloudCredentials.class,credentialsId);
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
