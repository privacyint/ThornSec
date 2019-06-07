package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.model.ServerModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.CrontabUnit;
import core.unit.fs.FileChecksumUnit;
import core.unit.fs.FileDownloadUnit;
import core.unit.pkg.InstalledUnit;

public class Nextcloud extends AStructuredProfile {
	
	private Nginx webserver;
	private PHP php;
	private MariaDB db;
	
	public Nextcloud(ServerModel me, NetworkModel networkModel) {
		super("nextcloud", me, networkModel);
		
		this.webserver = new Nginx(me, networkModel);
		this.php = new PHP(me, networkModel);
		this.db = new MariaDB(me, networkModel);
		
		this.db.setUsername("nextcloud");
		this.db.setUserPrivileges("ALL");
		this.db.setUserPassword("${NEXTCLOUD_PASSWORD}");
		this.db.setDb("nextcloud");
	}

	protected Vector<IUnit> getInstalled() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(webserver.getInstalled());
		units.addAll(php.getInstalled());
		units.addAll(db.getInstalled());
		
		units.addAll(((ServerModel)me).getBindFsModel().addDataBindPoint("nextcloud", "proceed", "nginx", "nginx", "0770"));

		units.addElement(new InstalledUnit("unzip", "proceed", "unzip"));
		units.addElement(new InstalledUnit("ca_certificates", "proceed", "ca-certificates"));
		units.addElement(new InstalledUnit("php_gd", "php_fpm_installed", "php-gd"));
		units.addElement(new InstalledUnit("curl", "curl"));
		units.addElement(new InstalledUnit("php_mod_curl", "php_fpm_installed", "php-curl"));		
		units.addElement(new InstalledUnit("php_mysql", "mariadb_installed", "php-mysql"));
		//units.addElement(new InstalledUnit("php_pdo", "mariadb_installed", "php-pdo"));
		units.addElement(new InstalledUnit("php_xml", "php_fpm_installed", "php-xml"));
		units.addElement(new InstalledUnit("php_zip", "php_fpm_installed", "php-zip"));
		units.addElement(new InstalledUnit("php_mbsgtring", "php_fpm_installed", "php-mbstring"));
		//units.addElement(new InstalledUnit("redis", "redis-server"));
		//units.addElement(new InstalledUnit("php_redis", "php_fpm_installed", "php-redis"));
		units.addElement(new InstalledUnit("php_intl", "php_fpm_installed", "php-intl"));
		
		((ServerModel)me).getUserModel().addUsername("redis");

		units.addElement(new FileDownloadUnit("nextcloud", "nextcloud_data_mounted", "https://download.nextcloud.com/server/releases/latest.zip", "/root/nextcloud.zip",
				"Couldn't download NextCloud.  This could mean you have no network connection, or that the specified download is no longer available."));
		units.addElement(new FileChecksumUnit("nextcloud", "nextcloud_downloaded", "/root/nextcloud.zip",
				"$(curl -s https://download.nextcloud.com/server/releases/latest.zip.sha512 | awk '{print $1}')",
				"NextCloud's checksum doesn't match.  This could indicate a failed download, or a MITM attack.  NextCloud's installation will fail."));

		units.addElement(new SimpleUnit("nextcloud_mysql_password", "nextcloud_checksum",
				"NEXTCLOUD_PASSWORD=`sudo grep \"dbpassword\" /media/data/www/nextcloud/config/config.php | head -1 | awk '{ print $3 }' | tr -d \"',\"`; [[ -z $NEXTCLOUD_PASSWORD ]] && NEXTCLOUD_PASSWORD=`openssl rand -hex 32`;",
				"sudo [ -f /media/data/www/nextcloud/config/config.php ] && sudo grep \"dbpassword\" /media/data/www/nextcloud/config/config.php | head -1 | awk '{ print $3 }' | tr -d \"',\"", "", "fail",
				"Couldn't set the NextCloud database user's password of ${NEXTCLOUD_PASSWORD}.  NextCloud will be left in a broken state.") );

		//Set up our database
		units.addAll(db.checkUserExists());
		units.addAll(db.checkDbExists());
		
		units.addElement(new SimpleUnit("nextcloud_unzipped", "nextcloud_checksum",
				"sudo unzip /root/nextcloud.zip -d /media/data/www/",
				"sudo [ -d /media/data/www/nextcloud/occ ] && echo pass", "pass", "pass",
				"NextCloud couldn't be extracted to the required directory."));

		//Only bother to do this if we haven't already set up Owncloud...
		//units.addElement(new SimpleUnit("owncloud_admin_password", "owncloud_unzipped",
		//		"OWNCLOUD_ADMIN_PASSWORD=`openssl rand -hex 32`;"
		//		+ "echo 'Admin password:' ${OWNCLOUD_ADMIN_PASSWORD}",
		//		"sudo [ -f /media/data/www/owncloud/config/config.php ] && echo pass || echo $OWNCLOUD_ADMIN_PASSWORD", "", "fail"));
		
		//units.addElement(new SimpleUnit("owncloud_installed", "owncloud_unzipped",
		//		"sudo -u nginx php /media/data/www/owncloud/occ maintenance:install"
		//				+ " --database \"mysql\""
		//				+ " --database-name \"owncloud\""
		//				+ " --database-user=\"owncloud\""
		//				+ " --database-pass \"${OWNCLOUD_PASSWORD}\""
		//				+ " --admin-user \"admin\""
		//				+ " --admin-pass \"admin\""
		//				+ " --data-dir \"/media/data/owncloud\";",
		//		"sudo [ -f /media/data/www/owncloud/version.php ] && echo pass", "pass", "pass",
		//		"OwnCloud could not be installed."));
		
//		units.addElement(new FileEditUnit("owncloud_memcache", "owncloud_checksum", ");", "'memcache.local' => '\\OC\\Memcache\\APCu');", "/media/data/www/owncloud/config/config.php",
	//			"Couldn't set a memcache for OwnCloud.  This will affect performance, and will give you an error in the Admin Console."));
		
		//units.addElement(new SimpleUnit("owncloud_up_to_date", "owncloud_installed",
			//	"sudo -u nginx php /media/data/www/owncloud/updater/application.php upgrade:start <<< '1';"
				//+ "sudo -u nginx php /media/data/www/owncloud/occ upgrade --no-app-disable;",
			//	"sudo -u nginx php /media/data/www/owncloud/updater/application.php upgrade:detect -n | grep 'No updates found online'", "", "fail"));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig() {
		Vector<IUnit> units =  new Vector<IUnit>();
		
		units.addAll(webserver.getPersistentConfig());
		units.addAll(db.getPersistentConfig());
		units.addAll(php.getPersistentConfig());
		units.addElement(new CrontabUnit("nextcloud", "nextcloud_unzipped", true, "nginx", "php -f /media/data/www/nextcloud/cron.php", "*", "*", "*", "*", "*/5", "Failed to get Nextcloud's cron job setup."));

		return units;
	}

	protected Vector<IUnit> getLiveConfig() {
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
		nginxConf += "    root /media/data/www/nextcloud/;\n";
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
		nginxConf += "    include /media/data/nginx_custom_conf_d/default.conf;\n";
		nginxConf += "}";
		
		webserver.addLiveConfig("default", nginxConf);
		
		units.addAll(webserver.getLiveConfig());
		units.addAll(php.getLiveConfig());
		units.addAll(db.getLiveConfig());
		
		units.addElement(new SimpleUnit("nextcloud_up_to_date", "nextcloud_unizipped",
				"sudo -u nginx php updater/updater.phar --no-interaction",
				"sudo -u nginx php updater/updater.phar | grep \"No update available\"", "No update available.", "pass"));
		
		return units;
	}
	
	public Vector<IUnit> getNetworking() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		me.addRequiredEgress("nextcloud.com");
		me.addRequiredEgress("apps.nextcloud.com");
		me.addRequiredEgress("download.nextcloud.com");
		me.addRequiredEgress("updates.nextcloud.com");
		//It requires opening to the wider web anyway :(
		me.addRequiredEgress("github.com");

		units.addAll(webserver.getNetworking());

		return units;
	}
}
