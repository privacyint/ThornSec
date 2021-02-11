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
 * This model represents a Bonded physical interface - i.e. the "physical"
 * interface to add to a trunk.
 */
public class BondInterfaceModel extends NetworkInterfaceModel {
	public BondInterfaceModel(NetworkInterfaceData myData, NetworkModel networkModel) throws InvalidNetworkInterfaceException {
		super(myData, networkModel);

		super.setInet(Inet.MANUAL);
		super.setReqdForOnline(true);
		super.setWeighting(0);
		super.setARP(false);
	}

	public void setBond(BondModel bond) {
		super.addToNetwork(Section.NETWORK, "Bond", bond.getIface());
	}

	@Override
	public Optional<FileUnit> getNetDevFile() {
		return Optional.empty(); // Don't need a NetDev for a Bonded Link
	}
}
