/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.core.model.machine;

//import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.privacyinternational.thornsec.core.StringUtils;
import org.privacyinternational.thornsec.core.data.machine.AMachineData;
import org.privacyinternational.thornsec.core.data.machine.AMachineData.MachineType;
import org.privacyinternational.thornsec.core.data.machine.configuration.NetworkInterfaceData;
import org.privacyinternational.thornsec.core.data.machine.configuration.NetworkInterfaceData.Inet;
import org.privacyinternational.thornsec.core.data.machine.configuration.TrafficRule;
import org.privacyinternational.thornsec.core.data.machine.configuration.TrafficRule.Encapsulation;
import org.privacyinternational.thornsec.core.data.machine.configuration.TrafficRule.Table;
import org.privacyinternational.thornsec.core.exception.AThornSecException;
import org.privacyinternational.thornsec.core.exception.data.InvalidIPAddressException;
import org.privacyinternational.thornsec.core.exception.data.InvalidPortException;
import org.privacyinternational.thornsec.core.iface.IUnit;
import org.privacyinternational.thornsec.core.model.AModel;
import org.privacyinternational.thornsec.core.model.machine.configuration.networking.DHCPClientInterfaceModel;
import org.privacyinternational.thornsec.core.model.machine.configuration.networking.NetworkInterfaceModel;
import org.privacyinternational.thornsec.core.model.machine.configuration.networking.StaticInterfaceModel;
import org.privacyinternational.thornsec.core.model.network.NetworkModel;
import org.privacyinternational.thornsec.core.profile.AProfile;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IncompatibleAddressException;
import inet.ipaddr.MACAddressString;
import inet.ipaddr.mac.MACAddress;
import org.privacyinternational.thornsec.profile.type.AMachine;
import org.privacyinternational.thornsec.profile.type.Device;
import org.privacyinternational.thornsec.profile.type.Server;
import org.privacyinternational.thornsec.profile.type.Service;

/**
 * This class represents a Machine on our network.
 * 
 * Everything on our network is a descendant from this class, 
 *
 * This is where we stash our various networking rules
 */
public abstract class AMachineModel extends AModel {
	private Map<String, NetworkInterfaceModel> networkInterfaces;

	private HostName domain;
	private Set<String> cnames;

	private InternetAddress emailAddress;

	private Map<MachineType, AProfile> types;

	private Boolean throttled;

	private Set<TrafficRule> firewallRules;
	private Set<IPAddress> externalIPs;

	AMachineModel(AMachineData myData, NetworkModel networkModel) throws AThornSecException {
		super(myData, networkModel);

		setTypesFromData(myData);
		setEmailFromData(myData);
		setNICsFromData(myData);
		setDomainFromData(myData);
		setCNAMEsFromData(myData);
		setExternalIPsFromData(myData);
		setFirewallFromData(myData);
	}

	private void setFirewallFromData(AMachineData myData) {
		this.firewallRules = myData.getTrafficRules();

		//assertNotNull(this.firewallRules);
	}

	public void addFirewallRule(TrafficRule rule) {
		//assertNotNull(rule);

		this.firewallRules.add(rule);
	}

	private void setExternalIPsFromData(AMachineData myData) {
		this.externalIPs = myData.getExternalIPs();	

		//assertNotNull(this.externalIPs);
	}

	private void setCNAMEsFromData(AMachineData myData) {
		this.cnames = myData.getCNAMEs().orElse(new LinkedHashSet<>());
	}

	private void setDomainFromData(AMachineData myData) {
		this.domain = myData.getDomain().orElse(new HostName("lan"));
	}

	/**
	 * Set up and initialise our various NICs as set from our Data
	 * @param myData
	 * @throws AThornSecException
	 */
	private void setNICsFromData(AMachineData myData) throws AThornSecException {
		if (myData.getNetworkInterfaces().isEmpty()) {
			return;
		}

		for (NetworkInterfaceData nicData : myData.getNetworkInterfaces().get().values()) {
			NetworkInterfaceModel nicModel = buildNICFromData(nicData);
			nicModel.init();
			this.addNetworkInterface(nicModel);
		}
	}

	private NetworkInterfaceModel buildNICFromData(NetworkInterfaceData ifaceData) throws AThornSecException {
		NetworkInterfaceModel nicModel = null;
		
		if (null == ifaceData.getInet()
				|| ifaceData.getInet().equals(Inet.STATIC)) {
			nicModel = new StaticInterfaceModel(ifaceData, getNetworkModel());
		}
		else {
			nicModel = new DHCPClientInterfaceModel(ifaceData, getNetworkModel());
		}

		return nicModel;
	}

	private void setEmailFromData(AMachineData myData) {
		try {
			this.emailAddress = myData.getEmailAddress()
					.orElse(new InternetAddress(getLabel() + "@" + getDomain()));
		} catch (AddressException e) {
			;; // You should not be able to get here. 
		}
	}

	private void setTypesFromData(AMachineData myData) {
		this.types = new LinkedHashMap<>();

		myData.getTypes().forEach(type -> {
			addType(type);
		});
	}

	public final void addType(MachineType type) {
//		//assertNotNull(type);

		switch (type) {
			case DEVICE:
				addType(type, new Device((ADeviceModel)this));
				break;
			case SERVER:
				addType(type, new Server((ServerModel)this));
				break;
			case SERVICE:
				addType(type, new Service((ServiceModel)this));
				break;
			default:
				break;
		}
	}

	public final void addType(MachineType type, AMachine profile) {
		this.types.put(type, profile);
	}

	public final void addNetworkInterface(NetworkInterfaceModel ifaceModel) {
		if (this.networkInterfaces == null) {
			this.networkInterfaces = new HashMap<>();
		}

		this.networkInterfaces.put(ifaceModel.getIface(), ifaceModel);
	}

	public final Collection<NetworkInterfaceModel> getNetworkInterfaces() {
		if (null == this.networkInterfaces) {
			return null;
		}

		return this.networkInterfaces.values();
	}

	public final Collection<IPAddress> getExternalIPs() {
		return this.externalIPs;
	}

	public final InternetAddress getEmailAddress() {
		return this.emailAddress;
	}

	public final void setEmailAddress(InternetAddress emailAddress) {
		//assertNotNull(emailAddress);

		this.emailAddress = emailAddress;
	}

	public final Optional<Set<String>> getCNAMEs() {
		return Optional.ofNullable(this.cnames);
	}

	public final void putCNAME(String... cnames) {
		if (this.cnames == null) {
			this.cnames = new LinkedHashSet<>();
		}

		for (final String cname : cnames) {
			this.cnames.add(cname);
		}
	}

	public HostName getDomain() {
		return this.domain;
	}

	public String getHostName() {
		return StringUtils.stringToAlphaNumeric(getLabel(), "-");
	}
	
	public final Boolean isThrottled() {
		return this.throttled;
	}

	public final void setIsThrottled(Boolean throttled) {
		this.throttled = throttled;
	}

	public Collection<IUnit> getUnits() throws AThornSecException {
		final Collection<IUnit> typesUnits = new ArrayList<>();

		for (final AProfile type : getTypes().values()) {
			typesUnits.addAll(type.getUnits());
		}

		return typesUnits;
	}

	/**
	 * Get all LAN IP addresses relating to this machine. 
	 * @return
	 */
	public Collection<IPAddress> getIPs() {
		return getIPs(false);
	}

	/**
	 * Returns all IP addresses related to this machine, optionally including
	 * external IP addresses as set from the data.
	 * @param includeExternalIPs
	 * @return
	 */
	public Collection<IPAddress> getIPs(boolean includeExternalIPs) {
		final Collection<IPAddress> ips = new ArrayList<>();

		getNetworkInterfaces().forEach(nic -> {
			nic.getAddresses().ifPresent(addresses -> ips.addAll(addresses));
		});

		if (includeExternalIPs) {
			ips.addAll(this.getExternalIPs());
		}

		return ips;
	}

	public MACAddress generateMAC(String iface) {
		final String name = getLabel() + iface;

		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-512");
		} catch (final NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		md.update(name.getBytes());

		final byte byteData[] = md.digest();
		final StringBuffer hashCodeBuffer = new StringBuffer();
		for (final byte element : byteData) {
			hashCodeBuffer.append(Integer.toString((element & 0xff) + 0x100, 16).substring(1));

			if (hashCodeBuffer.length() == 6) {
				break;
			}
		}

		final String address = "080027" + hashCodeBuffer.toString();

		try {
			return new MACAddressString(address).toAddress();
		} catch (final AddressStringException | IncompatibleAddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public final void addType(MachineType type, AProfile profile) {
		if (this.types == null) {
			this.types = new LinkedHashMap<>();
		}

		this.types.put(type, profile);
	}
	
	public final Map<MachineType, AProfile> getTypes() {
		return this.types;
	}

	public final Boolean isType(MachineType type) {
		return getType(type) != null;
	}

	public AProfile getType(MachineType type) {
		return this.types.getOrDefault(type, null);
	}

	public Set<TrafficRule> getFirewallRules() {
		return this.firewallRules;
	}

	/**
	 * Add a TCP egress (outbound/Internet) firewall rule to this machine
	 * @param destination a HostName representing the destination as either a
	 * 			hostname e.g. privacyinternational.org or IP address 
	 * @throws InvalidPortException if the port you're attempting to set is
	 * 			invalid
	 */
	public void addEgress(HostName destination) throws InvalidPortException {
		addEgress(Encapsulation.TCP, destination);
	}

	/**
	 * Add an egress (outbound/Internet) firewall rule to this machine
	 * @param encapsulation TCP/UDP
	 * @param destination a HostName representing the destination as either a
	 * 			hostname e.g. privacyinternational.org or IP address, optionally
	 * 			with a port (host:port) or defaults to 443
	 * @throws InvalidPortException if the port you're attempting to set is
	 * 			invalid
	 */
	public void addEgress(Encapsulation encapsulation, HostName destination) throws InvalidPortException {
		TrafficRule egressRule = new TrafficRule();

		egressRule.setTable(Table.EGRESS);

		egressRule.setSource(this.getHostName());

		egressRule.addDestination(destination);
		if (destination.getPort() == null) {
			egressRule.addPorts(443);
		}
		else {
			egressRule.addPorts(destination.getPort());
		}

		egressRule.setEncapsulation(encapsulation);

		this.addFirewallRule(egressRule);
	}

	/**
	 * Set this Machine to listen to TCP traffic on the provided port.
	 * 
	 * If the machine has been given (an) external IP(s), builds an ingress rule
	 * & allows access from LAN.
	 * 
	 * Don't use this method if you don't want these ports to be potentially
	 * publicly accessible.
	 * @param ports port(s) to listen on
	 * @throws InvalidPortException if trying to set an invalid port
	 */
	public void addListen(Integer port) throws InvalidPortException {
		addListen(Encapsulation.TCP, port);
	}

	/**
	 * Set this Machine to listen to {TCP|UDP} traffic *on all available interfaces*
	 * on the provided port(s).
	 * 
	 * If the machine has been given (an) external IP(s), builds an ingress rule
	 * & allows access from LAN.
	 * 
	 * Don't use this method if you don't want these ports to be potentially
	 * publicly accessible.
	 * 
	 * @param encapsulation TCP|UDP 
	 * @param ports port(s) to listen on
	 * @throws InvalidPortException if trying to listen on an invalid port
	 */
	public void addListen(Encapsulation encapsulation, Integer... ports) throws InvalidPortException {
		if (! this.getExternalIPs().isEmpty()) {
			try {
				addWANOnlyListen(encapsulation, ports);
			}
			catch (InvalidIPAddressException e) {
				;; //You shouldn't be able to get here.
				;; //Famous last words, right? :)
				e.printStackTrace();
			}
		}

		addLANOnlyListen(encapsulation, ports);
	}

	/**
	 * Set this Machine to listen to {TCP|UDP} traffic from our LAN machines on
	 * the provided port(s).
	 * 
	 * This method only exposes the port(s) to our LAN.
	 * @param encapsulation TCP|UDP
	 * @param ports port(s) to listen on
	 * @throws InvalidPortException if trying to listen on an invalid port
	 */
	public void addLANOnlyListen(Encapsulation encapsulation, Integer... ports) throws InvalidPortException {
		TrafficRule internalListenRule = new TrafficRule();

		internalListenRule.setTable(Table.FORWARD);
		internalListenRule.setEncapsulation(encapsulation);
		internalListenRule.addPorts(ports);
		internalListenRule.addDestination(new HostName(this.getHostName()));
		internalListenRule.setSource("*");

		this.addFirewallRule(internalListenRule);
	}

	/**
	 * Set this Machine to listen to {TCP|UDP} traffic from The Internet on the
	 * provided port(s).
	 * 
	 * This method makes this machine publicly accessible on its external IP
	 * address.
	 * @param encapsulation TCP|UDP
	 * @param ports port(s) to listen on
	 * @throws InvalidIPAddressException if the machine doesn't have public IPs
	 * @throws InvalidPortException if trying to listen on an invalid port
	 */
	public void addWANOnlyListen(Encapsulation encapsulation, Integer... ports) throws InvalidIPAddressException, InvalidPortException {
		if (this.getExternalIPs().isEmpty()) {
			throw new InvalidIPAddressException("Trying to listen to WAN on "
					+ getLabel() + " but it has no pulicly accessible IP address.");
		}

		TrafficRule externalListenRule = new TrafficRule();

		externalListenRule.setTable(Table.INGRESS);
		externalListenRule.setEncapsulation(encapsulation);
		externalListenRule.addPorts(ports);
		externalListenRule.addDestination(new HostName(this.getHostName()));
		externalListenRule.setSource("*");

		this.addFirewallRule(externalListenRule);
	}

	/**
	 * Redirect traffic from $originalDestination:$ports to $this:$ports.
	 * 
	 * This is done using Destination Network Address Translation, which replaces
	 * the $originalDestination IP address in each packet with $this
	 * @param encapsulation TCP|UDP
	 * @param originalDestination the original destination machine
	 * @param ports ports
	 * @throws InvalidPortException 
	 */
	public void addDNAT(Encapsulation encapsulation, AMachineModel originalDestination, Integer... ports) throws InvalidPortException {
		TrafficRule dnatRule = new TrafficRule();

		dnatRule.setTable(Table.DNAT);
		dnatRule.setEncapsulation(encapsulation);
		dnatRule.addPorts(ports);
		dnatRule.setSource(originalDestination.getHostName());
		dnatRule.addDestination(new HostName(this.getHostName()));

		this.addFirewallRule(dnatRule);
	}

	/**
	 * Redirect TCP traffic from $originalDestination:$ports to $this:$ports
	 * 
	 * This is done using Destination Network Address Translation, which replaces
	 * the $originalDestination IP address in each packet with $this
	 * @param originalDestination the original destination address
	 * @param ports ports
	 * @throws InvalidPortException 
	 */
	public void addDNAT(AMachineModel originalDestination, Integer... ports) throws InvalidPortException {
		addDNAT(Encapsulation.TCP, originalDestination, ports);
	}
}
