/**
 * Jenkins OCI Plugin
 *
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
package org.jenkinsci.plugins.oci.cloud;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.oci.cloud.Messages;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHAuthenticator;
import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.oracle.bmc.core.model.Image;
import com.oracle.bmc.core.model.Shape;
import com.oracle.bmc.core.model.Subnet;
import com.oracle.bmc.core.model.Vcn;
import com.oracle.bmc.identity.model.AvailabilityDomain;
import com.oracle.bmc.identity.model.Compartment;

import hudson.Extension;
import hudson.RelativePath;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Node;
import hudson.model.Node.Mode;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

public class OCISlaveTemplate extends AbstractDescribableImpl<OCISlaveTemplate> {
  private static final Logger LOGGER = Logger.getLogger(OCISlaveTemplate.class.getName());

  private final String name;
  private final int numExecutors;

  private final String labelString;
  private final Node.Mode mode;
  private final String sshCredentialsId;

  private final String compartmentOCID;
  private final String availabilityDomain;
  private final String shape;
  private final String imageOCID;
  private final String vcnOCID;
  private final String subnetOCID;

  private transient SSHUserPrivateKey sshCredentials;

  @DataBoundConstructor
  public OCISlaveTemplate(
      String name,
      String labelString,
      Mode mode,
      String sshCredentialsId,
      String compartmentOCID,
      String availabilityDomain,
      String shape,
      String imageOCID,
      String vcnOCID,
      String subnetOCID) {
    super();
    this.numExecutors = 1; // TODO(fkrestan): support multiple executors per slave
    this.name = name;
    this.labelString = labelString;
    this.mode = mode;
    this.sshCredentialsId = sshCredentialsId;

    this.compartmentOCID = compartmentOCID;
    this.availabilityDomain = availabilityDomain;
    this.shape = shape;
    this.imageOCID = imageOCID;
    this.vcnOCID = vcnOCID;
    this.subnetOCID = subnetOCID;

    readResolve();
  }

  /**
   * This method is called on deserialization of OCISlaveTemplate from
   * config.xml
   *
   * @return self
   */
  protected Object readResolve() {
    this.sshCredentials =
        CredentialsMatchers.firstOrNull(
            CredentialsProvider.lookupCredentials(SSHUserPrivateKey.class, Jenkins.getInstance(), ACL.SYSTEM, Collections.<DomainRequirement>emptyList()),
            CredentialsMatchers.withId(this.sshCredentialsId));

    return this;
  }

  @Extension
  public static final class DescriptorImpl extends Descriptor<OCISlaveTemplate> {
    // TODO(fkrestan): Implement verification logic (do not allow empty "select" items)

    @Override
    public String getDisplayName() {
      return Messages.Template_Diaplay_Name();
    }

    public ListBoxModel doFillSshCredentialsIdItems(@AncestorInPath Item context, @QueryParameter String sshCredentialsId) {
      StandardListBoxModel result = new StandardListBoxModel();

      if (context == null) {
        if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
          return result.includeCurrentValue(sshCredentialsId);
        }
      } else {
        if (!context.hasPermission(Item.EXTENDED_READ) && !context.hasPermission(CredentialsProvider.USE_ITEM)) {
          return result.includeCurrentValue(sshCredentialsId);
        }
      }

      List<DomainRequirement> domainRequirements = new ArrayList<DomainRequirement>();

      return result.includeMatchingAs(ACL.SYSTEM, context, SSHUserPrivateKey.class, domainRequirements, SSHAuthenticator.matcher());
    }

    public ListBoxModel doFillCompartmentOCIDItems(@QueryParameter @RelativePath("..") String credentialsId) {
      ListBoxModel items = new ListBoxModel();

      try {
        OCIApi api = new OCIApi(credentialsId);

        for (Compartment comp : api.getCompartments()) {
          items.add(comp.getName(), comp.getId());
        }
      } catch (IOException | InterruptedException e) {
        LOGGER.log(Level.FINE, "Could not initialize API while filling compartments to form: {}", e.getMessage());
      } catch (Exception e) {
        LOGGER.log(Level.FINE, "Could not get compartments. You may not have a valid rights for a compartment");
      }

      return items;
    }

    public ListBoxModel doFillShapeItems(@QueryParameter @RelativePath("..") String credentialsId, @QueryParameter String compartmentOCID) {
      ListBoxModel items = new ListBoxModel();

      try {
        OCIApi api = new OCIApi(credentialsId);

        for (Shape shape : api.getShapes(compartmentOCID)) {
          items.add(shape.getShape());
        }
      } catch (IOException | InterruptedException e) {
        LOGGER.log(Level.FINE, "Could not initialize API while filling shapes to form: {}", e.getMessage());
      } catch (Exception e) {
        LOGGER.log(Level.FINE, "Could not get shapes. You may not have a valid rights for a compartment");
      }

      return items;
    }

    public ListBoxModel doFillImageOCIDItems(@QueryParameter @RelativePath("..") String credentialsId, @QueryParameter String compartmentOCID) {
      ListBoxModel items = new ListBoxModel();

      try {
        OCIApi api = new OCIApi(credentialsId);

        for (Image image : api.getImages(compartmentOCID)) {
          items.add(image.getDisplayName(), image.getId());
        }
      } catch (IOException | InterruptedException e) {
        LOGGER.log(Level.FINE, "Could not initialize API while filling images to form: {}", e.getMessage());
      } catch (Exception e) {
        LOGGER.log(Level.FINE, "Could not get images. You may not have a valid rights for a compartment");
      }

      return items;
    }

    public ListBoxModel doFillAvailabilityDomainItems(@QueryParameter @RelativePath("..") String credentialsId, @QueryParameter String compartmentOCID) {
      ListBoxModel items = new ListBoxModel();

      try {
        OCIApi api = new OCIApi(credentialsId);

        for (AvailabilityDomain ad : api.getAvailabilityDomains(compartmentOCID)) {
          items.add(ad.getName());
        }
      } catch (IOException | InterruptedException e) {
        LOGGER.log(Level.FINE, "Could not initialize API while filling ADs to form: {}", e.getMessage());
      } catch (Exception e) {
        LOGGER.log(Level.FINE, "Could not get ADs. You may not have a valid rights for a compartment");
      }

      return items;
    }

    public ListBoxModel doFillVcnOCIDItems(@QueryParameter @RelativePath("..") String credentialsId, @QueryParameter String compartmentOCID) {
      ListBoxModel items = new ListBoxModel();

      try {
        OCIApi api = new OCIApi(credentialsId);

        for (Vcn v : api.getVcns(compartmentOCID)) {
          items.add(v.getDisplayName(), v.getId());
        }
      } catch (IOException | InterruptedException e) {
        LOGGER.log(Level.FINE, "Could not initialize API while filling VCNs to form: {}", e.getMessage());
      } catch (Exception e) {
        LOGGER.log(Level.FINE, "Could not get VCNs. You may not have a valid rights for a compartment");
      }

      return items;
    }

    public ListBoxModel doFillSubnetOCIDItems(
        @QueryParameter @RelativePath("..") String credentialsId,
        @QueryParameter String compartmentOCID,
        @QueryParameter String availabilityDomain,
        @QueryParameter String vcnOCID) {
      ListBoxModel items = new ListBoxModel();

      try {
        OCIApi api = new OCIApi(credentialsId);

        for (Subnet subnet : api.getSubnets(compartmentOCID, vcnOCID)) {
          if (subnet.getAvailabilityDomain().equals(availabilityDomain)) {
            items.add(subnet.getDisplayName(), subnet.getId());
          }
        }
      } catch (IOException | InterruptedException e) {
        LOGGER.log(Level.FINE, "Could not initialize API while filling subnets to form: {}", e.getMessage());
      } catch (Exception e) {
        LOGGER.log(Level.FINE, "Could not get subnets. You may not have a valid rights for a compartment");
      }

      return items;
    }
  }

  public SSHUserPrivateKey getSshCredentials() {
    return sshCredentials;
  }

  public String getName() {
    return name;
  }

  public String getSshCredentialsId() {
    return sshCredentialsId;
  }

  public int getNumExecutors() {
    return numExecutors;
  }

  public Node.Mode getMode() {
    return mode;
  }

  public String getCompartmentOCID() {
    return compartmentOCID;
  }

  public String getAvailabilityDomain() {
    return availabilityDomain;
  }

  public String getShape() {
    return shape;
  }

  public String getImageOCID() {
    return imageOCID;
  }

  public String getVcnOCID() {
    return vcnOCID;
  }

  public String getSubnetOCID() {
    return subnetOCID;
  }

  public String getLabelString() {
    return labelString;
  }
}
