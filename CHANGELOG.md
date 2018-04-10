# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/).

## 1.0.1 - April 2018
### Fixed

- Idle Termination Minutes. 0 now working as expected and Instance will not Terminate,

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
