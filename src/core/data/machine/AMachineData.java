/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.data.machine;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.stream.JsonParsingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import core.data.AData;
import core.data.machine.configuration.NetworkInterfaceData;
import core.data.machine.configuration.NetworkInterfaceData.Direction;
import core.exception.data.ADataException;
import core.exception.data.InvalidIPAddressException;
import core.exception.data.InvalidPortException;
import core.exception.data.machine.InvalidEmailAddressException;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.MACAddressString;

/**
 * Abstract class for something representing a "Machine" on our network.
 *
 * A machine, at its most basic, is something with network interfaces. This
 * means it can also talk TCP and UDP, can be throttled, and has a name
 * somewhere in DNS-world.
 *
 * Any of the properties of a "Machine" may be null, this is not where we're
 * doing error checking! :)
 */
public abstract class AMachineData extends AData {
	// Networking
	public enum Encapsulation {
		UDP, TCP
	}

	// These are the only types of machine I'll recognise until I'm told
	// otherwise...
	public enum MachineType {
		ROUTER("Router"), SERVER("Servers"), HYPERVISOR("Hypervisors"), DEDICATED("Dedicateds"), SERVICE("Services"),
		DEVICE("Devices"), USER("Users"), INTERNAL_ONLY("InternalOnlys"), EXTERNAL_ONLY("ExternalOnlys"),
		ADMIN("Administrators"), GUEST("Guests"), VPN("VPN");

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

	private Map<Direction, Collection<NetworkInterfaceData>> networkInterfaces;
	private final Collection<IPAddress> externalIPAddresses;
	private Collection<String> cnames;
	// Alerting
	private InternetAddress emailAddress;

	// Firewall
	private String firewallProfile;
	private Boolean throttled;

	private Map<Encapsulation, Collection<Integer>> listens;
	private Map<String, Collection<Integer>> dnats;
	private Collection<String> forwards;
	private Collection<HostName> ingresses;
	private Collection<HostName> egresses;

	private HostName domain;

	protected AMachineData(String label) {
		super(label);

		this.networkInterfaces = null;

		this.externalIPAddresses = null;

		this.cnames = null;

		this.emailAddress = null;

		this.firewallProfile = null;
		this.throttled = null;

		this.listens = null;

		this.dnats = null;
		this.forwards = null;
		this.ingresses = null;
		this.egresses = null;
	}

	private void addDNAT(String destination, Integer... ports) throws InvalidPortException {
		if (this.dnats == null) {
			this.dnats = new Hashtable<>();
		}

		Collection<Integer> currentPorts = this.dnats.get(destination);

		if (currentPorts == null) {
			currentPorts = new HashSet<>();
		}

		for (final Integer port : ports) {
			if (((port < 0)) || ((port > 65535))) {
				throw new InvalidPortException(port);
			}

			if (!currentPorts.contains(port)) {
				currentPorts.add(port);
			}
		}

		this.dnats.put(destination, currentPorts);
	}

	private void addEgress(HostName destination) {
		if (this.egresses == null) {
			this.egresses = new HashSet<>();
		}

		if (!this.egresses.contains(destination)) {
			this.egresses.add(destination);
		}
	}

	private void addFoward(String label) {
		if (this.forwards == null) {
			this.forwards = new HashSet<>();
		}

		if (!this.forwards.contains(label)) {
			this.forwards.add(label);
		}
	}

	private void addIngress(HostName source) {
		if (this.ingresses == null) {
			this.ingresses = new HashSet<>();
		}
		if (!this.ingresses.contains(source)) {
			this.ingresses.add(source);
		}
	}

	public final Collection<String> getCNAMEs() {
		return this.cnames;
	}

	public Map<String, Collection<Integer>> getDNATs() {
		return this.dnats;
	}

	public HostName getDomain() {
		return this.domain;
	}

	public final Collection<HostName> getEgresses() {
		return this.egresses;
	}

	public final InternetAddress getEmailAddress() {
		return this.emailAddress;
	}

	public final Collection<IPAddress> getExternalIPs() {
		return this.externalIPAddresses;
	}

	public final String getFirewallProfile() {
		return this.firewallProfile;
	}

	public final Collection<String> getForwards() {
		return this.forwards;
	}

	public final Collection<HostName> getIngresses() {
		return this.ingresses;
	}

	public final Map<Encapsulation, Collection<Integer>> getListens() {
		return this.listens;
	}

	public final Map<Direction, Collection<NetworkInterfaceData>> getNetworkInterfaces() {
		return this.networkInterfaces;
	}

	public final Boolean isThrottled() {
		return this.throttled;
	}

	private void putCNAME(String cname) {
		if (this.cnames == null) {
			this.cnames = new HashSet<>();
		}
		if (!this.cnames.contains(cname)) {
			this.cnames.add(cname);
		}
	}

	public void putLANNetworkInterface(NetworkInterfaceData... ifaces) {
		putNetworkInterface(Direction.LAN, ifaces);
	}

	private void putListenPort(Encapsulation encapsulation, Integer... ports) throws InvalidPortException {
		if (this.listens == null) {
			this.listens = new Hashtable<>();
		}

		Collection<Integer> currentPorts = this.listens.get(encapsulation);

		if (currentPorts == null) {
			currentPorts = new HashSet<>();
		}

		for (final Integer port : ports) {
			if (((port < 0)) || ((port > 65535))) {
				throw new InvalidPortException(port);
			}
			if (!currentPorts.contains(port)) {
				currentPorts.add(port);
			}
		}

		this.listens.put(encapsulation, currentPorts);
	}

	protected void putNetworkInterface(Direction dir, NetworkInterfaceData... ifaces) {
		if (this.networkInterfaces == null) {
			this.networkInterfaces = new Hashtable<>();
		}

		Collection<NetworkInterfaceData> currentIfaces = this.networkInterfaces.get(dir);

		if (currentIfaces == null) {
			currentIfaces = new HashSet<>();
		}

		for (final NetworkInterfaceData iface : ifaces) {
			if (!currentIfaces.contains(iface)) {
				currentIfaces.add(iface);
			}
		}

		this.networkInterfaces.put(dir, currentIfaces);

	}

	public void putWANNetworkInterface(NetworkInterfaceData... ifaces) {
		putNetworkInterface(Direction.WAN, ifaces);
	}

	@Override
	protected void read(JsonObject data) throws ADataException, JsonParsingException, IOException, URISyntaxException {
		setData(data);

		if (data.containsKey("email")) {
			try {
				this.emailAddress = new InternetAddress(data.getString("email"));

			} catch (final AddressException e) {
				throw new InvalidEmailAddressException(data.getString("email") + " is an invalid email address");
			}
		}

		// DNS-related stuff
		if (data.containsKey("domain")) {
			setDomain(new HostName(data.getString("domain")));
		}
		if (data.containsKey("cnames")) {
			final JsonArray cnames = data.getJsonArray("cnames");
			for (final JsonValue cname : cnames) {
				putCNAME(((JsonString) cname).getString());
			}
		}

		// Build network interfaces
		if (data.containsKey("network_interfaces")) {
			final JsonObject networkInterfaces = data.getJsonObject("network_interfaces");

			if (networkInterfaces.containsKey("wan")) {
				final JsonArray wanIfaces = networkInterfaces.getJsonArray("wan");
				for (int i = 0; i < wanIfaces.size(); ++i) {
					final NetworkInterfaceData iface = new NetworkInterfaceData(getLabel());

					iface.read(wanIfaces.getJsonObject(i));
					putWANNetworkInterface(iface);
				}
			}

			if (networkInterfaces.containsKey("lan")) {
				final JsonArray lanIfaces = networkInterfaces.getJsonArray("lan");
				for (int i = 0; i < lanIfaces.size(); ++i) {
					final NetworkInterfaceData iface = new NetworkInterfaceData(getLabel());

					iface.read(lanIfaces.getJsonObject(i));
					putLANNetworkInterface(iface);
				}
			}
		} else if (data.containsKey("macs")) {
			final JsonArray macs = data.getJsonArray("macs");
			for (int i = 0; i < macs.size(); ++i) {
				final NetworkInterfaceData iface = new NetworkInterfaceData(getLabel());

				iface.setMAC(new MACAddressString(macs.getString(i)).getAddress());
				putLANNetworkInterface(iface);
			}
		}

		// Firewall...
		if (data.containsKey("firewall")) {
			final JsonObject firewallConf = data.getJsonObject("firewall");

			// Custom profile? You're brave!
			if (firewallConf.containsKey("profile")) {
				this.firewallProfile = data.getString("profile");
			}

			if (data.containsKey("throttle")) {
				this.throttled = data.getBoolean("throttle");
			}

			if (data.containsKey("listen")) {
				final JsonObject listens = data.getJsonObject("listen");

				if (listens.containsKey("tcp")) {
					final JsonArray tcp = listens.getJsonArray("tcp");

					for (final JsonValue port : tcp) {
						putListenPort(Encapsulation.TCP, Integer.parseInt(port.toString()));
					}
				}
				if (listens.containsKey("udp")) {
					final JsonArray udp = listens.getJsonArray("udp");

					for (final JsonValue port : udp) {
						putListenPort(Encapsulation.UDP, Integer.parseInt(port.toString()));
					}
				}
			}
			if (data.containsKey("allow_forward_to")) {
				final JsonArray forwards = data.getJsonArray("allow_forward_to");

				for (final JsonValue forward : forwards) {
					addFoward(((JsonString) forward).getString());
				}
			}
			if (data.containsKey("allow_ingress_from")) {
				final JsonArray sources = data.getJsonArray("allow_ingress_from");

				for (final JsonValue source : sources) {
					addIngress(new HostName(((JsonString) source).getString()));
				}
			}
			if (data.containsKey("allow_egress_to")) {
				final JsonArray destinations = data.getJsonArray("allow_egress_to");

				for (final JsonValue destination : destinations) {
					addEgress(new HostName(((JsonString) destination).getString()));
				}
			}
			if (data.containsKey("dnat_to")) {
				final JsonArray destinations = data.getJsonArray("dnat_to");

				for (final JsonValue destination : destinations) {
					addDNAT(((JsonString) destination).getString());
				}
			}
			// External IP address?
			if (data.containsKey("external_ip")) {
				try {
					this.externalIPAddresses.add(new IPAddressString(data.getString("external_ip")).toAddress());

				} catch (final AddressStringException e) {
					throw new InvalidIPAddressException(data.getString("external_ip") + " on machine " + getLabel() + " is not a valid IP Address");
				}
			}
		}
	}

	private void setDomain(HostName domain) {
		this.domain = domain;
	}
}
