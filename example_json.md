## Example Config
```
{
	"office":{
		/****************************************************************************
		* Mandatory, per-network, cannot be declared/overridden on per-server basis *
		****************************************************************************/
		//External IP of the network
		"ip":"10.0.1.1",
		//Domain to use on the network
		"domain":"myorganisation.org",
		//Password storage/generation GPG key
		"gpg":"securepassword@myorganisation.org",
		//Email to use for alerts
		"adminemail":"admin@myorganisation.org",
		
		/****************************************************************************
		* Optional, per-network, cannot be declared/overridden on per-server basis  *
		****************************************************************************/
		//IP address class ({a,b,c}, default a)
		"class":"a",
		//External DNS server IP (default 8.8.8.8)
		"dns":"8.8.8.8",
		//Whether to ad-block at the router. Rather blunt, use wisely. (default no)
		"adblocking":"no",
		//Whether to auto-generate secure passphrases for VMs if they don't already have one in the password store. (default false)
		"autogenpasswds":"false",
		//Where to put all VM files on the hypervisor (default /media/VMs)
		"vmbase":"/media/VMs",		

		/****************************************************************************
		* Mandatory, server defaults, can be overridden on a per-server basis       *
		****************************************************************************/
		"keys":[
			"ssh-ed25519 ABCDEFGHIJKLMNOPQRSTUVWXYZ example-nonvalid-pubkey",
			"ssh-ed25519 ZYXWVUTSRQPONMLKJIHGFEDCBA second-example-nonvalid-pubkey"
		],

		/****************************************************************************
		* Optional, server defaults, can be overridden on a per-server basis        *
		****************************************************************************/
		//*nix user name (default thornsec)
		"user":"thornsec",
		//Full name of the admin account (default Thornsec Admin User)
		"adminname":"Dr McNuggets",
		//Default SSH connection type (default direct)
		"connection":"direct",
		//Default SSH connection port (default 22)
		"adminport":"65422",
		//Default SSH listening port (default 22)
		"sshport":"65422",
		//Run updates on server if require (default false)
		"update":"false",
		//Default "external" interface for a given server. (default enp0s3)
		"iface":"enp0s3",
		//Amount of RAM, in mb, to assign to a service (default 1024)
		"ram":"1024",
		//Non-exclusive number of cpus to assign to a service (default 1)
		"cpus":"8",
		//Disk size, in mb, to be allocated to the root filesystem of a service (default 8096)
		"disksize":"8096",
		//Disk size, in mb, to be allocated to the data storage of a service (default 8096)
		"datadisksize":"8096",
		//URL of the debian ISO you wish to use to build VMs (>= 9.0 supported, default 9.1.0-amd64-netinst)
		"debianisourl":"cdimage.debian.org/debian-cd/current/amd64/iso-cd/debian-9.1.0-amd64-netinst.iso",
		//Known-good sha512 checksum of the above ISO (this comes from debian.org, default 9.1.0-amd64-netinst)
		"debianisosha512":"697600a110c7a5a1471fbf45c8030dd99b3c570db612044730f09b4624aa49f2a3d79469d55f1c18610c2414e9fffde1533b9a6fab6f3af4b5ba7c2d59003dc1",		
		//Debian mirror to be used for packages (default ftp.uk.debian.org)
		"debianmirror":"ftp.uk.debian.org",






		/****************************************************************************
		* Server definitions                                                        *
		****************************************************************************/
		"servers":{
			//Example of a router configured using _PPPoE_
			//This is probably your default setup for a router if you're using ADSL, VDSL or point-to-point tunnel
			"ppp_router":{
				//Declare it's a router
				"types":["router"],
				//Give it the subnet of 1
				"subnet":"1",
				//Tell it to use PPPoE for external connection
				"extconnection":"ppp",
				//Declare the PPPoE iface
				"extiface":"ppp0",
				//Physical external iface to negotiate ppp
				"pppiface":"enp2s0",
				//Physical iface to offer DHCP/DNS internally
				"bridge":"phy",
				//Physical internal iface
				"iface":"enp3s0"
			},
			//Example of a router configured using dhcp
			//This is probably only used if you're trying to create a network inside your main network (VMs, perhaps)
			//Also likely if you're connected using coax, Ethernet over the last mile, or LTE/3G/WiMax etc
			"dhcp_router":{
				//Declare it's a router
				"types":["router"],
				//Give it the subnet of 1
				"subnet":"1",
				//Tell it to use dhcp for external connection
				"extconnection":"dhcp",
				//Declare the external iface to listen to
				"extiface":"enp2s0",
				//Physical iface to offer DHCP/DNS internally
				"bridge":"phy",
				//Physical internal iface
				"iface":"enp3s0"
			},
			//Example of a router configured statically
			//You'll probably only use this for externally hosted networks
			"static_router":{
				//Declare it's a router
				"types":["router"],
				//Give it the subnet of 1
				"subnet":"1",
				//Tell it to use static external connection
				"extconnection":"static",
				//Declare the external iface to listen to
				"extiface":"enp2s0",
				//Static configuration array
				"extconfig":[ {"address":"123.321.123.321", "netmask":"255.255.255.224", "broadcast":"123.321.123.255", "gateway":"123.321.123.1"}
				]
				//Physical iface to offer DHCP/DNS internally
				"bridge":"phy",
				//Physical internal iface
				"iface":"enp3s0"
			},
			"hypervisor":{
				//Tell it to be a hypervisor machine
				"types":["metal"],
				//Give it a subnet
				"subnet":"2",
				//This is the router-facing iface
				"extiface":"enp3s0",
				//This is the internal-facing iface (for VMs)
				"iface":"enp3s0",
				//We want to use virtual bridging here to keep the traffic off the physical iface
				"bridge":"virt",
				//Mac address of the physical iface
				"mac":"de:ad:be:ef:ca:fe"
			},
			"nginx_lb":{
				//This is a service
				"types":["service"],
				"subnet":"5",
				//On our hypervisor
				"metal":"hypervisor",
				"mac":"de:ad:be:ef:ca:fe",
				//Which is a reverse proxy
				"profiles":["Webproxy"],
				//Proxying the following web services
				"proxy":[ "etherpad",
						   "owncloud"
						]
			},
			"owncloud":{
				"types":["service"],
				"subnet":"10",
				"metal":"hypervisor",
				"mac":"de:ad:be:ef:ca:fe",
				//Override the data storage for this machine
				"datadisksize":"61440",
				//As it's Owncloud
				"profiles":["Owncloud"],
				//Which has many names
				"cnames":["www.owncloud", "contacts", "calendar", "documents"]
			},
			"etherpad":{
				"types":["service"],
				"subnet":"22",
				"metal":"hypervisor",
				"mac":"de:ad:be:ef:ca:fe",
				"profiles":["Etherpad"],
				"cnames":["pads"]
			},
			"vpn":{
				"types":["service"],
				"subnet":"3",
				"metal":"hypervisor",
				"mac":"de:ad:be:ef:ca:fe",
				"profiles":["LibreSwan"],
				"cnames":["office"],
				//We want to DNAT external traffic coming into the hypervisor directly to this machine on its VPN UDP ports
				"externalip":"10.0.2.2"
			}
		},
		
		
		
		
		
		
		/****************************************************************************
		* Device definitions; superusers, users, internal and external-only devicen *
		****************************************************************************/
		"devices":{
			//For devices which represent an individual's device, use their email username so they can receive emails (domain comes from network-level settings)
			//Access to internal stuff (including ability to SSH) & interwebs
			"superuser": {"macs":["de:ad:be:ef:ca:fe", "de:ad:be:ef:ca:fe"], "type":"superuser"},
			//Access to internal stuff & interwebs
			"user_a": {"macs":["de:ad:be:ef:ca:fe", "de:ad:be:ef:ca:fe"], "type":"user"},
			"user_b": {"macs":["de:ad:be:ef:ca:fe", "de:ad:be:ef:ca:fe"], "type":"user"},
			//For other devices, just use a useful identifier as it'll never receive email
			//Internal connections only
			"printer": {"macs":["de:ad:be:ef:ca:fe"], "type":"intonly"},
			//Interwebs only
			"guest_wifi": {"macs":["de:ad:be:ef:ca:fe"], "type":"extonly"}
		}
	}
}

```
