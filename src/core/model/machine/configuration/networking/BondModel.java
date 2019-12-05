/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine.configuration.networking;

import core.unit.fs.FileUnit;

/**
 * This model represents a Bond. You have to stack this on top of a Trunk for it
 * to work, of course.
 */
public class BondModel extends NetworkInterfaceModel {
	public BondModel(String name) {
		super(name);
	}

	@Override
	public FileUnit getNetworkFile() {
		// Don't need a Network for a Bond
		return null;
	}

	@Override
	public FileUnit getNetDevFile() {
		final FileUnit netdev = new FileUnit(getIface() + "_network", "proceed", "/etc/systemd/network/10-" + getIface() + ".netdev");

		netdev.appendLine("[NetDev]");
		netdev.appendLine("Name=" + getIface());
		netdev.appendLine("Kind=bond");
		netdev.appendCarriageReturn();

		netdev.appendLine("[Bond]");
		netdev.appendLine("Mode=802.3ad");

		return netdev;
	}
}
