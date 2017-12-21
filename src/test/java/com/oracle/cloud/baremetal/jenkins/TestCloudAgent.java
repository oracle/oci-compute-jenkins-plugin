package com.oracle.cloud.baremetal.jenkins;

import java.io.IOException;

import hudson.model.TaskListener;
import hudson.model.Descriptor.FormException;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.AbstractCloudSlave;
import jenkins.model.Jenkins;

@SuppressWarnings("serial")
public class TestCloudAgent extends AbstractCloudSlave {
    public static class Builder {
        public TestCloudAgent build() {
            return (TestCloudAgent)Jenkins.XSTREAM2.fromXML("<slave class='" + TestCloudAgent.class.getName() + "'/>\n");
        }
    }

    protected TestCloudAgent() throws FormException, IOException {
        // Always create an instance by deserializing XML to avoid the Slave
        // constructor, which attempts to initialize nodeProperties using
        // Jenkins.getInstance(), which is null.
        super(null, null, null, null, null, null, null, null, null);
        throw new UnsupportedOperationException();
    }

    @Override
    protected Object readResolve() {
        // Override to avoid Slave.readResolve, which attempts to initialize
        // nodeProperties using Jenkins.getInstance(), which is null.
        return this;
    }

    @Override
    public AbstractCloudComputer<?> createComputer() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void _terminate(TaskListener listener) throws IOException, InterruptedException {
        throw new UnsupportedOperationException();
    }
}
