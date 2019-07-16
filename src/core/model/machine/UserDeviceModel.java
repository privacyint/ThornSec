/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine;

import java.util.Collection;

import javax.mail.internet.AddressException;

import core.exception.data.machine.InvalidMachineException;
import core.iface.IUnit;
import core.model.network.NetworkModel;

/**
 * This model represents a User device on our network.
 *
 * User devices can see "everything" both internal and external - be careful
 * with these devices!
 */
public class UserDeviceModel extends ADeviceModel {
	public UserDeviceModel(String label, NetworkModel networkModel) throws InvalidMachineException, AddressException {
		super(label, networkModel);
	}

	@Override
	protected Collection<? extends IUnit> getPersistentFirewall() {
		return null;
	}
}
