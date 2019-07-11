package core.model.machine;

import core.exception.data.machine.InvalidDeviceException;
import core.exception.data.machine.InvalidMachineException;
import core.model.network.NetworkModel;

abstract public class ExternalOnlyDeviceModel extends ADeviceModel {
	
	public ExternalOnlyDeviceModel(String label, NetworkModel networkModel)
	throws InvalidMachineException, InvalidDeviceException {
		super(label, networkModel);
	}
}
