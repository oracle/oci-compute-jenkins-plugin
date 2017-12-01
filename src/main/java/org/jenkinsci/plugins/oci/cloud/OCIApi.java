/**
 * Jenkins OCI Plugin
 *
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
package org.jenkinsci.plugins.oci.cloud;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.oci.cloud.exceptions.InvalidSshCredentialsException;
import org.jenkinsci.plugins.oci.credentials.OCICredentials;
import org.jenkinsci.plugins.oci.utils.KeyUtils;
import org.jenkinsci.plugins.oci.utils.NotImplementedException;
import org.jenkinsci.plugins.oci.utils.StringPrivateKeySupplier;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.core.ComputeClient;
import com.oracle.bmc.core.ComputeWaiters;
import com.oracle.bmc.core.VirtualNetworkClient;
import com.oracle.bmc.core.model.Image;
import com.oracle.bmc.core.model.Instance;
import com.oracle.bmc.core.model.LaunchInstanceDetails;
import com.oracle.bmc.core.model.Shape;
import com.oracle.bmc.core.model.Subnet;
import com.oracle.bmc.core.model.Vcn;
import com.oracle.bmc.core.model.Vnic;
import com.oracle.bmc.core.model.VnicAttachment;
import com.oracle.bmc.core.requests.GetInstanceRequest;
import com.oracle.bmc.core.requests.GetVnicRequest;
import com.oracle.bmc.core.requests.LaunchInstanceRequest;
import com.oracle.bmc.core.requests.ListImagesRequest;
import com.oracle.bmc.core.requests.ListShapesRequest;
import com.oracle.bmc.core.requests.ListSubnetsRequest;
import com.oracle.bmc.core.requests.ListVcnsRequest;
import com.oracle.bmc.core.requests.ListVnicAttachmentsRequest;
import com.oracle.bmc.core.requests.TerminateInstanceRequest;
import com.oracle.bmc.core.responses.GetInstanceResponse;
import com.oracle.bmc.core.responses.LaunchInstanceResponse;
import com.oracle.bmc.core.responses.ListImagesResponse;
import com.oracle.bmc.core.responses.ListShapesResponse;
import com.oracle.bmc.core.responses.ListSubnetsResponse;
import com.oracle.bmc.core.responses.ListVcnsResponse;
import com.oracle.bmc.core.responses.ListVnicAttachmentsResponse;
import com.oracle.bmc.identity.Identity;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.identity.model.AvailabilityDomain;
import com.oracle.bmc.identity.model.Compartment;
import com.oracle.bmc.identity.requests.ListAvailabilityDomainsRequest;
import com.oracle.bmc.identity.requests.ListCompartmentsRequest;
import com.oracle.bmc.identity.responses.ListAvailabilityDomainsResponse;
import com.oracle.bmc.identity.responses.ListCompartmentsResponse;

import hudson.security.ACL;
import hudson.util.Secret;
import jenkins.model.Jenkins;

public class OCIApi {
  private static final Logger LOGGER = Logger.getLogger(OCIApi.class.getName());

  private final String credentialsId;
  private final OCICredentials credentials;
  private SimpleAuthenticationDetailsProvider provider;

  public OCIApi(String credentialsId) throws IOException, InterruptedException {
    this.credentialsId = credentialsId;

    this.credentials =
        CredentialsMatchers.firstOrNull(
            CredentialsProvider.lookupCredentials(OCICredentials.class, Jenkins.getInstance(), ACL.SYSTEM, Collections.<DomainRequirement>emptyList()),
            CredentialsMatchers.withId(credentialsId));

    if (credentials == null) {
      throw new IOException("There are no OCI credentials");
    }
    StringPrivateKeySupplier privateKeySupplier = new StringPrivateKeySupplier(credentials.getPrivateKey().getPlainText());

    this.provider =
        SimpleAuthenticationDetailsProvider.builder()
            .fingerprint(credentials.getPrivateKeyFingerprint())
            .privateKeySupplier(privateKeySupplier)
            .tenantId(credentials.getTentancyOCID())
            .userId(credentials.getUserOCID())
            .build();
  }

  public Instance startSlave(OCISlaveTemplate template, String name_in_oci, int timeout) throws Exception {
    LOGGER.log(Level.INFO, "OCI Provisioning: Begin");

    ComputeClient computeClient = new ComputeClient(this.provider);
    computeClient.setRegion(this.credentials.getRegionId());

    Map<String, String> metadata = new HashMap<>();

    try {
      // Cloud-init delivered ssh authorized_keys
      String privateSshKey = template.getSshCredentials().getPrivateKeys().get(0);
      String privateSshKeyPassphrase = Secret.toString(template.getSshCredentials().getPassphrase());
      String publicSshKey = KeyUtils.getSshPublicKey(privateSshKey, privateSshKeyPassphrase);
      metadata.put("ssh_authorized_keys", publicSshKey);
    } catch (IOException | NotImplementedException | IndexOutOfBoundsException e) {
      LOGGER.log(Level.WARNING, "Bad SSH credentials in OCI template \"{0}\"", template.getName());
      computeClient.close();
      throw new InvalidSshCredentialsException(e);
    }

    LOGGER.log(Level.INFO, "OCI Provisioning: {0}", name_in_oci);
    LaunchInstanceDetails details =
        LaunchInstanceDetails.builder()
            .metadata(metadata)
            .availabilityDomain(template.getAvailabilityDomain())
            .displayName(name_in_oci)
            .shape(template.getShape())
            .compartmentId(template.getCompartmentOCID())
            .imageId(template.getImageOCID())
            .subnetId(template.getSubnetOCID())
            .build();

    try {
      LaunchInstanceRequest request = LaunchInstanceRequest.builder().launchInstanceDetails(details).build();
      LaunchInstanceResponse response = computeClient.launchInstance(request);
      waitForInstanceProvisioningToComplete(computeClient, response.getInstance().getId(), template, timeout);

      return response.getInstance();
    } finally {
      computeClient.close();
    }
  }

  public void terminateSlave(String instanceId) {
    ComputeClient computeClient = new ComputeClient(this.provider);
    computeClient.setRegion(this.credentials.getRegionId());

    TerminateInstanceRequest request = TerminateInstanceRequest.builder().instanceId(instanceId).build();

    computeClient.terminateInstance(request);

    computeClient.close();
  }

  /**
   * Returns first public ip address, first private ip address or null (in
   * this precedence) for given instance
   *
   * @param instanceId
   *            Instance OCID
   * @param template
   *            Instance template constructed from cloud form in Jenkins UI
   * @return First public ip address, first private ip address or null in this
   *         precedence
   */
  public String getInstanceIp(String instanceId, OCISlaveTemplate template) throws RuntimeException {
    ComputeClient computeClient = new ComputeClient(this.provider);
    computeClient.setRegion(this.credentials.getRegionId());
    VirtualNetworkClient vcnClient = new VirtualNetworkClient(this.provider);
    vcnClient.setRegion(this.credentials.getRegionId());

    ListVnicAttachmentsResponse listVnicResponse =
        computeClient.listVnicAttachments(ListVnicAttachmentsRequest.builder().compartmentId(template.getCompartmentOCID()).instanceId(instanceId).build());

    List<VnicAttachment> vnics = listVnicResponse.getItems();
    String publicIp = null;
    String privateIp = null;

    for (int i = 0; i < vnics.size(); i++) {
      String vnicId = vnics.get(i).getVnicId();
      Vnic vnic = vcnClient.getVnic(GetVnicRequest.builder().vnicId(vnicId).build()).getVnic();

      publicIp = vnic.getPublicIp();

      if (publicIp != null) {
        LOGGER.log(Level.INFO, "Using public ip {0}", publicIp);
        break;
      }

      if (privateIp == null && vnic.getPrivateIp() != null) {
        privateIp = vnic.getPrivateIp();
        LOGGER.log(Level.INFO, "Using private ip {0} as fallback", privateIp);
      }
    }

    computeClient.close();
    vcnClient.close();

    if (publicIp != null) {
      return publicIp;
    }
    if (privateIp != null) {
      return privateIp;
    }
    LOGGER.log(Level.WARNING, "Instance {0} has no configured interfaces", instanceId);
    throw new RuntimeException("Instance has no configured interfaces");
  }

  public static List<Region> getRegions() {
    return Arrays.asList(Region.values());
  }

  public List<Compartment> getCompartments() throws Exception {

    Identity identityClient = new IdentityClient(this.provider);
    identityClient.setRegion(this.credentials.getRegionId());
    ListCompartmentsResponse listCompartments =
        identityClient.listCompartments(ListCompartmentsRequest.builder().compartmentId(credentials.getTentancyOCID()).build());
    return listCompartments.getItems();
  }

  public List<Shape> getShapes(String compartmentId) throws Exception {
    ComputeClient computeClient = new ComputeClient(this.provider);
    computeClient.setRegion(this.credentials.getRegionId());

    ListShapesResponse response = computeClient.listShapes(ListShapesRequest.builder().compartmentId(compartmentId).build());

    return response.getItems();
  }

  public List<Image> getImages(String compartmentId) throws Exception {
    ComputeClient computeClient = new ComputeClient(this.provider);
    computeClient.setRegion(this.credentials.getRegionId());

    ListImagesResponse response = computeClient.listImages(ListImagesRequest.builder().compartmentId(compartmentId).build());

    return response.getItems();
  }

  public List<AvailabilityDomain> getAvailabilityDomains(String compartmentId) throws Exception {

    Identity identityClient = new IdentityClient(this.provider);
    identityClient.setRegion(this.credentials.getRegionId());

    ListAvailabilityDomainsResponse listAvailabilityDomainsResponse =
        identityClient.listAvailabilityDomains(ListAvailabilityDomainsRequest.builder().compartmentId(compartmentId).build());

    identityClient.close();

    return listAvailabilityDomainsResponse.getItems();
  }

  public List<Vcn> getVcns(String compartmentId) throws Exception {
    VirtualNetworkClient vcnClient = new VirtualNetworkClient(this.provider);
    vcnClient.setRegion(this.credentials.getRegionId());

    ListVcnsResponse response = vcnClient.listVcns(ListVcnsRequest.builder().compartmentId(compartmentId).build());

    return response.getItems();
  }

  public List<Subnet> getSubnets(String compartmentId, String vcnOCID) throws Exception {
    VirtualNetworkClient vcnClient = new VirtualNetworkClient(this.provider);
    vcnClient.setRegion(this.credentials.getRegionId());
    Vcn vcn = null;

    for (Vcn v : this.getVcns(compartmentId)) {
      if (v.getId().equals(vcnOCID)) {
        vcn = v;
      }
    }

    ListSubnetsResponse response = vcnClient.listSubnets(ListSubnetsRequest.builder().compartmentId(compartmentId).vcnId(vcn.getId()).build());

    return response.getItems();
  }

  public void waitForInstanceProvisioningToComplete(ComputeClient computeClient, String instanceId, OCISlaveTemplate template, int timeout) throws Exception {

    Thread currentThread = Thread.currentThread();

    Thread waitThread =
        new Thread() {
          public void run() {

            ComputeWaiters waiter = computeClient.getWaiters();
            try {
              GetInstanceResponse response =
                  waiter.forInstance(GetInstanceRequest.builder().instanceId(instanceId).build(), Instance.LifecycleState.Running).execute();

              String instIP = getInstanceIp(instanceId, template);

              while (!checkSockAvail(instIP, 22)) {
                LOGGER.log(Level.INFO, " Waiting port 22... ");
                Thread.sleep(15 * 1000);
              }

            } catch (InterruptedException ex) {
              Thread.currentThread().interrupt();
            } catch (Exception ex) {
              Thread.currentThread().interrupt();
            }
          }
        };

    Timer timer = new Timer();
    timer.schedule(
        new TimerTask() {
          public void run() {
            waitThread.interrupt();
            currentThread.interrupt();
            LOGGER.log(Level.SEVERE, "Provisioning timeout");
          }
        },
        timeout * 1000);

    waitThread.start();

    try {
      waitThread.join();
    } catch (InterruptedException ex) {
      terminateSlave(instanceId);
      throw ex;
    }
  }

  public static boolean checkSockAvail(String Host, int port) {
    try (Socket s = new Socket(Host, port)) {
      return true;
    } catch (IOException ex) {
      /* ignore */
    }
    return false;
  }
}
