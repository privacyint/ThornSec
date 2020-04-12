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
import java.util.List;
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
import core.exception.runtime.InvalidMachineModelException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.machine.AMachineModel;
import core.model.machine.ServerModel;
import core.model.machine.configuration.networking.NetworkInterfaceModel;
import core.model.network.NetworkModel;
import core.unit.fs.FileEditUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddress.IPVersion;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.IncompatibleAddressException;
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
		INTERNET(Arm.INTERNET, MachineType.INTERNET), ROUTER(Arm.FIREWALL, MachineType.ROUTER), USERS(Arm.LAN, MachineType.USER),
		ADMINS(Arm.LAN, MachineType.ADMIN), SERVERS(Arm.LAN, MachineType.SERVER), INTERNAL_ONLY(Arm.LAN, MachineType.INTERNAL_ONLY),
		EXTERNAL_ONLY(Arm.LAN, MachineType.EXTERNAL_ONLY), GUESTS(Arm.LAN, MachineType.GUEST), VPN(Arm.LAN, MachineType.VPN);

		public static Set<ParentZone> internetZone = EnumSet.of(INTERNET);
		public static Set<ParentZone> routerZone = EnumSet.of(ROUTER);
		public static Set<ParentZone> lanZone = EnumSet.range(USERS, VPN);

		private Arm direction;
		private MachineType parentZone;

		ParentZone(Arm direction, MachineType type) {
			this.direction = direction;
			this.parentZone = type;
		}

		@Override
		public String toString() {
			return this.parentZone.toString();
		}

		public Arm getDirection() {
			return this.direction;
		}

		public MachineType getParentZone() {
			return this.parentZone;
		}
	}

	private static String CONFIG_BASEDIR = "/etc/shorewall";
	private final Collection<Rule> rules;
	
	private final ServerModel me;
	
	private class Rule {
		private String macro;
		private Action action;

		private Boolean invertSource;

		private String sourceZone;
		private String sourceSubZone;
		private Collection<Integer> sPorts;
		
		private String destinationZone;
		private String destinationSubZone;
		private Encapsulation proto;
		private Collection<Integer> dPorts;
		
		private Collection<IPAddress> origDest;
		
		private String rate;

		public Rule() {
			macro = null;
			action = null;
			
			this.invertSource = false;
			sourceSubZone = null;
			sPorts = null;

			destinationZone = null;
			destinationSubZone = null;
			proto = null;
			dPorts = null;
			
			origDest = null;
			
			rate = null;
		}
		
		public void setMacro(String macro) {
			this.macro = macro;
		}
		
		public void setAction(Action action) {
			this.action = action;
		}
		
		public void setSourceZone(String sourceZone) {
			this.sourceZone = sourceZone;
		}
		
		public void setInvertSource(Boolean val) {
			this.invertSource = val;
		}
		
		public void setDestinationZone(String destinationZone) {
			this.destinationZone = destinationZone;
		}
		
		public void setDestinationSubZone(String destinationSubZone) {
			this.destinationSubZone = destinationSubZone;
		}
		
		public void setProto(Encapsulation proto) {
			this.proto = proto;
		}
		
		public void setDPorts(Collection<Integer> dPorts) {
			this.dPorts = dPorts;
		}
		
		public void setOrigDest(Collection<IPAddress> origDest) {
			this.origDest = origDest;
		}
		
		public void setRate(String rate) {
			this.rate = rate;
		}
		
		public String getRule() {
			String _action = (macro == null) ? action.toString() : macro+"("+action.toString()+")";
			String _dPorts = null;
			String _sPorts = null;
			String _origDest = null;
			String _sourceZone = cleanZone(sourceZone);
			String _destinationZone = cleanZone(destinationZone);
			
			if (this.invertSource) {
				_sourceZone = "all!" + _sourceZone;
			}

			String _egress = this.destinationSubZone;
			if ((this.destinationSubZone != null)
					&& !(new HostName(this.destinationSubZone).isAddress())
					&& !(this.destinationSubZone.startsWith("&"))) {
				_egress += ".";
			}		
			if (dPorts != null) {
				_dPorts = dPorts.stream().map(Object::toString).collect(Collectors.joining(","));
			}
			if (sPorts != null) {
				_sPorts = sPorts.stream().map(Object::toString).collect(Collectors.joining(","));
			}
			if (origDest != null) {
				_origDest = this.origDest.stream().map(dest -> dest.withoutPrefixLength().toCompressedString())
						.collect(Collectors.joining(","));
			}
			
			String rule = "";
			rule += _action + "\t";
			rule += _sourceZone;
			rule += (sourceSubZone != null) ? ":" + sourceSubZone.toString() + "\t" : "\t";
			rule += _destinationZone;
			rule += (_egress != null) ? ":" + _egress + "\t" : "\t";
			rule += (proto != null) ? proto.toString().toLowerCase() + "\t" : "-\t";
			rule += (_dPorts != null) ? _dPorts + "\t" : "-\t";
			rule += (_sPorts != null) ? _sPorts + "\t" : "-\t";
			rule += (_origDest != null) ? _origDest + "\t" : "-\t";
			rule += (rate != null) ? rate : "";
		
			return rule;
		}
	}

	private class Comment extends Rule {
		
		private final String comment;
		
		public Comment(String comment) {
			this.comment = comment;
		}
		
		@Override
		public String getRule() {
			return "# " + this.comment;
		}
	}

	public ShorewallFirewall(String label, NetworkModel networkModel) throws InvalidServerModelException {
		super(label, networkModel);
		this.rules = new ArrayList<>();
		this.vlans = null;
		this.me = getNetworkModel().getServerModel(getLabel());
	}
	
	/**
	 * Zones must be a maximum of 10 alpha-numeric chars long
	 *
	 * @param zone
	 * @return valid zone name
	 */
	private String cleanZone(Object zone) {
		if (zone == null) {
			return null;
		}
		
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

		if (this.vlans != null) {
			this.vlans.forEach((vlan, type) -> { 
				Map<String, AMachineModel> machines = getNetworkModel().getMachines(type);
			
				maclist.addAll(machines2Maclist(type, machines.values()));
			});
		}

		return maclist;
	}

	private Collection<String> getHostsFile() {
		final Collection<String> hosts = new ArrayList<>();

		hosts.add("#Please see http://shorewall.net/manpages/shorewall-zones.html for more details");
		hosts.add("#zone\thosts\toptions");

		if (this.vlans != null) {
			this.vlans.forEach((vlan, type) -> { 
				Map<String, AMachineModel> machines = getNetworkModel().getMachines(type);
			
				hosts.addAll(machines2Host(type, machines.values()));
			});
		}

		return hosts;
	}
	
	private boolean hasRealUsers() {
		return getNetworkModel().getUserDevices() != null 
				&&
			getNetworkModel().getUserDevices()
				.values()
				.stream()
				.filter((dev) -> dev.getNetworkInterfaces() != null) //Only if there are any interfaces, though
				.count() > 0;
	}

	private Collection<Rule> getDNSRules() throws InvalidServerModelException {
		Collection<Rule> rules = new ArrayList<>();

		if (me.isRouter()) {
			Comment dnsComment = new Comment("DNS rules");
			rules.add(dnsComment);
	
			//Router always needs to talk DNS to itself.
			Rule routerRule = new Rule();
			routerRule.setMacro("DNS");
			routerRule.setAction(Action.ACCEPT);
			routerRule.setSourceZone(ParentZone.ROUTER.toString());
			routerRule.setDestinationZone(ParentZone.ROUTER.toString());
			rules.add(routerRule);
			
			if (this.vlans != null) {
				vlans.forEach((vlan, type) -> {
					Rule lanDnsRule = new Rule();
					lanDnsRule.setMacro("DNS");
					lanDnsRule.setAction(Action.ACCEPT);
					lanDnsRule.setSourceZone(type.toString());
					lanDnsRule.setDestinationZone(ParentZone.ROUTER.toString());
					lanDnsRule.setDestinationSubZone("&" + vlan.getIface());
					
					rules.add(lanDnsRule);
				});
			}
		}
		else {
			;; //TODO
		}
		
		return rules;
	}
	
	/**
	 * Builds the default rules for its host.
	 * 
	 * If your machine is a Router, it builds the various inter-zone communications
	 * (as we disallow everything by policy).
	 * 
	 * @return A collection of Rules
	 */
	private Collection<Rule> getDefaultRules() {
		Collection<Rule> rules = new ArrayList<>();
		
		if (me.isRouter()) {
			if (this.hasRealUsers()) {
				Rule userEgress = new Rule();
				userEgress.setAction(Action.ACCEPT);
				userEgress.setSourceZone(ParentZone.USERS.toString());
				userEgress.setDestinationZone(ParentZone.INTERNET.toString());
				
				rules.add(userEgress);
			}
			
			if (getNetworkModel().getExternalOnlyDevices().size() > 0) {
				Rule externalOnlyEgress = new Rule();
				externalOnlyEgress.setAction(Action.ACCEPT);
				externalOnlyEgress.setSourceZone(ParentZone.EXTERNAL_ONLY.toString());
				externalOnlyEgress.setDestinationZone(ParentZone.INTERNET.toString());
				
				rules.add(externalOnlyEgress);
			}
		}
		
		return rules;
	}
	
	private Collection<Rule> getRulesFile() throws InvalidServerException, InvalidServerModelException {
		Collection<Rule> rules = new ArrayList<>();
		
		if (getNetworkModel().getServerModel(getLabel()).isRouter()) {
			
			rules.addAll(getDNSRules());
			rules.addAll(getDefaultRules());
			
			// Iterate over every machine to build all of its rules
			getNetworkModel().getUniqueMachines().values().forEach(machine -> {
				Comment machineComment = new Comment(machine.getLabel());
				rules.add(machineComment);
				
				machine.getEgresses().forEach(egress -> {
					Rule machineEgressRule = new Rule();
					machineEgressRule.setAction(Action.ACCEPT);
					machineEgressRule.setSourceZone(machine.getLabel());
					machineEgressRule.setDestinationZone(ParentZone.INTERNET.toString());
					machineEgressRule.setDestinationSubZone(egress.getHost());
					
					rules.add(machineEgressRule);
				});
	
				machine.getListens().forEach((encapsulation, dPorts) -> {
					if (machine.getExternalIPs() != null) {
						Rule externalDNATRule = new Rule();
						externalDNATRule.setAction(Action.DNAT);
						externalDNATRule.setSourceZone(ParentZone.INTERNET.toString());
						externalDNATRule.setDestinationZone(machine.getLabel());
						externalDNATRule.setDestinationSubZone(
								machine.getIPs().stream().map(dest -> dest.withoutPrefixLength().toCompressedString())
										.collect(Collectors.joining(",")));
						externalDNATRule.setDPorts(dPorts);
						externalDNATRule.setProto(encapsulation);
						externalDNATRule.setOrigDest(machine.getExternalIPs());
						externalDNATRule.setInvertSource(true);
						
						rules.add(externalDNATRule);
						
						Rule externalDNATListenRule = new Rule();
						externalDNATListenRule.setAction(Action.ACCEPT);
						externalDNATListenRule.setSourceZone("all+");
						externalDNATListenRule.setProto(encapsulation);
						externalDNATListenRule.setDestinationZone(machine.getLabel());
						externalDNATListenRule.setDPorts(dPorts);
						
						rules.add(externalDNATListenRule);
					} else {
						Rule listenRule = new Rule();
						listenRule.setAction(Action.ACCEPT);
						listenRule.setSourceZone("all+");
						listenRule.setProto(encapsulation);
						listenRule.setDestinationZone(machine.getLabel());
						listenRule.setDPorts(dPorts);
						
						rules.add(listenRule);
					}
					
					machine.getDNAT().forEach((destination, dnatPorts)->{
						Rule dnatRule = new Rule();
						dnatRule.setAction(Action.DNAT);
						dnatRule.setSourceZone(machine.getLabel());
						dnatRule.setDestinationZone(machine.getLabel());
						dnatRule.setDestinationSubZone(
								machine.getIPs().stream().map(dest -> dest.withoutPrefixLength().toCompressedString())
										.collect(Collectors.joining(",")));
						dnatRule.setDPorts(dPorts);
						dnatRule.setProto(encapsulation);
						dnatRule.setInvertSource(true);
						
						try {
							dnatRule.setOrigDest(getNetworkModel().getMachineModel(destination).getIPs());
						} catch (InvalidMachineModelException | IncompatibleAddressException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						rules.add(dnatRule);
					});
				});
			});
		}
		else {
			;; //TODO
		}

		return rules;
	}

	/**
	 * Build our shorewall zones file.
	 *
	 * See http://shorewall.net/manpages/shorewall-zones.html for more details
	 *
	 * @return the zones file
	 */
	private FileUnit getZonesFile() {
		final FileUnit zones = new FileUnit("shorewall_zones", "shorewall_installed", CONFIG_BASEDIR + "/zones");

		zones.appendLine("#This is the file which creates our various zones");
		zones.appendLine("#Please see http://shorewall.net/manpages/shorewall-zones.html for more details");
		zones.appendLine("#zone\ttype");
		
		if (this.vlans != null) {
			this.vlans.forEach((vlan, type) -> {
				zones.appendLine("#" + vlan.getIface());
				zones.appendLine(cleanZone(vlan.getIface()) + "\tipv4");
				
				if (!getNetworkModel().getMachines(type).isEmpty()) {
					getNetworkModel().getMachines(type).forEach((label, machine) -> {
						zones.appendLine(cleanZone(vlan.getIface()) + ":" + cleanZone(label) + "\tipv4" + "\t#" + label);
					});
				}
				zones.appendCarriageReturn();
			});
		}
		
		return zones;
	}

	@Override
	public Collection<IUnit> getLiveConfig() throws ARuntimeException {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(getZonesFile());

		// Now assign machines their (sub)zone, and enforce our maclist
		final FileUnit hosts = new FileUnit("shorewall_hosts", "shorewall_interfaces", CONFIG_BASEDIR + "/hosts");
		hosts.appendLine(getHostsFile().toArray(String[]::new));

		units.add(hosts);

		final FileUnit maclist = new FileUnit("shorewall_maclist", "shorewall_hosts", CONFIG_BASEDIR + "/maclist");
		maclist.appendLine(getMaclistFile().toArray(String[]::new));

		units.add(maclist);

		// Finally, build our FW rules...
		final FileUnit rules = new FileUnit("shorewall_rules", "shorewall_hosts", CONFIG_BASEDIR + "/rules");
		try {
			getRulesFile().forEach(rule -> {
				rules.appendLine(rule.getRule());
			});
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

	/**
	 * This is where we build our default rules
	 *
	 * @return
	 */
	private FileUnit getPoliciesFile() {
		final FileUnit policies = new FileUnit("shorewall_policies", "shorewall_installed", CONFIG_BASEDIR + "/policy");
		policies.appendLine("#Default policies to use for intra-zone communication");
		policies.appendLine("#For specific rules, please look at " + CONFIG_BASEDIR + "/rules");
		policies.appendLine("#Please see http://shorewall.net/manpages/shorewall-policy.html for more details");
		policies.appendLine("#source\tdestination\taction");
		policies.appendLine("Internet\tall+\tDROP"); // DROP all ingress traffic
		policies.appendLine("all+\tall+\tREJECT"); // REJECT all other traffic

		return policies;
	}

	private FileUnit getInterfacesFile() {
		// Dedicate interfaces to parent zones
		final FileUnit interfaces = new FileUnit("shorewall_interfaces", "shorewall_policies",
				CONFIG_BASEDIR + "/interfaces");
		interfaces.appendLine("#Dedicate interfaces to parent zones");
		interfaces.appendLine("#Please see http://shorewall.net/manpages/shorewall-interfaces.html for more details");
		interfaces.appendLine("#zone\tinterface\tbroadcast\toptions");

		// First work out our Internet-facing NICs
		try {
			if (getNetworkModel().getData().getNetworkInterfaces(getLabel()).get(Direction.WAN) != null) {
					getNetworkModel().getData().getNetworkInterfaces(getLabel()).get(Direction.WAN).forEach((iface, nicData) -> {
						List<NetworkInterfaceModel> nicModels = me.getNetworkInterfaces()
								.stream()
								.filter((model) -> model.getIface().equals(iface))
								.collect(Collectors.toList());

						nicModels.forEach(nicModel -> {
							interfaces.appendLine(buildInterfaceLine(nicModel, MachineType.INTERNET));
						});
					});
			}
		} catch (JsonParsingException | ADataException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Then do everything else
		if (this.vlans != null) {
			this.vlans.forEach((nic, type) -> {
				interfaces.appendLine(buildInterfaceLine(nic, type));
			});
		}

		return interfaces;
	}

	private String buildInterfaceLine(NetworkInterfaceModel nic, MachineType type) {
		String line = "";
		line += cleanZone(type.toString());
		line += "\t" + nic.getIface();
		line += "\t-\t";
		//If it's explicitly DHCP or it's on our LAN, it must talk DHCP
		line += (nic.getInet().equals(Inet.DHCP) || ParentZone.lanZone.stream().anyMatch(zone -> zone.parentZone.equals(type))) ? "dhcp," : "";
		line += "routefilter,arp_filter";
		return line;
	}

	private FileUnit getMasqFile() {
		final FileUnit masq = new FileUnit("shorewall_masquerades", "shorewall_installed", CONFIG_BASEDIR + "/masq");
		try {
			if (getNetworkModel().getData().getNetworkInterfaces(getLabel()).get(Direction.WAN) != null) {
				getNetworkModel().getData().getNetworkInterfaces(getLabel()).get(Direction.WAN)
						.forEach((iface, nic) -> {
							this.vlans.forEach((vlan, zone) -> {
								final String line = iface + "\t" + vlan.getIface();

								if (!masq.containsLine(line)) {
									masq.appendLine(line);
								}
							});
						});
			}
		} catch (JsonParsingException | ADataException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return masq;
	}

	@Override
	public Collection<IUnit> getPersistentConfig() throws ARuntimeException {
		final Collection<IUnit> units = new ArrayList<>();

		final FileEditUnit shorewallConf = new FileEditUnit("shorewall_implicit_continue_on", "shorewall_installed",
				"IMPLICIT_CONTINUE=No", "IMPLICIT_CONTINUE=Yes", "/etc/shorewall/shorewall.conf",
				"I couldn't enable implicit continue on your firewall - this means many of our firewall configurations will fail.");
		units.add(shorewallConf);
		units.add(getPoliciesFile());
		units.add(getInterfacesFile());
		units.add(getMasqFile());

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
				addresses.add(getAddresses(nic.getAddresses()));
			}
		});

		return String.join(",", addresses);
	}

	/**
	 * Returns a comma-delimited string of given IP addresses, with hardcoded /32 subnet
	 * @param ips
	 * @return 
	 */
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
	 * @param type the zone
	 * @param machines the machines
	 * @return the hosts file contents
	 */
	private Collection<String> machines2Host(MachineType type, Collection<AMachineModel> machines) {
		final Collection<String> hosts = new ArrayList<>();

		hosts.add("");
		hosts.add("#" + type.toString());
		
		for (final AMachineModel machine : machines) {
			
			try {
				if (getNetworkModel().getServerModel(machine.getLabel()).isRouter()) {
					continue;
				}
			} catch (InvalidServerModelException e) {
				//As you were. This is not the droid you're looking for.
			}
			
			hosts.add(cleanZone(machine.getLabel()) + "\t" + type.toString() + ":"
					+ getAddresses(machine) + "\tmaclist");
		}

		return hosts;
	} 

	/**
	 * Parses a Machine into shorewall maclist lines
	 *
	 * @param machine
	 * @param type
	 * @return
	 */
	private Collection<String> machines2Maclist(MachineType type, Collection<AMachineModel> machines) {
		final Collection<String> maclist = new ArrayList<>();

		maclist.add("");
		maclist.add("#" + type.toString());

		for (final AMachineModel machine : machines) {
			
			try {
				if (getNetworkModel().getServerModel(machine.getLabel()).isRouter()) {
					continue;
				}
			} catch (InvalidServerModelException e) {
				//As you were. This is not the droid you're looking for.
			}
			
			machine.getNetworkInterfaces().forEach(nic -> {
				if (nic.getMac() != null) {
					maclist.add("ACCEPT\t" + type.toString() + "\t" + nic.getMac().toNormalizedString()
							+ "\t" + getAddresses(machine) + "\t#" + machine.getLabel());

				}
			});
		}

		return maclist;
	}
}
