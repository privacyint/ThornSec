/*
 * This code is part of the ThornSec project.
 * 
 * To learn more, please head to its GitHub repo: @privacyint
 * 
 * Pull requests encouraged.
 */
package profile.dns;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;

import core.iface.IUnit;
import core.model.machine.AMachineModel;
import core.model.machine.configuration.NetworkInterfaceModel;
import core.model.network.NetworkModel;

import core.unit.SimpleUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;

import core.exception.data.machine.InvalidMachineException;
import core.exception.data.machine.InvalidServerException;

import core.exception.runtime.ARuntimeException;
import core.exception.runtime.InvalidMachineModelException;
import core.exception.runtime.InvalidServerModelException;

public class UnboundDNSServer extends ADNSServerProfile {

	private static String UNBOUND_CONFIG_FILE_PATH = "/etc/unbound/unbound.conf";
	
	private Hashtable<String, Hashtable<String, Set<HostName>>> zones;
	
	public UnboundDNSServer(String label, NetworkModel networkModel) {
		super(label, networkModel);
		
		zones = null;
	}
	
	public Set<IUnit> getPersistentConfig()
	throws InvalidServerModelException, InvalidServerException {
		Integer cpus = networkModel.getData().getCpus(getLabel());
		Set<IUnit> units = new HashSet<IUnit>();

		//Config originally based on https://calomel.org/unbound_dns.html
		//See https://linux.die.net/man/5/unbound.conf for full config file
		networkModel.getServerModel(getLabel()).addConfigFile(UNBOUND_CONFIG_FILE_PATH);

		FileUnit unboundConf = new FileUnit("unbound_conf", "dns_installed", UNBOUND_CONFIG_FILE_PATH);
		unboundConf.appendLine("server:");
		//Force dropping user post-invocation 
		unboundConf.appendLine("    username: unbound");
		unboundConf.appendLine("    verbosity: 1");
		unboundConf.appendLine("    directory: \\\"/etc/unbound\\\"");
		//Stick it in a chroot. DNS is dangerous.
		unboundConf.appendLine("    chroot: \\\"/etc/unbound\\\"");
		unboundConf.appendLine("    pidfile: \\\"/etc/unbound/unbound.pid\\\"");
		//Only listen to lan/loopback traffic
		unboundConf.appendLine("    interface: 127.0.0.1");
		unboundConf.appendLine("    interface: 10.0.0.1");
		unboundConf.appendLine("    access-control: 127.0.0.0/8 allow");
		unboundConf.appendLine("    access-control: 10.0.0.0/8 allow");
		//Upstream DNS isn't allowed to point somewhere internal
		//Also stops DNS Rebinding attacks.
		unboundConf.appendLine("    private-address: 10.0.0.0/8");
		//Don't listen to anything else.
		unboundConf.appendLine("    access-control: 0.0.0.0/0 refuse");
		//Listen on :53
		unboundConf.appendLine("    port: 53");
		//Do TCP/UDP, IPv4 only
		unboundConf.appendLine("    do-tcp: yes");
		unboundConf.appendLine("    do-udp: yes");
		unboundConf.appendLine("    do-ip4: yes");
		//No IPv6, please.
		unboundConf.appendLine("    do-ip6: no");
		//Add some DNS hardening
		unboundConf.appendLine("    hide-identity: yes");
		unboundConf.appendLine("    hide-version: yes");
		unboundConf.appendLine("    harden-glue: yes");
		unboundConf.appendLine("    harden-dnssec-stripped: yes");
		unboundConf.appendLine("    use-caps-for-id: yes");
		//Add some response hardening
		unboundConf.appendLine("    unwanted-reply-threshold: 10000");
		unboundConf.appendLine("    do-not-query-localhost: no");
		unboundConf.appendLine("    val-clean-additional: yes");
		//Add some performance enhancements
		unboundConf.appendLine("    cache-min-ttl: 3600");
		unboundConf.appendLine("    cache-max-ttl: 86400");
		unboundConf.appendLine("    prefetch: yes");
		//Add sensible values based on the number of CPUs in your machine
		unboundConf.appendLine("    num-threads: " + cpus);
		unboundConf.appendLine("    msg-cache-slabs: " + (cpus * 2));
		unboundConf.appendLine("    rrset-cache-slabs: " + (cpus * 2));
		unboundConf.appendLine("    infra-cache-slabs: " + (cpus * 2));
		unboundConf.appendLine("    key-cache-slabs: " + (cpus * 2));
		unboundConf.appendLine("    rrset-cache-size: " + (cpus / 4) + "m");
		unboundConf.appendLine("    msg-cache-size: " + (cpus / 8) + "m");
		unboundConf.appendLine("    so-rcvbuf: 1m");
		//Only switch on blocking if the user actually wants it... 
		if (networkModel.getData().getAdBlocking()) {
			unboundConf.appendLine("    include: \\\"/etc/unbound/unbound.conf.d/adblock.zone\\\"");
		}
		//rDNS
		unboundConf.appendLine("    local-zone: \\\"10.in-addr.arpa.\\\" nodefault");
		unboundConf.appendLine("    stub-zone:");
		unboundConf.appendLine("        name: \\\"10.in-addr.arpa.\\\"");
		unboundConf.appendLine("        stub-addr: 10.0.0.1");
		//Zone related stuff
		for (String zone : zones.keySet()) {
			unboundConf.appendLine("    private-domain: \\\"" + zone + "\\\"");
			unboundConf.appendLine("    include: \\\"/etc/unbound/unbound.conf.d/" + zone + ".zone\\\"");
		}
		//Upstream DNS servers
		unboundConf.appendLine("    forward-zone:");
		unboundConf.appendLine("        name: \\\".\\\"");
		if (networkModel.getData().getUpstreamDNSIsTLS()) {
			unboundConf.appendLine("        forward-ssl-upstream: yes");
		}
		for (IPAddress upstream : networkModel.getData().getUpstreamDNSServers()) {
			if (networkModel.getData().getUpstreamDNSIsTLS()) {
				unboundConf.appendLine("        forward-addr: " + upstream.toIPv4() + "@853");
			}
			else {
				unboundConf.appendLine("        forward-addr: " + upstream.toIPv4());
			}
		}
		
		return units;
	}

	public Set<IUnit> getInstalled()
	throws InvalidServerModelException {
		Set<IUnit> units = new HashSet<IUnit>();
		
		units.add(new InstalledUnit("dns", "unbound"));
		networkModel.getServerModel(getLabel()).addSystemUsername("unbound");
		networkModel.getServerModel(getLabel()).addProcessString("/usr/sbin/unbound -d$");

		if (networkModel.getData().getAdBlocking()) {
			units.add(new InstalledUnit("ca_certificates", "ca-certificates"));
		}
		
		return units;
	}
	

	public Set<IUnit> getLiveConfig()
	throws InvalidMachineException, InvalidServerModelException {
		Set<IUnit> units = new HashSet<IUnit>();
		
		//Start by updating the ad block list (if req'd)
		if (networkModel.getData().getAdBlocking()) {
			units.add(new SimpleUnit("adblock_up_to_date", "proceed",
					"sudo wget -O /etc/unbound/rawhosts https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"
					+ " && cat /etc/unbound/rawhosts | grep '^0\\.0\\.0\\.0'"
					+ " | awk '{print \"local-zone: \\\"\"$2\"\\\" redirect\\nlocal-data: \\\"\"$2\" A 0.0.0.0\\\"\"}'"
					+ " | sudo tee /etc/unbound/unbound.conf.d/adblock.zone > /dev/null"
					+ " && sudo service unbound restart",				
					"[ ! -f /etc/unbound/rawhosts ] && echo fail || wget -O - -o /dev/null https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts | cmp /etc/unbound/rawhosts 2>&1", "", "pass"));
		}
		
		//Now make sure all of the various zones are there & up to date
		for (String domain : zones.keySet()) {
			FileUnit zoneFile = new FileUnit(domain + "_dns_internal_zone", "dns_installed", "/etc/unbound/unbound.conf.d/" + domain + ".zone");
			zoneFile.appendLine("    local-zone: \\\"" + domain + ".\\\" typetransparent"); //Typetransparent passes resolution upwards if not found locally
	
			Hashtable<String, Set<HostName>> zone = zones.get(domain);
			AMachineModel hostMachine = null;
			
			for (String hostName : zone.keySet()) {
				//It may not be a real machine. It might be a poison. Deal with it.
				try {
					hostMachine = networkModel.getMachineModel(hostName);

					for (String ifaceName : hostMachine.getLANInterfaces().keySet()) {
						NetworkInterfaceModel iface = hostMachine.getLANInterface(ifaceName);
						
						zoneFile.appendLine("    local-data-ptr: \\\"" + iface.getAddress() + " " + hostMachine.getFQDN() + "\\\"");
						zoneFile.appendLine("    local-data-ptr: \\\"" + iface.getGateway() + " router." + hostMachine.getFQDN() + "\\\"");
						
						for (HostName cname : hostMachine.getCNames()) {
							zoneFile.appendLine("    local-data: \\\"" + cname.getHost() + " A " + iface.getAddress() + "\\\"");
							zoneFile.appendLine("    local-data: \\\"" + cname.toString() + " A " + iface.getAddress() + "\\\"");
						}
					}
					
				}
				catch (InvalidMachineModelException e) {
					for (HostName externalIP : zone.get(hostName)) {
						zoneFile.appendLine("    local-data: \\\"" + hostName + " A " + externalIP.getAddress() + "\\\"");
					}
				}
			}
			
			units.add(zoneFile);
		}
		
		units.add(new RunningUnit("dns", "unbound", "unbound"));
		
		FileUnit resolvConf = new FileUnit("dns_resolv_conf", "dns_running", "/etc/resolv.conf",
				"Unable to change your DNS to point at the local one.  This will probably cause VM building to fail, amongst other problems");
		resolvConf.appendLine("search " + networkModel.getData().getFQDN(getLabel()));
		resolvConf.appendLine("nameserver 10.0.0.1");
		
		units.add(resolvConf);
		
		networkModel.getServerModel(getLabel()).addConfigFile("/etc/resolv.conf");
		
		return units;
	}

	@Override
	public Set<IUnit> getPersistentFirewall() throws ARuntimeException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<IUnit> getLiveFirewall() throws ARuntimeException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addRecord(String domain, String host, HostName... records) {
		Hashtable<String, Set<HostName>> zone = zones.get(domain);
		if (zone == null) { zone = new Hashtable<String, Set<HostName>>(); }
		
		Set<HostName> hosts = zone.get(host);
		if (hosts == null) { hosts = new HashSet<HostName>(); }
		
		hosts.addAll(new HashSet<HostName>(Arrays.asList(records)));
		zone.put(host, hosts);
		zones.put(domain, zone);
	}

}
