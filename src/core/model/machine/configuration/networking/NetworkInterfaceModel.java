/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine.configuration.networking;

import java.util.ArrayList;
import java.util.Collection;

import core.data.machine.configuration.NetworkInterfaceData.Inet;
import inet.ipaddr.IPAddress;
import inet.ipaddr.mac.MACAddress;

/**
 * This model represents a Network Interface Card (NIC) attached to our network.
 *
 * Whilst originally based on the traditional "SysVinit" /etc/network/interfaces
 * file, with Debian's continued march towards its successor Systemd, we have
 * migrated to utilise systemd-networkd.
 *
 * This provides portability to other GNU/Linux distributions, but was not a
 * decision which was taken lightly.
 *
 * For more information, see https://wiki.debian.org/Debate/initsystem/sysvinit
 */
public abstract class NetworkInterfaceModel implements ISystemdNetworkd {
	private final String label;

	private String comment;
	private String iface;
	private Inet inet;

	private MACAddress mac;

	private Collection<IPAddress> addresses;

	private IPAddress subnet;
	private IPAddress netmask;
	private IPAddress broadcast;
	private IPAddress gateway;

	private Boolean arp;
	private Boolean ipForwarding;
	private Boolean ipMasquerading;

	/**
	 * Creates a new NetworkInterfaceModel with the given iface name.
	 *
	 * Don't invoke me directly.
	 */
	protected NetworkInterfaceModel(String label) {
		this.addresses = null;

		this.label = label;
		this.iface = label;
		this.inet = null;
		this.subnet = null;
		this.netmask = null;
		this.broadcast = null;
		this.gateway = null;
		this.mac = null;
		this.comment = null;

		this.arp = null;
		this.ipForwarding = null;
		this.ipMasquerading = null;
	}

	@Override
	public final Collection<IPAddress> getAddresses() {
		return this.addresses;
	}

	public final IPAddress getBroadcast() {
		return this.broadcast;
	}

	protected final String getComment() {
		return this.comment;
	}

	@Override
	public final IPAddress getGateway() {
		return this.gateway;
	}

	/**
	 * Get the interface's name
	 *
	 * @return interface's name
	 */
	public final String getIface() {
		return this.iface;
	}

	protected final Boolean getARP() {
		return this.arp;
	}

	protected final Boolean getIsIPForwarding() {
		return this.ipForwarding;
	}

	protected final Boolean getIsIPMasquerading() {
		return this.ipMasquerading;
	}

	public final MACAddress getMac() {
		return this.mac;
	}

	public final IPAddress getNetmask() {
		return this.netmask;
	}

	public final IPAddress getSubnet() {
		return this.subnet;
	}

	public Inet getInet() {
		return this.inet;
	}

	public final void addAddress(IPAddress address) {
		// Don't add null addresses
		if (address == null) {
			return;
		}

		if (this.addresses == null) {
			this.addresses = new ArrayList<>();
		}
		this.addresses.add(address);
	}

	public final void setBroadcast(IPAddress broadcast) {
		this.broadcast = broadcast;
	}

	protected final void setComment(String comment) {
		this.comment = comment;
	}

	public final void setGateway(IPAddress gateway) {
		this.gateway = gateway;
	}

	public void setIface(String iface) {
		this.iface = iface;
	}

	public final void setARP(Boolean value) {
		this.arp = value;
	}

	public final void setIsIPForwarding(Boolean value) {
		this.ipForwarding = value;
	}

	public final void setIsIPMasquerading(Boolean value) {
		this.ipMasquerading = value;
	}

	public final void setMac(MACAddress mac) {
		this.mac = mac;
	}

	public final void setInet(Inet inet) {
		this.inet = inet;
	}

	protected final void setNetmask(IPAddress netmask) {
		this.netmask = netmask;
	}

	protected void setSubnet(IPAddress subnet) {
		this.subnet = subnet;
	}

	final public String getLabel() {
		return this.label;
	}
}
