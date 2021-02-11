/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.core.model.machine;

import org.privacyinternational.thornsec.core.data.machine.DedicatedData;
import org.privacyinternational.thornsec.core.exception.AThornSecException;
import org.privacyinternational.thornsec.core.model.network.NetworkModel;

/**
 * This model represents a Dedicated Server on our network.
 */
public class DedicatedModel extends ServerModel {

	public DedicatedModel(DedicatedData machineData, NetworkModel networkModel)
			throws AThornSecException {
		super(machineData, networkModel);
	}
}
