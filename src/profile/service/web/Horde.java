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
import core.unit.fs.FileEditUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import profile.stack.LEMP;
import profile.stack.Nginx;

/**
 * This profile installs and configures a Horde installation
 *
 * @TODO: either fix or kill
 */
public class Horde extends AStructuredProfile {

	private final LEMP lempStack;

	public Horde(String label, NetworkModel networkModel) {
		super(label, networkModel);

		this.lempStack = new LEMP(getLabel(), networkModel);
	}

	@Override
	protected Collection<IUnit> getInstalled() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.lempStack.getInstalled());

		this.lempStack.getDB().setUsername("horde");
		this.lempStack.getDB().setUserPrivileges("ALL");
		this.lempStack.getDB().setUserPassword("${HORDE_PASSWORD}");
		this.lempStack.getDB().setDb("horde");

		units.add(new InstalledUnit("php_pear", "proceed", "php-pear"));

		units.add(new SimpleUnit("horde_channel_discover", "php_pear_installed",
				"sudo pear channel-discover per.horde.org", "sudo pear channel-info pear.horde.org",
				"Unknown channel \"pear.horde.org\"", "fail"));

		units.add(new FileEditUnit("pear_base_dir", "horde_channel_discover", "/usr/share/php/htdocs",
				"/media/data/www", "/etc/pear/pear.conf"));

		units.add(new SimpleUnit("horde_installed", "pear_base_dir_edited", "sudo pear install horde/horde_role",
				"sudo pear list-all | grep horde", "", "fail"));

		return units;
	}

	@Override
	protected Collection<IUnit> getPersistentConfig() throws InvalidServerException, InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new SimpleUnit("horde_mysql_password", "proceed",
				"HORDE_PASSWORD=`grep \"password\" /media/data/www/sites/default/settings.php 2>/dev/null | grep -v \"[*#]\" | awk '{ print $3 }' | tr -d \"',\"`; [[ -z $HORDE_PASSWORD ]] && HORDE_PASSWORD=`openssl rand -hex 32`",
				"echo $HORDE_PASSWORD", "", "fail"));

		final FileUnit nginxConf = new FileUnit("horde_nginx_config", "nginx_installed",
				Nginx.DEFAULT_CONFIG_FILE.toString());

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
		nginxConf.appendLine("        fastcgi_pass unix:/var/run/php5-fpm.sock;");
		nginxConf.appendLine("        fastcgi_param SCRIPT_FILENAME  \\$document_root\\$fastcgi_script_name;");
		nginxConf.appendLine("        fastcgi_index index.php;");
		nginxConf.appendLine("        include fastcgi_params;");
		nginxConf.appendLine("    }");
		nginxConf.appendLine("}");

		this.lempStack.getWebserver().addLiveConfig(nginxConf);
		units.addAll(this.lempStack.getPersistentConfig());

		// Set up our database
		units.addAll(this.lempStack.getDB().checkUserExists());
		units.addAll(this.lempStack.getDB().checkDbExists());

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

		return units;
	}
}
