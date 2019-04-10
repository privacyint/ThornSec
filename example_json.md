## Example Config
```
{
	"office":{
		/****************************************************************************
		* Mandatory, per-network, cannot be declared/overridden on per-server basis *
		****************************************************************************/
		//Configuration IP for the network
		"ip":"10.0.0.1",
		//Domain to use on the network
		"domain":"myorganisation.org",
		//Password storage/generation GPG key
		"gpg":"securepassword@myorganisation.org",
		//Email to use for alerts
		"adminemail":"admin@myorganisation.org",
		
		/****************************************************************************
		* Optional, per-network, cannot be declared/overridden on per-server basis  *
		****************************************************************************/
		//Upstream DNS server IP (default 1.1.1.1/1.0.0.1)
		"dns":["1.1.1.1", "1.0.0.1"],
		//DNS over TLS? (default false)
		"dtls":"true",
		//Whether to ad-block at the router. Uses Steven Black's hosts list, use wisely. (default false)
		"adblocking":"false",
		//Whether to auto-generate secure passphrases for VMs if they don't already have one in the password store. (default false)
		"autogenpasswds":"false",
		//Where to put all ThornSec-related files on the hypervisor (default /srv/VMs)
		"vmbase":"/srv/VMs",		

		/****************************************************************************
		* Optional, server defaults, can be overridden on a per-server basis        *
		****************************************************************************/
		//*nix login user name (default thornsec)
		"myuser":"thornsec",
		//Default SSH connection type (default direct)
		"connection":"direct",
		//Default SSH connection port (default 22)
		"adminport":"65422",
		//Default SSH listening port (default 22)
		"sshport":"65422",
		//Run updates on server if require (default false)
		"update":"false",
		//Default "external" interface for a given server. (default enp0s17)
		"lan":[{"iface":"enp0s17"}]
		//Amount of RAM, in mb, to assign to a service (default 1024)
		"ram":"1024",
		//Non-exclusive number of cpus to assign to a service (default 1) - this should be <= the number of *physical* CPUs in the hypervisor
		"cpus":"8",
		//Disk size, in mb, to be allocated to the root filesystem of a service (default 8096)
		"disksize":"8096",
		//Disk size, in mb, to be allocated to the data storage of a service (default 8096)
		"datadisksize":"8096",
		//URL of the debian ISO you wish to use to build VMs (>= 9.0 supported, default is pulled from the latest netinst on cdimage.debian.org)
		"debianisourl":"cdimage.debian.org/debian-cd/current/amd64/iso-cd/debian-9.1.0-amd64-netinst.iso",
		//Known-good sha512 checksum of the above ISO (this comes from debian.org, default is pulled from the SHA512SUM file on cdimage.debian.org)
		"debianisosha512":"697600a110c7a5a1471fbf45c8030dd99b3c570db612044730f09b4624aa49f2a3d79469d55f1c18610c2414e9fffde1533b9a6fab6f3af4b5ba7c2d59003dc1",		
		//Debian mirror to be used for packages (default free.hands.com)
		"debianmirror":"free.hands.com",
		//Subdirectory to be used for pulling packages (default /debian)
		"debiandirectory":"/debian",
		//Admins (accounts to create on our services)
		"admins":["admin1", "admin2"],






		/****************************************************************************
		* Server definitions                                                        *
		****************************************************************************/
		"servers":{
			//Example of a router configured using _PPPoE_
			//This is probably your default setup for a router if you're using ADSL, VDSL or point-to-point tunnel
			"ppp_router":{
				//Declare it's a router
				"types":["router"],
				//Declare the PPPoE iface on WAN
				"wan":[ {"iface":"enp0s0", "inettype":"ppp"} ],
				//Declare the LAN iface (bridged to lan0 internally)
				"lan":[ {"iface":"enp2s2"} ],
				//Verbose logging from iptables
				"debug":"true",
			},
			//Example of a router configured using dhcp
			//This is probably only used if you're trying to create a network inside your main network (VMs, perhaps)
			//Also likely if you're connected using coax, Ethernet over the last mile, or LTE/3G/WiMax etc
			"dhcp_router":{
				//Declare it's a router
				"types":["router"],
				"wan":[ {"iface":"enp1s2", "inettype":"dhcp"} ],
				"lan":[ {"iface":"enp4s2"} ],
				"debug":"true",
			},
			//Example of a router configured statically
			//You'll probably only use this for externally hosted networks
			"static_router":{
				//Declare it's a router
				"types":["router"],
				//Tell it to use static external connection(s)
				"wan":[
					{ "iface":"eno1", "inettype":"static", "address":"1.2.3.4", "netmask":"255.255.255.0", "broadcast":"1.2.3.255", "gateway":"1.2.3.1" },
					{ "iface":"eno1", "inettype":"static", "address":"2.3.4.5", "netmask":"255.255.255.0", "broadcast":"2.3.4.255"}
				],
				"lan":[ { "iface":"eno2" } ],
			},
			"hypervisor":{
				//Tell it to be a hypervisor machine
				"types":["metal"],
				"lan":[ {"iface":"enp1s0f0", "mac":"de:ad:be:ef:ca:fe"} ],
				"allowegress": [
					{"destination":"backupserver.com", "ports":"443"}
				]
			},
			"nginx_lb":{
				//This is a service
				"types":["service"],
				//On our hypervisor
				"metal":"hypervisor",
				//Which is a reverse proxy
				"profiles":["Webproxy"],
				//Proxying the following web services
				"proxy":[ "etherpad",
						   "owncloud"
						]
			},
			"owncloud":{
				"types":["service"],
				"metal":"hypervisor",
				//Override the data storage for this machine
				"datadisksize":"61440",
				//As it's Nextcloud
				"profiles":["Nextcloud"],
				//Which has many names
				"cnames":["www.nextcloud", "contacts", "calendar", "documents"]
			},
			"etherpad":{
				"types":["service"],
				"metal":"hypervisor",
				"profiles":["Etherpad"],
				"cnames":["pads"],
				//We want a different set of admins on this machine
				"admins":["admin2", "admin3"],
				//This machine needs to be able to call out to some other destinations, because reasons
				"allowegress":[{"destination":"1.2.3.4", "ports":"22,80,443"},
				               {"destination":"google.com", "ports":"443"}
				 ]
			},
			"vpn":{
				"types":["service"],
				"subnet":"3",
				"metal":"hypervisor",
				"profiles":["LibreSwan"],
				"cnames":["office"],
				//We want to DNAT external traffic coming into the hypervisor directly to this machine on its VPN UDP ports
				"externalip":"1.2.3.4"
			}
		},
		
		
		
		
		
		
		/****************************************************************************
		* Device definitions; superusers, users, internal and external-only devicen *
		****************************************************************************/
		//Setting "throttle" to "false" switches off QoS throttling
		//Manage will open up :80 for devices which are managed via a web browser


		"internaldevices":{
			//Internal connections only, but can be managed by admins on ports {22|80}
			"printer": {"macs":["de:ad:be:ef:ca:fe"], "throttle":"true", "managed":"true", "ports":"22,80"},
		},
		
		"externaldevices":{
			//Interwebs only
			"guest_wifi": {"macs":["de:ad:be:ef:ca:fe"], "throttle":"false"},
			"staff_wifi": {"macs":["de:ad:be:ef:ca:fe"]}
		},
		
		"users":{
			"admin1":{
				"fullname":"Dr McNuggets",
				"macs":["de:ad:be:ef:ca:fe", "de:ad:be:ef:ca:fe"],
				"sshkey":"ssh-ed25519 abcdefghijklmnopqrstuvwxyzzyxwvutsrqponmlkjihgfedcba fake-ssh-key"
			},
			"admin2":{
				"fullname":"The Incredible Mr Hong",
				"macs":["de:ad:be:ef:ca:fe", "de:ad:be:ef:ca:fe"],
				"sshkey":"ssh-ed25519 abcdefghijklmnopqrstuvwxyzzyxwvutsrqponmlkjihgfedcba fake-ssh-key"
			},
			"admin3":{
				"fullname":"SemanticX",
				"macs":["de:ad:be:ef:ca:fe", "de:ad:be:ef:ca:fe"],
				"sshkey":"ssh-ed25519 abcdefghijklmnopqrstuvwxyzzyxwvutsrqponmlkjihgfedcba fake-ssh-key"
			},
			"user1":{"macs":["de:ad:be:ef:ca:fe"]},
			"user2":{"macs":["de:ad:be:ef:ca:fe"]},
			"user3":{"macs":["de:ad:be:ef:ca:fe"]},
			"user4":{"macs":["de:ad:be:ef:ca:fe"]},
			"user5":{"macs":["de:ad:be:ef:ca:fe"]},
			"user6":{"macs":["de:ad:be:ef:ca:fe"]},
			"user7":{"macs":["de:ad:be:ef:ca:fe"]},
			"user8":{"macs":["de:ad:be:ef:ca:fe"]}
		}
	}
}

```
