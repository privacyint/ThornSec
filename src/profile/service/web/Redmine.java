package profile.service.web;

import java.util.Vector;

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

public class Redmine extends AStructuredProfile {
	
	private Nginx webserver;
	private MariaDB db;
	
	public Redmine(String label, NetworkModel networkModel) {
		super("redmine", networkModel);
		
		this.webserver = new Nginx(getLabel(), networkModel);
		this.db = new MariaDB(getLabel(), networkModel);
		
		this.db.setUsername("redmine");
		this.db.setUserPrivileges("ALL");
		this.db.setUserPassword("${REDMINE_PASSWORD}");
		this.db.setDb("redmine");
	}

	protected Set<IUnit> getInstalled() {
		Set<IUnit> units = new HashSet<IUnit>();
				
		units.addAll(webserver.getInstalled());
		units.addAll(db.getInstalled());
				
		units.add(new InstalledUnit("redmine", "proceed", "redmine-mysql"));
		units.add(new InstalledUnit("thin", "redmine_installed", "thin"));
		units.add(new InstalledUnit("sendmail", "proceed", "sendmail"));
		
		networkModel.getServerModel(getLabel()).addProcessString("thin server \\(/var/run/thin/sockets/thin.[0-3].sock\\)$");
		
		return units;
	}
	
	protected Set<IUnit> getPersistentConfig() {
		Set<IUnit> units =  new HashSet<IUnit>();
		
		units.addAll(networkModel.getServerModel(getLabel()).getBindFsModel().addDataBindPoint("redmine_logs", "proceed", "www-data", "www-data", "0640"));
		units.addAll(networkModel.getServerModel(getLabel()).getBindFsModel().addDataBindPoint("redmine_files", "proceed", "www-data", "www-data", "0750"));
		units.addAll(networkModel.getServerModel(getLabel()).getBindFsModel().addDataBindPoint("redmine_data", "proceed", "www-data", "www-data", "0750"));

		units.add(new SimpleUnit("logs_symlinked", "redmine_installed",
				//We don't really care about logs at this point
				"sudo mv /var/log/redmine/* /media/data/redmine_logs;"
				//Then symlink
				+ "sudo rm -R /var/log/redmine;"
				+ "sudo ln -s /media/data/redmine_logs /var/log/redmine;",
				"sudo [ -L /var/log/redmine ] && echo pass || echo fail", "pass", "pass"));
		
		units.add(new SimpleUnit("files_symlinked", "redmine_installed",
				//Move over fresh installation if the files aren't already there
				"if [ ! -d /media/data/redmine_files/default ]; then sudo mv /var/lib/redmine/* /media/data/redmine_files ; fi ;"
				//Then symlink
				+ "sudo rm -R /var/lib/redmine ; sudo ln -s /media/data/redmine_files /var/lib/redmine;",
				"[ -L /var/lib/redmine ] && echo pass || echo fail", "pass", "pass"));

		units.add(new SimpleUnit("data_symlinked", "redmine_installed",
				//Move over fresh installation if the files aren't already there
				"if [ ! -d /media/data/redmine_data/config ] ; then sudo mv /usr/share/redmine/* /media/data/redmine_data ; fi ;"
				//Then symlink
				+ "sudo rm -R /usr/share/redmine ; sudo ln -s /media/data/redmine_data /usr/share/redmine;",
				"[ -L /usr/share/redmine ] && echo pass || echo fail", "pass", "pass"));

		units.add(new FileOwnUnit("database_config", "redmine_installed", "/etc/redmine/default/database.yml", "nginx"));
		units.add(new FileOwnUnit("secret_key", "redmine_installed", "/etc/redmine/default/secret_key.txt", "nginx"));
		
		units.add(new SimpleUnit("redmine_mysql_password", "proceed",
				"REDMINE_PASSWORD=`sudo grep \"password\" /usr/share/redmine/instances/default/config/database.yml 2>/dev/null | grep -v \"[*#]\" | awk '{ print $2 }'`; [[ -z $REDMINE_PASSWORD ]] && REDMINE_PASSWORD=`openssl rand -hex 32`",
				"echo $REDMINE_PASSWORD", "", "fail",
				"Couldn't set the Redmine database user's password.  Redmine will be left in a broken state."));
		
		//Set up our database
		units.addAll(db.checkUserExists());
		units.addAll(db.checkDbExists());

		String dbConfig = "";
		dbConfig += "production:\n";
		dbConfig += "  adapter: mysql2\n";
		dbConfig += "  database: redmine\n";
		dbConfig += "  host: localhost\n";
		dbConfig += "  username: redmine\n";
		dbConfig += "  password: ${REDMINE_PASSWORD}\n"; 
		dbConfig += "  encoding: utf8";
		
		units.add(new DirUnit("database_config_dir", "proceed", "/usr/share/redmine/instances/default/config/"));
		units.add(new FileUnit("database_config", "mariadb_installed", dbConfig, "/usr/share/redmine/instances/default/config/database.yml"));	
		
		units.add(new DirUnit("thin_pid_dir", "thin_installed", "/var/run/thin"));
		units.add(new DirOwnUnit("thin_pid_dir", "thin_pid_dir_created", "/var/run/thin", "www-data"));
		units.add(new DirPermsUnit("thin_pid_dir_perms", "thin_pid_dir_chowned", "/var/run/thin", "744"));
		
		units.add(new DirUnit("thin_sockets_dir", "thin_installed", "/var/run/thin/sockets"));
		units.add(new DirOwnUnit("thin_sockets_dir_permissions", "thin_sockets_dir_created", "/var/run/thin/sockets", "www-data"));
		
		String thinConfig = "";
		thinConfig += "---\n";
		thinConfig += "chdir: \"/usr/share/redmine\"\n";
		thinConfig += "environment: production\n";
		thinConfig += "timeout: 30\n";
		thinConfig += "log: \"/media/data/redmine_logs/redmine.log\"\n";
		thinConfig += "pid: \"/var/run/thin/redmine.pid\"\n";
		thinConfig += "max_conns: 1024\n";
		thinConfig += "max_persistent_conns: 100\n";
		thinConfig += "require: []\n";
		thinConfig += "wait: 30\n";
		thinConfig += "threadpool_size: 20\n";
		thinConfig += "socket: \"/var/run/thin/sockets/thin.sock\"\n";
		thinConfig += "daemonize: true\n";
		thinConfig += "user: www-data\n";
		thinConfig += "group: www-data\n";
		thinConfig += "servers: 4";
		
		units.add(new FileUnit("thin_config", "thin_installed", thinConfig, "/etc/thin2.3/redmine.yml"));
		
		units.add(new SimpleUnit("secret_key_base", "proceed",
				"SECRET_KEY_BASE=`grep \"secret_key_base:\" /usr/share/redmine/config/secrets.yml 2>/dev/null | awk '{ print $2 }'`; [[ -z $SECRET_KEY_BASE ]] && SECRET_KEY_BASE=`cd /usr/share/redmine; sudo rake secret`",
				"echo $SECRET_KEY_BASE", "", "fail"));
		
		String thinSecretKey = "";
		thinSecretKey += "production:\n";
		thinSecretKey += "  secret_key_base: ${SECRET_KEY_BASE}";
		
		units.add(new FileUnit("thin_secret_key", "thin_installed", thinSecretKey, "/usr/share/redmine/config/secrets.yml"));
		
		String emailConf = "";
		emailConf += "default:\n";
		emailConf += "  email_delivery:\n";
		emailConf += "    delivery_method: :sendmail";
		units.add(new FileUnit("redmine_allow_email", "sendmail_installed", emailConf, "/usr/share/redmine/config/secrets.yml"));

		String nginxConf = "";
		nginxConf += "upstream thin_cluster {\n";
		nginxConf += "    server unix:/var/run/thin/sockets/thin.0.sock;\n";
		nginxConf += "    server unix:/var/run/thin/sockets/thin.1.sock;\n";
		nginxConf += "    server unix:/var/run/thin/sockets/thin.2.sock;\n";
		nginxConf += "    server unix:/var/run/thin/sockets/thin.3.sock;\n";
		nginxConf += "}\n";
		nginxConf += "\n";
		nginxConf += "server {\n";
		nginxConf += "    listen *:80 default;\n"; 
		nginxConf += "    server_name _;\n";
		nginxConf += "\n";
		nginxConf += "    root /media/data/redmine_data/instances/default/public;\n";
		nginxConf += "\n";
		nginxConf += "    location ^~ /plugin_assets/ {\n";
		nginxConf += "        gzip_static on;\n";
		nginxConf += "        expires max;\n";
		nginxConf += "        add_header Cache-Control public;\n";
		nginxConf += "    }\n";
		nginxConf += "\n";
		nginxConf += "    try_files $uri/index.html $uri.html $uri @cluster;\n"; 
		nginxConf += "\n";
		nginxConf += "    location @cluster {\n"; 
		nginxConf += "        proxy_pass http://thin_cluster;\n"; 
		nginxConf += "    }\n";
		nginxConf += "    include /media/data/nginx_custom_conf_d/default.conf;\n";
		nginxConf += "}";
		
		webserver.addLiveConfig("default", nginxConf);
		
		units.add(new DirOwnUnit("redmine_cache", "redmine_installed", "/var/cache/redmine/default/tmp", "www-data"));
		units.add(new DirOwnUnit("redmine_cache_tmp", "redmine_installed", "/var/cache/redmine/default/tmp/cache", "www-data"));
		
		units.addAll(webserver.getPersistentConfig());
		units.addAll(db.getPersistentConfig());
		
		return units;
	}

	protected Set<IUnit> getLiveConfig() {
		Set<IUnit> units = new HashSet<IUnit>();
		
		units.addAll(webserver.getLiveConfig());
		units.addAll(db.getLiveConfig());
		
		return units;
	}
	
	public Set<IUnit> getPersistentFirewall() {
		Set<IUnit> units = new HashSet<IUnit>();
		
		units.addAll(webserver.getPersistentFirewall());
		units.addAll(db.getPersistentFirewall());
		
		networkModel.getServerModel(getLabel()).addEgress("rubygems.org", new Integer[] { 443 });
		
		return units;
	}
}
