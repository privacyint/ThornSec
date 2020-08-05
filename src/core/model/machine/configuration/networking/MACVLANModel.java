/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine.configuration.networking;

import java.util.Optional;
import core.data.machine.AMachineData.MachineType;
import core.data.machine.configuration.NetworkInterfaceData;
import core.data.machine.configuration.NetworkInterfaceData.Inet;
import core.model.network.NetworkModel;
import core.unit.fs.FileUnit;

/**
 * This model represents a MACVLAN. You have to stack this on top of a Trunk for
 * it to work, of course.
 */
public class MACVLANModel extends NetworkInterfaceModel {
	private MachineType type;
	
	public MACVLANModel(NetworkInterfaceData myData, NetworkModel networkModel) {
		super(myData, networkModel);
		
		super.setInet(Inet.MACVLAN);
		super.setWeighting(20);
		super.setReqdForOnline(true);
	}

	public MACVLANModel() {
		this(new NetworkInterfaceData("MACVLAN"), null);
	}
	
	public void setType(MachineType type) {
		this.type = type;
	}
	
	public MachineType getType() {
		return this.type;
	}

	@Override
	public Optional<FileUnit> getNetDevFile() {
		FileUnit netDev = super.getNetDevFile().get();
		
		netDev.appendCarriageReturn();
		netDev.appendLine("[MACVLAN]");
		netDev.appendLine("Mode=bridge");

		return Optional.of(netDev);
	}
}
