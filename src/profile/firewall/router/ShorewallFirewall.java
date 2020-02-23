/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.firewall.router;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.json.stream.JsonParsingException;
import core.StringUtils;
import core.data.machine.AMachineData.Encapsulation;
import core.data.machine.AMachineData.MachineType;
import core.data.machine.configuration.NetworkInterfaceData.Direction;
import core.data.machine.configuration.NetworkInterfaceData.Inet;
import core.exception.AThornSecException;
import core.exception.data.ADataException;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.ARuntimeException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.machine.AMachineModel;
import core.model.network.NetworkModel;
import core.unit.fs.FileEditUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;
import profile.firewall.AFirewallProfile;

/**
 * For more information on this Firewall, please see
 * http://shorewall.org/configuration_file_basics.htm
 * 
 *
 */
//@TODO:This class is mucky, and needs some major refactoring to make readable
public class ShorewallFirewall extends AFirewallProfile {
	public enum Action {
		ACCEPT, DNAT, DROP, REJECT, REDIRECT;
	}

	public enum Arm {
		LAN, FIREWALL, INTERNET;
	}

	public enum ParentZone {
		INTERNET(Arm.INTERNET, "Internet"), ROUTER(Arm.FIREWALL, "$FW"), USERS(Arm.LAN, "Users"),
		ADMINS(Arm.LAN, "Administrators"), SERVERS(Arm.LAN, "Servers"), INTERNAL_ONLY(Arm.LAN, "InternalOnlys"),
		EXTERNAL_ONLY(Arm.LAN, "ExternalOnlys"), GUESTS(Arm.LAN, "Guests"), VPN(Arm.LAN, "VPN");

		public static Set<ParentZone> internetZone = EnumSet.of(INTERNET);
		public static Set<ParentZone> routerZone = EnumSet.of(ROUTER);
		public static Set<ParentZone> lanZone = EnumSet.range(USERS, VPN, GUESTS);

		private Arm direction;
		private String parentZone;

		ParentZone(Arm direction, String parentZone) {
			this.direction = direction;
			this.parentZone = parentZone;
		}

		@Override
		public String toString() {
			return this.parentZone;
		}

		public Arm getDirection() {
			return this.direction;
		}

		public String getParentZone() {
			return this.parentZone;
		}
	}

	private static String CONFIG_BASEDIR = "/etc/shorewall";
	private final Collection<String> rules;
	Map<ParentZone, Collection<AMachineModel>> hostMap;
	

	public ShorewallFirewall(String label, NetworkModel networkModel) {
		super(label, networkModel);
		this.rules = new ArrayList<>();
		this.hostMap = new LinkedHashMap<>();
	}
	
	private Map<ParentZone, Collection<AMachineModel>> getHostMap() {
		return this.hostMap;
	}

	/**
	 * Zones must be a maximum of 10 alpha-numeric chars long
	 *
	 * @param zone
	 * @return valid zone name
	 */
	private String cleanZone(Object zone) {
		String _zone = zone.toString();
		String prefix = "";

		if (_zone.startsWith("$")) {
			prefix = "\\" + _zone.substring(0, 1);
		} else if (_zone.startsWith("!")) {
			prefix = "!";
		}

		_zone = StringUtils.stringToAlphaNumeric(_zone);

		// @TODO: Refactor this (we use it for MAC generation elsewhere...)
		if (_zone.length() > 10) {
			MessageDigest md = null;
			try {
				md = MessageDigest.getInstance("SHA-512");
			} catch (final NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			md.update(zone.toString().getBytes());

			final byte byteData[] = md.digest();
			final StringBuffer hashCodeBuffer = new StringBuffer();
			for (final byte element : byteData) {
				hashCodeBuffer.append(Integer.toString((element & 0xff) + 0x100, 16).substring(1));

				if (hashCodeBuffer.length() > 3) {
					break;
				}
			}

			_zone = _zone.substring(0, 7) + hashCodeBuffer.toString().substring(0, 3);
		}

		return prefix + _zone;
	}

	@Override
	public Collection<IUnit> getInstalled() throws ARuntimeException {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("shorewall", "proceed", "shorewall"));

		return units;
	}

	private Collection<String> getMaclistFile() {
		final Collection<String> maclist = new ArrayList<>();

		getHostMap().forEach((zone, machines) -> {
			maclist.addAll(machines2Maclist(zone, machines));
		});

		return maclist;
	}

	private Collection<String> getHostsFile() {
		final Collection<String> hosts = new ArrayList<>();

		hosts.add("#Please see http://shorewall.net/manpages/shorewall-zones.html for more details");
		hosts.add("#zone\thosts\toptions");

		hostMap.forEach((zone, machines) -> {
			hosts.addAll(machines2Host(zone, machines));
		});

		return hosts;
	}

	/**
	 * Please be aware that each param will have its toString() method invoked
	 * 
	 * @param action
	 * @param sourceZone
	 * @param sourceSubZone
	 * @param destinationZone
	 * @param destinationSubZone
	 * @param encapsulation
	 * @param dPorts
	 * @param sPorts
	 * @param origDest
	 * @param rate
	 */
	private void addRule(Object action, Object sourceZone, Object sourceSubZone, Object destinationZone, Object destinationSubZone, Encapsulation encapsulation, String dPorts, String sPorts,String origDest, String rate) {
		addRule(action, sourceZone, ":", sourceSubZone, destinationZone, destinationSubZone, encapsulation, dPorts, sPorts, origDest, rate);
	}
	
	private void addRule(Object action, Object sourceZone, Object delimiter, Object sourceSubZone, Object destinationZone, Object destinationSubZone, Encapsulation encapsulation, String dPorts, String sPorts,
			String origDest, String rate) {
		String rule = "";
		rule += action.toString() + "\t";
		rule += cleanZone(sourceZone);
		rule += (sourceSubZone != null) ? delimiter.toString() + sourceSubZone.toString() + "\t" : "\t";
		rule += cleanZone(destinationZone);
		rule += (destinationSubZone != null) ? ":" + destinationSubZone.toString() + "\t" : "\t";
		rule += (encapsulation != null) ? encapsulation.toString().toLowerCase() + "\t" : "-\t";
		rule += (dPorts != null) ? dPorts + "\t" : "-\t";
		rule += (sPorts != null) ? sPorts + "\t" : "-\t";
		rule += (origDest != null) ? origDest + "\t" : "-\t";
		rule += (rate != null) ? rate : "";

		this.rules.add(rule);
	}

	/**
	 * 
	 * @param action
	 * @param sourceZone
	 * @param sourceSubZone
	 * @param destinationZone
	 * @param destinationSubZone
	 * @param encapsulation
	 * @param dPorts
	 */
	private void addRule(Object action, Object sourceZone, Object sourceSubZone, Object destinationZone, Object destinationSubZone,Encapsulation encapsulation,String dPorts) {
		addRule(action, sourceZone, sourceSubZone, destinationZone, destinationSubZone, encapsulation, dPorts, null, null, null);
	}
	
	/**
	 * 
	 * @param action
	 * @param sourceZone
	 * @param sourceSubZone
	 * @param destinationZone
	 * @param destinationSubZone
	 */
	private void addRule(Object action, Object sourceZone, Object sourceSubZone, Object destinationZone, Object destinationSubZone) {
		addRule(action, sourceZone, sourceSubZone, destinationZone, destinationSubZone, null, null, null, null, null);
	}
	
	/**
	 * 
	 * @param macro
	 * @param sourceZone
	 * @param destinationZone
	 */
	private void addListenRule(String macro, String sourceZone, String destinationZone) {
		addListenRule(macro,Action.ACCEPT,sourceZone,null,destinationZone,null);
	}
	
	/**
	 * 
	 * @param macro
	 * @param action
	 * @param sourceZone
	 * @param sourceSubZone
	 * @param destination
	 * @param destinationSubZone
	 */
	private void addListenRule(String macro, Action action, String sourceZone,String sourceSubZone, String destination, String destinationSubZone) {
		addListenRule(macro, action, sourceZone, sourceSubZone, destination, destinationSubZone, null, null,null);
	}
	
	/**
	 * 
	 * @param sourceZone
	 * @param destinationZone
	 * @param encapsulation
	 * @param ports
	 */
	private void addListenRule(String sourceZone, String destinationZone, Encapsulation encapsulation, Collection<Integer> ports) {
		addListenRule(sourceZone, destinationZone, encapsulation, ports, null);
	}
	
	private void addListenRule(String sourceZone, String destinationZone, Encapsulation encapsulation, Integer... ports) {
		addListenRule(sourceZone, destinationZone, encapsulation, Arrays.asList(ports));
	}
	
	/**
	 * 
	 * @param sourceZone
	 * @param destinationZone
	 * @param encapsulation
	 * @param ports
	 * @param externalIPs
	 */
	private void addListenRule(String sourceZone, String destinationZone, Encapsulation encapsulation, Collection<Integer> ports, Collection<IPAddress> externalIPs) {
		String _ports = null;
		String _externalIPs = null;
		
		if (ports != null) {
			_ports = ports.stream().map(Object::toString).collect(Collectors.joining(","));
		}
		if (externalIPs!=null) {
			_externalIPs = externalIPs.stream().map(IPAddress::toCompressedString).collect(Collectors.joining(","));
		}
		
		addListenRule(null, Action.ACCEPT, sourceZone, null, destinationZone, null, encapsulation, _ports, _externalIPs);
	}
		
	/**
	 * 
	 * @param macro
	 * @param action
	 * @param sourceZone
	 * @param sourceSubZone
	 * @param destination
	 * @param destinationSubZone
	 * @param encapsulation
	 * @param dPorts
	 */
	private void addListenRule(String macro, Action action, String sourceZone,String sourceSubZone, String destination, String destinationSubZone, Encapsulation encapsulation, String dPorts, String externalIPs) {
		String _action = (macro == null) ? action.toString(): macro+"("+action.toString()+")";
		
		addRule(_action, sourceZone, sourceSubZone, destination, destinationSubZone, encapsulation, dPorts);
	}
	
	/**
	 * 
	 * @param sourceZone
	 * @param destinationZone
	 * @param destinationSubZone
	 * @param ports
	 * @param originalDestination
	 */
	private void addDNATRule(String sourceZone, String destinationZone, String destinationSubZone, Collection<Integer> ports, String originalDestination) {
		addDNATRule(sourceZone, null, null, destinationZone, destinationSubZone, ports, originalDestination);
	}
	
	/**
	 * 
	 * @param sourceZone
	 * @param destinationZone
	 * @param destinationSubZone
	 * @param ports
	 */
	private void addDNATRule(String sourceZone, String delimiter, String sourceSubZone, String destinationZone, String destinationSubZone, Collection<Integer> ports, String originalDestination) {
		final String _ports = ports.stream().map(Object::toString).collect(Collectors.joining(","));
		
		addRule(Action.DNAT, sourceZone, delimiter, cleanZone(sourceSubZone), destinationZone, destinationSubZone, Encapsulation.TCP, _ports, null, originalDestination, null);
	}
	
	/**
	 * Allow unfettered access to the Internet from a given zone
	 * @param sourceZone
	 */
	private void addEgressRule(ParentZone sourceZone) {
		addRule(Action.ACCEPT.toString(), sourceZone, null, ParentZone.INTERNET.toString(), null);
	}
	
	/**
	 * 
	 * @param action
	 * @param sourceZone
	 * @param destination
	 */
	private void addEgressRule(String sourceZone, HostName destination) {
		String _egress = destination.getHost();
		if (!destination.isAddress()) {
			_egress += ".";
		}
		
		addRule(Action.ACCEPT.toString(), sourceZone, null, ParentZone.INTERNET.toString(), _egress);
	}

	private Collection<String> getRulesFile() throws InvalidServerException, InvalidServerModelException {
		String routerZone = ParentZone.ROUTER.toString();
		
		addListenRule("DNS", routerZone, routerZone);
		ParentZone.lanZone.forEach(sourceZone -> {
			addListenRule("DNS", Action.ACCEPT, sourceZone.toString(), null, routerZone, "&" + sourceZone.toString());
		});
		addListenRule(ParentZone.USERS.toString(), routerZone, Encapsulation.TCP, getNetworkModel().getData().getSSHPort(getLabel()));
		
		//Whitelist Users & External-only, because they need 'net access
		addEgressRule(ParentZone.USERS);
		addEgressRule(ParentZone.EXTERNAL_ONLY);
		
		// Iterate over every machine to build all of its rules
		getNetworkModel().getUniqueMachines().forEach((machineLabel, machine) -> {
			machine.getEgresses().forEach(egress -> {
				addEgressRule(machineLabel, egress);
			});

			machine.getListens().forEach((encapsulation, ports) -> {
				try {
					if (getNetworkModel().getServerModel(machineLabel).isRouter()) {
						return;
					}
				} catch (InvalidServerModelException e) {
					return;
				}

				if (machine.getExternalIPs() != null) {
					addDNATRule(ParentZone.INTERNET.toString(), machineLabel, machine.getIP(), ports, machine.getExternalIPs().toString());
					addListenRule("all+", machineLabel,  encapsulation, ports, machine.getExternalIPs());
				} else {
					addListenRule(ParentZone.USERS.toString(), machineLabel, encapsulation, ports);
				}
				
				machine.getDNAT().forEach((destination, dnatPorts)->{
					try {
						addDNATRule("all", "!", machineLabel, machineLabel, machine.getIP(), dnatPorts, getNetworkModel().getServerModel(destination).getIP());
					} catch (InvalidServerModelException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});
			});
		});

		return this.rules;
	}

	private Collection<String> getZonesFile() {
		// Build our zones
		final Collection<String> zones = new ArrayList<>();
		zones.add("#This is the file which creates our various zones");
		zones.add("#Please see http://shorewall.net/manpages/shorewall-zones.html for more details");
		zones.add("#zone\ttype");
		zones.add(cleanZone(ParentZone.INTERNET) + "\tipv4");
		zones.add("");

		zones.add("#Here, we build our server zone, and give each server its own subzone");
		zones.add(cleanZone(ParentZone.SERVERS) + "\tipv4");
		getNetworkModel().getServers().keySet().forEach(server -> {
			try {
				if (getNetworkModel().getServerModel(server).isRouter()) {
					zones.add(cleanZone(server) + "\tfirewall");
				} else {
					zones.add(cleanZone(server) + ":" + cleanZone(ParentZone.SERVERS) + "\tipv4");
				}
			} catch (InvalidServerModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
		final FileUnit rules =
				new FileUnit("shorewall_rules", "shorewall_hosts", CONFIG_BASEDIR + "/rules");
		try {
			rules.appendLine(getRulesFile().toArray(String[]::new));
		} catch (InvalidServerException | InvalidServerModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		units.add(rules);

		return units;
	}

	@Override
	public Collection<IUnit> getLiveFirewall() throws ARuntimeException {
		return new ArrayList<>();
	}

	@Override
	public Collection<IUnit> getPersistentConfig() throws ARuntimeException {
		this.hostMap.put(ParentZone.SERVERS, getNetworkModel().getMachines(MachineType.SERVER).values());
		this.hostMap.put(ParentZone.USERS, getNetworkModel().getMachines(MachineType.USER).values());
		this.hostMap.put(ParentZone.ADMINS, getNetworkModel().getMachines(MachineType.ADMIN).values());
		this.hostMap.put(ParentZone.INTERNAL_ONLY, getNetworkModel().getMachines(MachineType.INTERNAL_ONLY).values());
		this.hostMap.put(ParentZone.EXTERNAL_ONLY, getNetworkModel().getMachines(MachineType.EXTERNAL_ONLY).values());
		
		final Collection<IUnit> units = new ArrayList<>();

		final FileEditUnit shorewallConf = new FileEditUnit("shorewall_implicit_continue_on",
				"shorewall_installed", "IMPLICIT_CONTINUE=No", "IMPLICIT_CONTINUE=Yes",
				"/etc/shorewall/shorewall.conf",
				"I couldn't enable implicit continue on your firewall - this means many of our firewall configurations will fail.");
		units.add(shorewallConf);

		// Build our default policies
		final FileUnit policies = new FileUnit("shorewall_policies", "shorewall_installed", CONFIG_BASEDIR + "/policy");
		policies.appendLine("#Default policies to use for intra-zone communication");
		policies.appendLine("#For specific rules, please look at " + CONFIG_BASEDIR + "/rules");
		policies.appendLine("#Please see http://shorewall.net/manpages/shorewall-policy.html for more details");
		policies.appendLine("#source\tdestination\taction");
		policies.appendLine("Internet\tall+\tDROP"); // DROP all ingress traffic
		policies.appendLine("all+\tall+\tREJECT"); // REJECT all other traffic
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
				ParentZone.USERS, MachineType.USER, ParentZone.ADMINS, MachineType.ADMIN,
				ParentZone.INTERNAL_ONLY, MachineType.INTERNAL_ONLY, ParentZone.EXTERNAL_ONLY,
				MachineType.EXTERNAL_ONLY);

		if (getNetworkModel().getData().buildAutoGuest()) {
			zoneMappings = new HashMap<>(zoneMappings);
			zoneMappings.put(ParentZone.GUESTS, MachineType.GUEST);
		}

		zoneMappings.forEach((zone, type) -> {
			interfaces.appendLine(
					cleanZone(zone) + "\t" + type.toString() + "\t-\tdhcp,routefilter,arp_filter");
		});

		units.add(interfaces);

		// Once we've done all that, it's time to tell shorewall about our various
		// masquerading
		final FileUnit masq = new FileUnit("shorewall_masquerades", "shorewall_installed",
				CONFIG_BASEDIR + "/masq");
		try {
			getNetworkModel().getData().getNetworkInterfaces(getLabel()).get(Direction.WAN).forEach(nic -> {
				ParentZone.lanZone.forEach(zone -> {
					masq.appendLine(nic.getIface() + "\t" + zone.toString());
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

		machine.getNetworkInterfaces().forEach(nic -> {
			if (nic.getAddresses() != null) {
				nic.getAddresses().forEach(address -> {
					if (address != null) {
						addresses.add(address.getLowerNonZeroHost().withoutPrefixLength().toCompressedString() + "/32");
					}
				});
			}
		});

		return String.join(",", addresses);
	}

	private String getAddresses(Collection<IPAddress> ips) {
		final Collection<String> addresses = new ArrayList<>();

		ips.forEach(address -> {
			if (address != null) {
				addresses.add(address.getLowerNonZeroHost().withoutPrefixLength().toCompressedString() + "/32");
			}
		});

		return String.join(",", addresses);
	}

	/**
	 * Turns a zone and an array of AMachineModels into the Shorewall hosts file format
	 *
	 * @param zone the zone
	 * @param machines the machines
	 * @return the hosts file contents
	 */
	private Collection<String> machines2Host(ParentZone zone, Collection<AMachineModel> machines) {
		final Collection<String> hosts = new ArrayList<>();

		for (final AMachineModel machine : machines) {
			
			try {
				if (getNetworkModel().getServerModel(machine.getLabel()).isRouter()) {
					continue;
				}
			} catch (InvalidServerModelException e) {
				//As you were. This is not the droid you're looking for.
			}
			
			hosts.add(cleanZone(machine.getLabel()) + "\t" + zone.toString() + ":"
					+ getAddresses(machine) + "\tmaclist");
		}

		return hosts;
	} 

	/**
	 * Parses a Machine into shorewall maclist lines
	 *
	 * @param machine
	 * @param zone
	 * @return
	 */
	private Collection<String> machines2Maclist(ParentZone zone, Collection<AMachineModel> machines) {
		final Collection<String> maclist = new ArrayList<>();

		for (final AMachineModel machine : machines) {
			
			try {
				if (getNetworkModel().getServerModel(machine.getLabel()).isRouter()) {
					continue;
				}
			} catch (InvalidServerModelException e) {
				//As you were. This is not the droid you're looking for.
			}
			
			machine.getNetworkInterfaces().forEach(nic -> {
				maclist.add("ACCEPT\t" + zone.toString() + "\t" + nic.getMac().toNormalizedString()
						+ "\t" + getAddresses(machine) + "\t#" + machine.getLabel());

			});
		}

		return maclist;
	}
}
