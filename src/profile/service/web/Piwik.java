package profile.service.web;

import java.util.Vector;

import core.iface.IUnit;
import core.model.network.NetworkModel;

import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.FileChecksumUnit;
import core.unit.fs.FileDownloadUnit;
import core.unit.pkg.InstalledUnit;

public class Piwik extends AStructuredProfile {

	private Nginx webserver;
	private PHP php;
	private MariaDB db;
	
	public Piwik(String label, NetworkModel networkModel) {
		super("piwik", networkModel);
		
		this.webserver = new Nginx(getLabel(), networkModel);
		this.php = new PHP(getLabel(), networkModel);
		this.db = new MariaDB(getLabel(), networkModel);
		
		this.db.setUsername("piwik");
		this.db.setUserPrivileges("SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, CREATE TEMPORARY TABLES, LOCK TABLES");
		this.db.setUserPassword("${PIWIK_PASSWORD}");
		this.db.setDb("piwik");
	}

	protected Set<IUnit> getInstalled() {
		Set<IUnit> units = new HashSet<IUnit>();
				
		units.addAll(webserver.getInstalled());
		units.addAll(php.getInstalled());
		units.addAll(db.getInstalled());

		units.add(new InstalledUnit("unzip", "proceed", "unzip"));
		units.add(new InstalledUnit("ca_certificates", "proceed", "ca-certificates"));
		units.add(new InstalledUnit("php_mysql", "php_fpm_installed", "php-mysql"));
		units.add(new InstalledUnit("php_mbstring", "php_fpm_installed", "php-mbstring"));
		units.add(new InstalledUnit("php_gd", "php_fpm_installed", "php-gd"));
		units.add(new InstalledUnit("php_xml", "php_fpm_installed", "php-xml"));

		units.add(new FileDownloadUnit("piwik", "proceed", "https://builds.matomo.org/piwik.zip", "/root/piwik.zip",
				"Couldn't download Piwik.  This could mean you ave no network connection, or that the specified download is no longer available."));
		units.add(new FileChecksumUnit("piwik", "piwik_downloaded", "/root/piwik.zip", "449a91225b0f942f454bbccd5fba1ff9ea9d0459b37f69004d43060c24e3626b6303373c66711b316314ec72ed96fda3c76b4a4f6a930c1569a2a72ed6ff6a1f",
				"Piwik's checksum doesn't match.  This could indicate a failed download, MITM attack, or a newer version than our code supports.  Piwik's installation will fail."));

		units.add(new SimpleUnit("piwik_installed", "piwik_checksum",
				"sudo unzip /root/piwik.zip -d /media/data/www",
				"[ -d /media/data/www/piwik ] && echo pass || echo fail", "pass", "pass",
				"Piwik couldn't be extracted to the required directory."));
		
		return units;
	}
	
	protected Set<IUnit> getPersistentConfig() {
		Set<IUnit> units =  new HashSet<IUnit>();
		
		units.addAll(webserver.getPersistentConfig());
		units.addAll(php.getPersistentConfig());
		units.addAll(db.getPersistentConfig());
		
		return units;
	}

	protected Set<IUnit> getLiveConfig() {
		Set<IUnit> units = new HashSet<IUnit>();
		
		units.add(new SimpleUnit("piwik_mysql_password", "piwik_installed",
				"PIWIK_PASSWORD=`sudo grep \"password\" /media/data/www/piwik/config/config.ini.php | head -1 | awk '{ print $3 }' | tr -d \"\\\",\"`; [[ -z $PIWIK_PASSWORD ]] && PIWIK_PASSWORD=`openssl rand -hex 32`;"
				+ "echo \"Your database password is ${PIWIK_PASSWORD}\"",
				"echo $PIWIK_PASSWORD", "", "fail",
				"Couldn't set the Piwik database user's password.  Piwik will be left in a broken state."));
		
		//Set up our database
		units.addAll(db.checkUserExists());
		units.addAll(db.checkDbExists());
				
		String nginxConf = "";
		nginxConf += "server {\n";
		nginxConf += "    listen *:80 default;\n";
		nginxConf += "    server_name _;\n";
		nginxConf += "    root /media/data/www/piwik;\n";
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
		nginxConf += "    location ~ /\\.ht {\n";
		nginxConf += "        deny all;\n";
		nginxConf += "    }\n";
		nginxConf += "    include /media/data/nginx_custom_conf_d/default.conf;\n";
		nginxConf += "}";
		
		webserver.addLiveConfig("default", nginxConf);
		
		units.addAll(webserver.getLiveConfig());
		units.addAll(php.getLiveConfig());
		units.addAll(db.getLiveConfig());
		
		return units;
	}
	
	public Set<IUnit> getPersistentFirewall() {
		Set<IUnit> units = new HashSet<IUnit>();
		
		units.addAll(webserver.getPersistentFirewall());
		networkModel.getServerModel(getLabel()).addEgress("builds.matomo.org", new Integer[]{443});

		return units;
	}
}
