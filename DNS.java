package profile;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import core.iface.IUnit;
import core.model.FirewallModel;
import core.model.NetworkModel;
import core.model.ServerModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.CustomFileUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;

public class DNS extends AStructuredProfile {

	private Vector<InetAddress> gateways;
	private HashMap<String, Vector<String>> domainRecords;
	private HashMap<String, InetAddress> poison;
	
	private boolean useDtls;
	
	private String invalidChars;
	
	public DNS(ServerModel me, NetworkModel networkModel) {
		super("dns", me, networkModel);
		
		domainRecords = new HashMap<String, Vector<String>>();
		gateways      = new Vector<InetAddress>();
		poison        = new HashMap<String, InetAddress>();

		useDtls = networkModel.getData().getDTLS();

		invalidChars = "[^\\.\\-a-zA-Z0-9]";
	}

	private void addGateway(InetAddress gatewayIp) {
		if (!gateways.contains(gatewayIp) ) {
			gateways.add(gatewayIp);
		}
	}
	
	public void addDomainRecord(String domain, InetAddress gatewayIp, String[] subdomains, InetAddress serverIP) {
		this.addGateway(gatewayIp);
		
		Vector<String> records = domainRecords.get(domain);
		
		if (records == null) {
			domainRecords.put(domain, new Vector<String>());
		}
		records = domainRecords.get(domain);

		//subdomains[0] *should always* be the canonical hostname...
		records.addElement("    local-data-ptr: \\\"" + serverIP.getHostAddress() + " " + subdomains[0].replaceAll(invalidChars, "-") + "." + domain + "\\\"");
		records.addElement("    local-data-ptr: \\\"" + gatewayIp.getHostAddress() + " router." + subdomains[0].replaceAll(invalidChars, "-") + "." + domain + "\\\"");

		for (String subdomain : subdomains) {
			//If you're trying to have a cname which is just the domain, it craps out unless you do this...
			if (!subdomain.equals("")) {
				subdomain = subdomain.replaceAll(invalidChars, "-");

				records.addElement("    local-data: \\\"" + subdomain + " A " + serverIP.getHostAddress() + "\\\"");
				records.addElement("    local-data: \\\"" + subdomain + "." + domain + " A " + serverIP.getHostAddress() + "\\\"");
			}
			else {
				records.addElement("    local-data: \\\"" + domain + " A " + serverIP.getHostAddress() + "\\\"");
			}
		}
		
		domainRecords.put(domain,  records);
	}
	
	//public void addPoison(String domain, InetAddress ip) {
	//	poison.put(domain, ip);
	//}
	
	public Vector<IUnit> getPersistentConfig() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		Integer cpus = networkModel.getData().getCpus(me.getLabel());
		
		units.addElement(new CustomFileUnit("dns_custom_zone", "dns_installed", "/etc/unbound/unbound.conf.d/custom.zone"));
		
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
		config += "    access-control: " + networkModel.getData().getIP().getHostAddress() + "/32 allow\n";
		config += "    hide-identity: yes\n";
		config += "    hide-version: yes\n";
		config += "    harden-glue: yes\n";
		config += "    harden-dnssec-stripped: yes\n";
		config += "    use-caps-for-id: yes\n";
		config += "    cache-min-ttl: 3600\n";
		config += "    cache-max-ttl: 86400\n";
		config += "    prefetch: yes\n";
		config += "    num-threads: " + cpus + "\n";
		config += "    msg-cache-slabs: " + (cpus * 2) + "\n";
		config += "    rrset-cache-slabs: " + (cpus * 2) + "\n";
		config += "    infra-cache-slabs: " + (cpus * 2) + "\n";
		config += "    key-cache-slabs: " + (cpus * 2) + "\n";
		config += "    rrset-cache-size: " + (cpus / 4) + "m\n";
		config += "    msg-cache-size: " + (cpus / 8) + "m\n";
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

		if (networkModel.getData().getAdBlocking()) {
			config += "    include: \\\"/etc/unbound/unbound.conf.d/adblock.zone\\\"\n";
		}
		for (String domain : domainRecords.keySet()) {
			config += "    include: \\\"/etc/unbound/unbound.conf.d/" + domain + ".zone\\\"\n";
		}
		if (!poison.isEmpty()) {
			config += "    include: \\\"/etc/unbound/unbound.conf.d/poison.zone\\\"\n";
		}

		config += "    include: \\\"/etc/unbound/unbound.conf.d/custom.zone\\\"\n";
		//rDNS
		config += "    local-zone: \\\"10.in-addr.arpa.\\\" nodefault\n";
		config += "    stub-zone:\n";
		config += "        name: \\\"10.in-addr.arpa.\\\"\n";
		config += "        stub-addr: 10.0.0.1\n";
		//External DNS servers
		config += "    forward-zone:\n";
		config += "        name: \\\".\\\"";
		//Is our upstream TLS?
		config += (useDtls) ? "\n        forward-ssl-upstream: yes" : "";
		for (InetAddress upstream : networkModel.getData().getDNS()) {
			config += "\n        forward-addr: " + upstream.getHostAddress();
			//Over TLS?
			config += (useDtls) ? "@853" : "";
		}
			
		units.addElement(((ServerModel)me).getConfigsModel().addConfigFile("dns_persistent", "dns_installed", config, "/etc/unbound/unbound.conf"));
		
		return units;
	}

	protected Vector<IUnit> getInstalled() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new InstalledUnit("dns", "unbound"));

		((ServerModel)me).getUserModel().addUsername("unbound");
		((ServerModel)me).getProcessModel().addProcess("/usr/sbin/unbound -d$");

		return units;
	}
	
	public Vector<IUnit> getNetworking() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		Vector<String> userIfaces = new Vector<String>();
		
		FirewallModel fm = ((ServerModel)me).getFirewallModel();
		
		int dnsPort = (useDtls) ? 853 : 53;
		
		fm.addFilterInput("dns_ipt_in_udp",	"-p udp --dport " + dnsPort + " -j ACCEPT", "Allow outbound DNS requests over UDP");
		fm.addFilterOutput("dns_ipt_out_udp", "-p udp --sport " + dnsPort + " -j ACCEPT", "Allow responses from external DNS over UDP");
		fm.addFilterInput("dns_ipt_in_tcp", "-p tcp --dport " + dnsPort + " -j ACCEPT", "Allow outbound DNS requests over TCP");
		fm.addFilterOutput("dns_ipt_out_tcp", "-p tcp --sport " + dnsPort + " -j ACCEPT", "Allow responses from external DNS over TCP");
		//fm.addFilterOutput("dns_ipt_out_tcp_lo", "-p tcp --dport " + dnsPort + " -j ACCEPT");
		fm.addChain("dns_ipt_chain", "filter", "dnsd");
		fm.addFilter("dns_ext", "dnsd", "-j DROP", "Drop everything else");
		fm.addFilter("dns_ext_log", "dnsd",	"-j LOG --log-prefix \\\"ipt-dnsd: \\\"", "Log any externally bound DNS traffic");
		fm.addFilterInput("dns_ext_in",	"-p udp --sport " + dnsPort + " -j dnsd", "This is an external DNS response - jump to the DNS chain");
		fm.addFilterOutput("dns_ext_out", "-p udp --sport " + dnsPort + " -j dnsd", "This is an external DNS request - jump to the DNS chain");
		
		for (InetAddress upstream : networkModel.getData().getDNS()) {
			fm.addFilter("dns_ext_server_" + upstream.getHostAddress().replaceAll("\\.", "_") + "_in", "dnsd",
					"-s " + upstream.getHostAddress()
					+ " -p udp"
					+ " --sport " + dnsPort
					+ " -j ACCEPT",
					"Allow upstream DNS responses");
			fm.addFilter("dns_ext_server_" + upstream.getHostAddress().replaceAll("\\.", "_") + "_out", "dnsd",
					"-d " + upstream.getHostAddress()
					+ " -p udp"
					+ " --dport " + dnsPort
					+ " -j ACCEPT",
					"Alllow requests to upstream DNS");
		}

		fm.addFilter("dns_allow_loopback_in", "dnsd", "-i lo -j ACCEPT", "Allow local DNS resolution");
		fm.addFilter("dns_allow_loopback_out", "dnsd", "-o lo -j ACCEPT", "Allow local DNS resolution");

		if (!((ServerModel)me).isMetal()) {
			for (String iface : new Vector<String>(networkModel.getData().getLanIfaces(me.getLabel()).keySet())) {
				userIfaces.addElement(iface + ":2+");
				if (!networkModel.getData().getVpnOnly()) {
					userIfaces.addElement(iface + ":1+");
				}
				
			}
		}
		else {
			userIfaces.addElement("br+");
		}
		
		for (String iface : userIfaces) {
			fm.addFilter("dns_allow_in", "dnsd", "-i " + iface + " -j ACCEPT", "Accept inbound traffic for user");
			fm.addFilter("dns_allow_bridges_out", "dnsd", "-o " + iface + " -j ACCEPT", "Accept outbound traffic for user");
		}
		
		for (String iface : new Vector<String>(networkModel.getData().getLanIfaces(me.getLabel()).keySet())) {
			fm.addFilter("dns_allow_in", "dnsd", "-i " + iface + " -j ACCEPT", "");
			fm.addFilter("dns_allow_out", "dnsd", "-o " + iface + " -j ACCEPT", "");
		}
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String ifaceConfig = "";
		
		ifaceConfig += "    interface: 127.0.0.1\n";
		ifaceConfig += "    interface: 10.0.0.1\n";
		
		for (InetAddress gateway : this.gateways) {
			ifaceConfig += "    interface: " + gateway.getHostAddress() + "\n";
		}
		
		units.addElement(((ServerModel)me).getConfigsModel().addConfigFile("dns_listening_faces", "dns_installed", ifaceConfig.replaceAll("\\s+$", ""), "/etc/unbound/unbound.conf.d/interfaces.conf"));

		for (String domain : domainRecords.keySet()) {
			String zoneConfig = "";
			zoneConfig += "    local-zone: \\\"" + domain + ".\\\" typetransparent";
	
			Vector<String> records = domainRecords.get(domain);
			
			for (String record : records) {
				zoneConfig += "\n";
				zoneConfig += record;
			}
			
			units.addElement(((ServerModel)me).getConfigsModel().addConfigFile(domain.replaceAll("\\.", "_").replaceAll("-",  "_") + "_dns_internal_zone", "dns_installed", zoneConfig.replaceAll("\\s+$", ""), "/etc/unbound/unbound.conf.d/" + domain + ".zone"));
		}
		
		if (!poison.isEmpty()) {
			String poisonConfig = "";
			for (Map.Entry<String, InetAddress> record : poison.entrySet()) {
				poisonConfig += "\n    local-zone: \\\"" + record.getKey() + "\\\" redirect";
				poisonConfig += "\n    local-data: \\\"" + record.getKey() + " A " + record.getValue().toString() + "\\\"";
			}
			units.addElement(((ServerModel)me).getConfigsModel().addConfigFile("dns_poison_zone", "dns_installed", poisonConfig.replaceAll("\\s+$", ""), "/etc/unbound/unbound.conf.d/poison.zone"));
		}
		
		if (networkModel.getData().getAdBlocking()) {
			units.addElement(new SimpleUnit("adblock_up_to_date", "proceed",
					"sudo wget -O /etc/unbound/rawhosts https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts;"
					+ "cat /etc/unbound/rawhosts | grep '^0\\.0\\.0\\.0' | sudo awk '{print \"local-zone: \\\"\"$2\"\\\" redirect\\nlocal-data: \\\"\"$2\" A 0.0.0.0\\\"\"}' > /etc/unbound/unbound.conf.d/adblock.zone",				
					"[ ! -f /etc/unbound/rawhosts ] && echo fail || wget -O - -o /dev/null https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts | cmp /etc/unbound/rawhosts 2>&1", "", "pass"));
		}
		
		units.addElement(new RunningUnit("dns", "unbound", "unbound"));
		
		String resolv = "";
		resolv += "search " + networkModel.getData().getDomain(me.getLabel()) + "\n";
		resolv += "nameserver 127.0.0.1";
		units.addElement(new FileUnit("dns_resolv_conf", "dns_running", resolv, "/etc/resolv.conf",
				"Unable to change your DNS to point at the local one.  This will probably cause VM building to fail, amongst other problems"));
		
		return units;
	}

}
