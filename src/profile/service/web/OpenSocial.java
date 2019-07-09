package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.model.ServerModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.pkg.InstalledUnit;

public class OpenSocial extends AStructuredProfile {
	
	private Nginx webserver;
	private PHP php;
	private MariaDB db;
	
	public OpenSocial(ServerModel me, NetworkModel networkModel) {
		super("opensocial", me, networkModel);
		
		this.webserver = new Nginx(me, networkModel);
		this.php = new PHP(me, networkModel);
		this.db = new MariaDB(me, networkModel);
		
		this.db.setUsername("opensocial");
		this.db.setUserPrivileges("SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, INDEX, ALTER, CREATE TEMPORARY TABLES, LOCK TABLES");
		this.db.setUserPassword("${OPENSOCIAL_PASSWORD}");
		this.db.setDb("opensocial");
	}

	protected Vector<IUnit> getInstalled() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(webserver.getInstalled());
		units.addAll(php.getInstalled());
		units.addAll(db.getInstalled());
		
		units.addElement(new InstalledUnit("ca_certificates", "proceed", "ca-certificates"));
		units.addElement(new InstalledUnit("composer", "proceed", "composer"));
		units.addElement(new InstalledUnit("php_xml", "php_fpm_installed", "php-xml"));
		units.addElement(new InstalledUnit("php_gd", "php_fpm_installed", "php-gd"));
		units.addElement(new InstalledUnit("php_mysql", "php_fpm_installed", "php-mysql"));
		units.addElement(new InstalledUnit("php_mbstring", "php_fpm_installed", "php-mbstring"));
		units.addElement(new InstalledUnit("php_common", "php_fpm_installed", "php-common"));
		units.addElement(new InstalledUnit("curl", "proceed", "curl"));
		units.addElement(new InstalledUnit("unzip", "proceed", "unzip"));
		units.addElement(new InstalledUnit("php_mod_curl", "php_fpm_installed", "php-curl"));
		units.addElement(new InstalledUnit("sendmail", "proceed", "sendmail"));
		
		((ServerModel)me).getProcessModel().addProcess("sendmail: MTA: accepting connections$");
		((ServerModel)me).getUserModel().addUsername("smmta");
		((ServerModel)me).getUserModel().addUsername("smmpa");
		((ServerModel)me).getUserModel().addUsername("smmsp");
		
		units.addAll(((ServerModel)me).getBindFsModel().addDataBindPoint("drush", "composer_installed", "nginx", "nginx", "0750"));
		
		units.addElement(new SimpleUnit("drush_installed", "composer_installed",
				"sudo -u nginx bash -c 'composer create-project drush/drush /media/data/drush -n'",
				"sudo [ -f /media/data/drush/drush ] && echo pass || echo fail", "pass", "pass",
				"Couldn't install drush. The installation of OpenSocial will fail."));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig() {
		Vector<IUnit> units =  new Vector<IUnit>();
		
		units.addAll(webserver.getPersistentConfig());
		units.addAll(db.getPersistentConfig());
		units.addAll(php.getPersistentConfig());
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String nginxConf = "";
		nginxConf += "server {\n";
		nginxConf += "    listen *:80 default;\n";
		nginxConf += "    server_name _;\n";
		nginxConf += "    root /media/data/www/html;\n";
		nginxConf += "    index index.php;\n";
		nginxConf += "    sendfile off;\n";
		nginxConf += "    default_type text/plain;\n";
		nginxConf += "    server_tokens off;\n";
		nginxConf += "\n";
		nginxConf += "    client_max_body_size 0;\n";
		nginxConf += "\n";
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
		nginxConf += "        fastcgi_pass unix:" + php.getSockPath() + ";\n";
		nginxConf += "        fastcgi_param SCRIPT_FILENAME  \\$document_root\\$fastcgi_script_name;\n";
		nginxConf += "        fastcgi_index index.php;\n";
		nginxConf += "        include fastcgi_params;\n";
		nginxConf += "    }\n";
		nginxConf += "    location ~ /\\.ht {\n";
		nginxConf += "        deny all;\n";
		nginxConf += "    }\n";
		nginxConf += "    include /media/data/nginx_custom_conf_d/default.conf;\n";
		nginxConf += "}";
		
		webserver.addLiveConfig("default", nginxConf);
		
		units.addAll(webserver.getLiveConfig());
		units.addAll(php.getLiveConfig());
		units.addAll(db.getLiveConfig());
		
		units.addElement(new SimpleUnit("opensocial_mysql_password", "proceed",
				"OPENSOCIAL_PASSWORD=`sudo grep \"password\" /media/data/www/html/sites/default/settings.php 2>/dev/null | grep -v \"[*#]\" | awk '{ print $3 }' | tr -d \"',\"`; [[ -z $OPENSOCIAL_PASSWORD ]] && OPENSOCIAL_PASSWORD=`openssl rand -hex 32`",
				"echo $OPENSOCIAL_PASSWORD", "", "fail",
				"Couldn't set a password for OpenSocial's database user. The installation will fail."));

		units.addElement(new SimpleUnit("opensocial_salt", "proceed",
				"OPENSOCIAL_SALT=`sudo grep \"hash_salt'] =\" /media/data/www/html/sites/default/settings.php 2>/dev/null | grep -v \"[*#]\" | awk '{ print $3 }' | tr -d \"',;\"`; [[ -z $OPENSOCIAL_SALT ]] && OPENSOCIAL_SALT=`openssl rand -hex 75`",
				"echo $OPENSOCIAL_SALT", "", "fail",
				"Couldn't set a hash salt for OpenSocial's one-time login links. Your installation may not function correctly."));
		
		//Set up our database
		units.addAll(db.checkUserExists());
		units.addAll(db.checkDbExists());
				
		units.addElement(new SimpleUnit("opensocial_installed", "drush_installed",
				"sudo composer create-project goalgorilla/social_template:dev-master /media/data/www --no-interaction"
				+ " && sudo /media/data/drush/drush -y -r /media/data/www/html site-install social --db-url=mysql://opensocial:${OPENSOCIAL_PASSWORD}@localhost:3306/opensocial --account-pass=admin",
				"sudo /media/data/drush/drush status -r /media/data/www/html 2>&1 | grep 'Install profile'", "", "fail",
				"OpenSocial could not be installed."));

		String[] cnames = networkModel.getData().getCnames(me.getLabel());
		String   domain = networkModel.getData().getDomain(me.getLabel()).replaceAll("\\.", "\\\\.");  
		
		String opensocialConf = "";
		opensocialConf += "<?php\n";
		opensocialConf += "\n";
		opensocialConf += "\\$databases = array();\n";
		opensocialConf += "\\$config_directories = array();\n";
		opensocialConf += "\n";
		opensocialConf += "\\$settings['hash_salt'] = '${OPENSOCIAL_SALT}';";
		opensocialConf += "\\$settings['update_free_access'] = FALSE;\n";
		opensocialConf += "\n";
		opensocialConf += "\\$settings['container_yamls'][] = \\$app_root . '/' . \\$site_path . '/services.yml';\n";;
		opensocialConf += "\n";
		opensocialConf += "\\$settings['trusted_host_patterns'] = array(\n";
		opensocialConf += "    '^" + me.getHostname() + "\\\\." + domain + "$',\n";
		
		for (int i = 0; i < cnames.length; ++i) {
			opensocialConf += "    '^" + cnames[i].replaceAll("\\.", "\\\\.") + "." + domain + "$',\n";
		}
		
		opensocialConf += ");\n";
		opensocialConf += "\n";
		opensocialConf += "\\$settings['container_yamls'][] = \\$app_root . '/' . \\$site_path . '/services.yml';\n";
		opensocialConf += "\n";
		opensocialConf += "\\$settings['file_scan_ignore_directories'] = [\n"; 
		opensocialConf += "    'node_modules',\n"; 
		opensocialConf += "    'bower_components',\n";
		opensocialConf += "];\n";
		opensocialConf += "\\$databases['default']['default'] = array (\n"; 
		opensocialConf += "    'database' => 'opensocial',\n";
		opensocialConf += "    'username' => 'opensocial',\n";
		opensocialConf += "    'password' => '${OPENSOCIAL_PASSWORD}',\n";
		opensocialConf += "    'prefix' => '',\n"; 
		opensocialConf += "    'host' => 'localhost',\n"; 
		opensocialConf += "    'port' => '3306',\n"; 
		opensocialConf += "    'namespace' => 'Drupal\\\\Core\\\\Database\\\\Driver\\\\mysql',\n"; 
		opensocialConf += "    'driver' => 'mysql',\n";
		opensocialConf += ");\n"; 
		opensocialConf += "\n";
		opensocialConf += "\\$settings['install_profile'] = 'social';";

		units.addElement(((ServerModel)me).getConfigsModel().addConfigFile("opensocial", "opensocial_installed", opensocialConf, "/media/data/www/html/sites/default/settings.php"));
		
		return units;
	}
	
	public Vector<IUnit> getNetworking() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(webserver.getNetworking());

		me.addRequiredEgressDestination("packagist.org");
		me.addRequiredEgressDestination("github.com");
		me.addRequiredEgressDestination("packages.drupal.org");
		me.addRequiredEgressDestination("asset-packagist.org");
		me.addRequiredEgressDestination("api.github.com");
		me.addRequiredEgressDestination("codeload.github.com");
		me.addRequiredEgressDestination("git.drupal.org");
		
		return units;
	}
}
