package com.oracle.cloud.baremetal.jenkins;

import hudson.slaves.AbstractCloudComputer;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;

import java.io.IOException;

public class BaremetalCloudComputer extends AbstractCloudComputer<BaremetalCloudAgent> {
    public BaremetalCloudComputer(BaremetalCloudAgent slave) {
        super(slave);
    }

    @Override
    public HttpResponse doDoDelete() throws IOException {
        checkPermission(DELETE);
        try {
            BaremetalCloudAgent node = getNode();
            if (node != null) { // No need to terminate nodes again
                node.terminate();
            }
            return new HttpRedirect("../..");
        } catch (InterruptedException e) {
            return HttpResponses.error(500, e);
        }
    }
}
