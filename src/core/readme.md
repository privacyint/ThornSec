# ThornSec Core

This is the framework which allows ThornSec to produce and run its unit tests, but it's pretty useless without profiles.

## /unit
This contains our various unit tests, the fundamental building blocks for a ThornSec profile.  You should always use correct unit test for the task if at all possible, only resorting to "SimpleUnit" where a type of unit test doesn't already exist.  If you use a SimpleUnit to do a similar thing more than once, please think about creating a new Unit and submitting a pull request.

All unit tests will check for their respective params on audit, and will enforce them on config.

### /unit/fs
Unit tests related to file system operations.  Remember - everything on *nix is a file.

#### DirMountedUnit
Has directory foo been mounted?

#### DirOwnUnit
Who owns a directory foo?

#### DirPermsUnit
Which permissions does directory foo have?

#### DirUnit
Does directory foo exist?

#### FileAppendUnit
Has string foo been appended to file bar?

#### FileChecksumUnit
Does this file foo's checksum match "known good"?  (Bear in mind, this requires our "known good" checksum to be correct in the first place...)

#### FileDownloadUnit
Has file foo been downloaded to directory bar?

#### FileEditUnit
Has sed correctly edited file foo?

#### FileOwnUnit
Does file foo have the correct ownership?

#### FilePermsUnit
Does file foo have the correct permissions?

#### FileUnit
Does file foo exist, and does its content match string bar?

#### GitCloneUnit
Has git repo foo been cloned to dir bar?

### /unit/pkg
Unit tests related to apt packages

#### InstalledUnit
Has package foo been installed on the current machine?

#### RunningUnit
Is package foo running on the current machine?

### /data
This holds all the information about various devices on your network.

#### DeviceData
Information about devices (superuser, user, intonly, extonly)

#### NetworkData
Information which is network-wide.

#### ServerData
Information which is specific to a given server.

### /core/model
This contains models for configuring various different aspects of a machine.

#### AptSourcesModel
Add the various apt sources to /etc/apt/sources.list.d/

#### BindFSModel
Build our bindfs mounts.

#### DeviceModel
Networking information about a device.

#### FirewallModel
Configures a machine's iptables - default drop, whitelist.

#### InterfaceModel
Network interfaces for a given machine.

#### ProcessModel
A list of expected processes to be running on a given machine.

#### ServerModel
This represents a server.  Incorporates all of the other models to represent various aspects of it.

#### ThornsecModel
This reads our JSON into various ThornSec settings.

#### UserModel
A list of expected users to be on a given machine.

