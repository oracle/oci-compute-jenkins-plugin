package com.oracle.cloud.baremetal.jenkins;

import org.junit.Assert;
import org.junit.Test;

import hudson.Util;

public class JenkinsUtilUnitTest {
    @Test
    public void testUnescape() {
        for (char c1 = 0; c1 < 256; c1++) {
            for (char c2 = 0; c2 < 256; c2++) {
                String s = new String(new char[] { c1, c2 });
                Assert.assertEquals(s, JenkinsUtil.unescape(Util.escape(s)));
            }
        }
    }
}
