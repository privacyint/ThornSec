package profile.service.machine;

import java.util.HashSet;
import java.util.Set;

import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.network.NetworkModel;

import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.pkg.InstalledUnit;

public class Msmtp extends AStructuredProfile {
	
	public Msmtp(String label, NetworkModel networkModel) {
		super("msmtp", networkModel);
	}

	protected Set<IUnit> getInstalled()
	throws InvalidServerModelException {
		Set<IUnit> units = new HashSet<IUnit>();
		
		units.addAll(getNetworkModel().getServerModel(getLabel()).getBindFsModel().addLogBindPoint("msmtp", "proceed", "nginx", "0750"));

		units.add(new InstalledUnit("msmtp", "proceed", "msmtp"));
		units.add(new InstalledUnit("ca_certificates", "proceed", "ca-certificates"));
		
		return units;
	}

	protected Set<IUnit> getLiveConfig() {
		Set<IUnit> units = new HashSet<IUnit>();
		
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
		
		units.add(new SimpleUnit("msmtprc_exists", "msmtp_installed",
				"",
				"sudo [ -f \"/etc/msmtprc\" ] && echo pass || echo fail", "pass", "pass",
				"You need to create the file /etc/msmtprc, using the following format:\n" + msmtprc));
		
		return units;
	}
}
