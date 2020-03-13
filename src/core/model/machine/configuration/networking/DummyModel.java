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
 * This model represents a dummy interface. This is the networking equivalent of
 * /dev/null.
 *
 * Use this interface where you need a trunk but don't have a "real" NIC spare
 */
public class DummyModel extends NetworkInterfaceModel {
	public DummyModel(String name) {
		super(name);
		super.setInet(Inet.DUMMY);
	}

	@Override
	public FileUnit getNetworkFile() {
		// Don't need a Network for a dummy interface, it's a trunk
		return null;
	}

	@Override
	public FileUnit getNetDevFile() {
		final FileUnit netdev = new FileUnit(getIface() + "_netdev", "proceed",
				"/etc/systemd/network/10-" + getIface() + ".netdev");

		netdev.appendLine("[NetDev]");
		netdev.appendLine("Name=" + getIface());
		netdev.appendLine("Kind=" + getInet().getInet());

		return netdev;
	}
}
