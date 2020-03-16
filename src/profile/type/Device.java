/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.type;

import java.util.ArrayList;
import java.util.Collection;

import javax.json.stream.JsonParsingException;

import core.exception.data.ADataException;
import core.exception.runtime.InvalidDeviceModelException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.network.NetworkModel;

/**
 * This is a device on our network
 */
public class Device extends AMachineProfile {

	public Device(String label, NetworkModel networkModel)
			throws InvalidServerModelException, JsonParsingException, ADataException, InvalidDeviceModelException {
		super(label, networkModel);
	}

	@Override
	public Collection<IUnit> getPersistentConfig() {
		final Collection<IUnit> units = new ArrayList<>();

		super.buildNICs();

		return units;
	}
}
