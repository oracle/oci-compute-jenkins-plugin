package com.oracle.cloud.baremetal.jenkins.credentials;

import com.cloudbees.plugins.credentials.common.StandardCredentials;

public interface BaremetalCloudCredentials extends StandardCredentials {

    String getFingerprint();

    String getApikey();

    String getPassphrase();

    String getTenantId();

    String getUserId();

    String getRegionId();

    boolean isInstancePrincipals();

}
