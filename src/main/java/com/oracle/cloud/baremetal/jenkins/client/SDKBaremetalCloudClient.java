package com.oracle.cloud.baremetal.jenkins.client;

import com.oracle.bmc.ClientConfiguration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.oracle.bmc.ClientRuntime;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.core.ComputeAsyncClient;
import com.oracle.bmc.core.ComputeClient;
import com.oracle.bmc.core.ComputeWaiters;
import com.oracle.bmc.core.VirtualNetworkAsyncClient;
import com.oracle.bmc.core.VirtualNetworkClient;
import com.oracle.bmc.core.model.CreateVnicDetails;
import com.oracle.bmc.core.model.Image;
import com.oracle.bmc.core.model.Instance;
import com.oracle.bmc.core.model.LaunchInstanceDetails;
import com.oracle.bmc.core.model.Shape;
import com.oracle.bmc.core.model.Subnet;
import com.oracle.bmc.core.model.Vcn;
import com.oracle.bmc.core.model.VnicAttachment;
import com.oracle.bmc.core.requests.GetInstanceRequest;
import com.oracle.bmc.core.requests.GetSubnetRequest;
import com.oracle.bmc.core.requests.GetVnicRequest;
import com.oracle.bmc.core.requests.LaunchInstanceRequest;
import com.oracle.bmc.core.requests.ListImagesRequest;
import com.oracle.bmc.core.requests.ListShapesRequest;
import com.oracle.bmc.core.requests.ListSubnetsRequest;
import com.oracle.bmc.core.requests.ListVcnsRequest;
import com.oracle.bmc.core.requests.ListVnicAttachmentsRequest;
import com.oracle.bmc.core.requests.TerminateInstanceRequest;
import com.oracle.bmc.core.responses.GetInstanceResponse;
import com.oracle.bmc.core.responses.GetSubnetResponse;
import com.oracle.bmc.core.responses.GetVnicResponse;
import com.oracle.bmc.core.responses.LaunchInstanceResponse;
import com.oracle.bmc.core.responses.ListSubnetsResponse;
import com.oracle.bmc.core.responses.ListVcnsResponse;
import com.oracle.bmc.core.responses.ListVnicAttachmentsResponse;
import com.oracle.bmc.core.responses.TerminateInstanceResponse;
import com.oracle.bmc.identity.Identity;
import com.oracle.bmc.identity.IdentityAsyncClient;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.identity.model.AvailabilityDomain;
import com.oracle.bmc.identity.model.Compartment;
import com.oracle.bmc.identity.requests.GetUserRequest;
import com.oracle.bmc.identity.requests.ListAvailabilityDomainsRequest;
import com.oracle.bmc.identity.requests.ListCompartmentsRequest;
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
    private int maxAsyncThreads;
    private ClientConfiguration clientConfig;

    public SDKBaremetalCloudClient(SimpleAuthenticationDetailsProvider provider, String regionId, int maxAsyncThreads) {
        this.provider = provider;
        this.regionId = regionId;
        this.maxAsyncThreads = maxAsyncThreads;
        this.clientConfig = ClientConfiguration.builder().maxAsyncThreads(maxAsyncThreads).build();
        ClientRuntime.setClientUserAgent("Oracle-Jenkins/" + Jenkins.VERSION);
    }

    private IdentityClient getIdentityClient() {
        IdentityClient identityClient = new IdentityClient(provider, null, new HTTPProxyConfigurator());
        identityClient.setRegion(regionId);
        return identityClient;
    }

    private IdentityAsyncClient getIdentityAsyncClient() {
        IdentityAsyncClient identityClient = new IdentityAsyncClient(provider, clientConfig, new HTTPProxyConfigurator());
        identityClient.setRegion(regionId);
        return identityClient;
    }

    private ComputeClient getComputeClient() {
        ComputeClient computeClient = new ComputeClient(provider, null, new HTTPProxyConfigurator());
        computeClient.setRegion(regionId);
        return computeClient;
    }

    private ComputeAsyncClient getComputeAsyncClient() {
        ComputeAsyncClient computeClient = new ComputeAsyncClient(provider, clientConfig, new HTTPProxyConfigurator());
        computeClient.setRegion(regionId);
        return computeClient;
    }

    private VirtualNetworkClient getVirtualNetworkClient() {
        VirtualNetworkClient networkClient = new VirtualNetworkClient(provider, null, new HTTPProxyConfigurator());
        networkClient.setRegion(regionId);
        return networkClient;
    }

    private VirtualNetworkAsyncClient getVirtualNetworkAsyncClient() {
        VirtualNetworkAsyncClient networkClient = new VirtualNetworkAsyncClient(provider, clientConfig, new HTTPProxyConfigurator());
        networkClient.setRegion(regionId);
        return networkClient;
    }

    @Override
    public void authenticate() throws BmcException {
        Identity identityClient = getIdentityClient();

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
        try (ComputeClient computeClient = getComputeClient()) {

            String ad = template.getAvailableDomain();
            String compartmentIdStr = template.getcompartmentId();
            String subnetIdStr = template.getSubnet();
            String imageIdStr = template.getImage();
            String shape = template.getShape();
            String sshPublicKey = template.getSshPublickey();
            String instanceName = name;

            boolean assignPublicIP = true;
            if(template.getAssignPublicIP() != null) {
                assignPublicIP = template.getAssignPublicIP();
            }

            Map<String, String> metadata = new HashMap<>();
            metadata.put("ssh_authorized_keys", sshPublicKey);

            GetSubnetResponse subnetResponse = getSubNet(subnetIdStr);
            if(subnetResponse.getSubnet().getProhibitPublicIpOnVnic()) {
                assignPublicIP=false;
            }

            LaunchInstanceResponse response = computeClient.launchInstance(LaunchInstanceRequest
                    .builder()
                    .launchInstanceDetails(
                            LaunchInstanceDetails
                            .builder()
                            .availabilityDomain(ad)
                            .compartmentId(compartmentIdStr)
                            .createVnicDetails(
                                    CreateVnicDetails.builder()
                                    .assignPublicIp(assignPublicIP)
                                    .subnetId(subnetIdStr)
                                    .build())
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
        try (ComputeClient computeClient = getComputeClient()) {
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
    public String getInstanceIp(BaremetalCloudAgentTemplate template, String instanceId) throws Exception {
        String Ip = "";

        try (ComputeClient computeClient = getComputeClient();
                VirtualNetworkClient vcnClient = getVirtualNetworkClient()) {

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

                // then check the vnic for a public IP or private IP
                String publicIpLocal = getVnicResponse.getVnic().getPublicIp();
                boolean usePublicIP = true;
                if(template.getUsePublicIP()!= null) {
                    usePublicIP = template.getUsePublicIP();
                }
                if (usePublicIP && publicIpLocal != null) {
                    LOGGER.info("Get public ip for instance " + instanceId + ": " + publicIpLocal);
                    Ip =  publicIpLocal;
                } else {
                    String privateIpLocal = getVnicResponse.getVnic().getPrivateIp();
                    if (privateIpLocal != null) {
                        LOGGER.info("Get private ip for instance " + instanceId + ": " + privateIpLocal);
                        Ip =  privateIpLocal;
                    }

                }
            }
        }
        return Ip;
    }

    @Override
    public List<Compartment> getCompartmentsList(String tenantId) throws Exception {
        try (Identity identityClient = getIdentityClient()) {
            List<Compartment> compartmentIds;
            compartmentIds = identityClient.listCompartments(ListCompartmentsRequest.builder().compartmentId(tenantId).build()).getItems();
            return compartmentIds;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get compartment list", e);
            throw e;
        }
    }


    @Override
    public List<AvailabilityDomain> getAvailabilityDomainsList(String compartmentId) throws Exception {
        List<AvailabilityDomain> availabilityDomainsList = new ArrayList<>();

        try (IdentityAsyncClient identityAsyncClient = getIdentityAsyncClient()) {
            ListAvailabilityDomainsRequest request = ListAvailabilityDomainsRequest.builder()
                    .compartmentId(compartmentId)
                    .build();
            availabilityDomainsList.addAll(identityAsyncClient.listAvailabilityDomains(request, null).get().getItems());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get Availability Domain list", e);
            throw e;
        }
        return availabilityDomainsList;
    }


    @Override
    public List<Image> getImagesList(String compartmentId) throws Exception {
        List<Image> imageList = new ArrayList<>();

        try (ComputeAsyncClient computeAsyncClient = getComputeAsyncClient()) {
            ListImagesRequest request = ListImagesRequest.builder()
                    .compartmentId(compartmentId)
                    .build();
            imageList.addAll(computeAsyncClient.listImages(request, null).get().getItems());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get Image list", e);
            throw e;
        }
        return imageList;
    }

    @Override
    public List<Shape> getShapesList(String compartmentId, String availableDomain, String imageId) throws Exception {
        List<Shape> shapeList = new ArrayList<>();

        try (ComputeAsyncClient computeAsyncClient = getComputeAsyncClient()) {
            ListShapesRequest request = ListShapesRequest.builder()
                    .compartmentId(compartmentId)
                    .availabilityDomain(availableDomain)
                    .imageId(imageId)
                    .build();
            shapeList.addAll(computeAsyncClient.listShapes(request, null).get().getItems());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get Shape list", e);
            throw e;
        }
        return shapeList;
    }

    @Override
    public List<Vcn> getVcnList(String tenantId) throws Exception {
        List<Compartment> compartmentList = getCompartmentsList(tenantId);
        List<Vcn> vcnList = new ArrayList<>();

        try (VirtualNetworkAsyncClient vnc = getVirtualNetworkAsyncClient()) {
            List<Future<ListVcnsResponse>> futureList = new ArrayList<>();

            // Asynchronously list VCNs in all available compartments
            for (Compartment compartment : compartmentList) {
                ListVcnsRequest request = ListVcnsRequest.builder().compartmentId(compartment.getId()).build();
                futureList.add(vnc.listVcns(request, null));
            }

            for (Future<ListVcnsResponse> future : futureList) {
                try {
                    vcnList.addAll(future.get().getItems());
                } catch (BmcException e) {
                    if (e.getStatusCode() != 404) { // NotAuthorizedOrNotFound
                        throw e;
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get VCN list", e);
            throw e;
        }
        return vcnList;
    }

    @Override
    public List<Subnet> getSubNetList(String tenantId,String vcnId) throws Exception {
        List<Compartment> compartmentList = getCompartmentsList(tenantId);
        List<Subnet> subnetList = new ArrayList<>();

        try (VirtualNetworkAsyncClient vnc = getVirtualNetworkAsyncClient()) {
            List<Future<ListSubnetsResponse>> futureList = new ArrayList<>();

            // The VCN and subnet can be from different compartment
            for (Compartment compartment : compartmentList) {
                ListSubnetsRequest request = ListSubnetsRequest.builder()
                        .compartmentId(compartment.getId())
                        .vcnId(vcnId)
                        .build();
                futureList.add(vnc.listSubnets(request, null));
            }

            for (Future<ListSubnetsResponse> future : futureList) {
                try {
                    subnetList.addAll(future.get().getItems());
                } catch (BmcException e) {
                    if (e.getStatusCode() != 404) { // NotAuthorizedOrNotFound
                        throw e;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get Subnet list", e);
            throw e;
        }
        return subnetList;
    }

    @Override
    public GetSubnetResponse getSubNet(String subnetId) throws Exception {
        GetSubnetResponse subnetResponse;
        try (VirtualNetworkClient vnc = getVirtualNetworkClient()) {
            subnetResponse = vnc.getSubnet(GetSubnetRequest.builder().subnetId(subnetId).build());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get subnet list", e);
            throw e;
        }
        return subnetResponse;
    }

    @Override
    public String terminateInstance(String instanceId) throws Exception {
        try (ComputeClient computeClient = getComputeClient()) {
            TerminateInstanceResponse response = computeClient
                    .terminateInstance(TerminateInstanceRequest.builder().instanceId(instanceId).build());
            return response.getOpcRequestId();
        }
    }

    @Override
    public Instance waitForInstanceTerminationToComplete(String instanceId) throws Exception {
        try (ComputeClient computeClient = getComputeClient()) {
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
        try (ComputeClient computeClient = getComputeClient()) {
            GetInstanceResponse response = computeClient.getInstance(GetInstanceRequest.builder().instanceId(instanceId).build());
            return response.getInstance().getLifecycleState();
    	}
    }
}
