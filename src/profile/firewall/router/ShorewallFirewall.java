/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.firewall.router;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.stream.JsonParsingException;

import core.StringUtils;
import core.data.machine.AMachineData.MachineType;
import core.data.machine.configuration.NetworkInterfaceData.Direction;
import core.data.machine.configuration.NetworkInterfaceData.Inet;
import core.exception.AThornSecException;
import core.exception.data.ADataException;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.ARuntimeException;
import core.iface.IUnit;
import core.model.machine.AMachineModel;
import core.model.network.NetworkModel;
import core.unit.fs.FileEditUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
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
		INTERNET("Internet"), ROUTER("Router"), USERS("Users"), ADMINS("Admins"), SERVERS("Servers"), INTERNAL_ONLY("InternalOnlys"), EXTERNAL_ONLY("ExternalOnlys"), GUESTS("Guests");

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
			zone = zone.substring(0, 10);
		}

		return zone;
	}

	private String cleanZone(ParentZone zone) {
		return cleanZone(zone.toString());
	}

	@Override
	public Collection<IUnit> getInstalled() throws ARuntimeException {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("shorewall", "proceed", "shorewall"));

		return units;
	}

	private Collection<String> getMaclistFile() {
		final Collection<String> maclist = new ArrayList<>();

		// Servers are the only ones we want to iterate, since we need to check for
		// Router machines
		getNetworkModel().getServers().values().forEach(server -> {
			try {
				if (!server.isRouter()) { // Ignore routers
					maclist.addAll(machines2Maclist(MachineType.SERVER, server));
				}
			} catch (final InvalidServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

		maclist.addAll(machines2Maclist(MachineType.USER, getNetworkModel().getUserDevices().values().toArray(AMachineModel[]::new)));
		maclist.addAll(machines2Maclist(MachineType.INTERNAL_ONLY, getNetworkModel().getInternalOnlyDevices().values().toArray(AMachineModel[]::new)));
		maclist.addAll(machines2Maclist(MachineType.EXTERNAL_ONLY, getNetworkModel().getExternalOnlyDevices().values().toArray(AMachineModel[]::new)));

		return maclist;
	}

	private Collection<String> getHostsFile() {
		final Collection<String> hosts = new ArrayList<>();

		hosts.add("#Please see http://shorewall.net/manpages/shorewall-zones.html for more details");
		hosts.add("#zone      hosts          options");

		// Servers are the only ones we want to iterate, since we need to check for
		// Router machines
		getNetworkModel().getServers().values().forEach(server -> {
			try {
				if (!server.isRouter()) { // Ignore routers
					hosts.addAll(machines2Host(ParentZone.SERVERS, server));
				}
			} catch (final InvalidServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

		hosts.addAll(machines2Host(ParentZone.USERS, getNetworkModel().getUserDevices().values().toArray(AMachineModel[]::new)));
		hosts.addAll(machines2Host(ParentZone.INTERNAL_ONLY, getNetworkModel().getUserDevices().values().toArray(AMachineModel[]::new)));
		hosts.addAll(machines2Host(ParentZone.EXTERNAL_ONLY, getNetworkModel().getUserDevices().values().toArray(AMachineModel[]::new)));

		return hosts;
	}

	private Collection<String> getRulesFile() {
		final Collection<String> rules = new ArrayList<>();

		// Iterate over every machine
		getNetworkModel().getUniqueMachines().forEach((label, machine) -> {
			// Start by building this machine's egresses.
			machine.getEgresses().forEach(egress -> {
				String line = "";

				line += "ACCEPT\t";
				line += cleanZone(label) + "\t";
				line += ParentZone.INTERNET + ":" + egress.getHost();
				// If it's a HostName rather than IP address make it "FQDN"-ish by appending "."
				if (!egress.isAddress()) {
					line += ".";
				}

				if (egress.getPort() != null) {
					line += "\ttcp\t";
					line += egress.getPort();
				}

				rules.add(line);
			});
		});

		return rules;
	}

	private Collection<String> getZonesFile() {
		// Build our zones
		final Collection<String> zones = new ArrayList<>();
		zones.add("#This is the file which creates our various zones");
		zones.add("#Please see http://shorewall.net/manpages/shorewall-zones.html for more details");
		zones.add("#zone\ttype");
		zones.add(cleanZone(ParentZone.INTERNET) + "\tipv4");
		zones.add(cleanZone(ParentZone.ROUTER) + "\tfirewall");
		zones.add("");

		zones.add("#Here, we build our server zone, and give each server its own subzone");
		zones.add(cleanZone(ParentZone.SERVERS) + "\tipv4");
		getNetworkModel().getServers().keySet().forEach(server -> {
			zones.add(cleanZone(server) + ":" + cleanZone(ParentZone.SERVERS) + "\tipv4");
		});
		zones.add("");

		zones.add("#Here, we build our user zone, and give each user their own subzone");
		zones.add("Users\tipv4");
		getNetworkModel().getUserDevices().keySet().forEach(user -> {
			zones.add(cleanZone(user) + ":" + cleanZone(ParentZone.USERS) + "\tipv4");
		});

		// TODO: Do we need an admin zone? Should it be sub-zoned too?
		zones.add(cleanZone(ParentZone.ADMINS) + ":" + cleanZone(ParentZone.USERS) + "\tipv4");
		zones.add("");

		zones.add("#Here, we build our internal only zone, and give each device its own subzone");
		zones.add(cleanZone(ParentZone.INTERNAL_ONLY) + "\tipv4");
		getNetworkModel().getInternalOnlyDevices().keySet().forEach(device -> {
			zones.add(cleanZone(device) + ":" + cleanZone(ParentZone.INTERNAL_ONLY) + "\tipv4");
		});
		zones.add("");

		zones.add("#Here, we build our external only zone, and give each device its own subzone");
		zones.add(cleanZone(ParentZone.EXTERNAL_ONLY) + "\tipv4");
		getNetworkModel().getExternalOnlyDevices().keySet().forEach(device -> {
			zones.add(cleanZone(device) + ":" + cleanZone(ParentZone.EXTERNAL_ONLY) + "\tipv4");
		});

		// Do we want an autoguest network? Build its zone if so
		if (getNetworkModel().getData().buildAutoGuest()) {
			zones.add(cleanZone(ParentZone.GUESTS) + "\tipv4");
		}

		return zones;
	}

	@Override
	public Collection<IUnit> getLiveConfig() throws ARuntimeException {
		final Collection<IUnit> units = new ArrayList<>();

		final FileUnit zones = new FileUnit("shorewall_zones", "shorewall_installed", CONFIG_BASEDIR + "/zones");
		zones.appendLine(getZonesFile().toArray(String[]::new));

		units.add(zones);

		// Now assign machines their (sub)zone, and enforce our maclist
		final FileUnit hosts = new FileUnit("shorewall_hosts", "shorewall_interfaces", CONFIG_BASEDIR + "/hosts");
		hosts.appendLine(getHostsFile().toArray(String[]::new));

		units.add(hosts);

		final FileUnit maclist = new FileUnit("shorewall_maclist", "shorewall_hosts", CONFIG_BASEDIR + "/maclist");
		maclist.appendLine(getMaclistFile().toArray(String[]::new));

		units.add(maclist);

		// Finally, build our FW rules...
		final FileUnit rules = new FileUnit("shorewall_rules", "shorewall_hosts", CONFIG_BASEDIR + "/rules");
		rules.appendLine(getRulesFile().toArray(String[]::new));

		units.add(rules);

		return units;
	}

	@Override
	public Collection<IUnit> getLiveFirewall() throws ARuntimeException {
		return new ArrayList<>();
	}

	@Override
	public Collection<IUnit> getPersistentConfig() throws ARuntimeException {
		final Collection<IUnit> units = new ArrayList<>();

		final FileEditUnit shorewallConf = new FileEditUnit("shorewall_implicit_continue_on", "shorewall_installed", "IMPLICIT_CONTINUE=No", "IMPLICIT_CONTINUE=Yes",
				"/etc/shorewall/shorewall.conf", "I couldn't enable implicit continue on your firewall - this means many of our firewall configurations will fail.");
		units.add(shorewallConf);

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

		// First work out our Internet-facing NICs
		try {
			getNetworkModel().getData().getNetworkInterfaces(getLabel()).get(Direction.WAN).forEach(nic -> {
				String line = "";
				line += ParentZone.INTERNET;
				line += "\t" + nic.getIface();
				line += "\t-\t";
				line += (nic.getInet().equals(Inet.DHCP)) ? "dhcp," : "";
				line += "routefilter,arp_filter";
				interfaces.appendLine(line);
			});
		} catch (JsonParsingException | ADataException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Then, declare our various interface:zone mapping
		Map<ParentZone, MachineType> zoneMappings = Map.of(ParentZone.SERVERS, MachineType.SERVER,
															ParentZone.USERS, MachineType.USER,
															ParentZone.ADMINS, MachineType.ADMIN,
															ParentZone.INTERNAL_ONLY, MachineType.INTERNAL_ONLY,
															ParentZone.EXTERNAL_ONLY, MachineType.EXTERNAL_ONLY);
		
		if (getNetworkModel().getData().buildAutoGuest()) {
			zoneMappings = new HashMap<>(zoneMappings);
			zoneMappings.put(ParentZone.GUESTS, MachineType.GUEST);
		}
		
		zoneMappings.forEach((zone, type) -> {
			interfaces.appendLine(cleanZone(zone) + "\t" + type.toString() + "\t-\tdhcp,routefilter,arp_filter");
		});
		
		units.add(interfaces);

		// Once we've done all that, it's time to tell shorewall about our various
		// masquerading
		final FileUnit masq = new FileUnit("shorewall_masquerades", "shorewall_installed", CONFIG_BASEDIR + "/masq");
		try {
			getNetworkModel().getData().getNetworkInterfaces(getLabel()).get(Direction.WAN).forEach(nic -> {
				List<MachineType> masqs = Arrays.asList(MachineType.SERVER, MachineType.USER, MachineType.ADMIN, MachineType.INTERNAL_ONLY, MachineType.EXTERNAL_ONLY);

				if (getNetworkModel().getData().buildAutoGuest()) {
					masqs = new ArrayList<>(masqs);
					masqs.add(MachineType.GUEST);
				}

				masqs.forEach(toMasq -> {
					masq.appendLine(nic.getIface() + "\t" + toMasq.toString());
				});
			});
		} catch (JsonParsingException | ADataException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		units.add(masq);

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws AThornSecException {
		return new ArrayList<>();
	}

	/**
	 * Returns a comma-delimited string of all IP addresses for a given machine
	 * 
	 * @param machine
	 * @return
	 */
	private String getAddresses(AMachineModel machine) {
		final Collection<String> addresses = new ArrayList<>();

		machine.getNetworkInterfaces().values().forEach(nic -> {
			nic.getAddresses().forEach(address -> {
				addresses.add(address.withoutPrefixLength().toCompressedString() + "/32");
			});
		});

		return String.join(",", addresses);
	}

	/**
	 * Turns a zone and an array of AMachineModels into the Shorewall hosts file
	 * format
	 *
	 * @param zone     the zone
	 * @param machines the machines
	 * @return the hosts file contents
	 */
	private Collection<String> machines2Host(ParentZone zone, AMachineModel... machines) {
		final Collection<String> hosts = new ArrayList<>();

		for (final AMachineModel machine : machines) {
			hosts.add(cleanZone(machine.getLabel()) + "\t" + zone.toString() + ":" + getAddresses(machine) + "\tmaclist");
		}

		return hosts;
	}

	/**
	 * Parses a Machine into shorewall maclist lines
	 *
	 * @param machine
	 * @param iface
	 * @return
	 */
	private Collection<String> machines2Maclist(MachineType iface, AMachineModel... machines) {
		final Collection<String> maclist = new ArrayList<>();

		for (final AMachineModel machine : machines) {
			machine.getNetworkInterfaces().values().forEach(nic -> {
				maclist.add("ACCEPT\t" + iface.toString() + "\t" + nic.getMac().toNormalizedString() + "\t" + getAddresses(machine) + "\t#" + machine.getLabel());

			});
		}

		return maclist;
	}
}
