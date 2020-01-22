/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.service.web;

import java.util.ArrayList;
import java.util.Collection;

import core.exception.data.InvalidPortException;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.network.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.FileUnit;
import core.unit.fs.GitCloneUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;
import profile.stack.MariaDB;
import profile.stack.Nginx;
import profile.stack.NodeJS;

/**
 * This profile creates and configures an Etherpad-Lite
 * (https://github.com/ether/etherpad-lite) instance
 */
public class Etherpad extends AStructuredProfile {

	private final Nginx webserver;
	private final NodeJS node;
	private final MariaDB db;

	public Etherpad(String label, NetworkModel networkModel) {
		super(label, networkModel);

		this.webserver = new Nginx(getLabel(), networkModel);
		this.node = new NodeJS(getLabel(), networkModel);
		this.db = new MariaDB(getLabel(), networkModel);

		this.db.setUsername("etherpad");
		this.db.setUserPrivileges("ALL");
		this.db.setUserPassword("${ETHERPAD_PASSWORD}");
		this.db.setDb("etherpad");
	}

	@Override
	protected Collection<IUnit> getInstalled() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.webserver.getInstalled());
		units.addAll(this.node.getInstalled());
		units.addAll(this.db.getInstalled());

		units.add(new InstalledUnit("gzip", "proceed", "gzip"));
		units.add(new InstalledUnit("git", "gzip_installed", "git"));
		units.add(new InstalledUnit("curl", "git_installed", "curl"));
		units.add(new InstalledUnit("python", "curl_installed", "python"));
		units.add(new InstalledUnit("libssl", "python_installed", "libssl-dev"));
		units.add(new InstalledUnit("pkg_config", "libssl_installed", "pkg-config"));

		units.add(new GitCloneUnit("etherpad", "nginx_installed", "https://github.com/ether/etherpad-lite.git",
				"/root/etherpad-lite", "Etherpad couldn't be downloaded.  Its installation will, therefore, fail."));

		units.add(new SimpleUnit("etherpad_install_dependencies", "etherpad_cloned",
				"sudo /root/etherpad-lite/bin/installDeps.sh",
				"sudo test -f /root/etherpad-lite/settings.json && echo pass || echo fail", "pass", "pass",
				"Couldn't install Etherpad's dependencies.  Etherpad's installation will fail."));

		units.add(new SimpleUnit("etherpad_installed", "etherpad_install_dependencies",
				"sudo cp -a /root/etherpad-lite/. /media/data/www/",
				"sudo test -f /media/data/www/settings.json && echo 'pass' || echo 'fail'", "pass", "pass",
				"Couldn't move Etherpad to the correct directory.  Etherpad won't be web accessible."));

		return units;
	}

	@Override
	protected Collection<IUnit> getPersistentConfig() throws InvalidServerModelException, InvalidServerException {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new SimpleUnit("etherpad_mysql_password", "etherpad_installed",
				"ETHERPAD_PASSWORD=`sudo grep \"password\" /media/data/www/settings.json | head -1 | awk '{ print $2 }' | tr -d \"',\"`; [[ -z $ETHERPAD_PASSWORD ]] && ETHERPAD_PASSWORD=`openssl rand -hex 32`",
				"echo $ETHERPAD_PASSWORD", "", "fail",
				"Couldn't set the Etherpad database user's password.  Etherpad will be left in a broken state."));

		// Set up our database
		units.addAll(this.db.checkUserExists());
		units.addAll(this.db.checkDbExists());

		final FileUnit settings = new FileUnit("settings_conf", "etherpad_installed", "/media/data/www/settings.json");

		settings.appendLine("{");
		settings.appendCarriageReturn();
		settings.appendLine("   'title': 'Etherpad',");
		settings.appendLine("	'favicon': 'favicon.ico',");
		settings.appendLine("	'ip': '127.0.0.1',");
		settings.appendLine("	'port': '8080',");
		settings.appendLine("	'showSettingsInAdminPage': 'true',");
		settings.appendCarriageReturn();
		settings.appendLine("	'dbType': 'mysql',");
		settings.appendLine("	'dbSettings': {");
		settings.appendLine("		'user': 'etherpad',");
		settings.appendLine("		'host': 'localhost',");
		settings.appendLine("		'password': '${ETHERPAD_PASSWORD}',");
		settings.appendLine("		'database': 'etherpad',");
		settings.appendLine("		'charset': 'utf8mb4'");
		settings.appendLine("	},");
		settings.appendCarriageReturn();
		settings.appendLine(
				"	'defaultPadText': 'Welcome to Etherpad!\\n\\nThis pad text is synchronized as you type, so that everyone viewing this page sees the same text. This allows you to collaborate seamlessly on documents!\\n\\nGet involved with Etherpad at http:\\/\\/etherpad.org\\n',");
		settings.appendLine("	'suppressErrorsInPadText': false,");
		settings.appendLine("	'requireSession' : false,");
		settings.appendLine("	'editOnly': false,");
		settings.appendLine("	'sessionNoPassword': false,");
		settings.appendLine("	'minify': true,");
		settings.appendLine("	'maxAge': 0,");
		settings.appendLine("	'abiword': null,");
		settings.appendLine("	'soffice': null,");
		settings.appendLine("	'tidyHtml': null,");
		settings.appendLine("	'allowUnknownFileEnds': true,");
		settings.appendLine("	'requireAuthentication': false,");
		settings.appendLine("	'requireAuthorization': false,");
		settings.appendLine("	'trustProxy': true,");
		settings.appendLine("	'disableIPlogging': false,");
		settings.appendCarriageReturn();
		settings.appendLine("	'users': {");
		settings.appendLine("	},");
		settings.appendCarriageReturn();
		settings.appendLine("	'socketTransportProtocols' : ['xhr-polling', 'jsonp-polling', 'htmlfile'],");
		settings.appendLine("	'loadTest': false,");
		settings.appendLine("	'indentationOnNewLine': true,");
		settings.appendCarriageReturn();
		settings.appendLine("	'toolbar': {");
		settings.appendLine("		'left': [");
		settings.appendLine("			['bold', 'italic', 'underline', 'strikethrough'],");
		settings.appendLine("			['orderedlist', 'unorderedlist', 'indent', 'outdent'],");
		settings.appendLine("			['undo', 'redo'],");
		settings.appendLine("			['clearauthorship']");
		settings.appendLine("		],");
		settings.appendLine("		'right': [");
		settings.appendLine("			['importexport', 'timeslider', 'savedrevision'],");
		settings.appendLine("			['settings', 'embed'],");
		settings.appendLine("			['showusers']");
		settings.appendLine("		],");
		settings.appendLine("		'timeslider': [");
		settings.appendLine("			['timeslider_export', 'timeslider_returnToPad']");
		settings.appendLine("		]");
		settings.appendLine("	},");
		settings.appendCarriageReturn();
		settings.appendLine("	'loglevel': 'INFO',");
		settings.appendLine("}");

		// https://github.com/ether/etherpad-lite/wiki/How-to-deploy-Etherpad-Lite-as-a-service
		final FileUnit serviceConf = new FileUnit("etherpad_service", "etherpad_installed",
				"/etc/systemd/system/etherpad-lite.service");
		units.add(serviceConf);

		serviceConf.appendLine("[Unit]");
		serviceConf.appendLine("Description=etherpad-lite (real-time collaborative document editing)");
		serviceConf.appendLine("After=syslog.target network.target");
		serviceConf.appendCarriageReturn();
		serviceConf.appendLine("[Service]");
		serviceConf.appendLine("Type=simple");
		serviceConf.appendLine("User=nginx");
		serviceConf.appendLine("Group=nginx");
		serviceConf.appendLine("ExecStart=/media/data/www/bin/run.sh");
		serviceConf.appendCarriageReturn();
		serviceConf.appendLine("[Install]");
		serviceConf.appendLine("WantedBy=multi-user.target");

		units.add(new SimpleUnit("etherpad_service_enabled", "etherpad_service_config",
				"sudo systemctl enable etherpad-lite", "sudo systemctl is-enabled etherpad-lite", "enabled", "pass",
				"Couldn't set Etherpad to auto-start on boot.  You will need to manually start the service (\"sudo service etherpad-lite start\") on reboot."));

		final FileUnit nginxConf = new FileUnit("etherpad_nginx", "etherpad_installed",
				Nginx.DEFAULT_CONFIG_FILE.toString());
		nginxConf.appendLine("upstream etherpad-lite {");
		nginxConf.appendLine("	server 127.0.0.1:8080;");
		nginxConf.appendLine("}");
		nginxConf.appendLine("");
		nginxConf.appendLine("server {");
		nginxConf.appendLine("	listen 80;");
		nginxConf.appendLine("");
		nginxConf.appendLine("	location / {");
		nginxConf.appendLine("		proxy_buffering off;");
		nginxConf.appendLine("		proxy_pass http://etherpad-lite/;");
		nginxConf.appendLine("		proxy_pass_header Server;");
		nginxConf.appendLine("	}");
		nginxConf.appendLine("}");

		this.webserver.addLiveConfig(nginxConf);

		return units;
	}

	@Override
	public Collection<IUnit> getLiveConfig() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.webserver.getLiveConfig());
		units.addAll(this.node.getLiveConfig());
		units.addAll(this.db.getLiveConfig());

		units.add(new RunningUnit("etherpad", "etherpad-lite", "etherpad-lite"));

		getNetworkModel().getServerModel(getLabel())
				.addProcessString("node /media/data/www/node_modules/ep_etherpad-lite/node/server.js$");

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws InvalidServerModelException, InvalidPortException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.webserver.getPersistentFirewall());
		units.addAll(this.node.getPersistentFirewall());
		units.addAll(this.db.getPersistentFirewall());

		// Let's open this box up to most of the internet.
		getNetworkModel().getServerModel(getLabel()).addEgress("github.com:443");
		getNetworkModel().getServerModel(getLabel()).addEgress("etherpad.org:80");
		getNetworkModel().getServerModel(getLabel()).addEgress("beta.etherpad.org:80");
		getNetworkModel().getServerModel(getLabel()).addEgress("code.jquery.com:80");
		getNetworkModel().getServerModel(getLabel()).addEgress("etherpad.org:443");
		getNetworkModel().getServerModel(getLabel()).addEgress("beta.etherpad.org:443");
		getNetworkModel().getServerModel(getLabel()).addEgress("code.jquery.com:443");

		return units;
	}
}
