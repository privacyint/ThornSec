/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.stack;

import java.util.ArrayList;
import java.util.Collection;
import core.exception.AThornSecException;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.InvalidMachineModelException;
import core.iface.IUnit;
import core.model.machine.ServerModel;
import core.profile.AStructuredProfile;

/**
 * This is a premade "LEMP" server i.e. MariaDB & PHP-FPM & Nginx.
 */
public class LEMP extends AStructuredProfile {

	private final Nginx webserver;
	private final PHP php;
	private final MariaDB db;

	public LEMP(ServerModel me) {
		super(me);

		this.webserver = new Nginx(me);
		this.php = new PHP(me);
		this.db = new MariaDB(me);
	}

	@Override
	public final Collection<IUnit> getInstalled() throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(getWebserver().getInstalled());
		units.addAll(getPHP().getInstalled());
		units.addAll(getDB().getInstalled());

		return units;
	}

	@Override
	public final Collection<IUnit> getPersistentConfig() throws InvalidServerException, InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(getWebserver().getPersistentConfig());
		units.addAll(getDB().getPersistentConfig());
		units.addAll(getPHP().getPersistentConfig());

		return units;
	}

	@Override
	public final Collection<IUnit> getLiveConfig() throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(getWebserver().getLiveConfig());
		units.addAll(getDB().getLiveConfig());
		units.addAll(getPHP().getLiveConfig());

		return units;
	}

	@Override
	public final Collection<IUnit> getPersistentFirewall() throws AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(getWebserver().getPersistentFirewall());
		units.addAll(getDB().getPersistentFirewall());
		units.addAll(getPHP().getPersistentFirewall());

		return units;
	}

	@Override
	public final Collection<IUnit> getLiveFirewall() throws AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(getWebserver().getLiveFirewall());
		units.addAll(getDB().getLiveFirewall());
		units.addAll(getPHP().getLiveFirewall());

		return units;
	}

	public final Nginx getWebserver() {
		return this.webserver;
	}

	public final MariaDB getDB() {
		return this.db;
	}

	public final PHP getPHP() {
		return this.php;
	}
}
