package com.oracle.cloud.baremetal.jenkins;

import java.io.IOException;
import java.security.UnrecoverableKeyException;
import java.util.Collection;
import java.util.Objects;

import hudson.model.Node;
import hudson.model.labels.LabelAtom;
import jenkins.bouncycastle.api.PEMEncodable;

public class TestBaremetalCloudAgentTemplate extends BaremetalCloudAgentTemplate {
    public static class Builder {
        String description;
        String numExecutors;
        Node.Mode mode;
        String labelString;
        String idleTerminationMinutes;
        int templateId;
        String remoteFS;
        String sshUser;
        String sshConnectTimeoutSeconds;
        String initScript;
        String startTimeoutSeconds;
        String initScriptTimeoutSeconds;


        String compartmentId;
        String availableDomain;
        String vcnId;
        String subnetId;
        String imageId;
        String shape;
        String sshPublickey;
        String sshPrivatekey;
        private boolean encryptSshPrivateKey = true;

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

        public Builder vcnId(String vcnId) {
            this.vcnId = vcnId;
            return this;
        }

        public Builder subnetId(String subnetId) {
            this.subnetId = subnetId;
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

        public Builder sshPublickey(String sshPublickey) {
            this.sshPublickey = sshPublickey;
            return this;
        }

        public Builder sshPrivatekey(String sshPrivatekey) {
            this.sshPrivatekey = sshPrivatekey;
            return this;
        }

        public Builder initScriptTimeoutSeconds(String initScriptTimeoutSeconds) {
            this.initScriptTimeoutSeconds = initScriptTimeoutSeconds;
            return this;
        }

        public Builder remoteFS(String remoteFS) {
            this.remoteFS = remoteFS;
            return this;
        }

        public Builder sshUser(String sshUser) {
            this.sshUser = sshUser;
            return this;
        }

        public Builder sshConnectTimeoutSeconds(String sshConnectTimeoutSeconds) {
            this.sshConnectTimeoutSeconds = sshConnectTimeoutSeconds;
            return this;
        }

        public Builder initScript(String initScript) {
            this.initScript = initScript;
            return this;
        }

        public Builder startTimeoutSeconds(String startTimeoutSeconds) {
            this.startTimeoutSeconds = startTimeoutSeconds;
            return this;
        }

        public Builder encryptSshPrivateKey(boolean encryptSshPrivateKey) {
            this.encryptSshPrivateKey  = encryptSshPrivateKey;
            return this;
        }

        public TestBaremetalCloudAgentTemplate build() {
            return new TestBaremetalCloudAgentTemplate(this);
        }
    }

    private boolean encryptSshPrivateKey;

    public TestBaremetalCloudAgentTemplate() {
        this(new Builder());
    }

    public TestBaremetalCloudAgentTemplate(Builder builder) {
        super(
                builder.compartmentId,
                builder.availableDomain,
                builder.vcnId,
                builder.subnetId,
                builder.imageId,
                builder.shape,
                builder.sshPublickey,
                builder.sshPrivatekey,
                builder.description,
                builder.remoteFS,
                builder.sshUser,
                builder.numExecutors,
                builder.mode,
                builder.labelString,
                builder.idleTerminationMinutes,
                builder.templateId,
                builder.initScript,
                builder.sshConnectTimeoutSeconds,
                builder.startTimeoutSeconds,
                builder.initScriptTimeoutSeconds);

        this.encryptSshPrivateKey  = builder.encryptSshPrivateKey;
    }

    @Override
    Collection<LabelAtom> parseLabels(String strings) {
        return BaremetalCloudTestUtils.parseLabels(strings);
    }

    @Override
    protected String getEncryptedValue(String str) {
        return encryptSshPrivateKey ? super.getEncryptedValue(str) : str;
    }

    public static class TestDescriptor extends DescriptorImpl {
        public static class Builder {
            BaremetalCloud.DescriptorImpl cloudDescriptor;
            PEMDecoder pemDecoder;

            public Builder cloudDescriptor(BaremetalCloud.DescriptorImpl cloudDescriptor) {
                this.cloudDescriptor = cloudDescriptor;
                return this;
            }

            public Builder pemDecoder(PEMDecoder pemDecoder) {
                this.pemDecoder = pemDecoder;
                return this;
            }

            public TestDescriptor build() {
                return new TestDescriptor(this);
            }
        }

        public interface PEMDecoder {
            PEMEncodable decode(String pem) throws UnrecoverableKeyException, IOException;
        }

        private final BaremetalCloud.DescriptorImpl cloudDescriptor;
        private final PEMDecoder pemDecoder;

        public TestDescriptor(Builder builder) {
            this.cloudDescriptor = builder.cloudDescriptor;
            this.pemDecoder = builder.pemDecoder;
        }

        @Override
        BaremetalCloud.DescriptorImpl getBaremetalCloudDescriptor() {
            return Objects.requireNonNull(cloudDescriptor, "cloudDescriptor");
        }

        @Override
        PEMEncodable decodePEM(String pem) throws UnrecoverableKeyException, IOException {
            return pemDecoder == null ? super.decodePEM(pem) : pemDecoder.decode(pem);
        }

    }
}
