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

abstract public class InternalOnlyDeviceModel extends ADeviceModel {

	public InternalOnlyDeviceModel(String label, NetworkModel networkModel)
			throws InvalidMachineException, AddressException {
		super(label, networkModel);
	}
}
