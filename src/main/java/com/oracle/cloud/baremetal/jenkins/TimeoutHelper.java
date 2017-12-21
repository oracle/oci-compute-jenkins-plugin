package com.oracle.cloud.baremetal.jenkins;

public class TimeoutHelper {
    private final Clock clock;
    private final long beginNanos;
    private final long timeoutNanos;
    private final long sleepMillis;

    public TimeoutHelper(Clock clock, long timeoutNanos, long sleepMillis) {
        this.clock = clock;
        this.beginNanos = clock.nanoTime();
        this.timeoutNanos = timeoutNanos;
        this.sleepMillis = sleepMillis;
    }

    public boolean sleep() throws InterruptedException {
        if (timeoutNanos != 0) {
            long durationNanos = clock.nanoTime() - beginNanos;
            if (durationNanos >= timeoutNanos) {
                return false;
            }
        }

        clock.sleep(sleepMillis);
        return true;
    }
}
