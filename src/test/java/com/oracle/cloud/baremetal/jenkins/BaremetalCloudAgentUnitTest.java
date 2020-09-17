package com.oracle.cloud.baremetal.jenkins;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import com.oracle.bmc.core.model.Instance;
import com.oracle.cloud.baremetal.jenkins.client.BaremetalCloudClient;

import hudson.model.TaskListener;
import hudson.slaves.RetentionStrategy.Always;

public class BaremetalCloudAgentUnitTest {
    @Rule
    public final BaremetalCloudMockery mockery = new BaremetalCloudMockery();

    private static TaskListener newTerminateTaskListener() {
        return null;
    }

    @Test
    public void testTerminateFromReadyStatus() throws Exception {
        final BaremetalCloudClient client = mockery.mock(BaremetalCloudClient.class);
        mockery.checking(new Expectations() {{
            oneOf(client).terminateInstance("in");
            oneOf(client).waitForInstanceTerminationToComplete("in");
        }});
        TestBaremetalCloudAgent agent = new TestBaremetalCloudAgent.Builder()
                .instanceId("in")
                .stopOnIdle(false)
                .cloud(new TestBaremetalCloud.Builder().client(client).clock(new TestClock()).build())
                .build();
        agent._terminate(newTerminateTaskListener());
    }

    @Test
    public void testTerminateFromStoppedStatus() throws Exception {
        final BaremetalCloudClient client = mockery.mock(BaremetalCloudClient.class);
        mockery.checking(new Expectations() {{
            oneOf(client).terminateInstance("in");
            oneOf(client).waitForInstanceTerminationToComplete("in");
        }});

        TestBaremetalCloudAgent agent = new TestBaremetalCloudAgent.Builder()
                .instanceId("in")
                .stopOnIdle(false)
                .cloud(new TestBaremetalCloud.Builder().client(client).clock(new TestClock()).build())
                .build();
        agent._terminate(newTerminateTaskListener());
    }

    @Test(expected = IOException.class)
    public void testTerminateStopError() throws Exception {
        final BaremetalCloudClient client = mockery.mock(BaremetalCloudClient.class);
        mockery.checking(new Expectations() {{
            oneOf(client).terminateInstance("in"); will(throwException(new Exception("test")));
        }});
        TestBaremetalCloudAgent agent = new TestBaremetalCloudAgent.Builder()
                .instanceId("in")
                .stopOnIdle(false)
                .cloud(new TestBaremetalCloud.Builder().client(client).clock(new TestClock()).build())
                .build();
        agent._terminate(newTerminateTaskListener());
    }

    public void testTerminateCloudNotFound() throws Exception {
        TestBaremetalCloudAgent agent = new TestBaremetalCloudAgent.Builder()
                .cloudName(BaremetalCloud.NAME_PREFIX + "cn")
                .instanceId("in")
                .stopOnIdle(false)
                .build();
        agent._terminate(newTerminateTaskListener());
    }

    @Test(expected = IllegalStateException.class)
    public void testAliveCloudNotFound() throws Exception {
        TestBaremetalCloudAgent agent = new TestBaremetalCloudAgent.Builder()
                .instanceId("in")
                .build();
        agent.isAlive();
    }

    @Test
    public void testAlive() throws Exception {
        final BaremetalCloudClient client = mockery.mock(BaremetalCloudClient.class);
        mockery.checking(new Expectations() {{
                oneOf(client).getInstanceState("in"); will(returnValue(Instance.LifecycleState.Running));
                oneOf(client).getInstanceState("in"); will(returnValue(Instance.LifecycleState.Starting));
                oneOf(client).getInstanceState("in"); will(returnValue(Instance.LifecycleState.Provisioning));
                oneOf(client).getInstanceState("in"); will(returnValue(Instance.LifecycleState.Stopping));
                oneOf(client).getInstanceState("in"); will(returnValue(Instance.LifecycleState.Stopped));
                oneOf(client).getInstanceState("in"); will(returnValue(Instance.LifecycleState.Terminated));
                oneOf(client).getInstanceState("in"); will(returnValue(Instance.LifecycleState.Terminating));
        }});

        TestBaremetalCloudAgent agent = new TestBaremetalCloudAgent.Builder()
                .cloud(new TestBaremetalCloud.Builder().client(client).build())
                .instanceId("in")
                .build();
        Assert.assertTrue(agent.isAlive());
        Assert.assertTrue(agent.isAlive());
        Assert.assertTrue(agent.isAlive());
        Assert.assertFalse(agent.isAlive());
        Assert.assertFalse(agent.isAlive());
        Assert.assertFalse(agent.isAlive());
        Assert.assertFalse(agent.isAlive());
    }

    @Test
    public void testCreateRetentionStrategy() throws IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException,
            NoSuchMethodException,
            SecurityException {

        Method method = BaremetalCloudAgent.class.getDeclaredMethod("createRetentionStrategy", String.class);
        method.setAccessible(true);

        Assert.assertTrue(method.invoke(null, "42") instanceof BaremetalCloudRetentionStrategy);
        Assert.assertTrue(method.invoke(null, "1") instanceof BaremetalCloudRetentionStrategy);
        Assert.assertTrue(method.invoke(null, "-1") instanceof BaremetalCloudRetentionStrategy);
        Assert.assertTrue(method.invoke(null, "") instanceof Always);
        Assert.assertTrue(method.invoke(null, "   ") instanceof Always);
        Assert.assertTrue(method.invoke(null, "0") instanceof Always);
    }
}
