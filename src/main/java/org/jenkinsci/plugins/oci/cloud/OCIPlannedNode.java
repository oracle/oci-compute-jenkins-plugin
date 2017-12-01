/**
 *  Jenkins OCI Plugin
 *
 *  Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 *  Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
package org.jenkinsci.plugins.oci.cloud;

import java.util.concurrent.Future;

import hudson.model.Node;
import hudson.slaves.NodeProvisioner.PlannedNode;

public class OCIPlannedNode extends PlannedNode {

  private final String cloudName;

  public OCIPlannedNode(String displayName, Future<Node> future, int numExecutors, String cloudName) {
    super(displayName, future, numExecutors);
    this.cloudName = cloudName;
  }

  public String getCloudName() {
    return cloudName;
  }
}
