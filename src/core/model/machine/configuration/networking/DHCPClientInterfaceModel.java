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
 * This model represents an interface which receives its information over DHCP
 */
public class DHCPClientInterfaceModel extends NetworkInterfaceModel {

	public DHCPClientInterfaceModel(NetworkInterfaceData myData, NetworkModel networkModel) {
		super(myData, networkModel);
		
		super.setInet(Inet.DHCP);
		super.setWeighting(10);
	}

	@Override
	public Optional<FileUnit> getNetworkFile() {
		FileUnit network = super.getNetworkFile().get();

		network.appendCarriageReturn();
		network.appendLine("DHCP=true");

		return Optional.of(network);
	}

	@Override
	public Optional<FileUnit> getNetDevFile() {
		return Optional.empty();
	}
}
