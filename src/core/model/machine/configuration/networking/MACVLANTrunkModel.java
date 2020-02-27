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
import core.unit.fs.FileUnit;

/**
 * This model represents a MACVLAN Trunk - i.e. the "physical" interface upon
 * which to stack the VLANs.
 */
public class MACVLANTrunkModel extends NetworkInterfaceModel {
	private Collection<MACVLANModel> vlans;

	public MACVLANTrunkModel(String name) {
		super(name);
		super.setInet(Inet.MACVLAN);

		vlans = null;
	}

	@Override
	public FileUnit getNetworkFile() {
		final FileUnit network = new FileUnit(getIface() + "_network", "proceed", "/etc/systemd/network/10-" + getIface() + ".network");
		network.appendLine("[Match]");
		network.appendLine("Name=" + getIface());
		network.appendCarriageReturn();

		network.appendLine("[Link]");
		network.appendLine("RequiredForOnline=yes");
		network.appendLine("ARP=no");
		network.appendCarriageReturn();

		network.appendLine("[Network]");
		network.appendLine("IPForward=yes");

		for (final MACVLANModel vlan : this.vlans) {
			network.appendLine("MACVLAN=" + vlan.getIface());
		}

		return network;
	}

	@Override
	public FileUnit getNetDevFile() {
		// Don't need a NetDev for a MACVLAN Trunk
		return null;
	}

	public final void addVLAN(MACVLANModel vlan) {
		if (this.vlans == null) {
			this.vlans = new ArrayList<>();
		}

		this.vlans.add(vlan);
	}

	public final Collection<MACVLANModel> getVLANs() {
		return this.vlans;
	}
}
