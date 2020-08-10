/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine.configuration.networking;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import core.data.machine.configuration.NetworkInterfaceData;
import core.data.machine.configuration.NetworkInterfaceData.Direction;
import core.data.machine.configuration.NetworkInterfaceData.Inet;
import core.exception.AThornSecException;
import core.exception.data.InvalidIPAddressException;
import core.model.AModel;
import core.model.network.NetworkModel;
import core.unit.fs.FileUnit;
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
public abstract class NetworkInterfaceModel extends AModel implements ISystemdNetworkd {

	private String comment;
	private String iface;
	private Inet inet;
	private Direction direction;
	private Integer weighting;

	private MACAddress mac;

	private Collection<IPAddress> addresses;

	private IPAddress subnet;
	private IPAddress netmask;
	private IPAddress broadcast;
	private IPAddress gateway;

	private Boolean arp;
	private Boolean ipForwarding;
	private Boolean ipMasquerading;
	private Boolean reqdForOnline;
	private Boolean configureWithoutCarrier;

	/**
	 * Creates a new NetworkInterfaceModel with the given iface name.
	 *
	 * Don't invoke me directly.
	 */
	protected NetworkInterfaceModel(NetworkInterfaceData ifaceData, NetworkModel networkModel) {
		super(ifaceData, networkModel);

		this.iface = getLabel();

		this.addresses = null;
		this.inet = null;
		this.direction = null;
		this.weighting = null;
		this.subnet = null;
		this.netmask = null;
		this.broadcast = null;
		this.gateway = null;
		this.mac = null;
		this.comment = null;

		this.arp = null;
		this.ipForwarding = null;
		this.ipMasquerading = null;
		this.reqdForOnline = null;
		this.configureWithoutCarrier = null;
	}
	
	@Override
	public NetworkInterfaceData getData() {
		return (NetworkInterfaceData) super.getData();
	}

	@Override
	public void init() throws AThornSecException {
		this.setDirection(getData().getDirection());
		this.setIface(getData().getIface());
		this.setInet(getData().getInet());
		
		if (getData().getAddresses().isPresent()) {
			this.addAddress(getData().getAddresses().get().toArray(IPAddress[]::new));
		}
		
		getData().getBroadcast().ifPresent((broadcast) -> {
			this.setBroadcast(broadcast);
		});
		getData().getComment().ifPresent((comment) -> {
			this.setComment(comment);
		});
		getData().getGateway().ifPresent((gateway) -> {
			this.setGateway(gateway);
		});
		getData().getMAC().ifPresent((mac) -> {
			this.setMac(mac);
		});
		getData().getNetmask().ifPresent((netmask) -> {
			this.setNetmask(netmask);
		});
		getData().getSubnet().ifPresent((subnet) -> {
			this.setSubnet(subnet);
		});
	}

	public Integer getWeighting() {
		//assertNotNull(this.weighting);
		
		return this.weighting;
	}

	public void setWeighting(Integer weighting) {
		//assertNotNull(weighting);
		
		this.weighting = weighting;
	}

	@Override
	public final Optional<Collection<IPAddress>> getAddresses() {
		return Optional.ofNullable(this.addresses);
	}

	public final Optional<IPAddress> getBroadcast() {
		return Optional.ofNullable(this.broadcast);
	}

	protected final Optional<String> getComment() {
		return Optional.ofNullable(this.comment);
	}

	@Override
	public final Optional<IPAddress> getGateway() {
		return Optional.ofNullable(this.gateway);
	}

	/**
	 * Get the interface's name
	 *
	 * @return interface's name
	 */
	public final String getIface() {
		//assertNotNull(this.iface);
		
		return this.iface;
	}

	protected final Optional<Boolean> getARP() {
		return Optional.ofNullable(this.arp);
	}

	protected final Optional<Boolean> getIsIPForwarding() {
		return Optional.ofNullable(this.ipForwarding);
	}

	protected final Optional<Boolean> getIsIPMasquerading() {
		return Optional.ofNullable(this.ipMasquerading);
	}

	@Override
	public final Optional<MACAddress> getMac() {
		return Optional.ofNullable(this.mac);
	}

	public final IPAddress getNetmask() {
		//assertNotNull(this.netmask);
		
		return this.netmask;
	}

	public final IPAddress getSubnet() {
		//assertNotNull(this.subnet);
		
		return this.subnet;
	}

	public Inet getInet() {
		//assertNotNull(this.inet);
		
		return this.inet;
	}

	/**
	 * Add (an) IP Address(es) to this Network Interface
	 * 
	 * @param addresses The address(es) to add
	 * @throws InvalidIPAddressException if attempting to set a null IP address
	 */
	public final void addAddress(IPAddress... addresses) throws InvalidIPAddressException {
		if (this.addresses == null) {
			this.addresses = new ArrayList<>();
		}
		
		for (final IPAddress address : addresses) {
			if (address == null) {
				throw new InvalidIPAddressException("One of your IP addresses "
						+ "for NIC " + getIface() + " is null");
			}

			this.addresses.add(address);
		}
	}

	public final void setBroadcast(IPAddress broadcast) {
		//assertNotNull(broadcast);
		
		this.broadcast = broadcast;
	}

	protected final void setComment(String comment) {
		//assertNotNull(comment);
		
		this.comment = comment;
	}

	public final void setGateway(IPAddress gateway) {
		//assertNotNull(gateway);
		
		this.gateway = gateway;
	}

	public void setIface(String iface) {
		//assertNotNull(iface);
		
		this.iface = iface;
	}

	public final void setARP(Boolean value) {
		//assertNotNull(value);
		
		this.arp = value;
	}

	public final void setIsIPForwarding(Boolean value) {
		//assertNotNull(value);
		
		this.ipForwarding = value;
	}

	public final void setIsIPMasquerading(Boolean ipMasquerading) {
		//assertNotNull(ipMasquerading);
		
		this.ipMasquerading = ipMasquerading;
	}

	public final void setMac(MACAddress mac) {
		//assertNotNull(mac);
		
		this.mac = mac;
	}

	public final void setInet(Inet inet) {
		//assertNotNull(inet);
		
		this.inet = inet;
	}

	protected final void setNetmask(IPAddress netmask) {
		//assertNotNull(netmask);
		
		this.netmask = netmask;
	}

	public void setSubnet(IPAddress subnet) {
		//assertNotNull(subnet);
		
		this.subnet = subnet;
	}

	private void setDirection(Direction direction) {
		//assertNotNull(direction);
		
		this.direction = direction;
	}
	
	public Direction getDirection() {
		//assertNotNull(this.direction);
		
		return this.direction;
	}
	
	/**
	 * Get our Network File, as described in 
	 * {@link https://www.freedesktop.org/software/systemd/man/systemd.network.html}
	 */
	public Optional<FileUnit> getNetworkFile() {
		final FileUnit network = new FileUnit(getIface() + "_network", "proceed",
				"/etc/systemd/network/" + getWeighting() + "-" + getIface() + ".network");

		network.appendLine("[Match]");
		network.appendLine("Name=" + getIface());
		network.appendCarriageReturn();

		network.appendLine("[Link]");
		getReqdForOnline().ifPresent((required) -> {
			network.appendLine("RequiredForOnline=" + required);
		});
		getARP().ifPresent((arp) -> {
			network.appendLine("ARP=" + arp);
		});
		network.appendCarriageReturn();

		network.appendLine("[Network]");
		getIsIPForwarding().ifPresent((forward) -> {
			network.appendLine("IPForward=" + forward);
		});

		return Optional.of(network);
	}
	
	public Optional<FileUnit> getNetDevFile() {
		final FileUnit netdev = new FileUnit(getIface() + "_netdev", "proceed",
				"/etc/systemd/network/" + getWeighting() + "-" + getIface() + ".netdev");

		netdev.appendLine("[NetDev]");
		netdev.appendLine("Name=" + getIface());
		netdev.appendLine("Kind=" + getInet().toString().toLowerCase());
		netdev.appendCarriageReturn();
		
		return Optional.of(netdev);
	}

	public Optional<Boolean> getReqdForOnline() {
		return Optional.ofNullable(this.reqdForOnline);
	}

	public void setReqdForOnline(Boolean reqdForOnline) {
		//assertNotNull(reqdForOnline);
		
		this.reqdForOnline = reqdForOnline;
	}

	public Optional<Boolean> getConfigureWithoutCarrier() {
		return Optional.ofNullable(configureWithoutCarrier);
	}

	public void setConfigureWithoutCarrier(Boolean configureWithoutCarrier) {
		//assertNotNull(configureWithoutCarrier);
		
		this.configureWithoutCarrier = configureWithoutCarrier;
	}
}
