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
		
		units.addAll(model.getServerModel(server).getBindFsModel().addDataBindPoint(server, model, "onionbalance", "onionbalance_installed", "onionbalance", "onionbalance", "0700"));
		units.addAll(model.getServerModel(server).getBindFsModel().addLogBindPoint(server, model, "onionbalance", "onionbalance_installed", "onionbalance", "0755"));
		units.addAll(model.getServerModel(server).getBindFsModel().addDataBindPoint(server, model, "tor", "tor_installed", "debian-tor", "debian-tor", "0700"));
		units.addAll(model.getServerModel(server).getBindFsModel().addLogBindPoint(server, model, "tor", "tor_installed", "debian-tor", "0755"));

		units.add(new DirUnit("onionbalance_var_run", "onionbalance_installed", "/var/run/onionbalance"));
		units.add(new DirOwnUnit("onionbalance_var_run", "onionbalance_var_run_created", "/var/run/onionbalance", "onionbalance"));
		
		String service = "";
		service += "[Unit]\n";
		service += "Description=OnionBalance - Tor Onion Service load balancer\n";
		service += "Documentation=man:onionbalance\n";
		service += "Documentation=file:///usr/share/doc/onionbalance/html/index.html\n";
		service += "Documentation=https://github.com/DonnchaC/onionbalance\n";
		service += "After=network.target, tor.service\n";
		service += "Wants=network-online.target\n";
		service += "ConditionPathExists=/etc/onionbalance/config.yaml\n";
		service += "\n";
		service += "[Service]\n";
		service += "Type=simple\n";
		service += "PIDFile=/run/onionbalance.pid\n";
		service += "Environment=\"ONIONBALANCE_LOG_LOCATION=/var/log/onionbalance/log\"\n";
		service += "ExecStartPre=/bin/chmod o+r /var/run/tor/control.authcookie\n";
		service += "ExecStartPre=/bin/chmod o+r /var/run/tor/control\n";
		service += "ExecStartPre=/bin/mkdir -p /var/run/onionbalance\n";
		service += "ExecStartPre=/bin/chown -R onionbalance:onionbalance /var/run/onionbalance\n";
		service += "ExecStart=/usr/sbin/onionbalance -c /etc/onionbalance/config.yaml\n";
		service += "ExecReload=/usr/sbin/onionbalance reload\n";
		service += "ExecStop=-/sbin/start-stop-daemon --quiet --stop --retry=TERM/5/KILL/5 --pidfile /run/onionbalance.pid\n";
		service += "TimeoutStopSec=5\n";
		service += "KillMode=mixed\n";
		service += "\n";
		service += "EnvironmentFile=-/etc/default/%p\n";
		service += "User=onionbalance\n";
		service += "PermissionsStartOnly=true\n";
		service += "Restart=always\n";
		service += "RestartSec=10s\n";
		service += "LimitNOFILE=65536\n";
		service += "\n";
		service += "NoNewPrivileges=yes\n";
		service += "PrivateDevices=yes\n";
		service += "PrivateTmp=yes\n";
		service += "ProtectHome=yes\n";
		service += "ProtectSystem=full\n";
		service += "ReadOnlyDirectories=/\n";
		service += "ReadWriteDirectories=-/proc\n";
		service += "ReadWriteDirectories=-/var/log/onionbalance\n";
		service += "ReadWriteDirectories=-/var/run\n";
		service += "\n";
		service += "[Install]\n";
		service += "WantedBy=multi-user.target";

		units.addElement(model.getServerModel(server).getConfigsModel().addConfigFile("onionbalance_service", "onionbalance_installed", service, "/lib/systemd/system/onionbalance.service"));
		
		String torrc = "";
		torrc += "Datadirectory /var/lib/tor\n";
		torrc += "ControlPort 9051\n";
		torrc += "CookieAuthentication 1\n";
		torrc += "SocksPort 0\n";
		torrc += "\n";
		torrc += "RunAsDaemon 1\n";
		torrc += "\n";
		torrc += "FascistFirewall 1";

		units.addElement(model.getServerModel(server).getConfigsModel().addConfigFile("torrc", "tor_installed", torrc, "/etc/tor/torrc"));

		units.addElement(new SimpleUnit("tor_service_enabled", "torrc",
				"sudo systemctl enable tor",
				"sudo systemctl is-enabled tor", "enabled", "pass",
				"Couldn't set tor to auto-start on boot.  You will need to manually start the service (\"sudo service tor start\") on reboot."));
		
		units.addElement(new SimpleUnit("onionbalance_service_enabled", "onionbalance_service",
				"sudo systemctl enable onionbalance",
				"sudo systemctl is-enabled onionbalance", "enabled", "pass",
				"Couldn't set onionbalance to auto-start on boot.  You will need to manually start the service (\"sudo service onionbalance start\") on reboot."));
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String onionbalanceConfig = "";
		onionbalanceConfig += "REFRESH_INTERVAL: 600\n";
		onionbalanceConfig += "services:\n";
		onionbalanceConfig += "    - key: /media/data/onionbalance/private_key\n";
		onionbalanceConfig += "      instances:";
		
		String[] backends = model.getData().getPropertyArray(server, "backend");
		for (String backend : backends) {
			onionbalanceConfig += "\n";
			onionbalanceConfig += "        - address: " + backend;
		}
		
		units.addElement(model.getServerModel(server).getConfigsModel().addConfigFile("onionbalance", "onionbalance_installed", onionbalanceConfig, "/etc/onionbalance/config.yaml"));
		
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