# Oracle Cloud Infrastructure Compute Plugin
[![Buil d Status](https://travis-ci.org/oracle/oci-compute-jenkins-plugin.svg?branch=master)](https://travis-ci.org/oracle/oci-compute-jenkins-plugin)
**Refer "old-code-base" branch for old codebase before Dec 21th, 2017**

Oracle Cloud Infrastructure (OCI) Compute Plugin allow Jenkins to create compute instances on Oracle Cloud Infrastructure (OCI) Compute and start agents on them dynamically, according to the job workload. When jobs finish and agents are idle for specified time, compute instances would be terminated and related cloud resources, eg, volumes, ip, etc,  would be recycled.  

## Prerequisites

1. You need to have a oracle cloud account, to sign up, try [Oracle Cloud](https://cloud.oracle.com/en_US/tryit)
2. Your Jenkins server must be installed with JDK8 or higher version

## Compatibility
1. Plugin is tested on minimum Jenkins version 1.625.3, they may not work with versions early than 1.554.
2. For Jenkins versions between 1.554 and 1.625, please make sure plugin 'bouncycastle API Plugin' has been pre-installed. This could be checked at 'Manage Jenkins' -> 'Manage Plugins' -> 'Installed' list, and could be found and installed from 'Available' list. 

## Installation
You can install or update the plugin through Jenkins update center or manually.
To install or update the plugin through Jenkins update center: Go to your Jenkins console, click "Manage Jenkins" -> "Manage Plugins", search "Oracle Cloud Infrastructure Compute Plugin".

Oracle Cloud Infrastructure (OCI) Compute Plugin depends on Oracle Cloud Infrastructure (OCI) Java SDK which has not been published to maven center, to compile and build the plugin from source code, you need to manually download Oracle Cloud Infrastructure Java SDK and install it to local maven repository first.

1. Install oci-java-sdk:
   ```
    $ git clone https://github.com/oracle/oci-java-sdk
    $ cd oci-java-sdk
    $ mvn compile install
   ```
2. Git clone oci-compute-jenkins-plugin project, check the value of <oci-java-sdk.version> in pom.xml, update it to oci-java-sdk's version installed, and build with:
   ```
   mvn package
   ```
3. Go to your Jenkins console, click "Manage Jenkins" -> "Manage Plugins" -> "Advanced" -> "Upload Plugin" -> Select oracle-cloud-infrastructure-compute.hpi file in target folder under your project -> click "Upload"


## Configuration 

### Add New Cloud
1. From Jenkins server console, click "Manage Jenkins" -> "Configure System"
2. Click "Add a new cloud" and select "Oracle Cloud Infrastructure Compute"
3. Enter credentials for Oracle Cloud Infrastructure (OCI) compute account (Note you can repeat step 3 multiple times to add multiple Oracle Cloud Infrastructure accounts)
   - Name: an identifier from Jenkins' perspective on this account, enter anything you want.
   - API Key: you can use the OpenSSL commands to generate the key pair in the required PEM format. Here API Key is the generated private key, and Oracle will use the public key to verify the authenticity of the request. You must upload the public key to the IAM Service. See the How to Generate an API Signing Key documentation for additional information.
   - Fingerprint: you can get the key's fingerprint with OpenSSL command such like "opensslrsa -pubout -outform DER -in ~/.oci/oci_api_key.pem | openssl md5 -c". And also when you upload the public key in the Console, the fingerprint is also automatically displayed there. The fingerprint would be found under identity/users
   - User Id: could find this as OCID shown as above.
   - Tenant Id: when you sign up for Oracle Cloud Infrastructure Services, Oracle creates a tenancy for your account, which is a secure and isolated partition within Oracle Cloud Infrastructure Services where you can create, organize, and administer your cloud resources.
   - Region: A localized geographic area, is composed of several Availability Domains. Most oracle cloud infrastructure resources are either region-specific, such as a Virtual Cloud Network, or Availability Domain-specific, such as a compute instance. See the Regions and Availability Domains documentation for additional information.
4. Click "Test Connection" to verify that Jenkins can successfully talk to Oracle Cloud Infrastructure Service with the account/credential you entered
5. Click "Advanced" and fill the number in "Instance Cap" field to limit the maximum number of compute instances that are allowed to be created using above account

### Add New Template
1. Click "Add" in "Instance Templates" section to add one resource configuration set for desired compute instances, you can click "Add" multiple times if you need different compute instance configurations.

2. Input or select values in the 'Instance Template' section:
   - Description: help other users/colleagues to understand what this template is used for.
   - Usage: it's recommended that you select "Only build jobs with label expressions matching this node" for now.
   - Labels: unique identifier which allows Jenkins to pick the right instance template to start.
   - Compartment: a compartment is a collection of related resources (such as instances, virtual cloud networks, block volumes) that can be accessed only by certain groups that have been given permission by an administrator. You can select a compartment where to create an instance.
   - Available Domain, Shape, Image, Virtual Cloud Network, Subnet: specify desired requirement on Domain, CPU, Image, Network *[Important : Make sure Java is installed in selected image or refer "Init Script" in Step 10 to install Java on the newly launched compute instances]*
   - SSH Public Key Name: here user can paste ssh public key
   - SSH Private Key: here user need to paste corresponding private key that allows jenkins connect to the compute instance provisioned using above selected public key.

4. Click "Verify SSH Key Pair" to verify the public key and private key are matched or not as blew.

5. Click "Advanced" for more configuration options:
   - Remote FS root, Remote SSH user: dedicated directory for Jenkins agent in agent node and the ssh user used for Jenkins master to access Jenkins agent, make sure the SSH user has written permission on Remote FS root directory.
   - Instance Creation Timeout: number of seconds to wait for new compute instance to reach state "ready", default value is 300. 
   - Instance SSH Connection Timeout: number of seconds to wait for new compute instance from state "ready" to be SSH connectable from Jenkins master, default value is 60.
   - Idle Termination Minutes: number of minutes for Jenkins to wait before deleting an idle agent, which means completely removal of the created compute instance. A value of 0 (or an empty string) indicates that idle agents should never be stopped/deleted. As an example, let's say an agent was started at 11:00 and Idle Termination Minutes was set 5. The agent executed several Jenkins jobs from 11:00 to 11:20, and has been idle since then. At 11:25 Jenkins finds the idle timeout of above agent has reached and the agent will be deleted. 
   - Number of Executors: this controls the number of concurrent builds that Jenkins can perform. So the value affects the overall system load Jenkins may incur. A good value to start with would be the number of processors on your system.
Increasing this value beyond that would cause each build to take longer, but it could increase the overall throughput, because it allows CPU to build one project while another build is waiting for I/O.
When using Jenkins in the master/agent mode, setting this value to 0 would prevent the master from doing any building on its own. Agents may not have zero executors, but may be temporarily disabled using the button on the agent's status page.
   - Init Script: user can define several lines of shell based commands to configure the provisioned compute instances (One-time) before the first Jenkins job starts to build; for example, if the image user selected does not have Java pre-installed, user can input Java installation command like "sudo yum -y install java"
   - Init Script Timeout: Number of seconds to wait for the completion of Init Script. Default value is 120 seconds.

6. Click "Save" or "Apply"

### Job configuration
We have added an instance template with a specific label string, to restrict projects can be run with this kind of template, user can simply input the same label string in "Label Expression"  field of  the Jenkins job configuration page.

