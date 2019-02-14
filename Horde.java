package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.model.ServerModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.FileEditUnit;
import core.unit.pkg.InstalledUnit;

public class Horde extends AStructuredProfile {
	
	private Nginx webserver;
	private PHP php;
	private MariaDB db;
	
	public Horde(ServerModel me, NetworkModel networkModel) {
		super("horde", me, networkModel);
		
		this.webserver = new Nginx(me, networkModel);
		this.php = new PHP(me, networkModel);
		this.db = new MariaDB(me, networkModel);
		
		this.db.setUsername("horde");
		this.db.setUserPrivileges("ALL");
		this.db.setUserPassword("${HORDE_PASSWORD}");
		this.db.setDb("horde");
	}

	protected Vector<IUnit> getInstalled() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(webserver.getInstalled());
		units.addAll(php.getInstalled());
		units.addAll(db.getInstalled());
		
		units.addElement(new InstalledUnit("php_pear", "proceed", "php-pear"));
		
		units.addElement(new SimpleUnit("horde_channel_discover", "php_pear_installed",
				"sudo pear channel-discover per.horde.org",
				"sudo pear channel-info pear.horde.org", "Unknown channel \"pear.horde.org\"", "fail"));
		
		units.addElement(new FileEditUnit("pear_base_dir", "horde_channel_discover", "/usr/share/php/htdocs", "/media/data/www", "/etc/pear/pear.conf"));
		
		units.addElement(new SimpleUnit("horde_installed", "pear_base_dir_edited",
				"sudo pear install horde/horde_role",
				"sudo pear list-all | grep horde", "", "fail"));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig() {
		Vector<IUnit> units =  new Vector<IUnit>();
		
		String nginxConf = "";
		nginxConf += "server {\n";
		nginxConf += "    listen *:80 default;\n";
		nginxConf += "    server_name _;\n";
		nginxConf += "    root /media/data/www;\n";
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
		nginxConf += "        fastcgi_pass unix:/var/run/php5-fpm.sock;\n";
		nginxConf += "        fastcgi_param SCRIPT_FILENAME  \\$document_root\\$fastcgi_script_name;\n";
		nginxConf += "        fastcgi_index index.php;\n";
		nginxConf += "        include fastcgi_params;\n";
		nginxConf += "    }\n";
		nginxConf += "    location ~/sites/default/files/civicrm/ConfigAndLog {\n";
		nginxConf += "        deny all;\n";
		nginxConf += "    }\n";
		nginxConf += "    location ~/sites/default/files/civicrm/custom/ {\n";
		nginxConf += "        deny all;\n";
		nginxConf += "    }\n";
		nginxConf += "    location ~/sites/default/files/civicrm/templates_c {\n";
		nginxConf += "        deny all;\n";
		nginxConf += "    }\n";
		nginxConf += "    location ~/sites/default/files/civicrm/upload {\n";
		nginxConf += "        deny all;\n";
		nginxConf += "    }\n";
		nginxConf += "    location ~ /\\.ht {\n";
		nginxConf += "        deny all;\n";
		nginxConf += "    }\n";
		nginxConf += "    include /media/data/nginx_custom_conf_d/default.conf;\n";
		nginxConf += "}";
		
		webserver.addLiveConfig("default", nginxConf);
		units.addAll(webserver.getPersistentConfig());
		units.addAll(db.getPersistentConfig());
		units.addAll(php.getPersistentConfig());
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(webserver.getLiveConfig());
		units.addAll(php.getLiveConfig());
		units.addAll(db.getLiveConfig());
		
		units.addElement(new SimpleUnit("horde_mysql_password", "proceed",
				"HORDE_PASSWORD=`grep \"password\" /media/data/www/sites/default/settings.php 2>/dev/null | grep -v \"[*#]\" | awk '{ print $3 }' | tr -d \"',\"`; [[ -z $HORDE_PASSWORD ]] && HORDE_PASSWORD=`openssl rand -hex 32`",
				"echo $HORDE_PASSWORD", "", "fail"));
		
		//Set up our database
		units.addAll(db.checkUserExists());
		units.addAll(db.checkDbExists());
		
		return units;
	}
	
	public Vector<IUnit> getNetworking() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(webserver.getNetworking());

		return units;
	}
}
