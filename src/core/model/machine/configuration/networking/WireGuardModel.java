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
import core.exception.data.InvalidIPAddressException;
import core.unit.fs.FileUnit;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.IncompatibleAddressException;

/**
 * This model represents a MACVLAN. You have to stack this on top of a Trunk for
 * it to work, of course.
 */
public class WireGuardModel extends NetworkInterfaceModel {

	Collection<String> peerKeys;
	String psk;
	Integer listenPort;

	public WireGuardModel(String name, String psk, Integer listenPort) {
		super(name);
		super.setInet(Inet.STATIC);
		this.peerKeys = null;
		this.psk = psk;
		this.listenPort = listenPort;
	}

	public WireGuardModel(String name, String psk, Integer listenPort, String subnet, String... addresses)
			throws InvalidIPAddressException {
		this(name, psk, listenPort);

		setSubnet(subnet);
		addAddress(addresses);
	}

	public void addAddress(String... addresses) throws InvalidIPAddressException {
		for (final String address : addresses) {
			final IPAddressString string = new IPAddressString(address);

			try {
				addAddress(string.toAddress());
			} catch (AddressStringException | IncompatibleAddressException e) {
				throw new InvalidIPAddressException(address);
			}
		}
	}

	@Override
	public FileUnit getNetworkFile() {
		final FileUnit network = new FileUnit(getIface() + "_network", "proceed",
				"/etc/systemd/network/20-" + getIface() + ".network");
		network.appendLine("[Match]");
		network.appendLine("Name=" + getIface());
		network.appendCarriageReturn();

		network.appendLine("[Network]");
		// There should only ever be one IP address here
		assert (super.getAddresses().size() == 1);
		for (final IPAddress address : super.getAddresses()) {
			network.appendLine("Address=" + address.getLowerNonZeroHost());
		}
		network.appendCarriageReturn();

		network.appendLine("[Route]");
		network.appendLine("GatewayOnLink=yes");
		network.appendCarriageReturn();

		network.appendLine("[RoutingPolicyRule]");
		network.appendLine("From=" + super.getSubnet());
		network.appendLine("To=" + super.getSubnet());

		return network;
	}

	@Override
	public FileUnit getNetDevFile() {
		final FileUnit netdev = new FileUnit(getIface() + "_netdev", "proceed",
				"/etc/systemd/network/20-" + getIface() + ".netdev");

		netdev.appendLine("[NetDev]");
		netdev.appendLine("Name=" + getIface());
		netdev.appendLine("Kind=wireguard");
		netdev.appendCarriageReturn();

		netdev.appendLine("[WireGuard]");
		netdev.appendLine("PrivateKey=/etc/wireguard/private.key");
		netdev.appendLine("ListenPort=" + this.listenPort);

		if (this.peerKeys != null) {
			this.peerKeys.forEach(peerKey -> {
				netdev.appendCarriageReturn();
				netdev.appendLine("[WireGuardPeer]");
				netdev.appendLine("PublicKey=" + peerKey);
				netdev.appendLine("PresharedKey=" + this.psk);
				netdev.appendLine("AllowedIPs=0.0.0.0/0");
			});
		}
		return netdev;
	}

	public void addPeer(String pubKey) {
		if (this.peerKeys == null) {
			this.peerKeys = new ArrayList<>();
		}

		this.peerKeys.add(pubKey);
	}

	@Override
	public void setSubnet(IPAddress subnet) {
		super.setSubnet(subnet);
	}

	public void setSubnet(String subnet) throws InvalidIPAddressException {
		final IPAddressString string = new IPAddressString(subnet);

		try {
			setSubnet(string.toAddress());
		} catch (AddressStringException | IncompatibleAddressException e) {
			throw new InvalidIPAddressException(subnet);
		}
	}
}
