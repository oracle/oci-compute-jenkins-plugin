# Oracle Cloud Infrastructure Compute Plugin

[**Oracle Cloud Infrastructure Compute Plugin**](https://updates.jenkins.io/latest/oracle-cloud-infrastructure-compute.hpi) allows users to access and manage cloud resources on the Oracle Cloud Infrastructure (OCI) from Jenkins.
A Jenkins master instance with Oracle Cloud Infrastructure Compute Plugin can spin up OCI Instances (slaves or agents) on demand within OCI, and remove the Instances and free its resources automatically once the Job completes.


## Table of Contents
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Compatibility](#compatibility)
- [Installation](#installation)
- [Building the Plugin from Repository](building_the_plugin_from_repository)
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

## Compatibility
1. Plugin is tested on minimum Jenkins version 1.625.3, it may not work with versions early than 1.554.
2. For Jenkins versions between 1.554 and 1.625, pre-install 'bouncycastle API Plugin' plugin.


## Installation
There are a number of ways to install the Oracle Cloud Infrastructure Compute Plugin.

1. Using the "Plugin Manager" in the web UI.
2. Using the Jenkins CLI install-plugin command.
3. Copying the .hpi file to the JENKINS_HOME/plugins directory.

#####Using the "Plugin Manager" in the web UI
The simplest and most common way of installing plugins is through the Manage Jenkins > Manage Plugins view, available to administrators of a Jenkins environment.

To install the Plugin in Jenkins: 
	
1. Click on **Manage Jenkins** in Home
2. Click **Manage Plugins**
3. Click **Available** tab
4. Search for "Oracle Cloud Infrastructure Compute Plugin" or "oracle-cloud-infrastructure-compute"
5. Click **Install**
6. Restart Jenkins

#####Using the Jenkins CLI install-plugin command

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


#####Copying the .hpi file to the JENKINS_HOME/plugins directory
Using the .hpi file that has been explicitly downloaded by a systems administrator, the administrator can manually copy the downloaded .hpi file into the JENKINS_HOME/plugins directory on the Jenkins master.
Link to latest .hpi version can be found [here](https://updates.jenkins.io/latest/oracle-cloud-infrastructure-compute.hpi).

The master will need to be restarted before the plugin is loaded and made available in the Jenkins environment.



## Building the Plugin from Repository
Jenkins plugins are packaged as self-contained .hpi files, which have all the necessary code, images, and other resources which the plugin needs to operate successfully. 

If desired, you can build the Oracle Cloud Infrastructure Compute Plugin Plugin .hpi from the source code, and then install the .hpi file in Jenkins.

To build the .hpi file, OCI Java SDK is required. OCI Java SDK is currently not published to Maven center so first install OCI Java SDK to the local Maven repository. Steps are outlined below.

Refer to OCI Java SDK licensing [here](https://github.com/oracle/oci-java-sdk/blob/master/LICENSE.txt).


##### Installing OCI Java SDK (Github)

    $ git clone https://github.com/oracle/oci-java-sdk
    $ cd oci-java-sdk
    $ mvn compile install




##### Compile the Plugin
1. git clone repo 
2. Update pom.xml with OCI Java SDK version you have installed

	```
	<oci-java-sdk.version>1.3.1</oci-java-sdk.version>
 	```

3. Compile and Install package

	```
	$ mvn package
	```

#####  Install the Plugin
A logged-in Jenkins administrator may upload the file from within the web UI.

1. Navigate to the Manage Jenkins > Manage Plugins page in the web UI.
1. Click on the Advanced tab.
1. Choose the .hpi file under the Upload Plugin section.
1. Upload the plugin file.


**or**	

The System Administrator can copy the .hpi file into the JENKINS_HOME/plugins directory on the Jenkins master.
The master will need to be restarted before the plugin is loaded and made available in the Jenkins environment.



## Configuration 

### Add New Cloud
1. From Jenkins Server console, click Manage Jenkins > Configure System
2. In Cloud section, click **Add a new cloud** and select **Oracle Cloud Infrastructure Compute**
3. Enter credentials to access your OCI account. You can create multiple Clouds.
   - **Name**  - Provide a name for this OCI Compute Cloud.
   - **Fingerprint** - Enter the Fingerprint from your OCI API Signing Key. If you do not have one, it can be left blank. For more information see [Security Credentials](https://docs.us-phoenix-1.oraclecloud.com/Content/General/Concepts/credentials.htm).
   - **API Key** - Enter the OCI API Signing Private Key. For more information see [Security Credentials](https://docs.us-phoenix-1.oraclecloud.com/Content/General/Concepts/credentials.htm).
   - **User Id** - Enter your User OCID.
   - **Tenant Id** - Enter your Tenant OCID.
   - **Region** - Enter Region, for example, us-phoenix-1. 
4. Click **Advanced** for more options.
	- **Instance Cap** - Enter a number to limit the maximum number of instances that can be created for this Cloud configuration. Leave this field empty to have no cap. 
	- **Max number of async threads** - The max number of async threads to use to load the Templates configuration. Consider reducing this value for Cloud configurations with a large number of Templates and if some values fail to load due to OCI API limit being exceeded. In this case the logs will show "User-rate limit exceeded" errors.
5. Click **Test Connection** to verify that Jenkins can successfully connect to the Oracle Cloud Infrastructure Service. 

### Add New Template
1. Click **Add** in **Instance Templates** section to add the OCI configuration. You can add multiple Templates to a Cloud configuration.

2. Input or select values in the 'Instance Template' section:
   - **Description** - Provide a description for this Template.
   - **Usage** - It's recommended that you select "Only build jobs with label expressions matching this node" for now.
   - **Labels** - Enter a unique identifier which allows Jenkins to pick the right instance template to run Job.
   - **Compartment** - The compartment from which the new Instance is launched. 
   - **Availability Domain** - Select the Availability Domain for your instance.  
   - **Shape** - Select the Shape for your instance.
   - **Image Compartment** -  The compartment from which to select the Instance's image. **Note:** if upgrading from v1.0.2 (or earlier) and Images are in a separate compartment than the default **Compartment** above, you may have to update the values in your existing Template configuration.
   - **Image** - Select the Image the instance will use. **Note:** Java should be installed on the image as a Jenkins requirement. Alternatively refer to "Init Script" in Step 10 to install Java on the newly launched instances.
   - **Virtual Cloud Network Compartment** -  The compartment from which to select the Virtual Cloud Network and Subnet. **Note:** if upgrading from v1.0.3 (or earlier) and the VCN is in a separate compartment than the default **Compartment** above, you may have to update the values in your existing Template configuration.
   - **Virtual Cloud Network** - Select the Virtual Cloud Network for your instance.
   - **Subnet** - Select Subnet of your Virtual Cloud Network.
   - **Assign Public IP Address** - By default, the Plugin will assign a public IP to an instance, provided the subnet has an available public IP range. If this Option is unchecked, only the private IP is assigned. 
   - **Connect Agent using Public IP**	- By default the Plugin will connect to the public IP of the instance. If this Option is unchecked, the Plugin will connect to the private IP of the instance. 
   - **SSH Public Key Name** - Enter ssh public key for your instance. For more information see [Security Credentials](https://docs.us-phoenix-1.oraclecloud.com/Content/General/Concepts/credentials.htm).
   - **SSH Private Key** - Enter ssh private key for your instance. For more information see [Security Credentials](https://docs.us-phoenix-1.oraclecloud.com/Content/General/Concepts/credentials.htm).
   
    

4. Click **Verify SSH Key Pair** to verify the public key and private key entered are a match.

5. Click **Advanced** for more options:
   - **Remote FS root** - Dedicated directory for Jenkins agent in instance.
   - **Remote SSH user** - ssh user used for Jenkins master to access instance. The ssh user should have written permission on Remote FS root directory.
   - **Instance Creation Timeout** - Number of seconds to wait for instance to reach state "ready", default value is 300. 
   - **Instance SSH Connection Timeout** - Number of seconds to wait for instance from state "ready" to be able to ssh connect from Jenkins master. Default value is 60.
   - **Idle Termination Minutes** - Number of minutes for Jenkins to wait before deleting and completely removing an idle instance. A value of 0 (or an empty string) indicates that instance will never be stopped/deleted. 
   - **Number of Executors** - Number of concurrent builds that Jenkins can perform. Value should be at least 1.
   - **Init Script** - You can define several lines of shell based commands to configure the instance (one-time) before the Jenkins Job runs. For example, if the image selected does not have Java pre-installed, you can add command "sudo yum -y install java"
   - **Init Script Timeout** - Number of seconds to wait for the completion of Init Script. Default value is 120 seconds. 
   - **Template Instance Cap** - Places a limit on the number of OCI Instances that Jenkins may launch from this Template. Leave this field empty to remove the Template Instance Cap. 

6. Click **Save** or **Apply**

## Licensing
Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.

This Plugin is licensed under the Universal Permissive License 1.0

This software is dual-licensed to you under the Universal Permissive License (UPL) and Apache License 2.0. 

See [LICENSE.txt](https://github.com/oracle/oci-compute-jenkins-plugin/blob/master/LICENSE.txt) for more details.

## Changelog

For Changelog please refer to [CHANGELOG.md](https://github.com/oracle/oci-compute-jenkins-plugin/blob/master/CHANGELOG.md).

## Contributing
Oracle Cloud Infrastructure Compute Plugin is an open source project. See [CONTRIBUTING.md](https://github.com/oracle/oci-compute-jenkins-plugin/blob/master/CONTRIBUTING.md) for more details.

Oracle gratefully acknowledges the contributions to Oracle Cloud Infrastructure Compute Plugin that have been made by the community.
