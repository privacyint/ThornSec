package profile;

import java.util.Vector;

import javax.json.JsonArray;
import javax.json.JsonObject;

import core.iface.IUnit;
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
	
	public Router() {
		super("router");
		
		dns = new DNS();
		dhcp = new DHCP();
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
		
		units.addAll(extConnConfigUnits(server, model));
		
		return units;
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new InstalledUnit("xsltproc", "xsltproc"));

		units.addAll(dns.getInstalled(server, model));
		units.addAll(dhcp.getInstalled(server, model));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String tcInit = "";
		tcInit += "#!/bin/bash\n";
		tcInit += "TC=/sbin/tc\n";
		tcInit += "#DNLD=150Kbit # DOWNLOAD Limit\n";
		tcInit += "#DWEIGHT=15Kbit # DOWNLOAD Weight Factor ~ 1/10 of DOWNLOAD Limit\n";
		tcInit += "UPLD=150KBit # UPLOAD Limit\n";
		tcInit += "UWEIGHT=15Kbit # UPLOAD Weight Factor\n";
		tcInit += "INTIFACE=" + model.getData().getIface(server) + "\n";
		tcInit += "EXTIFACE=" + model.getData().getExtIface(server) + "\n";
		tcInit += "\n";
		tcInit += "tc_start() {\n";
		tcInit += "#    \\$TC qdisc add dev \\$INTIFACE root handle 11: cbq bandwidth 1000Mbit avpkt 1000 mpu 64\n";
		tcInit += "#    \\$TC class add dev \\$INTIFACE parent 11:0 classid 11:1 cbq rate \\$DNLD weight \\$DWEIGHT allot 1514 prio 1 avpkt 1000 bounded\n";
		tcInit += "#    \\$TC filter add dev \\$INTIFACE parent 11:0 protocol ip handle 4 fw flowid 11:1\n";
		tcInit += "\n";
		tcInit += "    \\$TC qdisc add dev \\$EXTIFACE root handle 10: cbq bandwidth 1000Mbit avpkt 1000 mpu 64\n";
		tcInit += "    \\$TC class add dev \\$EXTIFACE parent 10:0 classid 10:1 cbq rate \\$UPLD weight \\$UWEIGHT allot 1514 prio 1 avpkt 1000 bounded\n";
		tcInit += "    \\$TC filter add dev \\$EXTIFACE parent 10:0 protocol ip handle 4 fw flowid 10:1\n";
		tcInit += "}\n";
		tcInit += "\n";
		tcInit += "tc_stop() {\n";
		tcInit += "#    \\$TC qdisc del dev \\$INTIFACE root\n";
		tcInit += "    \\$TC qdisc del dev \\$EXTIFACE root\n";
		tcInit += "}\n";
		tcInit += "\n";
		tcInit += "tc_restart() {\n";
		tcInit += "    tc_stop\n";
		tcInit += "    sleep 1\n";
		tcInit += "    tc_start\n";
		tcInit += "}\n";
		tcInit += "\n";
		tcInit += "tc_show() {\n";
		tcInit += "#    echo \\\"\\\"\n";
		tcInit += "#    echo \\\"\\$INTIFACE:\\\"\n";
		tcInit += "#    \\$TC qdisc show dev \\$INTIFACE\n";
		tcInit += "#    \\$TC class show dev \\$INTIFACE\n";
		tcInit += "#    \\$TC filter show dev \\$INTIFACE\n";
		tcInit += "#    echo \\\"\\\"\n";
		tcInit += "\n";
		tcInit += "    echo \\\"\\$EXTIFACE:\\\"\n";
		tcInit += "    \\$TC qdisc show dev \\$EXTIFACE\n";
		tcInit += "    \\$TC class show dev \\$EXTIFACE\n";
		tcInit += "    \\$TC filter show dev \\$EXTIFACE\n";
		tcInit += "    echo \\\"\\\"\n";
		tcInit += "}\n";
		tcInit += "\n";
		tcInit += "case \\\"\\$1\\\" in\n";
		tcInit += "  start)\n";
		tcInit += "    echo -n \\\"Starting bandwidth shaping: \\\"\n";
		tcInit += "    tc_start\n";
		tcInit += "    echo \\\"done\\\"\n";
		tcInit += "    ;;\n";
		tcInit += "  stop)\n";
		tcInit += "    echo -n \\\"Stopping bandwidth shaping: \\\"\n";
		tcInit += "    tc_stop\n";
		tcInit += "    echo \\\"done\\\"\n";
		tcInit += "    ;;\n";
		tcInit += "  restart)\n";
		tcInit += "    echo -n \\\"Restarting bandwidth shaping: \\\"\n";
		tcInit += "    tc_restart\n";
		tcInit += "    echo \\\"done\\\"\n";
		tcInit += "    ;;\n";
		tcInit += "  *)\n";
		tcInit += "    echo \\\"Usage: /etc/init.d/tc.sh {start|stop|restart|show}\\\"\n";
		tcInit += "    ;;\n";
		tcInit += "esac\n";
		tcInit += "exit 0";
		
		units.addElement(new FileUnit("tc_init_script_created", "proceed", tcInit, "/etc/init.d/tc.sh",
				"I couldn't output the file for starting bandwidth shaping.  This means you won't be able to "
				+ "control any potential exfiltration from your network."));
		units.addElement(new FilePermsUnit("tc_init_script_perms", "tc_init_script_created", "/etc/init.d/tc.sh", "755"));
		
		
		units.addAll(dhcp.getPersistentFirewall(server, model));
		units.addAll(dns.getPersistentFirewall(server, model));
		
		units.addAll(deviceIptUnits(server, model));
		units.addAll(serverIptUnits(server, model));

		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(dhcp.getLiveConfig(server, model));
		units.addAll(dns.getLiveConfig(server, model));
		
		units.addAll(subnetConfigUnits(server, model));
		
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
				if (model.getDeviceModel(devices[i]).getWiredMac() != null) {
					units.addElement(model.getServerModel(server).getInterfaceModel().addIface(devices[i].replaceAll("-", "_") + "_router_wired_iface",
																							   "static",
																							   model.getData().getIface(server),
																							   null,
																							   model.getDeviceModel(devices[i]).getWiredGateway(),
																							   model.getData().getNetmask(),
																							   null,
																							   null));
				}
				if (model.getDeviceModel(devices[i]).getWirelessMac() != null) {
					units.addElement(model.getServerModel(server).getInterfaceModel().addIface(devices[i].replaceAll("-", "_") + "_router_wireless_iface",
																							   "static",
																							   model.getData().getIface(server),
																							   null,
																							   model.getDeviceModel(devices[i]).getWirelessGateway(),
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
	
	private Vector<IUnit> serverIptUnits(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String[] servers = model.getServerLabels();

		//Force traffic to/from servers to jump to our chains
		for (int i = 0; i < servers.length; ++i) {
			units.addElement(model.getServerModel(server).getFirewallModel().addFilterForward(
					servers[i].replace("-",  "_") + "_ipt_server_src",
					"-s " + model.getServerModel(servers[i]).getBroadcast() + "/30 -j "
					+ servers[i].replace("-",  "_") + "_fwd"));
			units.addElement(model.getServerModel(server).getFirewallModel().addFilterForward(
					servers[i].replace("-",  "_") + "_ipt_server_dst",
					"-d " + model.getServerModel(servers[i]).getBroadcast() + "/30 -j " + servers[i].replace("-",  "_") + "_fwd"));

			//Build the chain
			units.addElement(model.getServerModel(server).getFirewallModel().addChain("server_fwd_chain_" + servers[i].replace("-",  "_"), "filter", servers[i].replace("-",  "_") + "_fwd"));
		
			//Add our rules (backwards(!))
			units.addElement(model.getServerModel(server).getFirewallModel().addFilter("server_default_drop_" + servers[i].replaceAll("-", "_"), servers[i].replace("-",  "_") + "_fwd",
					"-j DROP"));

			//Allow (and log) anything passing through the router
			units.addElement(model.getServerModel(server).getFirewallModel().addFilter("server_allow_inet_traffic_" + servers[i].replaceAll("-",  "_"), servers[i].replace("-",  "_") + "_fwd",
					"-i " + model.getData().getExtIface(server) + " -o " + model.getData().getIface(server) + " -j ACCEPT"));
			units.addElement(model.getServerModel(server).getFirewallModel().addFilter("server_allow_inet_traffic_" + servers[i].replaceAll("-",  "_"), servers[i].replace("-",  "_") + "_fwd",
					"-i " + model.getData().getIface(server) + " -o " + model.getData().getExtIface(server) + " -j ACCEPT"));
			units.addElement(model.getServerModel(server).getFirewallModel().addFilter("server_log_inet_traffic_" + servers[i].replaceAll("-",  "_"), servers[i].replace("-",  "_") + "_fwd",
					"-j LOG --log-prefix \\\"ipt-" + servers[i] + ": \\\""));
		
			units.addElement(model.getServerModel(server).getFirewallModel().addFilter("allow_all_server_traffic_" + servers[i].replaceAll("-",  "_"), servers[i].replace("-",  "_") + "_fwd",
					"-s " + model.getServerModel(servers[i]).getBroadcast() + "/30"
					+ " -j ACCEPT"));

			units.addElement(model.getServerModel(server).getFirewallModel().addFilter("allow_all_server_traffic_" + servers[i].replaceAll("-",  "_"), servers[i].replace("-",  "_") + "_fwd",
					"-d " + model.getServerModel(servers[i]).getBroadcast() + "/30"
					+ " -j ACCEPT"));
			
			//We now want to make sure that under no circumstances can servers SSH between each other. Don't care about anything else!
			//Anything else can be handled on the server-side iptables rules.
			for (int j = 0; j < servers.length; ++j) {
				if (i != j) {
					units.addElement(model.getServerModel(server).getFirewallModel().addFilter("server_deny_ssh_traffic_" + servers[i].replaceAll("-",  "_"), servers[i].replace("-",  "_") + "_fwd",
							"-s " + model.getServerModel(servers[i]).getBroadcast() + "/30"
							+ " -d " + model.getServerModel(servers[j]).getBroadcast()+  "/30"
							+ " -p tcp"
							+ " --dport " + model.getData().getSSHPort(servers[j])
							+ " -j DROP"));
				}
			}
		}
	
		return units;
	}
	
	
	private Vector<IUnit> deviceIptUnits(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String[] devices = model.getDeviceLabels();
		String[] servers = model.getServerLabels();
		Vector<String> users = new Vector<String>();
		Vector<String> superusers = new Vector<String>();
		Vector<String> intOnly = new Vector<String>();
		Vector<String> extOnly = new Vector<String>();
		
		for (int i = 0; i < devices.length; ++i) {
			switch (model.getDeviceModel(devices[i]).getType()) {
				case "superuser":
					superusers.add(devices[i]);
					users.add(devices[i]);
					break;
				case "user":
					users.add(devices[i]);
					break;
				case "intonly":
					intOnly.add(devices[i]);
					break;
				case "extonly":
					extOnly.add(devices[i]);
					break;
				default:
					//It'll default drop.
			}
		}
		
		//Create user forward chains
		for (int i = 0; i < users.size(); ++i) {
			if (model.getDeviceModel(users.elementAt(i)).getWiredMac() != null) {
				//First force to jump to our chain if wired
				units.addElement(model.getServerModel(server).getFirewallModel().addFilterForward(
						users.elementAt(i).replace("-",  "_") + "_ipt_wired_src",
						"-s " + model.getDeviceModel(users.elementAt(i)).getWiredBroadcast() + "/30 -j " + users.elementAt(i).replace("-",  "_") + "_fwd"));
				units.addElement(model.getServerModel(server).getFirewallModel().addFilterForward(
						users.elementAt(i).replace("-",  "_") + "_ipt_wired_dst",
						"-d " + model.getDeviceModel(users.elementAt(i)).getWiredBroadcast() + "/30 -j " + users.elementAt(i).replace("-",  "_") + "_fwd"));
			}
			//Also if wireless
			if (model.getDeviceModel(users.elementAt(i)).getWirelessMac() != null) {
				units.addElement(model.getServerModel(server).getFirewallModel().addFilterForward(
						users.elementAt(i).replace("-",  "_") + "_ipt_wireless_src",
						"-s " + model.getDeviceModel(users.elementAt(i)).getWirelessBroadcast() + "/30 -j " + users.elementAt(i).replace("-",  "_") + "_fwd"));
				units.addElement(model.getServerModel(server).getFirewallModel().addFilterForward(
						users.elementAt(i).replace("-",  "_") + "_ipt_wireless_dst",
						"-d " + model.getDeviceModel(users.elementAt(i)).getWirelessBroadcast() + "/30 -j " + users.elementAt(i).replace("-",  "_") + "_fwd"));		
			}
			
			//Create our user's egress chain for bandwidth (exfil?) tracking
			//In future, we could perhaps do some form of traffic blocking malarky here?
			units.addElement(model.getServerModel(server).getFirewallModel().addChain("egress_bandwidth_monitor", "filter", users.elementAt(i).replace("-",  "_") + "_egress"));
			//Egress Rules (backwards)
			units.addElement(model.getServerModel(server).getFirewallModel().addFilter("egress_default_drop", users.elementAt(i).replace("-",  "_") + "_egress",
					"-j DROP"));
			//Allow anything passing through the router
			units.addElement(model.getServerModel(server).getFirewallModel().addFilter("allow_egress_traffic", users.elementAt(i).replace("-",  "_") + "_egress",
					"-i " + model.getData().getIface(server) + " -o " + model.getData().getExtIface(server) + " -j ACCEPT"));
			//Mark any connection which has uploaded > 20mb
			units.addElement(model.getServerModel(server).getFirewallModel().addMangleForward("mark_large_downloads", 
					"-o " + model.getData().getExtIface(server) +" -m connbytes --connbytes 20971520: --connbytes-dir both --connbytes-mode bytes -j MARK --set-mark 4"));

			//Create our ingress chain for download bandwidth tracking
			units.addElement(model.getServerModel(server).getFirewallModel().addChain("ingress_bandwidth_monitor", "filter", users.elementAt(i).replace("-",  "_") + "_ingress"));
			//Ingress Rules (backwards)
			units.addElement(model.getServerModel(server).getFirewallModel().addFilter("ingress_default_drop", users.elementAt(i).replace("-",  "_") + "_ingress",
					"-j DROP"));
			//Allow anything passing through the router
			units.addElement(model.getServerModel(server).getFirewallModel().addFilter("allow_ingress_traffic", users.elementAt(i).replace("-",  "_") + "_ingress",
					"-i " + model.getData().getExtIface(server) + " -o " + model.getData().getIface(server) + " -j ACCEPT"));

			
			//Build the user's chain
			units.addElement(model.getServerModel(server).getFirewallModel().addChain("user_fwd_chain_" + users.elementAt(i).replace("-",  "_"), "filter", users.elementAt(i).replace("-",  "_") + "_fwd"));
			
			//Add our rules (backwards(!))
			units.addElement(model.getServerModel(server).getFirewallModel().addFilter("user_default_drop_" + users.elementAt(i).replaceAll("-", "_"), users.elementAt(i).replace("-",  "_") + "_fwd",
					"-j DROP"));
			//Log anything passing through the router, jump to our ingress/egress chains
			units.addElement(model.getServerModel(server).getFirewallModel().addFilter("user_allow_inet_traffic_" + users.elementAt(i).replaceAll("-",  "_"), users.elementAt(i).replace("-",  "_") + "_fwd",
					"-i " + model.getData().getExtIface(server) + " -o " + model.getData().getIface(server) + " -j " + users.elementAt(i).replace("-",  "_") + "_ingress"));
			units.addElement(model.getServerModel(server).getFirewallModel().addFilter("user_allow_inet_traffic_" + users.elementAt(i).replaceAll("-",  "_"), users.elementAt(i).replace("-",  "_") + "_fwd",
					"-i " + model.getData().getIface(server) + " -o " + model.getData().getExtIface(server) + " -j " + users.elementAt(i).replace("-",  "_") + "_egress"));
			units.addElement(model.getServerModel(server).getFirewallModel().addFilter("user_log_inet_traffic_" + users.elementAt(i).replaceAll("-",  "_"), users.elementAt(i).replace("-",  "_") + "_fwd",
					"-j LOG --log-prefix \\\"ipt-" + users.elementAt(i) + ": \\\""));
			
			if (model.getDeviceModel(users.elementAt(i)).getWiredMac() != null) {
				units.addElement(model.getServerModel(server).getFirewallModel().addFilter("user_fwd_router_" + users.elementAt(i).replaceAll("-",  "_"), users.elementAt(i).replace("-",  "_") + "_fwd",
						"-s " + model.getDeviceModel(users.elementAt(i)).getWiredGateway() + " -j ACCEPT"));
			}
			//Also if wireless
			if (model.getDeviceModel(users.elementAt(i)).getWirelessMac() != null) {
				units.addElement(model.getServerModel(server).getFirewallModel().addFilter("user_fwd_router_" + users.elementAt(i).replaceAll("-",  "_"), users.elementAt(i).replace("-",  "_") + "_fwd",
					"-s " + model.getDeviceModel(users.elementAt(i)).getWirelessGateway() + " -j ACCEPT"));
			}
			for (int j = 0; j < intOnly.size(); ++j) {
				if (model.getDeviceModel(users.elementAt(i)).getWiredMac() != null) {
					//Want to be able to talk to/from internal devices
					units.addElement(model.getServerModel(server).getFirewallModel().addFilter("user_fwd_int_only_" + intOnly.elementAt(j).replace("-",  "_"),  users.elementAt(i).replace("-",  "_") + "_fwd",
							"-s " + model.getDeviceModel(users.elementAt(i)).getWiredBroadcast() + "/30 -d " + model.getDeviceModel(intOnly.elementAt(j)).getWiredIP() + " -j ACCEPT"));
					units.addElement(model.getServerModel(server).getFirewallModel().addFilter("user_fwd_int_only_" + intOnly.elementAt(j).replace("-",  "_"),  users.elementAt(i).replace("-",  "_") + "_fwd",
							"-d " + model.getDeviceModel(users.elementAt(i)).getWiredBroadcast() + "/30 -s " + model.getDeviceModel(intOnly.elementAt(j)).getWiredIP() + " -j ACCEPT"));
				}
				//Also if wireless
				if (model.getDeviceModel(users.elementAt(i)).getWirelessMac() != null) {
					units.addElement(model.getServerModel(server).getFirewallModel().addFilter("user_fwd_int_only_" + intOnly.elementAt(j).replace("-",  "_"),  users.elementAt(i).replace("-",  "_") + "_fwd",
							"-s " + model.getDeviceModel(users.elementAt(i)).getWirelessBroadcast() + "/30 -d " + model.getDeviceModel(intOnly.elementAt(j)).getWirelessIP() + " -j ACCEPT"));
					units.addElement(model.getServerModel(server).getFirewallModel().addFilter("user_fwd_int_only_" + intOnly.elementAt(j).replace("-",  "_"),  users.elementAt(i).replace("-",  "_") + "_fwd",
							"-d " + model.getDeviceModel(users.elementAt(i)).getWirelessBroadcast() + "/30 -s " + model.getDeviceModel(intOnly.elementAt(j)).getWirelessIP() + " -j ACCEPT"));
				}
				
			}
			for (int k = 0; k < servers.length; ++k) {
				if (model.getDeviceModel(users.elementAt(i)).getWiredMac() != null) {
					//Want to be able to talk to/from services on our network - but only on ports 80/443
					units.addElement(model.getServerModel(server).getFirewallModel().addFilter("user_fwd_servers_" + servers[k].replace("-",  "_"),  users.elementAt(i).replace("-",  "_") + "_fwd",
							"-s " + model.getDeviceModel(users.elementAt(i)).getWiredBroadcast() + "/30 -d " + model.getServerModel(servers[k]).getIP()
							+ " -p tcp --dport 80 -j ACCEPT"));				
					units.addElement(model.getServerModel(server).getFirewallModel().addFilter("user_fwd_servers_" + servers[k].replace("-",  "_"),  users.elementAt(i).replace("-",  "_") + "_fwd",
							"-s " + model.getDeviceModel(users.elementAt(i)).getWiredBroadcast() + "/30 -d " + model.getServerModel(servers[k]).getIP()
							+ " -p tcp --dport 443 -j ACCEPT"));				
					
					//Unless they're a superuser!  Then they can SSH in :)
					if (superusers.contains(users.elementAt(i))) {
						units.addElement(model.getServerModel(server).getFirewallModel().addFilter("superuser_" + users.elementAt(i).replaceAll("-", "_") + "_fwd_servers_" + servers[k].replace("-",  "_"),  users.elementAt(i).replace("-",  "_") + "_fwd",
								"-s " + model.getDeviceModel(users.elementAt(i)).getWiredBroadcast() + "/30 -d " + model.getServerModel(servers[k]).getIP()
								+ " -p tcp --dport " + model.getData().getSSHPort(servers[k]) + " -j ACCEPT"));										
					}
					
					//And servers can talk back to us
					units.addElement(model.getServerModel(server).getFirewallModel().addFilter("user_fwd_servers_" + servers[k].replace("-",  "_"),  users.elementAt(i).replace("-",  "_") + "_fwd",
							"-d " + model.getDeviceModel(users.elementAt(i)).getWiredBroadcast() + "/30 -s " + model.getServerModel(servers[k]).getIP()
							+ " -j ACCEPT"));
				}
				//Also if wireless
				if (model.getDeviceModel(users.elementAt(i)).getWirelessMac() != null) {
					units.addElement(model.getServerModel(server).getFirewallModel().addFilter("user_fwd_servers_" + servers[k].replace("-",  "_"),  users.elementAt(i).replace("-",  "_") + "_fwd",
							"-s " + model.getDeviceModel(users.elementAt(i)).getWirelessBroadcast() + "/30 -d " + model.getServerModel(servers[k]).getIP()
							+ " -p tcp --dport 80 -j ACCEPT"));				

					units.addElement(model.getServerModel(server).getFirewallModel().addFilter("user_fwd_servers_" + servers[k].replace("-",  "_"),  users.elementAt(i).replace("-",  "_") + "_fwd",
							"-s " + model.getDeviceModel(users.elementAt(i)).getWirelessBroadcast() + "/30 -d " + model.getServerModel(servers[k]).getIP()
							+ " -p tcp --dport 443 -j ACCEPT"));				

					//Unless they're a superuser!  Then they can SSH in :)
					if (superusers.contains(users.elementAt(i))) {
						units.addElement(model.getServerModel(server).getFirewallModel().addFilter("superuser_" + users.elementAt(i).replaceAll("-", "_") + "_fwd_servers_" + servers[k].replace("-",  "_"),  users.elementAt(i).replace("-",  "_") + "_fwd",
								"-s " + model.getDeviceModel(users.elementAt(i)).getWirelessBroadcast() + "/30 -d " + model.getServerModel(servers[k]).getIP()
								+ " -p tcp --dport " + model.getData().getSSHPort(servers[k]) + " -j ACCEPT"));										
					}
					
					//And servers can talk back to us
					units.addElement(model.getServerModel(server).getFirewallModel().addFilter("user_fwd_servers_" + servers[k].replace("-",  "_"),  users.elementAt(i).replace("-",  "_") + "_fwd",
							"-d " + model.getDeviceModel(users.elementAt(i)).getWirelessBroadcast() + "/30 -s " + model.getServerModel(servers[k]).getIP()
							+ " -j ACCEPT"));
				}
			}
		}
		
		//Create internal only forward chains
		for (int i = 0; i < intOnly.size(); ++i) {
			//First force to jump to our chain
			if (model.getDeviceModel(intOnly.elementAt(i)).getWiredMac() != null) {
				units.addElement(model.getServerModel(server).getFirewallModel().addFilterForward(
						intOnly.elementAt(i).replace("-",  "_") + "_ipt_src",
						"-s " + model.getDeviceModel(intOnly.elementAt(i)).getWiredIP() + " -j " + intOnly.elementAt(i).replace("-",  "_") + "_fwd"));
				units.addElement(model.getServerModel(server).getFirewallModel().addFilterForward(
						intOnly.elementAt(i).replace("-",  "_") + "_ipt_dst",
						"-d " + model.getDeviceModel(intOnly.elementAt(i)).getWiredIP() + " -j " + intOnly.elementAt(i).replace("-",  "_") + "_fwd"));
			}
			if (model.getDeviceModel(intOnly.elementAt(i)).getWirelessMac() != null) {
				units.addElement(model.getServerModel(server).getFirewallModel().addFilterForward(
						intOnly.elementAt(i).replace("-",  "_") + "_ipt_src",
						"-s " + model.getDeviceModel(intOnly.elementAt(i)).getWirelessIP() + " -j " + intOnly.elementAt(i).replace("-",  "_") + "_fwd"));
				units.addElement(model.getServerModel(server).getFirewallModel().addFilterForward(
						intOnly.elementAt(i).replace("-",  "_") + "_ipt_dst",
						"-d " + model.getDeviceModel(intOnly.elementAt(i)).getWirelessIP() + " -j " + intOnly.elementAt(i).replace("-",  "_") + "_fwd"));
			}
			//Build the chain
			units.addElement(model.getServerModel(server).getFirewallModel().addChain("int_only_fwd_chain" + intOnly.elementAt(i).replace("-",  "_"), "filter", intOnly.elementAt(i).replace("-",  "_") + "_fwd"));
			
			//Drop anything else
			units.addElement(model.getServerModel(server).getFirewallModel().addFilter("int_only_fwd_drop_" + intOnly.elementAt(i).replace("-",  "_"), intOnly.elementAt(i).replace("-",  "_") + "_fwd",
					"-j DROP"));
			
			if (model.getDeviceModel(intOnly.elementAt(i)).getWiredMac() != null) {
				units.addElement(model.getServerModel(server).getFirewallModel().addFilter("int_only_fwd_router_" + intOnly.elementAt(i).replaceAll("-",  "_"), intOnly.elementAt(i).replace("-",  "_") + "_fwd",
						"-s " + model.getDeviceModel(users.elementAt(i)).getWiredGateway() + " -j ACCEPT"));
			}
			if (model.getDeviceModel(intOnly.elementAt(i)).getWirelessMac() != null) {
				units.addElement(model.getServerModel(server).getFirewallModel().addFilter("int_only_fwd_router_" + intOnly.elementAt(i).replaceAll("-",  "_"), intOnly.elementAt(i).replace("-",  "_") + "_fwd",
						"-s " + model.getDeviceModel(users.elementAt(i)).getWirelessGateway() + " -j ACCEPT"));				
			}
			for (int j = 0; j < users.size(); ++j) {
				//Want to accept all traffic from users in
				if (model.getDeviceModel(users.elementAt(j)).getWiredMac() != null) {
					if (model.getDeviceModel(intOnly.elementAt(i)).getWiredMac() != null) { //Wired users can get at the wired interface
						units.addElement(model.getServerModel(server).getFirewallModel().addFilter("int_only_fwd_accept_in_" + intOnly.elementAt(i).replace("-",  "_"), intOnly.elementAt(i).replace("-",  "_") + "_fwd",
								"-s " + model.getDeviceModel(users.elementAt(j)).getWiredBroadcast() + "/30 -d " + model.getDeviceModel(intOnly.elementAt(i)).getWiredIP() + " -j ACCEPT"));
						//Only established connections out
						units.addElement(model.getServerModel(server).getFirewallModel().addFilter("int_only_fwd_accept_out_" + intOnly.elementAt(i).replace("-",  "_"), intOnly.elementAt(i).replace("-",  "_") + "_fwd",
								"-d " + model.getDeviceModel(users.elementAt(j)).getWiredBroadcast() + "/30 -s " + model.getDeviceModel(intOnly.elementAt(i)).getWiredIP() + " -m state --state ESTABLISHED,RELATED -j ACCEPT"));
					}
					if (model.getDeviceModel(intOnly.elementAt(i)).getWirelessMac() != null) { //Wired users can get at the wireless interface
						units.addElement(model.getServerModel(server).getFirewallModel().addFilter("int_only_fwd_accept_in_" + intOnly.elementAt(i).replace("-",  "_"), intOnly.elementAt(i).replace("-",  "_") + "_fwd",
								"-s " + model.getDeviceModel(users.elementAt(j)).getWiredBroadcast() + "/30 -d " + model.getDeviceModel(intOnly.elementAt(i)).getWirelessIP() + " -j ACCEPT"));
						//Only established connections out
						units.addElement(model.getServerModel(server).getFirewallModel().addFilter("int_only_fwd_accept_out_" + intOnly.elementAt(i).replace("-",  "_"), intOnly.elementAt(i).replace("-",  "_") + "_fwd",
								"-d " + model.getDeviceModel(users.elementAt(j)).getWiredBroadcast() + "/30 -s " + model.getDeviceModel(intOnly.elementAt(i)).getWirelessIP() + " -m state --state ESTABLISHED,RELATED -j ACCEPT"));
					}				
				}
				if (model.getDeviceModel(users.elementAt(j)).getWirelessMac() != null) {
					if (model.getDeviceModel(intOnly.elementAt(i)).getWiredMac() != null) { //Wireless users can get at the wired interface
						units.addElement(model.getServerModel(server).getFirewallModel().addFilter("int_only_fwd_accept_in_" + intOnly.elementAt(i).replace("-",  "_"), intOnly.elementAt(i).replace("-",  "_") + "_fwd",
								"-s " + model.getDeviceModel(users.elementAt(j)).getWirelessBroadcast() + "/30 -d " + model.getDeviceModel(intOnly.elementAt(i)).getWiredIP() + " -j ACCEPT"));
						//Only established connections out
						units.addElement(model.getServerModel(server).getFirewallModel().addFilter("int_only_fwd_accept_out_" + intOnly.elementAt(i).replace("-",  "_"), intOnly.elementAt(i).replace("-",  "_") + "_fwd",
								"-d " + model.getDeviceModel(users.elementAt(j)).getWirelessBroadcast() + "/30 -s " + model.getDeviceModel(intOnly.elementAt(i)).getWiredIP() + " -m state --state ESTABLISHED,RELATED -j ACCEPT"));
					}
					if (model.getDeviceModel(intOnly.elementAt(i)).getWirelessMac() != null) { //Wireless users can get at the wireless interface
						units.addElement(model.getServerModel(server).getFirewallModel().addFilter("int_only_fwd_accept_in_" + intOnly.elementAt(i).replace("-",  "_"), intOnly.elementAt(i).replace("-",  "_") + "_fwd",
								"-s " + model.getDeviceModel(users.elementAt(j)).getWirelessBroadcast() + "/30 -d " + model.getDeviceModel(intOnly.elementAt(i)).getWirelessIP() + " -j ACCEPT"));
						//Only established connections out
						units.addElement(model.getServerModel(server).getFirewallModel().addFilter("int_only_fwd_accept_out_" + intOnly.elementAt(i).replace("-",  "_"), intOnly.elementAt(i).replace("-",  "_") + "_fwd",
								"-d " + model.getDeviceModel(users.elementAt(j)).getWirelessBroadcast() + "/30 -s " + model.getDeviceModel(intOnly.elementAt(i)).getWirelessIP() + " -m state --state ESTABLISHED,RELATED -j ACCEPT"));
					}	
				}
			}

		}
		//Create external only forward chains
		for (int i = 0; i < extOnly.size(); ++i) {
			//First force to jump to our chain
			units.addElement(model.getServerModel(server).getFirewallModel().addFilterForward(
					extOnly.elementAt(i).replace("-",  "_") + "_ipt_src",
					"-s " + model.getDeviceModel(extOnly.elementAt(i)).getWiredIP() + " -j " + extOnly.elementAt(i).replace("-",  "_") + "_fwd"));
			units.addElement(model.getServerModel(server).getFirewallModel().addFilterForward(
					extOnly.elementAt(i).replace("-",  "_") + "_ipt_dst",
					"-d " + model.getDeviceModel(extOnly.elementAt(i)).getWiredIP() + " -j " + extOnly.elementAt(i).replace("-",  "_") + "_fwd"));
			
			//Build the chain
			units.addElement(model.getServerModel(server).getFirewallModel().addChain("ext_only_fwd_chain" + extOnly.elementAt(i).replace("-",  "_"), "filter", extOnly.elementAt(i).replace("-",  "_") + "_fwd"));
			//Drop anything else
			units.addElement(model.getServerModel(server).getFirewallModel().addFilter("ext_only_fwd_drop_" + extOnly.elementAt(i).replace("-",  "_"), extOnly.elementAt(i).replace("-",  "_") + "_fwd",
					"-j DROP"));
			units.addElement(model.getServerModel(server).getFirewallModel().addFilter("ext_only_allow_inet_traffic_" + extOnly.elementAt(i).replaceAll("-",  "_"), extOnly.elementAt(i).replace("-",  "_") + "_fwd",
					"-i " + model.getData().getExtIface(server) + " -o " + model.getData().getIface(server) + " -j ACCEPT"));
			units.addElement(model.getServerModel(server).getFirewallModel().addFilter("ext_only_allow_inet_traffic_" + extOnly.elementAt(i).replaceAll("-",  "_"), extOnly.elementAt(i).replace("-",  "_") + "_fwd",
					"-i " + model.getData().getIface(server) + " -o " + model.getData().getExtIface(server) + " -j ACCEPT"));

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
