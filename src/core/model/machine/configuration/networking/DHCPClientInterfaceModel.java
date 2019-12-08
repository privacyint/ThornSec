/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine.configuration.networking;

import core.data.machine.configuration.NetworkInterfaceData.Inet;
import core.unit.fs.FileUnit;

/**
 * This model represents an interface which receives its information over DHCP
 */
public class DHCPClientInterfaceModel extends NetworkInterfaceModel {

	public DHCPClientInterfaceModel(String name) {
		super(name);
		super.setInet(Inet.DHCP);
	}

	@Override
	public FileUnit getNetworkFile() {
		final FileUnit network = new FileUnit(getIface() + "_network", "proceed", "/etc/systemd/network/00-" + getIface() + ".network");
		network.appendLine("[Match]");
		network.appendLine("Name=" + getIface());
		network.appendCarriageReturn();

		network.appendLine("[Network]");
		network.appendLine("DHCP=yes");

		if (super.getIsIPForwarding() != null) {
			network.appendLine("IPForward=" + super.getIsIPForwarding());
		}

		if (super.getIsIPMasquerading() != null) {
			network.appendLine("IPMasquerade=" + super.getIsIPMasquerading());
		}

		if (super.getARP() != null) {
			network.appendLine("ARP=" + super.getARP());
		}

		return network;
	}

	@Override
	public FileUnit getNetDevFile() {
		// Don't need a NetDev for a MACVLAN Trunk
		return null;
	}
}
