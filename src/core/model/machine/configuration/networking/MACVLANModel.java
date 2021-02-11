/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine.configuration.networking;

import core.data.machine.AMachineData.MachineType;
import core.data.machine.configuration.NetworkInterfaceData;
import core.data.machine.configuration.NetworkInterfaceData.Direction;
import core.data.machine.configuration.NetworkInterfaceData.Inet;
import core.exception.data.machine.configuration.InvalidNetworkInterfaceException;
import core.model.network.NetworkModel;

/**
 * This model represents a MACVLAN. You have to stack this on top of a Trunk for
 * it to work, of course.
 */
public class MACVLANModel extends NetworkInterfaceModel {
	private MachineType type;

	public MACVLANModel(NetworkInterfaceData myData, NetworkModel networkModel) throws InvalidNetworkInterfaceException {
		super(myData, networkModel);

		super.setInet(Inet.MACVLAN);
		super.setWeighting(20);
		super.setReqdForOnline(true);
		super.setConfigureWithoutCarrier(true);
		super.setGatewayOnLink(true);
		super.addToNetDev(Section.MACVLAN, "Mode", "bridge");
		super.setDirection(Direction.LAN);
	}

	public MACVLANModel() throws InvalidNetworkInterfaceException {
		this(new NetworkInterfaceData("MACVLAN"), null);
	}

	public void setType(MachineType type) {
		this.type = type;
	}

	public MachineType getType() {
		return this.type;
	}
}
