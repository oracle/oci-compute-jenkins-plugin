package com.oracle.cloud.baremetal.jenkins.client;

public interface BaremetalCloudClientFactory {
    BaremetalCloudClient createClient(String fingerprint, String apikey, String passphrase, String tenantId, String userId, String regionId, int maxAsyncThreads);
}
