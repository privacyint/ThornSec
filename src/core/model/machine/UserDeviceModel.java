package core.model.machine;

import core.exception.data.machine.InvalidDeviceException;
import core.exception.data.machine.InvalidMachineException;
import core.model.network.NetworkModel;

abstract public class UserDeviceModel extends ADeviceModel {
	
	public UserDeviceModel(String label, NetworkModel networkModel)
	throws InvalidMachineException {
		super(label, networkModel);
	}
}
