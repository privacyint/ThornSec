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
import core.unit.fs.DirOwnUnit;
import core.unit.fs.DirPermsUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileOwnUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import profile.stack.MariaDB;
import profile.stack.Nginx;

/**
 * A profile which configures and installs a Redmine (https://www.redmine.org/)
 * instance
 */
public class Redmine extends AStructuredProfile {

	private final Nginx webserver;
	private final MariaDB db;

	public Redmine(String label, NetworkModel networkModel) {
		super(label, networkModel);

		this.webserver = new Nginx(getLabel(), networkModel);
		this.db = new MariaDB(getLabel(), networkModel);
	}

	@Override
	protected Collection<IUnit> getInstalled() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.webserver.getInstalled());
		units.addAll(this.db.getInstalled());

		units.add(new InstalledUnit("redmine", "proceed", "redmine-mysql"));
		units.add(new InstalledUnit("thin", "redmine_installed", "thin"));
		units.add(new InstalledUnit("sendmail", "proceed", "sendmail"));

		getNetworkModel().getServerModel(getLabel())
				.addProcessString("thin server \\(/var/run/thin/sockets/thin.[0-3].sock\\)$");

		return units;
	}

	@Override
	protected Collection<IUnit> getPersistentConfig() throws InvalidServerModelException, InvalidServerException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(getNetworkModel().getServerModel(getLabel()).getBindFsModel().addDataBindPoint("redmine_logs",
				"proceed", "www-data", "www-data", "0640"));
		units.addAll(getNetworkModel().getServerModel(getLabel()).getBindFsModel().addDataBindPoint("redmine_files",
				"proceed", "www-data", "www-data", "0750"));
		units.addAll(getNetworkModel().getServerModel(getLabel()).getBindFsModel().addDataBindPoint("redmine_data",
				"proceed", "www-data", "www-data", "0750"));

		this.db.setUsername("redmine");
		this.db.setUserPrivileges("ALL");
		this.db.setUserPassword("${REDMINE_PASSWORD}");
		this.db.setDb("redmine");

		// TODO: This method is messy and has race conditions. Let's fix it for v3...

		units.add(new SimpleUnit("logs_symlinked", "redmine_installed",
				// We don't really care about logs at this point
				"sudo mv /var/log/redmine/* /media/data/redmine_logs;"
						// Then symlink
						+ "sudo rm -R /var/log/redmine;" + "sudo ln -s /media/data/redmine_logs /var/log/redmine;",
				"sudo [ -L /var/log/redmine ] && echo pass || echo fail", "pass", "pass"));

		units.add(new SimpleUnit("files_symlinked", "redmine_installed",
				// Move over fresh installation if the files aren't already there
				"if [ ! -d /media/data/redmine_files/default ]; then sudo mv /var/lib/redmine/* /media/data/redmine_files ; fi ;"
						// Then symlink
						+ "sudo rm -R /var/lib/redmine ; sudo ln -s /media/data/redmine_files /var/lib/redmine;",
				"[ -L /var/lib/redmine ] && echo pass || echo fail", "pass", "pass"));

		units.add(new SimpleUnit("data_symlinked", "redmine_installed",
				// Move over fresh installation if the files aren't already there
				"if [ ! -d /media/data/redmine_data/config ] ; then sudo mv /usr/share/redmine/* /media/data/redmine_data ; fi ;"
						// Then symlink
						+ "sudo rm -R /usr/share/redmine ; sudo ln -s /media/data/redmine_data /usr/share/redmine;",
				"[ -L /usr/share/redmine ] && echo pass || echo fail", "pass", "pass"));

		units.add(
				new FileOwnUnit("database_config", "redmine_installed", "/etc/redmine/default/database.yml", "nginx"));
		units.add(new FileOwnUnit("secret_key", "redmine_installed", "/etc/redmine/default/secret_key.txt", "nginx"));

		units.add(new SimpleUnit("redmine_mysql_password", "proceed",
				"REDMINE_PASSWORD=`sudo grep \"password\" /usr/share/redmine/instances/default/config/database.yml 2>/dev/null | grep -v \"[*#]\" | awk '{ print $2 }'`; [[ -z $REDMINE_PASSWORD ]] && REDMINE_PASSWORD=`openssl rand -hex 32`",
				"echo $REDMINE_PASSWORD", "", "fail",
				"Couldn't set the Redmine database user's password.  Redmine will be left in a broken state."));

		// Set up our database
		units.addAll(this.db.checkUserExists());
		units.addAll(this.db.checkDbExists());

		units.add(new DirUnit("database_config_dir", "proceed", "/usr/share/redmine/instances/default/config/"));

		final FileUnit dbConfig = new FileUnit("database_config", "mariadb_installed",
				"/usr/share/redmine/instances/default/config/database.yml");
		units.add(dbConfig);
		dbConfig.appendLine("production:");
		dbConfig.appendLine("  adapter: mysql2");
		dbConfig.appendLine("  database: redmine");
		dbConfig.appendLine("  host: localhost");
		dbConfig.appendLine("  username: redmine");
		dbConfig.appendLine("  password: ${REDMINE_PASSWORD}");
		dbConfig.appendLine("  encoding: utf8");

		units.add(new DirUnit("thin_pid_dir", "thin_installed", "/var/run/thin"));
		units.add(new DirOwnUnit("thin_pid_dir", "thin_pid_dir_created", "/var/run/thin", "www-data"));
		units.add(new DirPermsUnit("thin_pid_dir_perms", "thin_pid_dir_chowned", "/var/run/thin", "744"));

		units.add(new DirUnit("thin_sockets_dir", "thin_installed", "/var/run/thin/sockets"));
		units.add(new DirOwnUnit("thin_sockets_dir_permissions", "thin_sockets_dir_created", "/var/run/thin/sockets",
				"www-data"));

		final FileUnit thinConfig = new FileUnit("thin_config", "thin_installed", "/etc/thin2.3/redmine.yml");
		units.add(thinConfig);
		thinConfig.appendLine("---");
		thinConfig.appendLine("chdir: \"/usr/share/redmine\"");
		thinConfig.appendLine("environment: production");
		thinConfig.appendLine("timeout: 30");
		thinConfig.appendLine("log: \"/media/data/redmine_logs/redmine.log\"");
		thinConfig.appendLine("pid: \"/var/run/thin/redmine.pid\"");
		thinConfig.appendLine("max_conns: 1024");
		thinConfig.appendLine("max_persistent_conns: 100");
		thinConfig.appendLine("require: []");
		thinConfig.appendLine("wait: 30");
		thinConfig.appendLine("threadpool_size: 20");
		thinConfig.appendLine("socket: \"/var/run/thin/sockets/thin.sock\"");
		thinConfig.appendLine("daemonize: true");
		thinConfig.appendLine("user: www-data");
		thinConfig.appendLine("group: www-data");
		thinConfig.appendLine("servers: 4");

		units.add(new SimpleUnit("secret_key_base", "proceed",
				"SECRET_KEY_BASE=`grep \"secret_key_base:\" /usr/share/redmine/config/secrets.yml 2>/dev/null | awk '{ print $2 }'`; [[ -z $SECRET_KEY_BASE ]] && SECRET_KEY_BASE=`cd /usr/share/redmine; sudo rake secret`",
				"echo $SECRET_KEY_BASE", "", "fail"));

		final FileUnit thinSecretKey = new FileUnit("thin_secret_key", "thin_installed",
				"/usr/share/redmine/config/secrets.yml");
		units.add(thinSecretKey);
		thinSecretKey.appendLine("production:");
		thinSecretKey.appendLine("  secret_key_base: ${SECRET_KEY_BASE}");

		final FileUnit emailConf = new FileUnit("redmine_allow_email", "msmtp",
				"/usr/share/redmine/config/secrets.yml");
		units.add(emailConf);

		emailConf.appendLine("default:");
		emailConf.appendLine("  email_delivery:");
		emailConf.appendLine("    delivery_method: :sendmail");

		final FileUnit nginxConf = new FileUnit("redmine_nginx_conf", "nginx_installed",
				Nginx.DEFAULT_CONFIG_FILE.toString());
		nginxConf.appendLine("upstream thin_cluster {");
		nginxConf.appendLine("    server unix:/var/run/thin/sockets/thin.0.sock;");
		nginxConf.appendLine("    server unix:/var/run/thin/sockets/thin.1.sock;");
		nginxConf.appendLine("    server unix:/var/run/thin/sockets/thin.2.sock;");
		nginxConf.appendLine("    server unix:/var/run/thin/sockets/thin.3.sock;");
		nginxConf.appendLine("}");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("server {");
		nginxConf.appendLine("    listen *:80 default;");
		nginxConf.appendLine("    server_name _;");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("    root /media/data/redmine_data/instances/default/public;");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("    location ^~ /plugin_assets/ {");
		nginxConf.appendLine("        gzip_static on;");
		nginxConf.appendLine("        expires max;");
		nginxConf.appendLine("        add_header Cache-Control public;");
		nginxConf.appendLine("    }");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("    try_files $uri/index.html $uri.html $uri @cluster;");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("    location @cluster {");
		nginxConf.appendLine("        proxy_pass http://thin_cluster;");
		nginxConf.appendLine("    }");
		nginxConf.appendLine("    include /media/data/nginx_custom_conf_d/default.conf;");
		nginxConf.appendLine("}");

		this.webserver.addLiveConfig(nginxConf);

		units.add(new DirOwnUnit("redmine_cache", "redmine_installed", "/var/cache/redmine/default/tmp", "www-data"));
		units.add(new DirOwnUnit("redmine_cache_tmp", "redmine_installed", "/var/cache/redmine/default/tmp/cache",
				"www-data"));

		units.addAll(this.webserver.getPersistentConfig());
		units.addAll(this.db.getPersistentConfig());

		return units;
	}

	@Override
	public Collection<IUnit> getLiveConfig() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.webserver.getLiveConfig());
		units.addAll(this.db.getLiveConfig());

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws InvalidServerModelException, InvalidPortException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.webserver.getPersistentFirewall());
		units.addAll(this.db.getPersistentFirewall());

		getNetworkModel().getServerModel(getLabel()).addEgress("www.rubygems.org:443");

		return units;
	}
}
