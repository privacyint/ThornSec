package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.model.ServerModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.pkg.InstalledUnit;

public class Drupal8 extends AStructuredProfile {
	
	private Nginx webserver;
	private PHP php;
	private MariaDB db;
	
	public Drupal8(ServerModel me, NetworkModel networkModel) {
		super("drupal8", me, networkModel);
		
		this.webserver = new Nginx(me, networkModel);
		this.php = new PHP(me, networkModel);
		this.db = new MariaDB(me, networkModel);
		
		this.db.setUsername("drupal");
		this.db.setUserPrivileges("SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, INDEX, ALTER, CREATE TEMPORARY TABLES, LOCK TABLES");
		this.db.setUserPassword("${DRUPAL_PASSWORD}");
		this.db.setDb("drupal");
	}

	protected Vector<IUnit> getInstalled() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(webserver.getInstalled());
		units.addAll(php.getInstalled());
		units.addAll(db.getInstalled());
		
		units.addElement(new InstalledUnit("ca_certificates", "proceed", "ca-certificates"));
		units.addElement(new InstalledUnit("curl", "proceed", "curl"));
		units.addElement(new InstalledUnit("unzip", "proceed", "unzip"));

		units.addElement(new InstalledUnit("composer", "proceed", "composer"));
		
		units.addElement(new InstalledUnit("php_xml", "php_fpm_installed", "php-xml"));
		units.addElement(new InstalledUnit("php_gd", "php_fpm_installed", "php-gd"));
		units.addElement(new InstalledUnit("php_mysql", "php_fpm_installed", "php-mysql"));
		units.addElement(new InstalledUnit("php_common", "php_fpm_installed", "php-common"));
		units.addElement(new InstalledUnit("php_mbstring", "php_fpm_installed", "php-mbstring"));
		units.addElement(new InstalledUnit("php_mod_curl", "php_fpm_installed", "php-curl"));
		
		units.addAll(((ServerModel)me).getBindFsModel().addDataBindPoint("drush", "composer_installed", "nginx", "nginx", "0750"));
		
		units.addElement(new SimpleUnit("drush_installed", "composer_installed",
				"sudo -u nginx bash -c 'composer create-project drush/drush /media/data/drush -n'",
				"sudo [ -f /media/data/drush/drush ] && echo pass || echo fail", "pass", "pass",
				"Couldn't install drush. The installation of Drupal will fail."));
		
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
		nginxConf += "    root /media/data/www;\n";
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
		nginxConf += "    location ~/sites/default/files/civicrm/ConfigAndLog {\n";
		nginxConf += "        deny all;\n";
		nginxConf += "    }\n";
		nginxConf += "    location ~/sites/default/files/civicrm/custom/ {\n";
		nginxConf += "        deny all;\n";
		nginxConf += "    }\n";
		nginxConf += "    location ~/sites/default/files/civicrm/templates_c {\n";
		nginxConf += "        deny all;\n";
		nginxConf += "    }\n";
		nginxConf += "    location ~/sites/default/files/civicrm/upload {\n";
		nginxConf += "        deny all;\n";
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
		
		units.addElement(new SimpleUnit("drupal_mysql_password", "proceed",
				"DRUPAL_PASSWORD=`sudo grep \"password\" /media/data/www/sites/default/settings.php 2>/dev/null | grep -v \"[*#]\" | awk '{ print $3 }' | tr -d \"',\"`; [[ -z $DRUPAL_PASSWORD ]] && DRUPAL_PASSWORD=`openssl rand -hex 32`",
				"echo $DRUPAL_PASSWORD", "", "fail",
				"Couldn't set a password for Drupal's database user. The installation will fail."));
		
		units.addAll(this.db.checkUserExists());
		units.addAll(this.db.checkDbExists());
		
		units.addElement(new SimpleUnit("drupal_installed", "drush_installed",
				"sudo /media/data/drush/drush -y dl drupal-8 --destination=/media/data --drupal-project-rename=www"
				+ " && sudo /media/data/drush/drush -y -r /media/data/www site-install --db-url=mysql://drupal:${DRUPAL_PASSWORD}@localhost:3306/drupal --db-prefix=drupal --account-pass=admin",
				"sudo /media/data/drush/drush status -r /media/data/www 2>&1 | grep 'Drupal root'", "", "fail",
				"Couldn't install Drupal."));
	
		return units;
	}
	
	public Vector<IUnit> getNetworking() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		me.addRequiredEgressDestination("drupal.org");
		me.addRequiredEgressDestination("packages.drupal.org");
		me.addRequiredEgressDestination("packagist.org");
		me.addRequiredEgressDestination("api.github.com");
		me.addRequiredEgressDestination("github.com");
		me.addRequiredEgressDestination("codeload.github.com");
		
		units.addAll(webserver.getNetworking());

		return units;
	}

}
