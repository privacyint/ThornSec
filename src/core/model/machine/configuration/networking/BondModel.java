/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine.configuration.networking;

import java.util.Optional;
import core.data.machine.configuration.NetworkInterfaceData;
import core.data.machine.configuration.NetworkInterfaceData.Inet;
import core.model.network.NetworkModel;
import core.unit.fs.FileUnit;

/**
 * This model represents a Bond. You have to stack this on top of a Trunk for it
 * to work, of course.
 */
public class BondModel extends NetworkInterfaceModel {
	public BondModel(NetworkInterfaceData myData, NetworkModel networkModel) {
		super(myData, networkModel);
		
		super.setInet(Inet.BOND);
		super.setWeighting(0);
	}

	public BondModel() {
		this(null, null);
	}

	@Override
	public Optional<FileUnit> getNetworkFile() {
		return Optional.empty(); // Don't need a Network for a Bond
	}

	@Override
	public Optional<FileUnit> getNetDevFile() {
		FileUnit netdev = super.getNetDevFile().get();

		netdev.appendCarriageReturn();
		netdev.appendLine("[Bond]");
		netdev.appendLine("Mode=802.3ad");

		return Optional.of(netdev);
	}
}
