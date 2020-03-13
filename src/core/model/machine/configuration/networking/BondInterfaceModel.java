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
 * This model represents a Bonded physical interface - i.e. the "physical"
 * interface to add to a trunk.
 */
public class BondInterfaceModel extends NetworkInterfaceModel {
	private final NetworkInterfaceModel trunk;

	public BondInterfaceModel(String name, NetworkInterfaceModel trunk) {
		super(name);

		this.trunk = trunk;
		super.setInet(Inet.MANUAL);
	}

	@Override
	public FileUnit getNetworkFile() {
		final FileUnit network = new FileUnit(getIface() + "_network", "proceed", "/etc/systemd/network/00-" + getIface() + ".network");
		network.appendLine("[Match]");
		network.appendLine("Name=" + getIface());
		network.appendCarriageReturn();

		network.appendLine("[Link]");
		network.appendLine("RequiredForOnline=yes");
		network.appendLine("ARP=no");
		network.appendCarriageReturn();

		network.appendLine("[Network]");
		network.appendLine("Bond=" + getBond());

		return network;
	}

	private String getBond() {
		return this.trunk.getIface();
	}

	@Override
	public FileUnit getNetDevFile() {
		// Don't need a NetDev for a Bonded Link
		return null;
	}
}
