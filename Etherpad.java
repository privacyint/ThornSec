package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.FileChecksumUnit;
import core.unit.fs.FileDownloadUnit;
import core.unit.fs.FilePermsUnit;
import core.unit.fs.GitCloneUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;

public class Etherpad extends AStructuredProfile {
	
	private Nginx webserver;
	private PHP php;
	private MariaDB db;
	
	public Etherpad() {
		super("etherpad");
		
		this.webserver = new Nginx();
		this.php = new PHP();
		this.db = new MariaDB();
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();

		units.addAll(webserver.getInstalled(server, model));
		units.addAll(php.getInstalled(server, model));
		units.addAll(db.getInstalled(server, model));
		
		units.addElement(new InstalledUnit("gzip", "proceed", "gzip"));
		units.addElement(new InstalledUnit("git", "gzip_installed", "git"));
		units.addElement(new InstalledUnit("curl", "git_installed", "curl"));
		units.addElement(new InstalledUnit("python", "curl_installed", "python"));
		units.addElement(new InstalledUnit("libssl", "python_installed", "libssl-dev"));
		units.addElement(new InstalledUnit("pkg_config", "libssl_installed", "pkg-config"));
		units.addElement(new InstalledUnit("build_essential", "pkg_config_installed", "build-essential"));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units =  new Vector<IUnit>();

		units.addAll(webserver.getPersistentConfig(server, model));
		units.addAll(db.getPersistentConfig(server, model));
		units.addAll(php.getPersistentConfig(server, model));	
		
		//Can't use node >= 7 just yet...
		//https://github.com/ether/etherpad-lite/issues/3074
		units.addElement(new FileDownloadUnit("nodejs", "build_essential_installed", "https://deb.nodesource.com/setup_6.x", "/root/nodejs.sh",
				"nodejs couldn't be downloaded.  Etherpad's installation will fail."));
		units.addElement(new FileChecksumUnit("nodejs", "nodejs_downloaded", "/root/nodejs.sh", "41b304696198364ad174e3799674496261c2093800b88314c796aebea16a6428127354b46f943493c8104edaf13fe9ad4c308b37c414d0639d765f8c56f093f4",
				"nodejs's checksum doesn't match.  This could indicate a failed download, MITM attack, or a newer version than our code supports.  Etherpad's installation will fail."));
		units.addElement(new FilePermsUnit("nodejs_is_executable", "nodejs_checksum", "/root/nodejs.sh", "755", "nodejs couldn't be set to be executable.  Etherpad's installation will fail."));
		
		units.addElement(new SimpleUnit("nodejs_setup_environment", "nodejs_is_executable_chmoded",
				"sudo -E /root/nodejs.sh",
				"sudo test -f /etc/apt/sources.list.d/nodesource.list && echo 'pass' || echo 'fail'", "pass", "pass",
				"nodejs's setup environment couldn't be configured.  Etherpad's installation will fail."));

		units.addElement(new InstalledUnit("nodejs", "nodejs_setup_environment", "nodejs"));
		
		units.addElement(new GitCloneUnit("etherpad", "nginx_installed", "git://github.com/ether/etherpad-lite.git", "/root/etherpad-lite",
				"Etherpad couldn't be downloaded.  Its installation will, therefore, fail."));
		
		units.addElement(new SimpleUnit("etherpad_install_dependencies", "etherpad_cloned",
				"sudo /root/etherpad-lite/bin/installDeps.sh",
				"sudo test -f /root/etherpad-lite/settings.json && echo 'pass' || echo 'fail'", "pass", "pass",
				"Couldn't install Etherpad's dependencies.  Etherpad's installation will fail."));
		
		units.addElement(new SimpleUnit("etherpad_installed", "etherpad_install_dependencies",
				"sudo cp -a /root/etherpad-lite/. /media/data/www/",
				"sudo test -f /media/data/www/settings.json && echo 'pass' || echo 'fail'", "pass", "pass",
				"Couldn't move Etherpad to the correct directory.  Etherpad won't be web accessible."));
		
		units.addElement(new SimpleUnit("etherpad_mysql_password", "etherpad_installed",
				"ETHERPAD_PASSWORD=`sudo grep \"password\" /media/data/www/settings.json | head -1 | awk '{ print $2 }' | tr -d \"\\\",\"`; [[ -z $ETHERPAD_PASSWORD ]] && ETHERPAD_PASSWORD=`openssl rand -hex 32`",
				"echo $ETHERPAD_PASSWORD", "", "fail",
				"Couldn't set the Etherpad database user's password.  Etherpad will be left in a broken state."));
		
		units.addAll(db.createDb("etherpad", "etherpad", "ALL", "ETHERPAD_PASSWORD"));

		String settings = "";
		settings += "{";
		settings += "\n";
		settings += "	\\\"title\\\": \\\"Etherpad\\\",\n";
		settings += "	\\\"favicon\\\": \\\"favicon.ico\\\",\n";
		settings += "	\\\"ip\\\": \\\"127.0.0.1\\\",\n";
		settings += "	\\\"port\\\": \\\"8080\\\",\n";
		settings += "	\\\"showSettingsInAdminPage\\\": \\\"true\\\",\n";
		settings += "\n";
		settings += "	\\\"dbType\\\": \\\"mysql\\\",\n";
		settings += "	\\\"dbSettings\\\": {\n";
		settings += "		\\\"user\\\": \\\"etherpad\\\",\n";
		settings += "		\\\"host\\\": \\\"localhost\\\",\n";
		settings += "		\\\"password\\\": \\\"${ETHERPAD_PASSWORD}\\\",\n";
		settings += "		\\\"database\\\": \\\"etherpad\\\",\n";
		settings += "		\\\"charset\\\": \\\"utf8mb4\\\"\n";
		settings += "	},\n";
		settings += "\n";
		settings += "	\\\"defaultPadText\\\": \\\"Welcome to Etherpad!\\n\\nThis pad text is synchronized as you type, so that everyone viewing this page sees the same text. This allows you to collaborate seamlessly on documents!\\n\\nGet involved with Etherpad at http:\\/\\/etherpad.org\\n\\\",\n";
		settings += "	\\\"suppressErrorsInPadText\\\": false,\n";
		settings += "	\\\"requireSession\\\" : false,\n";
		settings += "	\\\"editOnly\\\": false,\n";
		settings += "	\\\"sessionNoPassword\\\": false,\n";
		settings += "	\\\"minify\\\": true,\n";
		settings += "	\\\"maxAge\\\": 0,\n";
		settings += "	\\\"abiword\\\": null,\n";
		settings += "	\\\"soffice\\\": null,\n";
		settings += "	\\\"tidyHtml\\\": null,\n";
		settings += "	\\\"allowUnknownFileEnds\\\": true,\n";
		settings += "	\\\"requireAuthentication\\\": false,\n";
		settings += "	\\\"requireAuthorization\\\": false,\n";
		settings += "	\\\"trustProxy\\\": true,\n";
		settings += "	\\\"disableIPlogging\\\": false,\n";
		settings += "\n";
		settings += "	\\\"users\\\": {\n";
		settings += "	},\n";
		settings += "\n";
		settings += "	\\\"socketTransportProtocols\\\" : [\\\"xhr-polling\\\", \\\"jsonp-polling\\\", \\\"htmlfile\\\"],\n";
		settings += "	\\\"loadTest\\\": false,\n";
		settings += "	\\\"indentationOnNewLine\\\": true,\n";
		settings += "\n";
		settings += "	\\\"toolbar\\\": {\n";
		settings += "		\\\"left\\\": [\n";
		settings += "			[\\\"bold\\\", \\\"italic\\\", \\\"underline\\\", \\\"strikethrough\\\"],\n";
		settings += "			[\\\"orderedlist\\\", \\\"unorderedlist\\\", \\\"indent\\\", \\\"outdent\\\"],\n";
		settings += "			[\\\"undo\\\", \\\"redo\\\"],\n";
		settings += "			[\\\"clearauthorship\\\"]\n";
		settings += "		],\n";
		settings += "		\\\"right\\\": [\n";
		settings += "			[\\\"importexport\\\", \\\"timeslider\\\", \\\"savedrevision\\\"],\n";
		settings += "			[\\\"settings\\\", \\\"embed\\\"],\n";
		settings += "			[\\\"showusers\\\"]\n";
		settings += "		],\n";
		settings += "		\\\"timeslider\\\": [\n";
		settings += "			[\\\"timeslider_export\\\", \\\"timeslider_returnToPad\\\"]\n";
		settings += "		]\n";
		settings += "	},\n";
		settings += "\n";
		settings += "	\\\"loglevel\\\": \\\"INFO\\\",\n";
		settings += "}";
		
		units.addElement(model.getServerModel(server).getConfigsModel().addConfigFile("etherpad", "etherpad_installed", settings, "/media/data/www/settings.json"));
		
		//https://github.com/ether/etherpad-lite/wiki/How-to-deploy-Etherpad-Lite-as-a-service
		String serviceConf = "";
		serviceConf += "[Unit]\n";
		serviceConf += "Description=etherpad-lite (real-time collaborative document editing)\n";
		serviceConf += "After=syslog.target network.target\n";
		serviceConf += "\n";
		serviceConf += "[Service]\n";
		serviceConf += "Type=simple\n";
		serviceConf += "User=nginx\n";
		serviceConf += "Group=nginx\n";
		serviceConf += "ExecStart=/media/data/www/bin/run.sh\n";
		serviceConf += "\n";
		serviceConf += "[Install]\n";
		serviceConf += "WantedBy=multi-user.target";

		units.addElement(model.getServerModel(server).getConfigsModel().addConfigFile("etherpad_service", "etherpad_installed", serviceConf, "/etc/systemd/system/etherpad-lite.service"));

		units.addElement(new SimpleUnit("etherpad_service_enabled", "etherpad_service_config",
				"sudo systemctl enable etherpad-lite",
				"sudo systemctl is-enabled etherpad-lite", "enabled", "pass",
				"Couldn't set Etherpad to auto-start on boot.  You will need to manually start the service (\"sudo service etherpad-lite start\") on reboot."));
				
		units.addElement(new RunningUnit("etherpad", "etherpad-lite", "etherpad-lite"));
		
		model.getServerModel(server).getProcessModel().addProcess("node /media/data/www/node_modules/ep_etherpad-lite/node/server.js$");
		
		String nginxConf = "";
		nginxConf += "upstream etherpad-lite {\n";
	    nginxConf += "	server 127.0.0.1:8080;\n";
	    nginxConf += "}\n";
	    nginxConf += "\n";
	    nginxConf += "server {\n";
	    nginxConf += "	listen 80;\n";
	    nginxConf += "\n";
	    nginxConf += "	location / {\n";
	    nginxConf += "		proxy_buffering off;\n";
	    nginxConf += "		proxy_pass http://etherpad-lite/;\n";
	    nginxConf += "		proxy_pass_header Server;\n";
	    nginxConf += "	}\n";
	    nginxConf += "}";
		
		webserver.addLiveConfig("default", nginxConf);	
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(webserver.getLiveConfig(server, model));
		units.addAll(php.getLiveConfig(server, model));
		units.addAll(db.getLiveConfig(server, model));
						
		return units;
	}
	
	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(webserver.getPersistentFirewall(server, model));
		model.getServerModel(server).addRouterFirewallRule(server, model, "allow_github", "github.com", new String[]{"80","443"});
		model.getServerModel(server).addRouterFirewallRule(server, model, "allow_etherpad", "etherpad.org", new String[]{"80","443"});
		model.getServerModel(server).addRouterFirewallRule(server, model, "allow_beta_etherpad", "beta.etherpad.org", new String[]{"80","443"});
		model.getServerModel(server).addRouterFirewallRule(server, model, "allow_deb_nodesource", "deb.nodesource.com", new String[]{"80","443"});

		return units;
	}

}
