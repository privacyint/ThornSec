/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.service.email;

import java.util.ArrayList;
import java.util.Collection;

import core.data.machine.AMachineData.Encapsulation;
import core.exception.data.InvalidPortException;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.network.NetworkModel;
import core.profile.AStructuredProfile;
import profile.stack.MariaDB;
import profile.stack.Nginx;
import profile.stack.PHP;

/**
 * This profile is, in effect, a stub for now.
 *
 * It handles firewall stuff, but other than that, you're on your own.
 */
public class EmailServer extends AStructuredProfile {

	private final Nginx webserver;
	private final PHP php;
	private final MariaDB db;

	public EmailServer(String label, NetworkModel networkModel) {
		super(label, networkModel);

		this.webserver = new Nginx(getLabel(), networkModel);
		this.php = new PHP(getLabel(), networkModel);
		this.db = new MariaDB(getLabel(), networkModel);
	}

	@Override
	protected Collection<IUnit> getInstalled() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.webserver.getInstalled());
		units.addAll(this.php.getInstalled());
		units.addAll(this.db.getInstalled());

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentConfig() throws InvalidServerException, InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.webserver.getPersistentConfig());
		units.addAll(this.php.getPersistentConfig());
		units.addAll(this.db.getPersistentConfig());

		return units;
	}

	@Override
	public Collection<IUnit> getLiveConfig() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.webserver.getLiveConfig());
		units.addAll(this.php.getLiveConfig());
		units.addAll(this.db.getLiveConfig());

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws InvalidServerModelException, InvalidPortException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.webserver.getPersistentFirewall());

		getNetworkModel().getServerModel(getLabel()).addListen(Encapsulation.TCP, 25, 465, 993);
		getNetworkModel().getServerModel(getLabel()).addEgress("spamassassin.apache.org");
		getNetworkModel().getServerModel(getLabel()).addEgress("sa-update.pccc.com");

		return units;
	}
}
