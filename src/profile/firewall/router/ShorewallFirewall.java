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
import java.util.List;
import java.util.stream.Collectors;

import core.StringUtils;
import core.data.machine.AMachineData.MachineType;
import core.exception.runtime.ARuntimeException;
import core.iface.IUnit;
import core.model.machine.AMachineModel;
import core.model.machine.ExternalOnlyDeviceModel;
import core.model.machine.InternalOnlyDeviceModel;
import core.model.machine.ServerModel;
import core.model.machine.UserDeviceModel;
import core.model.machine.configuration.NetworkInterfaceModel;
import core.model.network.NetworkModel;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import inet.ipaddr.HostName;
import profile.firewall.AFirewallProfile;

/**
 * For more information on this Firewall, please see
 * http://shorewall.org/configuration_file_basics.htm
 */
public class ShorewallFirewall extends AFirewallProfile {
	public enum Action {
		ACCEPT, DNAT, DROP, REJECT, REDIRECT;
	}

	private static String CONFIG_BASEDIR = "/etc/shorewall";

	public ShorewallFirewall(String label, NetworkModel networkModel) {
		super(label, networkModel);
	}

	@Override
	public Collection<IUnit> getInstalled() throws ARuntimeException {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("shorewall", "proceed", "shorewall"));

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentConfig() throws ARuntimeException {
		final Collection<IUnit> units = new ArrayList<>();

		// Build our default policies
		final FileUnit policies = new FileUnit("shorewall_policies", "shorewall_installed", CONFIG_BASEDIR + "/policy");
		policies.appendLine("#Default policies to use for intra-zone communication");
		policies.appendLine("#For specific rules, please look at " + CONFIG_BASEDIR + "/rules");
		policies.appendLine("#Please see http://shorewall.net/manpages/shorewall-policy.html for more details");
		policies.appendLine("#source       destination action");
		policies.appendLine("wan           all+        DROP"); // DROP all ingress traffic
		policies.appendLine("all+          all+        REJECT"); // REJECT all other traffic
		units.add(policies);

		// Dedicate interfaces to zones - we'll just do it generically here (i.e. an
		// iface can be more than one zone), so we can be a bit more explicit in its
		// hosts file
		final FileUnit interfaces = new FileUnit("shorewall_interfaces", "shorewall_policies",
				CONFIG_BASEDIR + "/interfaces");
		interfaces.appendLine("#Dedicate interfaces to zones");
		interfaces.appendLine("#Please see http://shorewall.net/manpages/shorewall-interfaces.html for more details");
		interfaces.appendLine(
				"#If you're looking for how we assign zones to interfaces, please see " + CONFIG_BASEDIR + "/hosts");
		interfaces.appendLine("?FORMAT 2");
		interfaces.appendLine("#zone      interface      options");
		interfaces.appendLine("-          " + MachineType.SERVER.toString()
				+ "        dhcp,tcpflags,nosmurfs,routefilter,logmartians");
		interfaces.appendLine("-          " + MachineType.USER.toString()
				+ "          dhcp,tcpflags,nosmurfs,routefilter,logmartians");
		// TODO: Do we need this admin VLAN?
		interfaces.appendLine("-          " + MachineType.ADMIN.toString()
				+ "         dhcp,tcpflags,nosmurfs,routefilter,logmartians");
		interfaces.appendLine("-          " + MachineType.INTERNAL_ONLY.toString()
				+ "       dhcp,tcpflags,nosmurfs,routefilter,logmartians");
		interfaces.appendLine("-          " + MachineType.EXTERNAL_ONLY.toString()
				+ "       dhcp,tcpflags,nosmurfs,routefilter,logmartians");
		if (getNetworkModel().getData().buildAutoGuest()) {
			interfaces.appendLine("-          " + MachineType.GUEST.toString()
					+ "      dhcp,tcpflags,nosmurfs,routefilter,logmartians");
		}
		units.add(interfaces);

		return units;
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
	public Collection<IUnit> getLiveConfig() throws ARuntimeException {
		final Collection<IUnit> units = new ArrayList<>();

		// Build our zones
		final FileUnit zones = new FileUnit("shorewall_zones", "shorewall_installed", CONFIG_BASEDIR + "/zones");
		zones.appendLine("#This is the file which creates our various zones");
		zones.appendLine("#Please see http://shorewall.net/manpages/shorewall-zones.html for more details");
		zones.appendLine("#zone type");
		zones.appendLine("fw    firewall");
		zones.appendLine("wan   ipv4");
		zones.appendCarriageReturn();

		zones.appendLine("#Here, we build our server zone, and give each server its own subzone");
		zones.appendLine(cleanZone(MachineType.SERVER.toString()) + " ipv4");
		for (final String serverLabel : getNetworkModel().getServers().keySet()) {
			zones.appendLine(cleanZone(serverLabel) + ":" + cleanZone(MachineType.SERVER.toString()) + " ipv4");
		}
		zones.appendCarriageReturn();

		zones.appendLine("#Here, we build our user zone, and give each user their own subzone");
		zones.appendLine(cleanZone(MachineType.USER.toString()) + " ipv4");
		for (final String userLabel : getNetworkModel().getUserDevices().keySet()) {
			zones.appendLine(cleanZone(userLabel) + ":" + cleanZone(MachineType.USER.toString()) + " ipv4");
		}

		// TODO: Do we need an admin zone? Should it be sub-zoned too?
		zones.appendLine(
				cleanZone(MachineType.ADMIN.toString()) + ":" + cleanZone(MachineType.USER.toString()) + " ipv4");
		zones.appendCarriageReturn();

		zones.appendLine("#Here, we build our internal only zone, and give each device its own subzone");
		zones.appendLine(cleanZone(MachineType.INTERNAL_ONLY.toString()) + " ipv4");
		for (final String deviceLabel : getNetworkModel().getInternalOnlyDevices().keySet()) {
			zones.appendLine(cleanZone(deviceLabel) + ":" + cleanZone(MachineType.INTERNAL_ONLY.toString()) + " ipv4");
		}
		zones.appendCarriageReturn();

		zones.appendLine("#Here, we build our external only zone, and give each device its own subzone");
		zones.appendLine(cleanZone(MachineType.EXTERNAL_ONLY.toString()) + " ipv4");
		for (final String deviceLabel : getNetworkModel().getExternalOnlyDevices().keySet()) {
			zones.appendLine(cleanZone(deviceLabel) + ":" + cleanZone(MachineType.EXTERNAL_ONLY.toString()) + " ipv4");
		}

		// Do we want an autoguest network? Build its zone if so
		if (getNetworkModel().getData().buildAutoGuest()) {
			zones.appendLine(cleanZone(MachineType.GUEST.toString()) + ":"
					+ cleanZone(MachineType.EXTERNAL_ONLY.toString()) + " ipv4");
		}

		units.add(zones);

		// Now assign machines their (sub)zone
		final FileUnit hosts = new FileUnit("shorewall_hosts", "shorewall_interfaces", CONFIG_BASEDIR + "/hosts");
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

			hosts.appendLine(machine2Host(server, MachineType.SERVER));
		}

		for (final UserDeviceModel user : getNetworkModel().getUserDevices().values()) {
			hosts.appendLine(machine2Host(user, MachineType.USER));
		}

		for (final InternalOnlyDeviceModel device : getNetworkModel().getInternalOnlyDevices().values()) {
			hosts.appendLine(machine2Host(device, MachineType.INTERNAL_ONLY));
		}

		for (final ExternalOnlyDeviceModel device : getNetworkModel().getExternalOnlyDevices().values()) {
			hosts.appendLine(machine2Host(device, MachineType.EXTERNAL_ONLY));
		}
		units.add(hosts);

		// Finally, build our FW rules...
		final FileUnit rules = new FileUnit("shorewall_rules", "shorewall_hosts", CONFIG_BASEDIR + "/rules");
		rules.appendLine("#This file is just a list of includes, please see the rules_* files");
		rules.appendLine("#for per-subnet rules");
		units.add(rules);

		// Now iterate through our various zones and build them
		for (final MachineType zone : List.of(MachineType.ADMIN, MachineType.USER, MachineType.GUEST,
				MachineType.SERVER, MachineType.INTERNAL_ONLY, MachineType.EXTERNAL_ONLY)) {
			rules.appendLine("INCLUDE rules_" + zone.toString());

			final Collection<AMachineModel> machinesInZone = getNetworkModel().getMachines(zone).values();

			final FileUnit zoneRules = new FileUnit(zone.toString() + "_shorewall_rules", "shorewall_rules",
					CONFIG_BASEDIR + "/rules_" + zone.toString());
			zoneRules.appendLine("#This file lists our firewall rules for the " + zone.toString() + " zone");
			zoneRules.appendLine(
					"#ACTION       SOURCE       DEST       PROTO       DPORT       SPORT       ORIGINAL_DEST");

			for (final AMachineModel machine : machinesInZone) {
				for (final String label : machine.getDNAT().keySet()) {
					String source = null;
					String newDestination = null;
					String originalDestination = null;

					final String allIPs = machine.getNetworkInterfaces().stream()
							.map(i -> i.getAddress().withoutPrefixLength().toCompressedString())
							.collect(Collectors.joining(","));

					originalDestination = cleanZone(label) + ":" + allIPs;

					// If it's *this* machine, needs to be changed to the reserved $FW keyword.
					if (getNetworkModel().getServerModel(getLabel()).equals(machine)) {
						source = "all!$FW";
						newDestination = "$FW";
					} else {
						source = "all!" + cleanZone(machine.getLabel());
						newDestination = cleanZone(machine.getLabel()) + ":" + allIPs;
					}

					final String proto = "any";
					final String sport = "-";
					final String dport = machine.getDNAT().get(label).stream().map(i -> i.toString())
							.collect(Collectors.joining(","));

					zoneRules.appendLine(
							makeRule(Action.DNAT, source, newDestination, proto, dport, sport, originalDestination));
				}
			}

			zoneRules.appendCarriageReturn();
			zoneRules.appendLine("#Now let's move onto per-machine rules");

			for (final AMachineModel machine : machinesInZone) {
				zoneRules.appendLine("?COMMENT " + machine.getLabel());

				for (final HostName source : machine.getIngresses()) {
					zoneRules.appendLine(makeRule(Action.ACCEPT, "wan:" + source.getHost(),
							cleanZone(machine.getLabel()), "-", source.getPort().toString(), "-", "-"));
				}

				for (final HostName destination : machine.getEgresses()) {
					final Integer dport = destination.getPort();
					String dportString = null;
					if (dport == null) {
						dportString = "-";
					} else {
						dportString = dport.toString();
					}
					zoneRules.appendLine(makeRule(Action.ACCEPT, cleanZone(machine.getLabel()),
							"wan:" + destination.getHost(), "-", dportString, "-", "-"));
				}

				for (final String destination : machine.getForwards()) {
					zoneRules.appendLine(makeRule(Action.ACCEPT, cleanZone(machine.getLabel()),
							"wan:" + cleanZone(destination), "-", "-", "-", "-"));
				}

				zoneRules.appendLine("?COMMENT");
			}

			units.add(zoneRules);
		}

		return units;
	}

	private String makeRule(Action action, String source, String dest, String proto, String dport, String sport,
			String origDest) {
		String lines = "";

		if (origDest == null) {
			origDest = "-";
		}
		if (dport == null) {
			dport = "-";
		}
		if (sport == null) {
			sport = "-";
		}

		// Generate a descriptive comment
		lines += "?COMMENT " + action.toString() + " traffic from " + source + " to " + dest + ":" + dport;
		lines += (origDest != "-") ? " instead of " + origDest + "\n" : "\n";

		lines += action.toString() + " " + source + " " + dest + " " + proto + " " + dport + " " + sport + " "
				+ origDest + "\n";
		lines += "?COMMENT";

		return lines;
	}

	/**
	 * Parses a Machine into a shorewall host file
	 *
	 * @param machine
	 * @param zone
	 * @return
	 */
	private String machine2Host(AMachineModel machine, MachineType zone) {
		String nics = "";
		for (final NetworkInterfaceModel nic : machine.getNetworkInterfaces()) {
			nics += nic.getAddress().withoutPrefixLength().toCompressedString() + "/32,";
		}
		nics = nics.replaceAll(",$", ""); // Get rid of any trailing comma

		return cleanZone(machine.getLabel()) + " " + zone.toString() + ":" + nics;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws ARuntimeException {
		return new ArrayList<>();
	}

	@Override
	public Collection<IUnit> getLiveFirewall() throws ARuntimeException {
		return new ArrayList<>();
	}
}
