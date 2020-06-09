[**Oracle Cloud Infrastructure Compute Plugin**](https://updates.jenkins.io/latest/oracle-cloud-infrastructure-compute.hpi) allows users to access and manage cloud resources on the Oracle Cloud Infrastructure (OCI) from Jenkins.
A Jenkins master instance with Oracle Cloud Infrastructure Compute Plugin can spin up OCI Instances (slaves or agents) on demand within OCI, and remove the Instances and free its resources automatically once the Job completes.



## Table of Contents

- [Features](#features)
- [Prerequisites](#prerequisites)
- [Compatibility](#compatibility)
- [Installation](#installation)
- [Building](building)
- [Upgrade](#upgrade)
- [Configuration](#configuration)
- [Licensing](#licensing)
- [Changelog](#changelog)
- [Contributing](#contributing)



## Features

**Oracle Cloud Infrastructure Compute Plugin** provides functionality to dynamically allocate OCI resources for continuous integration tasks, and to bring up and down OCI Instances and resources as required to serve Jenkins Build Jobs.

After installing the Plugin, you can add OCI Clouds and Templates with your required OCI Instance configuration. The Template will have a Label that you can use in your Jenkins Job. Multiple Templates are supported. The Template options include Labels, Domains, Credentials, Shapes, Images, Virtual Cloud Network, Template Instance Cap, etc.
After your Jenkins Job completes its work, the OCI Instance is cleanly removed and resources are released back to the OCI pool.

View Oracle Cloud Infrastructure Compute Plugin page on the [plugins.jenkins.io](https://plugins.jenkins.io/oracle-cloud-infrastructure-compute) site for more information.



## Prerequisites

1. Oracle Cloud Account. To sign up, visit [Oracle Cloud](https://cloud.oracle.com/en_US/tryit).

2. Jenkins installed with JDK 8 or higher.

3. Required Plugins: [bouncycastle API](https://plugins.jenkins.io/bouncycastle-api), [SSH Credentials](https://plugins.jenkins.io/ssh-credentials/), and [Credentials](https://plugins.jenkins.io/credentials)

   

## Compatibility
Minimum Jenkins requirement: *2.164.x*




## Installation
There are a number of ways to install the Oracle Cloud Infrastructure Compute Plugin.

- Using the Plugin Manager in the web UI.
- Using the Jenkins CLI install-plugin command.
- Copying the .hpi file to the JENKINS_HOME/plugins directory.



##### Using the Plugin Manager

The simplest and most common way of installing plugins is through the Manage Jenkins > Manage Plugins view, available to Administrators of a Jenkins environment.

To install the Plugin in Jenkins: 
1. Click on **Manage Jenkins** in Home
2. Click **Manage Plugins**
3. Click **Available** tab
4. Search for **Oracle Cloud Infrastructure Compute Plugin** or **oracle-cloud-infrastructure-compute**
5. Click **Install**
6. Restart Jenkins



##### Using the Jenkins CLI

Administrators may also use the [Jenkins CLI](https://jenkins.io/doc/book/managing/cli/) which provides a command to install plugins.

	java -jar jenkins-cli.jar -s http://localhost:8080/ install-plugin SOURCE ... [-deploy] [-name VAL] [-restart]
	
	Installs a plugin either from a file, an URL, or from update center.
	
	 SOURCE    : If this points to a local file, that file will be installed. If
	         	 this is an URL, Jenkins downloads the URL and installs that as a
	        	 plugin.Otherwise the name is assumed to be the short name of the
	             plugin i.e. "oracle-cloud-infrastructure-compute",and the
	         	 plugin will be installed from the update center.
	 -deploy   : Deploy plugins right away without postponing them until the reboot.
	 -name VAL : If specified, the plugin will be installed as this short name
	         	 (whereas normally the name is inferred from the source name
	         	 automatically).
	 -restart  : Restart Jenkins upon successful installation.


Link to latest .hpi version can be found [here](https://updates.jenkins.io/latest/oracle-cloud-infrastructure-compute.hpi).



##### Copying the .hpi file to the plugin directory 

Using the .hpi file that has been explicitly downloaded by a systems administrator, the administrator can manually copy the downloaded .hpi file into the JENKINS_HOME/plugins directory on the Jenkins master.
Link to latest .hpi version can be found [here](https://updates.jenkins.io/latest/oracle-cloud-infrastructure-compute.hpi).

The Jenkins master will need to be restarted before the plugin is loaded and made available in the Jenkins environment.



## Building
Jenkins plugins are packaged as self-contained .hpi files, which have all the necessary code, images, and other resources which the plugin needs to operate successfully. 

If desired, you can build the Oracle Cloud Infrastructure Compute Plugin .hpi from the source code, and then install the .hpi file in Jenkins.

To build the .hpi file, OCI Java SDK is required and is available on [Maven Central](https://search.maven.org/search?q=g:com.oracle.oci.sdk) and [JCenter](https://bintray.com/oracle/jars/oci-java-sdk).

Refer to OCI Java SDK licensing [here](https://github.com/oracle/oci-java-sdk/blob/master/LICENSE.txt).

##### Compile the Plugin
1. git clone repo

2. If you want to use the latest version of OCI Java SDK, update pom.xml

   ```
   <oci-java-sdk.version>1.17.4</oci-java-sdk.version>
   ```

   Compile and Install package:

   ```
   $ mvn package
   ```

   

#####  Install the Plugin
A logged-in Jenkins administrator may upload the file from within the web UI.

1. Navigate to the Manage Jenkins > Manage Plugins page in the web UI
1. Click on the Advanced tab
1. Choose the .hpi file under the Upload Plugin section
1. Click Upload

**or**	

The System Administrator can copy the .hpi file into the JENKINS_HOME/plugins directory on the Jenkins master.
The master will need to be restarted before the plugin is loaded and made available in the Jenkins environment.




## Upgrade
Updates are listed in the Updates tab of the **Manage Plugins** page and can be installed by checking the checkbox of the Oracle Cloud Infrastructure Compute plugin updates and clicking the **Download now and install after restart** button.

**Note**:  Upgrading the Plugin may require you to update your already created OCI Cloud and Templates Configuration. After upgrade please check all OCI Cloud values are OK in Manage Jenkins > Configure System. For example, a new method of adding OCI Credentials was added in v106 of the Plugin. Previously these OCI Credentials were added in the OCI Cloud Configuration. If upgrading to v106 from an earlier version, then you may have to update the values in your existing Cloud configuration.

**Note**: A plugin version with new functionality may only take effect on Slaves built with that new version. You may need to remove older Slaves.



## Configuration 

#### Add OCI Credentials

Oracle Cloud Infrastructure Credentials are required to connect to your Oracle Cloud Infrastructure. For more information on OCI Credentials and other required keys, please see [Security Credentials](https://docs.cloud.oracle.com/iaas/Content/General/Concepts/credentials.htm).

You can add these OCI Credentials by navigating to the Jenkins Server console, Credentials, System,  and **Add Credentials**

*or*

by navigating to the Jenkins Server console, click Manage Jenkins, and Configure System. In Cloud section, click **Add a new cloud** and select **Oracle Cloud Infrastructure Compute**. In **Credentials**, click **Add**.

Once in the New Credentials Screen, select **Oracle Cloud Infrastructure Credentials** from the **Kind** Drop-Down.

- **Fingerprint** - The Fingerprint for the key pair being used. 
- **API Key** - The OCI API Signing Private Key. 
- **PassPhrase** - The PassPhrase for the key pair being used.
- **Tenant Id** - The Tenant OCID.
- **User Id** - The OCID of the User whose API signing key you are using. 
- **Region** - The OCI region to use for all OCI API requests for example, us-phoenix-1.
- **ID** - An internal unique ID by which these credentials are identified from jobs and other configuration.
- **Description** - An optional description to help tell similar credentials apart.
- **Calling Services from an Instance** - You can authorize an instance to make API calls in Oracle Cloud 
  Infrastructure services.  After you set up the required resources and policies in OCI, an application running on an instance can call OCI public services, removing the need to configure user credentials or a configuration file. If using this functionality, then the Jenkins Master is configured to authorize an instance to make API calls in OCI services. By checking this Option, only the Tenant Id and Region Fields are required. See [Calling Services from an Instance](https://docs.cloud.oracle.com/iaas/Content/Identity/Tasks/callingservicesfrominstances.htm) documentation for additional information.

Click **Verify Credentials** that you can connect successfully to your Oracle Cloud Infrastructure.



#### Add OCI Cloud

1. From Jenkins Server console, click Manage Jenkins, and Configure System
2. In Cloud section, click **Add a new cloud** and select **Oracle Cloud Infrastructure Compute**
3. Enter credentials to access your OCI account. You can create multiple Clouds.
   - **Name**  - A name for this OCI Compute Cloud.
   - **Credentials** - The OCI credentials required to connect to your Oracle Cloud Infrastructure.
     If you want to add an OCI Credential click **Add**. See the previous **Add OCI Credentials** section for more information.
4. Click **Advanced** for more options.
	- **Instance Cap** - A number to limit the maximum number of instances that can be created for this Cloud configuration. Leave this field empty to have no cap. 
	- **Max number of async threads** - The max number of async threads to use to load the Templates configuration. Consider reducing this value for Cloud configurations with a large number of Templates and if some values fail to load due to OCI API limit being exceeded. In this case the logs will show "User-rate limit exceeded" errors.



#### Add OCI Template

1. Click **Add** in **Instance Templates** section to add the OCI configuration. You can add multiple Templates to a Cloud configuration.

2. Input or select values in the Instance Template section:
   - **Description** - Provide a description for this Template.
   - **Usage** - It's recommended that you select "Only build jobs with label expressions matching this node" for now.
   - **Labels** - Enter a unique identifier which allows Jenkins to pick the right instance template to run Job.
   - **Compartment** - The compartment from which the new Instance is launched. 
   - **Availability Domain** - The Availability Domain for your instance.  
   - **Image Compartment** -  The compartment from which to select the Instance's image. 
   - **Image** - Select the Image the instance will use. **Note:** Java should be installed on the image as a Jenkins requirement. Alternatively refer to **Init Script** in Advanced section below to install Java on the newly launched Linux instances. **Note:** Windows images also need to be preconfigured and to be able to authenticate with SSH.
   - **Shape** - The Shape for your instance.
   - **Virtual Cloud Network Compartment** -  The compartment from which to select the Virtual Cloud Network and Subnet. 
   - **Virtual Cloud Network** - The Virtual Cloud Network for your instance.
   - **Subnet** - Subnet of your Virtual Cloud Network.
   - **Assign Public IP Address** - The Plugin will assign a public IP to an instance, provided the subnet has an available public IP range. If this Option is unchecked, only the private IP is assigned. 
   - **Connect Agent using Public IP** - The Plugin will connect to the public IP of the instance. If this Option is unchecked, the Plugin will connect to the private IP of the instance. 
   - **SSH credentials** - The Private SSH Key for accessing the OCI instance. For more information, please see [Credentials](https://docs.cloud.oracle.com/iaas/Content/General/Concepts/credentials.htm).
   
3. Click **Advanced** for more options:
   - **Remote FS root** - Dedicated directory for Jenkins agent in instance.
   - **Instance Creation Timeout** - Number of seconds to wait for instance to reach state Running. Default value is 900. 
   - **Instance SSH Connection Timeout** - Number of seconds to wait for instance from state Running to be able to ssh connect from Jenkins master. Default value is 900.
   - **Idle Termination Minutes** - Number of minutes for Jenkins to wait before deleting and completely removing an idle instance. A value of 0 (or an empty string) indicates that instance will never be stopped/deleted. 
   - **Number of Executors** - Number of concurrent builds that Jenkins can perform. Value should be at least 1.
   - **Init Script** - You can define several lines of shell based commands to configure the instance (one-time) before it comes online. For example, if the image selected does not have Java pre-installed, you can add command "sudo yum -y install java". This functionality works for Linux instances only.
   - **Init Script Timeout** - Number of seconds to wait for the completion of Init Script. Default value is 120 seconds. 
   - **Template Instance Cap** - Places a limit on the number of OCI Instances that Jenkins may launch from this Template. Leave this field empty to remove the Template Instance Cap. 

6. Click **Save** or **Apply**



## Licensing

Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved.

This Plugin is licensed under the Universal Permissive License 1.0

This software is dual-licensed to you under the Universal Permissive License (UPL) and Apache License 2.0. 

See [LICENSE.txt](https://github.com/oracle/oci-compute-jenkins-plugin/blob/master/LICENSE.txt) for more details.



## Changelog

For CHANGELOG please refer to [CHANGELOG.md](https://github.com/oracle/oci-compute-jenkins-plugin/blob/master/CHANGELOG.md).



## Contributing
Oracle Cloud Infrastructure Compute Plugin is an open source project. See [CONTRIBUTING.md](https://github.com/oracle/oci-compute-jenkins-plugin/blob/master/CONTRIBUTING.md) for more details.

Oracle gratefully acknowledges the contributions to Oracle Cloud Infrastructure Compute Plugin that have been made by the community.
