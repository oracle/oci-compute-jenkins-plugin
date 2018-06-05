# Oracle Cloud Infrastructure Compute Plugin

**Oracle Cloud Infrastructure Compute Plugin** allows users to access and manage cloud resources on the Oracle Cloud Infrastructure (OCI) from Jenkins.
A Jenkins master instance with Oracle Cloud Infrastructure Compute Plugin can spin up instances (slaves or agents) on demand within OCI, and remove the instances and free its resources automatically once the Job completes.

## Table of Contents
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Compatibility](#compatibility)
- [Installation](#installation)
- [Configuration](#configuration)
- [Licensing](#licensing)
- [Changelog](#changelog)
- [Contributing](#contributing)

## Features
**Oracle Cloud Infrastructure Compute Plugin** provides functionality to dynamically allocate OCI resources for continuous integration tasks, and to bring up and down services or nodes as required to serve Jenkins Build Jobs.

After installing the Oracle Cloud Infrastructure Compute Plugin, you can add an OCI Cloud(s) and Template(s) with the preferred instance configuration. The Template will have a Label that you can use in your Jenkins Job. Multiple Templates are supported. The Template options include Labels, Domains, Credentials, Shapes, Images, Virtual Cloud Network, etc.
After your Jenkins Job completes the instance is cleanly removed and resources are released back to the OCI pool.

## Prerequisites

1. Oracle Cloud Account. To sign up, visit [Oracle Cloud](https://cloud.oracle.com/en_US/tryit).
2. Jenkins installed with JDK 8 or higher.

## Compatibility
1. Plugin is tested on minimum Jenkins version 1.625.3, it may not work with versions early than 1.554.
2. For Jenkins versions between 1.554 and 1.625, pre-install 'bouncycastle API Plugin' plugin.


## Installation
The Oracle Cloud Infrastructure Compute Plugin is available via Jenkins Update Center or can be installed manually.

To install the plugin through Jenkins Update Center navigate to Manage Jenkins > Manage Plugins > Available, and search "Oracle Cloud Infrastructure Compute Plugin"

To build the plugin from repos, OCI Java SDK is required. OCI Java SDK is currently not published to Maven center. To compile and build the plugin, first install OCI Java SDK to the local Maven repository. Refer to OCI Java SDK licensing [here](https://github.com/oracle/oci-java-sdk/blob/master/LICENSE.txt).


##### Installing OCI Java SDK (Github) 
    $ git clone https://github.com/oracle/oci-java-sdk
    $ cd oci-java-sdk
    $ mvn compile install

##### Manualy Building and Installing OCI Oracle Cloud Infrastructure Compute Plugin
1. git clone repo 
2. Update pom.xml with OCI Java SDK you have installed

	```
	<oci-java-sdk.version>1.2.39</oci-java-sdk.version>
 	```

3. Compile and Install package

	```
	$ mvn package
	```

4. Install hpi:

	- Manage Jenkins > Manage Plugins > Click the Advanced tab > Upload Plugin section, click Choose File > Click Upload
**or**

	- Copy the downloaded .hpi file into the JENKINS_HOME/plugins directory on the Jenkins master


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
4. Click **Test Connection** to verify that Jenkins can successfully connect to the Oracle Cloud Infrastructure Service.
5. Click **Advanced** and if required, enter a number in the **Instance Cap** field to limit the maximum number of instances that can be created for this Cloud configuration.

### Add New Template
1. Click **Add** in **Instance Templates** section to add the OCI configuration. You can add multiple Templates to a Cloud configuration.

2. Input or select values in the 'Instance Template' section:
   - **Description** - Provide a description for this Template.
   - **Usage** - It's recommended that you select "Only build jobs with label expressions matching this node" for now.
   - **Labels** - Enter a unique identifier which allows Jenkins to pick the right instance template to run Job.
   - **Compartment** - Select a compartment where the instance will be created.
   - **Availability Domain** - Select the Availability Domain for your instance.
   - **Shape** - Select the Shape for your instance.
   - **Image** - Select the Image the instance will use. The Drop Down values are in the format - Image(Compartment). **Note:** Java should be installed on the image as a Jenkins requirement. Alternatively refer to "Init Script" in Step 10 to install Java on the newly launched instances.
   - **Virtual Cloud Network** - Select the Virtual Cloud Network for your instance.
   - **Subnet** - Select Subnet of your Virtual Cloud Network.
   - **Assign Public IP Address** - By default, the plugin will assign a public IP to an instance, provided the subnet has an available public IP range. If this Option is unchecked, only the private IP is assigned. 
   - **Connect Agent using Public IP**	- By default the Plugin will connect to the public IP of the instance (agent). If this Option is unchecked, the Plugin will connect to the private IP of the instance. 
   - **SSH Public Key Name** - Enter ssh public key for your instance. For more information see [Security Credentials](https://docs.us-phoenix-1.oraclecloud.com/Content/General/Concepts/credentials.htm).
   - **SSH Private Key** - Enter ssh private key for your instance. For more information see [Security Credentials](https://docs.us-phoenix-1.oraclecloud.com/Content/General/Concepts/credentials.htm).
   
    

4. Click **Verify SSH Key Pair** to verify the public key and private key entered are a match.

5. Click **Advanced** for more configuration options:
   - **Remote FS root** - Dedicated directory for Jenkins agent in instance.
   - **Remote SSH user** - ssh user used for Jenkins master to access instance. The ssh user should have written permission on Remote FS root directory.
   - **Instance Creation Timeout** - Number of seconds to wait for instance to reach state "ready", default value is 300. 
   - **Instance SSH Connection Timeout** - Number of seconds to wait for instance from state "ready" to be able to ssh connect from Jenkins master. Default value is 60.
   - **Idle Termination Minutes** - Number of minutes for Jenkins to wait before deleting and completely removing an idle instance. A value of 0 (or an empty string) indicates that instance should never be stopped/deleted. 
   - **Number of Executors** - Number of concurrent builds that Jenkins can perform. Value should be at least 1.
   - **Init Script** - You can define several lines of shell based commands to configure the instance (one-time) before the Jenkins Job runs. For example, if the image selected does not have Java pre-installed, you can add command "sudo yum -y install java"
   - **Init Script Timeout** - Number of seconds to wait for the completion of Init Script. Default value is 120 seconds.

6. Click **Save** or **Apply**

## Licensing
Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.

This plugin is licensed under the Universal Permissive License 1.0

This software is dual-licensed to you under the Universal Permissive License (UPL) and Apache License 2.0. 

See LICENSE.txt for more details.

## Changelog

For Changlog please refer to CHANGELOG.md.

## Contributing
Oracle Cloud Infrastructure Compute Plugin is an open source project. See CONTRIBUTING.md for more details.

Oracle gratefully acknowledges the contributions to Oracle Cloud Infrastructure Compute Plugin that have been made by the community.
