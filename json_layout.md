#JSON Config

There are several parts to the JSON config.

Whilst in the early development stages of Thornsec defining many of these were mandatory, as of v1.0, we have now preseeded these values with defaults, which can be overridden on a per-network or per-server basis.

The JSON is non-RFC compliant to the extent that comments (both single // and multi /\* */ line) are parsed out before crunching.  The reasons for this should hopefully be fairly obvious!

All key-value pairs are strings.

The basic layout of the JSON is as follows:

```json
{
	"networkName":{
		//Mandatory network-wide settings
		 
		//Server defaults, can be overriden on a per-server basis

		//VM config defaults, can be overridden on a per-VM basis
		
		"servers":{
			"server_x":{
				//Per-server options
			}
		},
		
		"internaldevices":{
			"device_y": { /*device info*/ }
		},
		
		"externaldevices":{
			"device_z": { /*device info*/ }
		},
		
		"users":{
			"user_a": { /*user info*/ }
		},

	}
}
``` 

#Global settings

##Network settings
These settings are applied to the whole network, and cannot be declared/overriden on a per-server basis.

###Mandatory Settings (No default values)

- ####domain
	Network's domain, e.g. "myorganisation.org"

- ####ip
IP address to access the network for configuration.
For internal deployments, this'll be the *internal* IP address of the router (e.g. 10.0.1.1).
For external service deployments, this'll be the *external* IP address for the box

- ####gpg
Identifier of the GPG pub key against which we store server credentials - the priv key for this GPG must be in the user's keychain on the local machine *running the jar*.
Usually this will be an email address.

- ####adminemail
This is the email address of the admin on your network - this is used to send automated alerts, e.g. bandwidth monitoring.




###Optional Settings (Default Values)

- ####class
IP address class.  Can be a, b, or c.
*Default Value:** a*** (10.0.0.0)

- ####dns
IP address of your external DNS server.
*Default Value:** 8.8.8.8*** (Google)

- ####adblocking
Whether to null-route known ad servers at the router.  Use wisely, as this is a rather blunt tool and may cause dramas to your users.
*Default Value:** no***

- ####autogenpasswds
*This value is only used if the passphrase for a given service doesn't already exist in your pass store.  If the passprase already exists, it will just use the one from the store*
Unless this is *declared* ***and*** *set to "true"*, Thornsec will not generate passwords for VMs.  In this case, when it spins up new VMs it will set the passphrase to "secret" and will expire it, requiring the user to log into the machine and change it before they can do further configuration.
If set to true, VMs will have a securely generated passphrase set, encrypted against the GPG key provided in the gpg config setting.
Passphrases are stored in the following format: Thornsec/{domain}/{network}/{vmLabel}
*Default Value:** false***

- #### vmbase
The root directory for all VM-based files on the hypervisor.  "/media/VMs" or similar is recommended.
*Default Value:** /media/VMs***


##Server defaults
Please note, these settings can be declared/overridden on a per-network and a per-server basis, if required

###Mandatory Settings (No default values)

- ####admins (mandatory)
This is an array of usernames which can SSH into a given server:
```
"admins":[ "foo", "bar" ]
```

All further details are taken from the corresponding entry in the user block


###Optional Settings (Default values)

- ####myuser
Login username for the servers
*Default Value:** thornsec***

- ####adminname
This is the real name of the admin
*Default Value:** Thornsec Admin User***

- ####connection
Can either be "tunnelled" or "direct".  If "tunnelled", all SSH connections to VMs are tunnelled via the IP address set above (generally an external IP adress).  If "direct", direct SSH connections are made between the machine running the jar and the machine to be configured.
*Default Value:** direct***

- ####adminport
This is the port which SSH will connect to
*Default Value:** 22***

- ####sshport
This is the port a server's SSHd will listen on - usually (although not *always*) the same as adminport
*Default Value:** 22***

- ####update
Whether to automatically run apt update && apt upgrade on configuration
*Default Value:** false***

- ####iface
This is the "external" interface of a given server.  Due to systemd's naming conventions, this will be based on the pci location of your network cards.  For VMs, this should always be enp0s3, but will change on the metals.
*Default Value:** enp0s3*** (Which will be correct for services)

- ####ram
Amount of RAM (in Megabytes) to allocate to each VM.  A minimum of 512 is required, with at least 1024 recommended.
*Default Value:** 1024***

- ####cpus
Number of CPUs to allocate to each VM.  This figure is non-exclusive, so you probably want to set it to the number of threads on the hypervisor, unless you're pinning.
*Default Value:** 1***

- ####disksize
This is the disk size to be allocated to the VM.  It's expandable on write up to the limit given here in Megabytes, but as most data is stored in the hypervisor, 8096 is more than enough for the majority of use cases.
*Default Value:** 8096***

- ####datadisksize
This is the size of the disk to mount for data under /media/metaldata in the VMs
*Default Value:** 8096***

- ####debianisourl
The url of the debian net-install ISO you wish to use.  Grab this from debian.org
*Default Value:** cdimage.debian.org/debian-cd/current/amd64/iso-cd/debian-9.1.0-amd64-netinst.iso***

- ####debianisosha512
The sha512 sum of the ISO.  You must get this from debian.org.  Make sure it's correct, or your VMs won't be created!
*Default Value:** 697600a110c7a5a1471fbf45c8030dd99b3c570db612044730f09b4624aa49f2a3d79469d55f1c18610c2414e9fffde1533b9a6fab6f3af4b5ba7c2d59003dc1***

- ####debianmirror
The mirror you wish your VMs to use (i.e. ftp.{country}.debian.org)
*Default Value:** ftp.uk.debian.org***


##Per-server settings
There are, in effect, three different types of servers you'll encounter in a given network.

###Mandatory Settings (No default values)

- ####subnet
The subnet assigned to this server (use 1-99, inclusive)

- ####metal (only required for VMs)
The name of the hypervisor

- ####mac
The mac address for this machine.  If a physical interface, the mac address of this interface, or if a VM you can generate one.

- ####types
This is an array describing what this machine is; router, metal (hypervisor), service (VM).

- ####extconnection (only required for Routers)
Either ppp, dhcp, or static.
	- ppp is for ppp assigned external IP (typical office environment)
	- dhcp is for dhcp assigned external IP (atypical, perhaps useful for segregating networks from your main one)
	- static is for statically assigned external IP - typically an externally hosted server

- ####bridge (only required for Routers/Metals)
Tells the hypervisor or router which interface to bridge its internal connections on. Either phy or virt.
	- phy - bridge to its physical iface (usual for a router)
	- virt - create internal bridges which don't touch the physical iface (usual for a metal)

- ####profiles
An array of the profiles to be configured/audited on this machine.  Try and keep this to one profile per machine.

###Router
This is intended to be a physically separate machine to your hypervisor (for internal deployments).  External deployments are a slightly different case, where you'd identify the metal as a router too, to keep traffic off the physical interface and to do internal routing between your various VMs.

A typical router looks like this:

```
			"router":{ //This is the hostname
				"types":["router"], //Tell it to be a router
				"subnet":"1",
				"extiface":"ppp0", //usually enpXsY or ppp0
				"pppiface":"enpXsY", //only needed if using ppp
				"bridge":"phy", //Usually routers do physical bridging unless they're also a metal where it'll be virt
				"extconnection":"ppp", // ppp, dhcp, or static
				"iface":"enpXsY" //Internal-facing interface
			},
```

###Metal
This is your hypervisor machine.  Bear in mind it will need to handle a number of VMs, so keep in mind RAM and storage when provisioning.

A typical metal looks like this:

```
			"hypervisor":{ //hostname
				"types":["metal"], //Tell it to be a hypervisor
				"subnet":"2",
				"extiface":"enpXsY", //Tell it which iface faces the router
				"bridge":"virt", //Get it to do virtual bridging for its VMs
				"mac":"de:ad:be:ef:ca:fe" //Mac address. Used in the router
			},
```

###Service
This is a VM.

A typical service looks like this:

```
			"staging":{
				"types":["service"],
				"subnet":"25",
				"metal":"hypervisor", //Host name of its metal
				"mac":"de:ad:be:ef:ca:fe",
				"profiles":["Drupal"], //Array of profiles for this machine
				"cnames":["cname.1", "cname.2", "cname3"],
				"externalip":"XXX.XXX.XXX.XXX" //This is optional, and is used for DNATing external connections to this machine
			},
```

You can override globals declared at the top on a per-VM basis by declaring different values in its stanza.

##List of available profiles
* CiviCRM
* Drupal
* DrupalCommons
* Etherpad
* Git
* LibreSwan
* MariaDB
* MISP
* Nginx
* OpenVPN
* Owncloud
* PHP
* Redmine
* SVN
* Tor
* Webproxy

##Devices
There are three different types of devices

###internalonly
This is a peripheral such as a printer which doesn't need a net connection or ability to talk to internal services or other peripherals except user machines, and should never be the one instigating a connection

###externalonly
These are devices such as guest wifi or VOIP phones which should never have access to anything internal (including internal DNS)

###users
This type of device has access to all internal services and peripherals, as well as the outside world

This block would typically look something like this:

```
		"internaldevices":{
			"printer": {"macs":["de:ad:be:ef:ca:fe"], "managed":"true"},
		},

		"externaldevices":{
			"guest_wifi": {"macs":["de:ad:be:ef:ca:fe"]}
		},
		
		"users":{
			"admin": {
				"fullname":"Admin User",
				"macs":["de:ad:be:ef:ca:fe", "de:ad:be:ef:ca:fe"],
				"sshkey":"ssh-ed25519 abcdefghijklmnopqrstuvwxyzzyxwvutsrqponmlkjihgfedcba fake-ssh-key"
			},
			"user":{"macs":["de:ad:be:ef:ca:fe"]}
		}
```