package profile;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import core.iface.IUnit;
import core.model.FirewallModel;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;

public class DNS extends AStructuredProfile {

	private Vector<String> gateways;
	private HashMap<String, Vector<String>> domainRecords;
	private HashMap<String, String> poison;
	
	private boolean useDtls;
	
	private String invalidChars;
	
	public DNS() {
		super("dns");
		
		domainRecords = new HashMap<String, Vector<String>>();
		gateways      = new Vector<String>();
		poison        = new HashMap<String, String>();
		
		invalidChars = "[^\\-a-zA-Z0-9]";
	}

	private void addGateway(String ip) {
		if (!gateways.contains(ip) ) {
			gateways.add(ip);
		}
	}
	
	public void addDomainRecord(String domain, String gatewayIp, String[] subdomains, String ip) {
		this.addGateway(gatewayIp);
		
		Vector<String> records = domainRecords.get(domain);
		
		if (records == null) {
			domainRecords.put(domain, new Vector<String>());
		}
		records = domainRecords.get(domain);

		//subdomains[0] *should always* be the canonical hostname...
		records.addElement("    local-data-ptr: \\\"" + ip + " " + subdomains[0].replaceAll(invalidChars, "-") + "." + domain + "\\\"");
		records.addElement("    local-data-ptr: \\\"" + gatewayIp + " router." + subdomains[0].replaceAll(invalidChars, "-") + "." + domain + "\\\"");

		for (String subdomain : subdomains) {
			//If you're trying to have a cname which is just the domain, it craps out unless you do this...
			if (!subdomain.equals("")) {
				subdomain = subdomain.replaceAll(invalidChars, "-");

				records.addElement("    local-data: \\\"" + subdomain + " A " + ip + "\\\"");
				records.addElement("    local-data: \\\"" + subdomain + "." + domain + " A " + ip + "\\\"");
			}
			else {
				records.addElement("    local-data: \\\"" + domain + " A " + ip + "\\\"");
			}
		}
		
		domainRecords.put(domain,  records);
	}
	
	public void addPoison(String domain, String ip) {
		poison.put(domain, ip);
	}
	
	public Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		useDtls       = model.getData().getDTLS();
		
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new SimpleUnit("dns_custom_zone", "unbound_installed",
				"sudo touch /etc/unbound/unbound.conf.d/custom.zone",
				"[ -f /etc/unbound/unbound.conf.d/custom.zone ] && echo pass || echo fail", "pass", "pass"));
		
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
		config += "    access-control: 127.0.0.0/8 allow\n";
		config += "    access-control: 10.0.0.0/8 allow\n";
		config += "    access-control: 172.16.0.0/12 allow\n";
		config += "    access-control: 192.168.0.0/16 allow\n";
		config += "    access-control: 0.0.0.0/0 refuse\n";
		config += "    access-control: " + model.getData().getIP() + "/32 allow\n";
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
		config += "    private-address: 10.0.0.0/8\n";
		config += "    private-address: 172.16.0.0/12\n";
		config += "    private-address: 192.168.0.0/16\n";
		for (String domain : domainRecords.keySet()) {
			config += "    private-domain: \\\"" + domain + "\\\"\n";
		}
		config += "    unwanted-reply-threshold: 10000\n";
		config += "    do-not-query-localhost: no\n";
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
		for (String domain : domainRecords.keySet()) {
			config += "    include: \\\"/etc/unbound/unbound.conf.d/" + domain + ".zone\\\"\n";
		}
		if (!poison.isEmpty()) {
			config += "    include: \\\"/etc/unbound/unbound.conf.d/poison.zone\\\"\n";
		}
		config += "    include: \\\"/etc/unbound/unbound.conf.d/custom.zone\\\"\n";
		//rDNS
		config += "    local-zone: \\\"" + model.getServerModel(server).getGateway().split("\\.")[0] + ".in-addr.arpa.\\\" nodefault\n";
		config += "    stub-zone:\n";
		config += "        name: \\\"" + model.getServerModel(server).getGateway().split("\\.")[0] + ".in-addr.arpa.\\\"\n";
		config += "        stub-addr: " + model.getServerModel(server).getGateway() + "\n";
		//External DNS servers
		config += "    forward-zone:\n";
		config += "        name: \\\".\\\"";
		//Is our upstream TLS?
		config += (useDtls) ? "\n        forward-ssl-upstream: yes" : "";
		for (String upstream : model.getData().getDNS()) {
			config += "\n        forward-addr: " + upstream;
			//Over TLS?
			config += (useDtls) ? "@853" : "";
		}
			
		units.addElement(model.getServerModel(server).getConfigsModel().addConfigFile("dns_persistent", "dns_installed", config, "/etc/unbound/unbound.conf"));
		
		return units;
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new InstalledUnit("dns", "unbound"));

		model.getServerModel(server).getUserModel().addUsername("unbound");
		model.getServerModel(server).getProcessModel().addProcess("/usr/sbin/unbound -d$");

		return units;
	}
	
	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		Vector<String> userIfaces = new Vector<String>();
		
		FirewallModel fm = model.getServerModel(server).getFirewallModel();
		
		int dnsPort = (useDtls) ? 853 : 53;
		
		fm.addFilterInput("dns_ipt_in_udp",	"-p udp --dport " + dnsPort + " -j ACCEPT");
		fm.addFilterOutput("dns_ipt_out_udp", "-p udp --sport " + dnsPort + " -j ACCEPT");
		fm.addFilterInput("dns_ipt_in_tcp", "-p tcp --dport " + dnsPort + " -j ACCEPT");
		fm.addFilterOutput("dns_ipt_out_tcp", "-p tcp --sport " + dnsPort + " -j ACCEPT");
		fm.addFilterOutput("dns_ipt_out_tcp_lo", "-p tcp --dport " + dnsPort + " -j ACCEPT");
		fm.addChain("dns_ipt_chain", "filter", "dnsd");
		fm.addFilter("dns_ext", "dnsd", "-j DROP");
		fm.addFilter("dns_ext_log", "dnsd",	"-j LOG --log-prefix \\\"ipt-dnsd: \\\"");
		fm.addFilterInput("dns_ext_in",	"-p udp --sport " + dnsPort + " -j dnsd");
		fm.addFilterOutput("dns_ext_out", "-p udp --sport " + dnsPort + " -j dnsd");
		
		for (String upstream : model.getData().getDNS()) {
			fm.addFilter("dns_ext_server_" + upstream.replaceAll("\\.", "_") + "_in", "dnsd",
					"-s " + upstream
					+ " -p udp"
					+ " --sport " + dnsPort
					+ " -j ACCEPT");
			fm.addFilter("dns_ext_server_" + upstream.replaceAll("\\.", "_") + "_out", "dnsd",
					"-d " + upstream
					+ " -p udp"
					+ " --dport " + dnsPort
					+ " -j ACCEPT");
		}

		fm.addFilter("dns_allow_loopback_in", "dnsd", "-i lo -j ACCEPT");
		fm.addFilter("dns_allow_loopback_out", "dnsd", "-o lo -j ACCEPT");

		if (!model.getServerModel(server).isMetal()) {
			userIfaces.addElement(model.getData().getIface(server) + ":2+");
			if (!model.getData().getVpnOnly()) {
				userIfaces.addElement(model.getData().getIface(server) + ":1+");
			}
		}
		else {
			userIfaces.addElement("br+");
		}
		
		for (String iface : userIfaces) {
			fm.addFilter("dns_allow_in", "dnsd", "-i " + iface + " -j ACCEPT");
			fm.addFilter("dns_allow_bridges_out", "dnsd", "-o " + iface + " -j ACCEPT");
		}
		
		fm.addFilter("dns_allow_in", "dnsd", "-i " + model.getData().getIface(server) + " -j ACCEPT");
		fm.addFilter("dns_allow_out", "dnsd", "-o " + model.getData().getIface(server) + " -j ACCEPT");
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new RunningUnit("dns", "unbound", "unbound"));
		
		String ifaceConfig = "";
		
		ifaceConfig += "    interface: 127.0.0.1\n";
		
		for (String gateway : this.gateways) {
			ifaceConfig += "    interface: " + gateway + "\n";
		}
		
		units.addElement(model.getServerModel(server).getConfigsModel().addConfigFile("dns_listening_faces", "dns_installed", ifaceConfig.replaceAll("\\s+$", ""), "/etc/unbound/unbound.conf.d/interfaces.conf"));

		for (String domain : domainRecords.keySet()) {
			String zoneConfig = "";
			zoneConfig += "    local-zone: \\\"" + domain + ".\\\" transparent";
	
			Vector<String> records = domainRecords.get(domain);
			
			for (String record : records) {
				zoneConfig += "\n";
				zoneConfig += record;
			}
			
			units.addElement(model.getServerModel(server).getConfigsModel().addConfigFile(domain.replaceAll("\\.", "_").replaceAll("-",  "_") + "_dns_internal_zone", "dns_installed", zoneConfig.replaceAll("\\s+$", ""), "/etc/unbound/unbound.conf.d/" + domain + ".zone"));
		}
		
		if (!poison.isEmpty()) {
			String poisonConfig = "";
			for (Map.Entry<String, String> record : poison.entrySet()) {
				poisonConfig += "\n    local-zone: \\\"" + record.getKey() + "\\\" redirect";
				poisonConfig += "\n    local-data: \\\"" + record.getKey() + " A " + record.getValue() + "\\\"";
			}
			units.addElement(model.getServerModel(server).getConfigsModel().addConfigFile("dns_poison_zone", "dns_installed", poisonConfig.replaceAll("\\s+$", ""), "/etc/unbound/unbound.conf.d/poison.zone"));
		}
		
		return units;
	}

}
