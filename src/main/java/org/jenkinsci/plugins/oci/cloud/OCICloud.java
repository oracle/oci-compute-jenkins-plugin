/**
 * Jenkins OCI Plugin
 *
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
package org.jenkinsci.plugins.oci.cloud;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.anyOf;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.instanceOf;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.oci.cloud.Messages;
import org.jenkinsci.plugins.oci.credentials.OCICredentials;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.oracle.bmc.core.model.Instance;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Label;
import hudson.model.Node;
import hudson.security.ACL;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner;
import hudson.slaves.NodeProvisioner.PlannedNode;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

public class OCICloud extends Cloud {

  private static final Logger LOGGER = Logger.getLogger(OCICloud.class.getName());

  private final String credentialsId;
  private final List<OCISlaveTemplate> templates;

  private transient OCIApi api;

  private final int nodesMax;

  private final int timeout;

  @DataBoundConstructor
  public OCICloud(String credentialsId, String name, Node.Mode mode, int timeout, int nodesMax, List<OCISlaveTemplate> templates) {
    super(name);
    this.credentialsId = credentialsId;
    this.nodesMax = nodesMax;
    this.timeout = timeout;
    this.templates = (templates == null) ? Collections.emptyList() : templates;

    readResolve();
  }

  /**
   * This method is called on deserialization of OCICloud from config.xml
   *
   * @return self
   */
  protected Object readResolve() {
    LOGGER.log(Level.INFO, "Initializing OCIApi");

    try {
      this.api = new OCIApi(credentialsId);
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Could not initialize API");
      this.api = null;
    }

    return this;
  }

  @Override
  public boolean canProvision(Label label) {
    LOGGER.log(Level.FINE, "checking whether we can provision {0}", label);
    return getFirstMatchingTemplate(label) != null;
  }

  /**
   * Get first template matching given label.
   *
   * <pre>
   * The precedence is as follows:
   * 1. First template matching label in {@link Node.Mode#NORMAL} mode
   * 2. First template matching label in {@link Node.Mode#EXCLUSIVE} mode
   * 3. null
   * </pre>
   *
   * @param label
   *        Label to match
   * @return Matching template, null if none found
   */
  public OCISlaveTemplate getFirstMatchingTemplate(Label label) {
    OCISlaveTemplate matchingTemplate = null;

    for (OCISlaveTemplate template : this.templates) {
      if (label == null && template.getMode() == Node.Mode.NORMAL) {
        return template;
      }
      if (label != null && label.matches(Label.parse(template.getLabelString()))) {
        if (template.getMode() == Node.Mode.NORMAL) {
          return template; // first NORMAL matching
        } else {
          matchingTemplate = template;
        }
      }
    }
    // Returns null on empty list
    return matchingTemplate;
  }

  /**
   * Get provisioned and provisioning node count
   *
   * When node is scheduled for provisioning, its Node instance
   * is not created immediately, thus adding the number of scheduled nodes.
   *
   * @return provisioned and provisioning node count
   */
  private int getTotalNodeCount() {
    int count = 0;

    // Provisioned nodes
    List<Node> nodes = Jenkins.getInstance().getNodes();
    for (Node n : nodes) {
      if (n instanceof OCISlave && ((OCISlave) n).getCloudName() == this.name) {
        count++;
        LOGGER.log(Level.INFO, "Counting nodes {0}", n.getDisplayName());
      }
    }

    // Nodes yet to be provisioned
    Jenkins jenkins = Jenkins.getInstance();
    List<PlannedNode> plannedNodeList = jenkins.unlabeledNodeProvisioner.getPendingLaunches();

    for (Label l : jenkins.getLabels()) {
      plannedNodeList.addAll(l.nodeProvisioner.getPendingLaunches());
    }

    for (PlannedNode pn : plannedNodeList) {
      if (pn instanceof OCIPlannedNode && ((OCIPlannedNode) pn).getCloudName() == this.name) {
        count++;
        LOGGER.log(Level.INFO, "Counting nodes {0}", pn.displayName);
      }
    }

    LOGGER.log(Level.INFO, "Cloud " + this.name + " has currently {0} nodes", count);
    return count;
  }

  @Override
  public Collection<PlannedNode> provision(Label label, int excessWorkload) {
    LOGGER.log(Level.INFO, "New node provision request. Workload: " + excessWorkload);

    List<NodeProvisioner.PlannedNode> nodes = new ArrayList<>();

    OCISlaveTemplate template = getFirstMatchingTemplate(label);

    while (excessWorkload > 0 && getTotalNodeCount() < nodesMax) {
      LOGGER.log(Level.INFO, "Excess workload: " + excessWorkload + " - provisioning new Jenkins slave on OCI");
      int numExecutors = template.getNumExecutors();

      Future<Node> futureNode = Computer.threadPoolForRemoting.submit(new ProvisionTask(template, this));

      PlannedNode node =
          new OCIPlannedNode(
              this.name + "-provisioning", // name to be visible during the provisioning process...
              futureNode,
              numExecutors,
              this.name);

      nodes.add(node);
      excessWorkload -= template.getNumExecutors();
    }

    return nodes;
  }

  private class ProvisionTask implements Callable<Node> {

    OCISlaveTemplate template;
    OCICloud cloud;

    public ProvisionTask(OCISlaveTemplate template, OCICloud cloud) {
      this.template = template;
      this.cloud = cloud;
    }

    public Node call() throws Exception {
      // make sure the name in OCI console refers to particular jenkins master who asked for the instance
      // this name is not used in jenkins UI
      String templateName = template.getName().equals("") ? "" : "-" + template.getName();
      String cloudName = cloud.name.equals("") ? "oci" : cloud.name;
      String slaveBaseName = cloudName + templateName;

      String hostname = InetAddress.getLocalHost().getHostName();
      String name_for_oci_console = hostname + "-jenkins-" + slaveBaseName;

      LOGGER.log(Level.INFO, "OCI Provisioning: CREATE {0}", name_for_oci_console);
      Instance instance = cloud.getApi().startSlave(template, name_for_oci_console, timeout);

      // once we got the instace from OCI generate the name for jenkins
      // note we're using last 6 characters from instance ocid which are the same characters that would be displayed in OCI console
      String i_ocid = instance.getId();
      String name_for_jenkins_ui = slaveBaseName + "-" + i_ocid.substring(i_ocid.length() - 6);

      return new OCISlave(cloud.name, name_for_jenkins_ui, template, i_ocid);
    }
  }

  @Extension
  public static final class DescriptorImpl extends Descriptor<Cloud> {

    @Override
    public String getDisplayName() {
      return Messages.Cloud_Diaplay_Name();
    }

    public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item context, @QueryParameter String credentialsId) {
      StandardListBoxModel result = new StandardListBoxModel();

      if (context == null) {
        if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
          return result.includeCurrentValue(credentialsId);
        }
      } else {
        if (!context.hasPermission(Item.EXTENDED_READ) && !context.hasPermission(CredentialsProvider.USE_ITEM)) {
          return result.includeCurrentValue(credentialsId);
        }
      }

      List<DomainRequirement> domainRequirements = new ArrayList<DomainRequirement>();

      return result.includeMatchingAs(ACL.SYSTEM, context, OCICredentials.class, domainRequirements, anyOf(instanceOf(OCICredentials.class)));
    }
  }

  public static OCICloud getCloud(String name) {
    Cloud cloud = Jenkins.getInstance().getCloud(name);

    if (cloud instanceof OCICloud) {
      return (OCICloud) cloud;
    }

    return null;
  }

  OCIApi getApi() {
    return api;
  }

  public String getCredentialsId() {
    return credentialsId;
  }

  public int getTimeout() {
    return timeout;
  }

  public int getNodesMax() {
    return nodesMax;
  }

  public List<OCISlaveTemplate> getTemplates() {
    return templates;
  }
}
