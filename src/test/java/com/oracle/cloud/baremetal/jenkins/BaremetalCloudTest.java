package com.oracle.cloud.baremetal.jenkins;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.slaves.Cloud;

public class BaremetalCloudTest {

	@Rule
    public JenkinsRule r = new JenkinsRule();

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    @Ignore
    public void testConfigRoundtrip() throws Exception {
    	BaremetalCloud orig = new TestBaremetalCloud.Builder().cloudName("foo").build();
    	r.jenkins.clouds.add(orig);
        r.submit(r.createWebClient().goTo("configure").getFormByName("config"));

        Cloud actual = r.jenkins.clouds.iterator().next();
        r.assertEqualBeans(orig, actual, "fingerprint,apikey,passphrase,tenantId,userId");
    }

}
