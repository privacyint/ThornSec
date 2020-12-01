/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.data.machine.configuration;

//import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import javax.json.JsonObject;

import core.data.AData;
import core.exception.data.ADataException;
import core.exception.data.InvalidIPAddressException;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddress.IPVersion;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.IncompatibleAddressException;
import inet.ipaddr.MACAddressString;
import inet.ipaddr.mac.MACAddress;

/**
 * Represents a network interface. Its internal data represents
 * https://wiki.debian.org/NetworkConfiguration#iproute2_method which means we
 * can allow our admins to have fine-grained control over their ifaces!
 */
public class NetworkInterfaceData extends AData {
	public enum Direction {
		LAN, WAN;
	}

	public enum Inet {
		MANUAL("manual"),
		STATIC("static"),
		DHCP("dhcp"),
		MACVLAN("macvlan"),
		BOND("bond"),
		PPP("PPPoE"),
		DUMMY("dummy"),
		WIREGUARD("wireguard");

		private String inet;

		Inet(String inet) {
			this.inet = inet;
		}

		public String toString() {
			return this.inet;
		}
	}

	private String iface;
	private String comment;

	private Direction direction;
	private Inet inet;
	private MACAddress mac;

	private Collection<IPAddress> addresses;
	private IPAddress gateway;
	private IPAddress subnet;
	private IPAddress netmask;
	private IPAddress broadcast;

	public NetworkInterfaceData(String label) {
		super(label);

		this.iface = null;
		this.direction = null;
		this.inet = null;
		this.addresses = null;
		this.gateway = null;
		this.subnet = null;
		this.broadcast = null;
		this.mac = null;
		this.comment = null;
	}

	@Override
	public void read(JsonObject data) throws ADataException {
		this.iface = data.getString("iface", null);

		readInet(data);
		readDirection(data);
		readAddress(data);
		readSubnet(data);
		readBroadcast(data);
		readGateway(data);
		readMAC(data);
		readComment(data);
	}

	/**
	 * @param data
	 */
	private void readComment(JsonObject data) {
		if (!data.containsKey("comment")) {
			return;
		}

		setComment(data.getString("comment"));
	}

	/**
	 * @param data
	 */
	private void readMAC(JsonObject data) {
		if (!data.containsKey("mac")) {
			return;
		}

		setMAC(new MACAddressString(data.getString("mac")).getAddress());
	}

	/**
	 * @param data
	 * @throws AddressStringException
	 * @throws IncompatibleAddressException
	 */
	private void readGateway(JsonObject data) throws InvalidIPAddressException {
		if (!data.containsKey("gateway")) {
			return;
		}

		try {
			setGateway(new IPAddressString(data.getString("gateway")).toAddress(IPVersion.IPV4));
		} catch (AddressStringException | IncompatibleAddressException e) {
			throw new InvalidIPAddressException(data.getString("gateway"));
		}
	}

	/**
	 * @param data
	 * @throws AddressStringException
	 * @throws IncompatibleAddressException
	 */
	private void readBroadcast(JsonObject data) throws InvalidIPAddressException {
		if (!data.containsKey("broadcast")) {
			return;
		}

		try {
			setBroadcast(new IPAddressString(data.getString("broadcast")).toAddress(IPVersion.IPV4));
		} catch (AddressStringException | IncompatibleAddressException e) {
			throw new InvalidIPAddressException(data.getString("broadcast"));
		}
	}

	/**
	 * @param data
	 * @throws AddressStringException
	 * @throws IncompatibleAddressException
	 */
	private void readSubnet(JsonObject data) throws InvalidIPAddressException {
		if (!data.containsKey("subnet")) {
			return;
		}

		try {
			setSubnet(new IPAddressString(data.getString("subnet")).toAddress(IPVersion.IPV4));
		} catch (AddressStringException | IncompatibleAddressException e) {
			throw new InvalidIPAddressException(data.getString("subnet"));
		}
	}

	/**
	 * @param data
	 * @throws AddressStringException
	 * @throws IncompatibleAddressException
	 */
	private void readAddress(JsonObject data) throws InvalidIPAddressException {
		if (!data.containsKey("address")) {
			return;
		}

		try {
			addAddress(new IPAddressString(data.getString("address")).toAddress(IPVersion.IPV4));
		} catch (AddressStringException | IncompatibleAddressException e) {
			throw new InvalidIPAddressException(data.getString("address"));
		}
	}

	/**
	 * @param data
	 */
	private void readDirection(JsonObject data) {
		if (!data.containsKey("direction")) {
			return;
		}

		setDirection(Direction.valueOf(data.getString("direction").toUpperCase()));
	}

	/**
	 * @param data
	 */
	private void readInet(JsonObject data) {
		if (!data.containsKey("inet")) {
			return;
		}

		setInet(Inet.valueOf(data.getString("inet").toUpperCase()));
	}

	final public void setDirection(Direction direction) {
		this.direction = direction;
	}

	final public Optional<Collection<IPAddress>> getAddresses() {
		return Optional.ofNullable(this.addresses);
	}

	final public Optional<IPAddress> getBroadcast() {
		return Optional.ofNullable(this.broadcast);
	}

	final public Optional<String> getComment() {
		return Optional.ofNullable(this.comment);
	}

	final public Optional<IPAddress> getGateway() {
		return Optional.ofNullable(this.gateway);
	}

	final public String getIface() {
		return this.iface;
	}
	
	final public Direction getDirection() {
		return this.direction;
	}

	final public Inet getInet() {
		return this.inet;
	}

	final public Optional<MACAddress> getMAC() {
		return Optional.ofNullable(this.mac);
	}

	final public Optional<IPAddress> getNetmask() {
		return Optional.ofNullable(this.netmask);
	}

	final public Optional<IPAddress> getSubnet() {
		return Optional.ofNullable(this.subnet);
	}

	/**
	 * 
	 * @param address
	 * @return true if the IP address was successfully added,
	 * 		  false if IP already exists on this interface 
	 */
	protected final Boolean addAddress(IPAddress address) {
		if (this.addresses == null) {
			this.addresses = new HashSet<>();
		}
		
		return this.addresses.add(address);
	}

	public final void addAddress(IPAddress... addresses) {
		for (int i = 0; i < addresses.length; i++) {
			addAddress(addresses[i]);
		}
	}

	protected final void setBroadcast(IPAddress broadcast) {
		this.broadcast = broadcast;
	}

	protected final void setComment(String comment) {
		this.comment = comment;
	}

	protected final void setGateway(IPAddress gateway) {
		this.gateway = gateway;
	}

	public final void setIface(String iface) {
		this.iface = iface;
	}

	protected final void setInet(Inet inet) {
		this.inet = inet;
	}

	public final void setMAC(MACAddress mac) {
		this.mac = mac;
	}

	protected final void setNetmask(IPAddress netmask) {
		this.netmask = netmask;
	}

	protected final void setSubnet(IPAddress subnet) {
		this.subnet = subnet;
	}
}
