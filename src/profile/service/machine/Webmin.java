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
import core.data.machine.configuration.TrafficRule.Encapsulation;
import core.exception.data.InvalidPortException;
import core.exception.runtime.InvalidMachineModelException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.machine.ServerModel;
import core.profile.AStructuredProfile;
import core.unit.pkg.InstalledUnit;
import inet.ipaddr.HostName;

/**
 * This is a profile for https://webmin.com
 */
public class Webmin extends AStructuredProfile {

	public Webmin(ServerModel me) {
		super(me);
	}

	@Override
	public Collection<IUnit> getPersistentConfig() throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		//getServerModel().getAptSourcesModel().addAptSource("webmin", "deb http://download.webmin.com/download/repository sarge contrib",
		//		"keyserver.ubuntu.com", "D97A3AE911F63C51");

		return units;
	}

	@Override
	public Collection<IUnit> getInstalled() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("webmin", "proceed", "webmin"));

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws InvalidPortException, InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		getMachineModel().addEgress(new HostName("download.webmin.com"));
		getMachineModel().addLANOnlyListen(Encapsulation.TCP, 10000);

		return units;
	}
}
