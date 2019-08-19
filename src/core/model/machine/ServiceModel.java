/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import javax.json.stream.JsonParsingException;
import javax.mail.internet.AddressException;

import core.exception.AThornSecException;
import core.iface.IUnit;
import core.model.network.NetworkModel;

/**
 * This model represents a Service on our network.
 *
 * A service is a machine which is run on a HyperVisor
 */
public class ServiceModel extends ServerModel {
	public ServiceModel(String label, NetworkModel networkModel)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException, ClassNotFoundException, URISyntaxException, AddressException,
			IOException, JsonParsingException, AThornSecException {
		super(label, networkModel);
	}

	@Override
	public Set<IUnit> getUnits() throws AThornSecException {
		final Set<IUnit> units = new HashSet<>();

		units.addAll(super.getUnits());

		return units;
	}
}
