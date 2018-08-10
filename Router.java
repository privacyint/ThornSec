package profile;

import java.util.Arrays;
import java.util.Vector;

import javax.json.JsonArray;
import javax.json.JsonObject;

import core.iface.IUnit;
import core.model.FirewallModel;
import core.model.InterfaceModel;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.FileOwnUnit;
import core.unit.fs.FilePermsUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;

public class Router extends AStructuredProfile {

	private DNS dns;
	private DHCP dhcp;
	private QoS qos;
	
	private FirewallModel firewall;
	private InterfaceModel interfaces;
	
	private Vector<String> userIfaces;
	
	private String invalidChars;
	
	private String internalIface;
	private String externalIface;
	private String netmask;
	private String domain;
	
	private Vector<String> internalOnlyDevices;
	private Vector<String> externalOnlyDevices;
	private Vector<String> peripheralDevices;
	private Vector<String> userDevices;
	
	public Router() {
		super("router");
		
		dns  = new DNS();
		dhcp = new DHCP();
		qos  = new QoS();
		
		userIfaces = new Vector<String>();
		userIfaces.addElement(":2+");
		
		invalidChars = "[^\\-a-zA-Z0-9]";
		
		internalOnlyDevices = new Vector<String>();
		externalOnlyDevices = new Vector<String>();
		peripheralDevices   = new Vector<String>();
		userDevices         = new Vector<String>();
	}

	public DHCP getDHCP() {
		return this.dhcp;
	}
	
	public DNS getDNS() {
		return this.dns;
	}
	
	public Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();

		externalIface = model.getData().getExtIface(server);
		internalIface = model.getData().getIface(server);

		firewall   = model.getServerModel(server).getFirewallModel();
		interfaces = model.getServerModel(server).getInterfaceModel();
		
		netmask = model.getData().getNetmask();
		domain  = model.getData().getDomain(server);
		
		//Let's just get this out of the way up here rather than repeating over and over
		//This class is difficult enough to follow already!! 
		for (String device : model.getDeviceLabels()) {
			switch (model.getDeviceModel(device).getType()) {
				case "User":
					userDevices.add(device);
					break;
				case "Internal":
					internalOnlyDevices.add(device);
					break;
				case "External":
					externalOnlyDevices.add(device);
					break;
				default:
					//In theory, we should never get here. Theory is a fine thing.
					System.out.println("Encountered an unsupported device type for " + device);
			}
		}
		
		//Also lump them together in one - we don't discriminate int/ext 99% of the time
		peripheralDevices.addAll(internalOnlyDevices);
		peripheralDevices.addAll(externalOnlyDevices);
		
		units.addAll(subnetConfigUnits(server, model));
		
		String sysctl = "";
		sysctl += "net.ipv4.ip_forward=1\n";
		sysctl += "net.ipv6.conf.all.disable_ipv6=1\n";
		sysctl += "net.ipv6.conf.default.disable_ipv6=1\n";
		sysctl += "net.ipv6.conf.lo.disable_ipv6=1";

		units.addElement(model.getServerModel(server).getConfigsModel().addConfigFile("sysctl", "proceed", sysctl, "/etc/sysctl.conf"));
		
		units.addAll(dhcp.getPersistentConfig(server, model));
		units.addAll(dns.getPersistentConfig(server, model));
		units.addAll(qos.getPersistentConfig(server, model));
		
		units.addAll(extConnConfigUnits(server, model));
		
		units.addAll(dailyBandwidthEmailDigestUnits(server, model));
		
		units.addAll(routerScript(server, model));
		
		return units;
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new InstalledUnit("xsltproc", "xsltproc"));
		units.addElement(new InstalledUnit("sendmail", "sendmail"));;
		
		model.getServerModel(server).getProcessModel().addProcess("sendmail: MTA: accepting connections$");
		model.getServerModel(server).getUserModel().addUsername("smmta");
		model.getServerModel(server).getUserModel().addUsername("smmpa");
		model.getServerModel(server).getUserModel().addUsername("smmsp");
		
		units.addAll(dns.getInstalled(server, model));
		units.addAll(dhcp.getInstalled(server, model));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		if (!model.getData().getVpnOnly()) {
			userIfaces.addElement(":1+");
		}
		
		units.addAll(dhcp.getPersistentFirewall(server, model));
		units.addAll(dns.getPersistentFirewall(server, model));
		units.addAll(qos.getPersistentFirewall(server, model));
		
		units.addAll(userIptUnits(server, model));
		units.addAll(intOnlyIptUnits(server, model));
		units.addAll(extOnlyIptUnits(server, model));
		units.addAll(serverIptUnits(server, model));

		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(dhcp.getLiveConfig(server, model));
		units.addAll(dns.getLiveConfig(server, model));
		
		return units;
	}
	
	private String buildUserDailyBandwidthEmail(String sender, String recipient, String subject, String username, boolean includeBlurb) {
		String email = "";
		email += "echo -e \\\"";
		
		email += "subject:" + subject + "\\n";
		email += "from:" + sender + "\\n";
		email += "recipients:" + recipient + "\\n";
		email += "\\n";
		email += "UL: \\`iptables -L " + username + "_egress -v -n | tail -n 2 | head -n 1 | awk '{ print \\$2 }'\\`\\n";
		email += "DL: \\`iptables -L " + username + "_ingress -v -n | tail -n 2 | head -n 1 | awk '{ print \\$2 }'\\`";
		
		if (includeBlurb) {
			email += "\\n";
			email += "\\n";
			email += "=====================================================================================";
			email += "\\n";
			email += "\\n";
			email += "As you know, one of the key advantages afforded by operating our network through Thornsec is that it monitors for uploading traffic. ";
			email += "The reason for this is that we want to try and check for any data exfiltration from our network.";
			email += "\\n";
			email += "\\n";
			email += "Part of this monitoring allows our router to give you a daily digest of how much you've downloaded and uploaded.";
			email += "\\n";
			email += "\\n";
			email += "This is not a punitive email, this information is not logged, and is only sent to you, as a user.  This will hopefully alert you to any strange activity ";
			email += "coming from your device.";
			email += "\\n";
			email += "\\n";
			email += "If you think there is something suspicious about the figures above, please forward this email to the Tech Team so they can look into it for you.";
			email += "\\n";
			email += "\\n";
			email += "Thanks!";
			email += "\\n";
			email += "Tech Team";
		}
		
		email += "\\\"";
		email += "|sendmail \"" + recipient + "\"\n\n";
		
		return email;
	}
	
	private Vector<IUnit> dailyBandwidthEmailDigestUnits(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();

		String script = "";
		script += "#!/bin/bash\n";
		
		//Iterate through users first; they need alerting individually
		for (String user : userDevices) {
			if (model.getData().getDeviceMacs(user).length > 0) { //But only if they're an internal user 
				//Email the user only
				script += "\n\n";
				script += buildUserDailyBandwidthEmail(model.getData().getAdminEmail(),
												user + "@" + model.getData().getDomain(server),
												"[" + user + "." + model.getData().getLabel() + "] Daily Bandwidth Digest",
												user,
												true);
				script += "iptables -Z " + user.replaceAll(invalidChars, "_") + "_ingress\n";
				script += "iptables -Z " + user.replaceAll(invalidChars, "_") + "_egress";
			}
		}

		script += "\n\n";

		script += "echo -e \\\"";
		script += "subject: [" + model.getData().getLabel() + "." + model.getData().getDomain(server) + "] Daily Bandwidth Digest\\n";
		script += "from:" + server + "@" + model.getData().getDomain(server) + "\\n";
		script += "recipients:" + model.getData().getAdminEmail() + "\\n";

		//Iterate through everything which should be reported back to admins.
		//This used to be an individual email per device/server, but this is useless as it just spams the admins
		for (String peripheral : peripheralDevices) {
			script += "\\n\\n";
			script += "Digest for " + peripheral + ":\\n";
			script += "UL: \\`iptables -L " + peripheral + "_egress -v -n | tail -n 2 | head -n 1 | awk '{ print \\$2 }'\\`\\n";
			script += "DL: \\`iptables -L " + peripheral + "_ingress -v -n | tail -n 2 | head -n 1 | awk '{ print \\$2 }'\\`";
		}

		//Then servers
		for (String srv : model.getServerLabels()) {
			script += "\\n\\n";
			script += "Digest for " + srv + ":\\n";
			script += "UL: \\`iptables -L " + srv + "_egress -v -n | tail -n 2 | head -n 1 | awk '{ print \\$2 }'\\`\\n";
			script += "DL: \\`iptables -L " + srv + "_ingress -v -n | tail -n 2 | head -n 1 | awk '{ print \\$2 }'\\`";
		}

		script += "\\\"";
		script += "|sendmail \"" + model.getData().getAdminEmail() + "\"\n";
		
		for (String peripheral : peripheralDevices) {
			script += "\niptables -Z " + peripheral.replaceAll(invalidChars, "_") + "_ingress";
			script += "\niptables -Z " + peripheral.replaceAll(invalidChars, "_") + "_egress";
		}

		for (String srv : model.getServerLabels()) {
			script += "\niptables -Z " + srv.replaceAll(invalidChars, "_") + "_ingress";
			script += "\niptables -Z " + srv.replaceAll(invalidChars, "_") + "_egress";
		}
		
		units.addElement(new FileUnit("daily_bandwidth_alert_script_created", "proceed", script, "/etc/cron.daily/bandwidth", "I couldn't create the bandwidth digest script.  This means you and your users won't receive daily updates on bandwidth use"));
		units.addElement(new FilePermsUnit("daily_bandwidth_alert_script", "daily_bandwidth_alert_script_created", "/etc/cron.daily/bandwidth", "750", "I couldn't set the bandwidth digest script to be executable.  This means you and your users won't receive daily updates on bandwidth use"));
		
		return units;
	}

	private Vector<IUnit> subnetConfigUnits(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		for (String srv : model.getServerLabels()) {
			String[] cnames  = model.getData().getCnames(srv);
			
			String ip       = model.getServerModel(srv).getIP();
			String gateway  = model.getServerModel(srv).getGateway();
			String domain   = model.getData().getDomain(srv);
			String hostname = srv.replaceAll(invalidChars, "_");

			String[] subdomains = new String[cnames.length + 1];
			System.arraycopy(new String[] {model.getData().getHostname(srv)},0,subdomains,0, 1);
			System.arraycopy(cnames,0,subdomains,1, cnames.length);

			if (!model.getServerModel(server).isMetal()) {
				String bridge = model.getData().getProperty(server, "bridge");

				//If we're bridging to an actual iface, we need to declare it
				if (bridge != null) {
					units.addElement(this.interfaces.addIface(hostname + "_bridge",
						"manual", bridge, null, null, null, null, null));
				}
				
				units.addElement(this.interfaces.addIface(hostname + "_router_iface",
										"static",
										this.internalIface + ((!srv.equals(server)) ? ":0" + model.getData().getSubnet(srv) : ""),
										(srv.equals(server)) ? bridge : null,
										gateway,
										this.netmask,
										null,
										null));
			}

			this.dns.addDomainRecord(domain, gateway, subdomains, ip);
		}

		for (String device : model.getDeviceLabels()) {
			String[] gateways = model.getDeviceModel(device).getGateways();
			String[] ips      = model.getDeviceModel(device).getIPs();
			
			String subnet = model.getDeviceModel(device).get3rdOctet();
			
			for (int i = 0; i < gateways.length; ++i) {
				String subdomain = device.replaceAll(invalidChars, "-") + "." + model.getLabel() + ".lan." + i;
				
				units.addElement(this.interfaces.addIface(device.replaceAll(invalidChars + "-", "_") + "_router_iface_" + i,
										"static",
										this.internalIface + ":1" + subnet + i,
										null,
										gateways[i],
										this.netmask,
										null,
										null));
				
				this.dns.addDomainRecord(this.domain, gateways[i], new String[] {subdomain}, ips[i]);
			}
		}

		units.addElement(new SimpleUnit("ifaces_up", "proceed",
				"sudo service networking restart",
				"sudo ip addr | grep " + this.internalIface, "", "fail",
				"Couldn't bring your network interfaces up.  This can potentially be resolved by a restart (assuming you've had no other network-related errors)."));

		return units;
	}
	
	private Vector<IUnit> baseIptConfig(String server, NetworkModel model, String name, String subnet) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String cleanName    = name.replace(invalidChars, "_");
		String fwdChain     = cleanName + "_fwd";
		String ingressChain = cleanName + "_ingress";
		String egressChain  = cleanName + "_egress";
		
		//Create our egress chain for bandwidth (exfil?) tracking
		//In future, we could perhaps do some form of traffic blocking malarky here?
		this.firewall.addChain(cleanName + "_egress_chain", "filter", egressChain);
		//Create our ingress chain for download bandwidth tracking
		this.firewall.addChain(cleanName + "_ingress_chain", "filter", ingressChain);
		//Create our forward chain for all other rules
		this.firewall.addChain(cleanName + "_fwd_chain", "filter", fwdChain);

		//Force traffic to/from a given subnet to jump to our chains
		this.firewall.addFilterForward(cleanName + "_ipt_server_src",
				"-s " + subnet
				+ " -j "+ fwdChain);
		this.firewall.addFilterForward(cleanName + "_ipt_server_dst",
				"-d " + subnet
				+ " -j " + fwdChain);

		//We want to default drop anything not explicitly whitelisted
		//Make sure that these are the very first rules as the chain may have been pre-populated
		this.firewall.addFilter(cleanName + "_fwd_default_drop", fwdChain, 0,
				"-j DROP");
		
		//Don't allow any traffic in from the outside world
		this.firewall.addFilter(cleanName + "_ingress_default_drop", ingressChain, 0,
				"-j DROP");

		//Don't allow any traffic out to the outside world
		this.firewall.addFilter(cleanName + "_egress_default_drop", egressChain, 0,
				"-j DROP");
		
		//Add our forward chain rules (backwards(!))
		//Allow our router to talk to us
		this.firewall.addFilter(cleanName + "_allow_router_traffic", fwdChain,
				"-s " + subnet
				+ " -j ACCEPT");

		//Masquerade on the external iface
		this.firewall.addNatPostrouting(cleanName + "_masquerade_external",
				"-o " + externalIface
				+ " -j MASQUERADE");
		
		return units;
	}
	
	private Vector<IUnit> serverIptUnits(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		for (String srv : model.getServerLabels()) {
			String serverSubnet    = model.getServerModel(srv).getSubnet() + "/30";
			String cleanServerName = srv.replaceAll(invalidChars, "_");
			String fwdChain        = cleanServerName + "_fwd";
			String egressChain     = cleanServerName + "_egress";
			String ingressChain    = cleanServerName + "_ingress";
			
			baseIptConfig(server, model, srv, serverSubnet);
			
			if (model.getServerModel(srv).isRouter()) {
				this.firewall.addFilter(cleanServerName + "_allow_email_out", egressChain,
						"-p tcp"
						+ " --dport 25"
						+ " -j ACCEPT");
			}

			//Allow admins to SSH to their server(s)
			for (String admin : model.getData().getAdmins(srv)) {
				//Not an "internal" user
				if (model.getDeviceModel(admin).getSubnets().length == 0) { continue; }
				
				this.firewall.addFilter(cleanServerName + "_ssh_" + admin.replaceAll(invalidChars,  "_"), fwdChain,
						"-s " + model.getDeviceModel(admin).getSubnets()[0] + "/24"
						+ " -d " + serverSubnet
						+ " -p tcp"
						+ " --dport " + model.getData().getSSHPort(srv)
						+ " -j ACCEPT");
			}
			
			//Only actually do this if we have any users!
			if (userDevices.size() > 0) {
				int actualUsers = 0;
				String userRule = "";
				userRule += "-s ";
				for (String user : userDevices) {
					if (model.getDeviceModel(user).getSubnets().length == 0) { continue; }
					++actualUsers;
					userRule += model.getDeviceModel(user).getSubnets()[0] + "/24,";
				}
				userRule = userRule.replaceAll(",$", ""); //Remove any trailing comma
				userRule += " -d " + serverSubnet;
				userRule += " -p tcp";
				userRule += " -m state --state NEW";
				userRule += " -m tcp -m multiport --dports 80,443";
				userRule += " -j ACCEPT";
				
				if (actualUsers > 0) {
					this.firewall.addFilter("allow_users_80_443_" + server, fwdChain, userRule);
				}
			}
			
			//And servers can talk back, if established/related traffic
			this.firewall.addFilter(cleanServerName + "_allow_related_traffic", fwdChain,
					"-p tcp"
					+ " -m state --state ESTABLISHED,RELATED"
					+ " -j ACCEPT");
	
			//Jump to the ingress/egress chains
			this.firewall.addFilter(cleanServerName + "_jump_ingress", fwdChain,
					"-i " + this.externalIface
					+ " -j " + ingressChain);
			this.firewall.addFilter(cleanServerName + "_jump_egress", fwdChain,
					"-o " + this.externalIface
					+ " -j " + egressChain);
			//Log anything hopping to our egress chain
			this.firewall.addFilter(cleanServerName + "_log_egress_traffic", egressChain,
					"-j LOG --log-prefix \\\"ipt-" + cleanServerName + ": \\\"");
			//Allow related ingress traffic
			this.firewall.addFilter(cleanServerName + "_allow_related_ingress_traffic", ingressChain,
					"-p tcp"
					+ " -m state --state ESTABLISHED,RELATED"
					+ " -j ACCEPT");
		}
	
		return units;
	}
	
	private Vector<IUnit> userIptUnits(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		for (String user : userDevices) {
			//Not an "internal" user
			if (model.getDeviceModel(user).getSubnets().length == 0) { continue; }
			
			String cleanUserName = user.replace(invalidChars, "_");
			String fwdChain      = cleanUserName + "_fwd";
			String ingressChain  = cleanUserName + "_ingress";
			String egressChain   = cleanUserName + "_egress";
			
			String userSubnet = model.getDeviceModel(user).getSubnets()[0] + "/24";
			
			baseIptConfig(server, model, user, userSubnet);

			for (String iface : userIfaces) {
				//First make sure we actually *have* servers (thanks, Chris...)
				if (model.getServerLabels().length == 0) { continue; }
				
				//They can talk to our servers on :80 && :443
				String serverRule = "";
				serverRule += "-i " + this.internalIface + iface;
				serverRule += " -d ";

				for (String srv : model.getServerLabels()) {
					serverRule += model.getServerModel(srv).getSubnet() + "/30,";
				}
				serverRule = serverRule.replaceAll(",$", ""); //Remove any trailing comma
				
				serverRule += " -p tcp";
				serverRule += " -m state --state NEW";
				serverRule += " -m tcp -m multiport --dports 80,443";
				serverRule += " -j ACCEPT";
				
				this.firewall.addFilter("allow_users_80_443_" + server, fwdChain, serverRule);
			}
			
			//Allow superusers to SSH into their servers
			for (String srv : model.getServerLabels()) {
				if (Arrays.asList(model.getData().getAdmins(srv)).contains(user)) {
					for (String iface : userIfaces) {
						this.firewall.addFilter(cleanUserName + "_allow_ssh_traffic_" + srv.replaceAll("-",  "_"), fwdChain,
								"-i " + this.internalIface + iface
								+ " -d " + model.getServerModel(srv).getSubnet() + "/30"
								+ " -p tcp"
								+ " --dport " + model.getData().getSSHPort(srv)
								+ " -j ACCEPT");
					}
				}
			}
			
			//Does this user have ports which should be allowed internally?
			//Configure, if so!
			if (model.getDeviceModel(user).getPorts().length > 0) {
				String[] ports = model.getDeviceModel(user).getPorts();
				
				//Firstly, allow all other users to talk to it on given ports
				for (String iface : userIfaces) {
					String otherUsersRule = "";
					otherUsersRule += "-i " + this.internalIface + iface;
					otherUsersRule += " -p tcp";
					otherUsersRule += " -s ";
					
					for (String u : userDevices) {
						otherUsersRule += model.getDeviceModel(u).getSubnets()[0] + "/30,";
					}
					
					otherUsersRule = otherUsersRule.replaceAll(",$", ""); // Remove any trailing comma
					otherUsersRule += " -m multiport";
					otherUsersRule += " --dports ";
					
					for (int i = 0; i < ports.length; ++i) {
						otherUsersRule += ports[i] + ",";
					}
					
					otherUsersRule = otherUsersRule.replaceAll(",$", ""); // Remove any trailing comma
					otherUsersRule += " -j ACCEPT";
					
					this.firewall.addFilter("allow_user_ports_" + user, fwdChain, otherUsersRule);
				}
			}
			
			//Allow the user currently being configured to talk with other users on specified ports
			for (String iface : userIfaces) {
				for (String u : userDevices) {
					if (model.getDeviceModel(u).getPorts().length > 0) {
						String[] ports = model.getDeviceModel(u).getPorts();
						
						String myRule = "";
						myRule += "-i " + this.internalIface + iface;
						myRule += " -p tcp";
						myRule += " -d " + model.getDeviceModel(u).getSubnets()[0] + "/24";
						myRule += " -m state --state NEW";
						myRule += " -m tcp -m multiport --dports ";

						for (int i = 0; i < ports.length; ++i) {
							myRule += ports[i] + ",";
						}

						myRule = myRule.replaceAll(",$", ""); // Remove any trailing comma
						
						myRule += " -j ACCEPT";
						
						this.firewall.addFilter(cleanUserName + "_allow_talk_to_" + u.replaceAll("-",  "_"), fwdChain, myRule);
					}
				}
			}
			
			//Configure what they can do with internal only devices
			for (String iface : userIfaces) {
				
				if (internalOnlyDevices.size() == 0) { continue; }
				
				String intOnlyRule = "";
				intOnlyRule += "-i " + this.internalIface + iface;
				intOnlyRule += " -d ";

				for (String device : internalOnlyDevices) {
					intOnlyRule += model.getDeviceModel(device).getSubnets()[0] + "/30,";
				}
				
				intOnlyRule = intOnlyRule.replaceAll(",$", ""); //Remove any trailing comma
				intOnlyRule += " -p tcp";
				intOnlyRule += " -m state --state NEW";
				intOnlyRule += " -j ACCEPT";
				this.firewall.addFilter("allow_int_only_" + server, fwdChain, intOnlyRule);
			}

			//Allow them to manage certain ext only devicen, if they're a superuser
			if (Arrays.asList(model.getData().getAdmins()).contains(user)) {
				for (String device : externalOnlyDevices) {
					//But only if they're managed.
					if (!model.getDeviceModel(device).isManaged()) {
						continue;
					}
					
					for (String iface : userIfaces) {
						this.firewall.addFilter(cleanUserName + "_allow_managed_traffic_nonvpn_" + device.replaceAll("-",  "_"), fwdChain,
								"-i " + this.internalIface + iface
								+ " -d " + model.getDeviceModel(device).getSubnets()[0] + "/24"
								+ " -p tcp"
								+ " -m state --state NEW"
								+ " -m tcp -m multiport --dports 80,443,22"
								+ " -j ACCEPT");
					}
				}
			}
			
			//Users can talk to the outside world
			this.firewall.addFilter(cleanUserName + "_allow_egress_traffic", egressChain,
					"-o " + this.externalIface
					+ " -j ACCEPT");
			//And can accept established/related traffic from the outside world, too
			this.firewall.addFilter(cleanUserName + "_allow_ingress_traffic", ingressChain,
					"-i " + this.externalIface
					+ " -m state --state ESTABLISHED,RELATED"
					+ " -j ACCEPT");

			this.firewall.addFilter(cleanUserName + "_allow_related_traffic", fwdChain,
					"-p tcp"
					+ " -m state --state ESTABLISHED,RELATED"
					+ " -j ACCEPT");
			
			//Jump to the ingress/egress chains
			this.firewall.addFilter(cleanUserName + "_allow_ingress", fwdChain,
					"-i " + this.externalIface
					+ " -j " + ingressChain);
			this.firewall.addFilter(cleanUserName + "_allow_egress", fwdChain,
					"-o " + this.externalIface
					+ " -j " + egressChain);
			
			//Log any NEW connections on our egress chain
			this.firewall.addFilter(cleanUserName + "_log_new_egress_traffic", egressChain,
					"-m state --state NEW"
					+ " -j LOG --log-prefix \\\"ipt-" + cleanUserName + "-[new]: \\\"");
			//Log any DESTROYED conections on our egress chain
			this.firewall.addFilter(cleanUserName + "_log_destroyed_fin_egress_traffic", egressChain,
					"-p tcp --tcp-flags FIN FIN"
					+ " -j LOG --log-prefix \\\"ipt-" + cleanUserName + "-[fin]: \\\"");
			this.firewall.addFilter(cleanUserName + "_log_destroyed_fin_egress_traffic", egressChain,
					"-p tcp --tcp-flags RST RST"
					+ " -j LOG --log-prefix \\\"ipt-" + cleanUserName + "-[rst]: \\\"");
		}
		
		return units;
	}
	
	private Vector<IUnit> intOnlyIptUnits(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		for (String device : internalOnlyDevices) {
			String cleanDeviceName = device.replaceAll(invalidChars, "_");
			String fwdChain        = cleanDeviceName + "_fwd";
			String ingressChain    = cleanDeviceName + "_ingress";
			String egressChain     = cleanDeviceName + "_egress";
			
			String deviceSubnet = model.getDeviceModel(device).getSubnets()[0] + "/24";
			
			baseIptConfig(server, model, device, deviceSubnet);

			//Jump to the ingress/egress chains
			this.firewall.addFilter(cleanDeviceName + "_allow_ingress", fwdChain,
					"-i " + this.externalIface
					+ " -j " + ingressChain);
			this.firewall.addFilter(cleanDeviceName + "_allow_egress", fwdChain,
					"-o " + this.externalIface
					+ " -j " + egressChain);
			//Log anything hopping to our egress chain
			this.firewall.addFilter(cleanDeviceName + "_log_egress_traffic", egressChain,
					"-j LOG --log-prefix \\\"ipt-" + cleanDeviceName + ": \\\"");
			
			//Users can talk to our internal only devices
			//Configure what they can do with internal only devices
			String intOnlyRule = "";
			intOnlyRule += "-d " + deviceSubnet;
			intOnlyRule += " -s ";

			for (String user : userDevices) {
				intOnlyRule += model.getDeviceModel(user).getSubnets()[0] + "/24,";
			}
			
			intOnlyRule = intOnlyRule.replaceAll(",$", ""); //Remove any trailing comma
			intOnlyRule += " -p tcp";
			intOnlyRule += " -m state --state NEW";
			intOnlyRule += " -j ACCEPT";
			this.firewall.addFilter("allow_users_" + device, fwdChain, intOnlyRule);
			
			this.firewall.addFilter(cleanDeviceName + "_allow_related_traffic", fwdChain,
					"-p tcp"
					+ " -m state --state ESTABLISHED,RELATED"
					+ " -j ACCEPT");
		}
		
		return units;
	}
	
	private Vector<IUnit> extOnlyIptUnits(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		for (String device : externalOnlyDevices) {
			String cleanDeviceName = device.replace(invalidChars, "_");
			String fwdChain     = cleanDeviceName + "_fwd";
			String ingressChain = cleanDeviceName + "_ingress";
			String egressChain  = cleanDeviceName + "_egress";
			
			String deviceSubnet = model.getDeviceModel(device).getSubnets()[0] + "/24";
			
			baseIptConfig(server, model, device, deviceSubnet);

			//External only devices can talk to the outside world
			this.firewall.addFilter(cleanDeviceName + "_allow_egress_traffic", egressChain,
					"-o " + this.externalIface
					+ " -j ACCEPT");
			//And can accept established/related traffic from the outside world, too
			this.firewall.addFilter(cleanDeviceName + "_allow_ingress_traffic", ingressChain,
					"-i " + this.externalIface
					+ " -m state --state ESTABLISHED,RELATED"
					+ " -j ACCEPT");
			
			if (model.getDeviceModel(device).isManaged()) {
				for (String user : userDevices) {
					//Allow them to manage certain ext only devicen, if they're a superuser
					if (Arrays.asList(model.getData().getAdmins()).contains(user)) {
						//And can accept established/related traffic from the outside world, too
						this.firewall.addFilter(cleanDeviceName + "_allow_management_traffic", fwdChain,
								"-s " + model.getDeviceModel(user).getSubnets()[0] + "/24"
								+ " -j ACCEPT");
					}
				}
			}
			
			//Jump to the ingress/egress chains
			this.firewall.addFilter(cleanDeviceName + "_allow_ingress", fwdChain,
					"-i " + this.externalIface
					+ " -j " + ingressChain);
			this.firewall.addFilter(cleanDeviceName + "_allow_egress", fwdChain,
					"-o " + this.externalIface
					+ " -j " + egressChain);
		}
		
		return units;
	}
	
	private Vector<IUnit> extConnConfigUnits(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();

		if (model.getData().getExtConn(server).contains("ppp")) {
			units.addElement(new InstalledUnit("ext_ppp", "ppp"));
			units.addElement(this.interfaces.addPPPIface("router_ext_ppp_iface", model.getData().getProperty(server, "pppiface")));
			model.getServerModel(server).getProcessModel().addProcess("/usr/sbin/pppd call provider$");
			
			model.getServerModel(server).getConfigsModel().addConfigFilePath("/etc/ppp/peers/dsl-provider$");
			model.getServerModel(server).getConfigsModel().addConfigFilePath("/etc/ppp/options$");
			
			units.addElement(new FileUnit("resolv_conf", "proceed", "nameserver 127.0.0.1", "/etc/ppp/resolv.conf"));
			
			units.addElement(this.firewall.addMangleForward("clamp_mss_to_pmtu",
					"-p tcp --tcp-flags SYN,RST SYN -m tcpmss --mss 1400:1536 -j TCPMSS --clamp-mss-to-pmtu"));
		}
		else if (model.getData().getExtConn(server).equals("dhcp")){
			units.addElement(this.interfaces.addIface("router_ext_dhcp_iface", 
														"dhcp",
														this.externalIface,
														null,
														null,
														null,
														null,
														null));

			String dhclient = "option rfc3442-classless-static-routes code 121 = array of unsigned integer 8;\n";
			dhclient += "send host-name = gethostname();\n";
			dhclient += "supersede domain-search \\\"" + this.domain + "\\\";\n";
			dhclient += "supersede domain-name-servers " + model.getServerModel(server).getGateway() + ";\n";
			dhclient += "request subnet-mask, broadcast-address, time-offset, routers,\n";
			dhclient += "	domain-name, domain-name-servers, domain-search, host-name,\n";
			dhclient += "	dhcp6.name-servers, dhcp6.domain-search,\n";
			dhclient += "	netbios-name-servers, netbios-scope, interface-mtu,\n";
			dhclient += "	rfc3442-classless-static-routes, ntp-servers;";
			units.addElement(new FileUnit("router_ext_dhcp_persist", "proceed", dhclient, "/etc/dhcp/dhclient.conf"));

			units.addElement(this.firewall.addFilterInput("router_ext_dhcp_in",
					"-i " + this.externalIface
					+ " -d 255.255.255.255"
					+ " -p udp"
					+ " --dport 68"
					+ " --sport 67"
					+ " -j ACCEPT"));
			units.addElement(this.firewall.addFilterOutput("router_ext_dhcp_ipt_out", 
					"-o " + this.externalIface
					+ " -p udp"
					+ " --dport 67"
					+ " --sport 68"
					+ " -j ACCEPT"));
		}
		else if(model.getData().getExtConn(server).equals("static")) {
			JsonArray interfaces = (JsonArray) model.getData().getPropertyObjectArray(server, "extconfig");
			
			for (int i = 0; i < interfaces.size(); ++i) {
				JsonObject row = interfaces.getJsonObject(i);
				
				String address   = row.getString("address");
				String netmask   = row.getString("netmask");
				String gateway   = row.getString("gateway", null);
				String broadcast = row.getString("broadcast", null);
				String iface     = this.externalIface;
				
				if (i > 0) {
					iface += ":" + i;
				}
				
				units.addElement(this.interfaces.addIface("router_ext_static_iface_" + i,
											"static",
											iface,
											null,
											address,
											netmask,
											broadcast,
											gateway));
			}
		}
		
		return units;
	}
	
	protected Vector<IUnit> routerScript(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String admin = "";
		admin += "#!/bin/bash\n";
		admin += "\n";
		admin += "RED='\\\\033[0;31m'\n"; 
		admin += "GREEN='\\\\033[0;32m'\n"; 
		admin += "NC='\\\\033[0m'\n";
		admin += "\n";
		admin += "function checkInternets {\n"; 
		admin += "        clear\n";
		admin += "\n";
		admin += "        echo \\\"Checking your internet connectivity, please wait...\\\"\n"; 
		admin += "        echo \n";
		admin += "        echo \\\"1/3 (8.8.8.8 - Google DNS)     : \\$(ping -q -w 1 -c 1 8.8.8.8 &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n";
		admin += "        echo \\\"2/3 (208.67.222.222 - OpenDNS) : \\$(ping -q -w 1 -c 1 208.67.222.222 &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n";
		admin += "        echo \\\"3/3 (1.1.1.1 - Cloudflare DNS) : \\$(ping -q -w 1 -c 1 1.1.1.1 &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n";
		admin += "        echo \n";
		admin += "        read -n 1 -s -r -p \\\"Press any key to return to the main menu...\\\"\n";
		admin += "}\n";
		admin += "\n";
		admin += "function checkDNS {\n";
		admin += "        clear\n";
		admin += "\n";
		admin += "        echo \\\"Checking your external DNS server is resolving correctly\\\"\n";
		admin += "        echo \n";
		admin += "        echo \\\"Getting the DNS record for Google.com : \\$( dig +short google.com. &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n";
		admin += "        echo \n";
		admin += "        read -n 1 -s -r -p \\\"Press any key to return to the main menu...\\\"\n";
		admin += "}\n";
		admin += "\n";
		admin += "function restartUnbound {\n";
		admin += "        clear\n";
		admin += "\n";
		admin += "        echo \\\"Restarting the DNS Server - please wait...\\\"\n";
		admin += "        echo \n";
		admin += "        echo \\\"Stopping DNS Server : \\$(service unbound stop &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n";
		admin += "        echo \\\"Starting DNS Server : \\$(service unbound start &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n";
		admin += "        echo \n";
		admin += "        read -n 1 -s -r -p \\\"Press any key to return to the main menu...\\\"\n";
		admin += "}\n";
		admin += "\n";
		admin += "function restartDHCP {\n";
		admin += "        clear\n";
		admin += "\n";
		admin += "        echo \\\"Restarting the DHCP Server - please wait...\\\"\n";
		admin += "        echo \n";
		admin += "        echo \\\"Stopping DHCP Server : \\$(service isc-dhcp-server stop &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n";
		admin += "        echo \\\"Starting DHCP Server : \\$(service isc-dhcp-server start &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n";
		admin += "        echo \n";
		admin += "        read -n 1 -s -r -p \\\"Press any key to return to the main menu...\\\"\n"; 
		admin += "}\n";
		admin += "\n";
		if (model.getData().getExtConn(server).contains("ppp")) {
			admin += "function restartPPPoE {\n";
			admin += "        clear\n";
			admin += "\n";
			admin += "        echo \\\"Restarting the PPPoE Client - please wait...\\\"\n";
			admin += "        echo \n";
			admin += "        echo \\\"Stopping PPPoE Client : \\$(poff &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n";
			admin += "        echo \\\"Starting PPPoE Client : \\$(pon &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n";
			admin += "        echo \n";
			admin += "        sleep 2\n";
			admin += "        checkInternets\n";
			admin += "}\n";
			admin += "\n";
		}
		admin += "function reloadIPT {\n";
		admin += "        clear\n";
		admin += "\n";
		admin += "        echo \\\"Reloading the firewall - please wait...\\\"\n";
		admin += "        echo \n";
		admin += "        echo \\\"Flushing firewall rules  : \\$(iptables -F &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n";
		admin += "        echo \\\"Reloading firewall rules : \\$(iptables-restore < /etc/iptables/iptables.conf &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n";
		admin += "        echo \n";
		admin += "        read -n 1 -s -r -p \\\"Press any key to return to the main menu...\\\"\n";
		admin += "}\n";
		admin += "\n";
		admin += "function tracert {\n";
		admin += "        clear\n";
		admin += "\n";
		admin += "        echo \\\"Conducting a traceroute between the router and Google.com - please wait...\\\"\n";
		admin += "        echo \n";
		admin += "        traceroute google.com\n";
		admin += "        echo \n";
		admin += "        read -n 1 -s -r -p \\\"Press any key to return to the main menu...\\\"\n";
		admin += "}\n";
		admin += "\n";
		if (model.getData().getExtConn(server).contains("ppp")) {
			admin += "function configurePPPoE {\n";
			admin += "        correct=\\\"false\\\"\n";
			admin += "        \n";
			admin += "        while [ \\\"\\${correct}\\\" = \\\"false\\\" ]; do\n";
			admin += "            clear\n";
			admin += "            \n";
			admin += "            echo \\\"Enter your ISP's login username and press [ENTER]\\\"\n";
			admin += "            read -r username\n";
			admin += "            echo \\\"Enter your ISP's login password and press [ENTER]\\\"\n";
			admin += "            read -r password\n";
			admin += "            \n";
			admin += "            clear\n";
			admin += "            \n";
			admin += "            echo -e \\\"Username: \\${GREEN}\\${username}\\${NC}\\\"\n";
			admin += "            echo -e \\\"Password: \\${GREEN\\}${password}\\${NC}\\\"\n";
			admin += "            \n";
			admin += "            read -r -p \\\"Are the above credentials correct? [Y/n]\\\" yn\n";
			admin += "            \n";
			admin += "            case \\\"\\${yn}\\\" in\n";
			admin += "                [nN]* ) correct=\\\"false\\\";;\n";
			admin += "                    * ) correct=\\\"true\\\";\n";
			admin += "                        printf \\\"\\\"%s\\\"      *      \\\"%s\\\"\\\" \\\"\\${username}\\\" \\\"\\${password}\\\" > /etc/ppp/chap-secrets;;\n";
			admin += "			esac\n";
			admin += "		done\n";
			admin += "      \n";
			admin += "      read -n 1 -s -r -p \\\"Press any key to return to the main menu...\\\"\n";
			admin += "\n";
			admin += "}\n";
			admin += "\n";
		}
		admin += "if [ \\\"\\${EUID}\\\" -ne 0 ]\n";
		admin += "    then echo -e \\\"\\${RED}This script requires running as root.  Please sudo and try again.\\${NC}\\\"\n";
		admin += "    exit\n";
		admin += "fi\n";
		admin += "\n";
		admin += "while true; do\n";
		admin += "        clear\n";
		admin += "        echo \\\"Choose an option:\\\"\n";
		admin += "        echo \\\"1) Check Internet Connectivity\\\"\n";
		admin += "        echo \\\"2) Check External DNS\\\"\n";
		admin += "        echo \\\"3) Restart Internal DNS Server\\\"\n";
		admin += "        echo \\\"4) Restart Internal DHCP Server\\\"\n";
		admin += "        echo \\\"5) Flush & Reload Firewall\\\"\n";
		admin += "        echo \\\"6) Traceroute\\\"\n";
		if (model.getData().getExtConn(server).contains("ppp")) {
			admin += "        echo \\\"7) Restart PPPoE (Internet) Connection\\\"\n";
			admin += "        echo \\\"C) Configure PPPoE credentials\\\"\n";
		}
		admin += "        echo \\\"R) Reboot Router\\\"\n";
		admin += "        echo \\\"Q) Quit\\\"\n";
		admin += "        read -r -p \\\"Select your option: \\\" opt\n";
		admin += "        case \\\"\\${opt}\\\" in\n";
		admin += "                1   ) checkInternets;;\n";
		admin += "                2   ) checkDNS;;\n";
		admin += "                3   ) restartUnbound;;\n";
		admin += "                4   ) restartDHCP;;\n";
		admin += "                5   ) reloadIPT;;\n";
		admin += "                6   ) tracert;;\n";
		if (model.getData().getExtConn(server).contains("ppp")) {
			admin += "                7   ) restartPPPoE;;\n";
			admin += "                c|C ) configurePPPoE;;\n";
		}
		admin += "                r|R ) reboot;;\n";
		admin += "                q|Q ) exit;;\n";
		admin += "        esac\n";
		admin += "done";

		units.addElement(new FileUnit("router_admin", "proceed", admin, "/root/routerAdmin.sh"));
		units.addElement(new FileOwnUnit("router_admin", "router_admin", "/root/routerAdmin.sh", "root"));
		units.addElement(new FilePermsUnit("router_admin", "router_admin_chowned", "/root/routerAdmin.sh", "500"));
		
		return units;
	}
}
