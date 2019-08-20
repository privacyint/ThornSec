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

import core.exception.runtime.ARuntimeException;
import core.iface.IUnit;
import core.model.network.NetworkModel;
import core.unit.SimpleUnit;
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

		units.add(new SimpleUnit("iptables_disabled", "proceed", "sudo systemctl disable iptables",
				"systemctl is-enabled iptables", "disabled", "pass"));

		units.add(new InstalledUnit("shorewall", "iptables_disabled", "shorewall"));

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
		policies.appendLine("wan           all         DROP"); // DROP all ingress traffic
		policies.appendLine("fw            all         REJECT"); // REJECT all traffic
		policies.appendLine("servers       all         REJECT"); // REJECT all traffic
		policies.appendLine("users         all         REJECT"); // REJECT all traffic
		policies.appendLine("admins        all         REJECT"); // REJECT all traffic
		policies.appendLine("internalOnlys all         REJECT"); // REJECT all traffic
		policies.appendLine("externalOnlys all         REJECT"); // REJECT all traffic
		units.add(policies);

		// Dedicate interfaces to zones - we'll just do it generically here (i.e. an
		// iface can be more than one zone), so we can be a bit more explicit in its
		// hosts file
		final FileUnit interfaces = new FileUnit("shorewall_interfaces", "shorewall_installed",
				CONFIG_BASEDIR + "/interfaces");
		interfaces.appendLine("#Dedicate interfaces to zones");
		interfaces.appendLine("#Please see http://shorewall.net/manpages/shorewall-interfaces.html for more details");
		interfaces.appendLine(
				"#If you're looking for how we are assigning zones, please see " + CONFIG_BASEDIR + "/hosts");
		interfaces.appendLine("?FORMAT 2");
		interfaces.appendLine("#zone       interface      options");
		// The below zone is currently a catch-all so we can create Routers with a
		// single iface...(!)
		// interfaces.appendLine("- " + getNetworkModel().getServerModel(getLabel()));
		// // TODO
		interfaces.appendLine("-           servers        detect tcpflags,nosmurfs,routefiulter,logmartians");
		interfaces.appendLine("-           users          detect dhcp,tcpflags,nosmurfs,routefiulter,logmartians");
		// TODO: Do we need this admin VLAN?
		interfaces.appendLine("-           admins         detect tcpflags,nosmurfs,routefiulter,logmartians");
		interfaces.appendLine("-           internalOnlys  detect dhcp,tcpflags,nosmurfs,routefiulter,logmartians");
		interfaces.appendLine("-           externalOnlys  detect dhcp,tcpflags,nosmurfs,routefiulter,logmartians");
		if (getNetworkModel().getData().buildAutoGuest()) {
			interfaces.appendLine("autoguest   autoguest      detect tcpflags,nosmurfs,routefiulter,logmartians");
		}
		units.add(interfaces);

		return units;
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

		// Build a sub-zone per server
		zones.appendLine("servers ipv4");
		for (final String serverLabel : getNetworkModel().getServers().keySet()) {
			zones.appendLine("servers:" + serverLabel + " ipv4");
		}

		// Build a sub-zone per user
		zones.appendLine("users ipv4");
		for (final String userLabel : getNetworkModel().getUserDevices().keySet()) {
			zones.appendLine("users:" + userLabel + " ipv4");
		}

		// TODO: Do we need an admin zone? Should it be sub-zoned too?
		zones.appendLine("admins:users ipv4");

		// Build a sub-zone per internal only device
		zones.appendLine("internalOnlys ipv4");
		for (final String deviceLabel : getNetworkModel().getInternalOnlyDevices().keySet()) {
			zones.appendLine("internalOnlys:" + deviceLabel + " ipv4");
		}

		// Build a sub-zone per external only device
		zones.appendLine("externalOnlys ipv4");
		for (final String deviceLabel : getNetworkModel().getExternalOnlyDevices().keySet()) {
			zones.appendLine("externalOnlys:" + deviceLabel + " ipv4");
		}

		// Do we want an autoguest network? Build its zone if so
		if (getNetworkModel().getData().buildAutoGuest()) {
			zones.appendLine("externalOnlys:autoguest ipv4");
		}

		units.add(zones);

		return units;
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
