/*
 * This code is part of the ThornSec project.
 * 
 * To learn more, please head to its GitHub repo: @privacyint
 * 
 * Pull requests encouraged.
 */
package profile.service.machine;

import java.util.HashSet;
import java.util.Set;

import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.network.NetworkModel;

import core.profile.AStructuredProfile;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;

/**
 * This is a profile for https://letsencrypt.org/ - a free, automated CA
 * 
 * Implement this profile to get automatic SSL certificates on your LB!
 */
public class LetsEncrypt extends AStructuredProfile {
	
	public LetsEncrypt(String label, NetworkModel networkModel) {
		super("letsencrypt", networkModel);
	}

	protected Set<IUnit> getInstalled() {
		Set<IUnit> units = new HashSet<IUnit>();
		
		units.add(new InstalledUnit("certbot", "proceed", "certbot"));
		
		return units;
	}
	
	protected Set<IUnit> getPersistentConfig()
	throws InvalidServerModelException {
		Set<IUnit> units =  new HashSet<IUnit>();
		
		String config = "";
		config += "rsa-key-size = 4096\n";
		config += "email = " + networkModel.getServerModel(getLabel()).getEmailAddress();
		
		units.add(new FileUnit("certbot_default_config", "certbot_installed", config, "/etc/letsencrypt/cli.ini"));
		
		return units;
	}

	protected Set<IUnit> getLiveConfig() {
		Set<IUnit> units = new HashSet<IUnit>();

		//First, are we a web proxy?

		return units;
	}
	
//	private Set<IUnit> getCert(String name, String machineName, String path, String[] domains) {
//		Set<IUnit> units = new HashSet<IUnit>();
		
		//String invocation = "certbot"
		//		+ " certonly" //Just issue cert
		//		+ " --agree-tos" //ToS are for suckers
		//		+ " -n" //force non-interactive
		//		+ " -d " + String.join(",", domains) //Domains"
		//		+ " --cert-name " + machineName 
		//		+ " --cert-path /media/data/tls/"
		//		;
		
		
//		return units;
//	}
	
	public Set<IUnit> getPersistentFirewall()
	throws InvalidServerModelException {
		Set<IUnit> units = new HashSet<IUnit>();

		networkModel.getServerModel(getLabel()).addEgress("acme-v01.api.letsencrypt.org");
		
		return units;
	}
}
