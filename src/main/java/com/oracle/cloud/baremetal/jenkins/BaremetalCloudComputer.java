package com.oracle.cloud.baremetal.jenkins;

import hudson.slaves.AbstractCloudComputer;

public class BaremetalCloudComputer extends AbstractCloudComputer<BaremetalCloudAgent> {
    public BaremetalCloudComputer(BaremetalCloudAgent slave) {
        super(slave);
    }
}
