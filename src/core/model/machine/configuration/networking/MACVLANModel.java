/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine.configuration.networking;

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
public class MACVLANModel extends NetworkInterfaceModel {
	public MACVLANModel(String name, MACVLANTrunkModel trunk) {
		super(name);
		trunk.addVLAN(this);
		super.setInet(Inet.STATIC);
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
		final FileUnit network = new FileUnit(getIface() + "_network", "proceed", "/etc/systemd/network/20-" + getIface() + ".network");
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
		final FileUnit netdev = new FileUnit(getIface() + "_netdev", "proceed", "/etc/systemd/network/20-" + getIface() + ".netdev");

		netdev.appendLine("[NetDev]");
		netdev.appendLine("Name=" + getIface());
		netdev.appendLine("Kind=macvlan");
		netdev.appendCarriageReturn();

		netdev.appendLine("[MACVLAN]");
		netdev.appendLine("Mode=bridge");

		return netdev;
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
