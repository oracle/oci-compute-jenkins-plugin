package com.oracle.cloud.baremetal.jenkins.client;

import com.oracle.bmc.ClientConfiguration;

import java.util.*;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.oracle.bmc.ClientRuntime;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.core.ComputeAsyncClient;
import com.oracle.bmc.core.ComputeClient;
import com.oracle.bmc.core.ComputeWaiters;
import com.oracle.bmc.core.VirtualNetworkAsyncClient;
import com.oracle.bmc.core.VirtualNetworkClient;
import com.oracle.bmc.core.model.*;
import com.oracle.bmc.core.requests.*;
import com.oracle.bmc.core.responses.*;
import com.oracle.bmc.identity.Identity;
import com.oracle.bmc.identity.IdentityAsyncClient;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.identity.model.AvailabilityDomain;
import com.oracle.bmc.identity.model.Compartment;
import com.oracle.bmc.identity.model.TagNamespaceSummary;
import com.oracle.bmc.identity.model.Tenancy;
import com.oracle.bmc.identity.requests.*;
import com.oracle.bmc.identity.responses.GetTenancyResponse;
import com.oracle.bmc.identity.responses.ListCompartmentsResponse;
import com.oracle.bmc.identity.responses.ListTagNamespacesResponse;
import com.oracle.bmc.model.BmcException;
import com.oracle.cloud.baremetal.jenkins.BaremetalCloudAgentTemplate;
import com.oracle.bmc.core.model.NetworkSecurityGroup;
import com.oracle.cloud.baremetal.jenkins.BaremetalCloudNsgTemplate;
import com.oracle.cloud.baremetal.jenkins.BaremetalCloudTagsTemplate;
import jenkins.model.Jenkins;

/**
 * An implementation of ComputeCloudClient using the JAX-RS client API.
 */
public class SDKBaremetalCloudClient implements BaremetalCloudClient {
    private static final Logger LOGGER = Logger.getLogger(SDKBaremetalCloudClient.class.getName());

    private SimpleAuthenticationDetailsProvider provider;
    private String regionId;
    private ClientConfiguration clientConfig;
    private InstancePrincipalsAuthenticationDetailsProvider instancePrincipalsProvider;
    private boolean instancePrincipals;
    private String tenantId;

    public SDKBaremetalCloudClient(SimpleAuthenticationDetailsProvider provider, String regionId, int maxAsyncThreads) {
        this.provider = provider;
        this.regionId = regionId;
        this.clientConfig = ClientConfiguration.builder().maxAsyncThreads(maxAsyncThreads).build();
        this.tenantId = provider.getTenantId();
        ClientRuntime.setClientUserAgent("Oracle-Jenkins/" + Jenkins.VERSION);
    }

    public SDKBaremetalCloudClient(InstancePrincipalsAuthenticationDetailsProvider instancePrincipalsProvider, String regionId, int maxAsyncThreads, String instancePrincipalsTenantId) {
        this.instancePrincipalsProvider = instancePrincipalsProvider;
        this.regionId = regionId;
        this.instancePrincipals = true;
        this.tenantId = instancePrincipalsTenantId;
        this.clientConfig = ClientConfiguration.builder().maxAsyncThreads(maxAsyncThreads).build();
        ClientRuntime.setClientUserAgent("Oracle-Jenkins/" + Jenkins.VERSION);
    }


    private IdentityClient getIdentityClient() {
        IdentityClient identityClient;
        if (!instancePrincipals) {
            identityClient = new IdentityClient(provider, null, new HTTPProxyConfigurator());
        } else {
            identityClient = new IdentityClient(instancePrincipalsProvider, null, new HTTPProxyConfigurator());
        }
        identityClient.setRegion(regionId);
        return identityClient;
    }

    private IdentityAsyncClient getIdentityAsyncClient() {
        IdentityAsyncClient identityClient;
        if (!instancePrincipals) {
            identityClient = new IdentityAsyncClient(provider, clientConfig, new HTTPProxyConfigurator());
        } else {
            identityClient = new IdentityAsyncClient(instancePrincipalsProvider, clientConfig, new HTTPProxyConfigurator());
        }
        identityClient.setRegion(regionId);
        return identityClient;
    }

    private ComputeClient getComputeClient() {
        ComputeClient computeClient;
        if (!instancePrincipals) {
            computeClient = new ComputeClient(provider, null, new HTTPProxyConfigurator());
        } else {
            computeClient = new ComputeClient(instancePrincipalsProvider, null, new HTTPProxyConfigurator());
        }
        computeClient.setRegion(regionId);
        return computeClient;
    }

    private ComputeAsyncClient getComputeAsyncClient() {
        ComputeAsyncClient computeClient;
        if (!instancePrincipals) {
            computeClient = new ComputeAsyncClient(provider, clientConfig, new HTTPProxyConfigurator());
        } else {
            computeClient = new ComputeAsyncClient(instancePrincipalsProvider, null, new HTTPProxyConfigurator());
        }
        computeClient.setRegion(regionId);
        return computeClient;
    }

    private VirtualNetworkClient getVirtualNetworkClient() {
        VirtualNetworkClient networkClient;
        if (!instancePrincipals) {
            networkClient = new VirtualNetworkClient(provider, null, new HTTPProxyConfigurator());
        } else {
            networkClient = new VirtualNetworkClient(instancePrincipalsProvider, null, new HTTPProxyConfigurator());
        }
        networkClient.setRegion(regionId);
        return networkClient;
    }

    private VirtualNetworkAsyncClient getVirtualNetworkAsyncClient() {
        VirtualNetworkAsyncClient networkClient;
        if (!instancePrincipals) {
            networkClient = new VirtualNetworkAsyncClient(provider, clientConfig, new HTTPProxyConfigurator());
        } else {
            networkClient = new VirtualNetworkAsyncClient(instancePrincipalsProvider, null, new HTTPProxyConfigurator());
        }
        networkClient.setRegion(regionId);
        return networkClient;
    }

    @Override
    public void authenticate() throws BmcException {
        Identity identityClient = getIdentityClient();

        try{
            if (!instancePrincipals) {
                identityClient.getUser(GetUserRequest.builder().userId(provider.getUserId()).build());
            } else {
                identityClient.getTenancy(GetTenancyRequest.builder().tenancyId(tenantId).build());
            }
        }catch(BmcException e){
            LOGGER.log(Level.FINE, "Failed to connect to Oracle Cloud Infrastructure. Please verify all credential information.", e);
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
            String compartmentIdStr = template.getCompartmentId();
            String subnetIdStr = template.getSubnet();
            String imageIdStr = template.getImage();
            String shape = template.getShape();
            String sshPublicKey = template.getPublicKey();

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

            LaunchInstanceShapeConfigDetails shapeConfig = null;
            if (!template.getNumberOfOcpus().isEmpty() && !template.getMemoryInGBs().isEmpty()) {
                shapeConfig = LaunchInstanceShapeConfigDetails.builder()
                        .ocpus(Float.parseFloat(template.getNumberOfOcpus()))
                        .memoryInGBs(Float.parseFloat(template.getMemoryInGBs()))
                        .build();
            }

            List<String> nsgIds = new ArrayList<>();
            if (template.getNsgIds() != null && !template.getNsgIds().isEmpty()) {
                nsgIds = template.getNsgIds().stream()
                        .map(BaremetalCloudNsgTemplate::getNsgId)
                        .collect(Collectors.toList());
            }

            LaunchInstanceDetails.Builder instanceDetailsBuilder = LaunchInstanceDetails
                    .builder()
                    .availabilityDomain(ad)
                    .compartmentId(compartmentIdStr)
                    .createVnicDetails(
                            CreateVnicDetails.builder()
                                    .assignPublicIp(assignPublicIP)
                                    .subnetId(subnetIdStr)
                                    .nsgIds(nsgIds)
                                    .build())
                    .displayName(name)
                    .imageId(imageIdStr)
                    .metadata(metadata)
                    .shape(shape)
                    .shapeConfig(shapeConfig)
                    .subnetId(subnetIdStr);

            if(template.getTags() != null) {
                Map<String,String> freeFormTags = new HashMap<>();
                Map<String,Map<String,Object>> definedTags = new HashMap<>();
                for (BaremetalCloudTagsTemplate tag : template.getTags()) {
                    if (tag.getNamespace().equals("None")) {
                        freeFormTags.put(tag.getKey(),tag.getValue());
                    } else {
                        Map<String,Object> definedTag = new HashMap<>();
                        if (definedTags.containsKey(tag.getNamespace())) {
                            definedTag = definedTags.get(tag.getNamespace());
                        }
                        definedTag.put(tag.getKey(),tag.getValue());
                        definedTags.put(tag.getNamespace(), definedTag);
                    }
                }
                if (!freeFormTags.isEmpty()) {
                    instanceDetailsBuilder.freeformTags(freeFormTags);
                }

                if (!definedTags.isEmpty()) {
                    instanceDetailsBuilder.definedTags(definedTags);
                }
            }

            LaunchInstanceResponse response = computeClient.launchInstance(LaunchInstanceRequest
                    .builder()
                    .launchInstanceDetails(
                            instanceDetailsBuilder
                            .build())
                    .build());

            instance = response.getInstance();
            return instance;

        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, "Failed to launch instance " + name, ex);

	    if (instance != null && instance.getId() != null) {
                try {
                    terminateInstance(instance.getId());
                } catch (Exception e) {
	            LOGGER.log(Level.WARNING, "Failed to terminate unlaunchable instance" + name, e);
                }
	    }
            throw ex;
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

            String compartmentId = template.getCompartmentId();

            // for the instance, list its vnic attachments
            ListVnicAttachmentsResponse listVnicResponse =
                    computeClient.listVnicAttachments(
                            ListVnicAttachmentsRequest.builder()
                                    .compartmentId(compartmentId)
                                    .instanceId(instanceId)
                                    .build());
            // for each vnic attachment, get the vnic details from the virtualNetwork API
            List<VnicAttachment> vnics = listVnicResponse.getItems();
            for (VnicAttachment vnic : vnics) {

                String vnicId = vnic.getVnicId();

                GetVnicResponse getVnicResponse =
                        vcnClient.getVnic(GetVnicRequest.builder().vnicId(vnicId).build());

                // then check the vnic for a public IP or private IP
                String publicIpLocal = getVnicResponse.getVnic().getPublicIp();
                boolean usePublicIP = true;
                if (template.getUsePublicIP() != null) {
                    usePublicIP = template.getUsePublicIP();
                }
                if (usePublicIP && publicIpLocal != null) {
                    LOGGER.info("Get public ip for instance " + instanceId + ": " + publicIpLocal);
                    Ip = publicIpLocal;
                } else {
                    String privateIpLocal = getVnicResponse.getVnic().getPrivateIp();
                    if (privateIpLocal != null) {
                        LOGGER.info("Get private ip for instance " + instanceId + ": " + privateIpLocal);
                        Ip = privateIpLocal;
                    }

                }
            }
        }
        return Ip;
    }

    @Override
    public Tenancy getTenant() throws Exception {

        try (IdentityClient identityClient = getIdentityClient()) {
            GetTenancyResponse response =  identityClient.getTenancy(GetTenancyRequest.builder().tenancyId(tenantId).build());
            return response.getTenancy();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get root compartment", e);
            throw e;
        }
    }

    @Override
    public List<Compartment> getCompartmentsList() throws Exception {
        List<Compartment> compartmentIds = new ArrayList<>();
        ListCompartmentsRequest.Builder builder;
        try (IdentityAsyncClient identityAsyncClient = getIdentityAsyncClient()) {
            builder = ListCompartmentsRequest.builder().compartmentId(tenantId).compartmentIdInSubtree(Boolean.TRUE);
            String nextPageToken = null;
            do {
                builder.page(nextPageToken);
                Future<ListCompartmentsResponse> listResponse = identityAsyncClient.listCompartments(builder.build(), null);
                compartmentIds.addAll(listResponse.get().getItems());
                nextPageToken = listResponse.get().getOpcNextPage();
            } while (nextPageToken != null);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get compartment list", e);
            throw e;
        }
        return compartmentIds;
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
            ListImagesRequest.Builder builder = ListImagesRequest.builder().compartmentId(compartmentId);
            String nextPageToken = null;
            do {
                builder.page(nextPageToken);
                Future<ListImagesResponse> response  = computeAsyncClient.listImages(builder.build(), null);
                imageList.addAll(response.get().getItems());
                nextPageToken = response.get().getOpcNextPage();
            } while (nextPageToken != null);
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
            ListShapesRequest.Builder  builder = ListShapesRequest.builder()
                    .compartmentId(compartmentId)
                    .availabilityDomain(availableDomain)
                    .imageId(imageId);
            String nextPageToken = null;
            do {
                builder.page(nextPageToken);
                Future<ListShapesResponse> response  = computeAsyncClient.listShapes(builder.build(), null);
                shapeList.addAll(response.get().getItems());
                nextPageToken = response.get().getOpcNextPage();
            } while (nextPageToken != null);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get Shape list", e);
            throw e;
        }
        return shapeList;
    }

    public Integer[] getMinMaxOcpus(String compartmentId, String availableDomain, String imageId, String shape) throws Exception {
        Integer[] ocpuOptions = new Integer[2];

        getShapesList(compartmentId, availableDomain, imageId).stream()
                .parallel()
                .filter(n -> n.getShape().equals(shape))
                .forEach(n -> {
                    ocpuOptions[0] = n.getOcpuOptions().getMin().intValue();
                    ocpuOptions[1] = n.getOcpuOptions().getMax().intValue();
                });

        return ocpuOptions;
    }

    public Integer[] getMinMaxMemory(String compartmentId, String availableDomain, String imageId, String shape) throws Exception {
        Integer[] memoryOptions = new Integer[2];

        getShapesList(compartmentId, availableDomain, imageId).stream()
                .parallel()
                .filter(n -> n.getShape().equals(shape))
                .forEach(n -> {
                    memoryOptions[0] = n.getMemoryOptions().getMinInGBs().intValue();
                    memoryOptions[1] = n.getMemoryOptions().getMaxInGBs().intValue();
                });

        return memoryOptions;
    }

    @Override
    public List<Vcn> getVcnList(String compartmentId) throws Exception {        
        List<Vcn> vcnList = new ArrayList<>();

        try (VirtualNetworkAsyncClient vnc = getVirtualNetworkAsyncClient()) {
            ListVcnsRequest.Builder builder = ListVcnsRequest.builder().compartmentId(compartmentId);
            String nextPageToken = null;
            do {
                builder.page(nextPageToken);
                Future<ListVcnsResponse> response = vnc.listVcns(builder.build(), null);
                vcnList.addAll(response.get().getItems());
                nextPageToken = response.get().getOpcNextPage();
            } while (nextPageToken != null);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get VCN list", e);
            throw e;
        }
        return vcnList;
    }

    @Override
    public List<Subnet> getSubNetList(String compartmentId,String vcnId) throws Exception {
        List<Subnet> subnetList = new ArrayList<>();

        try (VirtualNetworkAsyncClient vnc = getVirtualNetworkAsyncClient()) {
            ListSubnetsRequest.Builder builder = ListSubnetsRequest.builder()
                    .compartmentId(compartmentId)
                    .vcnId(vcnId);
            String nextPageToken = null;
            do {
                builder.page(nextPageToken);
                Future<ListSubnetsResponse> response = vnc.listSubnets(builder.build(), null);
                subnetList.addAll(response.get().getItems());
                nextPageToken = response.get().getOpcNextPage();
            } while (nextPageToken != null);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get Subnet list", e);
            throw e;
        }
        return subnetList;
    }

    public List<NetworkSecurityGroup> getNsgIdsList(String compartmentId) throws Exception {
        List<NetworkSecurityGroup> nsgList = new ArrayList<>();
        try (VirtualNetworkAsyncClient vnc = getVirtualNetworkAsyncClient()) {
            ListNetworkSecurityGroupsRequest request = ListNetworkSecurityGroupsRequest.builder()
                    .compartmentId(compartmentId)
                    .build();
            nsgList.addAll(vnc.listNetworkSecurityGroups(request,null).get().getItems());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get Network Security Group list", e);
            throw e;
        }
        return nsgList;
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
                    Instance.LifecycleState.Stopping,
                    Instance.LifecycleState.Stopped,
                    Instance.LifecycleState.Terminating,
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

    @Override
    public List<Instance> getStoppedInstances(String compartmentId, String availableDomain) throws Exception {
        List<Instance> instances = new ArrayList<>();
        try (ComputeClient computeClient = getComputeClient()) {
            ListInstancesResponse response = computeClient.listInstances(ListInstancesRequest.builder()
                    .compartmentId(compartmentId)
                    .availabilityDomain(availableDomain)
                    .lifecycleState(Instance.LifecycleState.Stopped)
                    .build());
            instances.addAll(response.getItems());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get Stopped list", e);
            throw e;
        }
        return instances;
    }

    @Override
    public String stopInstance(String instanceId) throws Exception {
        try (ComputeClient computeClient = getComputeClient()) {
            InstanceActionRequest.Builder builder = InstanceActionRequest.builder()
                    .action("STOP")
                    .instanceId(instanceId);
            InstanceActionResponse response = computeClient.instanceAction(builder.build());

            return response.getOpcRequestId();
        }catch(Exception ex){
            throw new Exception("Failed to stop an instance: " + ex.getMessage());
        }
    }

    @Override
    public Instance startInstance(String instanceId) throws Exception {
        try (ComputeClient computeClient = getComputeClient()) {
            InstanceActionRequest.Builder builder = InstanceActionRequest.builder()
                    .action("START")
                    .instanceId(instanceId);
            InstanceActionResponse response = computeClient.instanceAction(builder.build());
            return response.getInstance();
        }catch(Exception ex){
            throw new Exception("Failed to start an instance: " + ex.getMessage());
        }
    }

    @Override
    public List<TagNamespaceSummary> getTagNamespaces(String compartmentId) throws Exception {
        List<TagNamespaceSummary> tagNamespaces = new ArrayList<>();
        try (IdentityAsyncClient identityAsyncClient = getIdentityAsyncClient()) {
            ListTagNamespacesRequest.Builder builder = ListTagNamespacesRequest.builder()
                    .compartmentId(compartmentId)
                    .includeSubcompartments(Boolean.TRUE);
            String nextPageToken = null;
            do {
                builder.page(nextPageToken);
                Future<ListTagNamespacesResponse> response = identityAsyncClient.listTagNamespaces(builder.build(),null);
                tagNamespaces.addAll(response.get().getItems());
                nextPageToken = response.get().getOpcNextPage();
            } while (nextPageToken != null);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get tag namespaces list", e);
            throw e;
        }
        return tagNamespaces;
    }
}
