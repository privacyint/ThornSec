package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.pkg.InstalledUnit;

public class MISP extends AStructuredProfile {
	
	Nginx webserver;
	PHP php;
	MariaDB db;
	
	String webBase;
	
	public MISP() {
		super("misp");
		
		this.webserver = new Nginx();
		this.php = new PHP();
		this.db = new MariaDB();
		this.webBase = "/media/data/www/MISP/";
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		//units.addElement(new SimpleUnit("nginx_user", "proceed",
		//		"sudo adduser nginx --system --shell=/bin/false --disabled-login --ingroup nginx",
		//		"id nginx 2>&1", "id: nginx: no such user", "fail"));
		
		units.addAll(webserver.getInstalled(server, model));
		units.addAll(php.getInstalled(server, model));
		units.addAll(db.getInstalled(server, model));
		
		//Hush your face, postfix config!
		units.addElement(new SimpleUnit("postfix_mailname", "proceed",
				"sudo debconf-set-selections <<< 'postfix postfix/mailname string " + model.getData().getDomain(server) + "'",
				"sudo debconf-show postfix | grep 'postfix/mailname:' || dpkg -l | grep '^.i' | grep 'postfix'", "", "fail"));
		units.addElement(new SimpleUnit("postfix_mailer_type", "postfix_mailname",
				"sudo debconf-set-selections <<< 'postfix postfix/main_mailer_type string \"Satellite system\"'",
				"sudo debconf-show postfix | grep 'postfix/main_mailer_type:' || dpkg -l | grep '^.i' | grep 'postfix'", "", "fail"));
		units.addElement(new InstalledUnit("postfix", "proceed", "postfix"));
		
		//Install Dependencies
		units.addElement(new InstalledUnit("curl", "proceed", "curl"));
		units.addElement(new InstalledUnit("gcc", "proceed", "gcc"));
		units.addElement(new InstalledUnit("git", "proceed", "git"));
		units.addElement(new InstalledUnit("gnupg_agent", "proceed", "gnupg-agent"));
		units.addElement(new InstalledUnit("make", "proceed", "make"));
		units.addElement(new InstalledUnit("python", "proceed", "python"));
		units.addElement(new InstalledUnit("openssl", "proceed", "openssl"));
		units.addElement(new InstalledUnit("redis_server", "proceed", "redis-server"));
		units.addElement(new InstalledUnit("zip", "proceed", "zip"));
		
		units.addAll(model.getServerModel(server).getBindFsModel().addBindPoint(server, model, "gnupg_home", "gnupg_agent_installed", "/media/metaldata/gpg", "/media/data/gpg", "nginx", "nginx", "0755"));
		
		//Install PHP dependencies
		units.addElement(new InstalledUnit("php5_cli", "php5_fpm_installed", "php5-cli"));
		units.addElement(new InstalledUnit("php_crypt_gpg", "php5_fpm_installed", "php-crypt-gpg"));
		units.addElement(new InstalledUnit("php5_dev", "php5_fpm_installed", "php5-dev"));
		units.addElement(new InstalledUnit("php5_json", "php5_fpm_installed", "php5-json"));
		units.addElement(new InstalledUnit("php5_xml", "php5_fpm_installed", "php5-xml"));
		units.addElement(new InstalledUnit("php5_readline", "php5_fpm_installed", "php5-readline"));
		units.addElement(new InstalledUnit("php5_redis", "php5_fpm_installed", "php5-redis"));

		//Install python dependencies
		units.addElement(new InstalledUnit("python_dev", "proceed", "python-dev"));
		units.addElement(new InstalledUnit("python_pip", "proceed", "python-pip"));
		units.addElement(new InstalledUnit("libxml2_dev", "proceed", "libxml2-dev"));
		units.addElement(new InstalledUnit("libxslt1_dev", "proceed", "libxslt1-dev"));
		units.addElement(new InstalledUnit("zlib1g_dev", "proceed", "zlib1g-dev"));
				
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units =  new Vector<IUnit>();
		
		String nginxConf = "";
		nginxConf += "server {\n";
		nginxConf += "    listen *:80 default;\n";
		nginxConf += "    server_name _;\n";
		nginxConf += "    root " + webBase + "app/webroot/;\n";
		nginxConf += "    index index.php;\n";
		nginxConf += "    sendfile off;\n";
		nginxConf += "    default_type text/plain;\n";
		nginxConf += "    server_tokens off;\n";
		nginxConf += "    location / {\n";
		nginxConf += "        try_files \\$uri @rewrite;\n";
		nginxConf += "    }\n";
		nginxConf += "    location @rewrite {\n";
		nginxConf += "        rewrite ^ /index.php;\n";
		nginxConf += "    }\n";
		nginxConf += "    error_page   500 502 503 504  /50x.html;\n";
		nginxConf += "    location = /50x.html {\n";
		nginxConf += "        root   /usr/share/nginx/html;\n";
		nginxConf += "    }\n";
		nginxConf += "    location ~ \\.php\\$ {\n";
		nginxConf += "        fastcgi_split_path_info ^(.+\\.php)(/.+)\\$;\n";
		nginxConf += "        fastcgi_pass unix:/var/run/php5-fpm.sock;\n";
		nginxConf += "        fastcgi_param SCRIPT_FILENAME  \\$document_root\\$fastcgi_script_name;\n";
		nginxConf += "        fastcgi_index index.php;\n";
		nginxConf += "        include fastcgi_params;\n";
		nginxConf += "    }\n";
		nginxConf += "}";
		
		webserver.addLiveConfig("default", nginxConf);
		units.addAll(webserver.getPersistentConfig(server, model));
		units.addAll(db.getPersistentConfig(server, model));
		units.addAll(php.getPersistentConfig(server, model));
		
		units.addAll(model.getServerModel(server).getBindFsModel().addBindPoint(server, model, "nginx", "nginx_installed", "/media/metaldata/www/MISP/app/tmp", "/media/data/www/MISP/app/tmp", "nginx", "nginx", "0770"));
		units.addAll(model.getServerModel(server).getBindFsModel().addBindPoint(server, model, "nginx", "nginx_installed", "/media/metaldata/www/MISP/app/files", "/media/data/www/MISP/app/files", "nginx", "nginx", "0770"));
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(webserver.getLiveConfig(server, model));
		units.addAll(php.getLiveConfig(server, model));
		units.addAll(db.getLiveConfig(server, model));
		
		units.addElement(new SimpleUnit("misp_cloned", "git_installed",
				"sudo -u nginx bash -c '"
						+ "cd /media/data/www;"
						+ "git clone https://github.com/MISP/MISP.git tmp;"
						+ "mv tmp/.git ./MISP && rm -rf tmp && cd MISP && git reset --hard;"
						+ "git checkout tags/$(git describe --tags `git rev-list --tags --max-count=1`);"
						+ "git config core.filemode false;"
				+ "'",
				"sudo [ -f " + webBase + "README.md ] && echo pass || echo fail", "pass", "pass"));
		
		units.addElement(new SimpleUnit("python_cybox_cloned", "git_installed",
				"sudo -u nginx bash -c '"
						+ "cd " + webBase + "app/files/scripts;"
						+ "git clone https://github.com/CybOXProject/python-cybox.git;"
						+ "cd python-cybox;"
						+ "git checkout v2.1.0.12;"
				+ "';"
				+ "cd " + webBase + "app/files/scripts/python-cybox;"
				+ "sudo python setup.py install;",
				"sudo [ -d " + webBase + "app/files/scripts/python-cybox ] && echo pass || echo fail", "pass", "pass"));

		units.addElement(new SimpleUnit("python_stix_cloned", "git_installed",
				"sudo -u nginx bash -c '"
						+ "cd " + webBase + "app/files/scripts;"
						+ "git clone https://github.com/STIXProject/python-stix.git;"
						+ "cd python-stix;"
						+ "git checkout v1.1.1.4;"
						+ "';"
				+ "cd " + webBase + "app/files/scripts/python-stix;"
				+ "sudo python setup.py install;",
				"sudo [ -d " + webBase + "app/files/scripts/python-stix ] && echo pass || echo fail", "pass", "pass"));
		
		units.addElement(new SimpleUnit("cake_cloned", "git_installed",
				"sudo -u nginx bash -c '"
						+ "cd " + webBase + ";"
						+ "git submodule init;"
						+ "git submodule update;"
						+ "cd app;"
						+ "php composer.phar require kamisama/cake-resque:4.1.2;"
						+ "php composer.phar config vendor-dir Vendor;"
						+ "php composer.phar install;"
				+ "'",
				"sudo [ -d " + webBase + "app/Plugin/CakeResque ] && echo pass || echo fail", "pass", "pass"));
		
		units.addElement(new SimpleUnit("scheduler_worker_enabled", "cake_cloned",
				"sudo -u nginx cp -fa " + webBase + "INSTALL/setup/config.php " + webBase + "app/Plugin/CakeResque/Config/config.php;",
				"sudo [ -f " + webBase + "app/Plugin/CakeResque/Config/config.php ] && echo pass || echo fail", "pass", "pass"));
		
		units.addElement(new SimpleUnit("misp_mysql_password", "proceed",
				"MISP_PASSWORD=`grep \"password\" " + webBase + "app/Config/database.php 2>/dev/null | grep -v \"[*#]\" | awk '{ print $3 }' | tr -d \"',\"`; [[ -z $MISP_PASSWORD ]] && MISP_PASSWORD=`openssl rand -hex 32`",
				"echo $MISP_PASSWORD", "", "fail"));
		
		units.addAll(db.createDb("misp", "misp", "ALL", "MISP_PASSWORD"));
		
		units.addElement(new SimpleUnit("misp_database_exists", "misp_mariadb_user_exists",
				"sudo -u nginx bash -c 'mysql -umisp -p${MISP_PASSWORD} < " + webBase + "INSTALL/MYSQL.sql;'",
				"sudo [ echo $(mysqlshow -umisp -p${MISP_PASSWORD} misp attributes 1>/dev/null 2>/dev/null) == 0 ] && echo pass || echo fail", "", "fail"));
		
		units.addElement(new SimpleUnit("bootstrap_config", "proceed",
				"sudo -u nginx bash -c 'cp -a " + webBase + "app/Config/bootstrap.default.php " + webBase + "app/Config/bootstrap.php;'",
				"sudo [ -f " + webBase + "app/Config/bootstrap.php ] && echo pass || echo fail;", "pass", "pass"));

		units.addElement(new SimpleUnit("database_config", "proceed",
				"sudo -u nginx cp -a " + webBase + "app/Config/database.default.php " + webBase + "app/Config/database.php;"
				+ "sudo sed -i \"s/db\\ login/misp/g\" " + webBase + "app/Config/database.php;"
				+ "sudo sed -i \"s/db\\ password/${MISP_PASSWORD}/g\" " + webBase + "app/Config/database.php;",
				"sudo [ -f " + webBase + "app/Config/database.php ] && echo pass || echo fail;", "pass", "pass"));

		units.addElement(new SimpleUnit("core_config", "proceed",
				"sudo -u nginx cp -a " + webBase + "app/Config/core.default.php " + webBase + "app/Config/core.php;",
				"sudo [ -f " + webBase + "app/Config/core.php ] && echo pass || echo fail;", "pass", "pass"));
		
		units.addElement(new SimpleUnit("config_config", "proceed",
				"sudo -u nginx cp -a " + webBase + "app/Config/config.default.php " + webBase + "app/Config/config.php;"
				+ "sudo sed -i \"s/'salt'       => ''/'salt'       => '`openssl rand -hex 64`'/g\" " + webBase + "app/Config/config.php;",
				"sudo [ -f " + webBase + "app/Config/config.php ] && echo pass || echo fail;", "pass", "pass"));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(webserver.getPersistentFirewall(server, model));

		return units;
	}

}