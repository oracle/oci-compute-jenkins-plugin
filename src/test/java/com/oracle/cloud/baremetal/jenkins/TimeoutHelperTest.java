package com.oracle.cloud.baremetal.jenkins;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

public class TimeoutHelperTest {
    @Test
    public void test() throws Exception {
        TestClock clock = new TestClock();
        long beginNanos = clock.nanoTime;
        TimeoutHelper th = new TimeoutHelper(clock, TimeUnit.MILLISECONDS.toNanos(3), 2);
        Assert.assertTrue(th.sleep());
        Assert.assertEquals(2, TimeUnit.NANOSECONDS.toMillis(clock.nanoTime - beginNanos));
        Assert.assertTrue(th.sleep());
        Assert.assertEquals(4, TimeUnit.NANOSECONDS.toMillis(clock.nanoTime - beginNanos));
        Assert.assertFalse(th.sleep());
        Assert.assertEquals(4, TimeUnit.NANOSECONDS.toMillis(clock.nanoTime - beginNanos));
    }

    @Test
    public void testNoTimeout() throws Exception {
        TimeoutHelper th = new TimeoutHelper(new TestClock(), 0, Long.MAX_VALUE);
        Assert.assertTrue(th.sleep());
        Assert.assertTrue(th.sleep());
        Assert.assertTrue(th.sleep());
    }
}
