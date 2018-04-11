package com.oracle.cloud.baremetal.jenkins.retry;

public interface Retry<T> {

    /**
     * Check that another retry can be attempted.
     *
     * @return true, if another retry is possible
     */
    boolean canRetry();

    /**
     * Start attempting to run provided task.
     *
     * @return task's return value
     * @throws Exception on task failure
     *          TimeoutException on task timeout
     */
    T run() throws Exception;

}
