package com.oracle.cloud.baremetal.jenkins;

import java.util.Arrays;
import java.util.List;

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import com.oracle.bmc.core.model.Instance;
import com.oracle.cloud.baremetal.jenkins.client.BaremetalCloudClient;

import hudson.model.Node;

public class BaremetalCloudInstanceMonitorUnitTest {
    @Rule
    public final BaremetalCloudMockery mockery = new BaremetalCloudMockery();

    static class TestBaremetalCloudInstanceMonitor extends BaremetalCloudInstanceMonitor {
        boolean removed;
        Node agent;

        TestBaremetalCloudInstanceMonitor(Node agent) {
            this.agent = agent;
        }

        @Override
        protected List<Node> getNodes() {
            return Arrays.asList(agent);
        }

        @Override
        protected void removeNode(BaremetalCloudAgent agent) {
            removed = true;
        }
    }

    private TestBaremetalCloudAgent newBaremetalCloudAgent(Instance.LifecycleState state, final boolean terminate) throws Exception {
        final BaremetalCloudClient client = mockery.mock(BaremetalCloudClient.class);
        mockery.checking(new Expectations() {{
            oneOf(client).getInstanceState("in"); will(returnValue(state));
            if (terminate) {
                oneOf(client).terminateInstance("in");
                oneOf(client).waitForInstanceTerminationToComplete("in");
            }
        }});
        TestBaremetalCloudAgent agent = new TestBaremetalCloudAgent.Builder()
                .instanceId("in")
                .stopOnIdle(false)
                .cloud(new TestBaremetalCloud.Builder().client(client).clock(new TestClock()).build())
                .build();
        return agent;
    }

    @Test
    public void testExecuteAlive() throws Exception {
        TestBaremetalCloudAgent agent = newBaremetalCloudAgent(Instance.LifecycleState.Running, false);
        TestBaremetalCloudInstanceMonitor monitor = new TestBaremetalCloudInstanceMonitor(agent);
        monitor.execute(null);
        Assert.assertFalse(monitor.removed);
    }

    @Test
    public void testExecuteNotAlive() throws Exception {
        TestBaremetalCloudAgent agent = newBaremetalCloudAgent(Instance.LifecycleState.Stopped, true);
        TestBaremetalCloudInstanceMonitor monitor = new TestBaremetalCloudInstanceMonitor(agent);
        monitor.execute(null);
        Assert.assertTrue(monitor.removed);
    }

    @Test
    public void testExecuteError() {
        TestBaremetalCloudAgent agent = new TestBaremetalCloudAgent.Builder().build();
        TestBaremetalCloudInstanceMonitor monitor = new TestBaremetalCloudInstanceMonitor(agent);
        monitor.execute(null);
        Assert.assertFalse(monitor.removed);
    }
}
