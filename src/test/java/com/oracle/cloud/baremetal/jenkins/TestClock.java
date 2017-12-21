package com.oracle.cloud.baremetal.jenkins;

import java.util.concurrent.TimeUnit;

public class TestClock implements Clock {
    // Initialize to MAX_VALUE to try to detect wraparound bugs: callers should
    // use now-begin>=duration rather than now>=begin+duration.
    public long nanoTime = Long.MAX_VALUE;

    @Override
    public long nanoTime() {
        return nanoTime;
    }

    @Override
    public void sleep(long millis) {
        nanoTime += TimeUnit.MILLISECONDS.toNanos(millis);
    }
}
