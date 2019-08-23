/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.dns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import core.exception.data.machine.InvalidMachineException;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.ARuntimeException;
import core.exception.runtime.InvalidMachineModelException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.machine.AMachineModel;
import core.model.machine.configuration.NetworkInterfaceModel;
import core.model.network.NetworkModel;
import core.unit.SimpleUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;
import inet.ipaddr.HostName;

/**
 * Creates and configures an internal, recursive DNS server for your network.
 *
 * Please see https://nlnetlabs.nl/projects/unbound/about/ for more details.
 */
public class UnboundDNSServer extends ADNSServerProfile {

	private static String UNBOUND_CONFIG_FILE_PATH = "/etc/unbound/unbound.conf";
	private static Integer DEFAULT_UPSTREAM_DNS_PORT = 853;

	private final Hashtable<String, Hashtable<String, Set<HostName>>> zones;

	public UnboundDNSServer(String label, NetworkModel networkModel) {
		super(label, networkModel);

		this.zones = new Hashtable<>();
	}

	@Override
	public Collection<IUnit> getPersistentConfig() throws InvalidServerModelException, InvalidServerException {
		final Integer cpus = getNetworkModel().getData().getCPUs(getLabel());
		final Collection<IUnit> units = new ArrayList<>();

		// Config originally based on https://calomel.org/unbound_dns.html
		// See https://linux.die.net/man/5/unbound.conf for full config file
		final FileUnit unboundConf = new FileUnit("unbound_conf", "dns_installed", UNBOUND_CONFIG_FILE_PATH);
		unboundConf.appendLine("server:");
		// Force dropping user post-invocation
		unboundConf.appendLine("    username: unbound");
		unboundConf.appendLine("    verbosity: 1");
		unboundConf.appendLine("    directory: \\\"/etc/unbound\\\"");
		// Stick it in a chroot. DNS is dangerous.
		unboundConf.appendLine("    chroot: \\\"/etc/unbound\\\"");
		unboundConf.appendLine("    pidfile: \\\"/etc/unbound/unbound.pid\\\"");
		// Only listen to lan/loopback traffic
		unboundConf.appendLine("    interface: 127.0.0.1");
		unboundConf.appendLine("    interface: 10.0.0.1");
		unboundConf.appendLine("    access-control: 127.0.0.0/8 allow");
		unboundConf.appendLine("    access-control: 10.0.0.0/8 allow");
		// Upstream DNS isn't allowed to point somewhere internal
		// Also stops DNS Rebinding attacks.
		unboundConf.appendLine("    private-address: 10.0.0.0/8");
		unboundConf.appendLine("    private-address: 176.16.0.0/12");
		unboundConf.appendLine("    private-address: 192.168.0.0/16");
		// Don't listen to anything else.
		unboundConf.appendLine("    access-control: 0.0.0.0/0 refuse");
		// Listen on :53
		unboundConf.appendLine("    port: 53");
		// Do TCP/UDP, IPv4 only
		unboundConf.appendLine("    do-tcp: yes");
		unboundConf.appendLine("    do-udp: yes");
		unboundConf.appendLine("    do-ip4: yes");
		// No IPv6, please.
		unboundConf.appendLine("    do-ip6: no");
		// Add some DNS hardening
		unboundConf.appendLine("    hide-identity: yes");
		unboundConf.appendLine("    hide-version: yes");
		unboundConf.appendLine("    harden-glue: yes");
		unboundConf.appendLine("    harden-dnssec-stripped: yes");
		unboundConf.appendLine("    use-caps-for-id: yes");
		// Add some response hardening
		unboundConf.appendLine("    unwanted-reply-threshold: 10000");
		unboundConf.appendLine("    do-not-query-localhost: no");
		unboundConf.appendLine("    val-clean-additional: yes");
		// Add some performance enhancements
		unboundConf.appendLine("    cache-min-ttl: 3600");
		unboundConf.appendLine("    cache-max-ttl: 86400");
		unboundConf.appendLine("    prefetch: yes");
		// Add sensible values based on the number of CPUs in your machine
		unboundConf.appendLine("    num-threads: " + cpus);
		unboundConf.appendLine("    msg-cache-slabs: " + (cpus * 2));
		unboundConf.appendLine("    rrset-cache-slabs: " + (cpus * 2));
		unboundConf.appendLine("    infra-cache-slabs: " + (cpus * 2));
		unboundConf.appendLine("    key-cache-slabs: " + (cpus * 2));
		unboundConf.appendLine("    rrset-cache-size: " + (cpus / 4) + "m");
		unboundConf.appendLine("    msg-cache-size: " + (cpus / 8) + "m");
		unboundConf.appendLine("    so-rcvbuf: 1m");
		// Only switch on blocking if the user actually wants it...
		if (getNetworkModel().getData().adBlocking()) {
			unboundConf.appendLine("    include: \\\"/etc/unbound/unbound.conf.d/adblock.zone\\\"");
		}
		// rDNS
		unboundConf.appendLine("    local-zone: \\\"10.in-addr.arpa.\\\" nodefault");
		unboundConf.appendLine("    stub-zone:");
		unboundConf.appendLine("        name: \\\"10.in-addr.arpa.\\\"");
		unboundConf.appendLine("        stub-addr: 10.0.0.1");
		// Zone related stuff
		for (final String zone : this.zones.keySet()) {
			unboundConf.appendLine("    private-domain: \\\"" + zone + "\\\"");
			unboundConf.appendLine("    include: \\\"/etc/unbound/unbound.conf.d/" + zone + ".zone\\\"");
		}
		// Upstream DNS servers
		unboundConf.appendLine("    forward-zone:");
		unboundConf.appendLine("        name: \\\".\\\"");
		for (final HostName upstream : getNetworkModel().getData().getUpstreamDNSServers()) {
			Integer port = upstream.getPort();
			if (port == null) {
				port = DEFAULT_UPSTREAM_DNS_PORT;
			}
			if (port == 853) {
				unboundConf.appendLine("        forward-ssl-upstream: yes");
			}
			unboundConf.appendLine("        forward-addr: " + upstream.getHost() + "@" + port);
		}

		return units;
	}

	@Override
	public Collection<IUnit> getInstalled() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("dns", "proceed", "unbound"));
		getNetworkModel().getServerModel(getLabel()).addSystemUsername("unbound");
		getNetworkModel().getServerModel(getLabel()).addProcessString("/usr/sbin/unbound -d$");

		if (getNetworkModel().getData().adBlocking()) {
			units.add(new InstalledUnit("ca_certificates", "proceed", "ca-certificates"));
			units.add(new InstalledUnit("wget", "proceed", "wget"));
		}

		return units;
	}

	@Override
	public Collection<IUnit> getLiveConfig() throws InvalidMachineException, InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		// Start by updating the ad block list (if req'd)
		if (getNetworkModel().getData().adBlocking()) {
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
		for (final String domain : this.zones.keySet()) {
			final FileUnit zoneFile = new FileUnit(domain + "_dns_internal_zone", "dns_installed",
					"/etc/unbound/unbound.conf.d/" + domain + ".zone");
			zoneFile.appendLine("    local-zone: \\\"" + domain + ".\\\" typetransparent"); // Typetransparent passes
																							// resolution upwards if not
																							// found locally

			final Hashtable<String, Set<HostName>> zone = this.zones.get(domain);
			AMachineModel hostMachine = null;

			for (final String hostName : zone.keySet()) {
				// It may not be a real machine. It might be a poison. Deal with it.
				try {
					hostMachine = getNetworkModel().getMachineModel(hostName);

					for (final NetworkInterfaceModel iface : hostMachine.getNetworkInterfaces()) {
						// @TODO: Double-check logic here
						zoneFile.appendLine("    local-data-ptr: \\\"" + iface.getAddress() + " "
								+ hostMachine.getLabel() + "\\\"");
						zoneFile.appendLine("    local-data-ptr: \\\"" + iface.getGateway() + " router."
								+ hostMachine.getLabel() + "\\\"");

						for (final String cname : hostMachine.getCNAMEs()) {
							zoneFile.appendLine("    local-data: \\\"" + cname + " A " + iface.getAddress() + "\\\"");
							zoneFile.appendLine("    local-data: \\\"" + cname + " A " + iface.getAddress() + "\\\"");
						}
					}

				} catch (final InvalidMachineModelException e) {
					for (final HostName externalIP : zone.get(hostName)) {
						zoneFile.appendLine(
								"    local-data: \\\"" + hostName + " A " + externalIP.getAddress() + "\\\"");
					}
				}
			}

			units.add(zoneFile);
		}

		units.add(new RunningUnit("dns", "unbound", "unbound"));

		final FileUnit resolvConf = new FileUnit("dns_resolv_conf", "dns_running", "/etc/resolv.conf",
				"Unable to change your DNS to point at the local one.  This will probably cause VM building to fail, amongst other problems");
		units.add(resolvConf);
		resolvConf.appendLine("search " + getNetworkModel().getData().getDomain(getLabel()));
		resolvConf.appendLine("nameserver 10.0.0.1");

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws ARuntimeException {
		for (final HostName upstream : getNetworkModel().getData().getUpstreamDNSServers()) {
			getNetworkModel().getServerModel(getLabel()).addEgress(upstream);
		}

		return new HashSet<>();
	}

	@Override
	public Collection<IUnit> getLiveFirewall() throws ARuntimeException {
		return new HashSet<>();
	}

	@Override
	public void addRecord(String domain, String host, HostName... records) {
		Hashtable<String, Set<HostName>> zone = this.zones.get(domain);
		if (zone == null) {
			zone = new Hashtable<>();
		}

		Set<HostName> hosts = zone.get(host);
		if (hosts == null) {
			hosts = new HashSet<>();
		}

		hosts.addAll(new HashSet<>(Arrays.asList(records)));
		zone.put(host, hosts);
		this.zones.put(domain, zone);
	}

}
