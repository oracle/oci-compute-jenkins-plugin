package com.oracle.cloud.baremetal.jenkins;

import java.util.Collection;
import java.util.Collections;

import hudson.model.Label;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner.PlannedNode;

public class TestCloud extends Cloud {
    public TestCloud() {
        this(TestCloud.class.getName());
    }

    public TestCloud(String name) {
        super(name);
    }

    @Override
    public Collection<PlannedNode> provision(Label label, int excessWorkload) {
        return Collections.emptyList();
    }

    @Override
    public boolean canProvision(Label label) {
        return false;
    }
}
