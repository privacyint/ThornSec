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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.stream.JsonParsingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import core.data.AData;
import core.data.machine.configuration.NetworkInterfaceData;
import core.exception.data.ADataException;
import core.exception.data.InvalidDestinationException;
import core.exception.data.InvalidPortException;
import core.exception.data.machine.InvalidEmailAddressException;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.IncompatibleAddressException;

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
	// These are the only types of machine I'll recognise until I'm told
	// otherwise...
	public enum MachineType {
		ROUTER("Router"), SERVER("Server"), HYPERVISOR("HyperVisor"), DEDICATED("Dedicated"), SERVICE("Service"),
		DEVICE("Device"), USER("User"), INTERNAL_ONLY("Internal-Only Device"), EXTERNAL_ONLY("External-Only Device");

		private String machineType;

		MachineType(String machineType) {
			this.machineType = machineType;
		}

		@Override
		public String toString() {
			return this.machineType;
		}
	}

	// Networking
	public enum Encapsulation {
		UDP, TCP
	}

	public static Boolean DEFAULT_IS_THROTTLED = true;

	private Set<NetworkInterfaceData> networkInterfaces;

	private Set<IPAddress> externalIPAddresses;

	private Set<String> cnames;
	// Alerting
	private InternetAddress emailAddress;

	// Firewall
	private String firewallProfile;
	private Boolean throttled;

	private Map<Encapsulation, Set<Integer>> listens;

	private Set<String> forwards;
	private Set<HostName> ingresses;
	private Set<HostName> egresses;

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

		this.forwards = null;
		this.ingresses = null;
		this.egresses = null;
	}

	@Override
	protected void read(JsonObject data) throws ADataException, JsonParsingException, IOException, URISyntaxException {
		setData(data);

		// Network Interfaces
		if (data.containsKey("network_interfaces")) {
			final JsonArray wanIfaces = data.getJsonArray("wan");
			for (int i = 0; i < wanIfaces.size(); ++i) {
				final NetworkInterfaceData iface = new NetworkInterfaceData(getLabel());

				iface.read(wanIfaces.getJsonObject(i));
				putNetworkInterface(iface);
			}
		}

		// External IP addresses
		if (data.containsKey("externalips")) {
			final JsonArray ips = data.getJsonArray("externalips");
			for (final JsonValue ip : ips) {
				IPAddress address;
				try {
					address = new IPAddressString(ip.toString()).toAddress();
				} catch (final AddressStringException e) {
					throw new InvalidDestinationException();
				} catch (final IncompatibleAddressException e) {
					throw new InvalidDestinationException();
				}
				putExternalIPAddress(address);
			}
		}

		// DNS-related stuff
		if (data.containsKey("domain")) {
			setDomain(new HostName(data.getString("domain")));
		}
		if (data.containsKey("cnames")) {
			final JsonArray cnames = data.getJsonArray("cnames");
			for (final JsonValue cname : cnames) {
				putCNAME(cname.toString());
			}
		}

		// Firewall...
		this.firewallProfile = data.getString("firewall", null);
		if (data.containsKey("throttled")) {
			this.throttled = data.getBoolean("throttle");
		}
		if (data.containsKey("listen")) {
			final JsonObject listens = data.getJsonObject("listen");

			if (listens.containsKey("tcp")) {
				final JsonArray tcp = listens.getJsonArray("tcp");

				for (int i = 0; i < tcp.size(); ++i) {
					putPort(Encapsulation.TCP, tcp.getInt(i));
				}
			}
			if (listens.containsKey("udp")) {
				final JsonArray udp = listens.getJsonArray("udp");

				for (int i = 0; i < udp.size(); ++i) {
					putPort(Encapsulation.UDP, udp.getInt(i));
				}
			}
		}
		if (data.containsKey("allowforwardto")) {
			final JsonArray forwards = data.getJsonArray("allowforwardto");

			for (final JsonValue forward : forwards) {
				addFoward(forward.toString());
			}
		}
		if (data.containsKey("allowingressfrom")) {
			final JsonArray sources = data.getJsonArray("allowingressfrom");

			for (final JsonValue source : sources) {
				addIngress(new HostName(source.toString()));
			}
		}
		if (data.containsKey("allowegressto")) {
			final JsonArray destinations = data.getJsonArray("allowingressfrom");

			for (final JsonValue destination : destinations) {
				addEgress(new HostName(destination.toString()));
			}
		}
		if (data.containsKey("email")) {
			try {
				this.emailAddress = new InternetAddress(data.getString("email"));

			} catch (final AddressException e) {
				throw new InvalidEmailAddressException();
			}
		}
	}

	private void addEgress(HostName destination) {
		if (this.egresses == null) {
			this.egresses = new HashSet<>();
		}

		this.egresses.add(destination);
	}

	private void addIngress(HostName source) {
		if (this.ingresses == null) {
			this.ingresses = new HashSet<>();
		}

		this.ingresses.add(source);
	}

	private void addFoward(String label) {
		if (this.forwards == null) {
			this.forwards = new HashSet<>();
		}

		this.forwards.add(label);
	}

	private void putPort(Encapsulation encapsulation, Integer... ports) throws InvalidPortException {
		if (this.listens == null) {
			this.listens = new Hashtable<>();
		}

		Set<Integer> currentPorts = this.listens.get(encapsulation);

		if (currentPorts == null) {
			currentPorts = new HashSet<>();
		}

		for (final Integer port : ports) {
			if (((port < 0)) || ((port > 65535))) {
				throw new InvalidPortException();
			}
			currentPorts.add(port);
		}

		this.listens.put(encapsulation, currentPorts);
	}

	private void setDomain(HostName domain) {
		this.domain = domain;
	}

	private void putCNAME(String cname) {
		if (this.cnames == null) {
			this.cnames = new HashSet<>();
		}

		this.cnames.add(cname);
	}

	private void putExternalIPAddress(IPAddress address) {
		if (this.externalIPAddresses == null) {
			this.externalIPAddresses = new LinkedHashSet<>();
		}

		this.externalIPAddresses.add(address);
	}

	private void putNetworkInterface(NetworkInterfaceData iface) {
		if (this.networkInterfaces == null) {
			this.networkInterfaces = new LinkedHashSet<>();
		}

		this.networkInterfaces.add(iface);
	}

	public final Set<NetworkInterfaceData> getNetworkInterfaces() {
		return this.networkInterfaces;
	}

	public final Map<Encapsulation, Set<Integer>> getListens() {
		return this.listens;
	}

	public final Set<String> getForwards() {
		return this.forwards;
	}

	public final Set<HostName> getIngresses() {
		return this.ingresses;
	}

	public final Set<HostName> getEgresses() {
		return this.egresses;
	}

	public final Boolean isThrottled() {
		return this.throttled;
	}

	public final Set<String> getCNAMEs() {
		return this.cnames;
	}

	public final InternetAddress getEmailAddress() {
		return this.emailAddress;
	}

	public final Set<IPAddress> getExternalIPs() {
		return this.externalIPAddresses;
	}

	public final String getFirewallProfile() {
		return this.firewallProfile;
	}

	public HostName getDomain() {
		return this.domain;
	}
}
