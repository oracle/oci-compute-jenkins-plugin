package com.oracle.cloud.baremetal.jenkins;

public interface Clock {
    Clock INSTANCE = new Clock() {
        @Override
        public long nanoTime() {
            return System.nanoTime();
        }

        @Override
        public void sleep(long millis) throws InterruptedException {
            Thread.sleep(millis);
        }
    };

    long nanoTime();
    void sleep(long millis) throws InterruptedException;
}
