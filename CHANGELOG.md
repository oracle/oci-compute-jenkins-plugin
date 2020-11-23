# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/).

## 1.0.11 - November 2020

### Fixed

- OCI Java SDK 1.26.0
- Tags Template option
- Custom Instance Name Prefix option
- root compartment added to Compartment list
- Images added to the Stop/Start filter



## 1.0.10 - September 2020

### Fixed

- Fix java.util.NoSuchElementException: No value present



### 1.0.9 - September 2020

### Added

- Network **Subnet Compartment** field.
- **Network Security Groups** field.
- **Identical Named Images** checkbox to automatically select the newest Image if multiple Images exist with same name.
- **Stop on Idle Timeout** checkbox so an instance is stopped and not terminated when the Idle timeout expires.
- Log if an Instance was created via Jenkins Job label or via Jenkins Nodes.

## 1.0.8 - July 2020

### Added

- OCI Java SDK 1.19.2
- Support for E3/Flex Compute Shapes. **Note:** After upgrade please check all OCI Cloud values are OK in Manage Jenkins > Manage Nodes and Clouds > Configure Clouds. Then Click **Save**.
- Improvements in Instance Termination behavior.

### Fixed

- Fix in Exception handling to avoid dangling instances.

## 1.0.7 - June 2020

### Added

- OCI Java SDK 1.17.4

## 1.0.6 - September 2019

### Added

- OCI API keys and SSH Keys are now defined in Jenkins Credentials. **Note:** if upgrading you need to update the values in your existing Cloud configuration(s).
- Support for Instance Principals and calling services from an Instance. See [Calling Services from an Instance](https://docs.cloud.oracle.com/iaas/Content/Identity/Tasks/callingservicesfrominstances.htm) documentation for additional information.
- OCI Java SDK 1.7.0

## 1.0.5 - July 2019

### Added
- Jenkins Master's IP Address to the Instance Names in OCI i.e. jenkins-**12.191.12.125**-182258c6-7dc7-4d8c-acce-1a292a56cfaa.
- Regional subnets in the Virtual Networking service support.
- OCI Java SDK 1.5.11

### Changed
- Default values for **Instance Creation Timeout** and **Instance SSH Connection Timeout** to 900.

### Fixed
- OCI Slaves removing from Jenkins when Jenkins loses Network Connectivity.


## 1.0.4 - November 2018
### Fixed
- Compartments listed are no longer limited to 25 values.
- Child Compartments are now visible in Compartments.

### Added
- Template Instance Cap functionality. Instance Cap can now be placed at Template level.
- "Virtual Cloud Network Compartment" Drop Down in Template configuration to access Network resources in separate compartments. Improves Template loading performance. **Note:** if upgrading from v1.0.3 (or earlier) and the Networks resources is in a separate compartment than the default Compartment, you may have to update the values in your existing Template configuration.

## 1.0.3 - October 2018
### Fixed
- Fix "I/O error in channel oci-compute" java.io.EOFException severe messages in Jenkins log. 
- Fix issue where some values fail due to OCI API limit being exceeded with large number of Templates.

### Changed
- Plugin Description seen in Plugin's Available screen.

### Added
- "Max number of async threads" Field in Cloud configuration. Allows user to specify the max number of async threads to use when loading Templates configuration.
- "Image Compartment" Drop Down in Template configuration for images in separate compartments. **Note:** if upgrading from v1.0.2 (or earlier) and the Images are in a separate compartment than the default Compartment, you may have to update the values in your existing Template configuration.


## 1.0.2 - June 2018
### Fixed
- Instance cap can no longer be exceeded
- Fix error on Node Configuration Screen

### Changed
- Subnets now filtering by Availability Domain
- Use Jenkins HTTP proxy configuration for OCI API calls
- Prevent termination of temporarily offline Agents

### Added
- Faster loading of Cloud and Template configuration options in Jenkins Configure screen
- Better error description for remote machine with no Java installed
- "Name" and "Number of Executors" reconfiguration options in the Nodes > Configure Screen

## 1.0.1 - April 2018
### Fixed

- Idle Termination Minutes. 0 now working as expected and Instance will not Terminate.

- Fixed broken links in Plugin Help options.


- Fixed "unexpected stream termination" issue which removes HTTP Proxy for ssh connection to agents.
- ssh credentials are now encrypted in Jenkins config file.


### Changed
- Shorten Compartment Drop-Down names and removed bloated bracket content.

### Added
- Ability to access Images, Virtual Cloud Network, and Subnet items from separate Compartments.

- Checkbox to attach Public IP to Instances. If this option is unchecked, only the private IP is assigned. 


- Checkbox to use Public IP to ssh to instances. If this Option is unchecked, the Plugin will connect to the private IP of the instance. 

## 1.0.0 - December 2017
### Added
- Initial Release
- Support added for OCI resource allocation via Jenkins plugin
