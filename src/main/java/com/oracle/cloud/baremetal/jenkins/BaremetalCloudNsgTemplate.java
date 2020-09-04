package com.oracle.cloud.baremetal.jenkins;


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

import java.util.logging.Level;
import java.util.logging.Logger;

public class BaremetalCloudNsgTemplate extends AbstractDescribableImpl<BaremetalCloudNsgTemplate> {
    private static final Logger LOGGER = Logger.getLogger(BaremetalCloud.class.getName());
    private final String nsgId;

    @DataBoundConstructor
    public BaremetalCloudNsgTemplate(String nsgId) {
        this.nsgId = nsgId;
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

        public ListBoxModel doFillNsgIdItems(@QueryParameter @RelativePath("../..") String credentialsId,
                                             @QueryParameter @RelativePath("../..") String maxAsyncThreads,
                                             @QueryParameter @RelativePath("..") String vcnCompartmentId,
                                             @QueryParameter @RelativePath("..") String vcnId) {
            ListBoxModel model = new ListBoxModel();
            model.add("<Select a Virtual Cloud Network>", "");

            if (credentialsId.isEmpty() || vcnCompartmentId.isEmpty() || vcnId.isEmpty()) {
                return model;
            }

            try {
                model.clear();
                BaremetalCloudClientFactory factory = SDKBaremetalCloudClientFactory.INSTANCE;
                BaremetalCloudClient client = factory.createClient(credentialsId, Integer.parseInt(maxAsyncThreads));
                client.getNsgIdsList(vcnCompartmentId,vcnId).stream()
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
