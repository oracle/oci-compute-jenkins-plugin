## Jenkins OCI Plugin ##
[![Buil d Status](https://travis-ci.org/oracle/jenkins-oci-plugin.svg?branch=master)](https://travis-ci.org/oracle/jenkins-oci-plugin)

**Jenkins OCI Plugin** allows users to access and manage cloud resources on the Oracle Cloud Infrastructure (OCI) from Jenkins.
A Jenkins master instance with Jenkins OCI Plugin can spin up agents (Instances) on demand within the Oracle Cloud Infrastructure, and remove the agents automatically once the Job completes.

### Table of Contents ###
1. [Features](#features)
1. [Prerequisites](#prerequisites)
1. [Installation](#installation)
1. [Configuration](#configuration)
2. [Known issues and limitations](#configuration)
1. [Licensing](#licensing)
1. [Contributing](#contributing)
2. [Changelog](#changelog)


### Features
**Jenkins OCI Plugin** provides functionality to dynamically allocate OCI resources for continuous integration tasks, and to bring up and down services or nodes as required to serve Jenkins Build Jobs.

After installing Jenkins OCI Plugin, you can add a OCI Cloud option and a Template with the desired Shape, Image, VCN, etc. The Template will have a Label that you can use in your Jenkins Job. Multiple Templates are supported. The Template options include Labels, Domains, Credentials, Shapes, Images, Agent Limits, and Timeouts.


### Prerequisites
Following Plugins are required:

- credentials v2.1.14 or later
- ssh-slaves v1.6 or later
- ssh-credentials v1.13 or later

[OCI Java SDK](https://github.com/oracle/oci-java-sdk) also needs to be installed. Refer to its licensing [here](https://github.com/oracle/oci-java-sdk/blob/master/LICENSE.txt).

### Installation
Currently before compiling the plugin, we need to compile and install [OCI Java SDK](https://github.com/oracle/oci-java-sdk). Refer to OCI Java SDK [issue 25](https://github.com/oracle/oci-java-sdk/issues/25). Tested with Maven versions 3.3.9 and 3.5.0. 

For example:

    $ git clone https://github.com/oracle/oci-java-sdk
    $ cd oci-java-sdk
    $ mvn compile install

Compile the Jenkins OCI Plugin hpi file using maven:

    $ git clone https://github.com/oracle/jenkins-oci-plugin
    $ cd jenkins-oci-plugin
    $ mvn compile hpi:hpi

Install hpi:

 - Manage Jenkins > Manage Plugins > Click the Advanced tab > Upload Plugin section, click Choose File > Click Upload
 
or

- Copy the downloaded .hpi file into the JENKINS_HOME/plugins directory on the Jenkins master

Restart Jenkins and "OCI Plugin" is visible in the Installed section of Manage Plugins.

### Configuration

**Create OCI Cloud credentials**

1. Go to Jenkins Dashboard → Credentials → Add Credentials
1. Select "Oracle Cloud Infrastructure Credentials"
1. Fill in your OCI details
1. To check, click "Verify Credentials"
1. Save

   
**Create SSH credentials**

1. Go to Jenkins Dashboard → Credentials → Add Credentials
1. Select "SSH Username with private key"
1. Fill in the username and private key (new oci agents will be configured to accept this ssh key, only RSA keys are supported currently)
1. Save

**Create OCI Cloud**

1. Go to Jenkins Dashboard → Configure System
1. In the "Cloud" section click "Add a new cloud" → OCI Cloud
1. Select the previously created OCI Cloud credentials
1. Click "Add Template"
1. Fill in your preferred details for your template adding a label (i.e. "sample_label"), shape, image, etc. 



**Running a Sample Job**
    
1. Go to Jenkins Dashboard → New Item
1. Create a Freestyle project
1. In the Job Configuration
	- Check "Restrict where this project can be run" and fill in the label (i.e. "sample_label")
	1. In "Build" section click "Add build step" → Execute shell → echo "Hello World"; sleep 60;
1. Save
2. On the newly configured job → Click "Build"

The job will wait in the queue until an instance in the OCI is created and running (you can watch the provisioning progress in the logs or in OCI). Once provisioned, the job is executed on the new build agent. After 10 mins being idle the instance is terminated.


### Known issues and Limitations
The following are a list of known issues and limitations for this plugin. Also you can log any issues, feedback, or new requirements under the “Issues” tab of this GitHub repository.

- Ability to keep Agent forever for analysis.
- Ability to create agent independently of jobs.
- Currently there is nothing seen in Jenkins UI during Instance provisioning.
- Implement a "ProvisionStrategy" for concurrent Job Runs. To be able to run same jobs concurrently. Workaround is to create "parametrized" job.
- Currently only RSA keys are supported for Images. 
- Add a Max Retry limit on Provisioning agents. Currently if a agent cannot be provisioned, it keeps trying.
- Helps for Template options.
- In case of timeout or other issue there is nothing to stop provisioning.
- Input validation.
- Log for provisioning Agent to show more content.
- Use shortname rather than full hostname for OCI Instances.
- Sane reporting of invalid OCI credentials. 
- Update tasks status when cloud errors happen.

### Licensing
Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.

This plugin is licensed under the Universal Permissive License 1.0

See [LICENSE.txt](https://github.com/oracle/jenkins-oci-plugin/blob/master/LICENSE.txt) for more details.

### Contributing
Jenkins OCI Plugin is an open source project. See [CONTRIBUTING.md](https://github.com/oracle/jenkins-oci-plugin/blob/master/CONTRIBUTING.md) for more details.

Oracle gratefully acknowledges the contributions to Jenkins OCI Plugin that have been made by the community.

### Changelog

See [CHANGELOG](https://github.com/oracle/jenkins-oci-plugin/blob/master/CHANGELOG.md).
