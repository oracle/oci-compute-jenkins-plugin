/**
 * Jenkins OCI Plugin
 *
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
package org.jenkinsci.plugins.oci.credentials;

import java.io.IOException;
import java.io.StringReader;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.logging.Logger;

import org.jenkinsci.plugins.oci.credentials.Messages;
import org.jenkinsci.plugins.oci.cloud.OCIApi;
import org.jenkinsci.plugins.oci.cloud.OCICloud;
import org.jenkinsci.plugins.oci.utils.KeyUtils;
import org.jenkinsci.plugins.oci.utils.StringPrivateKeySupplier;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.identity.requests.GetUserRequest;

import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;

/**
 * See:
 * https://github.com/jenkinsci/credentials-plugin/blob/master/docs/implementation.adoc
 *
 * @author Filip Krestan
 */
public class OCICredentialsImpl extends BaseStandardCredentials implements OCICredentials {
  private static final Logger LOGGER = Logger.getLogger(OCICloud.class.getName());

  private final String regionId;
  private final String tentancyOCID;
  private final String userOCID;
  private final Secret privateKey;
  private String privateKeyFingerprint;

  @DataBoundConstructor
  public OCICredentialsImpl(CredentialsScope scope, String id, String description, String regionId, String tentancyOCID, String userOCID, String privateKey) {
    super(scope, id, description);
    this.regionId = regionId;
    this.tentancyOCID = tentancyOCID;
    this.userOCID = userOCID;
    this.privateKey = Secret.fromString(privateKey);

    try {
      this.privateKeyFingerprint = KeyUtils.getDERKeyFingerprint(new StringReader(privateKey));
    } catch (IOException | NoSuchAlgorithmException e) {
      e.printStackTrace();
      this.privateKeyFingerprint = null;
    }
  }

  @Extension
  public static class DescriptorImpl extends BaseStandardCredentials.BaseStandardCredentialsDescriptor {

    @Override
    public String getDisplayName() {
      return Messages.Credentials_Diaplay_Name();
    }

    public ListBoxModel doFillRegionIdItems() {
      ListBoxModel items = new ListBoxModel();
      List<Region> regions = OCIApi.getRegions();

      for (Region region : regions) {
        items.add(region.name(), region.getRegionId());
      }

      return items;
    }

    public FormValidation doVerifyConfiguration(
        @QueryParameter String regionId, @QueryParameter String tentancyOCID, @QueryParameter String userOCID, @QueryParameter String privateKey) {
      FormValidation valid = null;

      try {
        String privateKeyFingerprint = KeyUtils.getDERKeyFingerprint(new StringReader(privateKey));
        LOGGER.info("Extracted key fingerprint: " + privateKeyFingerprint);

        // Test connection
        StringPrivateKeySupplier privateKeySupplier = new StringPrivateKeySupplier(privateKey);
        SimpleAuthenticationDetailsProvider provider =
            SimpleAuthenticationDetailsProvider.builder()
                .fingerprint(privateKeyFingerprint)
                .privateKeySupplier(privateKeySupplier)
                .tenantId(tentancyOCID)
                .userId(userOCID)
                .build();

        IdentityClient identity = new IdentityClient(provider);

        identity.setRegion(regionId);
        String userOCIDResponse = identity.getUser(GetUserRequest.builder().userId(userOCID).build()).getUser().getId();
        identity.close();
        LOGGER.info("Credentials verified: " + userOCIDResponse);
      } catch (IOException | NoSuchAlgorithmException e) {
        return FormValidation.error(Messages.Credentials_Validation_Key_ERROR());
      }

      return FormValidation.ok(Messages.Credentials_Validation_OK());
    }
  }

  @Override
  public String getTentancyOCID() {
    return tentancyOCID;
  }

  @Override
  public String getUserOCID() {
    return userOCID;
  }

  @Override
  public String getPrivateKeyFingerprint() {
    return privateKeyFingerprint;
  }

  @Override
  public Secret getPrivateKey() throws IOException, InterruptedException {
    return privateKey;
  }

  @Override
  public String getRegionId() {
    return regionId;
  }
}
