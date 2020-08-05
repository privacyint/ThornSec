/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;

import java.util.Optional;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import core.data.machine.AMachineData.Encapsulation;
import core.exception.AThornSecException;
import core.exception.data.ADataException;
import core.exception.data.InvalidPortException;
import core.iface.IUnit;
import core.model.AModel;
import core.model.machine.configuration.networking.NetworkInterfaceModel;
import core.model.network.NetworkModel;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IncompatibleAddressException;
import inet.ipaddr.MACAddressString;
import inet.ipaddr.mac.MACAddress;

/**
 * This class represents a Machine on our network.
 *
 * This is where we stash our various networking rules
 */
public abstract class AMachineModel extends AModel {
	private ArrayList<NetworkInterfaceModel> networkInterfaces;

	private final HostName domain;
	private final Collection<String> cnames;

	private InternetAddress emailAddress;

	private Integer cidr;

	private Boolean throttled;

	private final Map<Encapsulation, Collection<Integer>> listens;

	private Collection<String> forwards;
	private Collection<HostName> ingresses;
	private Collection<HostName> egresses;
	private final Collection<IPAddress> externalIPs;

	private final Map<String, Collection<Integer>> dnats;

	AMachineModel(String label, NetworkModel networkModel) throws AddressException, JsonParsingException, ADataException, IOException {
		super(label, networkModel);

		this.networkInterfaces = new ArrayList<>();
		
		this.emailAddress = getNetworkModel().getData().getEmailAddress(getLabel());

		this.domain = getNetworkModel().getData().getDomain(getLabel());
		this.cnames = getNetworkModel().getData().getCNAMEs(getLabel());

		this.externalIPs = getNetworkModel().getData().getExternalIPs(getLabel());

		this.throttled = getNetworkModel().getData().isThrottled(getLabel());

		this.listens = getNetworkModel().getData().getListens(getLabel());
		this.ingresses = getNetworkModel().getData().getIngresses(getLabel());
		this.egresses = getNetworkModel().getData().getEgresses(getLabel());
		this.forwards = getNetworkModel().getData().getForwards(getLabel());
		this.dnats = getNetworkModel().getData().getDNATs(getLabel());
	}

//	final private NetworkInterfaceModel ifaceDataToModel(NetworkInterfaceData ifaceData) {
//		final NetworkInterfaceModel ifaceModel = new NetworkInterfaceModel(getLabel(), getNetworkModel());
//
//		ifaceModel.setIface(ifaceData.getIface());
//		ifaceModel.setAddress(ifaceData.getAddress());
//		// ifaceModel.setMACVLANs(ifaceData.getMACVLANs());
//		ifaceModel.setBroadcast(ifaceData.getBroadcast());
//		ifaceModel.setComment(ifaceData.getComment());
//		ifaceModel.setGateway(ifaceData.getGateway());
//		ifaceModel.setInet(ifaceData.getInet());
//		ifaceModel.setMac(ifaceData.getMAC());
//		ifaceModel.setNetmask(ifaceData.getNetmask());
//		ifaceModel.setSubnet(ifaceData.getSubnet());
//
//		return ifaceModel;
//	}

	public final Integer getCIDR() {
		return this.cidr;
	}

	public final void setCIDR(Integer cidr) {
		this.cidr = cidr;
	}

	public final void addNetworkInterface(NetworkInterfaceModel ifaceModel) {
		this.networkInterfaces.add(ifaceModel);
	}

	public final Collection<NetworkInterfaceModel> getNetworkInterfaces() {
		return this.networkInterfaces;
	}

	public final void addIngress(String... sources) {
		for (final String source : sources) {
			addIngress(new HostName(source));
		}
	}

	public final void addIngress(HostName... sources) {
		if (this.ingresses == null) {
			this.ingresses = new HashSet<>();
		}

		for (final HostName source : sources) {
			this.ingresses.add(source);
		}
	}

	public final Collection<HostName> getIngresses() {
		return this.ingresses;
	}

	public final void addDNAT(String destination, Integer... ports) throws InvalidPortException {
		Collection<Integer> dnats = this.dnats.get(destination);

		if (dnats == null) {
			dnats = new HashSet<>();
		}

		for (final Integer port : ports) {
			if ((port < 1) || (port > 65535)) {
				throw new InvalidPortException(port);
			}

			dnats.add(port);
		}

		this.dnats.put(destination, dnats);
	}

	public final Map<String, Collection<Integer>> getDNAT() {
		return this.dnats;
	}

	public final void addListen(Encapsulation enc, Integer... ports) throws InvalidPortException {
		Collection<Integer> listening = this.listens.get(enc);

		if (listening == null) {
			listening = new HashSet<>();
		}

		for (final Integer port : ports) {
			if ((port < 1) || (port > 65535)) {
				throw new InvalidPortException(port);
			}

			listening.add(port);
		}

		this.listens.put(enc, listening);
	}

	public final Map<Encapsulation, Collection<Integer>> getListens() {
		return this.listens;
	}

	public final void addEgress(String... egresses) {
		for (final String egress : egresses) {
			addEgress(new HostName(egress));
		}
	}

	public final void addEgress(HostName... egresses) {
		if (this.egresses == null) {
			this.egresses = new LinkedHashSet<>();
		}

		for (final HostName egress : egresses) {
			this.egresses.add(egress);
		}

	}

	public final Collection<HostName> getEgresses() {
		return this.egresses;
	}

	public final Map<String, Collection<Integer>> getRequiredDnat() {
		return this.dnats;
	}

	public final void addForward(String... destinations) {
		if (this.forwards == null) {
			this.forwards = new HashSet<>();
		}

		for (final String destination : destinations) {
			this.forwards.add(destination);
		}
	}

	public final Collection<String> getForwards() {
		return this.forwards;
	}

	public final Collection<IPAddress> getExternalIPs() {
		return this.externalIPs;
	}

	public final InternetAddress getEmailAddress() {
		return this.emailAddress;
	}

	public final void setEmailAddress(InternetAddress emailAddress) {
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

	public Collection<IPAddress> getIPs() {
		final Collection<IPAddress> ips = new ArrayList<>();

		for (final NetworkInterfaceModel nic : getNetworkInterfaces()) {
			if (nic.getAddresses().isEmpty()) {
				continue;
			}
			
			for (final IPAddress ip : nic.getAddresses().get()) {
				if ((getExternalIPs() != null) && getExternalIPs().contains(ip)) {
					continue;
				}
				
				ips.add(ip);
			}
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
	
}
