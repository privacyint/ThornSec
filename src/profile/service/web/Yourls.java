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
import profile.stack.LEMP;
import profile.stack.Nginx;
import profile.stack.PHP;

public class Yourls extends AStructuredProfile {

	private final LEMP lempStack;

	public Yourls(String label, NetworkModel networkModel) {
		super(label, networkModel);

		this.lempStack = new LEMP(getLabel(), networkModel);
	}

	@Override
	protected Collection<IUnit> getInstalled() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.lempStack.getInstalled());

		return units;
	}

	@Override
	protected Collection<IUnit> getPersistentConfig() throws InvalidServerException, InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		this.lempStack.getDB().setUsername("yourls");
		this.lempStack.getDB().setUserPrivileges("ALL");
		this.lempStack.getDB().setUserPassword("${YOURLS_PASSWORD}");
		this.lempStack.getDB().setDb("yourls");

		units.add(new GitCloneUnit("yourls", "proceed", "https://github.com/YOURLS/YOURLS.git", "/media/data/www",
				"Could not download Yourls. This is fatal."));

		final FileUnit nginxConf = new FileUnit("yourls_nginx_config", "nginx_installed",
				Nginx.DEFAULT_CONFIG_FILE.toString());
		nginxConf.appendLine("server {");
		nginxConf.appendLine("    listen *:80 default;");
		nginxConf.appendLine("    server_name _;");
		nginxConf.appendLine("    root /media/data/www;");
		nginxConf.appendLine("    index index.php;");
		nginxConf.appendLine("    sendfile off;");
		nginxConf.appendLine("    default_type text/plain;");
		nginxConf.appendLine("    server_tokens off;");
		nginxConf.appendLine("");
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

		units.add(new SimpleUnit("yourls_mysql_password", "proceed",
				"YOURLS_PASSWORD=`grep 'YOURLS_DB_PASS' /media/data/www/user/config.php 2>/dev/null | awk '{ print $2 }' | tr -d \"',);\");` [[ -z $YOURLS_PASSWORD ]] && YOURLS_PASSWORD=`openssl rand -hex 32`",
				"echo $YOURLS_PASSWORD", "", "fail",
				"Couldn't set a password for Yourl's database user. The installation will fail."));

		units.add(new SimpleUnit("yourl_cookie_salt", "proceed",
				"YOURLS_COOKIEKEY=`sudo grep 'YOURLS_COOKIEKEY' /media/data/www/user/config.php 2>/dev/null | grep -v \"[*#]\" | awk '{ print $3 }' | tr -d \"',;\"`; [[ -z $YOURLS_COOKIEKEY ]] && YOURLS_COOKIEKEY=`openssl rand -hex 75`",
				"echo $YOURLS_COOKIEKEY", "", "fail",
				"Couldn't set a cookie hash salt for Yourls. Your installation may not function correctly."));

		final FileUnit yourlsConfig = new FileUnit("yourls_config", "nginx_installed",
				"/media/data/www/user/config.php");
		units.add(yourlsConfig);
		yourlsConfig.appendLine("<?php");
		yourlsConfig.appendLine("	define('YOURLS_DB_USER', 'yourls');");
		yourlsConfig.appendLine("	define('YOURLS_DB_PASS', '$YOURLS_DB_PASS');");
		yourlsConfig.appendLine("	define('YOURLS_DB_NAME', 'yourls');");
		yourlsConfig.appendLine("	define('YOURLS_DB_HOST', 'localhost');");
		yourlsConfig.appendLine("	define('YOURLS_DB_PREFIX', 'yourls_');");
		yourlsConfig.appendLine(
				"	define('YOURLS_SITE', '" + getNetworkModel().getServerModel(getLabel()).getDomain() + "');");
		yourlsConfig.appendLine("	define('YOURLS_HOURS_OFFSET', 0);");
		yourlsConfig.appendLine("	define('YOURLS_LANG', '');");
		yourlsConfig.appendLine("	define('YOURLS_UNIQUE_URLS', false);");
		yourlsConfig.appendLine("	define('YOURLS_PRIVATE', true);");
		yourlsConfig.appendLine("	define('YOURLS_COOKIEKEY', '$YOURLS_COOKIEKEY');");
		yourlsConfig.appendLine("	$yourls_user_passwords = array(");
		yourlsConfig.appendLine("	        'admin' => 'password',");
		yourlsConfig.appendLine("	);");
		yourlsConfig.appendLine("	define('YOURLS_DEBUG', false);");
		yourlsConfig.appendLine("	define('YOURLS_URL_CONVERT', 36);");
		yourlsConfig.appendLine("	$yourls_reserved_URL = array(");
		// The below line was fun to write, as we are children...
		yourlsConfig.appendLine(
				"		'porn', 'fag', 'trannie', 'tranny', 'faggot', 'sex', 'nigger', 'fuck', 'cunt', 'dick', 'shit', 'spic', 'twat', 'pussy',");
		yourlsConfig.appendLine("	);");

		units.addAll(this.lempStack.getPersistentConfig());

		return units;
	}

	@Override
	protected Collection<IUnit> getLiveConfig() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.lempStack.getLiveConfig());

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws InvalidServerModelException, InvalidPortException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.lempStack.getPersistentFirewall());

		getNetworkModel().getServerModel(getLabel()).addEgress("www.github.com");

		return units;
	}

}