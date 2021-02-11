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
import core.exception.runtime.InvalidMachineModelException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.machine.ServerModel;
import core.model.network.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.pkg.InstalledUnit;

public class Msmtp extends AStructuredProfile {

	public Msmtp(ServerModel me) {
		super(me);
	}

	@Override
	public Collection<IUnit> getInstalled() throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("msmtp", "proceed", "msmtp"));
		units.add(new InstalledUnit("ca_certificates", "proceed", "ca-certificates"));

		return units;
	}

	@Override
	public Collection<IUnit> getLiveConfig() {
		final Collection<IUnit> units = new ArrayList<>();

		String msmtprc = "";
		msmtprc += "account        default\n";
		msmtprc += "host           <SMTP HOST>\n";
		msmtprc += "port           25\n";
		msmtprc += "from           <FROM EMAIL ADDRESS>\n";
		msmtprc += "user           <SMTP USERNAME>\n";
		msmtprc += "password       <SMTP PASSWORD>\n";
		msmtprc += "auth           on\n";
		msmtprc += "tls            on\n";
		msmtprc += "tls_trust_file /etc/ssl/certs/ca-certificates.crt\n";
		msmtprc += "logfile        /var/log/msmtp/msmtp.log";

		units.add(new SimpleUnit("msmtprc_exists", "msmtp_installed", "",
				"sudo [ -f \"/etc/msmtprc\" ] && echo pass || echo fail", "pass", "pass",
				"You need to create the file /etc/msmtprc, using the following format:\n" + msmtprc));

		return units;
	}
}
