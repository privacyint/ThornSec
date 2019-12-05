/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.service.machine;

import java.util.ArrayList;
import java.util.Collection;

import core.data.machine.AMachineData.Encapsulation;
import core.exception.data.InvalidPortException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.network.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.pkg.InstalledUnit;

/**
 * This is a profile for https://webmin.com
 */
public class Webmin extends AStructuredProfile {

	public Webmin(String label, NetworkModel networkModel) {
		super(label, networkModel);
	}

	@Override
	protected Collection<IUnit> getPersistentConfig() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		getNetworkModel().getServerModel(getLabel()).getAptSourcesModel().addAptSource("webmin", "deb http://download.webmin.com/download/repository sarge contrib",
				"keyserver.ubuntu.com", "0xD97A3AE911F63C51");

		return units;
	}

	@Override
	protected Collection<IUnit> getInstalled() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("webmin", "proceed", "webmin"));

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws InvalidServerModelException, InvalidPortException {
		final Collection<IUnit> units = new ArrayList<>();

		getNetworkModel().getServerModel(getLabel()).addEgress("download.webmin.com");
		getNetworkModel().getServerModel(getLabel()).addListen(Encapsulation.TCP, 10000);

		return units;
	}
}
