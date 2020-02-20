package com.oracle.cloud.baremetal.jenkins.client;

import com.oracle.cloud.baremetal.jenkins.credentials.BaremetalCloudCredentials;

public interface BaremetalCloudClientFactory {
    BaremetalCloudClient createClient(String credentialsId, int maxAsyncThreads);
}
