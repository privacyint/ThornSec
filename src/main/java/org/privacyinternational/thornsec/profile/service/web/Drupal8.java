/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.profile.service.web;

import java.util.ArrayList;
import java.util.Collection;
import org.privacyinternational.thornsec.core.exception.AThornSecException;
import org.privacyinternational.thornsec.core.exception.data.machine.InvalidServerException;
import org.privacyinternational.thornsec.core.exception.runtime.InvalidMachineModelException;
import org.privacyinternational.thornsec.core.iface.IUnit;
import org.privacyinternational.thornsec.core.model.machine.ServerModel;
import org.privacyinternational.thornsec.core.profile.AStructuredProfile;
import org.privacyinternational.thornsec.core.unit.SimpleUnit;
import org.privacyinternational.thornsec.core.unit.fs.FileEditUnit;
import org.privacyinternational.thornsec.core.unit.fs.FileUnit;
import org.privacyinternational.thornsec.core.unit.pkg.InstalledUnit;
import inet.ipaddr.HostName;
import org.privacyinternational.thornsec.profile.stack.LEMP;
import org.privacyinternational.thornsec.profile.stack.Nginx;
import org.privacyinternational.thornsec.profile.stack.PHP;

/**
 * This profile will install and configure Drupal 8
 */
public class Drupal8 extends AStructuredProfile {

	private final LEMP lempStack;

	public Drupal8(ServerModel me) {
		super(me);

		this.lempStack = new LEMP(me);
	}

	@Override
	public Collection<IUnit> getInstalled() throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.lempStack.getInstalled());

		units.add(new InstalledUnit("ca_certificates", "proceed", "ca-certificates"));
		units.add(new InstalledUnit("composer", "proceed", "composer"));
		units.add(new InstalledUnit("curl", "proceed", "curl"));
		units.add(new InstalledUnit("unzip", "proceed", "unzip"));

		units.add(new InstalledUnit("php_xml", "php_fpm_installed", "php-xml"));
		units.add(new InstalledUnit("php_gd", "php_fpm_installed", "php-gd"));
		units.add(new InstalledUnit("php_mysql", "php_fpm_installed", "php-mysql"));
		units.add(new InstalledUnit("php_common", "php_fpm_installed", "php-common"));
		units.add(new InstalledUnit("php_mod_curl", "php_fpm_installed", "php-curl"));
		units.add(new InstalledUnit("php_mbstring", "php_fpm_installed", "php-mbstring"));

		units.add(new SimpleUnit("drush_installed", "composer_installed",
				"sudo -u nginx bash -c 'composer create-project drush/drush /media/data/drush -n'",
				"sudo [ -f /media/data/drush/drush ] && echo pass || echo fail", "pass", "pass",
				"Couldn't install drush. The installation of Drupal will fail."));

		units.add(new SimpleUnit("drupal_installed", "drush_installed",
				"sudo /media/data/drush/drush -y dl drupal-8 --destination=/media/data --drupal-project-rename=www"
						+ " && sudo /media/data/drush/drush -y -r /media/data/www site-install --db-url=mysql://drupal:${DRUPAL_PASSWORD}@localhost:3306/drupal --account-pass=admin",
				"sudo /media/data/drush/drush status -r /media/data/www 2>&1 | grep 'Drupal root'", "", "fail",
				"Couldn't install Drupal."));

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentConfig() throws InvalidServerException, InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		this.lempStack.getDB().setUsername("drupal");
		this.lempStack.getDB().setUserPrivileges(
				"SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, INDEX, ALTER, CREATE TEMPORARY TABLES, LOCK TABLES");
		this.lempStack.getDB().setUserPassword("${DRUPAL_PASSWORD}");
		this.lempStack.getDB().setDb("drupal");

		final FileUnit nginxConf = new FileUnit("nginxConf", "nginx_installed",
				Nginx.CONF_D_DIRECTORY + "default.conf");
		nginxConf.appendLine("server {");
		nginxConf.appendLine("    listen *:80 default;");
		nginxConf.appendLine("    server_name _;");
		nginxConf.appendLine("    root /media/data/www;");
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
		nginxConf.appendLine("        fastcgi_pass unix:" + PHP.SOCK_PATH + ";");
		nginxConf.appendLine("        fastcgi_param SCRIPT_FILENAME  \\$document_root\\$fastcgi_script_name;");
		nginxConf.appendLine("        fastcgi_index index.php;");
		nginxConf.appendLine("        fastcgi_read_timeout 300;");
		nginxConf.appendLine("        include fastcgi_params;");
		nginxConf.appendLine("    }");
		nginxConf.appendLine("    location ~/sites/default/files/civicrm/ConfigAndLog {");
		nginxConf.appendLine("        deny all;");
		nginxConf.appendLine("    }");
		nginxConf.appendLine("    location ~/sites/default/files/civicrm/custom/ {");
		nginxConf.appendLine("        deny all;");
		nginxConf.appendLine("    }");
		nginxConf.appendLine("    location ~/sites/default/files/civicrm/templates_c {");
		nginxConf.appendLine("        deny all;");
		nginxConf.appendLine("    }");
		nginxConf.appendLine("    location ~/sites/default/files/civicrm/upload {");
		nginxConf.appendLine("        deny all;");
		nginxConf.appendLine("    }");
		nginxConf.appendLine("    location ~ /\\.ht {");
		nginxConf.appendLine("        deny all;");
		nginxConf.appendLine("    }");
		nginxConf.appendLine("    include /media/data/nginx_custom_conf_d/default.conf;");
		nginxConf.appendLine("}");

		this.lempStack.getWebserver().addLiveConfig(nginxConf);

		units.add(new FileEditUnit("drupal_base_url", "drupal_installed", "^\\# \\$base_url = 'http:",
				"\\$base_url = 'https:", "/media/data/www/sites/default/settings.php",
				"Couldn't set Drupal's URI to https. This could cause issues with hyperlinks in the installation."));

		units.add(new SimpleUnit("drupal_mysql_password", "proceed",
				"DRUPAL_PASSWORD=`sudo grep \"password\" /media/data/www/sites/default/settings.php 2>/dev/null | grep -v \"[*#]\" | awk '{ print $3 }' | tr -d \"',\"`; [[ -z $DRUPAL_PASSWORD ]] && DRUPAL_PASSWORD=`openssl rand -hex 32`",
				"echo $DRUPAL_PASSWORD", "", "fail",
				"Couldn't set a password for Drupal's database user. The installation will fail."));

		units.addAll(this.lempStack.getDB().checkUserExists());
		units.addAll(this.lempStack.getDB().checkDbExists());

		units.addAll(this.lempStack.getPersistentConfig());

		return units;
	}

	@Override
	public Collection<IUnit> getLiveConfig() throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.lempStack.getLiveConfig());

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		getMachineModel().addEgress(new HostName("drupal.org"));
		getMachineModel().addEgress(new HostName("packagist.org"));
		getMachineModel().addEgress(new HostName("api.github.com"));
		getMachineModel().addEgress(new HostName("github.com"));
		getMachineModel().addEgress(new HostName("codeload.github.com"));

		units.addAll(this.lempStack.getPersistentFirewall());

		return units;
	}

}
