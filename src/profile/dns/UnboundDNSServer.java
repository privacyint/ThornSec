/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.dns;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import core.StringUtils;
import core.data.machine.AMachineData.Encapsulation;
import core.data.machine.AMachineData.MachineType;
import core.exception.data.InvalidPortException;
import core.exception.runtime.ARuntimeException;
import core.exception.runtime.InvalidMachineModelException;
import core.iface.IUnit;
import core.model.machine.AMachineModel;
import core.model.machine.configuration.networking.ISystemdNetworkd;
import core.unit.SimpleUnit;
import core.unit.fs.DirOwnUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;
import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;
import profile.type.Router;

/**
 * Creates and configures an internal, recursive DNS server for your network.
 *
 * Please see https://nlnetlabs.nl/projects/unbound/about/ for more details.
 */
public class UnboundDNSServer extends ADNSServerProfile {

	private static String UNBOUND_CONFIG_DIR = "/etc/unbound/";
	private static String UNBOUND_CONFIG_DROPIN_DIR = UNBOUND_CONFIG_DIR + "unbound.conf.d/";
	private static String UNBOUND_CONFIG_FILE = UNBOUND_CONFIG_DIR + "unbound.conf";

	private static String UNBOUND_PIDFILE = "/var/run/unbound/unbound.pid";

	private static Integer DEFAULT_UPSTREAM_DNS_PORT = 853;

	private final Map<HostName, Set<AMachineModel>> zones;

	private FileUnit unboundConf;

	/**
	 * Initialise an Unbound DNS Server on this machine
	 * @param me Our ServerModel
	 */
	public UnboundDNSServer(ServerModel me) {
		super(me);

		this.zones = new Hashtable<>();
		this.unboundConf = null;
	}

	/**
	 * Builds our unbound config file, see 
	 * https://linux.die.net/man/5/unbound.conf for full config file
	 * 
	 * Config originally based on https://calomel.org/unbound_dns.html
	 * @return Units for unbound.conf and the drop-in directory
	 */
	@Override
	public Collection<IUnit> getPersistentConfig() {
		final Integer cpus = getServerModel().getCPUs();
		
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(getRootHints());
		units.addAll(populateInternalZones());

		this.unboundConf = new FileUnit("unbound_conf", "dns_installed",
				UNBOUND_CONFIG_FILE,
				"root", "root", 644,
				"I was unable to create Unbound's config file. Your DNS server will fail to boot.");
		units.add(unboundConf);

		unboundConf.appendLine("server:");

		dropUserPostInvocation("unbound");
		setLogVerbosity(1);
		setWorkingDirectory(UNBOUND_CONFIG_DIR);
		setChroot(""); // TODO: implement  
		setPIDFile(UNBOUND_PIDFILE);

		buildListeningIfaces();
		doIfacesAccessControl();
		setPrivateAddresses();

		setListeningPort(DEFAULT_LISTEN_PORT);
		setListenTCP("yes");
		setListenUDP("yes");
		setListenIPv4("yes");
		setListenIPv6("no");

		// Add some DNS hardening
		hideIdentity("yes");
		unboundConf.appendLine("\thide-version: yes");
		unboundConf.appendLine("\tharden-glue: yes");
		unboundConf.appendLine("\tharden-dnssec-stripped: yes");
		unboundConf.appendLine("\tuse-caps-for-id: yes");
		// Add some response hardening
		unboundConf.appendLine("\tunwanted-reply-threshold: 10000");
		unboundConf.appendLine("\tdo-not-query-localhost: no");
		unboundConf.appendLine("\tval-clean-additional: yes");
		// Add some performance enhancements
		unboundConf.appendLine("\tcache-min-ttl: 3600");
		unboundConf.appendLine("\tcache-max-ttl: 86400");
		unboundConf.appendLine("\tprefetch: yes");
		// Add sensible values based on the number of CPUs in your machine
		unboundConf.appendLine("\tnum-threads: " + cpus);
		unboundConf.appendLine("\tmsg-cache-slabs: " + (cpus * 2));
		unboundConf.appendLine("\trrset-cache-slabs: " + (cpus * 2));
		unboundConf.appendLine("\tinfra-cache-slabs: " + (cpus * 2));
		unboundConf.appendLine("\tkey-cache-slabs: " + (cpus * 2));
		unboundConf.appendLine("\trrset-cache-size: " + (cpus / 4) + "m");
		unboundConf.appendLine("\tmsg-cache-size: " + (cpus / 8) + "m");
		unboundConf.appendLine("\tso-rcvbuf: 1m");
		// Only switch on blocking if the user actually wants it...
		if (getNetworkModel().getData().doAdBlocking().orElse(false)) {
			unboundConf.appendLine("\tinclude: \\\"/etc/unbound/unbound.conf.d/adblock.zone\\\"");
		}
		// Root hints
		unboundConf.appendLine("\troot-hints: \\\"/etc/unbound/root.hints\\\"");
		// Zone related stuff
		for (final HostName zone : this.zones.keySet()) {
			unboundConf.appendLine("\tprivate-domain: \\\"" + zone.getHost() + "\\\"");
			unboundConf.appendLine("\tinclude: \\\"/etc/unbound/unbound.conf.d/" + zone.getHost() + ".zone\\\"");
		}
		// rDNS
		myRouter.getVLANTrunk().getVLANs().forEach(vlan -> {
			unboundConf.appendLine("\t#" + vlan.getIface());
			
			IPAddress subnet = vlan.getSubnet().getLowerNonZeroHost().withoutPrefixLength();
		
			unboundConf.appendLine("\tlocal-zone: \\\"" + subnet.toReverseDNSLookupString() + "\\\" nodefault");
			unboundConf.appendLine("\tstub-zone:");
			unboundConf.appendLine("\t\tname: \\\"" + subnet.toReverseDNSLookupString() + "\\\"");
			unboundConf.appendLine("\t\tstub-addr: " + subnet.toCompressedString());
		});
		// Upstream DNS servers
		unboundConf.appendLine("\tforward-zone:");
		unboundConf.appendLine("\t\tname: \\\".\\\"");
		for (final HostName upstream : getNetworkModel().getUpstreamDNSServers()) {
			Integer port = upstream.getPort();
			if (port == null) {
				port = DEFAULT_UPSTREAM_DNS_PORT;
			}
			if (port == 853) {
				unboundConf.appendLine("\t\tforward-ssl-upstream: yes");
			}
			unboundConf.appendLine("\t\tforward-addr: " + upstream.getHost() + "@" + port);
		}

		final DirUnit unboundConfD = new DirUnit("unbound_conf_d", "dns_installed", UNBOUND_CONFIG_DROPIN_DIR, "root", "root", 0664, "");

		units.add(unboundConfD);

		return units;
	}

	/**
	 * If enabled id.server and hostname.bind queries are refused. 
	 * @param value "yes" or "no"
	 */
	private void hideIdentity(String value) {
		unboundConf.appendLine("\thide-identity: " + value);		
	}

	/**
	 * Enable or disable whether ip6 queries are answered or issued.
	 * 
	 * If disabled, queries are not answered on IPv6, and queries are not sent
	 * on IPv6 to the upstream nameservers. 
	 * @param value "yes" or "no"
	 */
	private void setListenIPv6(String value) {
		unboundConf.appendLine("\tdo-ip6: " + value);		
	}

	/**
	 * Enable or disable whether ip4 queries are answered or issued
	 * @param value "yes" or "no"
	 */
	private void setListenIPv4(String value) {
		unboundConf.appendLine("\tdo-ip4: " + value);
	}

	/**
	 * Enable or disable whether UDP queries are answered or issued
	 * @param value "yes" or "no"
	 */
	private void setListenUDP(String value) {
		unboundConf.appendLine("\tdo-udp: " + value);
	}

	/**
	 * Enable or disable whether TCP queries are answered or issued
	 * @param value "yes" or "no"
	 */
	private void setListenTCP(String value) {
		unboundConf.appendLine("\tdo-tcp: " + value);
	}

	/**
	 * Set port number on which the server responds to queries.
	 * @param port the port to listen on
	 */
	private void setListeningPort(Integer port) {
		unboundConf.appendLine("\tport: " + port);
	}

	/**
	 * Set addresses on your private network so they are not allowed to be
	 * returned for public internet names. Any occurence of such addresses are
	 * removed from DNS answers. Additionally, the DNSSEC validator may mark the
	 * answers bogus. This protects against so-called DNS Rebinding, where a
	 * user browser is turned into a network proxy, allowing remote access
	 * through the browser to other parts of your private network.
	 */
	private void setPrivateAddresses() {
		getServerModel().getIPs().forEach(ip -> {
			IPAddress subnet = ip.getLowerNonZeroHost().withoutPrefixLength();
			unboundConf.appendLine("\tprivate-address: " + subnet.toCompressedString());
		});
	}

	/**
	 * Allow DNS queries, but only from LAN, not the wider Internet.
	 */
	private void doIfacesAccessControl() {
		/**
		 * The allow action does allow nonrecursive queries to access the
		 * local-data that is configured. The reason is that this does not
		 * involve the unbound server recursive lookup algorithm, and static
		 * data is served in the reply. This supports normal operations where
		 * nonrecursive queries are made for the authoritative data. For
		 * nonrecursive queries any replies from the dynamic cache are refused. 
		 */
		unboundConf.appendLine("\taccess-control: 127.0.0.1/32 allow");

		getServerModel().getIPs().forEach(ip -> {
			IPAddress subnet = ip.getLowerNonZeroHost().withoutPrefixLength();
			unboundConf.appendLine("\taccess-control: " + subnet.toCompressedString() + " allow");
		});

		/*
		 * Refuse stops queries, but sends a DNS rcode REFUSED error message back
		 * 
		 * We use refused not drop, because it's protocol-friendly. The DNS
		 * protocol is not designed to handle dropped packets due to policy,
		 * and dropping may result in (possibly excessive) retried queries. 
		 */
		unboundConf.appendLine("\taccess-control: 0.0.0.0/0 refuse");
	}

	/**
	 * Listen on the various LAN IP addresses (including loopback) assigned to
	 * this machine
	 */
	private void buildListeningIfaces() {
		// Listen to lan/loopback traffic
		unboundConf.appendLine("\tinterface: 127.0.0.1");

		getServerModel().getIPs().forEach(ip -> {
			IPAddress subnet = ip.getLowerNonZeroHost().withoutPrefixLength();
			unboundConf.appendLine("\tinterface: " + subnet.toCompressedString());
		});
	}

	/**
	 * The process id is written to the file
	 * @param path path to the PID file
	 */
	private void setPIDFile(String path) {
		unboundConf.appendLine("\tpidfile: \\\"" + path + "\\\"");
	}

	/**
	 * Puts the Unbound daemon in a chroot (https://en.wikipedia.org/wiki/Chroot)
	 * This is not a foolproof security measure, but every obstacle is positive
	 * @param chrootDir chroot directory for the program, or empty string to not
	 * 					perform a chroot
	 */
	private void setChroot(String chrootDir) {
		unboundConf.appendLine("\tchroot: \\\"" + chrootDir + "\\\"");
	}

	/**
	 * Sets the working directory for the program
	 * @param workingDir working directory for the program
	 */
	private void setWorkingDirectory(String workingDir) {
		unboundConf.appendLine("\tdirectory: \\\"" + workingDir + "\\\"");
	}

	/**
	 * Bigger the number, the noisier the logs
	 * @param verbosity 0 means no verbosity, only errors.
	 * 					1 for operational information.
	 * 					2 for detailed operational information.
	 * 					3 for query level information, output per query.
	 * 					4 for algorithm level information.
	 * 					5 logs client identification for cache misses 
	 */
	private void setLogVerbosity(int verbosity) {
		unboundConf.appendLine("\tverbosity: " + verbosity);
	}

	/**
	 * If given, after binding the port the user privileges are dropped.
	 * Default user is "unbound".
	 * @param user pass specific username to drop to, null to use the default,
	 * 				or empty string for no user change
	 */
	private void dropUserPostInvocation(String user) {
		if (user == null) { user = "unbound"; }

		unboundConf.appendLine("\tusername: \\\"" + user + "\\\"");
	}

	/**
	 * Build our internal DNS zones from the whole network
	 * @return
	 */
	private Collection<IUnit> populateInternalZones() {
		final Collection<IUnit> units = new ArrayList<>();

		addRecord(getNetworkModel().getMachines().values());

		return units;
	}

	/**
	 * Downloads a root hints file from InterNIC. This is used as authoritative
	 * records for the "." zone.
	 * 
	 * See https://kb.isc.org/docs/aa-01309 for a little more info
	 * @return a unit which downloads InterNIC's root hints file to /etc/unbound
	 */
	private Collection<IUnit> getRootHints() {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new SimpleUnit("root_hints", "proceed",
				"sudo wget -O /etc/unbound/root.hints https://www.internic.net/domain/named.root",
				"[ ! -f /etc/unbound/root.hints ] && echo fail",
				"", "pass"));

		return units;
	}

	@Override
	public Collection<IUnit> getInstalled() throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("dns", "proceed", "unbound"));
		getServerModel().addSystemUsername("unbound");
		getServerModel().addProcessString("/usr/sbin/unbound -d$");

		if (getNetworkModel().getData().doAdBlocking().isPresent()
				&& getNetworkModel().getData().doAdBlocking().get()) {
			units.add(new InstalledUnit("ca_certificates", "proceed", "ca-certificates"));
			units.add(new InstalledUnit("wget", "proceed", "wget"));
		}

		return units;
	}

	@Override
	public Collection<IUnit> getLiveConfig() throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		// Start by updating the ad block list (if req'd)
		if (getNetworkModel().getData().doAdBlocking().isPresent()
				&& getNetworkModel().getData().doAdBlocking().get()) {
			units.add(new SimpleUnit("adblock_up_to_date", "proceed",
					"sudo wget -O /etc/unbound/rawhosts https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"
							+ " && cat /etc/unbound/rawhosts | grep '^0\\.0\\.0\\.0'"
							+ " | awk '{print \"local-zone: \\\"\"$2\"\\\" redirect\\nlocal-data: \\\"\"$2\" A 0.0.0.0\\\"\"}'"
							+ " | sudo tee /etc/unbound/unbound.conf.d/adblock.zone > /dev/null"
							+ " && sudo service unbound restart",
					"[ ! -f /etc/unbound/rawhosts ] && echo fail || wget -O - -o /dev/null https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts | cmp /etc/unbound/rawhosts 2>&1",
					"", "pass"));
		}

		// Now make sure all of the various zones are there & up to date
		for (final HostName domain : this.zones.keySet()) {
			final FileUnit zoneFile = new FileUnit(domain.getHost() + "_dns_internal_zone", "dns_installed", "/etc/unbound/unbound.conf.d/" + domain.getHost() + ".zone");
			units.add(zoneFile);
			// Typetransparent passes resolution upwards if not found locally
			zoneFile.appendLine("\tlocal-zone: \\\"" + domain.getHost() + ".\\\" typetransparent");

			for (final AMachineModel machine : this.zones.get(domain)) {
				for (final ISystemdNetworkd iface : machine.getNetworkInterfaces()) {

					if (iface.getAddresses() == null) {
						continue;
					}

					for (final IPAddress ip : iface.getAddresses()) {
						if (ip == null) {
							continue;
						}
						
						zoneFile.appendLine(createRecords(machine, ip));
					}
				}
			}
		}

		units.add(new RunningUnit("dns", "unbound", "unbound"));

		final FileUnit resolvConf = new FileUnit("dns_resolv_conf", "dns_running", "/etc/resolv.conf",
				"Unable to change your DNS to point at the local one.  This will probably cause VM building to fail, amongst other problems");
		units.add(resolvConf);
		resolvConf.appendLine("search " + getMachineModel().getDomain().getHost());
		resolvConf.appendLine("nameserver 127.0.0.1");

		return units;
	}
	
	private Collection<String> createRecords(AMachineModel machine, IPAddress ip) {
		Collection<String> records = new ArrayList<>();
		
		String hostname = StringUtils.stringToAlphaNumeric(machine.getLabel(), "-").toLowerCase();

		records.add("\tlocal-data: \\\"" + hostname + " A " + ip.getLowerNonZeroHost().withoutPrefixLength() + "\\\"");
		records.add("\tlocal-data: \\\"" + hostname  + "." + machine.getDomain() + " A " + ip.getLowerNonZeroHost().withoutPrefixLength() + "\\\"");
		records.add("\tlocal-data-ptr: \\\"" + ip.getLowerNonZeroHost().withoutPrefixLength() + " " + machine.getLabel().toLowerCase() + "\\\"");
		records.add("\tlocal-data-ptr: \\\"" + ip.getLowerNonZeroHost().withoutPrefixLength() + " " + machine.getLabel().toLowerCase() + "." + machine.getDomain() + "\\\"");
		
		if (machine.getCNAMEs() != null) {
			for (final String cname : machine.getCNAMEs()) {
				records.add("\tlocal-data: \\\"" + cname.toLowerCase() + " A " + ip.getLowerNonZeroHost().withoutPrefixLength() + "\\\"");
				records.add("\tlocal-data: \\\"" + hostname  + "." + machine.getDomain() + " A " + ip.getLowerNonZeroHost().withoutPrefixLength() + "\\\"");
				if (cname.equals(".")) {
					records.add("\tlocal-data: \\\"" + machine.getDomain().getHost() + " A " + ip.withoutPrefixLength() + "\\\"");
				}
				else {
					records.add("\tlocal-data: \\\"" + cname.toLowerCase() + "." + machine.getDomain().getHost() + " A " + ip.withoutPrefixLength() + "\\\"");
				}
			}
		}
		
		return records;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws ARuntimeException, InvalidPortException {
		for (final HostName upstream : getNetworkModel().getData().getUpstreamDNSServers()) {
			getNetworkModel().getServerModel(getLabel()).addEgress(upstream);
		}

		getNetworkModel().getServerModel(getLabel()).addListen(Encapsulation.TCP, 53);
		getNetworkModel().getServerModel(getLabel()).addListen(Encapsulation.UDP, 53);

		return new HashSet<>();
	}

	@Override
	public Collection<IUnit> getLiveFirewall() throws ARuntimeException {
		return new HashSet<>();
	}

	@Override
	public void addRecord(AMachineModel... machines) {
		Map<HostName, Set<AMachineModel>> zones = this.zones;
		if (zones == null) {
			zones = new Hashtable<>();
		}

		for (AMachineModel machine : machines) {
			Set<AMachineModel> _machines = zones.get(machine.getDomain());
			if (_machines == null) {
				_machines = new HashSet<>();
			}
	
			_machines.add(machine);
			this.zones.put(machine.getDomain(), _machines);
		}
	}

}
