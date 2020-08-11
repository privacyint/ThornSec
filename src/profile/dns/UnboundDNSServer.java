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
import core.exception.data.machine.InvalidServerException;
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

	private static String UNBOUND_CONFIG_FILE_PATH = "/etc/unbound/unbound.conf";
	private static Integer DEFAULT_UPSTREAM_DNS_PORT = 853;

	private final Map<HostName, Set<AMachineModel>> zones;

	public UnboundDNSServer(ServerModel me) {
		super(me);

		this.zones = new Hashtable<>();
	}

	@Override
	public Collection<IUnit> getPersistentConfig() throws InvalidServerException, InvalidMachineModelException {
		final Integer cpus = getServerModel().getCPUs();
		
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(getRootHints());
		units.addAll(populateInternalZones());

		// Config originally based on https://calomel.org/unbound_dns.html
		// See https://linux.die.net/man/5/unbound.conf for full config file
		final FileUnit unboundConf = new FileUnit("unbound_conf", "dns_installed",
				UNBOUND_CONFIG_FILE_PATH,
				"root", "root", 644,
				"I was unable to create Unbound's config file. Your DNS server will fail to boot.");
		units.add(unboundConf);
		unboundConf.appendLine("server:");
		dropUserPostInvocation(unboundConf, "unbound");

		unboundConf.appendLine("\tverbosity: 1");
		unboundConf.appendLine("\tdirectory: \\\"/etc/unbound\\\"");
		// Stick it in a chroot. DNS is dangerous.
		// unboundConf.appendLine("\tchroot: \\\"/etc/unbound\\\"");
		unboundConf.appendLine("\tpidfile: \\\"/var/run/unbound.pid\\\"");
		// Listen to lan/loopback traffic
		unboundConf.appendLine("\tinterface: 127.0.0.1");
		unboundConf.appendLine("\taccess-control: 127.0.0.1/32 allow");
		myRouter.getVLANTrunk().getVLANs().forEach(vlan -> {
			unboundConf.appendLine("\t#" + vlan.getIface());
			
			IPAddress subnet = vlan.getSubnet().getLowerNonZeroHost().withoutPrefixLength();
		
			// Listen on this LAN interface
			unboundConf.appendLine("\tinterface: " + subnet.toCompressedString());
			// Allow it to receive traffic
			unboundConf.appendLine("\taccess-control: " + subnet.toCompressedString() + " allow");
			// Stop DNS Rebinding attacks, and upstream DNS must be WAN
			unboundConf.appendLine("\tprivate-address: " + subnet.toCompressedString());
		});
		// Don't listen to anything else.
		unboundConf.appendLine("\taccess-control: 0.0.0.0/0 refuse");
		// Listen on :53
		unboundConf.appendLine("\tport: 53");
		// Do TCP/UDP, IPv4 only
		unboundConf.appendLine("\tdo-tcp: yes");
		unboundConf.appendLine("\tdo-udp: yes");
		unboundConf.appendLine("\tdo-ip4: yes");
		// No IPv6, please.
		unboundConf.appendLine("\tdo-ip6: no");
		// Add some DNS hardening
		unboundConf.appendLine("\thide-identity: yes");
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

		final DirUnit unboundConfD = new DirUnit("unbound_conf_d", "dns_installed", UNBOUND_CONFIG_FILE_PATH + ".d", "unbound", "unbound", 0660, "");

		units.add(unboundConfD);

		return units;
	}

	/**
	 * If given, after binding the port the user privileges are dropped.
	 * Default user is "unbound".
	 * 
	 * @param unboundConf Config FileUnit
	 * @param user pass specific username to drop to, null to use the default,
	 * 				or empty string for no user change
	 */
	private void dropUserPostInvocation(FileUnit unboundConf, String user) {
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
