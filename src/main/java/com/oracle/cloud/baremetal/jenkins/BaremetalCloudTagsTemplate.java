package com.oracle.cloud.baremetal.jenkins;


import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.oracle.cloud.baremetal.jenkins.client.BaremetalCloudClient;
import com.oracle.cloud.baremetal.jenkins.client.BaremetalCloudClientFactory;
import com.oracle.cloud.baremetal.jenkins.client.SDKBaremetalCloudClientFactory;
import com.oracle.cloud.baremetal.jenkins.credentials.BaremetalCloudCredentials;
import hudson.Extension;
import hudson.RelativePath;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BaremetalCloudTagsTemplate extends AbstractDescribableImpl<BaremetalCloudTagsTemplate> {
    private static final Logger LOGGER = Logger.getLogger(BaremetalCloud.class.getName());
    private final String namespace;
    private final String key;
    private final String value;

    @DataBoundConstructor
    public BaremetalCloudTagsTemplate(
            String namespace,
            String key,
            String value) {
        this.namespace = namespace;
        this.key = key;
        this.value = value;
    }

    @Exported
    public String getNamespace() {
        return namespace;
    }

    @Exported
    public String getKey() {
        return key;
    }

    @Exported
    public String getValue() {
        return value;
    }

    public String toString() {
        return key+":"+value;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<BaremetalCloudTagsTemplate> {

        public ListBoxModel doFillNamespaceItems(@QueryParameter @RelativePath("../..") String credentialsId,
                                             @QueryParameter @RelativePath("../..") String maxAsyncThreads,
                                             @QueryParameter @RelativePath("..") String compartmentId) {
            ListBoxModel model = new ListBoxModel();
            model.add("<Select a compartment>", "");

            if (credentialsId.isEmpty() || compartmentId.isEmpty()) {
                return model;
            }

            try {
                model.clear();
                model.add("None (add a free-form tag)","None");
                BaremetalCloudClientFactory factory = SDKBaremetalCloudClientFactory.INSTANCE;
                BaremetalCloudClient client = factory.createClient(credentialsId, Integer.parseInt(maxAsyncThreads));
                BaremetalCloudCredentials credentials = CredentialsMatchers.firstOrNull(
                        CredentialsProvider.lookupCredentials(BaremetalCloudCredentials.class, Jenkins.getInstance(), ACL.SYSTEM, Collections.<DomainRequirement>emptyList()),
                        CredentialsMatchers.withId(credentialsId));
                if (credentials != null){
                    compartmentId = credentials.getTenantId();
                }
                client.getTagNamespaces(compartmentId).stream()
                        .forEach(n -> model.add(n.getName(), n.getName()));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to get tag namespaces list", e);
            }
            return model;

        }

        @Override
        public String getDisplayName() {
            return "";
        }
    }
}
