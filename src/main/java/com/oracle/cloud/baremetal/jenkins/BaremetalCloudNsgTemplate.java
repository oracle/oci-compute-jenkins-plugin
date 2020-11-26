package com.oracle.cloud.baremetal.jenkins;


import com.oracle.bmc.identity.model.Tenancy;
import com.oracle.cloud.baremetal.jenkins.client.BaremetalCloudClient;
import com.oracle.cloud.baremetal.jenkins.client.BaremetalCloudClientFactory;
import com.oracle.cloud.baremetal.jenkins.client.SDKBaremetalCloudClientFactory;
import hudson.Extension;
import hudson.RelativePath;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BaremetalCloudNsgTemplate extends AbstractDescribableImpl<BaremetalCloudNsgTemplate> {
    private static final Logger LOGGER = Logger.getLogger(BaremetalCloud.class.getName());
    private final String nsgCompartmentId;
    private final String nsgId;

    @DataBoundConstructor
    public BaremetalCloudNsgTemplate(String nsgCompartmentId, String nsgId) {
        this.nsgCompartmentId = nsgCompartmentId;
        this.nsgId = nsgId;
    }

    @Exported
    public String getNsgCompartmentId() {
        return nsgCompartmentId == null ? "" : nsgCompartmentId;
    }

    @Exported
    public String getNsgId() {
        return nsgId;
    }

    public String toString() {
        return nsgId;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<BaremetalCloudNsgTemplate> {

        public ListBoxModel doFillNsgCompartmentIdItems(@QueryParameter @RelativePath("../..") String credentialsId,
                                                        @QueryParameter @RelativePath("../..") String maxAsyncThreads)
                                                        throws IOException, ServletException {
            ListBoxModel model = new ListBoxModel();
            model.add("<Select a NSG compartment>", "");

            if (credentialsId.isEmpty()) {
                return model;
            }

            try{
                BaremetalCloudClientFactory factory = SDKBaremetalCloudClientFactory.INSTANCE;
                BaremetalCloudClient client = factory.createClient(credentialsId, Integer.parseInt(maxAsyncThreads));
                Tenancy tenant = client.getTenant();
                model.add(tenant.getName(), tenant.getId());
                client.getCompartmentsList().stream()
                        .forEach(n -> model.add(n.getName(),n.getId()));
            }catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to get compartment list", e);
            }
            return model;
        }

        public ListBoxModel doFillNsgIdItems(@QueryParameter @RelativePath("../..") String credentialsId,
                                             @QueryParameter @RelativePath("../..") String maxAsyncThreads,
                                             @QueryParameter @RelativePath("..") String vcnCompartmentId,
                                             @QueryParameter String nsgCompartmentId) {
            ListBoxModel model = new ListBoxModel();
            model.add("<First select 'NSG compartment'>", "");

            if (credentialsId.isEmpty() || (nsgCompartmentId.isEmpty() && vcnCompartmentId.isEmpty())) {
                return model;
            }

            //This is needed for upgrade compatibility
            if (nsgCompartmentId.isEmpty() && !vcnCompartmentId.isEmpty()){
                nsgCompartmentId = vcnCompartmentId;
            }
            try {
                model.clear();
                BaremetalCloudClientFactory factory = SDKBaremetalCloudClientFactory.INSTANCE;
                BaremetalCloudClient client = factory.createClient(credentialsId, Integer.parseInt(maxAsyncThreads));
                client.getNsgIdsList(nsgCompartmentId).stream()
                        .forEach(n -> model.add(n.getDisplayName(),n.getId()));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to get NSG list", e);
            }
            return model;

        }

        @Override
        public String getDisplayName() {
            return "";
        }
    }
}
