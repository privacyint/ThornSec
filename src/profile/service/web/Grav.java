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
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import profile.stack.Nginx;
import profile.stack.PHP;

/**
 * This profile builds and configures a grav (https://getgrav.org) instance.
 */
public class Grav extends AStructuredProfile {

	private final Nginx webserver;
	private final PHP php;

	public Grav(String label, NetworkModel networkModel) {
		super(label, networkModel);

		this.webserver = new Nginx(getLabel(), networkModel);
		this.php = new PHP(getLabel(), networkModel);
	}

	@Override
	protected Set<IUnit> getInstalled() throws InvalidServerModelException {
		final Set<IUnit> units = new HashSet<>();

		units.addAll(this.webserver.getInstalled());
		units.addAll(this.php.getInstalled());

		units.add(new InstalledUnit("php_cli", "php_fpm_installed", "php-cli"));
		units.add(new InstalledUnit("php_common", "php_fpm_installed", "php-common"));
		units.add(new InstalledUnit("php_curl", "php_fpm_installed", "php-curl"));
		units.add(new InstalledUnit("php_gd", "php_fpm_installed", "php-gd"));
		units.add(new InstalledUnit("php_json", "php_fpm_installed", "php-json"));
		units.add(new InstalledUnit("php_readline", "php_fpm_installed", "php-readline"));
		units.add(new InstalledUnit("php_mbstring", "php_fpm_installed", "php-mbstring"));
		units.add(new InstalledUnit("php_xml", "php_fpm_installed", "php-xml"));
		units.add(new InstalledUnit("php_zip", "php_fpm_installed", "php-zip"));

		return units;
	}

	@Override
	protected Set<IUnit> getPersistentConfig() throws InvalidServerException, InvalidServerModelException {
		final Set<IUnit> units = new HashSet<>();

		units.addAll(this.webserver.getPersistentConfig());
		units.addAll(this.php.getPersistentConfig());

		return units;
	}

	@Override
	protected Set<IUnit> getLiveConfig() throws InvalidServerModelException {
		final Set<IUnit> units = new HashSet<>();

		final FileUnit nginxConf = new FileUnit("nginx_conf", "nginx_installed", Nginx.DEFAULT_CONFIG_FILE.toString());
		nginxConf.appendLine("server {");
		nginxConf.appendLine("    listen 80;");
		nginxConf.appendLine("    server_name _;");
		nginxConf.appendLine("");
		nginxConf.appendLine("    root /media/data/www/grav/;");
		nginxConf.appendLine("    index  index.php;");
		nginxConf.appendLine("    try_files \\$uri \\$uri/ =404;");
		nginxConf.appendLine("");
		nginxConf.appendLine("    location / {");
		nginxConf.appendLine("        if (!-e \\$request_filename){ rewrite ^(.*)\\$ /index.php last; }");
		nginxConf.appendLine("    }");
		nginxConf.appendLine("");
		nginxConf.appendLine("    location /images/ {");
		nginxConf.appendLine("        #Serve images statically");
		nginxConf.appendLine("    }");
		nginxConf.appendLine("");
		nginxConf.appendLine("    location /user {");
		nginxConf.appendLine("        rewrite ^/user/accounts/(.*)\\$ /error redirect;");
		nginxConf.appendLine("        rewrite ^/user/config/(.*)\\$ /error redirect;");
		nginxConf.appendLine(
				"        rewrite ^/user/(.*)\\.(txt|md|html|php|yaml|json|twig|sh|bat)\\$ /error redirect;");
		nginxConf.appendLine("     }");
		nginxConf.appendLine("");
		nginxConf.appendLine("    location /cache {");
		nginxConf.appendLine("        rewrite ^/cache/(.*) /error redirect;");
		nginxConf.appendLine("    }");
		nginxConf.appendLine("");
		nginxConf.appendLine("    location /bin {");
		nginxConf.appendLine("         rewrite ^/bin/(.*)\\$ /error redirect;");
		nginxConf.appendLine("    }");
		nginxConf.appendLine("");
		nginxConf.appendLine("    location /backup {");
		nginxConf.appendLine("        rewrite ^/backup/(.*) /error redirect;");
		nginxConf.appendLine("    }");
		nginxConf.appendLine("");
		nginxConf.appendLine("    location /system {");
		nginxConf.appendLine(
				"        rewrite ^/system/(.*)\\.(txt|md|html|php|yaml|json|twig|sh|bat)\\$ /error redirect;");
		nginxConf.appendLine("    }");
		nginxConf.appendLine("");
		nginxConf.appendLine("    location /vendor {");
		nginxConf.appendLine(
				"        rewrite ^/vendor/(.*)\\.(txt|md|html|php|yaml|json|twig|sh|bat)\\$ /error redirect;");
		nginxConf.appendLine("    }");
		nginxConf.appendLine("");
		nginxConf.appendLine("    location ~ \\.php\\$ {");
		nginxConf.appendLine("        try_files \\$uri =404;");
		nginxConf.appendLine("        fastcgi_split_path_info ^(.+\\.php)(/.+)\\$;");
		nginxConf.appendLine("        fastcgi_pass unix:" + PHP.SOCK_PATH + ";");
		nginxConf.appendLine("        fastcgi_index  index.php;");
		nginxConf.appendLine("        fastcgi_param  SCRIPT_FILENAME  \\$document_root\\$fastcgi_script_name;");
		nginxConf.appendLine("        include        fastcgi_params;");
		nginxConf.appendLine("    }");
		nginxConf.appendLine("");
		nginxConf.appendLine("    location ~ /\\.ht {");
		nginxConf.appendLine("        deny  all;");
		nginxConf.appendLine("    }");
		nginxConf.appendLine("}");

		this.webserver.addLiveConfig(nginxConf);

		units.addAll(this.webserver.getLiveConfig());
		units.addAll(this.php.getLiveConfig());

		return units;
	}

	@Override
	public Set<IUnit> getPersistentFirewall() throws InvalidServerModelException, InvalidPortException {
		final Set<IUnit> units = new HashSet<>();

		getNetworkModel().getServerModel(getLabel()).addEgress("getgrav.com");

		units.addAll(this.webserver.getPersistentFirewall());

		return units;
	}
}
