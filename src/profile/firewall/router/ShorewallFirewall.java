/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.firewall.router;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.json.stream.JsonParsingException;
import core.StringUtils;
import core.data.machine.AMachineData.MachineType;
import core.data.machine.configuration.TrafficRule;
import core.data.machine.configuration.NetworkInterfaceData.Direction;
import core.data.machine.configuration.NetworkInterfaceData.Inet;
import core.data.machine.configuration.TrafficRule.Encapsulation;
import core.exception.AThornSecException;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.ARuntimeException;
import core.exception.runtime.InvalidFirewallRuleException;
import core.exception.runtime.InvalidMachineModelException;
import core.exception.runtime.InvalidProfileException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.machine.AMachineModel;
import core.model.machine.ServerModel;
import core.model.machine.configuration.networking.MACVLANModel;
import core.model.machine.configuration.networking.MACVLANTrunkModel;
import core.unit.fs.FileEditUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;
import profile.firewall.AFirewallProfile;
import profile.type.Router;

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
		INTERNET(Arm.INTERNET, MachineType.INTERNET),
		ROUTER(Arm.FIREWALL, MachineType.ROUTER),
		USERS(Arm.LAN, MachineType.USER),
		ADMINS(Arm.LAN, MachineType.ADMIN),
		SERVERS(Arm.LAN, MachineType.SERVER),
		INTERNAL_ONLY(Arm.LAN, MachineType.INTERNAL_ONLY),
		EXTERNAL_ONLY(Arm.LAN, MachineType.EXTERNAL_ONLY),
		GUESTS(Arm.LAN, MachineType.GUEST),
		VPN(Arm.LAN, MachineType.VPN);

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

	private class ShorewallRule {
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

		public ShorewallRule() {
			macro = null;
			action = null;

			invertSource = false;
			sourceSubZone = null;
			sPorts = null;

			destinationZone = null;
			destinationSubZone = null;
			proto = null;
			dPorts = null;

			origDest = null;

			rate = null;
		}

		public ShorewallRule(TrafficRule rule) throws InvalidFirewallRuleException {
			this();

			switch (rule.getTable()) {
				case DNAT:
					buildDNAT(rule);
					break;
				case EGRESS:
					buildEgress(rule);
					break;
				case FORWARD:
					buildForward(rule);
					break;
				case INGRESS:
					buildIngress(rule);
					break;
				default:
					throw new InvalidFirewallRuleException(rule.getTable() + " is not recognised");
			}
		}

		/**
		 * Ingests an Ingress TrafficRule, turning it into the Shorewall equivalent.
		 * 
		 * The rule's destinationZone must be a valid machine label.
		 * 
		 * @param rule Ingress TrafficRule
		 */
		private void buildIngress(TrafficRule rule) {
			this.setAction(Action.ACCEPT);
			this.setSourceZone(cleanZone(ParentZone.INTERNET.toString()));
			this.setProto(rule.getEncapsulation());
			this.setDPorts(rule.getPorts());
			this.setDestinationZone(
				rule.getDestinations()
					.stream()
					.<String>map(destination -> cleanZone(destination.getHost()))
					.collect(Collectors.joining(","))
			);
			this.setDestinationSubZone(
				rule.getDestinations()
					.stream()
					.<String>map(destination -> destination.getHost())
					.<Collection<IPAddress>>map(label -> this.getMachineModel(label).getIPs())
					.flatMap(Collection::stream)
					.<String>map(ip -> ip.withoutPrefixLength().toCompressedString())
					.collect(Collectors.joining(","))
			);
		}

		/**
		 * This "overrides" the getMachineModel method from our NetworkModel.
		 * 
		 * This method is to be used on the assumption that getMachineModel
		 * will never throw an exception.
		 * @param label Machine's label
		 * @return the requested AMachineModel
		 */
		private AMachineModel getMachineModel(String label) {
			AMachineModel machine = null;

			try {
				machine = getNetworkModel().getMachineModel(label);
			} catch (InvalidMachineModelException e) {
				;; // Famous last words, as ever...
				e.printStackTrace();
			}

			return machine;
		}

		/**
		 * Ingests a Forward (intra-zone) TrafficRule, turning it into the Shorewall equivalent.
		 * 
		 * The rule's destinationZone must be a valid machine label.
		 * 
		 * @param rule Forward TrafficRule
		 */
		private void buildForward(TrafficRule rule) throws InvalidFirewallRuleException {
			Boolean destIsExternallyAccessible = rule.getDestinations()
					.stream()
					.map(destination -> destination.getHost())
					.filter(label -> !this.getMachineModel(label).getExternalIPs().isEmpty())
					.count() > 0;

			this.setAction(Action.ACCEPT);
			if (destIsExternallyAccessible) {
				this.setSourceZone("any");
			}
			else {
				this.setSourceZone(
					getNetworkModel().getSubnets().keySet()
					 .stream()
					 .filter(type -> !getNetworkModel().getMachines(type).isEmpty())
					 .map(type -> cleanZone(type))
					 .collect(Collectors.joining(","))
				);
			}
			this.setProto(rule.getEncapsulation());
			this.setDPorts(rule.getPorts());
			this.setDestinationZone(
				rule.getDestinations()
					.stream()
					.map(destination -> cleanZone(destination.getHost()))
					.collect(Collectors.joining(","))
			);
			this.setDestinationSubZone(
				rule.getDestinations()
					.stream()
					.map(destination -> destination.getHost())
					.map(label -> this.getMachineModel(label).getIPs())
					.flatMap(Collection::stream)
					.filter(ip -> ip.isLocal())
					.map(ip -> ip.withoutPrefixLength().toCompressedString())
					.collect(Collectors.joining(","))
			);
		}

		/**
		 * Ingests an Egress TrafficRule, turning it into the Shorewall equivalent.
		 * @param rule Egress TrafficRule
		 */
		private void buildEgress(TrafficRule rule) throws InvalidFirewallRuleException {
			this.setAction(Action.ACCEPT);
			this.setSourceZone(rule.getSource());
			this.setProto(rule.getEncapsulation());
			this.setDPorts(rule.getPorts());
			this.setDestinationZone(ParentZone.INTERNET.toString());
			this.setDestinationSubZone(
				rule.getDestinations()
					.stream()
					.map(destination -> destination.getHost())
					.collect(Collectors.joining(","))
			);
		}

		/**
		 * Ingests a DNAT TrafficRule, turning it into the Shorewall equivalent.
		 * 
		 * The rule's sourceZone and destinationZone must be valid machine labels
		 * 
		 * @param rule Ingress TrafficRule
		 */
		private void buildDNAT(TrafficRule rule) throws InvalidFirewallRuleException {
			this.setAction(Action.DNAT);
			this.setInvertSource(true); //don't DNAT if we're the source
			this.setSourceZone(cleanZone(rule.getSource())); //new destination
			this.setSourceSubZone(
				this.getMachineModel(rule.getSource()).getIPs()
					.stream()
					.map(ip -> ip.withoutPrefixLength().toCompressedString())
					.collect(Collectors.joining(","))
			);
			this.setDestinationZone(
				rule.getDestinations()
					.stream()
					.map(destination -> cleanZone(destination.getHost()))
					.collect(Collectors.joining(","))
			); //original destination
			this.setDestinationSubZone(
				rule.getDestinations()
					.stream()
					.map(destination -> destination.getHost())
					.map(label -> this.getMachineModel(label).getIPs())
					.flatMap(Collection::stream)
					.map(ip -> ip.withoutPrefixLength().toCompressedString())
					.collect(Collectors.joining(","))
			);
			this.setDPorts(rule.getPorts());
			this.setProto(rule.getEncapsulation());
			this.setOrigDest(this.getMachineModel(rule.getSource()).getIPs());
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

		public void setSourceSubZone(String sourceSubZone) {
			this.sourceSubZone = sourceSubZone;
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

	private class Comment extends ShorewallRule {
		private final String comment;

		public Comment(String comment) {
			this.comment = comment;
		}

		@Override
		public String getRule() {
			return "# " + this.comment;
		}
	}

	private Router myRouter;

	public ShorewallFirewall(ServerModel me) {
		super(me);
		this.myRouter = (Router) me.getProfiles().get(MachineType.ROUTER.toString());
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
			prefix = "\\$";
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

	/**
	 * Builds our Maclist file as per http://shorewall.org/manpages/shorewall-maclist.html
	 * 
	 * This maclist file reflects the whole network
	 * 
	 * @return the contents of the maclist file
	 * @throws InvalidServerException if you don't have exactly 1 router on your network
	 */
	private Collection<String> getMaclistFile() throws InvalidProfileException {
		final Collection<String> maclist = new ArrayList<>();

		getRouter().getNetworkInterfaces()
		.stream()
		.filter(nic -> nic instanceof MACVLANTrunkModel)
		.forEach(nic -> {
			((MACVLANTrunkModel)nic).getVLANs().forEach(vlan -> {
				Set<AMachineModel> machines = getNetworkModel().getMachines(vlan.getType());
				maclist.addAll(machines2Maclist(vlan.getType(), machines));
			});
		});

		return maclist;
	}

	/**
	 * Get this network's Router
	 * @return the Router
	 * @throws InvalidProfileException if the network contains <>1 router
	 */
	private ServerModel getRouter() throws InvalidProfileException {
		if (getServerModel().isType(MachineType.ROUTER)) {
			return getServerModel();
		}
		else {
			Set<AMachineModel> routers = getNetworkModel().getMachines(MachineType.ROUTER);

			if (routers.isEmpty()) {
				throw new InvalidProfileException("You must have at least one router on your network!");
			}
			else if (routers.size() > 1) {
				throw new InvalidProfileException("You must only have one router on your network!");
			}

			return (ServerModel) routers.iterator().next();
		}
	}

	private Collection<String> getHostsFile() {
		final Collection<String> hosts = new ArrayList<>();

		hosts.add("#Please see http://shorewall.net/manpages/shorewall-zones.html for more details");
		hosts.add("#zone\thosts\toptions");

		getServerModel().getNetworkInterfaces()
		.stream()
		.filter(nic -> nic instanceof MACVLANTrunkModel)
		.forEach(nic -> {
			((MACVLANTrunkModel)nic).getVLANs().forEach(vlan -> {
				Set<AMachineModel> machines = getNetworkModel().getMachines(vlan.getType());

				hosts.addAll(machines2Host(vlan.getType(), machines));
			});
		});

		return hosts;
	}

	private Collection<ShorewallRule> getRulesFile() throws InvalidServerException, InvalidMachineModelException {
		Collection<ShorewallRule> rules = new ArrayList<>();

		if (getMachineModel().isType(MachineType.ROUTER)) {
			// Iterate over every machine to build all of its rules
			getNetworkModel().getMachines().values().forEach((machine) -> {
				if (machine.getFirewallRules().isEmpty()) {
					return;
				}

				Comment machineComment = new Comment(machine.getLabel());
				rules.add(machineComment);

				machine.getFirewallRules().forEach(rule -> {
					try {
						rules.add(new ShorewallRule(rule));
					} catch (InvalidMachineModelException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
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

		getServerModel().getNetworkInterfaces()
		.stream()
		.filter(nic -> nic instanceof MACVLANTrunkModel)
		.forEach(nic -> {
			((MACVLANTrunkModel)nic).getVLANs().forEach(vlan -> {
				zones.appendLine("#" + vlan.getIface());
				zones.appendLine(cleanZone(vlan.getIface()) + "\tipv4");

				getNetworkModel().getMachines(vlan.getType()).forEach(machine -> {
					zones.appendText(cleanZone(machine.getLabel()));
					zones.appendText(machine.isType(MachineType.ROUTER)
										? ""
										: ":" + cleanZone(vlan.getIface())
					);
					zones.appendText(machine.isType(MachineType.ROUTER)
										? "\tfirewall"
										: "\tipv4"
					);
					zones.appendText("\t#" + machine.getLabel());
					zones.appendCarriageReturn();
				});

				zones.appendCarriageReturn();
			});
		});

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
		}
		catch (InvalidServerException | InvalidServerModelException e) {
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
		policies.appendLine(ParentZone.INTERNET + "\tall+\tDROP"); // DROP all ingress traffic
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
//			if (getMachineModel().getNetworkInterfaces().get(Direction.WAN) != null) {
//					getNetworkModel().getData().getNetworkInterfaces(getLabel()).get(Direction.WAN).forEach((iface, nicData) -> {
//						List<NetworkInterfaceModel> nicModels = null;
//						try {
//							nicModels = getMachineModel().getNetworkInterfaces()
//									.stream()
//									.filter((model) -> model.getIface().equals(iface))
//									.collect(Collectors.toList());
//						} catch (InvalidMachineModelException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//
//						nicModels.forEach(nicModel -> {
//							interfaces.appendLine(buildInterfaceLine(nicModel, MachineType.INTERNET));
//						});
//					});
//			}
		} catch (JsonParsingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Then do everything else
		getServerModel().getNetworkInterfaces()
		.stream()
		.filter(nic -> nic instanceof MACVLANTrunkModel)
		.forEach(nic -> {
			((MACVLANTrunkModel)nic).getVLANs().forEach(vlan -> {
				interfaces.appendLine(buildInterfaceLine(vlan));
			});
		});

		return interfaces;
	}

	private String buildInterfaceLine(MACVLANModel nic) {
		String line = "";
		line += cleanZone(nic.getType().toString());
		line += "\t" + nic.getIface();
		line += "\t-\t";
		//If it's explicitly DHCP or it's on our LAN, it must talk DHCP
		line += (nic.getInet().equals(Inet.DHCP) || ParentZone.lanZone.stream().anyMatch(zone -> zone.parentZone.equals(nic.getType()))) ? "dhcp," : "";
		line += "routefilter,arp_filter";
		return line;
	}

	private FileUnit getMasqFile() {
		final FileUnit masq = new FileUnit("shorewall_masquerades", "shorewall_installed", CONFIG_BASEDIR + "/masq");

		getMachineModel().getNetworkInterfaces()
			.stream()
			.filter(nic -> Direction.WAN.equals(nic.getDirection()))
			.forEach(wanNic -> {
				getServerModel().getNetworkInterfaces()
				.stream()
				.filter(_nic -> _nic instanceof MACVLANTrunkModel)
				.forEach(macVLANTrunk -> {
					((MACVLANTrunkModel)macVLANTrunk).getVLANs().forEach(vlan -> {
						final String line = macVLANTrunk.getIface() + "\t" + vlan.getIface();

						if (!masq.containsLine(line)) {
							masq.appendLine(line);
						}
					});
				});
			});

		return masq;
	}

	@Override
	public Collection<IUnit> getPersistentConfig() throws ARuntimeException {
		final Collection<IUnit> units = new ArrayList<>();

		if (this.myRouter == null) {
			return units;
		}

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
			nic.getAddresses().ifPresent(nicAddresses -> {
				nicAddresses.forEach(ip -> {
					addresses.add(ip.withoutPrefixLength().toString());
				});
			});
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

		machines.stream()
			.filter(machine -> !machine.isType(MachineType.ROUTER))
			.forEach(machine -> {
				hosts.add(cleanZone(machine.getLabel())
					+ "\t" + cleanZone(type.toString())
					+ ":" + getAddresses(machine)
					+ "\tmaclist");
			});

		return hosts;
	} 

	/**
	 * Parses machines into shorewall maclist lines
	 *
	 * @param type the machine type, only used for labelling
	 * @param machines the machines to list in the maclist file
	 * @return a Collection of Strings containing the maclist file lines
	 */
	private Collection<String> machines2Maclist(MachineType type, Collection<AMachineModel> machines) {
		final Collection<String> maclist = new ArrayList<>();

		maclist.add("#" + type.toString());

		machines.forEach(machine -> {
			machine.getNetworkInterfaces().stream()
				.filter(nic -> nic.getMac().isPresent())
				.filter(nic -> nic.getAddresses().isPresent())
				.forEach(nic -> {
					String mac = nic.getMac().get().toNormalizedString();
					String addresses = nic.getAddresses().get().stream()
											.map(ip -> ip.withoutPrefixLength())
											.map(Object::toString)
											.collect(Collectors.joining(","));

					maclist.add("ACCEPT"
								+ "\t" + cleanZone(type.toString())
								+ "\t" + mac
								+ "\t" + addresses
								+ "\t#" + machine.getLabel());
				});
		});

		return maclist;
	}
}
