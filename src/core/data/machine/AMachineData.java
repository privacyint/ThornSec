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
import java.util.Set;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.stream.JsonParsingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import core.data.AData;
import core.data.machine.configuration.NetworkInterfaceData;
import core.exception.data.ADataException;
import core.exception.data.machine.InvalidEmailAddressException;
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
	// Networking
	public enum Encapsulation { UDP, TCP }

	private Set<NetworkInterfaceData> lanInterfaces;
	private Set<NetworkInterfaceData> wanInterfaces;

	private IPAddress externalIPAddress;

	// DNS
	private HostName fqdn;
	private Set<HostName> cnames;

	// Alerting
	private InternetAddress emailAddress;

	// Firewall
	private String firewallProfile;
	private Boolean throttled;

	private Hashtable<Encapsulation, Set<HostName>> listens;

	private Set<HostName> forwards;
	private Set<HostName> ingresses;
	private Set<HostName> egresses;

	protected AMachineData(String label) {
		super(label);

		this.lanInterfaces = null;
		this.wanInterfaces = null;

		this.externalIPAddress = null;

		this.fqdn = null;
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
		this.setData(data);

		// These are all arrays, we need to do some further processing first!
		final JsonArray lanIfaces = super.getPropertyObjectArray("lan");
		final JsonArray wanIfaces = super.getPropertyObjectArray("wan");
		final Set<String> cnames = getPropertyArray("cnames");
		final Set<String> listensTCP = getPropertyArray("listentcp");
		final Set<String> listensUDP = getPropertyArray("listenudp");
		final Set<String> forwards = getPropertyArray("forward");
		final Set<String> ingresses = getPropertyArray("ingress");
		final Set<String> egresses = getPropertyArray("egress");

		// Let's set some fields
		this.externalIPAddress = new IPAddressString(getStringProperty("externalip", null)).getAddress();
		this.fqdn = new HostName(getStringProperty("fqdn", null));
		this.firewallProfile = getStringProperty("firewall", null);
		this.throttled = getBooleanProperty("throttle");

		// But only set these fields !null if we actually have anything to put in them
		if (lanIfaces != null) {
			this.lanInterfaces = new HashSet<>();

			for (int i = 0; i < lanIfaces.size(); ++i) {
				final NetworkInterfaceData iface = new NetworkInterfaceData();
				iface.setHost(this.getFQDN());
				iface.read(lanIfaces.getJsonObject(i));
				this.lanInterfaces.add(iface);
			}
		}
		if (wanIfaces != null) {
			this.wanInterfaces = new HashSet<>();

			for (int i = 0; i < wanIfaces.size(); ++i) {
				final NetworkInterfaceData iface = new NetworkInterfaceData();
				iface.setHost(this.getFQDN());
				iface.read(wanIfaces.getJsonObject(i));
				this.wanInterfaces.add(iface);
			}
		}
		if (cnames != null) {
			this.cnames = new HashSet<>();

			for (final String cname : cnames) {
				this.cnames.add(new HostName(cname));
			}
		}
		if ((listensTCP != null) || (listensUDP != null)) {
			this.listens = new Hashtable<>();

			if (listensTCP != null) {
				this.listens.put(Encapsulation.TCP, setStringToSetHostName(listensTCP));
			}
			if (listensUDP != null) {
				this.listens.put(Encapsulation.UDP, setStringToSetHostName(listensUDP));
			}
		}
		if (forwards != null) {
			this.forwards = setStringToSetHostName(forwards);

		}
		if (ingresses != null) {
			this.ingresses = setStringToSetHostName(ingresses);
		}
		if (egresses != null) {
			this.egresses = setStringToSetHostName(egresses);
		}

		try {
			this.emailAddress = new InternetAddress(getStringProperty("email", null));
		} catch (final AddressException e) {
			throw new InvalidEmailAddressException();
		}
	}

	final private Set<HostName> setStringToSetHostName(Set<String> hostnamesStrings) {
		final Set<HostName> hostnames = new HashSet<>();

		for (final String hostnameString : hostnamesStrings) {
			hostnames.add(new HostName(hostnameString));
		}

		return hostnames;
	}

	public final HostName getFQDN() {
		return this.fqdn;
	}

	public final Set<NetworkInterfaceData> getLanInterfaces() {
		return this.lanInterfaces;
	}

	public final Set<NetworkInterfaceData> getWanInterfaces() {
		return this.wanInterfaces;
	}

	public final IPAddress getExternalIP() {
		return this.externalIPAddress;
	}

	public final Hashtable<Encapsulation, Set<HostName>> getListens() {
		return this.listens;
	}

	public final Set<HostName> getForwards() {
		return this.forwards;
	}

	public final Set<HostName> getIngresses() {
		return this.ingresses;
	}

	public final Set<HostName> getEgresses() {
		return this.egresses;
	}

	public final Boolean getIsThrottled() {
		return this.throttled;
	}

	public final Set<HostName> getCnames() {
		return this.cnames;
	}

	public final InternetAddress getEmailAddress() {
		return this.emailAddress;
	}

	public final IPAddress getExternalIp() {
		return this.externalIPAddress;
	}

	public final String getFirewallProfile() {
		return this.firewallProfile;
	}

}
