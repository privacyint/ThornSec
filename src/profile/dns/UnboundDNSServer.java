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
import core.data.machine.configuration.TrafficRule.Encapsulation;
import core.exception.data.InvalidPortException;
import core.exception.runtime.ARuntimeException;
import core.exception.runtime.InvalidMachineModelException;
import core.exception.runtime.InvalidProfileException;
import core.iface.IUnit;
import core.model.machine.AMachineModel;
import core.model.machine.ServerModel;
import core.model.machine.configuration.networking.MACVLANTrunkModel;
import core.unit.SimpleUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;
import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;

/**
 * Creates and configures an internal, recursive DNS server for your network.
 *
 * Please see https://nlnetlabs.nl/projects/unbound/about/ for more details.
 */
public class UnboundDNSServer extends ADNSServerProfile {
	private static Integer DEFAULT_UPSTREAM_DNS_PORT = 853;

	private static String UNBOUND_CONFIG_DIR = "/etc/unbound/";
	private static String UNBOUND_CONFIG_DROPIN_DIR = UNBOUND_CONFIG_DIR + "unbound.conf.d/";
	private static String UNBOUND_CONFIG_FILE = UNBOUND_CONFIG_DIR + "unbound.conf";

	private static String UNBOUND_PIDFILE = "/var/run/unbound/unbound.pid";

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
	 * @throws InvalidProfileException 
	 */
	@Override
	public Collection<IUnit> getPersistentConfig() throws InvalidProfileException {
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
		hideVersion("yes");
		hardenGlue("yes");
		hardenDNSSECStripped("yes");
		useCapsForID("yes");

		// Add some response hardening
		unwantedReplyThreshold(10000);
		doNotQueryLocalhost("no");
		valCleanAdditional("yes");

		// Add some performance enhancements
		cacheMiniumumTTL(60*60); // 1 hour
		cacheMaximumTTL((60*60)*24); // 1 day
		prefetch("yes");

		// Configure sensible values based on the number of CPUs in your machine
		final Integer cpus = getServerModel().getCPUs();
		numThreads(cpus/2);
		msgCacheSlabs(16);
		rrsetCacheSlabs(16);
		infraCacheSlabs(16);
		keyCacheSlabs(16);
		rrsetCacheSize(100);
		msgCacheSize(50);
		soRCVBuffer(0);

		adBlocking(getNetworkModel().doAdBlocking());
		rootHints(UNBOUND_CONFIG_DIR + "root.hints");

		setInternalZones();
		rDNS("nodefault");

		forwardZone();

		return units;
	}

	/**
	 * Set our upstream DNS servers
	 */
	private void forwardZone() {
		this.unboundConf.appendLine("\tforward-zone:");
		this.unboundConf.appendLine("\t\tname: \\\".\\\"");
		for (final HostName upstream : getNetworkModel().getUpstreamDNSServers()) {
			Integer port = upstream.getPort();
			if (port == null) {
				port = DEFAULT_UPSTREAM_DNS_PORT;
			}
			if (port == 853) {
				this.unboundConf.appendLine("\t\tforward-ssl-upstream: yes");
			}
			this.unboundConf.appendLine("\t\tforward-addr: " + upstream.getHost() + "@" + port);
		}
	}

	/**
	 * Configure our reverse DNS for our subnets.
	 * 
	 * Answers for local zones are authoritative DNS answers. By default the
	 * zones are class IN.
	 * 
	 * This is achieved through building a stub zone for each subnet.
	 * @param type The type determines the answer to give if there is no match
	 * 				from local-data.
	 * 
	 * 				The types are deny, refuse, static, transparent, redirect,
	 * 				nodefault, and typetransparent.
	 *
	 * 				deny:
	 * 					Do not send an answer, drop the query.
	 * 					If there is a match from local data, the query is answered.
	 * 				refuse:
	 *					Send an error message reply, with rcode REFUSED. If
	 *					there is a match from local data, the query is answered.
	 *				static:
	 *					If there is a match from local data, the query is answered.
	 *					Otherwise, the query is answered with nodata or nxdomain.
	 *					For a negative answer a SOA is included in the answer if
	 *					present as local-data for the zone apex domain.
	 *				transparent:
	 *					If there is a match from local data, the query is answered.
	 *					Otherwise if the query has a different name, the query is
	 *					resolved normally. If the query is for a name given in
	 *					localdata but no such type of data is given in localdata,
	 *					then a noerror nodata answer is returned. If no local-zone
	 *					is given local-data causes a transparent zone to be created
	 *					by default.
	 *				typetransparent:
	 *					If there is a match from local data, the query is answered.
	 *					If the query is for a different name, or for the same name
	 *					but for a different type, the query is resolved normally.
	 *					So, similar to transparent but types that are not listed
	 *					in local data are resolved normally, so if an A record is
	 *					in the local data that does not cause a nodata reply for
	 *					AAAA queries.
	 *				redirect:
	 *					The query is answered from the local data for the zone name.
	 *					There may be no local data beneath the zone name. This
	 *					answers queries for the zone, and all subdomains of the
	 *					zone with the local data for the zone. It can be used to
	 *					redirect a domain to return a different address record to
	 *					the end user, with local-zone: "example.com." redirect and
	 *					local-data: "example.com. A 127.0.0.1" queries for
	 *					www.example.com and www.foo.example.com are redirected,
	 *					so that users with web browsers cannot access sites with
	 *					suffix example.com.
	 *				nodefault:
	 *					Used to turn off default contents for AS112 zones.
	 *					The other types also turn off default contents for the zone.
	 *					The 'nodefault' option has no other effect than turning
	 *					off default contents for the given zone.
	 */
	private void rDNS(String type) {
		getServerModel().getNetworkInterfaces()
			.stream()
			.filter(nic -> nic instanceof MACVLANTrunkModel)
			.forEach(trunk -> {
				((MACVLANTrunkModel)trunk).getVLANs().forEach(vlan -> {
					IPAddress subnet = vlan.getSubnet().getLowerNonZeroHost().withoutPrefixLength();

					addComment(vlan.getIface());
					
					unboundConf.appendLine("\tlocal-zone: \\\"" + subnet.toReverseDNSLookupString() + "\\\" " + type);
					unboundConf.appendLine("\tstub-zone:");
					unboundConf.appendLine("\t\tname: \\\"" + subnet.toReverseDNSLookupString() + "\\\"");
					unboundConf.appendLine("\t\tstub-addr: " + subnet.toCompressedString());
				});
			});
	}

	/**
	 * Number of slabs in the RRset cache.
	 *
	 * Slabs reduce lock contention by threads.
	 *
	 * @param slabs Number of slabs in the RRset cache. Must be a power of 2
	 * @throws InvalidProfileException if number of slabs is invalid
	 */
	private void rrsetCacheSlabs(int slabs) throws InvalidProfileException {
		if (!isPowerOfTwo(slabs)) {
			throw new InvalidProfileException("rrset cache must be a power of two");
		}
		addSettingToConfig("rrset-cache-slabs", slabs);
	}

	/**
	 * Add a comment to the config file. This is used purely for readability,
	 * comments aren't parsed by Unbound
	 * @param comment text
	 */
	private void addComment(String comment) {
		unboundConf.appendLine("\t# " + comment);	
	}

	/**
	 * Set all domains represented in our network as being allowed to be served
	 * from here
	 */
	private void setInternalZones() {
		for (final HostName zone : this.zones.keySet()) {
			unboundConf.appendLine("\tprivate-domain: \\\"" + zone.getHost() + "\\\"");
			unboundConf.appendLine("\tinclude: \\\"" + UNBOUND_CONFIG_DROPIN_DIR + zone.getHost() + ".zone\\\"");
		}
	}

	/**
	 * Include the root hints file
	 * @param string
	 */
	private void rootHints(String path) {
		unboundConf.appendLine("\troot-hints: \\\"" + path + "\\\"");
	}

	/**
	 * If enabled, imports our adblocking zone, meaning
	 * @param doAdBlocking true to ad block, false otherwise
	 */
	private void adBlocking(boolean doAdBlocking) {
		if (!doAdBlocking) { return; }

		unboundConf.appendLine("\tinclude: \\\"" + UNBOUND_CONFIG_DROPIN_DIR + "adblock.zone\\\"");
	}

	/**
	 * If not 0, then set the SO_RCVBUF socket option to get more buffer space
	 * on UDP port incoming queries so that short spikes on busy servers do
	 * not drop packets (see counter in netstat -su). 
	 * @param size 0 (use system value). Otherwise, the number of bytes to ask
	 * 				for. The OS caps it at a maximum, on linux unbound needs
	 * 				root permission to bypass the limit, or the admin can set
	 * 				sysctl net.core.rmem_max.
	 * @throws InvalidProfileException if size is invalid
	 */
	private void soRCVBuffer(int size) throws InvalidProfileException {
		addSettingToConfig("so-rcvbuf", size);
	}

	/**
	 * Number of bytes size of the message cache
	 * @param megabytes size in megabytes
	 * @throws InvalidProfileException if size is invalid
	 */
	private void msgCacheSize(int megabytes) throws InvalidProfileException {
		addSettingToConfig("msg-cache-size", megabytes + "m");
	}

	/**
	 * Number of megabytes size of the RRset cache
	 * @param megasize size in megabytes
	 * @throws InvalidProfileException if size is invalid
	 */
	private void rrsetCacheSize(int megabytes) throws InvalidProfileException {
		addSettingToConfig("rrset-cache-size", megabytes + "m");
	}

	/**
	 * Checks whether a number is a power of two
	 * @param x number
	 * @return true if the number is a power of two
	 */
	private boolean isPowerOfTwo(int x) {
		return ((x & (x - 1)) == 0);
	}

	/**
	 * Number of slabs in the key cache.
	 *
	 * Slabs reduce lock contention by threads.
	 *
	 * Must be set to a power of 2.
	 *
	 * @param slabs Number of slabs in the key cache. Must be a power of
	 *              two.
	 * @throws InvalidProfileException if number of slabs is invalid
	 */
	private void keyCacheSlabs(int slabs) throws InvalidProfileException {
		if (!isPowerOfTwo(slabs)) {
			throw new InvalidProfileException("key cache must be a power of two");
		}
		addSettingToConfig("key-cache-slabs", slabs);
	}

	/**
	 * Number of slabs in the infrastructure cache.
	 *
	 * Slabs reduce lock contention by threads.
	 *
	 * Must be set to a power of 2.
	 *
	 * @param slabs Number of slabs in the infrastructure cache. Must be a power of
	 *              two.
	 * @throws InvalidProfileException if number of slabs is invalid
	 */
	private void infraCacheSlabs(int slabs) throws InvalidProfileException {
		if (!isPowerOfTwo(slabs)) {
			throw new InvalidProfileException("infrastructure cache must be a power of two");
		}
		addSettingToConfig("infra-cache-slabs", slabs);
	}

	/**
	 * Number of slabs in the message cache.
	 *
	 * Slabs reduce lock contention by threads.
	 *
	 * Must be set to a power of 2. Setting (close) to the number of cpus is a
	 * reasonable guess.
	 *
	 * @param slabs Number of slabs in the message cache. Must be a power of two.
	 * @throws InvalidProfileException if number of slabs is invalid
	 */
	private void msgCacheSlabs(int slabs) throws InvalidProfileException {
		if (!isPowerOfTwo(slabs)) {
			throw new InvalidProfileException("message cache must be a power of two");
		}
		addSettingToConfig("msg-cache-slabs", slabs);
	}

	/**
	 * The number of threads to create to serve clients
	 *
	 * @param threads >0
	 * @throws InvalidProfileException if number of threads is invalid
	 */
	private void numThreads(int threads) throws InvalidProfileException {
		if (threads < 1) {
			throw new InvalidProfileException("Number of threads must be a minimum of 1");
		}
		addSettingToConfig("num-threads", threads);
	}

	/**
	 * Whether cache elements are prefetched before they expire to keep the
	 * cache up to date.
	 * 
	 * Switching on generates about 10% more traffic and load on the machine,
	 * but popular items do not expire from the cache. 
	 * @param value "yes" or "no"
	 * @throws InvalidProfileException if invalid value passed
	 */
	private void prefetch(String value) throws InvalidProfileException {
		addSettingToConfig("prefetch", value);
	}

	/**
	 * Time to live maximum for RRsets and messages in the cache.
	 * 
	 * If this maximum is hit, responses to clients still get decrementing TTLs
	 * based on the upstream (larger) values.
	 * 
	 * When the internal TTL expires, the cache item has expired.
	 * 
	 * Can be set low to force the resolver to query for data more often, and
	 * not trust (very large) TTL values. 
	 * @param seconds maximum TTL in seconds
	 * @throws InvalidProfileException if invalid TTL
	 */
	private void cacheMaximumTTL(int seconds) throws InvalidProfileException {
		addSettingToConfig("cache-max-ttl", seconds);
	}

	/**
	 * Time to live minimum for RRsets and messages in the cache.
	 * 
	 * Depending on the value, the data may end up cached for longer than the
	 * domain owner intended, but fewer queries are made to upstream.
	 * 
	 * High values, especially more than an hour or so, can lead to trouble as
	 * the data in the cache may not match up with the live data any more.
	 * 
	 * Setting to 0 makes sure the data in the cache is as the domain owner
	 * intended. 
	 * @param min 0 to use domain's TTL, or fixed minimum cache in seconds
	 * @throws InvalidProfileException if given an invalid TTL
	 */
	private void cacheMiniumumTTL(int seconds) throws InvalidProfileException {
		addSettingToConfig("cache-min-ttl", seconds);
	}

	/**
	 * Instruct the validator to remove data from the additional section of secure
	 * messages that are not signed properly. Messages that are insecure, bogus,
	 * indeterminate or unchecked are not affected.
	 *
	 * Use this setting to protect the users that rely on this validator for
	 * authentication from potentially bad data in the additional section.
	 *
	 * @param value "yes" or "no"
	 * @throws InvalidProfileException if value is invalid
	 */
	private void valCleanAdditional(String value) throws InvalidProfileException {
		addSettingToConfig("val-clean-additional", value);
	}

	/**
	 * Whether to add localhost to the do-not-query-address entries
	 * @param value "yes" if allowing localhost or "no" otherwise
	 * @throws InvalidProfileException if value is invalid
	 */
	private void doNotQueryLocalhost(String value) throws InvalidProfileException {
		addSettingToConfig("do-not-query-localhost", "no");
	}

	/**
	 * If set, a total number of unwanted replies is kept track of in every
	 * thread. When it reaches the threshold, a defensive action is taken and
	 * a warning is printed to the log. The defensive action is to clear the
	 * rrset and message caches, hopefully flushing away any poison.
	 * @param value value to set
	 * @throws InvalidProfileException if value is incorrect
	 */
	private void unwantedReplyThreshold(int value) throws InvalidProfileException {
		addSettingToConfig("unwanted-reply-threshold", value);
	}

	/**
	 * Use 0x20-encoded random bits in the query to foil spoof attempts.
	 * 
	 * This perturbs the lowercase and uppercase of query names sent to
	 * authority servers and checks if the reply still has the correct casing.
	 * 
	 * This feature is an experimental implementation of draft dns-0x20.
	 * 
	 * @param value "yes" or "no"
	 * @throws InvalidProfileException if value is invalid
	 */
	private void useCapsForID(String value) throws InvalidProfileException {
		addSettingToConfig("use-caps-for-id", value);
	}

	/**
	 * Require DNSSEC data for trust-anchored zones.
	 * If such data is absent, the zone becomes bogus. If turned off, and no
	 * DNSSEC data is received (or the DNSKEY data fails to validate), then
	 * the zone is made insecure, this behaves like there is no trust anchor.
	 * 
	 * You could turn this off if you are sometimes behind an intrusive firewall
	 * (of some sort) that removes DNSSEC data from packets, or a zone changes
	 * from signed to unsigned to badly signed often.
	 * 
	 * If turned off you run the risk of a downgrade attack that disables
	 * security for a zone.
	 * @param value "yes" or "no"
	 * @throws InvalidProfileException is value is invalid  
	 */
	private void hardenDNSSECStripped(String value) throws InvalidProfileException {
		addSettingToConfig("harden-dnssec-stripped", value);
	}

	/**
	 * Add a setting to unbound's conf
	 * @param setting The name of the setting, as per man(5)
	 * @param value "yes" or "no"
	 * @throws InvalidProfileException if value is invalid 
	 */
	private void addSettingToConfig(String setting, String value) throws InvalidProfileException {
		if (!"yes".equals(value) && !"no".equals(value)) {
			throw new InvalidProfileException("value may only be \\\"yes\\\" or \\\"no\\\"");
		}
		unboundConf.appendLine("\t" + setting + ": " + value);
	}

	/**
	 * Add a setting to unbound's conf
	 * @param setting The name of the setting, as per man(5)
	 * @param value >=0
	 * @throws InvalidProfileException if value is invalid 
	 */
	private void addSettingToConfig(String setting, Integer value) throws InvalidProfileException {
		if (value == null || value < 0) {
			throw new InvalidProfileException("value may only be >=0");
		}
		unboundConf.appendLine("\t" + setting + ": " + value);
	}

	/**
	 * Trust glue only if it is within the servers authority
	 * @param value "on" or "off"
	 * @throws InvalidProfileException if value is invalid
	 */
	private void hardenGlue(String value) throws InvalidProfileException {
		addSettingToConfig("harden-glue", value);
	}

	/**
	 * If enabled version.server and version.bind queries are refused. 
	 * @param value "yes" or "no"
	 * @throws InvalidProfileException if value is invalid 
	 */
	private void hideVersion(String value) throws InvalidProfileException {
		addSettingToConfig("hide-version", value);
	}

	/**
	 * If enabled id.server and hostname.bind queries are refused. 
	 * @param value "yes" or "no"
	 * @throws InvalidProfileException if value is invalid 
	 */
	private void hideIdentity(String value) throws InvalidProfileException {
		addSettingToConfig("hide-identity", value);		
	}

	/**
	 * Enable or disable whether ip6 queries are answered or issued.
	 * 
	 * If disabled, queries are not answered on IPv6, and queries are not sent
	 * on IPv6 to the upstream nameservers. 
	 * @param value "yes" or "no"
	 * @throws InvalidProfileException if value is invalid 
	 */
	private void setListenIPv6(String value) throws InvalidProfileException {
		addSettingToConfig("do-ip6", value);		
	}

	/**
	 * Enable or disable whether ip4 queries are answered or issued
	 * @param value "yes" or "no"
	 * @throws InvalidProfileException if value is invalid 
	 */
	private void setListenIPv4(String value) throws InvalidProfileException {
		addSettingToConfig("do-ip4", value);		
	}

	/**
	 * Enable or disable whether UDP queries are answered or issued
	 * @param value "yes" or "no"
	 * @throws InvalidProfileException if value is invalid 
	 */
	private void setListenUDP(String value) throws InvalidProfileException {
		addSettingToConfig("do-udp", value);		
	}

	/**
	 * Enable or disable whether TCP queries are answered or issued
	 * @param value "yes" or "no"
	 * @throws InvalidProfileException 
	 */
	private void setListenTCP(String value) throws InvalidProfileException {
		addSettingToConfig("do-tcp", value);		
	}

	/**
	 * Set port number on which the server responds to queries.
	 * @param port the port to listen on
	 * @throws InvalidProfileException if port is invalid
	 */
	private void setListeningPort(Integer port) throws InvalidProfileException {
		if (port <1 || port > 65535) { throw new InvalidProfileException("Invalid listening port"); }

		addSettingToConfig("port", port);
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
			if (!ip.isLocal()) { return; }

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
			if (!ip.isLocal()) { return; }

			IPAddress subnet = ip.getLowerNonZeroHost();
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
			if (!ip.isLocal()) { return; }

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
	 * @throws InvalidProfileException if verbosity is invalid
	 */
	private void setLogVerbosity(int verbosity) throws InvalidProfileException {
		if (verbosity > 5) { throw new InvalidProfileException("Verbosity must be 0-5"); }

		addSettingToConfig("verbosity", verbosity);
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

		if (getNetworkModel().doAdBlocking()) {
			units.add(new InstalledUnit("ca_certificates", "proceed", "ca-certificates"));
			units.add(new InstalledUnit("wget", "proceed", "wget"));
		}

		return units;
	}

	/**
	 * Downloads our hosts file, and translates it into an Unbound zone
	 * @return a unit to download & update the adblock zone
	 */
	private IUnit getAdBlockFileUnit() {
		return new SimpleUnit("adblock_up_to_date", "proceed",
				"sudo wget -O /etc/unbound/rawhosts https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"
						+ " && cat /etc/unbound/rawhosts | grep '^0\\.0\\.0\\.0'"
						+ " | awk '{print \"local-zone: \\\"\"$2\"\\\" redirect\\nlocal-data: \\\"\"$2\" A 0.0.0.0\\\"\"}'"
						+ " | sudo tee /etc/unbound/unbound.conf.d/adblock.zone > /dev/null"
						+ " && sudo service unbound restart",
				"[ ! -f /etc/unbound/rawhosts ] && echo fail || wget -O - -o /dev/null https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts | cmp /etc/unbound/rawhosts 2>&1",
				"", "pass");
	}
	
	@Override
	public Collection<IUnit> getLiveConfig() throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		final DirUnit unboundConfD = new DirUnit("unbound_conf_d", "dns_installed", UNBOUND_CONFIG_DROPIN_DIR, "root", "root", 0664, "");
		units.add(unboundConfD);

		// Start by updating the ad block list (if req'd)
		if (getNetworkModel().doAdBlocking()) {
			units.add(getAdBlockFileUnit());
		}

		// Now make sure all of the various zones are there & up to date
		units.addAll(buildDropinZones());

		units.add(new RunningUnit("dns", "unbound", "unbound"));

		final FileUnit resolvConf = new FileUnit("dns_resolv_conf", "dns_running", "/etc/resolv.conf",
				"Unable to change your DNS to point at the local one.  This will probably cause VM building to fail, amongst other problems");
		units.add(resolvConf);
		resolvConf.appendLine("search " + getMachineModel().getDomain().getHost());
		resolvConf.appendLine("nameserver 127.0.0.1");

		return units;
	}
	
	private Collection<IUnit> buildDropinZones() {
		final Collection<IUnit> units = new ArrayList<>();

		this.zones.forEach((zone, machines) -> {
			final FileUnit zoneFile = new FileUnit(zone.getHost() + "_dns_internal_zone",
					"dns_installed",
					"/etc/unbound/unbound.conf.d/" + zone.getHost() + ".zone");
			units.add(zoneFile);
			// Typetransparent passes resolution upwards if not found locally
			zoneFile.appendLine("\tlocal-zone: \\\"" + zone.getHost() + ".\\\" typetransparent");
			zoneFile.appendLine(createRecords(machines));
		});

		return units;
	}

	/**
	 * Creates the DNS records for given machines
	 * @param machines machines to build DNS for
	 * @return a Collection of Strings representing the DNS records
	 */
	private Collection<String> createRecords(Collection<AMachineModel> machines) {
		Collection<String> records = new ArrayList<>();

		machines.forEach((machine) -> {
			records.addAll(createRecords(machine));
		});

		return records;
	}

	/**
	 * Creates the DNS records for a given machine
	 * @param machine machine to build DNS for
	 * @return a Collection of Strings representing the DNS records
	 */
	private Collection<String> createRecords(AMachineModel machine) {
		Collection<String> records = new ArrayList<>();

		machine.getIPs().forEach((ip) -> {
			// Add our A records for this machine, both with and without the domain
			records.add("\tlocal-data: \\\"" + machine.getHostName() + " A " + ip.withoutPrefixLength() + "\\\"");
			records.add("\tlocal-data: \\\"" + machine.getHostName()  + "." + machine.getDomain() + " A " + ip.withoutPrefixLength() + "\\\"");
			// Add our reverse-DNS records for this machine, with and without domain
			records.add("\tlocal-data-ptr: \\\"" + ip.withoutPrefixLength() + " " + machine.getHostName() + "\\\"");
			records.add("\tlocal-data-ptr: \\\"" + ip.withoutPrefixLength() + " " + machine.getHostName() + "." + machine.getDomain() + "\\\"");
	
			// Add any CNAMEs configured against this machine
			machine.getCNAMEs().ifPresent((cnames) -> {
				cnames.forEach((cname) -> {
					records.add("\tlocal-data: \\\"" + cname.toLowerCase() + " A " + ip.withoutPrefixLength() + "\\\"");
					records.add("\tlocal-data: \\\"" + cname.toLowerCase() + "." + machine.getDomain() + " A " + ip.withoutPrefixLength() + "\\\"");
					if (cname.equals(".")) {
						records.add("\tlocal-data: \\\"" + machine.getDomain().getHost() + " A " + ip.withoutPrefixLength() + "\\\"");
					}
					else {
						records.add("\tlocal-data: \\\"" + cname.toLowerCase() + "." + machine.getDomain().getHost() + " A " + ip.withoutPrefixLength() + "\\\"");
					}
				});
			});
		});
		
		return records;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws ARuntimeException, InvalidPortException {
		getUpstreamDNSRules();

		//Our local server listens on both TCP&UDP (for now, at least)
		getMachineModel().addLANOnlyListen(Encapsulation.UDP, 67);
		getMachineModel().addLANOnlyListen(Encapsulation.TCP, 67);

		return new HashSet<>();
	}

	/**
	 * Build the firewall rules required for communicating with our upstream DNS
	 * @throws InvalidPortException
	 */
	private void getUpstreamDNSRules() throws InvalidPortException {
		for (HostName upstream : getNetworkModel().getUpstreamDNSServers()) {
			//DNS, by default, is UDP
			Encapsulation upstreamProto = Encapsulation.UDP;

			//However, according to their RFCs, DoH(443) & DoT(853) are TCP
			if (upstream.getPort().equals(443) ||
					upstream.getPort().equals(853)) {
				upstreamProto = Encapsulation.TCP;
			}

			getMachineModel().addEgress(upstreamProto, upstream);
		}
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
