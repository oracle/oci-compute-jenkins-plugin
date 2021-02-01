package com.oracle.cloud.baremetal.jenkins;

import java.util.logging.Logger;

import javax.annotation.concurrent.GuardedBy;

import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.CloudRetentionStrategy;
import hudson.slaves.OfflineCause.SimpleOfflineCause;

public class BaremetalCloudRetentionStrategy extends CloudRetentionStrategy {
    private static final Logger LOGGER = Logger.getLogger(BaremetalCloud.class.getName());

    public BaremetalCloudRetentionStrategy(int idleMinutes) {
        super(idleMinutes);
    }

    /**
     * Prevent {@link CloudRetentionStrategy} from terminating the Computer if it is
     * in offline state set by user (e.g. from Web UI) or by another plugin.
     */
    @Override
    @GuardedBy("hudson.model.Queue.lock")
    public long check(final AbstractCloudComputer c) {
        if (c.isOffline() && c.getOfflineCause() instanceof SimpleOfflineCause) {
            LOGGER.fine(c.getDisplayName() + ": Node is set temporarily offline - will not terminate");
            return 1;
        }

        return super.check(c);
    }
}
