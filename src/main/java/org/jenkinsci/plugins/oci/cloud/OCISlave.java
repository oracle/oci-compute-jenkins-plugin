/**
 * Jenkins OCI Plugin
 *
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
package org.jenkinsci.plugins.oci.cloud;

import java.io.IOException;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.oracle.bmc.core.model.Instance;

import hudson.model.TaskListener;
import hudson.model.Descriptor.FormException;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.AbstractCloudSlave;
import hudson.slaves.CloudRetentionStrategy;
import hudson.slaves.NodeProperty;

public class OCISlave extends AbstractCloudSlave {
  private static final Logger LOGGER = Logger.getLogger(OCISlave.class.getName());

  /**
   * Name of the OCICloud - use {OCICloud$getCloud} to get the actual cloud instance
   */
  private final String cloudName;
  private String instanceId;

  public OCISlave(String cloudName, String name, OCISlaveTemplate template, String instanceId) throws FormException, IOException {
    super(
        name,
        "OCI Slave ", //nodeDescription
        "jenkins_agent", //remoteFS - this will create ~/jenkins_agent on a target machine regardless of user who logged in; we cannot specify full paths as we don't know the user and where he is allowed to write in advance
        template.getNumExecutors(), //numExecutors
        template.getMode(),
        template.getLabelString(),
        new SSHLauncher(
            OCICloud.getCloud(cloudName).getApi().getInstanceIp(instanceId, template), // ip address
            22, //port
            template.getSshCredentialsId(),
            null,
            null,
            null,
            null,
            180),
        new CloudRetentionStrategy(5), //new OnceRetentionStrategy(1)
        Collections.<NodeProperty<?>>emptyList() //nodeProperties
        );
    this.cloudName = cloudName;
    this.instanceId = instanceId;
  }

  @Override
  public AbstractCloudComputer<OCISlave> createComputer() {
    return new AbstractCloudComputer<OCISlave>(this);
  }

  @Override
  protected void _terminate(TaskListener listener) {
    LOGGER.log(Level.INFO, "Terminating OCISlave: {0}", getNodeName());
    OCICloud cloud = OCICloud.getCloud(cloudName);
    cloud.getApi().terminateSlave(instanceId);
  }

  public String getCloudName() {
    return cloudName;
  }
}
