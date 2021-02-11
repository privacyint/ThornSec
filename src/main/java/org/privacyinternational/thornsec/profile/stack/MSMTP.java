/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.profile.stack;

import java.util.ArrayList;
import java.util.Collection;
import org.privacyinternational.thornsec.core.exception.data.InvalidPortException;
import org.privacyinternational.thornsec.core.exception.runtime.InvalidMachineModelException;
import org.privacyinternational.thornsec.core.iface.IUnit;
import org.privacyinternational.thornsec.core.model.machine.ServerModel;
import org.privacyinternational.thornsec.core.profile.AStructuredProfile;
import org.privacyinternational.thornsec.core.unit.pkg.InstalledUnit;
import inet.ipaddr.HostName;

/**
 * This profile installs and configures msmtp (https://marlam.de/msmtp/).
 *
 * This allows servers to send authenticated emails (and otherwise)
 */
public class MSMTP extends AStructuredProfile {

	public MSMTP(ServerModel me) {
		super(me);
	}

	@Override
	public Collection<IUnit> getInstalled() {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("msmtp_mta", "proceed", "msmtp-mta"));

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws InvalidMachineModelException, InvalidPortException {
		final Collection<IUnit> units = new ArrayList<>();

		getServerModel().addEgress(new HostName("*:25,465"));

		return units;
	}
}
