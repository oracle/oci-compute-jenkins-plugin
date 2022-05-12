package com.oracle.cloud.baremetal.jenkins;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import hudson.model.Node;
import hudson.model.labels.LabelAtom;


public class TestBaremetalCloudAgentTemplate extends BaremetalCloudAgentTemplate {
    public static class Builder {
        String description;
        String numExecutors;
        Node.Mode mode;
        String labelString;
        String idleTerminationMinutes;
        int templateId;
        String remoteFS;
        Boolean assignPublicIP;
        Boolean usePublicIP;
        String sshConnectTimeoutSeconds;
        String jenkinsAgentUser;
        String initScript;
        Boolean exportJenkinsEnvVars;
        String startTimeoutSeconds;
        String initScriptTimeoutSeconds;
        String instanceCap;
        String compartmentId;
        String availableDomain;
        String vcnCompartmentId;
        String vcnId;
        String subnetCompartmentId;
        String subnetId;
        List<BaremetalCloudNsgTemplate> nsgIds;
        String imageCompartmentId;
        String imageId;
        String shape;
        String sshCredentialsId;
        String ocpu;
        Boolean autoImageUpdate;
        Boolean stopOnIdle;
        List<BaremetalCloudTagsTemplate> tags;
        String instanceNamePrefix;
        String memoryInGBs;
        Boolean doNotDisable;
        String retryTimeoutMins;

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder numExecutors(String numExecutors) {
            this.numExecutors = numExecutors;
            return this;
        }

        public Builder numExecutors(int numExecutors) {
            return numExecutors(Integer.toString(numExecutors));
        }

        public Builder mode(Node.Mode mode) {
            this.mode = mode;
            return this;
        }

        public Builder labelString(String labelString) {
            this.labelString = labelString;
            return this;
        }

        public Builder idleTerminationMinutes(String idleTerminationMinutes) {
            this.idleTerminationMinutes = idleTerminationMinutes;
            return this;
        }

        public Builder templateId(int templateId) {
            this.templateId = templateId;
            return this;
        }

        public Builder compartmentId(String compartmentId) {
            this.compartmentId = compartmentId;
            return this;
        }

        public Builder availableDomain(String availableDomain) {
            this.availableDomain = availableDomain;
            return this;
        }

        public Builder vcnCompartmentId(String vcnCompartmentId) {
            this.vcnCompartmentId = vcnCompartmentId;
            return this;
        }

        public Builder vcnId(String vcnId) {
            this.vcnId = vcnId;
            return this;
        }

        public Builder subnetCompartmentId(String subnetCompartmentId) {
            this.subnetCompartmentId = subnetCompartmentId;
            return this;
        }

        public Builder subnetId(String subnetId) {
            this.subnetId = subnetId;
            return this;
        }

        public Builder assignPublicIP(Boolean assignPublicIP) {
            this.assignPublicIP = assignPublicIP;
            return this;
        }

        public Builder usePublicIP(Boolean usePublicIP) {
            this.usePublicIP = usePublicIP;
            return this;
        }

        public Builder imageCompartmentId(String imageCompartmentId) {
            this.imageCompartmentId = imageCompartmentId;
            return this;
        }

        public Builder imageId(String imageId) {
            this.imageId = imageId;
            return this;
        }

        public Builder shape(String shape) {
            this.shape = shape;
            return this;
        }

        public Builder initScriptTimeoutSeconds(String initScriptTimeoutSeconds) {
            this.initScriptTimeoutSeconds = initScriptTimeoutSeconds;
            return this;
        }

        public Builder retryTimeoutMins(String retryTimeoutMins) {
            this.retryTimeoutMins = retryTimeoutMins;
            return this;
        }

        public Builder instanceCap(String instanceCap) {
            this.instanceCap = instanceCap;
            return this;
        }

        public Builder ocpu(String ocpu) {
            this.ocpu = ocpu;
            return this;
        }

        public Builder autoImageUpdate(Boolean autoImageUpdate) {
            this.autoImageUpdate = autoImageUpdate;
            return this;
        }

        public Builder nsgIds(List<BaremetalCloudNsgTemplate> nsgIds) {
            this.nsgIds = nsgIds;
            return this;
        }

        public Builder remoteFS(String remoteFS) {
            this.remoteFS = remoteFS;
            return this;
        }

        public Builder sshConnectTimeoutSeconds(String sshConnectTimeoutSeconds) {
            this.sshConnectTimeoutSeconds = sshConnectTimeoutSeconds;
            return this;
        }

        public Builder jenkinsAgentUser(String jenkinsAgentUser) {
            this.jenkinsAgentUser = jenkinsAgentUser;
            return this;
	}

        public Builder initScript(String initScript) {
            this.initScript = initScript;
            return this;
        }

        public Builder exportJenkinsEnvVars(Boolean exportJenkinsEnvVars) {
            this.exportJenkinsEnvVars = exportJenkinsEnvVars;
            return this;
        }
        public Builder startTimeoutSeconds(String startTimeoutSeconds) {
            this.startTimeoutSeconds = startTimeoutSeconds;
            return this;
        }

        public Builder stopOnIdle(Boolean stopOnIdle) {
            this.stopOnIdle = stopOnIdle;
            return this;
        }

        public Builder doNotDisable(Boolean doNotDisable) {
            this.doNotDisable = doNotDisable;
            return this;
        }

        public Builder tags(List<BaremetalCloudTagsTemplate> tags) {
            this.tags = tags;
            return this;
        }

        public Builder instanceNamePrefix(String instanceNamePrefix) {
            this.instanceNamePrefix = instanceNamePrefix;
            return this;
        }

        public Builder memoryInGBs(String memoryInGBs) {
            this.memoryInGBs = memoryInGBs;
            return this;
        }

        public TestBaremetalCloudAgentTemplate build() {
            return new TestBaremetalCloudAgentTemplate(this);
        }
    }

    public TestBaremetalCloudAgentTemplate() {
        this(new Builder());
    }

    public TestBaremetalCloudAgentTemplate(Builder builder) {
        super(
                builder.compartmentId,
                builder.availableDomain,
                builder.vcnCompartmentId,
                builder.vcnId,
                builder.subnetCompartmentId,
                builder.subnetId,
                builder.nsgIds,
                builder.imageCompartmentId,
                builder.imageId,
                builder.shape,
                builder.sshCredentialsId,
                builder.description,
                builder.remoteFS,
                builder.assignPublicIP,
                builder.usePublicIP,
                builder.numExecutors,
                builder.mode,
                builder.labelString,
                builder.idleTerminationMinutes,
                builder.templateId,
	        builder.jenkinsAgentUser,
                builder.initScript,
                builder.exportJenkinsEnvVars,
                builder.sshConnectTimeoutSeconds,
                builder.startTimeoutSeconds,
                builder.initScriptTimeoutSeconds,
                builder.instanceCap,
                builder.ocpu,
                builder.autoImageUpdate,
                builder.stopOnIdle,
                builder.tags,
                builder.instanceNamePrefix,
                builder.memoryInGBs,
                builder.doNotDisable,
                builder.retryTimeoutMins);

    }

    @Override
    Collection<LabelAtom> parseLabels(String strings) {
        return BaremetalCloudTestUtils.parseLabels(strings);
    }

    public static class TestDescriptor extends BaremetalCloudAgentTemplate.DescriptorImpl {
        public static class Builder {
            BaremetalCloud.DescriptorImpl cloudDescriptor;

            public Builder cloudDescriptor(BaremetalCloud.DescriptorImpl cloudDescriptor) {
                this.cloudDescriptor = cloudDescriptor;
                return this;
            }

            public TestDescriptor build() {
                return new TestDescriptor(this);
            }
        }

        private final BaremetalCloud.DescriptorImpl cloudDescriptor;

        public TestDescriptor(Builder builder) {
            this.cloudDescriptor = builder.cloudDescriptor;
            //this.pemDecoder = builder.pemDecoder;
        }

        @Override
        BaremetalCloud.DescriptorImpl getBaremetalCloudDescriptor() {
            return Objects.requireNonNull(cloudDescriptor, "cloudDescriptor");
        }

    }
}
