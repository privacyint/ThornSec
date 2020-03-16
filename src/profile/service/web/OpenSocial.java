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
import core.exception.data.machine.InvalidMachineException;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.InvalidMachineModelException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.network.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import profile.stack.LEMP;
import profile.stack.Nginx;
import profile.stack.PHP;

public class OpenSocial extends AStructuredProfile {

	private final LEMP lempStack;

	public OpenSocial(String label, NetworkModel networkModel) {
		super(label, networkModel);

		this.lempStack = new LEMP(getLabel(), networkModel);
	}

	@Override
	protected Collection<IUnit> getInstalled() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.lempStack.getInstalled());

		units.add(new InstalledUnit("ca_certificates", "proceed", "ca-certificates"));
		units.add(new InstalledUnit("composer", "proceed", "composer"));
		units.add(new InstalledUnit("php_xml", "php_fpm_installed", "php-xml"));
		units.add(new InstalledUnit("php_gd", "php_fpm_installed", "php-gd"));
		units.add(new InstalledUnit("php_mysql", "php_fpm_installed", "php-mysql"));
		units.add(new InstalledUnit("php_mbstring", "php_fpm_installed", "php-mbstring"));
		units.add(new InstalledUnit("php_common", "php_fpm_installed", "php-common"));
		units.add(new InstalledUnit("curl", "proceed", "curl"));
		units.add(new InstalledUnit("unzip", "proceed", "unzip"));
		units.add(new InstalledUnit("php_mod_curl", "php_fpm_installed", "php-curl"));

		units.addAll(getNetworkModel().getServerModel(getLabel()).getBindFsModel().addDataBindPoint("drush",
				"composer_installed", "nginx", "nginx", "0750"));

		units.add(new SimpleUnit("drush_installed", "composer_installed",
				"sudo -u nginx bash -c 'composer create-project drush/drush /media/data/drush -n'",
				"sudo [ -f /media/data/drush/drush ] && echo pass || echo fail", "pass", "pass",
				"Couldn't install drush. The installation of OpenSocial will fail."));

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentConfig() throws InvalidServerException, InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		this.lempStack.getDB().setUsername("opensocial");
		this.lempStack.getDB().setUserPrivileges(
				"SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, INDEX, ALTER, CREATE TEMPORARY TABLES, LOCK TABLES");
		this.lempStack.getDB().setUserPassword("${OPENSOCIAL_PASSWORD}");
		this.lempStack.getDB().setDb("opensocial");

		final FileUnit nginxConf = new FileUnit("opensocial_nginx_conf", "nginx_installed",
				Nginx.DEFAULT_CONFIG_FILE.toString());
		nginxConf.appendLine("server {");
		nginxConf.appendLine("    listen *:80 default;");
		nginxConf.appendLine("    server_name _;");
		nginxConf.appendLine("    root /media/data/www/html;");
		nginxConf.appendLine("    index index.php;");
		nginxConf.appendLine("    sendfile off;");
		nginxConf.appendLine("    default_type text/plain;");
		nginxConf.appendLine("    server_tokens off;");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("    client_max_body_size 0;");
		nginxConf.appendCarriageReturn();
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
		nginxConf.appendLine("        include fastcgi_params;");
		nginxConf.appendLine("    }");
		nginxConf.appendLine("    location ~ /\\.ht {");
		nginxConf.appendLine("        deny all;");
		nginxConf.appendLine("    }");
		nginxConf.appendLine("    include /media/data/nginx_custom_conf_d/default.conf;");
		nginxConf.appendLine("}");

		this.lempStack.getWebserver().addLiveConfig(nginxConf);

		units.add(new SimpleUnit("opensocial_mysql_password", "proceed",
				"OPENSOCIAL_PASSWORD=`sudo grep \"password\" /media/data/www/html/sites/default/settings.php 2>/dev/null | grep -v \"[*#]\" | awk '{ print $3 }' | tr -d \"',\"`; [[ -z $OPENSOCIAL_PASSWORD ]] && OPENSOCIAL_PASSWORD=`openssl rand -hex 32`",
				"echo $OPENSOCIAL_PASSWORD", "", "fail",
				"Couldn't set a password for OpenSocial's database user. The installation will fail."));

		units.add(new SimpleUnit("opensocial_salt", "proceed",
				"OPENSOCIAL_SALT=`sudo grep \"hash_salt'] =\" /media/data/www/html/sites/default/settings.php 2>/dev/null | grep -v \"[*#]\" | awk '{ print $3 }' | tr -d \"',;\"`; [[ -z $OPENSOCIAL_SALT ]] && OPENSOCIAL_SALT=`openssl rand -hex 75`",
				"echo $OPENSOCIAL_SALT", "", "fail",
				"Couldn't set a hash salt for OpenSocial's one-time login links. Your installation may not function correctly."));

		units.add(new SimpleUnit("opensocial_installed", "drush_installed",
				"sudo composer create-project goalgorilla/social_template:dev-master /media/data/www --no-interaction"
						+ " && sudo /media/data/drush/drush -y -r /media/data/www/html site-install social --db-url=mysql://opensocial:${OPENSOCIAL_PASSWORD}@localhost:3306/opensocial --account-pass=admin",
				"sudo /media/data/drush/drush status -r /media/data/www/html 2>&1 | grep 'Install profile'", "", "fail",
				"OpenSocial could not be installed."));

		units.addAll(this.lempStack.getPersistentConfig());

		return units;
	}

	@Override
	public Collection<IUnit> getLiveConfig() throws InvalidMachineException, InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.lempStack.getLiveConfig());

		// Set up our database
		final Collection<String> cnames = getNetworkModel().getData().getCNAMEs(getLabel());
		final String domain = getNetworkModel().getData().getDomain().replaceAll("\\.", "\\\\.");

		final FileUnit opensocialConf = new FileUnit("opensocial", "opensocial_installed",
				"/media/data/www/html/sites/default/settings.php");
		opensocialConf.appendLine("<?php");
		opensocialConf.appendCarriageReturn();
		opensocialConf.appendLine("\\$databases = array();");
		opensocialConf.appendLine("\\$config_directories = array();");
		opensocialConf.appendCarriageReturn();
		opensocialConf.appendLine("\\$settings['hash_salt'] = '${OPENSOCIAL_SALT}';");
		opensocialConf.appendLine("\\$settings['update_free_access'] = FALSE;");
		opensocialConf.appendCarriageReturn();
		opensocialConf
				.appendLine("\\$settings['container_yamls'][] = \\$app_root . '/' . \\$site_path . '/services.yml';");
		opensocialConf.appendCarriageReturn();
		opensocialConf.appendLine("\\$settings['trusted_host_patterns'] = array(");
		// TODO: fix this config file
		opensocialConf.appendLine("    '^" + getNetworkModel().getMachineModel(getLabel()) + "\\\\." + domain + "$',");

		for (final String cname : cnames) {
			opensocialConf.appendLine("    '^" + cname + "$',");
		}

		opensocialConf.appendLine(");");
		opensocialConf.appendCarriageReturn();
		opensocialConf
				.appendLine("\\$settings['container_yamls'][] = \\$app_root . '/' . \\$site_path . '/services.yml';");
		opensocialConf.appendCarriageReturn();
		opensocialConf.appendLine("\\$settings['file_scan_ignore_directories'] = [");
		opensocialConf.appendLine("    'node_modules',");
		opensocialConf.appendLine("    'bower_components',");
		opensocialConf.appendLine("];");
		opensocialConf.appendLine("\\$databases['default']['default'] = array (");
		opensocialConf.appendLine("    'database' => 'opensocial',");
		opensocialConf.appendLine("    'username' => 'opensocial',");
		opensocialConf.appendLine("    'password' => '${OPENSOCIAL_PASSWORD}',");
		opensocialConf.appendLine("    'prefix' => '',");
		opensocialConf.appendLine("    'host' => 'localhost',");
		opensocialConf.appendLine("    'port' => '3306',");
		opensocialConf.appendLine("    'namespace' => 'Drupal\\\\Core\\\\Database\\\\Driver\\\\mysql',");
		opensocialConf.appendLine("    'driver' => 'mysql',");
		opensocialConf.appendLine(");");
		opensocialConf.appendCarriageReturn();
		opensocialConf.appendLine("\\$settings['install_profile'] = 'social';");

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws InvalidServerModelException, InvalidPortException {
		final Collection<IUnit> units = new ArrayList<>();

		getNetworkModel().getServerModel(getLabel()).addEgress("packagist.org");
		getNetworkModel().getServerModel(getLabel()).addEgress("github.com");
		getNetworkModel().getServerModel(getLabel()).addEgress("packages.drupal.org");
		getNetworkModel().getServerModel(getLabel()).addEgress("asset-packagist.org");
		getNetworkModel().getServerModel(getLabel()).addEgress("api.github.com");
		getNetworkModel().getServerModel(getLabel()).addEgress("codeload.github.com");
		getNetworkModel().getServerModel(getLabel()).addEgress("git.drupal.org");

		units.addAll(this.lempStack.getPersistentFirewall());

		return units;
	}
}
