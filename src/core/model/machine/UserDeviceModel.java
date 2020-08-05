/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine;

import java.io.IOException;
import java.util.Collection;

import core.data.machine.UserDeviceData;
import core.exception.AThornSecException;

import core.exception.data.ADataException;
import core.exception.runtime.InvalidDeviceModelException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.network.NetworkModel;

/**
 * This model represents a User device on our network.
 *
 * User devices can see "everything" both internal and external - be careful
 * with these devices!
 */
public class UserDeviceModel extends ADeviceModel {
	public UserDeviceModel(UserDeviceData myData, NetworkModel networkModel) throws AThornSecException {
		super(myData, networkModel);
	}
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() {
		return null;
	}
}
