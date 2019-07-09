# ThornSec

Not orgsec. Not infosec. Not opsec. Hopefully just a small thorn in the side of your adversaries.

Like with Androcles, a mighty lion can easily be felled by the smallest thorn.

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


As an example, Thornsec is used by Privacy International to run all of our internal and external services on a mix of locally owned and hosted machines. So if you are an organisation using Dropbox for file sharing, Google Drive for collaborative editing and Slack for project management and are looking to get away from these private services (for cost or political reasons), Thornsec can help you set up and manage the open-source equivalent of these services on a server you own or rent.

## Why?

ThornSec has been a [journey of discovery for Privacy International](https://privacyinternational.org/blog/989/our-history-security-and-what-we-do-now).

It is our attempt to not only practice what we preach, [but to rethink what a "secure" network and organisation would look like](https://privacyinternational.org/advocacy-briefing/622/what-policy-makers-can-learn-about-cyber-security-thornsec), starting from first principles.

It has forced us to strip back our organisation to its basic components, and to rebuild it using Free and Open Source Software, in a way which is automatable, reproducible, and (most importantly) *auditable*.

Security is hard.  It can also be expensive to get right, and relies on entirely the knowledge and experience of those working on it.

Charities and NGOs have it worse than your average SME; funds are tight, and restricted as to what they can be spent on.  In hiring, it's often a chicken-and-egg problem of knowing what you need before you can hire someone to tell you what you need.

ThornSec is designed to be run by someone with little or no tech expertise, with a highly constrained budget, on consumer hardware.

It is designed to obscure as much of the "heavy lifting" from the end user as possible - they don't really need to understand, for instance, how virtualisation works, or how networking works.  We try, instead, to boil it down to basics: we have users, we have devices, we have a router, and we want to run services.  There is no "secret sauce", just taking advantage of everything in the *nix world being a file.

## How does it work?
*You get a VM, you get a VM, everybody gets a VM!*

Thornsec basically runs services in securly configured VMs only allowed to talk to the hypervisor and specific authorised destination (e.g.: Drupal.com to update your drupal instance)

Indeed, ThornSec, as a framework, has two major components:

```
- Router
- Hypervisor
```

ThornSec provides a fully-routed network, meaning all networking comms go via the Router.  Each user, device, and machine on a network can have their own tightly-defined subnet, and a tight set of whitelisted rules on what is allowed to talk to where.
The Hypervisor allows the quick spinning up/tearing down of VMs, and handles automated daily backups.

## What reporting does it provide?

At its base, ThornSec is a framework which provides a series of human-readable unit tests in a shell script.  On an audit run, it will check the configuration of a given machine, and will report back to the user any unit tests which failed.  On configuration, it will attempt to fix any unit tests which fail.

Because its configuration file provides a holistic view of the network as a whole, we can approach firewalling as a whitelist.

Every day, each user on the network will receive an email breakdown of their upload and download traffic.  This is to allow them to notice "odd" traffic patterns.  If the router detects a large upload, it will alert the user.  Any time anyone SSHs into any of the servers, all admins associated will receive an email with information about the connection.

## Why not {Chef,Puppet,[...]}

If you or your organisation are already comfortable using these tools, then great! ThornSec probably won't give you anything you don't already have.

The focus of ThornSec, however, is slightly different.  It is aimed specifically at NGOs to try and make them more secure, as well as giving them the opportunity to run internal services such Etherpad.


## What does it need to run?

See Requirements in the [Installation guide](Installation.md)

## Primary Platform Goals

1. help systems and networks be more secure than default;
2. complete operational transparency;
3. facilitate cooperation amongst sysadmins;
4. minimal pre-requisites;
5. avenue for learning with others.

## I want to give it a try!

There are several ways for you to try ThornSec out:
 - Download a pre-configured Raspberry Pi image to use as a router
 - Download the VirtualBox images to build a virtual network, with an example json file
 - Install Debian on a physical machine, and configure it using ThornSec!
 
You will find more information in the [Installation guide](Installation.md)

## Want to contribute?

1. What command should a sysadmin run to check if there is an issue?
2. What should the ouput of that command be if the test is passed or failed?
3. Bonus points: what command should a sysadmin run to resolve the issue?

If you can answer the first 2 questions then get in touch or write the appropriate test for inclusion into the codebase. If you can answer question 3 that's even better!

Learn more about the [structure](doc/structure.md) behind ThornSec so you can contribute.

## No Use

Not for use by governments of United Kingdom, United States and Israel.

