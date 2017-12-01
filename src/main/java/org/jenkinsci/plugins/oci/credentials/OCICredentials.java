/**
 * Jenkins OCI Plugin
 *
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
package org.jenkinsci.plugins.oci.credentials;

import java.io.IOException;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.util.Secret;

/**
 * See:
 * https://github.com/jenkinsci/credentials-plugin/blob/master/docs/implementation.adoc
 *
 * @author Filip Krestan
 */
public interface OCICredentials extends StandardCredentials {
  public String getTentancyOCID();

  public String getUserOCID();

  public String getPrivateKeyFingerprint();

  public Secret getPrivateKey() throws IOException, InterruptedException;

  public String getRegionId();
}
