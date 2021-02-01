package com.oracle.cloud.baremetal.jenkins;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.Node;
import hudson.model.labels.LabelAtom;
import hudson.util.FormValidation;
import jenkins.bouncycastle.api.SecurityProviderInitializer;

public class BaremetalCloudAgentTemplateUnitTest {
    static { TestMessages.init(); }
    static { new SecurityProviderInitializer(); }

    @Rule
    public final BaremetalCloudMockery mockery = new BaremetalCloudMockery();

    /*
     * Prevents:
     *   "java.lang.IllegalStateException: cannot initialize confidential key store until Jenkins has started"
     *
     * which was introduced in the unit tests with usage of hudson.util.Secret for SSH key storage.
     */
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void testGetDisplayName() {
        Assert.assertEquals("null", new TestBaremetalCloudAgentTemplate().getDisplayName());
        Assert.assertEquals("d", new TestBaremetalCloudAgentTemplate.Builder().description("d").build().getDisplayName());
    }

    @Test
    public void testGetDescription() {
        Assert.assertNull(new TestBaremetalCloudAgentTemplate().getDescription());
        Assert.assertEquals("d", new TestBaremetalCloudAgentTemplate.Builder().description("d").build().getDescription());
    }

    @Test
    public void testGetCompartmentId() {
        Assert.assertNull(new TestBaremetalCloudAgentTemplate().getCompartmentId());
        Assert.assertEquals("", new TestBaremetalCloudAgentTemplate.Builder().compartmentId("").build().getCompartmentId());
        Assert.assertEquals("123", new TestBaremetalCloudAgentTemplate.Builder().compartmentId("123").build().getCompartmentId());
    }

    @Test
    public void testGetVcnId() {
        Assert.assertNull(new TestBaremetalCloudAgentTemplate().getVcn());
        Assert.assertEquals("", new TestBaremetalCloudAgentTemplate.Builder().vcnId("").build().getVcn());
        Assert.assertEquals("123", new TestBaremetalCloudAgentTemplate.Builder().vcnId("123").build().getVcn());
    }

    @Test
    public void testSubnetId() {
        Assert.assertNull(new TestBaremetalCloudAgentTemplate().getSubnet());
        Assert.assertEquals("", new TestBaremetalCloudAgentTemplate.Builder().subnetId("").build().getSubnet());
        Assert.assertEquals("123", new TestBaremetalCloudAgentTemplate.Builder().subnetId("123").build().getSubnet());
    }

    @Test
    public void testGetRemoteFS() {
        Assert.assertNull(new TestBaremetalCloudAgentTemplate().getRemoteFS());
        Assert.assertEquals("rfs", new TestBaremetalCloudAgentTemplate.Builder().remoteFS("rfs").build().getRemoteFS());
    }

    @Test
    public void testGetNumExecutorsValue() {
        int defaultNumExecutors = BaremetalCloudAgentTemplate.DescriptorImpl.getDefaultNumExecutors();
        Assert.assertEquals(defaultNumExecutors, new TestBaremetalCloudAgentTemplate().getNumExecutorsValue());
        Assert.assertEquals(defaultNumExecutors, new TestBaremetalCloudAgentTemplate.Builder().numExecutors("").build().getNumExecutorsValue());
        Assert.assertEquals(defaultNumExecutors, new TestBaremetalCloudAgentTemplate.Builder().numExecutors(-1).build().getNumExecutorsValue());
        Assert.assertEquals(defaultNumExecutors, new TestBaremetalCloudAgentTemplate.Builder().numExecutors(0).build().getNumExecutorsValue());
        Assert.assertEquals(2, new TestBaremetalCloudAgentTemplate.Builder().numExecutors(2).build().getNumExecutorsValue());
        Assert.assertEquals(defaultNumExecutors, new TestBaremetalCloudAgentTemplate.Builder().numExecutors("x").build().getNumExecutorsValue());
    }

    @Test
    public void testGetMode() {
        Assert.assertNull(new TestBaremetalCloudAgentTemplate().getMode());
        Assert.assertSame(Node.Mode.NORMAL, new TestBaremetalCloudAgentTemplate.Builder().mode(Node.Mode.NORMAL).build().getMode());
    }

    @Test
    public void testGetLabelString() {
        Assert.assertNull(new TestBaremetalCloudAgentTemplate().getLabelString());
        Assert.assertEquals("a", new TestBaremetalCloudAgentTemplate.Builder().labelString("a").build().getLabelString());
    }

    @Test
    public void testGetLabelAtoms() {
        Assert.assertEquals(new LinkedHashSet<>(), new LinkedHashSet<>(new TestBaremetalCloudAgentTemplate().getLabelAtoms()));

        BaremetalCloudAgentTemplate t = new TestBaremetalCloudAgentTemplate.Builder().labelString("a b").build();
        LinkedHashSet<LabelAtom> expected = new LinkedHashSet<>(Arrays.asList(new LabelAtom("a"), new LabelAtom("b")));
        Assert.assertEquals(expected, new LinkedHashSet<>(t.getLabelAtoms()));
        Assert.assertEquals(expected, new LinkedHashSet<>(t.getLabelAtoms()));
    }

    @Test
    public void testGetIdleTerminationMinutes() {
        Assert.assertNull(new TestBaremetalCloudAgentTemplate().getIdleTerminationMinutes());
        Assert.assertEquals("2", new TestBaremetalCloudAgentTemplate.Builder().idleTerminationMinutes("2").build().getIdleTerminationMinutes());
    }

    @Test
    public void testGetNextTemplateId() {
        Assert.assertEquals(0, new TestBaremetalCloudAgentTemplate().getTemplateId());
        Assert.assertEquals(1, new TestBaremetalCloudAgentTemplate.Builder().templateId(1).build().getTemplateId());
    }

    @Test
    public void testGetShape() throws Exception {
        Assert.assertNull(new TestBaremetalCloudAgentTemplate().getShape());
        Assert.assertEquals("sn", new TestBaremetalCloudAgentTemplate.Builder().shape("sn").build().getShape());
    }

    @Test
    public void testGetAvailableDomain() throws Exception {
        Assert.assertNull(new TestBaremetalCloudAgentTemplate().getAvailableDomain());
        Assert.assertEquals("sln", new TestBaremetalCloudAgentTemplate.Builder().availableDomain("sln").build().getAvailableDomain());
    }

    @Test
    public void testGetImageId() throws Exception {
        Assert.assertNull(new TestBaremetalCloudAgentTemplate().getImage());
        Assert.assertEquals("iln", new TestBaremetalCloudAgentTemplate.Builder().imageId("iln").build().getImage());
    }

    @Test
    public void testGetSshConnectTimeoutSeconds() {
        Assert.assertNull(new TestBaremetalCloudAgentTemplate().getSshConnectTimeoutSeconds());
        Assert.assertEquals("", new TestBaremetalCloudAgentTemplate.Builder().sshConnectTimeoutSeconds("").build().getSshConnectTimeoutSeconds());
        Assert.assertEquals("x", new TestBaremetalCloudAgentTemplate.Builder().sshConnectTimeoutSeconds("x").build().getSshConnectTimeoutSeconds());
        Assert.assertEquals("-1", new TestBaremetalCloudAgentTemplate.Builder().sshConnectTimeoutSeconds("-1").build().getSshConnectTimeoutSeconds());
        Assert.assertEquals("0", new TestBaremetalCloudAgentTemplate.Builder().sshConnectTimeoutSeconds("0").build().getSshConnectTimeoutSeconds());
        Assert.assertEquals("1", new TestBaremetalCloudAgentTemplate.Builder().sshConnectTimeoutSeconds("1").build().getSshConnectTimeoutSeconds());
    }

    @Test
    public void testGetInitScript() {
        Assert.assertNull(new TestBaremetalCloudAgentTemplate().getInitScript());
        Assert.assertEquals("is", new TestBaremetalCloudAgentTemplate.Builder().initScript("is").build().getInitScript());
    }

    @Test
    public void testGetStartTimeoutSeconds() {
        Assert.assertNull(new TestBaremetalCloudAgentTemplate().getStartTimeoutSeconds());
        Assert.assertEquals("", new TestBaremetalCloudAgentTemplate.Builder().startTimeoutSeconds("").build().getStartTimeoutSeconds());
        Assert.assertEquals("x", new TestBaremetalCloudAgentTemplate.Builder().startTimeoutSeconds("x").build().getStartTimeoutSeconds());
        Assert.assertEquals("-1", new TestBaremetalCloudAgentTemplate.Builder().startTimeoutSeconds("-1").build().getStartTimeoutSeconds());
        Assert.assertEquals("0", new TestBaremetalCloudAgentTemplate.Builder().startTimeoutSeconds("0").build().getStartTimeoutSeconds());
        Assert.assertEquals("1", new TestBaremetalCloudAgentTemplate.Builder().startTimeoutSeconds("1").build().getStartTimeoutSeconds());
    }

    @Test
    public void testGetStartTimeoutNanos() {
        long defaultNanos = TimeUnit.SECONDS.toNanos(900);
        Assert.assertEquals(defaultNanos, new TestBaremetalCloudAgentTemplate().getStartTimeoutNanos());
        Assert.assertEquals(defaultNanos, new TestBaremetalCloudAgentTemplate.Builder().startTimeoutSeconds("").build().getStartTimeoutNanos());
        Assert.assertEquals(defaultNanos, new TestBaremetalCloudAgentTemplate.Builder().startTimeoutSeconds("x").build().getStartTimeoutNanos());
        Assert.assertEquals(defaultNanos, new TestBaremetalCloudAgentTemplate.Builder().startTimeoutSeconds("-1").build().getStartTimeoutNanos());
        Assert.assertEquals(0, new TestBaremetalCloudAgentTemplate.Builder().startTimeoutSeconds("0").build().getStartTimeoutNanos());
        Assert.assertEquals(TimeUnit.SECONDS.toNanos(1), new TestBaremetalCloudAgentTemplate.Builder().startTimeoutSeconds("1").build().getStartTimeoutNanos());
    }

    @Test
    public void testGetDefaultNumExecutors() {
        Assert.assertEquals(1, BaremetalCloudAgentTemplate.DescriptorImpl.getDefaultNumExecutors());
    }

    @Test
    public void testDoCheckNumExecutors() {
        Assert.assertEquals(FormValidation.Kind.OK, new BaremetalCloudAgentTemplate.DescriptorImpl().doCheckNumExecutors("1").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new BaremetalCloudAgentTemplate.DescriptorImpl().doCheckNumExecutors(null).kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new BaremetalCloudAgentTemplate.DescriptorImpl().doCheckNumExecutors("").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new BaremetalCloudAgentTemplate.DescriptorImpl().doCheckNumExecutors("x").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new BaremetalCloudAgentTemplate.DescriptorImpl().doCheckNumExecutors("0").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new BaremetalCloudAgentTemplate.DescriptorImpl().doCheckNumExecutors("-1").kind);
    }

    @Test
    public void testDoCheckLabelString() {
        Assert.assertEquals(FormValidation.Kind.OK, new BaremetalCloudAgentTemplate.DescriptorImpl().doCheckLabelString(null, Node.Mode.NORMAL).kind);
        Assert.assertEquals(FormValidation.Kind.WARNING, new BaremetalCloudAgentTemplate.DescriptorImpl().doCheckLabelString(null, Node.Mode.EXCLUSIVE).kind);

        Assert.assertEquals(FormValidation.Kind.OK, new BaremetalCloudAgentTemplate.DescriptorImpl().doCheckLabelString("", Node.Mode.NORMAL).kind);
        Assert.assertEquals(FormValidation.Kind.WARNING, new BaremetalCloudAgentTemplate.DescriptorImpl().doCheckLabelString("", Node.Mode.EXCLUSIVE).kind);

        Assert.assertEquals(FormValidation.Kind.OK, new BaremetalCloudAgentTemplate.DescriptorImpl().doCheckLabelString(" ", Node.Mode.NORMAL).kind);
        Assert.assertEquals(FormValidation.Kind.WARNING, new BaremetalCloudAgentTemplate.DescriptorImpl().doCheckLabelString(" ", Node.Mode.EXCLUSIVE).kind);

        Assert.assertEquals(FormValidation.Kind.OK, new BaremetalCloudAgentTemplate.DescriptorImpl().doCheckLabelString("a", Node.Mode.NORMAL).kind);
        Assert.assertEquals(FormValidation.Kind.OK, new BaremetalCloudAgentTemplate.DescriptorImpl().doCheckLabelString("a", Node.Mode.EXCLUSIVE).kind);
    }

    @Test
    public void testDoCheckIdleTerminationMinutes() {
        Assert.assertEquals(FormValidation.Kind.OK, new BaremetalCloudAgentTemplate.DescriptorImpl().doCheckIdleTerminationMinutes(null).kind);
        Assert.assertEquals(FormValidation.Kind.OK, new BaremetalCloudAgentTemplate.DescriptorImpl().doCheckIdleTerminationMinutes("").kind);
        Assert.assertEquals(FormValidation.Kind.OK, new BaremetalCloudAgentTemplate.DescriptorImpl().doCheckIdleTerminationMinutes(" ").kind);
        Assert.assertEquals(FormValidation.Kind.OK, new BaremetalCloudAgentTemplate.DescriptorImpl().doCheckIdleTerminationMinutes("1").kind);
        Assert.assertEquals(FormValidation.Kind.OK, new BaremetalCloudAgentTemplate.DescriptorImpl().doCheckIdleTerminationMinutes("0").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new BaremetalCloudAgentTemplate.DescriptorImpl().doCheckIdleTerminationMinutes("-1").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new BaremetalCloudAgentTemplate.DescriptorImpl().doCheckIdleTerminationMinutes("x").kind);
    }

    @Test
    public void testDoCheckSshConnectTimeoutSeconds() {
        Assert.assertEquals(FormValidation.Kind.ERROR, new TestBaremetalCloudAgentTemplate.DescriptorImpl().doCheckSshConnectTimeoutSeconds(null).kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new TestBaremetalCloudAgentTemplate.DescriptorImpl().doCheckSshConnectTimeoutSeconds("").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new TestBaremetalCloudAgentTemplate.DescriptorImpl().doCheckSshConnectTimeoutSeconds("x").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new TestBaremetalCloudAgentTemplate.DescriptorImpl().doCheckSshConnectTimeoutSeconds("-1").kind);
        Assert.assertEquals(FormValidation.Kind.OK, new TestBaremetalCloudAgentTemplate.DescriptorImpl().doCheckSshConnectTimeoutSeconds("0").kind);
        Assert.assertEquals(FormValidation.Kind.OK, new TestBaremetalCloudAgentTemplate.DescriptorImpl().doCheckSshConnectTimeoutSeconds("1").kind);
    }

    // Randomly generated with: ssh-keygen -t rsa -b 768
    private static final String TEST_PRIVATE_KEY_PEM =
            "-----BEGIN RSA PRIVATE KEY-----\n" +
            "MIIBywIBAAJhAKmEwj68Ssf3v5tkolZzwANvDs/PDGBSxC8A1FqsXQ+hrGa/j/JB\n" +
            "/R+xXPSvr/a1KWaPilXqhALt8+7LIfg4TbUxdhXVdVJupha7JwBUCBH87DFVQzc5\n" +
            "wqJJ7J6iIGZNNwIDAQABAmARLCi9UDfHIBrh9ATZ+ynVbzex54iabWgAVvYsJU/c\n" +
            "GIWtdvRvFy48OqxvASkzNdDMlI5QxpD92cfoykxFd/U4lPjcgKpInm7CkGVvJFtC\n" +
            "Qr2MG87iILNAuQWHwlljyuECMQDcJzo9u1+ue9wlcBUjhtfo7nCKxoEg9xsTRIWn\n" +
            "JnrWiGw6oyy/AIKxw/pSxN1d3DECMQDFHuYKp3VKMo4kz+J2XdXeYKg/iZ8ebeNu\n" +
            "id8tiTtiUtgoAA5znMwM5JAhh7EALecCMQCjunjSGFwcg/lBzo2qEkrY7Ru92cuH\n" +
            "HL+CIN/VZATPMD5tjZVlp5eLZVjx3X9UosECMQCjf/CBD8r6gxphsEh/s29MZ1HG\n" +
            "eckQfUcyjYsfAv/NmzeNXhaekISzgPWHyjvnESsCMD0vJJ8DsP01Zi4CGnN3Cw1t\n" +
            "D5T/rai8O9b0G4JOOXRTjv8v68ajYDotjolRwTCULw==\n" +
            "-----END RSA PRIVATE KEY-----\n";
    // Extracted from private key: ssh-keygen -y -f privkey.pem
    private static final String TEST_PUBLIC_KEY_SSH = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAAYQCphMI+vErH97+bZKJWc8ADbw7PzwxgUsQvANRarF0Poaxmv4/yQf0fsVz0r6/2tSlmj4pV6oQC7fPuyyH4OE21MXYV1XVSbqYWuycAVAgR/OwxVUM3OcKiSeyeoiBmTTc=";

    @Test
    public void testDoCheckStartTimeoutSeconds() {
        Assert.assertEquals(FormValidation.Kind.ERROR, new TestBaremetalCloudAgentTemplate.DescriptorImpl().doCheckStartTimeoutSeconds(null).kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new TestBaremetalCloudAgentTemplate.DescriptorImpl().doCheckStartTimeoutSeconds("").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new TestBaremetalCloudAgentTemplate.DescriptorImpl().doCheckStartTimeoutSeconds("x").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new TestBaremetalCloudAgentTemplate.DescriptorImpl().doCheckStartTimeoutSeconds("-1").kind);
        Assert.assertEquals(FormValidation.Kind.OK, new TestBaremetalCloudAgentTemplate.DescriptorImpl().doCheckStartTimeoutSeconds("0").kind);
        Assert.assertEquals(FormValidation.Kind.OK, new TestBaremetalCloudAgentTemplate.DescriptorImpl().doCheckStartTimeoutSeconds("1").kind);
    }

    @Test
    public void testDisableCause() {
        TestBaremetalCloudAgentTemplate template = new TestBaremetalCloudAgentTemplate();
        Assert.assertNull(template.getDisableCause());

        for (int i = 0; i < BaremetalCloudAgentTemplate.FAILURE_COUNT_LIMIT - 1; i++) {
            template.increaseFailureCount("error");
        }
        Assert.assertNull(template.getDisableCause());

        template.increaseFailureCount("error");
        Assert.assertNotNull(template.getDisableCause());
    }

    @Test
    public void testResetFailureCause() {
        TestBaremetalCloudAgentTemplate template = new TestBaremetalCloudAgentTemplate();
        Assert.assertNull(template.getDisableCause());

        for (int i = 0; i < BaremetalCloudAgentTemplate.FAILURE_COUNT_LIMIT - 1; i++) {
            template.increaseFailureCount("error");
        }
        template.resetFailureCount();
        template.increaseFailureCount("error");
        Assert.assertNull(template.getDisableCause());

        for (int i = 0; i < BaremetalCloudAgentTemplate.FAILURE_COUNT_LIMIT; i++) {
            template.increaseFailureCount("error");
        }
        Assert.assertNotNull(template.getDisableCause());
        template.resetFailureCount();
        Assert.assertNull(template.getDisableCause());
    }
}
