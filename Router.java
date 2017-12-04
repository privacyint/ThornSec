package profile;

import java.util.Vector;

import javax.json.JsonArray;
import javax.json.JsonObject;

import core.iface.IUnit;
import core.model.FirewallModel;
import core.model.InterfaceModel;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.FileAppendUnit;
import core.unit.fs.FilePermsUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;

public class Router extends AStructuredProfile {

	private DNS dns;
	private DHCP dhcp;
	private QoS qos;
	private Vector<String> userIfaces;
	
	public Router() {
		super("router");
		
		dns = new DNS();
		dhcp = new DHCP();
		qos = new QoS();
		
		userIfaces = new Vector<String>();
		userIfaces.addElement(":2+");
	}

	public DHCP getDHCP() {
		return this.dhcp;
	}
	
	public DNS getDNS() {
		return this.dns;
	}
	
	public Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(subnetConfigUnits(server, model));
		
		units.addElement(new FileAppendUnit("router_fwd", "proceed", "net.ipv4.ip_forward=1", "/etc/sysctl.conf",
				"Couldn't set IPv4 forwarding to on.  This will mean that your router won't work.  Sorry about that."));
		units.addElement(new FileAppendUnit("router_disable_all_ipv6", "proceed", "net.ipv6.conf.all.disable_ipv6=1", "/etc/sysctl.conf",
				"Couldn't disable IPv6 on your router.  This isn't great, but will probably not cause too many dramas."));
		units.addElement(new FileAppendUnit("router_disable_default_ipv6", "proceed", "net.ipv6.conf.default.disable_ipv6=1", "/etc/sysctl.conf",
				"Couldn't disable IPv6 on your router.  This isn't great, but will probably not cause too many dramas."));
		units.addElement(new FileAppendUnit("router_disable_lo_ipv6", "proceed", "net.ipv6.conf.lo.disable_ipv6=1", "/etc/sysctl.conf",
				"Couldn't disable IPv6 on your router.  This isn't great, but will probably not cause too many dramas."));

		units.addAll(dhcp.getPersistentConfig(server, model));
		units.addAll(dns.getPersistentConfig(server, model));
		units.addAll(qos.getPersistentConfig(server, model));
		
		units.addAll(extConnConfigUnits(server, model));
		
		units.addAll(dailyBandwidthEmailDigestUnits(server, model));
		
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
	
	private String buildDailyBandwidthEmail(String sender, String recipient, String subject, String username, boolean includeBlurb) {
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
		
		//Iterate through devicen first
		for (String device : model.getDeviceLabels()) {
			switch (model.getDeviceModel(device).getType()) {
				//Email the user only
				case "user":
				case "superuser":
					script += "\n\n";
					script += buildDailyBandwidthEmail(model.getData().getAdminEmail(),
													device + "@" + model.getData().getDomain(server),
													"[" + device + "." + model.getData().getLabel() + "] Daily Bandwidth Digest",
													device,
													true);
					break;
				//This is a peripheral of some sort.  Just let the responsible person know.
				case "intonly":
				case "extonly":
					script += "\n\n";
					script += buildDailyBandwidthEmail(device + "@" + model.getData().getDomain(server),
							model.getData().getAdminEmail(),
							"[" + device + "." + model.getLabel() + "] Daily Bandwidth Digest",
							device,
							false);
					break;
				default:
					//It'll default drop.
			}

			script += "iptables -Z " + device + "_ingress\n";
			script += "iptables -Z " + device + "_egress";
		}
		
		//Then servers
		for (String srv : model.getServerLabels()) {
			script += "\n\n";
			script += buildDailyBandwidthEmail(srv + "@" + model.getData().getDomain(srv),
					model.getData().getAdminEmail(),
					"[" + srv + "." + model.getLabel() + "] Daily Bandwidth Digest",
					srv,
					false);

			script += "iptables -Z " + srv + "_ingress\n";
			script += "iptables -Z " + srv + "_egress";		
		}

		units.addElement(new FileUnit("daily_bandwidth_alert_script_created", "proceed", script, "/etc/cron.daily/bandwidth", "I couldn't create the bandwidth digest script.  This means you and your users won't receive daily updates on bandwidth use"));
		units.addElement(new FilePermsUnit("daily_bandwidth_alert_script", "daily_bandwidth_alert_script_created", "/etc/cron.daily/bandwidth", "755", "I couldn't set the bandwidth digest script to be executable.  This means you and your users won't receive daily updates on bandwidth use"));
		
		return units;
	}

	private Vector<IUnit> subnetConfigUnits(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		InterfaceModel im = model.getServerModel(server).getInterfaceModel();
		
		String iface   = model.getData().getIface(server);
		String netmask = model.getData().getNetmask();
		
		for (String srv : model.getServerLabels()) {
			String[] cnames  = model.getData().getCnames(srv);
			
			String ip       = model.getServerModel(srv).getIP();
			String gateway  = model.getServerModel(srv).getGateway();
			String domain   = model.getData().getDomain(srv);
			String hostname = srv.replaceAll("-", "_");

			String[] subdomains = new String[cnames.length + 1];
			System.arraycopy(new String[] {model.getData().getHostname(srv)},0,subdomains,0, 1);
			System.arraycopy(cnames,0,subdomains,1, cnames.length);

			if (!model.getServerModel(server).isMetal()) {
				units.addElement(im.addIface(hostname + "_router_iface",
										"static",
										iface + ((!srv.equals(server)) ? ":0" + model.getData().getSubnet(srv) : ""),
										null,
										gateway,
										netmask,
										null,
										null));
			}
			
			this.dns.addDomainRecord(domain, gateway, subdomains, ip);
			
			for (String device : model.getDeviceLabels()) {
				String[] gateways = model.getDeviceModel(device).getGateways();
				String[] ips      = model.getDeviceModel(device).getIPs();
				
				domain   = model.getData().getDomain(server);
				String subnet   = gateways[0].split("\\.")[2];
				
				for (int i = 0; i < gateways.length; ++i) {
					String subdomain = device + "." + model.getLabel() + ".lan." + i;
					
					units.addElement(im.addIface(device.replaceAll("-", "_") + "_router_iface_" + i,
											"static",
											iface + ":1" + subnet + i,
											null,
											gateways[i],
											netmask,
											null,
											null));
					
					this.dns.addDomainRecord(domain, gateways[i], new String[] {subdomain}, ips[i]);
				}
			}
			
			units.addElement(new SimpleUnit("ifaces_up", "proceed",
					"sudo service networking restart",
					"sudo ip addr | grep " + iface, "", "fail",
					"Couldn't bring your network interfaces up.  This can potentially be resolved by a restart (assuming you've had no other network-related errors)."));
		}
			
		return units;
	}
	
	private Vector<IUnit> baseIptConfig(String server, NetworkModel model, String name, String subnet) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		FirewallModel fm = model.getServerModel(server).getFirewallModel();
		
		String cleanName    = name.replace("-", "_");
		String fwdChain     = cleanName + "_fwd";
		String ingressChain = cleanName + "_ingress";
		String egressChain  = cleanName + "_egress";

		//Create our egress chain for bandwidth (exfil?) tracking
		//In future, we could perhaps do some form of traffic blocking malarky here?
		fm.addChain(cleanName + "_egress_chain", "filter", egressChain);
		//Create our ingress chain for download bandwidth tracking
		fm.addChain(cleanName + "_ingress_chain", "filter", ingressChain);
		//Create our forward chain for all other rules
		fm.addChain(cleanName + "_fwd_chain", "filter", fwdChain);

		//Force traffic to/from a given subnet to jump to our chains
		fm.addFilterForward(cleanName + "_ipt_server_src",
				"-s " + subnet
				+ " -j "+ fwdChain);
		fm.addFilterForward(cleanName + "_ipt_server_dst",
				"-d " + subnet
				+ " -j " + fwdChain);

		//We want to default drop anything not explicitly whitelisted
		//Make sure that these are the very first rules as the chain may have been pre-populated
		fm.addFilter(cleanName + "_fwd_default_drop", fwdChain, 0,
				"-j DROP");
		
		//Don't allow any traffic in from the outside world
		fm.addFilter(cleanName + "_ingress_default_drop", ingressChain, 0,
				"-j DROP");

		//Don't allow any traffic out to the outside world
		fm.addFilter(cleanName + "_egress_default_drop", egressChain, 0,
				"-j DROP");
		
		//Add our forward chain rules (backwards(!))
		//Allow our router to talk to us
		fm.addFilter(cleanName + "_allow_router_traffic", fwdChain,
				"-s " + subnet
				+ " -j ACCEPT");

		return units;
	}
	
	private Vector<IUnit> serverIptUnits(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		Vector<String> users = new Vector<String>();
		
		FirewallModel fm = model.getServerModel(server).getFirewallModel();

		String extIface = model.getData().getExtIface(server);

		for (String device : model.getDeviceLabels()) {
			switch (model.getDeviceModel(device).getType()) {
				case "superuser":
				case "user":
					users.add(device);
					break;
			}
		}
		
		for (String srv : model.getServerLabels()) {
			String serverSubnet    = model.getServerModel(srv).getSubnet() + "/30";
			String cleanServerName = srv.replaceAll("-",  "_");
			String fwdChain        = cleanServerName + "_fwd";
			String egressChain     = cleanServerName + "_egress";
			String ingressChain    = cleanServerName + "_ingress";
			
			baseIptConfig(server, model, srv, serverSubnet);
			
			if (model.getServerModel(srv).isRouter()) {
				fm.addFilter(cleanServerName + "_allow_email_out", egressChain,
						"-p tcp"
						+ " --dport 25"
						+ " -j ACCEPT");
			}
			
			//Only actually do this if we have any users!
			if (users.size() > 0) {
				String userRule = "";
				userRule += "-s ";
				for (String user : users) {
					userRule += model.getDeviceModel(user).getSubnets()[0] + "/24,";
				}
				userRule = userRule.replaceAll(",$", ""); //Remove any trailing comma
				userRule += " -d " + serverSubnet;
				userRule += " -p tcp";
				userRule += " -m state --state NEW";
				userRule += " -m tcp -m multiport --dports 80,443";
				userRule += " -j ACCEPT";
				
				fm.addFilter("allow_users_80_443_" + server, fwdChain, userRule);
				
				for (String user : users) {
					//And if they're a superuser, they can SSH in, too
					if (model.getDeviceModel(user).getType().equals("superuser" )) {
						fm.addFilter(cleanServerName + "_ssh_" + user.replaceAll("-",  "_"), fwdChain,
								"-s " + model.getDeviceModel(user).getSubnets()[0] + "/24"
								+ " -d " + serverSubnet
								+ " -p tcp"
								+ " --dport " + model.getData().getSSHPort(srv)
								+ " -j ACCEPT");
					}
				}
			}
			
			//And servers can talk back, if established/related traffic
			fm.addFilter(cleanServerName + "_allow_related_traffic", fwdChain,
					"-p tcp"
					+ " -m state --state ESTABLISHED,RELATED"
					+ " -j ACCEPT");
	
			//Jump to the ingress/egress chains
			fm.addFilter(cleanServerName + "_jump_ingress", fwdChain,
					"-i " + extIface
					+ " -j " + ingressChain);
			fm.addFilter(cleanServerName + "_jump_egress", fwdChain,
					"-o " + extIface
					+ " -j " + egressChain);
			//Log anything hopping to our egress chain
			fm.addFilter(cleanServerName + "_log_egress_traffic", egressChain,
					"-j LOG --log-prefix \\\"ipt-" + cleanServerName + ": \\\"");
			//Allow related ingress traffic
			fm.addFilter(cleanServerName + "_allow_related_ingress_traffic", ingressChain,
					"-p tcp"
					+ " -m state --state ESTABLISHED,RELATED"
					+ " -j ACCEPT");

		}
	
		return units;
	}
	
	private Vector<IUnit> userIptUnits(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		Vector<String> users   = new Vector<String>();
		Vector<String> intOnly = new Vector<String>();
		Vector<String> extOnly = new Vector<String>();

		for (String device : model.getDeviceLabels()) {
			switch (model.getDeviceModel(device).getType()) {
				case "superuser":
				case "user":
					users.add(device);
					break;
				case "intonly":
					intOnly.add(device);
					break;
				case "extonly":
					extOnly.add(device);
					break;
			}
		}
		
		for (String user : users) {
			String cleanUserName = user.replace("-", "_");
			String fwdChain      = cleanUserName + "_fwd";
			String ingressChain  = cleanUserName + "_ingress";
			String egressChain   = cleanUserName + "_egress";
			
			String userSubnet = model.getDeviceModel(user).getSubnets()[0] + "/24";
			
			String intIface = model.getData().getIface(server);
			String extIface = model.getData().getExtIface(server);

			FirewallModel fm = model.getServerModel(server).getFirewallModel();
		
			baseIptConfig(server, model, user, userSubnet);

			for (String iface : userIfaces) {
				//They can talk to our servers on :80 && :443
				String serverRule = "";
				serverRule += "-i " + intIface + iface;
				serverRule += " -d ";

				for (String srv : model.getServerLabels()) {
					serverRule += model.getServerModel(srv).getSubnet() + "/30,";
				}
				serverRule = serverRule.replaceAll(",$", ""); //Remove any trailing comma
				
				serverRule += " -p tcp";
				serverRule += " -m state --state NEW";
				serverRule += " -m tcp -m multiport --dports 80,443";
				serverRule += " -j ACCEPT";
				
				fm.addFilter("allow_users_80_443_" + server, fwdChain, serverRule);
			}
			
			//Allow superusers to SSH into our servers
			if (model.getDeviceModel(user).getType().equals("superuser")) {
				for (String srv : model.getServerLabels()) {
					for (String iface : userIfaces) {
						fm.addFilter(cleanUserName + "_allow_ssh_traffic_" + srv.replaceAll("-",  "_"), fwdChain,
								"-i " + intIface + iface
								+ " -d " + model.getServerModel(srv).getSubnet() + "/30"
								+ " -p tcp"
								+ " --dport " + model.getData().getSSHPort(srv)
								+ " -j ACCEPT");
					}
				}
			}
				
			//Configure what they can do with internal only devices
			for (String iface : userIfaces) { 
				String intOnlyRule = "";
				intOnlyRule += "-i " + intIface + iface;
				intOnlyRule += " -d ";

				for (String device : intOnly) {
					intOnlyRule += model.getDeviceModel(device).getSubnets()[0] + "/30,";
				}
				
				intOnlyRule = intOnlyRule.replaceAll(",$", ""); //Remove any trailing comma
				intOnlyRule += " -p tcp";
				intOnlyRule += " -m state --state NEW";
				intOnlyRule += " -j ACCEPT";
				fm.addFilter("allow_int_only_" + server, fwdChain, intOnlyRule);
			}

			//Allow them to manage certain ext only devicen, if they're a superuser
			if (model.getDeviceModel(user).getType().equals("superuser")) {
				for (String device : extOnly) {
					for (String iface : userIfaces) {
						fm.addFilter(cleanUserName + "_allow_managed_traffic_nonvpn_" + device.replaceAll("-",  "_"), fwdChain,
								"-i " + intIface + iface
								+ " -d " + model.getDeviceModel(device).getSubnets()[0] + "/24"
								+ " -p tcp"
								+ " -m state --state NEW"
								+ " -m tcp -m multiport --dports 80,443"
								+ " -j ACCEPT");
					}
				}
			}
			
			//Users can talk to the outside world
			fm.addFilter(cleanUserName + "_allow_egress_traffic", egressChain,
					"-o " + extIface
					+ " -j ACCEPT");
			//And can accept established/related traffic from the outside world, too
			fm.addFilter(cleanUserName + "_allow_ingress_traffic", ingressChain,
					"-i " + extIface
					+ " -m state --state ESTABLISHED,RELATED"
					+ " -j ACCEPT");

			fm.addFilter(cleanUserName + "_allow_related_traffic", fwdChain,
					"-p tcp"
					+ " -m state --state ESTABLISHED,RELATED"
					+ " -j ACCEPT");
			
			//Jump to the ingress/egress chains
			fm.addFilter(cleanUserName + "_allow_ingress", fwdChain,
					"-i " + extIface
					+ " -j " + ingressChain);
			fm.addFilter(cleanUserName + "_allow_egress", fwdChain,
					"-o " + extIface
					+ " -j " + egressChain);
			
			//Log any NEW connections on our egress chain
			fm.addFilter(cleanUserName + "_log_new_egress_traffic", egressChain,
					"-m state --state NEW"
					+ " -j LOG --log-prefix \\\"ipt-" + cleanUserName + "-[new]: \\\"");
			//Log any DESTROYED conections on our egress chain
			fm.addFilter(cleanUserName + "_log_destroyed_fin_egress_traffic", egressChain,
					"-p tcp --tcp-flags FIN FIN"
					+ " -j LOG --log-prefix \\\"ipt-" + cleanUserName + "-[fin]: \\\"");
			fm.addFilter(cleanUserName + "_log_destroyed_fin_egress_traffic", egressChain,
					"-p tcp --tcp-flags RST RST"
					+ " -j LOG --log-prefix \\\"ipt-" + cleanUserName + "-[rst]: \\\"");
		}
		
		return units;
	}
	
	private Vector<IUnit> intOnlyIptUnits(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		Vector<String> users   = new Vector<String>();
		Vector<String> intOnly = new Vector<String>();

		for (String device : model.getDeviceLabels()) {
			switch (model.getDeviceModel(device).getType()) {
				case "superuser":
				case "user":
					users.add(device);
					break;
				case "intonly":
					intOnly.add(device);
					break;
			}
		}
		
		for (String device : intOnly) {
			String cleanDeviceName = device.replace("-", "_");
			String fwdChain        = cleanDeviceName + "_fwd";
			String ingressChain    = cleanDeviceName + "_ingress";
			String egressChain     = cleanDeviceName + "_egress";
			
			String deviceSubnet = model.getDeviceModel(device).getSubnets()[0] + "/24";
			
			String extIface = model.getData().getExtIface(server);
			String intIface = model.getData().getIface(server);

			FirewallModel fm = model.getServerModel(server).getFirewallModel();
		
			baseIptConfig(server, model, device, deviceSubnet);

			//Jump to the ingress/egress chains
			fm.addFilter(cleanDeviceName + "_allow_ingress", fwdChain,
					"-i " + extIface
					+ " -j " + ingressChain);
			fm.addFilter(cleanDeviceName + "_allow_egress", fwdChain,
					"-o " + extIface
					+ " -j " + egressChain);
			//Log anything hopping to our egress chain
			fm.addFilter(cleanDeviceName + "_log_egress_traffic", egressChain,
					"-j LOG --log-prefix \\\"ipt-" + cleanDeviceName + ": \\\"");
			
			//Users can talk to our internal only devices
			//Configure what they can do with internal only devices
			String intOnlyRule = "";
			intOnlyRule += "-d " + deviceSubnet;
			intOnlyRule += " -s ";

			for (String user : users) {
				intOnlyRule += model.getDeviceModel(user).getSubnets()[0] + "/24,";
			}
			
			intOnlyRule = intOnlyRule.replaceAll(",$", ""); //Remove any trailing comma
			intOnlyRule += " -p tcp";
			intOnlyRule += " -m state --state NEW";
			intOnlyRule += " -j ACCEPT";
			fm.addFilter("allow_users_" + device, fwdChain, intOnlyRule);
			
			fm.addFilter(cleanDeviceName + "_allow_related_traffic", fwdChain,
					"-p tcp"
					+ " -m state --state ESTABLISHED,RELATED"
					+ " -j ACCEPT");
		}
		
		return units;
	}
	
	private Vector<IUnit> extOnlyIptUnits(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		Vector<String> superusers = new Vector<String>();
		Vector<String> extOnly    = new Vector<String>();

		for (String device : model.getDeviceLabels()) {
			switch (model.getDeviceModel(device).getType()) {
				case "superuser":
					superusers.add(device);
				case "extonly":
					extOnly.add(device);
					break;
			}
		}
		
		for (String device : extOnly) {
			String cleanDeviceName = device.replace("-", "_");
			String fwdChain     = cleanDeviceName + "_fwd";
			String ingressChain = cleanDeviceName + "_ingress";
			String egressChain  = cleanDeviceName + "_egress";
			
			String deviceSubnet = model.getDeviceModel(device).getSubnets()[0] + "/24";
			
			String extIface = model.getData().getExtIface(server);

			FirewallModel fm = model.getServerModel(server).getFirewallModel();
		
			baseIptConfig(server, model, device, deviceSubnet);

			//External only devices can talk to the outside world
			fm.addFilter(cleanDeviceName + "_allow_egress_traffic", egressChain,
					"-o " + extIface
					+ " -j ACCEPT");
			//And can accept established/related traffic from the outside world, too
			fm.addFilter(cleanDeviceName + "_allow_ingress_traffic", ingressChain,
					"-i " + extIface
					+ " -m state --state ESTABLISHED,RELATED"
					+ " -j ACCEPT");
			
			if (model.getDeviceModel(device).isManaged()) {
				for (String superuser : superusers) {
					//And can accept established/related traffic from the outside world, too
					fm.addFilter(cleanDeviceName + "_allow_management_traffic", fwdChain,
							"-s " + model.getDeviceModel(superuser).getSubnets()[0] + "/24"
							+ " -m state --state ESTABLISHED,RELATED"
							+ " -j ACCEPT");
				}
			}
			
			//Jump to the ingress/egress chains
			fm.addFilter(cleanDeviceName + "_allow_ingress", fwdChain,
					"-i " + extIface
					+ " -j " + ingressChain);
			fm.addFilter(cleanDeviceName + "_allow_egress", fwdChain,
					"-o " + extIface
					+ " -j " + egressChain);
		}
		
		return units;
	}
	
	private Vector<IUnit> extConnConfigUnits(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();

		if (model.getData().getExtConn(server).contains("ppp")) {
			units.addElement(new InstalledUnit("ext_ppp", "ppp"));
			units.addElement(new RunningUnit("ext_ppp", "ppp", "pppd-dns"));
			units.addElement(model.getServerModel(server).getInterfaceModel().addPPPIface("router_ext_ppp_iface", model.getData().getProperty(server, "pppiface")));
			model.getServerModel(server).getProcessModel().addProcess("/usr/sbin/pppd call provider$");
			units.addElement(new FileUnit("resolv_conf", "proceed", "nameserver 127.0.0.1", "/etc/ppp/resolv.conf"));
		}
		else if (model.getData().getExtConn(server).equals("dhcp")){
			units.addElement(model.getServerModel(server).getInterfaceModel().addIface("router_ext_dhcp_iface", 
																						"dhcp",
																						model.getData().getExtIface(server),
																						null,
																						null,
																						null,
																						null,
																						null));

			String dhclient = "option rfc3442-classless-static-routes code 121 = array of unsigned integer 8;\n";
			dhclient += "send host-name = gethostname();\n";
			dhclient += "supersede domain-search \\\"" + model.getData().getDomain(server) + "\\\";\n";
			dhclient += "supersede domain-name-servers " + model.getServerModel(server).getGateway() + ";\n";
			dhclient += "request subnet-mask, broadcast-address, time-offset, routers,\n";
			dhclient += "	domain-name, domain-name-servers, domain-search, host-name,\n";
			dhclient += "	dhcp6.name-servers, dhcp6.domain-search,\n";
			dhclient += "	netbios-name-servers, netbios-scope, interface-mtu,\n";
			dhclient += "	rfc3442-classless-static-routes, ntp-servers;";
			units.addElement(new FileUnit("router_ext_dhcp_persist", "proceed", dhclient, "/etc/dhcp/dhclient.conf"));

			units.addElement(model.getServerModel(server).getFirewallModel().addFilterInput("router_ext_dhcp_in",
					"-i " + model.getData().getExtIface(server) + " -d 255.255.255.255 -p udp --dport 68 --sport 67 -j ACCEPT"));
			units.addElement(model.getServerModel(server).getFirewallModel().addFilterOutput("router_ext_dhcp_ipt_out", 
					"-o " + model.getData().getExtIface(server) + " -d " + model.getData().getDNS() + " -p udp --dport 67 --sport 68 -j ACCEPT"));
		}
		else if(model.getData().getExtConn(server).equals("static")) {
			JsonArray interfaces = (JsonArray) model.getData().getPropertyObjectArray(server, "extconfig");
			
			for (int i = 0; i < interfaces.size(); ++i) {
				JsonObject row = interfaces.getJsonObject(i);
				
				String address   = row.getString("address");
				String netmask   = row.getString("netmask");
				String gateway   = row.getString("gateway", null);
				String broadcast = row.getString("broadcast");
				String iface     = model.getData().getExtIface(server);
				
				if (i > 0) {
					iface += ":" + i;
				}
				
				units.addElement(model.getServerModel(server).getInterfaceModel().addIface("router_ext_static_iface_" + i,
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
}
