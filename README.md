# ThornSec

Not orgsec. Not infosec. Not opsec. Hopefully just a small thorn in the side of your adversaries.

Like with Androcles, a mighty lion can easily be felled by the smallest thorn.

# Why?

ThornSec has been a [journey of discovery for Privacy International](https://privacyinternational.org/blog/989/our-history-security-and-what-we-do-now).

It is our attempt to not only practice what we preach, [but to rethink what a "secure" network and organisation would look like](https://privacyinternational.org/advocacy-briefing/622/what-policy-makers-can-learn-about-cyber-security-thornsec), starting from first principles.

It has forced us to strip back our organisation to its basic components, and to rebuild it using Free and Open Source Software, in a way which is automatable, reproducible, and (most importantly) *auditable*.

Security is hard.  It can also be expensive to get right, and relies on entirely the knowledge and experience of those working on it.

Charities and NGOs have it worse than your average SME; funds are tight, and restricted as to what they can be spent on.  In hiring, it's often a chicken-and-egg problem of knowing what you need before you can hire someone to tell you what you need.

ThornSec is designed to be run by someone with little or no tech expertise, with a highly constrained budget, on consumer hardware.

It is designed to obscure as much of the "heavy lifting" from the end user as possible - they don't really need to understand, for instance, how virtualisation works, or how networking works.  We try, instead, to boil it down to basics: we have users, we have devices, we have a router, and we want to run services.  There is no "secret sauce", just taking advantage of everything in the *nix world being a file.

# What reporting does it provide?

At its base, ThornSec is a framework which provides a series of human-readable unit tests in a shell script.  On an audit run, it will check the configuration of a given machine, and will report back to the user any unit tests which failed.  On configuration, it will attempt to fix any unit tests which fail.

Because its configuration file provides a holistic view of the network as a whole, we can approach firewalling as a whitelist.

Every day, each user on the network will receive an email breakdown of their upload and download traffic.  This is to allow them to notice "odd" traffic patterns.  If the router detects a large upload, it will alert the user.  Any time anyone SSHs into any of the servers, all admins associated will receive an email with information about the connection.

# Why not {Chef,Puppet,[...]}

If you or your organisation are already comfortable using these tools, then great! ThornSec probably won't give you anything you don't already have.

The focus of ThornSec, however, is slightly different.  It is aimed specifically at NGOs to try and make them more secure, as well as giving them the opportunity to run internal services such Etherpad.

# What does it do?

ThornSec, as a framework, has two major components:

    Router

    Hypervisor


ThornSec provides a fully-routed network, meaning all networking comms go via the Router.  Each user, device, and machine on a network can have their own tightly-defined subnet, and a tight set of whitelisted rules on what is allowed to talk to where.

The Hypervisor allows the quick spinning up/tearing down of VMs, and handles automated daily backups.

# What does it need to run?

In terms of hardware, it needs (at the very least) a computer to function as a router, with two ethernet ports.  This router need not be expensive - for a small organisation (say 10 endpoints) a Raspberry Pi 3 is more than capable (although it will require an ethernet dongle).  We at Privacy International bought an enterprise-grade HP machine which cost us £..., and I have recently bought a thin client for £19 (Intel Atom, 2GB RAM, 20GB SSD), which will be more than up to the task.

If you're wanting to host services internally, you'll also need a computer to be your Hypervisor.  Again, this needn't be expensive - the machine I use at home cost me £175 (Core i5, 8GB RAM, 500GB SSD), and ThornSec is designed to run on consumer-grade hardware (so for instance a ThinkPad, which can be picked up very cheaply).

Both the Router and the Hypervisor need to be able to run Debian GNU/Linux, and the machine you will use to do the configuration needs to have (at the very least) a Java Runtime installed.  To take fuller advantage of the inbuilt functionality for password management, and one-click configuration, you should have a PGP key (I recommend one only for this use), have [pass](https://www.passwordstore.org/) installed, and be on a machine with the SSH private key installed.

# Primary Platform Goals

1. help systems and networks be more secure than default;
2. complete operational transparency;
3. facilitate cooperation amongst sysadmins;
4. minimal pre-requisites;
5. avenue for learning with others.

# I want to give it a try!

There are several ways for you to try ThornSec out:
 - Download a pre-configured Raspberry Pi image to use as a router
 - Download the VirtualBox images to build a virtual network, with an example json file
 - Install Debian on a physical machine, and configure it using ThornSec!

# Want to contribute?

1. What command should a sysadmin run to check if there is an issue?
2. What should the ouput of that command be if the test is passed or failed?
3. Bonus points: what command should a sysadmin run to resolve the issue?

If you can answer the first 2 questions then get in touch or write the appropriate test for inclusion into the codebase. If you can answer question 3 that's even better!

Learn more about the [structure](doc/structure.md) behind ThornSec so you can contribute.

# No Use

Not for use by governments of United Kingdom, United States and Israel.

