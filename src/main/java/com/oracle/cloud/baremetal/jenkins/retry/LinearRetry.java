package com.oracle.cloud.baremetal.jenkins.retry;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class LinearRetry<T> implements Retry<T> {
    private static final Logger LOGGER = Logger.getLogger(LinearRetry.class.getName());

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final Duration DEFAULT_RETRY_DELAY = Duration.ofSeconds(30);
    private static final Duration DEFAULT_RETRY_TIMEOUT = Duration.ofSeconds(30);

    private final Callable<T> task;
    private final int maxRetries;
    private final Duration retryDelay;
    private final Duration retryTimeout;

    private int retries;

    public LinearRetry(Callable<T> task) {
        this(task, DEFAULT_MAX_RETRIES);
    }

    public LinearRetry(Callable<T> task, int maxRetries) {
        this(task, maxRetries, DEFAULT_RETRY_DELAY, DEFAULT_RETRY_TIMEOUT);
    }

    /**
     * @param task Callable that throws exception on error
     * @param maxRetries Positive number of attempts to call task before raising exception
     * @param retryDelay Time between attempts
     * @param retryTimeout Maximum task execution time after which it is considered failed
     */
    public LinearRetry(Callable<T> task, int maxRetries, Duration retryDelay, Duration retryTimeout) {
        this.task = task;
        this.maxRetries = maxRetries;
        this.retryDelay = retryDelay;
        this.retryTimeout = retryTimeout;
        this.retries = 0;
    }

    /**
     * @see com.oracle.cloud.baremetal.jenkins.retry.Retry#canRetry()
     */
    @Override
    public boolean canRetry() {
        return retries < maxRetries;
    }

    /**
     * @see com.oracle.cloud.baremetal.jenkins.retry.Retry#run()
     */
    @Override
    public T run() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        LOGGER.fine("Start retring for task: " + task.toString());

        while(canRetry()) {
            ++retries;
            LOGGER.fine("Retry attempt " + retries + " of " + maxRetries + " for task: " + task.toString());

            Future<T> handler = executor.submit(task);

            try {
                return handler.get(retryTimeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                handler.cancel(true);

                if (!canRetry()) {
                    LOGGER.info("All retry attempts for task: " + task.toString() + " failed");
                    throw e;
                }
            }

            Thread.sleep(retryDelay.toMillis());
        }

        return null;
    }
}
