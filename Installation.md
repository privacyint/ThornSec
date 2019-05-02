# Thornsec Installation Guide
## What is Thornsec and why would I want it
Thornsec is service management system that uses virtualisation (with VirtualBox) to run applications in secure and separate environments. 

Thornec's purpose is to help small and medium group of people easily deploy, manage and maintain services in a secure way instead of relying on third-party providers. Whether you are an organisation, a small company, a wanna-be service provider or a very motivated individual, Thornsec offers a simple way to quickly and securely deploy,  audit, configure, and manage services such as Nextcloud, Etherpad or a CMS such as Drupal in secured virtual machines.  A full list of currently supported profiles can be found at https://github.com/privacyint/thornsec-profiles/.

Here is a list of the feature it offers:
- Entire network configuration (from router management to iptables rules for each service)
- Fully-routed network, using whitelisting for egress
- Easy integration via profiles with existing web services (Nextcloud, Etherpad, Redmine, Drupal, Grav...)
- User and external devices management (printers, wifi hotspots...)
- Automated backup management
- Automated updates
- Hypervisor to easily monitor and manage services through VMs (restart, wipe and rebuild, add, remove...)
- Bandwidth alerts on potential exfiltration


As an example, Thornsec is used by PI to run all of our internal and external services on a mix of locally owned and hosted machines. So if you are an organisation using Dropbox for file sharing, Google Drive for collaborative editing and Slack for project management and are looking to get away from these private services (for cost or political reasons), Thornsec can help you set up and manage the open-source equivalent of these services on a server you own or rent.

## How does it work
You get a VM, you get a VM, everybody gets a VM! 
Thornsec basically runs services in securly configured VMs only allowed to talk to the hypervisor and specific authorised destination (e.g.: Drupal.com to update your drupal instance)

Indeed, ThornSec, as a framework, has two major components:

```
- Router
- Hypervisor
```

ThornSec provides a fully-routed network, meaning all networking comms go via the Router.  Each user, device, and machine on a network can have their own tightly-defined subnet, and a tight set of whitelisted rules on what is allowed to talk to where.
The Hypervisor allows the quick spinning up/tearing down of VMs, and handles automated daily backups.

## Requirements
Thornsec can be deployed in a variety of environments. To install and run it you will need:

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


