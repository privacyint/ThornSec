/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine;

import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import core.exception.AThornSecException;
import core.exception.data.machine.InvalidMachineException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.network.NetworkModel;

public class ServiceModel extends ServerModel {

	ServiceModel(String label, NetworkModel networkModel) throws InvalidServerModelException, InvalidMachineException,
			InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException, ClassNotFoundException, URISyntaxException {
		super(label, networkModel);
	}

	@Override
	public Set<IUnit> getUnits() throws AThornSecException {
		final Set<IUnit> units = new HashSet<>();

		units.addAll(super.getUnits());

		return units;
	}
}
