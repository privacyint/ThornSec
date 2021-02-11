/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.core.model.machine.configuration.networking;

import java.util.Optional;
import org.privacyinternational.thornsec.core.data.machine.configuration.NetworkInterfaceData;
import org.privacyinternational.thornsec.core.data.machine.configuration.NetworkInterfaceData.Inet;
import org.privacyinternational.thornsec.core.exception.data.machine.configuration.InvalidNetworkInterfaceException;
import org.privacyinternational.thornsec.core.model.network.NetworkModel;
import org.privacyinternational.thornsec.core.unit.fs.FileUnit;

/**
 * This model represents a dummy interface. This is the networking equivalent of
 * /dev/null.
 *
 * Use this interface where you need a trunk but don't have a "real" NIC spare
 */
public class DummyModel extends NetworkInterfaceModel {
	public DummyModel(NetworkInterfaceData myData, NetworkModel networkModel) throws InvalidNetworkInterfaceException {
		super(myData, networkModel);

		super.setInet(Inet.DUMMY);
		super.setWeighting(10);
	}

	public DummyModel(NetworkModel networkModel) throws InvalidNetworkInterfaceException {
		this(new NetworkInterfaceData("dummy"), networkModel);
	}

	@Override
	public Optional<FileUnit> getNetworkFile() {
		return Optional.empty(); // Don't need a Network for a dummy interface
	}
}
