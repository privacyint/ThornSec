/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine;

import javax.mail.internet.AddressException;

import core.exception.data.machine.InvalidMachineException;
import core.model.network.NetworkModel;

abstract public class ADeviceModel extends AMachineModel {

	private Boolean managed;

	public ADeviceModel(String label, NetworkModel networkModel) throws InvalidMachineException, AddressException {
		super(label, networkModel);

		this.managed = networkModel.getData().isManaged(getLabel());
	}

	final public Boolean getIsManaged() throws InvalidMachineException {
		return this.managed;
	}

	final protected void setIsManaged(Boolean managed) {
		this.managed = managed;
	}
}
