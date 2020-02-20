package com.oracle.cloud.baremetal.jenkins;

import java.util.concurrent.Callable;

import com.oracle.cloud.baremetal.jenkins.retry.Retry;

/**
 * Dummy Retry for testing purposes that just runs the task, but does not retry anything.
 */
public class TestRetry<T> implements Retry<T> {
    private final Callable<T> task;

    public TestRetry(Callable<T> task) {
        this.task = task;
    }

    @Override
    public boolean canRetry() {
        return true;
    }

    @Override
    public T run() throws Exception {
        return task.call();
    }
}
