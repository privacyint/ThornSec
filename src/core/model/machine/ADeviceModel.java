package core.model.machine;

import core.exception.data.machine.InvalidDeviceException;
import core.exception.data.machine.InvalidMachineException;
import core.model.network.NetworkModel;

abstract public class ADeviceModel extends AMachineModel {
	
	private Boolean managed;

	public ADeviceModel(String label, NetworkModel networkModel)
	throws InvalidMachineException, InvalidDeviceException {
		super(label, networkModel);

		this.managed = networkModel.getData().getDeviceIsManaged(getLabel());
	}
	
	final public Boolean getIsManaged()
	throws InvalidMachineException, InvalidDeviceException {
		return this.managed;
	}
	
	final protected void setIsManaged(Boolean managed) {
		this.managed = managed;
	}
}
