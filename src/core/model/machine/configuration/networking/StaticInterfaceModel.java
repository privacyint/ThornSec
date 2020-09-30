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
 * This model represents a statically assigned network interface
 */
public class StaticInterfaceModel extends NetworkInterfaceModel {
	public StaticInterfaceModel(NetworkInterfaceData myData, NetworkModel networkModel) {
		super(myData, networkModel);
		
		super.setInet(Inet.STATIC);
		super.setWeighting(0);
		super.setReqdForOnline(true);
	}

	@Override
	public Optional<FileUnit> getNetDevFile() {
		return Optional.empty();
	}
}
