/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.stack;

import java.util.HashSet;
import java.util.Set;

import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.network.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.pkg.InstalledUnit;

/**
 * This profile installs and configures msmtp (https://marlam.de/msmtp/).
 *
 * This allows servers to send authenticated emails (and otherwise)
 */
public class MSMTP extends AStructuredProfile {

	public MSMTP(String label, NetworkModel networkModel) {
		super(label, networkModel);
	}

	@Override
	public Set<IUnit> getInstalled() {
		final Set<IUnit> units = new HashSet<>();

		units.add(new InstalledUnit("msmtp_mta", "proceed", "msmtp-mta"));

		return units;
	}

	@Override
	public Set<IUnit> getPersistentConfig() {
		final Set<IUnit> units = new HashSet<>();

		// TODO: iunno. Is there something which needs to go here?

		return units;
	}

	@Override
	public Set<IUnit> getLiveConfig() {
		final Set<IUnit> units = new HashSet<>();

		// TODO: check it's up to date, etc

		return units;
	}

	@Override
	public Set<IUnit> getPersistentFirewall() throws InvalidServerModelException {
		final Set<IUnit> units = new HashSet<>();

		this.networkModel.getServerModel(getLabel()).addEgress("*:25,465");

		return units;
	}
}
