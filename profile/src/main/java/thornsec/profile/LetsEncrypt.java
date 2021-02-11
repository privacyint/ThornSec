package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.model.ServerModel;
import core.profile.AStructuredProfile;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;

public class LetsEncrypt extends AStructuredProfile {
	
	public LetsEncrypt(ServerModel me, NetworkModel networkModel) {
		super("letsencrypt", me, networkModel);
	}

	protected Vector<IUnit> getInstalled() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new InstalledUnit("certbot", "proceed", "certbot"));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig() {
		Vector<IUnit> units =  new Vector<IUnit>();
		
		String config = "";
		config += "rsa-key-size = 4096\n";
		config += "email = " + networkModel.getData().getAdminEmail();
		
		units.addElement(new FileUnit("certbot_default_config", "certbot_installed", config, "/etc/letsencrypt/cli.ini"));
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig() {
		Vector<IUnit> units = new Vector<IUnit>();

		//First, are we a web proxy?

		return units;
	}
	
//	private Vector<IUnit> getCert(String name, String machineName, String path, String[] domains) {
//		Vector<IUnit> units = new Vector<IUnit>();
		
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
	
	public Vector<IUnit> getNetworking() {
		Vector<IUnit> units = new Vector<IUnit>();

		me.addRequiredEgress("acme-v01.api.letsencrypt.org");
		
		return units;
	}
}
