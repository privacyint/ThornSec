/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.core.model.machine;

import java.util.Collection;

import org.privacyinternational.thornsec.core.data.machine.InternalDeviceData;
import org.privacyinternational.thornsec.core.exception.AThornSecException;
import org.privacyinternational.thornsec.core.exception.data.InvalidPortException;
import org.privacyinternational.thornsec.core.iface.IUnit;
import org.privacyinternational.thornsec.core.model.network.NetworkModel;

/**
 * This model represents an "Internal-Only" device on our network.
 *
 * An internal-only device is one which can only be spoken to on our intranet,
 * with no access to the wider net.
 */
public class InternalOnlyDeviceModel extends ADeviceModel {
	public InternalOnlyDeviceModel(InternalDeviceData internalDeviceData, NetworkModel networkModel) throws AThornSecException {
		super(internalDeviceData, networkModel);
	}

	@Override
	protected Collection<IUnit> getPersistentFirewall() throws InvalidPortException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init() throws AThornSecException {
		// TODO Auto-generated method stub
		
	}
}
