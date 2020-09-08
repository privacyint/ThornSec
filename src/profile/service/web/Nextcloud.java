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
import core.exception.AThornSecException;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.InvalidMachineModelException;
import core.iface.IUnit;
import core.model.machine.ServerModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.CrontabUnit;
import core.unit.fs.FileChecksumUnit;
import core.unit.fs.FileChecksumUnit.Checksum;
import core.unit.fs.FileDownloadUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import inet.ipaddr.HostName;
import profile.stack.LEMP;
import profile.stack.Nginx;
import profile.stack.PHP;

/**
 * This profile creates and maintains a Nextcloud (https://nextcloud.com) Server
 */
public class Nextcloud extends AStructuredProfile {

	private final LEMP lempStack;

	public Nextcloud(ServerModel me) {
		super(me);

		this.lempStack = new LEMP(me);
	}

	@Override
	public Collection<IUnit> getInstalled() throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.lempStack.getInstalled());

		this.lempStack.getDB().setUsername("nextcloud");
		this.lempStack.getDB().setUserPrivileges("ALL");
		this.lempStack.getDB().setUserPassword("${NEXTCLOUD_PASSWORD}");
		this.lempStack.getDB().setDb("nextcloud");

		units.addAll(getNetworkModel().getServerModel(getLabel()).getBindFsModel().addDataBindPoint("nextcloud",
				"proceed", "nginx", "nginx", "0770"));

		units.add(new InstalledUnit("unzip", "proceed", "unzip"));
		units.add(new InstalledUnit("ca_certificates", "proceed", "ca-certificates"));
		units.add(new InstalledUnit("php_gd", "php_fpm_installed", "php-gd"));
		units.add(new InstalledUnit("curl", "proceed", "curl"));
		units.add(new InstalledUnit("php_mod_curl", "php_fpm_installed", "php-curl"));
		units.add(new InstalledUnit("php_mysql", "mariadb_installed", "php-mysql"));
		// units.add(new InstalledUnit("php_pdo", "mariadb_installed", "php-pdo"));
		units.add(new InstalledUnit("php_xml", "php_fpm_installed", "php-xml"));
		units.add(new InstalledUnit("php_zip", "php_fpm_installed", "php-zip"));
		units.add(new InstalledUnit("php_mbsgtring", "php_fpm_installed", "php-mbstring"));
		// units.add(new InstalledUnit("redis", "redis-server"));
		// units.add(new InstalledUnit("php_redis", "php_fpm_installed", "php-redis"));
		units.add(new InstalledUnit("php_intl", "php_fpm_installed", "php-intl"));

		units.add(new FileDownloadUnit("nextcloud", "nextcloud_data_mounted",
				"https://download.nextcloud.com/server/releases/latest.zip", "/root/nextcloud.zip",
				"Couldn't download NextCloud.  This could mean you have no network connection, or that the specified download is no longer available."));
		units.add(new FileChecksumUnit("nextcloud", "nextcloud_downloaded", Checksum.SHA512, "/root/nextcloud.zip",
				"$(curl -s https://download.nextcloud.com/server/releases/latest.zip.sha512 | awk '{print $1}')",
				"NextCloud's checksum doesn't match.  This could indicate a failed download, or a MITM attack.  NextCloud's installation will fail."));

		units.add(new SimpleUnit("nextcloud_mysql_password", "nextcloud_checksum",
				"NEXTCLOUD_PASSWORD=`sudo grep \"dbpassword\" /media/data/www/nextcloud/config/config.php | head -1 | awk '{ print $3 }' | tr -d \"',\"`; [[ -z $NEXTCLOUD_PASSWORD ]] && NEXTCLOUD_PASSWORD=`openssl rand -hex 32`;",
				"sudo [ -f /media/data/www/nextcloud/config/config.php ] && sudo grep \"dbpassword\" /media/data/www/nextcloud/config/config.php | head -1 | awk '{ print $3 }' | tr -d \"',\"",
				"", "fail",
				"Couldn't set the NextCloud database user's password of ${NEXTCLOUD_PASSWORD}.  NextCloud will be left in a broken state."));

		// Set up our database
		units.addAll(this.lempStack.getDB().checkUserExists());
		units.addAll(this.lempStack.getDB().checkDbExists());

		units.add(new SimpleUnit("nextcloud_unzipped", "nextcloud_checksum",
				"sudo unzip /root/nextcloud.zip -d /media/data/www/",
				"sudo [ -d /media/data/www/nextcloud/occ ] && echo pass", "pass", "pass",
				"NextCloud couldn't be extracted to the required directory."));

		// TODO: Fix up database for NC

		// Only bother to do this if we haven't already set up Owncloud...
		// units.add(new SimpleUnit("owncloud_admin_password", "owncloud_unzipped",
		// "OWNCLOUD_ADMIN_PASSWORD=`openssl rand -hex 32`;"
		// + "echo 'Admin password:' ${OWNCLOUD_ADMIN_PASSWORD}",
		// "sudo [ -f /media/data/www/owncloud/config/config.php ] && echo pass || echo
		// $OWNCLOUD_ADMIN_PASSWORD", "", "fail"));

		// units.add(new SimpleUnit("owncloud_installed", "owncloud_unzipped",
		// "sudo -u nginx php /media/data/www/owncloud/occ maintenance:install"
		// + " --database \"mysql\""
		// + " --database-name \"owncloud\""
		// + " --database-user=\"owncloud\""
		// + " --database-pass \"${OWNCLOUD_PASSWORD}\""
		// + " --admin-user \"admin\""
		// + " --admin-pass \"admin\""
		// + " --data-dir \"/media/data/owncloud\";",
		// "sudo [ -f /media/data/www/owncloud/version.php ] && echo pass", "pass",
		// "pass",
		// "OwnCloud could not be installed."));

//		units.add(new FileEditUnit("owncloud_memcache", "owncloud_checksum", ");", "'memcache.local' => '\\OC\\Memcache\\APCu');", "/media/data/www/owncloud/config/config.php",
		// "Couldn't set a memcache for OwnCloud. This will affect performance, and will
		// give you an error in the Admin Console."));

		// units.add(new SimpleUnit("owncloud_up_to_date", "owncloud_installed",
		// "sudo -u nginx php /media/data/www/owncloud/updater/application.php
		// upgrade:start <<< '1';"
		// + "sudo -u nginx php /media/data/www/owncloud/occ upgrade --no-app-disable;",
		// "sudo -u nginx php /media/data/www/owncloud/updater/application.php
		// upgrade:detect -n | grep 'No updates found online'", "", "fail"));

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentConfig() throws InvalidServerException, InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.lempStack.getPersistentConfig());

		units.add(new CrontabUnit("nextcloud", "nextcloud_unzipped", true, "nginx",
				"php -f /media/data/www/nextcloud/cron.php", "*", "*", "*", "*", "*/5",
				"Failed to get Nextcloud's cron job setup."));

		return units;
	}

	@Override
	public Collection<IUnit> getLiveConfig() throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		final FileUnit nginxConf = new FileUnit("nextcloud_nginx_conf", "nginx_installed",
				Nginx.DEFAULT_CONFIG_FILE.toString());

		nginxConf.appendLine("upstream php-handler {");
		nginxConf.appendLine("    server unix:" + PHP.SOCK_PATH + ";");
		nginxConf.appendLine("}");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("server {");
		nginxConf.appendLine("    listen 80;");
		nginxConf.appendLine("    server_name _;");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("    root /media/data/www/nextcloud/;");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("    location = /robots.txt {");
		nginxConf.appendLine("        allow all;");
		nginxConf.appendLine("        log_not_found off;");
		nginxConf.appendLine("        access_log off;");
		nginxConf.appendLine("    }");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("    location = /.well-known/carddav {");
		nginxConf.appendLine("        return 301 \\$scheme://\\$host/remote.php/dav;");
		nginxConf.appendLine("    }");
		nginxConf.appendLine("    location = /.well-known/caldav {");
		nginxConf.appendLine("        return 301 \\$scheme://\\$host/remote.php/dav;");
		nginxConf.appendLine("    }");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("    location /.well-known/acme-challenge { }");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("    client_max_body_size 512M;");
		nginxConf.appendLine("    fastcgi_buffers 64 4K;");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("    gzip off;");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("    error_page 403 /core/templates/403.php;");
		nginxConf.appendLine("    error_page 404 /core/templates/404.php;");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("    location / {");
		nginxConf.appendLine("        rewrite ^ /index.php\\$uri;");
		nginxConf.appendLine("    }");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("    location ~ ^/(?:build|tests|config|lib|3rdparty|templates|data)/ {");
		nginxConf.appendLine("        return 404;");
		nginxConf.appendLine("    }");
		nginxConf.appendLine("    location ~ ^/(?:\\.|autotest|occ|issue|indie|db_|console) {");
		nginxConf.appendLine("        return 404;");
		nginxConf.appendLine("    }");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine(
				"    location ~ ^/(?:index|remote|public|cron|core/ajax/update|status|ocs/v[12]|updater/.+|ocs-provider/.+|core/templates/40[34])\\.php(?:\\$|/) {");
		nginxConf.appendLine("        fastcgi_split_path_info ^(.+\\.php)(/.*)\\$;");
		nginxConf.appendLine("        include fastcgi_params;");
		nginxConf.appendLine("        fastcgi_param SCRIPT_FILENAME \\$document_root\\$fastcgi_script_name;");
		nginxConf.appendLine("        fastcgi_param PATH_INFO \\$fastcgi_path_info;");
		nginxConf.appendLine("        fastcgi_param HTTPS on;");
		nginxConf.appendLine("        fastcgi_param modHeadersAvailable true;");
		nginxConf.appendLine("        fastcgi_param front_controller_active true;");
		nginxConf.appendLine("        fastcgi_pass php-handler;");
		nginxConf.appendLine("        fastcgi_intercept_errors on;");
		nginxConf.appendLine("        fastcgi_request_buffering off;");
		nginxConf.appendLine("    }");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("    location ~ ^/(?:updater|ocs-provider)(?:\\$|/) {");
		nginxConf.appendLine("        try_files \\$uri \\$uri/ =404;");
		nginxConf.appendLine("        index index.php;");
		nginxConf.appendLine("    }");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("    location ~* \\.(?:css|js)\\$ {");
		nginxConf.appendLine("        try_files \\$uri /index.php\\$uri\\$is_args\\$args;");
		nginxConf.appendLine("        access_log off;");
		nginxConf.appendLine("    }");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("    location ~* \\.(?:svg|gif|png|html|ttf|woff|ico|jpg|jpeg)\\$ {");
		nginxConf.appendLine("        try_files \\$uri /index.php\\$uri\\$is_args\\$args;");
		nginxConf.appendLine("        access_log off;");
		nginxConf.appendLine("    }");
		nginxConf.appendLine("    include /media/data/nginx_custom_conf_d/default.conf;");
		nginxConf.appendLine("}");

		this.lempStack.getWebserver().addLiveConfig(nginxConf);

		units.add(new SimpleUnit("nextcloud_up_to_date", "nextcloud_unizipped",
				"sudo -u nginx php /media/data/www/nextcloud/updater/updater.phar --no-interaction",
				"sudo -u nginx php /media/data/www/nextcloud/updater/updater.phar | grep \"No update available\"",
				"No update available.", "pass"));

		getServerModel().getUserModel().addUsername("redis");

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		getMachineModel().addEgress(new HostName("nextcloud.com"));
		getMachineModel().addEgress(new HostName("apps.nextcloud.com"));
		getMachineModel().addEgress(new HostName("download.nextcloud.com"));
		getMachineModel().addEgress(new HostName("updates.nextcloud.com"));
		// It requires opening to the wider web anyway :(
		getMachineModel().addEgress(new HostName("github.com"));

		units.addAll(this.lempStack.getPersistentFirewall());

		return units;
	}
}
