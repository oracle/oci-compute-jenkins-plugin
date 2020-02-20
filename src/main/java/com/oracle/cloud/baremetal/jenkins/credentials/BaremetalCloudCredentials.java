package com.oracle.cloud.baremetal.jenkins.credentials;

import com.cloudbees.plugins.credentials.common.StandardCredentials;

public interface BaremetalCloudCredentials extends StandardCredentials {

    public String getFingerprint();

    public String getApikey();

    public String getPassphrase();

    public String getTenantId();

    public String getUserId();

    public String getRegionId();

    public boolean isInstancePrincipals();

}
