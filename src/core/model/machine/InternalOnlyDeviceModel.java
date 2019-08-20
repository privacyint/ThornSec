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
import core.iface.IUnit;
import core.model.network.NetworkModel;

/**
 * This model represents an "Internal-Only" device on our network.
 *
 * An internal-only device is one which can only be spoken to on our intranet,
 * with no access to the wider net.
 */
public class InternalOnlyDeviceModel extends ADeviceModel {
	public InternalOnlyDeviceModel(String label, NetworkModel networkModel)
			throws AddressException, JsonParsingException, ADataException, IOException {
		super(label, networkModel);
	}

	@Override
	protected Collection<IUnit> getPersistentFirewall() {
		// TODO Auto-generated method stub
		return null;
	}
}
