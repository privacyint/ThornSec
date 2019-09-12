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
import profile.firewall.AFirewallProfile;

public class ShorewallFirewall extends AFirewallProfile {

	private static String CONFIG_BASEDIR = "/etc/shorewall";

	public ShorewallFirewall(String label, NetworkModel networkModel) {
		super(label, networkModel);
	}

	@Override
	public Collection<IUnit> getInstalled() throws ARuntimeException {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("shorewall", "proceed", "shorewall"));
		// units.add(new DirUnit("shorewall_policies", "shorewall_installed",
		// CONFIG_BASEDIR + "/policy.d"));

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
		interfaces.appendLine("-          servers        dhcp,tcpflags,nosmurfs,routefilter,logmartians");
		// single iface...(!)
		// interfaces.appendLine("- " + getNetworkModel().getServerModel(getLabel()));
		// // TODO
		interfaces.appendLine("-          users          dhcp,tcpflags,nosmurfs,routefilter,logmartians");
		// TODO: Do we need this admin VLAN?
		interfaces.appendLine("-          admins         dhcp,tcpflags,nosmurfs,routefilter,logmartians");
		interfaces.appendLine("-          internal       dhcp,tcpflags,nosmurfs,routefilter,logmartians");
		interfaces.appendLine("-          external       dhcp,tcpflags,nosmurfs,routefilter,logmartians");
		if (getNetworkModel().getData().buildAutoGuest()) {
			interfaces.appendLine("-          autoguest      dhcp,tcpflags,nosmurfs,routefilter,logmartians");
		}
		units.add(interfaces);

		return units;
	}

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

		zones.appendLine("#Here, we build our server zone, and give each server its own subzone");
		zones.appendLine("servers ipv4");
		for (final String serverLabel : getNetworkModel().getServers().keySet()) {
			zones.appendLine(cleanZone(serverLabel) + ":servers ipv4");
		}

		zones.appendLine("#Here, we build our user zone, and give each user their own subzone");
		zones.appendLine("users ipv4");
		for (final String userLabel : getNetworkModel().getUserDevices().keySet()) {
			zones.appendLine(cleanZone(userLabel) + ":users ipv4");
		}

		// TODO: Do we need an admin zone? Should it be sub-zoned too?
		zones.appendLine("admins:users ipv4");

		zones.appendLine("#Here, we build our internal only zone, and give each device its own subzone");
		zones.appendLine("internal ipv4");
		for (final String deviceLabel : getNetworkModel().getInternalOnlyDevices().keySet()) {
			zones.appendLine(cleanZone(deviceLabel) + ":internal ipv4");
		}

		zones.appendLine("#Here, we build our external only zone, and give each device its own subzone");
		zones.appendLine("external ipv4");
		for (final String deviceLabel : getNetworkModel().getExternalOnlyDevices().keySet()) {
			zones.appendLine(cleanZone(deviceLabel) + ":external ipv4");
		}

		// Do we want an autoguest network? Build its zone if so
		if (getNetworkModel().getData().buildAutoGuest()) {
			zones.appendLine("autoguest:external ipv4");
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

			hosts.appendLine(machine2Host(server, "servers"));
		}

		for (final UserDeviceModel user : getNetworkModel().getUserDevices().values()) {
			hosts.appendLine(machine2Host(user, "users"));
		}

		for (final InternalOnlyDeviceModel device : getNetworkModel().getInternalOnlyDevices().values()) {
			hosts.appendLine(machine2Host(device, "internal"));
		}

		for (final ExternalOnlyDeviceModel device : getNetworkModel().getExternalOnlyDevices().values()) {
			hosts.appendLine(machine2Host(device, "external"));
		}
		units.add(hosts);

		// Finally, build our FW rules...
		final FileUnit rules = new FileUnit("shorewall_rules", "shorewall_hosts", CONFIG_BASEDIR + "/rules");
		rules.appendLine("#This is where we do our specific machine-based rules");
		rules.appendLine("#Please see http://shorewall.net/manpages/shorewall-rules.html for more details");
		rules.appendLine("#ACTION       SOURCE       DEST       PROTO       DPORT       SPORT       ORIGINAL_DEST");

		// Let's start with our DNAT
		for (final MachineType type : getNetworkModel().getMachines().keySet()) {
			for (final AMachineModel machine : getNetworkModel().getMachines(type).values()) {
				final Map<String, Collection<Integer>> dnat = machine.getDNAT();

				for (final String destination : dnat.keySet()) {
					rules.appendLine(makeRule("DNAT", "users", cleanZone(machine.getLabel()), Encapsulation.TCP,
							dnat.get(destination), "any", destination));
				}

			}
		}

		units.add(rules);

		return units;
	}

	private String makeRule(String action, String source, String destination, Encapsulation protocol,
			Collection<Integer> dports, String sport, String originalDestination) {

		final String dportsString = dports.stream().map(Object::toString).collect(Collectors.joining(","));

		return action + "    " + source + "    " + destination + "    " + protocol.toString() + "    " + dportsString
				+ "    " + sport + "    " + originalDestination;
	}

	private String machine2Host(AMachineModel machine, String zone) {
		String nics = "";
		for (final NetworkInterfaceModel nic : machine.getNetworkInterfaces()) {
			nics += nic.getAddress().withoutPrefixLength().toCompressedString() + "/32,";
		}
		nics = nics.replaceAll(",$", ""); // Get rid of any trailing comma

		return cleanZone(machine.getLabel()) + " " + zone + ":" + nics;
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
