package profile;

import java.util.Iterator;
import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.pkg.InstalledUnit;

public class OpenSocial extends AStructuredProfile {
	
	Nginx webserver;
	PHP php;
	MariaDB db;
	
	public OpenSocial() {
		super("opensocial");
		
		this.webserver = new Nginx();
		this.php = new PHP();
		this.db = new MariaDB();
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(webserver.getInstalled(server, model));
		units.addAll(php.getInstalled(server, model));
		units.addAll(db.getInstalled(server, model));
		
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
		
		units.addAll(model.getServerModel(server).getBindFsModel().addBindPoint(server, model, "drush", "composer_installed", "/media/metaldata/drush", "/media/data/drush", "nginx", "nginx", "0755"));
		
		units.addElement(new SimpleUnit("drush_installed", "composer_installed",
				"sudo -u nginx bash -c 'composer create-project drush/drush /media/data/drush -n'",
				"[ -f /media/data/drush/drush ] && echo pass || echo fail", "pass", "pass",
				"Couldn't install drush. The installation of OpenSocial will fail."));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units =  new Vector<IUnit>();
		
		units.addAll(webserver.getPersistentConfig(server, model));
		units.addAll(db.getPersistentConfig(server, model));
		units.addAll(php.getPersistentConfig(server, model));
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
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
		nginxConf += "}";
		
		webserver.addLiveConfig("default", nginxConf);
		
		units.addAll(webserver.getLiveConfig(server, model));
		units.addAll(php.getLiveConfig(server, model));
		units.addAll(db.getLiveConfig(server, model));
		
		units.addElement(new SimpleUnit("opensocial_mysql_password", "proceed",
				"OPENSOCIAL_PASSWORD=`sudo grep \"password\" /media/data/www/html/sites/default/settings.php 2>/dev/null | grep -v \"[*#]\" | awk '{ print $3 }' | tr -d \"',\"`; [[ -z $OPENSOCIAL_PASSWORD ]] && OPENSOCIAL_PASSWORD=`openssl rand -hex 32`",
				"echo $OPENSOCIAL_PASSWORD", "", "fail",
				"Couldn't set a password for OpenSocial's database user. The installation will fail."));
		
		units.addAll(db.createDb("opensocial", "opensocial", "SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, INDEX, ALTER, CREATE TEMPORARY TABLES, LOCK TABLES", "OPENSOCIAL_PASSWORD"));
		
		units.addElement(new SimpleUnit("opensocial_installed", "drush_installed",
				"sudo composer create-project goalgorilla/social_template:dev-master /media/data/www --no-interaction"
				+ " && sudo /media/data/drush/drush -y -r /media/data/www/html site-install social --db-url=mysql://opensocial:${OPENSOCIAL_PASSWORD}@localhost:3306/opensocial --account-pass=admin",
				"sudo /media/data/drush/drush status -r /media/data/www/html 2>&1 | grep 'Install profile'", "", "fail",
				"OpenSocial could not be installed."));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(webserver.getPersistentFirewall(server, model));

		model.getServerModel(server).addRouterFirewallRule(server, model, "allow_packagist", "packagist.org", new String[]{"80","443"});
		model.getServerModel(server).addRouterFirewallRule(server, model, "allow_github", "github.com", new String[]{"80","443"});
		model.getServerModel(server).addRouterFirewallRule(server, model, "allow_drupal_packages", "packages.drupal.org", new String[]{"80","443"});
		model.getServerModel(server).addRouterFirewallRule(server, model, "allow_asset_packagist", "asset-packagist.org", new String[]{"80","443"});
		model.getServerModel(server).addRouterFirewallRule(server, model, "allow_github_api", "api.github.com", new String[]{"80","443"});
		model.getServerModel(server).addRouterFirewallRule(server, model, "allow_github_codeload", "codeload.github.com", new String[]{"80","443"});
		model.getServerModel(server).addRouterFirewallRule(server, model, "allow_drupal_git", "git.drupal.org", new String[]{"80","443"});
		
		return units;
	}

}