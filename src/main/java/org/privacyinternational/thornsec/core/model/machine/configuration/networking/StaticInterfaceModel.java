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
 * This model represents a statically assigned network interface
 */
public class StaticInterfaceModel extends NetworkInterfaceModel {
	public StaticInterfaceModel(NetworkInterfaceData myData, NetworkModel networkModel) throws InvalidNetworkInterfaceException {
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
