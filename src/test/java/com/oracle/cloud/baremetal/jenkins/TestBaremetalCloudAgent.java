package com.oracle.cloud.baremetal.jenkins;

import java.io.IOException;

import hudson.model.Descriptor.FormException;
import hudson.slaves.SlaveComputer;
import jenkins.model.Jenkins;

@SuppressWarnings("serial")
public class TestBaremetalCloudAgent extends BaremetalCloudAgent {

    public static class Builder {
        private String numExecutors;
        private String cloudName;
        private String instanceId;
        private Boolean stopOnIdle;

        private BaremetalCloud cloud;

        public Builder numExecutors(String numExecutors) {
            this.numExecutors = numExecutors;
            return this;
        }

        public Builder numExecutors(int numExecutors) {
            return numExecutors(String.valueOf(numExecutors));
        }

        public Builder cloudName(String cloudName) {
            this.cloudName = cloudName;
            return this;
        }

        public Builder instanceId(String instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public Builder stopOnIdle(Boolean stopOnIdle) {
            this.stopOnIdle = stopOnIdle;
            return this;
        }
        public Builder cloud(BaremetalCloud cloud) {
            this.cloud = cloud;
            return this;
        }

        private void appendXml(StringBuilder xml, String name, Object value) {
            if (value != null) {
                xml.append("  <").append(name).append(">").append(value).append("</").append(name).append(">\n");
            }
        }

        public TestBaremetalCloudAgent build() {
            StringBuilder xml = new StringBuilder();
            xml.append("<slave class='").append(TestBaremetalCloudAgent.class.getName()).append("'>\n");
            appendXml(xml, "numExecutors", numExecutors);
            appendXml(xml, "cloudName", cloudName);
            appendXml(xml, "instanceId", instanceId);
            appendXml(xml, "stopOnIdle", stopOnIdle);
            xml.append("</slave>");

            TestBaremetalCloudAgent agent = (TestBaremetalCloudAgent)Jenkins.XSTREAM2.fromXML(xml.toString());
            agent.cloud = cloud;
            return agent;
        }
    }

    private BaremetalCloud cloud;

    protected TestBaremetalCloudAgent() throws FormException, IOException {
        // Always create an instance by deserializing XML to avoid the Slave
        // constructor, which attempts to initialize nodeProperties using
        // Jenkins.getInstance(), which is null.
        super(
                null, // name
                null, // template
                null, // cloudName
                null, // instanceId
                null); // host
        throw new UnsupportedOperationException();
    }

    @Override
    protected Object readResolve() {
        // Override to avoid Slave.readResolve, which attempts to initialize
        // nodeProperties using Jenkins.getInstance(), which is null.
        return this;
    }

    @Override
    public BaremetalCloud getCloud() {
        return cloud;
    }

    @Override
    public SlaveComputer getComputer() {
        return null;
    }
}
