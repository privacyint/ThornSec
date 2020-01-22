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

import javax.json.stream.JsonParsingException;
import javax.mail.internet.AddressException;

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
	public UserDeviceModel(String label, NetworkModel networkModel)
			throws AddressException, JsonParsingException, ADataException, IOException, InvalidServerModelException, InvalidDeviceModelException {
		super(label, networkModel);
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() {
		return null;
	}
}
