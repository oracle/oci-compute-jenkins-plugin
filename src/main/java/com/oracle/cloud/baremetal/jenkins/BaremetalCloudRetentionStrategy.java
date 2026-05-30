package com.oracle.cloud.baremetal.jenkins;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.concurrent.GuardedBy;

import hudson.model.Executor;
import hudson.model.ExecutorListener;
import hudson.model.Queue;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.CloudRetentionStrategy;
import hudson.slaves.OfflineCause.SimpleOfflineCause;

public class BaremetalCloudRetentionStrategy extends CloudRetentionStrategy implements ExecutorListener {
    private static final Logger LOGGER = Logger.getLogger(BaremetalCloud.class.getName());
    private final int idleMinutes;

    public BaremetalCloudRetentionStrategy(int idleMinutes) {
        super(idleMinutes > 0 ? idleMinutes : Integer.MAX_VALUE);
        this.idleMinutes = idleMinutes;
    }

    /**
     * Prevent {@link CloudRetentionStrategy} from terminating the Computer if it is
     * in offline state set by user (e.g. from Web UI) or by another plugin.
     * Also prevent idle termination when idleMinutes is 0 (keep-forever mode).
     */
    @Override
    @GuardedBy("hudson.model.Queue.lock")
    public long check(final AbstractCloudComputer c) {
        if (c.isOffline() && c.getOfflineCause() instanceof SimpleOfflineCause) {
            LOGGER.fine(c.getDisplayName() + ": Node is set temporarily offline - will not terminate");
            return 1;
        }
        if (idleMinutes == 0) {
            return 1;
        }
        return super.check(c);
    }

    @Override
    public void taskAccepted(Executor executor, Queue.Task task) {
        BaremetalCloudComputer computer = (BaremetalCloudComputer) executor.getOwner();
        BaremetalCloudAgent agent = computer.getNode();
        if (agent != null) {
            int maxTotalUses = agent.maxTotalUses;
            if (maxTotalUses <= 0) {
                LOGGER.fine("maxTotalUses set to unlimited (" + maxTotalUses + ") for agent " + agent.getInstanceId());
            } else if (maxTotalUses == 1) {
                LOGGER.info("maxTotalUses drained - suspending agent " + agent.getInstanceId());
                computer.setAcceptingTasks(false);
            } else {
                agent.maxTotalUses = maxTotalUses - 1;
                LOGGER.info("Agent " + agent.getInstanceId() + " has " + agent.maxTotalUses + " builds left");
            }
        }
    }

    @Override
    public void taskCompleted(Executor executor, Queue.Task task, long durationMS) {
        taskCompletedWithProblems(executor, task, durationMS, null);
    }

    @Override
    public void taskCompletedWithProblems(Executor executor, Queue.Task task, long durationMS, Throwable problems) {
        BaremetalCloudComputer computer = (BaremetalCloudComputer) executor.getOwner();
        BaremetalCloudAgent agent = computer.getNode();
        if (agent != null) {
            if (computer.countBusy() <= 1 && !computer.isAcceptingTasks()) {
                LOGGER.info("Agent " + agent.getInstanceId() + " is terminated due to maxTotalUses ("
                        + agent.maxTotalUses + ")");
                try {
                    agent.terminate();
                } catch (InterruptedException | IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to terminate agent " + agent.getInstanceId(), e);
                }
            }
        }
    }
}
