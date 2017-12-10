package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.FirewallModel;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.DirOwnUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileEditUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;

public class OnionBalance extends AStructuredProfile {
	
	public OnionBalance() {
		super("onionbalance");
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		model.getServerModel(server).getAptSourcesModel().addAptSource(server, model, "tor", "proceed", "deb http://deb.torproject.org/torproject.org stretch main", "keys.gnupg.net", "A3C4F0F979CAA22CDBA8F512EE8CBC9E886DDD89");
		
		units.addElement(new InstalledUnit("tor_keyring", "tor_gpg", "deb.torproject.org-keyring"));
		units.addElement(new InstalledUnit("tor", "tor_keyring_installed", "tor"));
		
		units.addElement(new InstalledUnit("onionbalance", "tor_installed", "onionbalance"));
		
		model.getServerModel(server).getUserModel().addUsername("debian-tor");
		model.getServerModel(server).getUserModel().addUsername("onionbalance");
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(model.getServerModel(server).getBindFsModel().addBindPoint(server, model, "tor", "tor_installed", "/media/metaldata/tor", "/media/data/tor", "debian-tor", "debian-tor", "0700"));
		units.addAll(model.getServerModel(server).getBindFsModel().addBindPoint(server, model, "tor_logs", "tor_installed", "/var/log/.tor", "/var/log/tor", "debian-tor", "debian-tor", "0755"));
		units.addAll(model.getServerModel(server).getBindFsModel().addBindPoint(server, model, "onionbalance", "onionbalance_installed", "/media/metaldata/onionbalance", "/media/data/onionbalance", "onionbalance", "onionbalance", "0700"));
		units.addAll(model.getServerModel(server).getBindFsModel().addBindPoint(server, model, "onionbalance_logs", "onionbalance_installed", "var/log/.onionbalance", "/var/log/onionbalance", "onionbalance", "onionbalance", "0755"));

		units.add(new FileEditUnit("onionbalanceconfig", "onionbalance_installed",
				"/etc/onionbalance/config.yaml",
				"/media/data/onionbalance/private_keys/config/master/config.yaml",
				"/etc/init.d/onionbalance"));
		units.add(new FileEditUnit("onionbalanceconfig", "onionbalance_installed",
						"/etc/onionbalance/config.yaml",
						"/media/data/onionbalance/private_keys/config/master/config.yaml",
						"/lib/systemd/system/onionbalance.service"));

		units.add(new DirUnit("onionbalance_var_run", "onionbalance_installed", "/var/run/onionbalance"));
		units.add(new DirOwnUnit("onionbalance_var_run", "onionbalance_var_run_created", "/var/run/onionbalance", "onionbalance"));
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new RunningUnit("tor", "tor", "/usr/bin/tor"));
		model.getServerModel(server).getProcessModel().addProcess("/usr/bin/tor --defaults-torrc /usr/share/tor/tor-service-defaults-torrc -f /etc/tor/torrc --RunAsDaemon 0$");

		return units;
	}
	
	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		//Allow the server to call out to torproject.org to download mainline
		model.getServerModel(server).addRouterFirewallRule(server, model, "allow_torproject", "deb.torproject.org", new String[]{"80","443"});
		
		for (String router : model.getRouters()) {
			model.getServerModel(router).getFirewallModel().addFilter(server + "_allow_onion_out_traffic", server + "_egress",
					"-p tcp"
					+ " -m tcp -m multiport --dports 80,443"
					+ " -j ACCEPT");
		}
		
		return units;
	}
	
}