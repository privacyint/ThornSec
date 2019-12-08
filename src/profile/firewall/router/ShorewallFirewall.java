/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.firewall.router;

import java.util.ArrayList;
import java.util.Collection;

import core.StringUtils;
import core.data.machine.AMachineData.Encapsulation;
import core.data.machine.AMachineData.MachineType;
import core.exception.AThornSecException;
import core.exception.runtime.ARuntimeException;
import core.iface.IUnit;
import core.model.machine.AMachineModel;
import core.model.machine.ExternalOnlyDeviceModel;
import core.model.machine.InternalOnlyDeviceModel;
import core.model.machine.ServerModel;
import core.model.machine.UserDeviceModel;
import core.model.machine.configuration.networking.ISystemdNetworkd;
import core.model.machine.configuration.networking.NetworkInterfaceModel;
import core.model.network.NetworkModel;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;
import profile.firewall.AFirewallProfile;

/**
 * For more information on this Firewall, please see
 * http://shorewall.org/configuration_file_basics.htm
 */
public class ShorewallFirewall extends AFirewallProfile {
	public enum Action {
		ACCEPT, DNAT, DROP, REJECT, REDIRECT;
	}

	public enum ParentZone {
		INTERNET("Internet"), ROUTER("Router"), USERS("Users"), ADMINS("Admins"), SERVERS("Servers"), INTERNAL_ONLY("InternalOnly"), EXTERNAL_ONLY("ExternalOnly"),
		GUESTS("Guests");

		private String parentZone;

		ParentZone(String parentZone) {
			this.parentZone = parentZone;
		}

		@Override
		public String toString() {
			return this.parentZone;
		}
	}

	private static String CONFIG_BASEDIR = "/etc/shorewall";

	public ShorewallFirewall(String label, NetworkModel networkModel) {
		super(label, networkModel);
	}

	/**
	 * Zones must be a maximum of 10 alpha-numeric chars long
	 *
	 * @param zone
	 * @return valid zone name
	 */
	private String cleanZone(String zone) {
		zone = StringUtils.stringToAlphaNumeric(zone);

		if (zone.length() > 10) {
			zone = zone.substring(0, 9);
		}

		return zone;
	}

	@Override
	public Collection<IUnit> getInstalled() throws ARuntimeException {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("shorewall", "proceed", "shorewall"));

		return units;
	}

	@Override
	public Collection<IUnit> getLiveConfig() throws ARuntimeException {
		final Collection<IUnit> units = new ArrayList<>();

		// Build our zones
		final FileUnit zones = new FileUnit("shorewall_zones", "shorewall_installed", CONFIG_BASEDIR + "/zones");
		zones.appendLine("#This is the file which creates our various zones");
		zones.appendLine("#Please see http://shorewall.net/manpages/shorewall-zones.html for more details");
		zones.appendLine("#zone\ttype");
		zones.appendLine(cleanZone(ParentZone.INTERNET.toString()) + "\tipv4");
		zones.appendLine(cleanZone(ParentZone.ROUTER.toString()) + "\tfirewall");
		zones.appendCarriageReturn();

		zones.appendLine("#Here, we build our server zone, and give each server its own subzone");
		zones.appendLine(cleanZone(ParentZone.SERVERS.toString()) + "\tipv4");
		for (final String serverLabel : getNetworkModel().getServers().keySet()) {
			zones.appendLine(cleanZone(serverLabel) + ":" + cleanZone(ParentZone.SERVERS.toString()) + "\tipv4");
		}
		zones.appendCarriageReturn();

		zones.appendLine("#Here, we build our user zone, and give each user their own subzone");
		zones.appendLine("Users\tipv4");
		for (final String userLabel : getNetworkModel().getUserDevices().keySet()) {
			zones.appendLine(cleanZone(userLabel) + ":" + cleanZone(ParentZone.USERS.toString()) + "\tipv4");
		}

		// TODO: Do we need an admin zone? Should it be sub-zoned too?
		zones.appendLine(cleanZone(ParentZone.ADMINS.toString()) + ":" + cleanZone(ParentZone.USERS.toString()) + "\tipv4");
		zones.appendCarriageReturn();

		zones.appendLine("#Here, we build our internal only zone, and give each device its own subzone");
		zones.appendLine(cleanZone(ParentZone.INTERNAL_ONLY.toString()) + "\tipv4");
		for (final String deviceLabel : getNetworkModel().getInternalOnlyDevices().keySet()) {
			zones.appendLine(cleanZone(deviceLabel) + ":" + cleanZone(ParentZone.INTERNAL_ONLY.toString()) + "\tipv4");
		}
		zones.appendCarriageReturn();

		zones.appendLine("#Here, we build our external only zone, and give each device its own subzone");
		zones.appendLine(cleanZone(ParentZone.EXTERNAL_ONLY.toString()) + "\tipv4");
		for (final String deviceLabel : getNetworkModel().getExternalOnlyDevices().keySet()) {
			zones.appendLine(cleanZone(deviceLabel) + ":" + cleanZone(ParentZone.EXTERNAL_ONLY.toString()) + "\tipv4");
		}

		// Do we want an autoguest network? Build its zone if so
		if (getNetworkModel().getData().buildAutoGuest()) {
			zones.appendLine(cleanZone(ParentZone.GUESTS.toString()) + "\tipv4");
		}

		units.add(zones);

		// Now assign machines their (sub)zone, and enforce our maclist
		final FileUnit hosts = new FileUnit("shorewall_hosts", "shorewall_interfaces", CONFIG_BASEDIR + "/hosts");
		final FileUnit maclist = new FileUnit("shorewall_maclist", "shorewall_hosts", CONFIG_BASEDIR + "/maclist");

		hosts.appendLine("#Please see http://shorewall.net/manpages/shorewall-zones.html for more details");
		hosts.appendLine("#zone      hosts          options");

		for (final ServerModel server : getNetworkModel().getServers().values()) {
			try {
				// We're a router, so we're a special case!
				if (server.isRouter()) {
					continue;
				}
			} catch (final Exception e) {
				continue;
			}

			hosts.appendLine(machine2Host(server, ParentZone.SERVERS));

			for (final NetworkInterfaceModel nic : server.getNetworkInterfaces().values()) {
				// TODO
				maclist.appendLine("ACCEPT\t" + ParentZone.SERVERS + "\t" + nic.getMac() + "\t" + nic.getAddresses() + "\t#" + server.getLabel());
			}
		}

		for (final UserDeviceModel user : getNetworkModel().getUserDevices().values()) {
			hosts.appendLine(machine2Host(user, ParentZone.USERS));
			for (final NetworkInterfaceModel nic : user.getNetworkInterfaces().values()) {
				// TODO
				maclist.appendLine("ACCEPT\t" + ParentZone.USERS + "\t" + nic.getMac() + "\t" + nic.getAddresses() + "\t#" + user.getLabel());
			}
		}

		for (final InternalOnlyDeviceModel device : getNetworkModel().getInternalOnlyDevices().values()) {
			hosts.appendLine(machine2Host(device, ParentZone.INTERNAL_ONLY));
			for (final NetworkInterfaceModel nic : device.getNetworkInterfaces().values()) {
				// TODO
				maclist.appendLine("ACCEPT\t" + ParentZone.INTERNAL_ONLY + "\t" + nic.getMac() + "\t" + nic.getAddresses() + "\t#" + device.getLabel());
			}
		}

		for (final ExternalOnlyDeviceModel device : getNetworkModel().getExternalOnlyDevices().values()) {
			hosts.appendLine(machine2Host(device, ParentZone.EXTERNAL_ONLY));
			for (final NetworkInterfaceModel nic : device.getNetworkInterfaces().values()) {
				// TODO
				maclist.appendLine("ACCEPT\t" + ParentZone.EXTERNAL_ONLY + "\t" + nic.getMac() + "\t" + nic.getAddresses() + "\t#" + device.getLabel());
			}
		}
		units.add(hosts);
		units.add(maclist);

		// Finally, build our FW rules...
		final FileUnit rules = new FileUnit("shorewall_rules", "shorewall_hosts", CONFIG_BASEDIR + "/rules");
		rules.appendLine("#This file is just a list of includes, please see the rules_* files");
		rules.appendLine("#for per-subnet rules");
		units.add(rules);

//		// Now iterate through our various zones and build them
//		for (final MachineType zone : List.of(MachineType.ADMIN, MachineType.USER, MachineType.GUEST, MachineType.SERVER, MachineType.INTERNAL_ONLY, MachineType.EXTERNAL_ONLY)) {
//			rules.appendLine("INCLUDE rules_" + zone.toString());
//
//			final Collection<AMachineModel> machinesInZone = getNetworkModel().getMachines(zone).values();
//
//			final FileUnit zoneRules = new FileUnit(zone.toString() + "_shorewall_rules", "shorewall_rules", CONFIG_BASEDIR + "/rules_" + zone.toString());
//			zoneRules.appendLine("#This file lists our firewall rules for the " + zone.toString() + " zone");
//			zoneRules.appendLine("#ACTION       SOURCE       DEST[zone:ip]       PROTO       DPORT       SPORT       ORIGINAL_DEST[ip]");
//
//			for (final AMachineModel machine : machinesInZone) {
//				for (final String label : machine.getDNAT().keySet()) {
//					String source = null;
//					String dest = null;
//					String origDest = null;
//
//					final String allIPs = getNetworkModel().getMachineModel(label).getNetworkInterfaces().stream()
//							.map(i -> i.getAddress().withoutPrefixLength().toCompressedString()).collect(Collectors.joining(","));
//
//					origDest = allIPs;
//
//					// If it's *this* machine, needs to be changed to the reserved $FW keyword.
//					if (getNetworkModel().getServerModel(getLabel()).equals(machine)) {
//						source = "all!\\\\$FW";
//						dest = "\\\\$FW";
//					} else {
//						source = "all!" + cleanZone(machine.getLabel());
//						dest = cleanZone(machine.getLabel()) + ":" + allIPs;
//					}
//
//					final String proto = Encapsulation.TCP.toString().toLowerCase();
//					final String sport = "-";
//					final String dport = machine.getDNAT().get(label).stream().map(i -> i.toString()).collect(Collectors.joining(","));
//
//					zoneRules.appendLine(makeRule(Action.DNAT, source, dest, proto, dport, sport, origDest));
//				}
//			}
//
//			zoneRules.appendCarriageReturn();
//			zoneRules.appendLine("#Now let's move onto per-machine rules");
//
//			for (final AMachineModel machine : machinesInZone) {
//				zoneRules.appendLine("?COMMENT " + machine.getLabel());
//
//				// Ingresses
//				for (final HostName source : machine.getIngresses()) {
//					zoneRules.appendLine(makeRule(Action.ACCEPT, "Internet:" + source.getHost(), cleanZone(machine.getLabel()), Encapsulation.TCP.toString().toLowerCase(),
//							source.getPort().toString(), "-", "-"));
//				}
//
//				// Egresses
//				for (final HostName destination : machine.getEgresses()) {
//					final Integer dport = destination.getPort();
//					String dportString = null;
//					// TODO: this is hacky.
//					String dest = destination.getHost();
//					final long count = dest.chars().filter(ch -> ch == '.').count();
//					if (count == 1) {
//						dest += ".";
//					}
//
//					if (dport == null) {
//						dportString = "-";
//					} else {
//						dportString = dport.toString();
//					}
//					zoneRules.appendLine(makeRule(Action.ACCEPT, cleanZone(machine.getLabel()), "wan:" + dest, Encapsulation.TCP.toString().toLowerCase(), dportString, "-", "-"));
//				}
//
//				// Forwards
//				for (final String destination : machine.getForwards()) {
//					zoneRules.appendLine(makeRule(Action.ACCEPT, cleanZone(machine.getLabel()), cleanZone(destination), Encapsulation.TCP.toString().toLowerCase(), "-", "-", "-"));
//				}
//
//				// TODO: listen rules
//
//				zoneRules.appendLine("?COMMENT");
//			}
//
//			units.add(zoneRules);
//		}

		return units;
	}

	@Override
	public Collection<IUnit> getLiveFirewall() throws ARuntimeException {
		return new ArrayList<>();
	}

	@Override
	public Collection<IUnit> getPersistentConfig() throws ARuntimeException {
		final Collection<IUnit> units = new ArrayList<>();

		// Build our default policies
		final FileUnit policies = new FileUnit("shorewall_policies", "shorewall_installed", CONFIG_BASEDIR + "/policy");
		policies.appendLine("#Default policies to use for intra-zone communication");
		policies.appendLine("#For specific rules, please look at " + CONFIG_BASEDIR + "/rules");
		policies.appendLine("#Please see http://shorewall.net/manpages/shorewall-policy.html for more details");
		policies.appendLine("#source       destination  action");
		policies.appendLine("Internet      all+         DROP"); // DROP all ingress traffic
		policies.appendLine("all+          all+         REJECT"); // REJECT all other traffic
		units.add(policies);

		// Dedicate interfaces to parent zones
		final FileUnit interfaces = new FileUnit("shorewall_interfaces", "shorewall_policies", CONFIG_BASEDIR + "/interfaces");
		interfaces.appendLine("#Dedicate interfaces to parent zones");
		interfaces.appendLine("#Please see http://shorewall.net/manpages/shorewall-interfaces.html for more details");
		interfaces.appendLine("#zone\tinterface\tbroadcast\toptions");
		interfaces.appendLine(cleanZone(ParentZone.SERVERS.toString()) + "\t" + MachineType.SERVER.toString() + "\t-\tdhcp,routefilter,arp_filter");
		interfaces.appendLine(cleanZone(ParentZone.USERS.toString()) + "\t" + MachineType.USER.toString() + "\t-\tdhcp,routefilter,arp_filter");
		interfaces.appendLine(cleanZone(ParentZone.ADMINS.toString()) + "\t" + MachineType.ADMIN.toString() + "\t-\tdhcp,routefilter,arp_filter");
		interfaces.appendLine(cleanZone(ParentZone.INTERNAL_ONLY.toString()) + "\t" + MachineType.INTERNAL_ONLY.toString() + "\t-\tdhcp,routefilter,arp_filter");
		interfaces.appendLine(cleanZone(ParentZone.EXTERNAL_ONLY.toString()) + "\t" + MachineType.EXTERNAL_ONLY.toString() + "\t-\tdhcp,routefilter,arp_filter");
		if (getNetworkModel().getData().buildAutoGuest()) {
			interfaces.appendLine(cleanZone(ParentZone.GUESTS.toString()) + "\t" + MachineType.GUEST.toString() + "\t-\tdhcp,routefilter,arp_filter");
		}
		units.add(interfaces);

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws AThornSecException {
		for (final HostName upstreamDNSServer : getNetworkModel().getData().getUpstreamDNSServers()) {
			getNetworkModel().getServerModel(getLabel()).addEgress(upstreamDNSServer);

		}

		getNetworkModel().getServerModel(getLabel()).addListen(Encapsulation.TCP, 53);
		getNetworkModel().getServerModel(getLabel()).addListen(Encapsulation.UDP, 53);

		return new ArrayList<>();
	}

	/**
	 * Parses a Machine into a shorewall host file
	 *
	 * @param machine
	 * @param zone
	 * @return
	 */
	private String machine2Host(AMachineModel machine, ParentZone zone) {
		String nics = "";
		for (final ISystemdNetworkd nic : machine.getNetworkInterfaces().values()) {
			for (final IPAddress ip : nic.getAddresses()) {
				nics += ip.withoutPrefixLength().toCompressedString() + "/32,";
			}
		}
		nics = nics.replaceAll(",$", ""); // Get rid of any trailing comma

		return cleanZone(machine.getLabel()) + " " + zone.toString() + ":" + nics + "\tmaclist";
	}
}
