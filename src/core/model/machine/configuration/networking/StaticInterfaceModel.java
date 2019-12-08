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
import inet.ipaddr.IPAddress;

/**
 * This model represents a statically assigned network interface
 */
public class StaticInterfaceModel extends NetworkInterfaceModel {
	public StaticInterfaceModel(String name) {
		super(name);
		super.setInet(Inet.STATIC);
	}

	@Override
	public FileUnit getNetworkFile() {
		final FileUnit network = new FileUnit(getIface() + "_network", "proceed", "/etc/systemd/network/00-" + getIface() + ".network");
		network.appendLine("[Match]");
		network.appendLine("Name=" + getIface());
		network.appendCarriageReturn();

		network.appendLine("[Network]");
		if (super.getAddresses() != null) {
			// Add all of this interface's IP addresses
			for (final IPAddress address : super.getAddresses()) {
				network.appendLine("Address=" + address);
			}
		}

		if (super.getGateway() != null) {
			network.appendLine("Gateway=" + super.getGateway());
		}

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
		return null;
	}
}
