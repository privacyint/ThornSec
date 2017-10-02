package profile;

import java.util.Vector;

import javax.json.JsonArray;
import javax.json.JsonObject;

import core.iface.IUnit;
import core.model.FirewallModel;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.FileAppendUnit;
import core.unit.fs.FilePermsUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;

public class Router extends AStructuredProfile {

	DNS dns;
	DHCP dhcp;
	QoS qos;
	
	public Router() {
		super("router");
		
		dns = new DNS();
		dhcp = new DHCP();
		qos = new QoS();
	}

	public Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
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
		
		units.addAll(dns.getInstalled(server, model));
		units.addAll(dhcp.getInstalled(server, model));
		units.addAll(qos.getInstalled(server, model));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
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
		units.addAll(qos.getLiveConfig(server, model));
		
		units.addAll(subnetConfigUnits(server, model));
		
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
		
		String[] devices = model.getDeviceLabels();
		String[] servers = model.getServerLabels();
		
		//Iterate through devicen first
		for (int i = 0; i < devices.length; ++i) {
			switch (model.getDeviceModel(devices[i]).getType()) {
				//Email the user only
				case "user":
				case "superuser":
					script += "\n\n";
					script += buildDailyBandwidthEmail(model.getData().getAdminEmail(),
													   devices[i] + "@" + model.getData().getDomain(),
													   "[" + devices[i] + "." + model.getData().getLabel() + "] Daily Bandwidth Digest",
													   devices[i],
													   true);
					break;
				//This is a peripheral of some sort.  Just let the responsible person know.
				case "intonly":
				case "extonly":
					script += "\n\n";
					script += buildDailyBandwidthEmail(devices[i] + "@" + model.getData().getDomain(),
							   model.getData().getAdminEmail(),
							   "[" + devices[i] + "." + model.getLabel() + "] Daily Bandwidth Digest",
							   devices[i],
							   false);
					break;
				default:
					//It'll default drop.
			}

			script += "iptables -Z " + devices[i] + "_ingress\n";
			script += "iptables -Z " + devices[i] + "_egress";
		}
		
		//Then servers
		for (int i = 0; i < servers.length; ++i) {
			script += "\n\n";
			script += buildDailyBandwidthEmail(servers[i] + "@" + model.getData().getDomain(),
					   model.getData().getAdminEmail(),
					   "[" + servers[i] + "." + model.getLabel() + "] Daily Bandwidth Digest",
					   servers[i],
					   false);

			script += "iptables -Z " + servers[i] + "_ingress\n";
			script += "iptables -Z " + servers[i] + "_egress";		
		}

		units.addElement(new FileUnit("daily_bandwidth_alert_script", "proceed", script, "/etc/cron.daily/bandwidth", "I couldn't create the bandwidth digest script.  This means you and your users won't receive daily updates on bandwidth use"));
		units.addElement(new FilePermsUnit("daily_bandwidth_alert_script", "daily_bandwidth_alert_script", "/etc/cron.daily/bandwidth", "755", "I couldn't set the bandwidth digest script to be executable.  This means you and your users won't receive daily updates on bandwidth use"));
		
		return units;
	}

	private Vector<IUnit> subnetConfigUnits(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		if (!model.getServerModel(server).isMetal()) {
			String[] servers = model.getServerLabels();
			String[] devices = model.getDeviceLabels();
	
			for (int i = 0; i < servers.length; ++i) {
				units.addElement(model.getServerModel(server).getInterfaceModel().addIface(servers[i].replaceAll("-", "_") + "_router_iface",
																						   "static",
																						   model.getData().getIface(server),
																						   null,
																						   model.getServerModel(servers[i]).getGateway(),
																						   model.getData().getNetmask(),
																						   null,
																						   null));
			}
						
			for (int i = 0; i < devices.length; ++i) {
				String[] gateways = model.getDeviceModel(devices[i]).getGateways();
				
				for (int j = 0; j < gateways.length; ++j) {
					units.addElement(model.getServerModel(server).getInterfaceModel().addIface(devices[i].replaceAll("-", "_") + "_router_iface_" + j,
																							   "static",
																							   model.getData().getIface(server),
																							   null,
																							   gateways[j],
																							   model.getData().getNetmask(),
																							   null,
																							   null));
				}
			}
			
			units.addElement(new SimpleUnit("ifaces_up", "proceed",
					"sudo service networking restart",
					"sudo ip addr | grep " + model.getData().getIface(server), "", "fail",
					"Couldn't bring your network interfaces up.  This can potentially be resolved by a restart (assuming you've had no other network-related errors)."));
		}
			
		return units;
	}
	
	private Vector<IUnit> baseIptConfig(String server, NetworkModel model, String name, String subnet) {
		Vector<IUnit> units = new Vector<IUnit>();
		
        FirewallModel fm = model.getServerModel(server).getFirewallModel();
        String intIface  = model.getData().getIface(server);
        String extIface  = model.getData().getExtIface(server);
        
        String cleanName    = name.replace("-", "_");
        String fwdChain     = cleanName + "_fwd";
        String ingressChain = cleanName + "_ingress";
        String egressChain  = cleanName + "_egress";

        //Create our egress chain for bandwidth (exfil?) tracking
		//In future, we could perhaps do some form of traffic blocking malarky here?
		units.addElement(fm.addChain(cleanName + "_egress_chain", "filter", egressChain));
		//Create our ingress chain for download bandwidth tracking
		units.addElement(fm.addChain(cleanName + "_ingress_chain", "filter", ingressChain));
		//Create our forward chain for all other rules
		units.addElement(fm.addChain(cleanName + "_fwd_chain", "filter", fwdChain));

        //Force traffic to/from a given subnet to jump to our chains
        units.addElement(fm.addFilterForward(cleanName + "_ipt_server_src",
				"-s " + subnet
				+ " -j "+ fwdChain));
		units.addElement(fm.addFilterForward(cleanName + "_ipt_server_dst",
				"-d " + subnet
				+ " -j " + fwdChain));

		//We want to default drop anything not explicitly whitelisted
		units.addElement(fm.addFilter(cleanName + "_fwd_default_drop", fwdChain,
                "-j DROP"));
		
		//Jump to the ingress/egress chains
		units.addElement(fm.addFilter(cleanName + "_allow_ingress", fwdChain,
				"-i " + extIface + " -o " + intIface
				+ " -j " + ingressChain));
		units.addElement(fm.addFilter(cleanName + "_allow_egress", fwdChain,
				"-i " + intIface + " -o " + extIface
				+ " -j " + egressChain));
		//Log anything hopping to our egress chain
		units.addElement(fm.addFilter(cleanName + "_log_egress_traffic", fwdChain,
				"-j LOG --log-prefix \\\"ipt-" + name + ": \\\""));

		//Don't allow any traffic in from the outside world
		units.addElement(fm.addFilter(cleanName + "_ingress_default_drop", ingressChain,
                "-j DROP"));

		//Don't allow any traffic out to the outside world
		units.addElement(fm.addFilter(cleanName + "_egress_default_drop", egressChain,
                "-j DROP"));
		
		//Add our forward chain rules (backwards(!))
		//Allow our router to talk to us
		units.addElement(fm.addFilter(cleanName + "_allow_router_traffic", fwdChain,
				"-s " + subnet
				+ " -j ACCEPT"));

		return units;
	}
	
	private Vector<IUnit> serverIptUnits(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String[] servers = model.getServerLabels();
		String[] devices = model.getDeviceLabels();
		
		Vector<String> users = new Vector<String>();
		
        FirewallModel fm = model.getServerModel(server).getFirewallModel();

        String intIface = model.getData().getIface(server);
        String extIface = model.getData().getExtIface(server);

		for (int i = 0; i < devices.length; ++i) {
			switch (model.getDeviceModel(devices[i]).getType()) {
				case "superuser":
				case "user":
					users.add(devices[i]);
					break;
			}
		}
        
		for (int i = 0; i < servers.length; ++i) {
			String serverSubnet    = model.getServerModel(servers[i]).getSubnet() + "/30";
			String cleanServerName = servers[i].replaceAll("-",  "_");
	        String fwdChain        = cleanServerName + "_fwd";
			
			units.addAll(baseIptConfig(server, model, servers[i], serverSubnet));
			
			for (int j = 0; j < users.size(); ++j) {
	            //They can talk to our servers on :80 && :443
				units.addElement(fm.addFilter(cleanServerName + "_allow_http_traffic_" + users.elementAt(j).replaceAll("-",  "_"), fwdChain,
						"-s " + model.getDeviceModel(users.elementAt(j)).getSubnets()[0] + "/24"
						+ " -d " + serverSubnet
						+ " -p tcp"
						+ " --dport 80"
						+ " -j ACCEPT"));
				units.addElement(fm.addFilter(cleanServerName + "_allow_https_traffic_" + users.elementAt(j).replaceAll("-",  "_"), fwdChain,
						"-s " + model.getDeviceModel(users.elementAt(j)).getSubnets()[0] + "/24"
						+ " -d " + serverSubnet
						+ " -p tcp"
						+ " --dport 443"
						+ " -j ACCEPT"));
				
				//And if they're a superuser, they can SSH in, too
				if (model.getDeviceModel(users.elementAt(j)).getType().equals("superuser" )) {
					units.addElement(fm.addFilter(cleanServerName + "_ssh_" + users.elementAt(j).replaceAll("-",  "_"), fwdChain,
							"-s " + model.getDeviceModel(users.elementAt(j)).getSubnets()[0] + "/24"
							+ " -d " + serverSubnet
							+ " -p tcp"
							+ " --dport " + model.getData().getSSHPort(servers[i])
							+ " -j ACCEPT"));
				}
				
				//And servers can talk back to them, if established/related traffic
				units.addElement(fm.addFilter(cleanServerName + "_allow_https_traffic_" + users.elementAt(j).replaceAll("-",  "_"), fwdChain,
						"-s " + serverSubnet
						+ " -d " + model.getDeviceModel(users.elementAt(j)).getSubnets()[0] + "/24"
						+ " -p tcp"
						+ " -m state --state ESTABLISHED,RELATED"
						+ " -j ACCEPT"));
			}

			
			/*
			 * There's probably no utility in this.
			 * 
			//We now want to make sure that under no circumstances can servers SSH between each other. Don't care about anything else!
			//Anything else can be handled on the server-side iptables rules.
			for (int j = 0; j < servers.length; ++j) {
				if (i != j) {
					units.addElement(fm.addFilter(cleanServerName + "_deny_intra_server_ssh_traffic_" + j, fwdChain,
							"-s " + serverSubnet
							+ " -d " + model.getServerModel(servers[j]).getSubnet() + "/30"
							+ " -p tcp"
							+ " --dport " + model.getData().getSSHPort(servers[j])
							+ " -j DROP"));
				}
			}*/
		}
	
		return units;
	}
	
	private Vector<IUnit> userIptUnits(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String[] devices       = model.getDeviceLabels();
		Vector<String> users   = new Vector<String>();
		Vector<String> intOnly = new Vector<String>();

		for (int i = 0; i < devices.length; ++i) {
			switch (model.getDeviceModel(devices[i]).getType()) {
				case "superuser":
				case "user":
					users.add(devices[i]);
					break;
				case "intOnly":
					intOnly.add(devices[i]);
					break;
			}
		}
		
		for (int i = 0; i < users.size(); ++i) {
            String cleanUserName   = users.elementAt(i).replace("-", "_");
            String fwdChain        = cleanUserName + "_fwd";
            String ingressChain    = cleanUserName + "_ingress";
            String egressChain     = cleanUserName + "_egress";
            
            String userSubnet = model.getDeviceModel(users.elementAt(i)).getSubnets()[0] + "/24";
            
            String intIface = model.getData().getIface(server);
            String extIface = model.getData().getExtIface(server);

            FirewallModel fm = model.getServerModel(server).getFirewallModel();
        
			units.addAll(baseIptConfig(server, model, users.elementAt(i), userSubnet));

			//Configure what users can do with our servers
        	String[] servers = model.getServerLabels();

			for (int j = 0; j < servers.length; ++j) {
	            //They can talk to our servers on :80 && :443
				units.addElement(fm.addFilter(cleanUserName + "_allow_http_traffic_" + servers[j].replaceAll("-",  "_"), fwdChain,
						"-s " + userSubnet
						+ " -d " + model.getServerModel(servers[j]).getSubnet() + "/30"
						+ " -p tcp"
						+ " --dport 80"
						+ " -j ACCEPT"));
				units.addElement(fm.addFilter(cleanUserName + "_allow_https_traffic_" + servers[j].replaceAll("-",  "_"), fwdChain,
						"-s " + userSubnet
						+ " -d " + model.getServerModel(servers[j]).getSubnet() + "/30"
						+ " -p tcp"
						+ " --dport 443"
						+ " -j ACCEPT"));

				//Allow superusers to SSH into our servers
	            if (model.getDeviceModel(users.elementAt(i)).getType().equals("superuser")) {
					units.addElement(fm.addFilter(cleanUserName + "_allow_ssh_traffic_" + servers[j].replaceAll("-",  "_"), fwdChain,
							"-s " + userSubnet
							+ " -d " + model.getServerModel(servers[j]).getSubnet() + "/30"
							+ " -p tcp"
							+ " --dport " + model.getData().getSSHPort(servers[j])
							+ " -j ACCEPT"));
	            }
				
				//And servers can talk back to them, if established/related traffic
				units.addElement(fm.addFilter(cleanUserName + "_allow_response_traffic_" + servers[j].replaceAll("-",  "_"), fwdChain,
						"-s " + model.getServerModel(servers[j]).getSubnet() + "/30"
						+ " -d " + userSubnet
						+ " -p tcp"
						+ " -m state --state ESTABLISHED,RELATED"
						+ " -j ACCEPT"));
			}
			
			//Configure what they can do with internal only devices
			for (int j = 0; j < intOnly.size(); ++j) {
				//Any user can talk to an internal-only device
				units.addElement(fm.addFilter(cleanUserName + "_allow_int_only_" + intOnly.elementAt(j).replaceAll("-",  "_"), fwdChain,
						"-s " + userSubnet
						+ " -d " + model.getDeviceModel(intOnly.elementAt(j)).getSubnets()[0] + "/24"
						+ " -j ACCEPT"));
				//But these devices can only talk back, not initiate a conversation
				units.addElement(fm.addFilter(cleanUserName + "_allow_int_only_response_" + intOnly.elementAt(j).replaceAll("-",  "_"), fwdChain,
						"-s " + model.getDeviceModel(intOnly.elementAt(j)).getSubnets()[0] + "/24"
						+ " -d " + userSubnet
						+ " -m state --state ESTABLISHED,RELATED"
						+ " -j ACCEPT"));
			}
			
			//Users can talk to the outside world
			units.addElement(fm.addFilter(cleanUserName + "_allow_egress_traffic", egressChain,
					"-i " + intIface
					+ " -o " + extIface
					+ " -j ACCEPT"));
			//And can accept established/related traffic from the outside world, too
			units.addElement(fm.addFilter(cleanUserName + "_allow_ingress_traffic", ingressChain,
					"-i " + extIface
					+ " -o " + intIface
					+ " -m state --state ESTABLISHED,RELATED"
					+ " -j ACCEPT"));
    	}
		
		return units;
	}
	
	private Vector<IUnit> intOnlyIptUnits(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String[] devices       = model.getDeviceLabels();
		Vector<String> users   = new Vector<String>();
		Vector<String> intOnly = new Vector<String>();

		for (int i = 0; i < devices.length; ++i) {
			switch (model.getDeviceModel(devices[i]).getType()) {
				case "superuser":
				case "user":
					users.add(devices[i]);
					break;
				case "intOnly":
					intOnly.add(devices[i]);
					break;
			}
		}
		
		for (int i = 0; i < intOnly.size(); ++i) {
            String cleanDeviceName = intOnly.elementAt(i).replace("-", "_");
            String fwdChain        = cleanDeviceName + "_fwd";
            String ingressChain    = cleanDeviceName + "_ingress";
            String egressChain     = cleanDeviceName + "_egress";
            
            String deviceSubnet = model.getDeviceModel(intOnly.elementAt(i)).getSubnets()[0] + "/24";
            
            String intIface = model.getData().getIface(server);
            String extIface = model.getData().getExtIface(server);

            FirewallModel fm = model.getServerModel(server).getFirewallModel();
        
			units.addAll(baseIptConfig(server, model, intOnly.elementAt(i), deviceSubnet));

            //Users can talk to our internal only devices
			for (int j = 0; j < users.size(); ++j) {
				//Any user can talk to an internal-only device
				units.addElement(fm.addFilter(cleanDeviceName + "_allow_traffic_" + users.elementAt(j).replaceAll("-",  "_"), fwdChain,
						"-s " + model.getDeviceModel(users.elementAt(j)).getSubnets()[0] + "/24"
						+ " -d " + deviceSubnet
						+ " -j ACCEPT"));
				//But these devices can only talk back, not initiate a conversation
				units.addElement(fm.addFilter(cleanDeviceName + "_allow_response_" + users.elementAt(j).replaceAll("-",  "_"), fwdChain,
						"-s " + deviceSubnet
						+ " -d " + model.getDeviceModel(users.elementAt(j)).getSubnets()[0] + "/24"
						+ " -m state --state ESTABLISHED,RELATED"
						+ " -j ACCEPT"));
			}
		}
		
		return units;
	}
	
	private Vector<IUnit> extOnlyIptUnits(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String[] devices       = model.getDeviceLabels();
		Vector<String> extOnly = new Vector<String>();

		for (int i = 0; i < devices.length; ++i) {
			switch (model.getDeviceModel(devices[i]).getType()) {
				case "extOnly":
					extOnly.add(devices[i]);
					break;
			}
		}
		
		for (int i = 0; i < extOnly.size(); ++i) {
            String cleanDeviceName = extOnly.elementAt(i).replace("-", "_");
            String fwdChain        = cleanDeviceName + "_fwd";
            String ingressChain    = cleanDeviceName + "_ingress";
            String egressChain     = cleanDeviceName + "_egress";
            
            String deviceSubnet = model.getDeviceModel(extOnly.elementAt(i)).getSubnets()[0] + "/24";
            
            String intIface = model.getData().getIface(server);
            String extIface = model.getData().getExtIface(server);

            FirewallModel fm = model.getServerModel(server).getFirewallModel();
        
			units.addAll(baseIptConfig(server, model, extOnly.elementAt(i), deviceSubnet));

			//External only devices can talk to the outside world
			units.addElement(fm.addFilter(cleanDeviceName + "_allow_egress_traffic", egressChain,
					"-i " + intIface
					+ " -o " + extIface
					+ " -j ACCEPT"));
			//And can accept established/related traffic from the outside world, too
			units.addElement(fm.addFilter(cleanDeviceName + "_allow_ingress_traffic", ingressChain,
					"-i " + extIface
					+ " -o " + intIface
					+ " -m state --state ESTABLISHED,RELATED"
					+ " -j ACCEPT"));
		}
		
		return units;
	}
	
	private Vector<IUnit> extConnConfigUnits(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();

		if (model.getData().getExtConn(server).contains("ppp")) {
			units.addElement(new InstalledUnit("ext_ppp", "ppp"));
			units.addElement(new RunningUnit("ext_ppp", "ppp", "pppd-dns"));
			units.addElement(model.getServerModel(server).getInterfaceModel().addPPPIface("router_ext_ppp_iface", model.getData().getProperty(server, "pppiface")));
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
			dhclient += "supersede domain-search \\\"" + model.getData().getDomain() + "\\\";\n";
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
				String gateway   = row.getString("gateway");
				String broadcast = row.getString("broadcast");

				units.addElement(model.getServerModel(server).getInterfaceModel().addIface("router_ext_static_iface_" + i,
																						   "static",
																						   model.getData().getExtIface(server),
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