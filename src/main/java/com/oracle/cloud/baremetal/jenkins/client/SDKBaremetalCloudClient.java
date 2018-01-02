package com.oracle.cloud.baremetal.jenkins.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.oracle.bmc.ClientRuntime;
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
import com.oracle.bmc.core.responses.GetVnicResponse;
import com.oracle.bmc.core.responses.LaunchInstanceResponse;
import com.oracle.bmc.core.responses.ListVnicAttachmentsResponse;
import com.oracle.bmc.core.responses.TerminateInstanceResponse;
import com.oracle.bmc.identity.Identity;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.identity.model.Compartment;
import com.oracle.bmc.identity.requests.GetUserRequest;
import com.oracle.bmc.identity.requests.ListAvailabilityDomainsRequest;
import com.oracle.bmc.identity.requests.ListCompartmentsRequest;
import com.oracle.bmc.identity.responses.ListAvailabilityDomainsResponse;
import com.oracle.bmc.model.BmcException;
import com.oracle.cloud.baremetal.jenkins.BaremetalCloudAgentTemplate;

import jenkins.model.Jenkins;

/**
 * An implementation of ComputeCloudClient using the JAX-RS client API.
 */
public class SDKBaremetalCloudClient implements BaremetalCloudClient {
    private static final Logger LOGGER = Logger.getLogger(SDKBaremetalCloudClient.class.getName());

    private SimpleAuthenticationDetailsProvider provider;
    private String regionId;

    public SDKBaremetalCloudClient(SimpleAuthenticationDetailsProvider provider, String regionId) {
        this.provider = provider;
        this.regionId = regionId;
        ClientRuntime.setClientUserAgent("Oracle-Jenkins/" + Jenkins.VERSION);
    }

    @Override
    public void authenticate() throws BmcException {
        Identity identityClient = new IdentityClient(provider);
        identityClient.setRegion(regionId);

        try{
            identityClient.getUser(GetUserRequest.builder().userId(provider.getUserId()).build());
        }catch(BmcException e){
            LOGGER.log(Level.FINE, "Failed to connect to Oracle Cloud Infrastructure, Please verify all the credential informations enterred", e);
            throw e;
        }finally{
            try {
                identityClient.close();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public Instance createInstance(String name, BaremetalCloudAgentTemplate template) throws Exception {
        Instance instance = null;
        try (ComputeClient computeClient = new ComputeClient(provider)) {

            String ad = template.getAvailableDomain();
            String compartmentIdStr = template.getcompartmentId();
            String subnetIdStr = template.getSubnet();
            String imageIdStr = template.getImage();
            String shape = template.getShape();
            String sshPublicKey = template.getSshPublickey();
            String instanceName = name;

            computeClient.setRegion(regionId);

            Map<String, String> metadata = new HashMap<>();
            metadata.put("ssh_authorized_keys", sshPublicKey);

            LaunchInstanceResponse response = computeClient.launchInstance(LaunchInstanceRequest
                    .builder()
                    .launchInstanceDetails(
                            LaunchInstanceDetails
                            .builder()
                            .availabilityDomain(ad)
                            .compartmentId(compartmentIdStr)
                            .displayName(instanceName)
                            .imageId(imageIdStr)
                            .metadata(metadata)
                            .shape(shape)
                            .subnetId(subnetIdStr)
                            .build())
                    .build());
            instance = response.getInstance();
            return instance;
        }catch(Exception ex){
            if(null != instance && null != instance.getId()){
                terminateInstance(instance.getId());
            }
            throw new Exception("Instance creation fails because: " + ex.getMessage());
        }
    }


    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub

    }


    @Override
    public Instance waitForInstanceProvisioningToComplete(String instanceId) throws Exception {
        try (ComputeClient computeClient = new ComputeClient(provider)) {
            computeClient.setRegion(regionId);
            ComputeWaiters waiter = computeClient.getWaiters();
            GetInstanceResponse response = waiter.forInstance(
                    GetInstanceRequest
                    .builder()
                    .instanceId(instanceId)
                    .build(),
                    Instance.LifecycleState.Running)
                    .execute();
            return response.getInstance();
        }
    }


    @Override
    public String getInstancePublicIp(BaremetalCloudAgentTemplate template, String instanceId) throws Exception {
        String publicIp = "";
        try (ComputeClient computeClient = new ComputeClient(provider);
            VirtualNetworkClient vcnClient = new VirtualNetworkClient(provider)) {

            computeClient.setRegion(regionId);
            vcnClient.setRegion(regionId);

            String compartmentId = template.getcompartmentId();

            // for the instance, list its vnic attachments
            ListVnicAttachmentsResponse listVnicResponse =
                    computeClient.listVnicAttachments(
                            ListVnicAttachmentsRequest.builder()
                                    .compartmentId(compartmentId)
                                    .instanceId(instanceId)
                                    .build());
            // for each vnic attachment, get the vnic details from the virtualNetwork API
            List<VnicAttachment> vnics = listVnicResponse.getItems();
            for (int i = 0; i < vnics.size(); i++) {

                String vnicId = vnics.get(i).getVnicId();

                GetVnicResponse getVnicResponse =
                        vcnClient.getVnic(GetVnicRequest.builder().vnicId(vnicId).build());

                // then check the vnic for a public IP
                String publicIpLocal = getVnicResponse.getVnic().getPublicIp();
                if (publicIpLocal != null) {
                    LOGGER.info("Get public ip for instance " + instanceId + ": " + publicIpLocal);
                    publicIp =  publicIpLocal;
                }
            }
        }
        return publicIp;
    }


    @Override
    public List<Compartment> getCompartmentsList(String tenantId) throws Exception {
        try (Identity identityClient = new IdentityClient(provider)) {
            identityClient.setRegion(regionId);
            List<Compartment> compartmentIds;
            compartmentIds = identityClient.listCompartments(ListCompartmentsRequest.builder().compartmentId(tenantId).build()).getItems();
            return compartmentIds;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get compartment list", e);
            throw e;
        }
    }


    @Override
    public ListAvailabilityDomainsResponse getAvailabilityDomainsList(String compartmentId) throws Exception {
        try (Identity identityClient = new IdentityClient(provider);) {
        identityClient.setRegion(regionId);
        ListAvailabilityDomainsResponse listAvailabilityDomainsResponse;
        listAvailabilityDomainsResponse = identityClient.listAvailabilityDomains(ListAvailabilityDomainsRequest.builder()
                .compartmentId(compartmentId).build());
        return listAvailabilityDomainsResponse;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get available domain", e);
            throw e;
        }
    }


    @Override
    public List<Image> getImagesList(String compartmentId) throws Exception {
        try (ComputeClient computeClient = new ComputeClient(provider)) {
            computeClient.setRegion(regionId);
            List<Image> list = computeClient.listImages(ListImagesRequest.builder().compartmentId(compartmentId).build()).getItems();
            return list;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get image list", e);
            throw e;
        }
    }

    @Override
    public List<Shape> getShapesList(String compartmentId, String availableDomain, String imageId) throws Exception {
        try (ComputeClient computeClient = new ComputeClient(provider)) {
            computeClient.setRegion(regionId);
            List<Shape> list = computeClient.listShapes(ListShapesRequest.builder().compartmentId(compartmentId).availabilityDomain(availableDomain).imageId(imageId).build()).getItems();
            return list;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get image list", e);
            throw e;
        }
    }

    @Override
    public List<Vcn> getVcnList(String compartmentId) throws Exception {
        List<Vcn> list;
        try (VirtualNetworkClient vnc = new VirtualNetworkClient(provider)) {
            vnc.setRegion(regionId);
            list = vnc.listVcns(ListVcnsRequest.builder().compartmentId(compartmentId).build()).getItems();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get VCN list", e);
            throw e;
        }
        return list;
    }

    @Override
    public List<Subnet> getSubNetList(String compartmentId, String vcnId) throws Exception {
        List<Subnet> list;
        try (VirtualNetworkClient vnc = new VirtualNetworkClient(provider)) {
            vnc.setRegion(regionId);
            list = vnc.listSubnets(ListSubnetsRequest.builder().compartmentId(compartmentId).vcnId(vcnId).build()).getItems();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get subnet list", e);
            throw e;
        }
        return list;
    }

    @Override
    public String terminateInstance(String instanceId) throws Exception {
        try (ComputeClient computeClient = new ComputeClient(provider)) {
            computeClient.setRegion(regionId);
            TerminateInstanceResponse response = computeClient
                    .terminateInstance(TerminateInstanceRequest.builder().instanceId(instanceId).build());
            return response.getOpcRequestId();
        }
    }


    @Override
    public Instance waitForInstanceTerminationToComplete(String instanceId) throws Exception {
        try (ComputeClient computeClient = new ComputeClient(provider)) {
            computeClient.setRegion(regionId);
            ComputeWaiters waiter = computeClient.getWaiters();
            GetInstanceResponse response = waiter.forInstance(
                    GetInstanceRequest.builder()
                    .instanceId(instanceId)
                    .build(),
                    Instance.LifecycleState.Terminated).execute();
            return response.getInstance();
        }
    }

    @Override
    public Instance.LifecycleState getInstanceState(String instanceId) throws Exception {
    	try (ComputeClient computeClient = new ComputeClient(provider)) {
            computeClient.setRegion(regionId);
            GetInstanceResponse response = computeClient.getInstance(GetInstanceRequest.builder().instanceId(instanceId).build());
            return response.getInstance().getLifecycleState();
    	}
    }
}
