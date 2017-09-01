package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.FileChecksumUnit;
import core.unit.fs.FileDownloadUnit;
import core.unit.fs.FileEditUnit;
import core.unit.pkg.InstalledUnit;

public class Owncloud extends AStructuredProfile {
	
	Nginx webserver;
	PHP php;
	MariaDB db;
	
	public Owncloud() {
		super("owncloud");
		
		this.webserver = new Nginx();
		this.php = new PHP();
		this.db = new MariaDB();
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(webserver.getInstalled(server, model));
		units.addAll(php.getInstalled(server, model));
		units.addAll(db.getInstalled(server, model));
		
		units.addAll(model.getServerModel(server).getBindFsModel().addBindPoint(server, model, "owncloud_data", "proceed", "/media/metaldata/owncloud", "/media/data/owncloud", "nginx", "nginx", "0770"));

		units.addElement(new InstalledUnit("unzip", "proceed", "unzip"));
		units.addElement(new InstalledUnit("ca_certificates", "proceed", "ca-certificates"));
		units.addElement(new InstalledUnit("php_gd", "php_fpm_installed", "php-gd"));
		units.addElement(new InstalledUnit("curl", "curl"));
		units.addElement(new InstalledUnit("php_mod_curl", "php_fpm_installed", "php-curl"));		
		units.addElement(new InstalledUnit("php_mysql", "mariadb_installed", "php-mysql"));
		units.addElement(new InstalledUnit("php_pdo", "mariadb_installed", "php-pdo"));
		units.addElement(new InstalledUnit("php_xml", "php_fpm_installed", "php-xml"));
		units.addElement(new InstalledUnit("php_unzip", "php_fpm_installed", "php-unzip"));
		units.addElement(new InstalledUnit("redis", "redis-server"));
		units.addElement(new InstalledUnit("php_redis", "php_fpm_installed", "php-redis"));
		
		model.getServerModel(server).getUserModel().addUsername("redis");

		units.addElement(new SimpleUnit("owncloud_mysql_password", "owncloud_checksum",
				"OWNCLOUD_PASSWORD=`sudo grep \"dbpassword\" /media/data/www/owncloud/config/config.php | head -1 | awk '{ print $3 }' | tr -d \"',\"`; [[ -z $OWNCLOUD_PASSWORD ]] && OWNCLOUD_PASSWORD=`openssl rand -hex 32`;",
				"sudo [ -f /media/data/www/owncloud/config/config.php ] && sudo grep \"dbpassword\" /media/data/www/owncloud/config/config.php | head -1 | awk '{ print $3 }' | tr -d \"',\"", "", "fail",
				"Couldn't set the OwnCloud database user's password.  OwnCloud will be left in a broken state."));

		units.addAll(db.createDb("owncloud", "owncloud", "ALL", "OWNCLOUD_PASSWORD"));

		units.addElement(new FileDownloadUnit("owncloud", "owncloud_data_mounted", "https://download.owncloud.org/community/owncloud-10.0.2.zip", "/root/owncloud.zip",
				"Couldn't download OwnCloud.  This could mean you have no network connection, or that the specified download is no longer available."));
		units.addElement(new FileChecksumUnit("owncloud", "owncloud_downloaded", "/root/owncloud.zip","de666286cd48210a53a3b019ef54741688747ed3fddf0a818d4d8647916831bc",
				"OwnCloud's checksum doesn't match.  This could indicate a failed download, MITM attack, or a newer version than our code supports.  OwnCloud's installation will fail."));

		units.addElement(new SimpleUnit("owncloud_unzipped", "owncloud_checksum",
				"sudo unzip /root/owncloud.zip -d /media/data/www/",
				"sudo [ -d /media/data/www/owncloud/l10n ] && echo pass", "pass", "pass",
				"OwnCloud couldn't be extracted to the required directory."));

		//Only bother to do this if we haven't already set up Owncloud...
		//units.addElement(new SimpleUnit("owncloud_admin_password", "owncloud_unzipped",
		//		"OWNCLOUD_ADMIN_PASSWORD=`openssl rand -hex 32`;"
		//		+ "echo 'Admin password:' ${OWNCLOUD_ADMIN_PASSWORD}",
		//		"sudo [ -f /media/data/www/owncloud/config/config.php ] && echo pass || echo $OWNCLOUD_ADMIN_PASSWORD", "", "fail"));
		
		units.addElement(new SimpleUnit("owncloud_installed", "owncloud_unzipped",
				"sudo -u nginx php /media/data/www/owncloud/occ maintenance:install"
						+ " --database \"mysql\""
						+ " --database-name \"owncloud\""
						+ " --database-user=\"owncloud\""
						+ " --database-pass \"${OWNCLOUD_PASSWORD}\""
						+ " --admin-user \"admin\""
						+ " --admin-pass \"admin\""
						+ " --data-dir \"/media/data/owncloud\";",
				"sudo [ -f /media/data/www/owncloud/version.php ] && echo pass", "pass", "pass",
				"OwnCloud could not be installed."));
		
		units.addElement(new FileEditUnit("owncloud_memcache", "owncloud_checksum", ");", "'memcache.local' => '\\OC\\Memcache\\APCu');", "/media/data/www/owncloud/config/config.php",
				"Couldn't set a memcache for OwnCloud.  This will affect performance, and will give you an error in the Admin Console."));
		
		units.addElement(new SimpleUnit("owncloud_up_to_date", "owncloud_installed",
				"sudo -u nginx php /media/data/www/owncloud/updater/application.php upgrade:start <<< '1';"
				+ "sudo -u nginx php /media/data/www/owncloud/occ upgrade --no-app-disable;",
				"sudo -u nginx php /media/data/www/owncloud/updater/application.php upgrade:detect -n | grep 'No updates found online'", "", "fail"));
		
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
		nginxConf += "upstream php-handler {\n";
		nginxConf += "    server unix:" + php.getSockPath() + ";\n";
		nginxConf += "}\n";
		nginxConf += "\n";
		nginxConf += "server {\n";
		nginxConf += "    listen 80;\n";
		nginxConf += "    server_name _;\n";
		nginxConf += "\n";
		nginxConf += "    root /media/data/www/owncloud/;\n";
		nginxConf += "\n";
		nginxConf += "    location = /robots.txt {\n";
		nginxConf += "        allow all;\n";
		nginxConf += "        log_not_found off;\n";
		nginxConf += "        access_log off;\n";
		nginxConf += "    }\n";
		nginxConf += "\n";
		nginxConf += "    location = /.well-known/carddav {\n";
		nginxConf += "        return 301 \\$scheme://\\$host/remote.php/dav;\n";
		nginxConf += "    }\n";
		nginxConf += "    location = /.well-known/caldav {\n";
		nginxConf += "        return 301 \\$scheme://\\$host/remote.php/dav;\n";
		nginxConf += "    }\n";
		nginxConf += "\n";
		nginxConf += "    location /.well-known/acme-challenge { }\n";
		nginxConf += "\n";
		nginxConf += "    client_max_body_size 512M;\n";
		nginxConf += "    fastcgi_buffers 64 4K;\n";
		nginxConf += "\n";
		nginxConf += "    gzip off;\n";
		nginxConf += "\n";
		nginxConf += "    error_page 403 /core/templates/403.php;\n";
		nginxConf += "    error_page 404 /core/templates/404.php;\n";
		nginxConf += "\n";
		nginxConf += "    location / {\n";
		nginxConf += "        rewrite ^ /index.php\\$uri;\n";
		nginxConf += "    }\n";
		nginxConf += "\n";
		nginxConf += "    location ~ ^/(?:build|tests|config|lib|3rdparty|templates|data)/ {\n";
		nginxConf += "        return 404;\n";
		nginxConf += "    }\n";
		nginxConf += "    location ~ ^/(?:\\.|autotest|occ|issue|indie|db_|console) {\n";
		nginxConf += "        return 404;\n";
		nginxConf += "    }\n";
		nginxConf += "\n";
		nginxConf += "    location ~ ^/(?:index|remote|public|cron|core/ajax/update|status|ocs/v[12]|updater/.+|ocs-provider/.+|core/templates/40[34])\\.php(?:\\$|/) {\n";
		nginxConf += "        fastcgi_split_path_info ^(.+\\.php)(/.*)\\$;\n";
		nginxConf += "        include fastcgi_params;\n";
		nginxConf += "        fastcgi_param SCRIPT_FILENAME \\$document_root\\$fastcgi_script_name;\n";
		nginxConf += "        fastcgi_param PATH_INFO \\$fastcgi_path_info;\n";
		nginxConf += "        fastcgi_param HTTPS on;\n";
		nginxConf += "        fastcgi_param modHeadersAvailable true;\n";
		nginxConf += "        fastcgi_param front_controller_active true;\n";
		nginxConf += "        fastcgi_pass php-handler;\n";
		nginxConf += "        fastcgi_intercept_errors on;\n";
		nginxConf += "        fastcgi_request_buffering off;\n";
		nginxConf += "    }\n";
		nginxConf += "\n";
		nginxConf += "    location ~ ^/(?:updater|ocs-provider)(?:\\$|/) {\n";
		nginxConf += "        try_files \\$uri \\$uri/ =404;\n";
		nginxConf += "        index index.php;\n";
		nginxConf += "    }\n";
		nginxConf += "\n";
		nginxConf += "    location ~* \\.(?:css|js)\\$ {\n";
		nginxConf += "        try_files \\$uri /index.php\\$uri\\$is_args\\$args;\n";
		nginxConf += "        access_log off;\n";
		nginxConf += "    }\n";
		nginxConf += "\n";
		nginxConf += "    location ~* \\.(?:svg|gif|png|html|ttf|woff|ico|jpg|jpeg)\\$ {\n";
		nginxConf += "        try_files \\$uri /index.php\\$uri\\$is_args\\$args;\n";
		nginxConf += "        access_log off;\n";
		nginxConf += "    }\n";
		nginxConf += "}";
		
		webserver.addLiveConfig("default", nginxConf);
		
		units.addAll(webserver.getLiveConfig(server, model));
		units.addAll(php.getLiveConfig(server, model));
		units.addAll(db.getLiveConfig(server, model));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(webserver.getPersistentFirewall(server, model));

		return units;
	}

}