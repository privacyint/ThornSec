/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.profile.service.machine;

import java.util.ArrayList;
import java.util.Collection;
import org.privacyinternational.thornsec.core.exception.data.InvalidPortException;
import org.privacyinternational.thornsec.core.exception.runtime.InvalidMachineModelException;
import org.privacyinternational.thornsec.core.iface.IUnit;
import org.privacyinternational.thornsec.core.model.machine.ServerModel;
import org.privacyinternational.thornsec.core.profile.AStructuredProfile;
import org.privacyinternational.thornsec.core.unit.fs.FileUnit;
import org.privacyinternational.thornsec.core.unit.pkg.InstalledUnit;
import inet.ipaddr.HostName;

/**
 * This is a profile for https://letsencrypt.org/ - a free, automated CA
 *
 * Implement this profile to get automatic SSL certificates on your LB!
 */
public class LetsEncrypt extends AStructuredProfile {

	public LetsEncrypt(ServerModel me) {
		super(me);
	}

	@Override
	public Collection<IUnit> getInstalled() {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("certbot", "proceed", "certbot"));

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentConfig() throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		String config = "";
		config += "rsa-key-size = 4096\n";
		config += "email = " + getMachineModel().getEmailAddress();

		units.add(new FileUnit("certbot_default_config", "certbot_installed", config, "/etc/letsencrypt/cli.ini"));

		return units;
	}

	@Override
	public Collection<IUnit> getLiveConfig() {
		final Collection<IUnit> units = new ArrayList<>();

		// First, are we a web proxy?

		return units;
	}

//	private Collection<IUnit> getCert(String name, String machineName, String path, String[] domains) {
//		Collection<IUnit> units = new ArrayList<>();

	// String invocation = "certbot"
	// + " certonly" //Just issue cert
	// + " --agree-tos" //ToS are for suckers
	// + " -n" //force non-interactive
	// + " -d " + String.join(",", domains) //Domains"
	// + " --cert-name " + machineName
	// + " --cert-path /media/data/tls/"
	// ;

//		return units;
//	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws InvalidMachineModelException, InvalidPortException {
		final Collection<IUnit> units = new ArrayList<>();

		getMachineModel().addEgress(new HostName("acme-v02.api.letsencrypt.org:80"));
		getMachineModel().addEgress(new HostName("acme-v02.api.letsencrypt.org:443"));
		getMachineModel().addEgress(new HostName("ocsp.int-x3.letsencrypt.org:80"));
		getMachineModel().addEgress(new HostName("ocsp.int-x3.letsencrypt.org:443"));

		return units;
	}
}
