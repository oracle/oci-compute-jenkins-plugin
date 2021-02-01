package com.oracle.cloud.baremetal.jenkins.client;

public interface BaremetalCloudClientFactory {
    BaremetalCloudClient createClient(String credentialsId, int maxAsyncThreads);
}
