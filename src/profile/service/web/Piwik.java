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
import core.unit.fs.FileChecksumUnit;
import core.unit.fs.FileChecksumUnit.Checksum;
import core.unit.fs.FileDownloadUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import profile.stack.LEMP;
import profile.stack.Nginx;
import profile.stack.PHP;

public class Piwik extends AStructuredProfile {

	private final LEMP lempStack;

	public Piwik(String label, NetworkModel networkModel) {
		super(label, networkModel);

		this.lempStack = new LEMP(label, networkModel);
	}

	@Override
	protected Collection<IUnit> getInstalled() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.lempStack.getInstalled());

		units.add(new InstalledUnit("unzip", "proceed", "unzip"));
		units.add(new InstalledUnit("ca_certificates", "proceed", "ca-certificates"));
		units.add(new InstalledUnit("php_mysql", "php_fpm_installed", "php-mysql"));
		units.add(new InstalledUnit("php_mbstring", "php_fpm_installed", "php-mbstring"));
		units.add(new InstalledUnit("php_gd", "php_fpm_installed", "php-gd"));
		units.add(new InstalledUnit("php_xml", "php_fpm_installed", "php-xml"));

		units.add(new FileDownloadUnit("piwik", "proceed", "https://builds.matomo.org/piwik.zip", "/root/piwik.zip",
				"Couldn't download Piwik.  This could mean you ave no network connection, or that the specified download is no longer available."));
		units.add(new FileChecksumUnit("piwik", "piwik_downloaded", Checksum.SHA512, "/root/piwik.zip",
				"449a91225b0f942f454bbccd5fba1ff9ea9d0459b37f69004d43060c24e3626b6303373c66711b316314ec72ed96fda3c76b4a4f6a930c1569a2a72ed6ff6a1f",
				"Piwik's checksum doesn't match.  This could indicate a failed download, MITM attack, or a newer version than our code supports.  Piwik's installation will fail."));

		units.add(new SimpleUnit("piwik_installed", "piwik_checksum", "sudo unzip /root/piwik.zip -d /media/data/www",
				"[ -d /media/data/www/piwik ] && echo pass || echo fail", "pass", "pass",
				"Piwik couldn't be extracted to the required directory."));

		return units;
	}

	@Override
	protected Collection<IUnit> getPersistentConfig() throws InvalidServerException, InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		this.lempStack.getDB().setUsername("piwik");
		this.lempStack.getDB().setUserPrivileges(
				"SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, CREATE TEMPORARY TABLES, LOCK TABLES");
		this.lempStack.getDB().setUserPassword("${PIWIK_PASSWORD}");
		this.lempStack.getDB().setDb("piwik");

		units.add(new SimpleUnit("piwik_mysql_password", "piwik_installed",
				"PIWIK_PASSWORD=`sudo grep \"password\" /media/data/www/piwik/config/config.ini.php | head -1 | awk '{ print $3 }' | tr -d \"\\\",\"`; [[ -z $PIWIK_PASSWORD ]] && PIWIK_PASSWORD=`openssl rand -hex 32`;"
						+ "echo \"Your database password is ${PIWIK_PASSWORD}\"",
				"echo $PIWIK_PASSWORD", "", "fail",
				"Couldn't set the Piwik database user's password.  Piwik will be left in a broken state."));

		// Set up our database
		final FileUnit nginxConf = new FileUnit("piwik_nginx_conf", "piwik_installed",
				Nginx.DEFAULT_CONFIG_FILE.toString());

		nginxConf.appendLine("server {");
		nginxConf.appendLine("    listen *:80 default;");
		nginxConf.appendLine("    server_name _;");
		nginxConf.appendLine("    root /media/data/www/piwik;");
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
		nginxConf.appendLine("        include fastcgi_params;");
		nginxConf.appendLine("    }");
		nginxConf.appendLine("    location ~ /\\.ht {");
		nginxConf.appendLine("        deny all;");
		nginxConf.appendLine("    }");
		nginxConf.appendLine("}");

		this.lempStack.getWebserver().addLiveConfig(nginxConf);

		units.addAll(this.lempStack.getPersistentConfig());

		return units;
	}

	@Override
	public Collection<IUnit> getLiveConfig() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.lempStack.getLiveConfig());

		return units;
	}

	@Override
	public Collection<IUnit> getLiveFirewall() throws InvalidServerModelException, InvalidPortException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.lempStack.getLiveFirewall());

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws InvalidServerModelException, InvalidPortException {
		final Collection<IUnit> units = new ArrayList<>();

		getNetworkModel().getServerModel(getLabel()).addEgress("builds.matomo.org:443");

		units.addAll(this.lempStack.getPersistentFirewall());

		return units;
	}
}
