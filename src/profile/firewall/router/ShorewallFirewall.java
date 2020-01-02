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
import java.util.HashSet;
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
import core.model.machine.configuration.networking.ISystemdNetworkd;
import core.model.network.NetworkModel;
import core.unit.fs.FileEditUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
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

	@Override
	public Collection<IUnit> getLiveConfig() throws ARuntimeException {
		final Collection<IUnit> units = new ArrayList<>();

		// Build our zones
		final FileUnit zones = new FileUnit("shorewall_zones", "shorewall_installed", CONFIG_BASEDIR + "/zones");
		zones.appendLine("#This is the file which creates our various zones");
		zones.appendLine("#Please see http://shorewall.net/manpages/shorewall-zones.html for more details");
		zones.appendLine("#zone\ttype");
		zones.appendLine(cleanZone(ParentZone.INTERNET) + "\tipv4");
		zones.appendLine(cleanZone(ParentZone.ROUTER) + "\tfirewall");
		zones.appendCarriageReturn();

		zones.appendLine("#Here, we build our server zone, and give each server its own subzone");
		zones.appendLine(cleanZone(ParentZone.SERVERS) + "\tipv4");
		getNetworkModel().getServers().keySet().forEach(server -> {
			zones.appendLine(cleanZone(server) + ":" + cleanZone(ParentZone.SERVERS) + "\tipv4");
		});
		zones.appendCarriageReturn();

		zones.appendLine("#Here, we build our user zone, and give each user their own subzone");
		zones.appendLine("Users\tipv4");
		getNetworkModel().getUserDevices().keySet().forEach(user -> {
			zones.appendLine(cleanZone(user) + ":" + cleanZone(ParentZone.USERS) + "\tipv4");
		});

		// TODO: Do we need an admin zone? Should it be sub-zoned too?
		zones.appendLine(cleanZone(ParentZone.ADMINS) + ":" + cleanZone(ParentZone.USERS) + "\tipv4");
		zones.appendCarriageReturn();

		zones.appendLine("#Here, we build our internal only zone, and give each device its own subzone");
		zones.appendLine(cleanZone(ParentZone.INTERNAL_ONLY) + "\tipv4");
		getNetworkModel().getInternalOnlyDevices().keySet().forEach(device -> {
			zones.appendLine(cleanZone(device) + ":" + cleanZone(ParentZone.INTERNAL_ONLY) + "\tipv4");
		});
		zones.appendCarriageReturn();

		zones.appendLine("#Here, we build our external only zone, and give each device its own subzone");
		zones.appendLine(cleanZone(ParentZone.EXTERNAL_ONLY) + "\tipv4");
		getNetworkModel().getExternalOnlyDevices().keySet().forEach(device -> {
			zones.appendLine(cleanZone(device) + ":" + cleanZone(ParentZone.EXTERNAL_ONLY) + "\tipv4");
		});

		// Do we want an autoguest network? Build its zone if so
		if (getNetworkModel().getData().buildAutoGuest()) {
			zones.appendLine(cleanZone(ParentZone.GUESTS) + "\tipv4");
		}

		units.add(zones);

		// Now assign machines their (sub)zone, and enforce our maclist
		final FileUnit hosts = new FileUnit("shorewall_hosts", "shorewall_interfaces", CONFIG_BASEDIR + "/hosts");
		final FileUnit maclist = new FileUnit("shorewall_maclist", "shorewall_hosts", CONFIG_BASEDIR + "/maclist");

		hosts.appendLine("#Please see http://shorewall.net/manpages/shorewall-zones.html for more details");
		hosts.appendLine("#zone      hosts          options");

		getNetworkModel().getServers().values().forEach(server -> {
			try {
				if (!server.isRouter()) { // Ignore routers
					hosts.appendLine(machine2Host(server, ParentZone.SERVERS));
					maclist.appendLine(machine2MaclistEntry(server, MachineType.SERVER).toArray(String[]::new));
				}
			} catch (final InvalidServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

		getNetworkModel().getUserDevices().values().forEach(user -> {
			hosts.appendLine(machine2Host(user, ParentZone.USERS));
			maclist.appendLine(machine2MaclistEntry(user, MachineType.USER).toArray(String[]::new));
		});

		getNetworkModel().getInternalOnlyDevices().values().forEach(device -> {
			hosts.appendLine(machine2Host(device, ParentZone.INTERNAL_ONLY));
			maclist.appendLine(machine2MaclistEntry(device, MachineType.INTERNAL_ONLY).toArray(String[]::new));
		});

		getNetworkModel().getExternalOnlyDevices().values().forEach(device -> {
			hosts.appendLine(machine2Host(device, ParentZone.EXTERNAL_ONLY));
			maclist.appendLine(machine2MaclistEntry(device, MachineType.EXTERNAL_ONLY).toArray(String[]::new));
		});

		units.add(hosts);
		units.add(maclist);

		// Finally, build our FW rules...
		final FileUnit rules = new FileUnit("shorewall_rules", "shorewall_hosts", CONFIG_BASEDIR + "/rules");
		units.add(rules);

		// Iterate over the whole network
		// However, bear in mind that we'll see machines more than once, so we need to
		// keep track
		final Collection<String> seen = new HashSet<>();
		getNetworkModel().getMachines().forEach((type, machines) -> {
			// Iterate over every machine
			machines.forEach((label, machine) -> {
				if (!seen.contains(label)) {
					seen.add(label);

					// Start by building this machine's egresses.
					machine.getEgresses().forEach(egress -> {
						String line = "";

						line += "ACCEPT\t";
						line += cleanZone(label) + "\t";
						line += ParentZone.INTERNET + ":" + egress.getHost();
						if (!egress.isAddress()) {
							line += ".";
						}

						if (egress.getPort() != null) {
							line += "\ttcp\t";
							line += egress.getPort();
						}

						rules.appendLine(line);
					});

				}
			});
		});

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

		return cleanZone(machine.getLabel()) + "\t" + zone.toString() + ":" + nics + "\tmaclist";
	}

	/**
	 * Parses a Machine into shorewall maclist lines
	 *
	 * @param machine
	 * @param iface
	 * @return
	 */
	private Collection<String> machine2MaclistEntry(AMachineModel machine, MachineType iface) {
		final Collection<String> lines = new ArrayList<>();

		for (final ISystemdNetworkd nic : machine.getNetworkInterfaces().values()) {
			String line = "ACCEPT\t" + iface.toString() + "\t" + nic.getMac().toNormalizedString() + "\t";

			for (final IPAddress ip : nic.getAddresses()) {
				line += ip.withoutPrefixLength().toCompressedString() + "/32,";
			}
			line = line.replaceAll(",$", ""); // Get rid of any trailing comma

			line += "\t#" + machine.getLabel();

			lines.add(line);
		}

		return lines;
	}
}
