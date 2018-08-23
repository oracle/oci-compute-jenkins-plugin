package com.oracle.cloud.baremetal.jenkins.client;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;

public class SDKBaremetalCloudClientFactory implements BaremetalCloudClientFactory {
    public static final BaremetalCloudClientFactory INSTANCE = new SDKBaremetalCloudClientFactory();

    private SDKBaremetalCloudClientFactory() {}

    @Override
    public BaremetalCloudClient createClient(String fingerprint, String apikey, String passphrase, String tenantId, String userId, String regionId, int maxAsyncThreads) {
        SimpleAuthenticationDetailsProvider provider =
                SimpleAuthenticationDetailsProvider.builder()
                .fingerprint(fingerprint)
                .passPhrase(passphrase)
                .privateKeySupplier(() ->  new ByteArrayInputStream(apikey.getBytes(StandardCharsets.UTF_8)))
                .tenantId(tenantId)
                .userId(userId)
                .build();
        return new SDKBaremetalCloudClient(provider, regionId, maxAsyncThreads);
    }
}
