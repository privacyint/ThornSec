/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine;

import core.data.machine.DedicatedData;
import core.exception.AThornSecException;
import core.model.network.NetworkModel;

/**
 * This model represents a Dedicated Server on our network.
 */
public class DedicatedModel extends ServerModel {

	public DedicatedModel(DedicatedData machineData, NetworkModel networkModel)
			throws AThornSecException {
		super(machineData, networkModel);
	}
}
