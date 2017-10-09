package profile;

import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;

public class DNS extends AStructuredProfile {

	HashMap<String, Vector<String>> serverDomains;
	
	public DNS() {
		super("dns");
		
		serverDomains = new HashMap<String, Vector<String>>();
	}

	public Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		getServerDomains(model);
		
		//Config taken from https://calomel.org/unbound_dns.html
		String config = "";
		config += "server:\n";
		config += "    verbosity: 1\n"; //Log verbosity
		config += "    include: \\\"/etc/unbound/unbound.conf.d/interfaces.conf\\\"\n";
		config += "    port: 53\n";
		config += "    do-ip4: yes\n";
		config += "    do-ip6: no\n";
		config += "    do-udp: yes\n";
		config += "    do-tcp: yes\n";
		config += "    access-control: 10.0.0.0/8 allow\n";
		config += "    access-control: 127.0.0.0/8 allow\n";
		config += "    access-control: 192.168.0.0/16 allow\n";
		config += "    access-control: 172.16.0.0/12 allow\n";
		config += "    hide-identity: yes\n";
		config += "    hide-version: yes\n";
		config += "    harden-glue: yes\n";
		config += "    harden-dnssec-stripped: yes\n";
		config += "    use-caps-for-id: yes\n";
		config += "    cache-min-ttl: 3600\n";
		config += "    cache-max-ttl: 86400\n";
		config += "    prefetch: yes\n";
		config += "    num-threads: " + model.getData().getCpus(server) + "\n";
		config += "    msg-cache-slabs: " + (Integer.parseInt(model.getData().getCpus(server))*2) + "\n";
		config += "    rrset-cache-slabs: " + (Integer.parseInt(model.getData().getCpus(server))*2) + "\n";
		config += "    infra-cache-slabs: " + (Integer.parseInt(model.getData().getCpus(server))*2) + "\n";
		config += "    key-cache-slabs: " + (Integer.parseInt(model.getData().getCpus(server))*2) + "\n";
		config += "    rrset-cache-size: " + (Integer.parseInt(model.getData().getRam(server))/4) + "m\n";
		config += "    msg-cache-size: " + (Integer.parseInt(model.getData().getRam(server))/8) + "m\n";
		config += "    so-rcvbuf: 1m\n";
		config += "    private-address: 192.168.0.0/16\n";
		config += "    private-address: 172.16.0.0/12\n";
		config += "    private-address: 10.0.0.0/8\n";
		for (String domain : serverDomains.keySet()) {
			config += "    private-domain: \\\"" + domain + "\\\"\n";
		}
		config += "    unwanted-reply-threshold: 10000\n";
		config += "    do-not-query-localhost: yes\n";
		config += "    val-clean-additional: yes\n";

		if (model.getData().getAdBlocking()) {
			config += "    local-zone: \\\"doubleclick.net\\\" redirect\n";
			config += "    local-data: \\\"doubleclick.net A 127.0.0.1\\\"\n";
			config += "    local-zone: \\\"googlesyndication.com\\\" redirect\n";
			config += "    local-data: \\\"googlesyndication.com A 127.0.0.1\\\"\n";
			config += "    local-zone: \\\"googleadservices.com\\\" redirect\n";
			config += "    local-data: \\\"googleadservices.com A 127.0.0.1\\\"\n";
			config += "    local-zone: \\\"google-analytics.com\\\" redirect\n";
			config += "    local-data: \\\"google-analytics.com A 127.0.0.1\\\"\n";
			config += "    local-zone: \\\"ads.youtube.com\\\" redirect\n";
			config += "    local-data: \\\"ads.youtube.com A 127.0.0.1\\\"\n";
			config += "    local-zone: \\\"adserver.yahoo.com\\\" redirect\n";
			config += "    local-data: \\\"adserver.yahoo.com A 127.0.0.1\\\"\n";
			config += "    local-zone: \\\"ask.com\\\" redirect\n";
			config += "    local-data: \\\"ask.com A 127.0.0.1\\\"\n";
		}
		for (String domain : serverDomains.keySet()) {
			config += "    include: \\\"/etc/unbound/unbound.conf.d/" + domain + ".zone\\\"\n";
		}
		//rDNS
		config += "    local-zone: \\\"" + model.getServerModel(server).getGateway().split("\\.")[0] + ".in-addr.arpa.\\\" nodefault\n";
		config += "    stub-zone:\n";
		config += "        name: \\\"" + model.getServerModel(server).getGateway().split("\\.")[0] + ".in-addr.arpa.\\\"\n";
		config += "        stub-addr: " + model.getServerModel(server).getGateway() + "\n";
		config += "    forward-zone:\n";
		config += "        name: \\\".\\\"\n";
		config += "        forward-addr: " + model.getData().getDNS();
		
		units.addElement(new FileUnit("dns_persistent_config", "dns_installed", config, "/etc/unbound/unbound.conf"));
		
		return units;
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new InstalledUnit("dns", "unbound"));

		model.getServerModel(server).getUserModel().addUsername("unbound");
		
//		String resolv = "";
//		resolv += "domain " + model.getData().getDomain() + "\n";
//		resolv += "search " + model.getData().getDomain() + "\n";
//		resolv += "nameserver " + model.getData().getDNS();
		
//		units.addElement(new FileUnit("persistent_resolv_config", "dns_installed", resolv, "/etc/resolv.conf"));

		model.getServerModel(server).getProcessModel().addProcess("/usr/sbin/unbound$");

		return units;
	}
	
	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(model.getServerModel(server).getFirewallModel().addFilterInput("dns_ipt_in_udp",
				"-i " + model.getData().getIface(server) + " -p udp --dport 53 -j ACCEPT"));
		units.addElement(model.getServerModel(server).getFirewallModel().addFilterOutput("dns_ipt_out_udp",
				"-o " + model.getData().getIface(server) + " -p udp --sport 53 -j ACCEPT"));
		units.addElement(model.getServerModel(server).getFirewallModel().addFilterInput("dns_ipt_in_tcp",
				"-i " + model.getData().getIface(server) + " -p tcp --dport 53 -j ACCEPT"));
		units.addElement(model.getServerModel(server).getFirewallModel().addFilterOutput("dns_ipt_out_tcp",
				"-o " + model.getData().getIface(server) + " -p tcp --sport 53 -j ACCEPT"));
		units.addElement(model.getServerModel(server).getFirewallModel().addFilterOutput("dns_ipt_out_tcp_lo",
				"-o " + model.getData().getIface(server) + " -p tcp --dport 953 -j ACCEPT"));
		units.addElement(model.getServerModel(server).getFirewallModel().addChain("dns_ipt_chain", "filter", "dnsd"));
		units.addElement(model.getServerModel(server).getFirewallModel().addFilter("dns_ext", "dnsd", "-j DROP"));
		units.addElement(model.getServerModel(server).getFirewallModel().addFilter("dns_ext_log", "dnsd",
				"-j LOG --log-prefix \\\"ipt-dnsd: \\\""));
		units.addElement(model.getServerModel(server).getFirewallModel().addFilterInput("dns_ext_in",
				"-i " + model.getData().getExtIface(server) + " -p udp --sport 53 -j dnsd"));
		units.addElement(model.getServerModel(server).getFirewallModel().addFilterOutput("dns_ext_out",
				"-o " + model.getData().getExtIface(server) + " -p udp --sport 53 -j dnsd"));
		
		int count = 1;
		StringTokenizer str = new StringTokenizer(model.getData().getDNS());
		while (str.hasMoreTokens()) {
			String ip = str.nextToken(";");
			units.addElement(model.getServerModel(server).getFirewallModel().addFilter("dns_ext_server_in_" + count,
					"dnsd", "-i " + model.getData().getExtIface(server) + " -s " + ip + " -p udp --sport 53 -j ACCEPT"));
			units.addElement(model.getServerModel(server).getFirewallModel().addFilter("dns_ext_server_out_" + count,
					"dnsd", "-o " + model.getData().getExtIface(server) + " -d " + ip + " -p udp --dport 53 -j ACCEPT"));
			count++;
		}

		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new RunningUnit("dns", "unbound", "unbound"));
		
		String[] servers = model.getServerLabels();
		String[] devices = model.getDeviceLabels();

		String ifaceConfig = "";
		for (int i = 0; i < servers.length; ++i) {
			ifaceConfig += "    interface: " + model.getServerModel(servers[i]).getGateway() + "\n";
		}
		for (int i = 0; i < devices.length; ++i) {
			String[] gateways = model.getDeviceModel(devices[i]).getGateways();

			for (int j = 0; j < gateways.length; ++j) {
				ifaceConfig += "    interface: " + gateways[j] + "\n";
			}
		}
		units.addElement(new FileUnit("dns_listening_interfaces", "dns_installed", ifaceConfig.replaceAll("\\s+$", ""), "/etc/unbound/unbound.conf.d/interfaces.conf"));

		for (String domain : serverDomains.keySet()) {
			String zoneConfig = "";
			zoneConfig += "    local-zone: \\\"" + domain + ".\\\" transparent";
	
			Vector<String> srvs = serverDomains.get(domain);
			
			//Forward DNS
			for (int i = 0; i < srvs.size(); ++i) {
				String hostname = model.getData().getHostname(srvs.elementAt(i));
				String ip       = model.getServerModel(srvs.elementAt(i)).getIP();
				String gateway  = model.getServerModel(srvs.elementAt(i)).getGateway();
				String subnet   = model.getData().getSubnet(srvs.elementAt(i));
				
//				if (!model.getServerModel(servers[i]).isRouter()) {
					zoneConfig += "\n";
					zoneConfig += "    local-data: \\\"" + hostname + " A " + ip +"\\\"\n";
					zoneConfig += "    local-data: \\\"" + hostname + "." + domain + " A " + ip +"\\\"\n";
					zoneConfig += "    local-data-ptr: \\\"" + ip + " " + hostname + "." + domain + "\\\"\n";
					zoneConfig += "    local-data-ptr: \\\"" + gateway + " router" + subnet + "." + domain + "\\\"";
					//CNAMEs - more like ALIASES
					if (model.getData().getCnames(srvs.elementAt(i)) != null) {
						for (int j = 0; j < model.getData().getCnames(srvs.elementAt(i)).length; ++j) {
							zoneConfig += "\n";
							zoneConfig += (model.getData().getCnames(srvs.elementAt(i))[j].equals("")) ? "" : "    local-data: \\\"" + model.getData().getCnames(srvs.elementAt(i))[j] + " A " + ip +"\\\"\n";
	                        zoneConfig += "    local-data: \\\"";
							zoneConfig += (model.getData().getCnames(srvs.elementAt(i))[j].equals("")) ? "" :  model.getData().getCnames(srvs.elementAt(i))[j] + ".";
							zoneConfig += domain + " A " + ip +"\\\"";
						}
					}
//				}
			}
			//If this domain is the same as our router's, add devicen to it
			if (domain.equals(model.getData().getDomain(model.getRouters().elementAt(0)))) {
				for (int i = 0; i < devices.length; ++i) {
					String cleanName  = devices[i].replaceAll("_", "-");
					String[] ips      = model.getDeviceModel(devices[i]).getIPs();
					String[] gateways = model.getDeviceModel(devices[i]).getGateways();
					
					for (int j = 0; j < ips.length; ++j) {
						zoneConfig += "\n";
						zoneConfig += "    local-data: \\\"" + cleanName + "." + j + " A " + ips[j] +"\\\"\n";
						zoneConfig += "    local-data: \\\"" + cleanName + "." + j + "." + domain + " A " + ips[j] +"\\\"\n";
						zoneConfig += "    local-data-ptr: \\\"" + ips[j] + " " + cleanName + "." + j + "." + domain + "\\\"\n";
						zoneConfig += "    local-data-ptr: \\\"" + gateways[j] + " router." + j + "." + cleanName + "." + domain + "\\\"";
					}
				}		
			}
			
			units.addElement(new FileUnit(domain.replaceAll("\\.", "_").replaceAll("-",  "_") + "_dns_internal_zone", "dns_installed", zoneConfig.replaceAll("\\s+$", ""), "/etc/unbound/unbound.conf.d/" + domain + ".zone"));
		}
		
		return units;
	}
	
	private void getServerDomains(NetworkModel model) {
		String[] servers = model.getServerLabels();
		
		for (int i = 0; i < servers.length; ++i) {
			String domain = model.getData().getDomain(servers[i]);
			
			Vector<String> srvs = serverDomains.get(domain);
			
			if (srvs == null) {
				serverDomains.put(domain, new Vector<String>());
			}
			srvs = serverDomains.get(domain);
			srvs.add(servers[i]);
			
			serverDomains.put(domain,  srvs);
		}
	}
	
}
