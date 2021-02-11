/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.profile.service.email;

import java.util.ArrayList;
import java.util.Collection;
import org.privacyinternational.thornsec.core.data.machine.configuration.TrafficRule.Encapsulation;
import org.privacyinternational.thornsec.core.exception.data.InvalidPortException;
import org.privacyinternational.thornsec.core.exception.data.machine.InvalidServerException;
import org.privacyinternational.thornsec.core.exception.runtime.InvalidMachineModelException;
import org.privacyinternational.thornsec.core.iface.IUnit;
import org.privacyinternational.thornsec.core.model.machine.ServerModel;
import org.privacyinternational.thornsec.core.profile.AStructuredProfile;
import inet.ipaddr.HostName;
import org.privacyinternational.thornsec.profile.stack.MariaDB;
import org.privacyinternational.thornsec.profile.stack.Nginx;
import org.privacyinternational.thornsec.profile.stack.PHP;

/**
 * This profile is, in effect, a stub for now.
 *
 * It handles firewall stuff, but other than that, you're on your own.
 */
public class EmailServer extends AStructuredProfile {

	private final Nginx webserver;
	private final PHP php;
	private final MariaDB db;

	public EmailServer(ServerModel me) {
		super(me);

		this.webserver = new Nginx(me);
		this.php = new PHP(me);
		this.db = new MariaDB(me);
	}

	@Override
	public Collection<IUnit> getInstalled() throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.webserver.getInstalled());
		units.addAll(this.php.getInstalled());
		units.addAll(this.db.getInstalled());

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentConfig() throws InvalidServerException, InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.webserver.getPersistentConfig());
		units.addAll(this.php.getPersistentConfig());
		units.addAll(this.db.getPersistentConfig());

		return units;
	}

	@Override
	public Collection<IUnit> getLiveConfig() throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.webserver.getLiveConfig());
		units.addAll(this.php.getLiveConfig());
		units.addAll(this.db.getLiveConfig());

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws InvalidPortException, InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.webserver.getPersistentFirewall());

		getMachineModel().addListen(Encapsulation.TCP, 25, 465, 993);
		getMachineModel().addEgress(new HostName("spamassassin.apache.org:443"));
		getMachineModel().addEgress(new HostName("sa-update.pccc.com:443"));

		return units;
	}
}
