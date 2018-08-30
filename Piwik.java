package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.FileChecksumUnit;
import core.unit.fs.FileDownloadUnit;
import core.unit.pkg.InstalledUnit;

public class Piwik extends AStructuredProfile {

	private Nginx webserver;
	private PHP php;
	private MariaDB db;
	
	public Piwik() {
		super("piwik");
		
		this.webserver = new Nginx();
		this.php = new PHP();
		this.db = new MariaDB();
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
				
		units.addAll(webserver.getInstalled(server, model));
		units.addAll(php.getInstalled(server, model));
		units.addAll(db.getInstalled(server, model));

		units.addElement(new InstalledUnit("unzip", "proceed", "unzip"));
		units.addElement(new InstalledUnit("ca_certificates", "proceed", "ca-certificates"));
		units.addElement(new InstalledUnit("php_mysql", "php_fpm_installed", "php-mysql"));
		units.addElement(new InstalledUnit("php_mbstring", "php_fpm_installed", "php-mbstring"));
		units.addElement(new InstalledUnit("php_gd", "php_fpm_installed", "php-gd"));
		units.addElement(new InstalledUnit("php_xml", "php_fpm_installed", "php-xml"));

		units.addElement(new FileDownloadUnit("piwik", "proceed", "https://builds.matomo.org/piwik.zip", "/root/piwik.zip",
				"Couldn't download Piwik.  This could mean you ave no network connection, or that the specified download is no longer available."));
		units.addElement(new FileChecksumUnit("piwik", "piwik_downloaded", "/root/piwik.zip", "449a91225b0f942f454bbccd5fba1ff9ea9d0459b37f69004d43060c24e3626b6303373c66711b316314ec72ed96fda3c76b4a4f6a930c1569a2a72ed6ff6a1f",
				"Piwik's checksum doesn't match.  This could indicate a failed download, MITM attack, or a newer version than our code supports.  Piwik's installation will fail."));

		units.addElement(new SimpleUnit("piwik_installed", "piwik_checksum",
				"sudo unzip /root/piwik.zip -d /media/data/www",
				"[ -d /media/data/www/piwik ] && echo pass || echo fail", "pass", "pass",
				"Piwik couldn't be extracted to the required directory."));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units =  new Vector<IUnit>();
		
		units.addAll(webserver.getPersistentConfig(server, model));
		units.addAll(php.getPersistentConfig(server, model));
		units.addAll(db.getPersistentConfig(server, model));
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new SimpleUnit("piwik_mysql_password", "piwik_installed",
				"PIWIK_PASSWORD=`sudo grep \"password\" /media/data/www/piwik/config/config.ini.php | head -1 | awk '{ print $3 }' | tr -d \"\\\",\"`; [[ -z $PIWIK_PASSWORD ]] && PIWIK_PASSWORD=`openssl rand -hex 32`;"
				+ "echo \"Your database password is ${PIWIK_PASSWORD}\"",
				"echo $PIWIK_PASSWORD", "", "fail",
				"Couldn't set the Piwik database user's password.  Piwik will be left in a broken state."));
		
		units.addAll(db.createDb("piwik", "piwik", "SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, CREATE TEMPORARY TABLES, LOCK TABLES", "PIWIK_PASSWORD"));
		
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
		model.getServerModel(server).addRouterEgressFirewallRule(server, model, "matomo", "builds.matomo.org", new String[]{"443"});

		return units;
	}

}