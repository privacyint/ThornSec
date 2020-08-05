/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.data.machine;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import core.data.AData;
import core.data.machine.configuration.NetworkInterfaceData;
import core.data.machine.configuration.TrafficRule;
import core.data.machine.configuration.TrafficRule.Encapsulation;
import core.data.machine.configuration.TrafficRule.Table;
import core.exception.data.ADataException;
import core.exception.data.InvalidIPAddressException;
import core.exception.data.InvalidPortException;
import core.exception.data.machine.InvalidEmailAddressException;
import core.exception.data.machine.configuration.InvalidNetworkInterfaceException;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;

/**
 * Abstract class for something representing a "Machine" on our network.
 *
 * A machine, at its most basic, is something with network interfaces. This
 * means it can also talk TCP and UDP, can be throttled, and has a name
 * somewhere in DNS-world.
 */
public abstract class AMachineData extends AData {
	// These are the only types of machine I'll recognise until I'm told otherwise...
	public enum MachineType {
		ROUTER("Router"),
		SERVER("Servers"),
		HYPERVISOR("Hypervisors"),
		DEDICATED("Dedicateds"),
		SERVICE("Services"),
		DEVICE("Devices"),
		USER("Users"),
		INTERNAL_ONLY("InternalOnlys"),
		EXTERNAL_ONLY("ExternalOnlys"),
		ADMIN("Administrators"),
		GUEST("Guests"),
		VPN("VPN"),
		INTERNET("Internet");

		private String machineType;

		MachineType(String machineType) {
			this.machineType = machineType;
		}

		@Override
		public String toString() {
			return this.machineType;
		}
	}

	public static Boolean DEFAULT_IS_THROTTLED = true;

	private Map<String, NetworkInterfaceData> networkInterfaces;
	private Set<IPAddress> externalIPAddresses;
	private Set<String> cnames;

	// Alerting
	private InternetAddress emailAddress;

	private Boolean throttled;

	private Set<TrafficRule> trafficRules;

	private HostName domain;

	protected Set<MachineType> types;

	protected AMachineData(String label) {
		super(label);

		this.networkInterfaces = null;
		this.emailAddress = null;
		this.throttled = null;

		this.types = new LinkedHashSet<>();
		this.externalIPAddresses = new LinkedHashSet<>();
		this.cnames = new LinkedHashSet<>();
		this.trafficRules = new LinkedHashSet<>();
	}

	@Override
	protected void read(JsonObject data) throws ADataException {
		super.setData(data);

		readEmailAddress();
		readDomain();
		readCNAMEs();
		readFirewallRules();
	}

	/**
	 * Read in any firewall-related data
	 * 
	 * @throws InvalidPortException if a requested port is outside the valid range
	 * @throws InvalidIPAddressException if a requested IP address isn't valid
	 */
	private void readFirewallRules() throws InvalidPortException, InvalidIPAddressException {
		if (!getData().containsKey("firewall")) {
			return;
		}
		
		readFirewallData(getData().getJsonObject("firewall"));		
	}

	/**
	 * Read in any CNAMEs which have been set in the data
	 */
	private void readCNAMEs() {
		if (!getData().containsKey("cnames")) {
			return;
		}

		final JsonArray cnames = getData().getJsonArray("cnames");
		
		for (final JsonValue cname : cnames) {
			putCNAME(((JsonString) cname).getString());
		}		
	}

	/**
	 * Read in this machine's domain, as it may be different to the network's
	 */
	private void readDomain() {
		if (!getData().containsKey("domain")) {
			return;
		}
		
		setDomain(new HostName(getData().getString("domain")));
	}

	/**
	 * Read in this machine's email address, if set
	 * 
	 * @throws InvalidEmailAddressException if the email address is invalid
	 */
	private void readEmailAddress() throws InvalidEmailAddressException {
		if (!getData().containsKey("email")) {
			return;
		}
		
		setEmailAddress(getData().getString("email"));
	}

	/**
	 * Set this machine's email address.
	 * 
	 * @param address The email address for this machine
	 * @throws InvalidEmailAddressException if the email address isn't valid
	 */
	private void setEmailAddress(String address) throws InvalidEmailAddressException {
		try {
			setEmailAddress(new InternetAddress(address));
		} catch (AddressException e) {
			throw new InvalidEmailAddressException(getData().getString("email")
					+ " is an invalid email address");
		}
	}

	/**
	 * Set the email address for this Machine
	 * 
	 * @param emailAddress The email address for this machine
	 */
	private void setEmailAddress(InternetAddress emailAddress) {
		this.emailAddress = emailAddress;
	}

	/**
	 * Read in listen rules for this machine. This punches holes in our Ingress
	 * table, allowing traffic to come into this machine on the requested port(s)
	 * in a given Encapsulation.
	 * 
	 * @param encapsulation The packet encapsulation
	 * @param ports The port(s) on which to listen
	 * @throws InvalidPortException if a port is outside of the valid range
	 */
	private void readListenRules(Encapsulation encapsulation, JsonArray ports) throws InvalidPortException {
		TrafficRule rule = new TrafficRule();
		rule.setEncapsulation(encapsulation);
		rule.setDestination(getLabel());
		rule.setTable(Table.INGRESS);
		
		for (final JsonValue port : ports) {
			rule.addPorts(Integer.parseInt(port.toString()));
		}

		this.addTrafficRule(rule);
	}
	
	/**
	 * Read in firewall-related data
	 * 
	 * @param firewallData The data object "firewall" associated with this machine
	 * @throws InvalidPortException
	 * @throws InvalidIPAddressException
	 */
	private void readFirewallData(JsonObject firewallData) throws InvalidPortException, InvalidIPAddressException {
		readThrottled(firewallData);
		readListens(firewallData);
		readForwards(firewallData);
		readDnats(firewallData);
		readIngresses(firewallData);
		readEgresses(firewallData);
		readExternalIPs(firewallData);
	}
	
	/**
	 * Read in any external IP addresses assigned to this machine
	 * @param firewallData the
	 * @throws InvalidIPAddressException
	 */
	private void readExternalIPs(JsonObject firewallData) throws InvalidIPAddressException {
		if (firewallData.containsKey("external_ip")) {
			putExternalIP(firewallData.getString("external_ip"));
		}
	}

	private void readEgresses(JsonObject firewallData) throws InvalidPortException {
		if (!firewallData.containsKey("allow_egress_to")) {
			return;
		}
		
		final JsonArray destinations = firewallData.getJsonArray("allow_egress_to");

		for (final JsonValue destination : destinations) {
			TrafficRule egressRule = new TrafficRule();
			egressRule.setDestination(((JsonString) destination).getString());
			egressRule.setSource(getLabel());
			
			this.addTrafficRule(egressRule);
		}
	}

	private void readIngresses(JsonObject firewallData) throws InvalidPortException {
		if (!firewallData.containsKey("allow_ingress_from")) {
			return;
		}
		final JsonArray sources = firewallData.getJsonArray("allow_ingress_from");

		for (final JsonValue source : sources) {
			TrafficRule ingressRule = new TrafficRule();
			ingressRule.setSource(((JsonString) source).getString());
			ingressRule.setDestination(getLabel());
			ingressRule.setTable(Table.INGRESS);
			
			this.addTrafficRule(ingressRule);
		}
	}		

	private void readDnats(JsonObject firewallData) throws InvalidPortException {
		if (!firewallData.containsKey("dnat_to")) {
			return;
		}

		final JsonArray destinations = firewallData.getJsonArray("dnat_to");

		for (final JsonValue destination : destinations) {
			TrafficRule dnatRule = new TrafficRule();
			dnatRule.setDestination(((JsonString) destination).getString());
			dnatRule.setSource(getLabel());
			dnatRule.setTable(Table.DNAT);
			
			addTrafficRule(dnatRule);
		}
	}

	private void readForwards(JsonObject firewallData) throws InvalidPortException {
		if (firewallData.containsKey("allow_forward_to")) {
			final JsonArray forwards = firewallData.getJsonArray("allow_forward_to");

			for (final JsonValue forward : forwards) {
				TrafficRule forwardRule = new TrafficRule();
				forwardRule.setDestination(((JsonString) forward).getString());
				forwardRule.setTable(Table.FORWARD);
				
				this.addTrafficRule(forwardRule);
			}
		}
	}

	private void readListens(JsonObject firewallData) throws InvalidPortException {
		if (!firewallData.containsKey("listen")) {
			return;
		}
		
		final JsonObject listens = firewallData.getJsonObject("listen");

		if (listens.containsKey("tcp")) {
			readListenRules(Encapsulation.TCP, listens.getJsonArray("tcp"));
		}
		if (listens.containsKey("udp")) {
			readListenRules(Encapsulation.UDP, listens.getJsonArray("udp"));
		}
	}

	private void readThrottled(JsonObject firewallData) {
		if (!firewallData.containsKey("throttle")) {
			return;
		}
		
		this.throttled = firewallData.getBoolean("throttle");		
	}

	public final Optional<Set<String>> getCNAMEs() {
		return Optional.ofNullable(this.cnames);
	}

	public Optional<HostName> getDomain() {
		return Optional.ofNullable(this.domain);
	}

	public final Set<TrafficRule> getTrafficRules() {
		return this.trafficRules;
	}

	public final Optional<InternetAddress> getEmailAddress() {
		return Optional.ofNullable(this.emailAddress);
	}

	public final Set<IPAddress> getExternalIPs() {
		return this.externalIPAddresses;
	}

	public final Optional<Map<String, NetworkInterfaceData>> getNetworkInterfaces() {
		return Optional.ofNullable(this.networkInterfaces);
	}

	public final Boolean isThrottled() {
		return this.throttled;
	}

	private void putCNAME(String cname) {
		this.cnames.add(cname);
	}

	/**
	 * 
	 * @param dir
	 * @param ifaces
	 * @throws InvalidNetworkInterfaceException
	 */
	protected void putNetworkInterface(NetworkInterfaceData... ifaces) throws InvalidNetworkInterfaceException {
		if (this.networkInterfaces == null) {
			this.networkInterfaces = new LinkedHashMap<>();
		}
		
		for (final NetworkInterfaceData iface : ifaces) {
			if (this.networkInterfaces.containsKey(iface.getIface())) {
				throw new InvalidNetworkInterfaceException("Interfaces can only be declared once");
			}

			this.networkInterfaces.put(iface.getIface(), iface);
		}
	}

	private void addTrafficRule(TrafficRule rule) {
		this.trafficRules.add(rule);
	}

	private void putExternalIP(String address) throws InvalidIPAddressException {
		if (this.externalIPAddresses == null) {
			this.externalIPAddresses = new LinkedHashSet<>();
		}
		
		try {
			this.externalIPAddresses.add(new IPAddressString(address).toAddress());
		}
		catch (final AddressStringException e) {
			throw new InvalidIPAddressException(address + " on machine "
					+ getLabel() + " is not a valid IP Address");
		}		
	}


	private void setDomain(HostName domain) {
		this.domain = domain;
	}

	protected void putType(String... types) {
		for (String type : types) {
			MachineType machineType = MachineType.valueOf(type.replaceAll("[^a-zA-Z]", "").toUpperCase());
			putType(machineType);
		}
	}

	protected void putType(MachineType... types) {
		for (MachineType type : types) {
			this.types.add(type);
		}
	}

	public final Collection<MachineType> getTypes() {
		return this.types;
	}
	
	public final Boolean isType(MachineType type) {
		return this.getTypes().contains(type);
	}
}
