/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.service.web;

import java.util.HashSet;
import java.util.Set;

import core.exception.data.InvalidPortException;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.network.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import profile.stack.LEMP;
import profile.stack.Nginx;

/**
 * This profile configures a MISP (https://www.misp-project.org/) server
 *
 * @TODO: Does this even still work?
 */
public class MISP extends AStructuredProfile {

	private final LEMP lempStack;
	private final String webBase;

	public MISP(String label, NetworkModel networkModel) {
		super(label, networkModel);

		this.lempStack = new LEMP(label, networkModel);
		this.webBase = "/media/data/www/MISP/";

		this.lempStack.getDB().setUsername("misp");
		this.lempStack.getDB().setUserPrivileges("ALL");
		this.lempStack.getDB().setUserPassword("${MISP_PASSWORD}");
		this.lempStack.getDB().setDb("misp");
	}

	@Override
	protected Set<IUnit> getInstalled() throws InvalidServerModelException {
		final Set<IUnit> units = new HashSet<>();

		units.addAll(this.lempStack.getInstalled());

		// Hush your face, postfix config!
		// @TODO
		units.add(new SimpleUnit("postfix_mailname", "proceed",
				"sudo debconf-set-selections <<< 'postfix postfix/mailname string " + getLabel() + "'",
				"sudo debconf-show postfix | grep 'postfix/mailname:' || dpkg -l | grep '^.i' | grep 'postfix'", "",
				"fail"));
		units.add(new SimpleUnit("postfix_mailer_type", "postfix_mailname",
				"sudo debconf-set-selections <<< 'postfix postfix/main_mailer_type string \"Satellite system\"'",
				"sudo debconf-show postfix | grep 'postfix/main_mailer_type:' || dpkg -l | grep '^.i' | grep 'postfix'",
				"", "fail"));
		units.add(new InstalledUnit("postfix", "proceed", "postfix"));

		// Install Dependencies
		units.add(new InstalledUnit("curl", "proceed", "curl"));
		units.add(new InstalledUnit("gcc", "proceed", "gcc"));
		units.add(new InstalledUnit("git", "proceed", "git"));
		units.add(new InstalledUnit("gnupg_agent", "proceed", "gnupg-agent"));
		units.add(new InstalledUnit("make", "proceed", "make"));
		units.add(new InstalledUnit("python", "proceed", "python"));
		units.add(new InstalledUnit("openssl", "proceed", "openssl"));
		units.add(new InstalledUnit("redis_server", "proceed", "redis-server"));
		units.add(new InstalledUnit("zip", "proceed", "zip"));

		units.addAll(getNetworkModel().getServerModel(getLabel()).getBindFsModel().addBindPoint("gnupg_home",
				"gnupg_agent_installed", "/media/metaldata/gpg", "/media/data/gpg", "nginx", "nginx", "0750"));

		// Install PHP dependencies
		units.add(new InstalledUnit("php5_cli", "php5_fpm_installed", "php5-cli"));
		units.add(new InstalledUnit("php_crypt_gpg", "php5_fpm_installed", "php-crypt-gpg"));
		units.add(new InstalledUnit("php5_dev", "php5_fpm_installed", "php5-dev"));
		units.add(new InstalledUnit("php5_json", "php5_fpm_installed", "php5-json"));
		units.add(new InstalledUnit("php5_xml", "php5_fpm_installed", "php5-xml"));
		units.add(new InstalledUnit("php5_readline", "php5_fpm_installed", "php5-readline"));
		units.add(new InstalledUnit("php5_redis", "php5_fpm_installed", "php5-redis"));

		// Install python dependencies
		units.add(new InstalledUnit("python_dev", "proceed", "python-dev"));
		units.add(new InstalledUnit("python_pip", "proceed", "python-pip"));
		units.add(new InstalledUnit("libxml2_dev", "proceed", "libxml2-dev"));
		units.add(new InstalledUnit("libxslt1_dev", "proceed", "libxslt1-dev"));
		units.add(new InstalledUnit("zlib1g_dev", "proceed", "zlib1g-dev"));

		// Install MISP etc
		units.add(new SimpleUnit("misp_cloned", "git_installed",
				"sudo -u nginx bash -c '" + "cd /media/data/www;" + "git clone https://github.com/MISP/MISP.git tmp;"
						+ "mv tmp/.git ./MISP && rm -rf tmp && cd MISP && git reset --hard;"
						+ "git checkout tags/$(git describe --tags `git rev-list --tags --max-count=1`);"
						+ "git config core.filemode false;" + "'",
				"sudo [ -f " + this.webBase + "README.md ] && echo pass || echo fail", "pass", "pass"));

		units.add(new SimpleUnit("python_cybox_cloned", "git_installed",
				"sudo -u nginx bash -c '" + "cd " + this.webBase + "app/files/scripts;"
						+ "git clone https://github.com/CybOXProject/python-cybox.git;" + "cd python-cybox;"
						+ "git checkout v2.1.0.12;" + "';" + "cd " + this.webBase + "app/files/scripts/python-cybox;"
						+ "sudo python setup.py install;",
				"sudo [ -d " + this.webBase + "app/files/scripts/python-cybox ] && echo pass || echo fail", "pass",
				"pass"));

		units.add(new SimpleUnit("python_stix_cloned", "git_installed",
				"sudo -u nginx bash -c '" + "cd " + this.webBase + "app/files/scripts;"
						+ "git clone https://github.com/STIXProject/python-stix.git;" + "cd python-stix;"
						+ "git checkout v1.1.1.4;" + "';" + "cd " + this.webBase + "app/files/scripts/python-stix;"
						+ "sudo python setup.py install;",
				"sudo [ -d " + this.webBase + "app/files/scripts/python-stix ] && echo pass || echo fail", "pass",
				"pass"));

		units.add(new SimpleUnit("cake_cloned", "git_installed",
				"sudo -u nginx bash -c '" + "cd " + this.webBase + ";" + "git submodule init;" + "git submodule update;"
						+ "cd app;" + "php composer.phar require kamisama/cake-resque:4.1.2;"
						+ "php composer.phar config vendor-dir Vendor;" + "php composer.phar install;" + "'",
				"sudo [ -d " + this.webBase + "app/Plugin/CakeResque ] && echo pass || echo fail", "pass", "pass"));

		units.add(new SimpleUnit("scheduler_worker_enabled", "cake_cloned",
				"sudo -u nginx cp -fa " + this.webBase + "INSTALL/setup/config.php " + this.webBase
						+ "app/Plugin/CakeResque/Config/config.php;",
				"sudo [ -f " + this.webBase + "app/Plugin/CakeResque/Config/config.php ] && echo pass || echo fail",
				"pass", "pass"));

		return units;
	}

	@Override
	protected Set<IUnit> getPersistentConfig() throws InvalidServerException, InvalidServerModelException {
		final Set<IUnit> units = new HashSet<>();

		final FileUnit nginxConf = new FileUnit("misp_nginx_conf", "misp_installed",
				Nginx.DEFAULT_CONFIG_FILE.toString());
		nginxConf.appendLine("server {");
		nginxConf.appendLine("    listen *:80 default;");
		nginxConf.appendLine("    server_name _;");
		nginxConf.appendLine("    root " + this.webBase + "app/webroot/;");
		nginxConf.appendLine("    index index.php;");
		nginxConf.appendLine("    sendfile off;");
		nginxConf.appendLine("    default_type text/plain;");
		nginxConf.appendLine("    server_tokens off;");
		nginxConf.appendLine("    location / {");
		nginxConf.appendLine("        try_files \\$uri @rewrite;");
		nginxConf.appendLine("    }");
		nginxConf.appendLine("    location @rewrite {");
		nginxConf.appendLine("        rewrite ^ /index.php;");
		nginxConf.appendLine("    }");
		nginxConf.appendLine("    error_page   500 502 503 504  /50x.html;");
		nginxConf.appendLine("    location = /50x.html {");
		nginxConf.appendLine("        root   /usr/share/nginx/html;");
		nginxConf.appendLine("    }");
		nginxConf.appendLine("    location ~ \\.php\\$ {");
		nginxConf.appendLine("        fastcgi_split_path_info ^(.+\\.php)(/.+)\\$;");
		nginxConf.appendLine("        fastcgi_pass unix:/var/run/php5-fpm.sock;");
		nginxConf.appendLine("        fastcgi_param SCRIPT_FILENAME  \\$document_root\\$fastcgi_script_name;");
		nginxConf.appendLine("        fastcgi_index index.php;");
		nginxConf.appendLine("        include fastcgi_params;");
		nginxConf.appendLine("    }");
		nginxConf.appendLine("}");

		this.lempStack.getWebserver().addLiveConfig(nginxConf);

		units.addAll(this.lempStack.getPersistentConfig());

		units.addAll(
				getNetworkModel().getServerModel(getLabel()).getBindFsModel().addBindPoint("nginx", "nginx_installed",
						"/media/metaldata/www/MISP/app/tmp", "/media/data/www/MISP/app/tmp", "nginx", "nginx", "0770"));
		units.addAll(getNetworkModel().getServerModel(getLabel()).getBindFsModel().addBindPoint("nginx",
				"nginx_installed", "/media/metaldata/www/MISP/app/files", "/media/data/www/MISP/app/files", "nginx",
				"nginx", "0770"));

		return units;
	}

	@Override
	protected Set<IUnit> getLiveConfig() throws InvalidServerModelException {
		final Set<IUnit> units = new HashSet<>();

		units.addAll(this.lempStack.getLiveConfig());

		units.add(new SimpleUnit("misp_mysql_password", "proceed", "MISP_PASSWORD=`grep \"password\" " + this.webBase
				+ "app/Config/database.php 2>/dev/null | grep -v \"[*#]\" | awk '{ print $3 }' | tr -d \"',\"`; [[ -z $MISP_PASSWORD ]] && MISP_PASSWORD=`openssl rand -hex 32`",
				"echo $MISP_PASSWORD", "", "fail"));

		// Set up our database
		units.addAll(this.lempStack.getDB().checkUserExists());
		units.addAll(this.lempStack.getDB().checkDbExists());

		units.add(new SimpleUnit("misp_database_exists", "misp_mariadb_user_exists",
				"sudo -u nginx bash -c 'mysql -umisp -p${MISP_PASSWORD} < " + this.webBase + "INSTALL/MYSQL.sql;'",
				"sudo [ echo $(mysqlshow -umisp -p${MISP_PASSWORD} misp attributes 1>/dev/null 2>/dev/null) == 0 ] && echo pass || echo fail",
				"", "fail"));

		units.add(new SimpleUnit("bootstrap_config", "proceed",
				"sudo -u nginx bash -c 'cp -a " + this.webBase + "app/Config/bootstrap.default.php " + this.webBase
						+ "app/Config/bootstrap.php;'",
				"sudo [ -f " + this.webBase + "app/Config/bootstrap.php ] && echo pass || echo fail;", "pass", "pass"));

		units.add(new SimpleUnit("database_config", "proceed",
				"sudo -u nginx cp -a " + this.webBase + "app/Config/database.default.php " + this.webBase
						+ "app/Config/database.php;" + "sudo sed -i \"s/db\\ login/misp/g\" " + this.webBase
						+ "app/Config/database.php;" + "sudo sed -i \"s/db\\ password/${MISP_PASSWORD}/g\" "
						+ this.webBase + "app/Config/database.php;",
				"sudo [ -f " + this.webBase + "app/Config/database.php ] && echo pass || echo fail;", "pass", "pass"));

		units.add(new SimpleUnit("core_config", "proceed",
				"sudo -u nginx cp -a " + this.webBase + "app/Config/core.default.php " + this.webBase
						+ "app/Config/core.php;",
				"sudo [ -f " + this.webBase + "app/Config/core.php ] && echo pass || echo fail;", "pass", "pass"));

		units.add(new SimpleUnit("config_config", "proceed",
				"sudo -u nginx cp -a " + this.webBase + "app/Config/config.default.php " + this.webBase
						+ "app/Config/config.php;"
						+ "sudo sed -i \"s/'salt'       => ''/'salt'       => '`openssl rand -hex 64`'/g\" "
						+ this.webBase + "app/Config/config.php;",
				"sudo [ -f " + this.webBase + "app/Config/config.php ] && echo pass || echo fail;", "pass", "pass"));

		return units;
	}

	@Override
	public Set<IUnit> getPersistentFirewall() throws InvalidServerModelException, InvalidPortException {
		final Set<IUnit> units = new HashSet<>();

		units.addAll(this.lempStack.getPersistentFirewall());

		return units;
	}
}
