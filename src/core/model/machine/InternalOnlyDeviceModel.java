package core.model.machine;

import core.exception.data.machine.InvalidDeviceException;
import core.exception.data.machine.InvalidMachineException;
import core.model.network.NetworkModel;

abstract public class InternalOnlyDeviceModel extends ADeviceModel {
	
	public InternalOnlyDeviceModel(String label, NetworkModel networkModel)
	throws InvalidMachineException, InvalidDeviceException {
		super(label, networkModel);
	}
}
