# Thornsec Installation Guide

## Requirements
Thornsec can be deployed in a variety of environments. To install and run it you will need:

### 0. Router

In terms of hardware, it needs (at the very least) a computer to function as a router, with two ethernet ports.  This router need not be expensive - for a small organisation (say 10 endpoints) a Raspberry Pi 3 is more than capable (although it will require an ethernet dongle).  We at Privacy International bought an enterprise-grade HP machine which cost us £..., and I have recently bought a thin client for £19 (Intel Atom, 2GB RAM, 20GB SSD), which will be more than up to the task.


### 1. Server

One or more server(s) you own (capable of virtualisation and with adapted disk space if you're planning on hosting files) OR one or more private server(s) you are renting. In both configuration, the router and hyperviser can both be on the same machine or run on a dedicated machine.
The machine(s) you will be using need to fit the minimum requirements to run Thornsec:

```
- Multithreading
- 8gb RAM
- Hardware virtualisation extensions (preferable)
- 250gb hard drive space
```

If you want to host services internally, a £175 machine (Core i5, 8GB RAM, 500GB SSD), can easily run ThornSec as it is designed to run on consumer-grade hardware (so for instance a ThinkPad, which can be picked up very cheaply). If you are a bit short on power running everything on this machine you can add a Raspberry Pi 3 to function as a router (although it will require an ethernet dongle).

> Both the Router and the Hypervisor need to be able to run Debian GNU/Linux, and the machine you will use to do the configuration needs to have (at the very least) a Java Runtime installed.  To take fuller advantage of the inbuilt functionality for password management, and one-click configuration, you should have a PGP key (I recommend one only for this use), have [pass](https://www.passwordstore.org/) installed, and be on a machine with the SSH private key installed.

### 2. Local machine

To manage your Thornsec install on your server you will obviously need a local machine which can run jar files (need JRE) and from wich you will be able configure your server and manage the Thornsec installation.

## Initial setup

When it comes to the OS, Thornsec expects Debian Stretch. So you will need to install Debian on your server(s) and partition your disks as follows using LVM (disk space must be adapted to your available storage):

```
* OS partition (8Go recommended) - this will be your hypervisor
* Boot parition
* Swap partition
* Rest unformatted - this will be where the VMs and data are
```

Install Debian on the OS partition. Luks encrypt the unformated partition and then create 3 partitions on this one:

```
* Data = /srv/ThornSec/disks/data - Where databases, files and all sort of data will go (so you can rebuild a VM and still have the data)
* Logs = /srv/ThornSec/logs
* Backups = /srv/ThornSec/backups
```

Mount the created partitions and restart the machine. Congrats, you are now ready to run Thornsec! 

Here is a list of the information you will need to create a configuration file, most of these can be found using the `ifconfig` command :
```
- IP address(es) - e.g. 1.1.1.1
- MAC address(es) - e.g.: 00:00:00:00
- Network Ports name(s) - e.g. enp0S1 
```

## Build your config
Thornsec configuration of your network happens thanks to only one file, a JSON file that contains information about your physical infrastructure and the services you want to run on it. This is where you define what service you want to run, on which server it will run, behind which load balancer it goes, what procol are used etc...
Two tools are currently at your disposal to build your config (a.k.a the JSON file):

- Looking at the example JSON and adapating it to your needs: https://github.com/privacyint/thornsec-core/blob/master/example_json.md
- Using the JSON creator tool (BETA) available here to build your network and export a JSON file: https://github.com/privacyint/thornsec-json-creator


Both can work together, you can either look at the JSON first and import it in the JSON creator to visualize it and edit it. Or you can use the JSON creator to generate a config from scratch. The JSON creator is still in beta and quite unstable though so you  might encounter issue when creating the file (but the files exported should work with Thornsec).

## Run Thornsec
Once your config file is ready, the last step is to run Thornsec on your local machine, using a build from https://github.com/privacyint/thornsec-builds.
Run Thornsec with your config file: 

```
java -jar ThornSec-manual-##-##-##.jar ~/path/to/your/config.json
```

The output from ThornSec is a bash script, which is designed to be human-readable.  This shell script can either be run directly from the GUI (using SSH), or you can export the script to a local file and transfer/run manually on the target machine.


