/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.profile.firewall;

import java.util.ArrayList;
import java.util.Collection;

import org.privacyinternational.thornsec.core.iface.IUnit;
import org.privacyinternational.thornsec.core.model.machine.ServerModel;
import org.privacyinternational.thornsec.core.profile.AStructuredProfile;
import org.privacyinternational.thornsec.core.unit.SimpleUnit;
import org.privacyinternational.thornsec.core.unit.pkg.InstalledUnit;

public class FirewallD extends AStructuredProfile {

	public FirewallD(ServerModel me) {
		super(me);
	}

	@Override
	public Collection<IUnit> getInstalled() {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("firewalld", "proceed", "firewalld"));

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentConfig() {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new SimpleUnit("default_firewall_zone", "firewalld_installed",
				"sudo firewall-cmd --set-default-zone=drop --permanent; sudo firewall-cmd --reload",
				"sudo firewall-cmd --get-default-zone", "drop", "pass"));

		return units;
	}

	@Override
	public Collection<IUnit> getUnits() {
		final Collection<IUnit> units = new ArrayList<>();

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() {
		// TODO Auto-generated method stub
		return null;
	}
}
