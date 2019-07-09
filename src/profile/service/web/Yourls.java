package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.model.ServerModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.GitCloneUnit;

public class Yourls extends AStructuredProfile {
	
	private Nginx webserver;
	private PHP php;
	private MariaDB db;
	
	public Yourls(ServerModel me, NetworkModel networkModel) {
		super("yourls", me, networkModel);
		
		this.webserver = new Nginx(me, networkModel);
		this.php       = new PHP(me, networkModel);
		this.db        = new MariaDB(me, networkModel);
		
		this.db.setUsername("yourls");
		this.db.setUserPrivileges("ALL");
		this.db.setUserPassword("${YOURLS_PASSWORD}");
		this.db.setDb("yourls");
	}

	protected Vector<IUnit> getInstalled() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(webserver.getInstalled());
		units.addAll(php.getInstalled());
		units.addAll(db.getInstalled());
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig() {
		Vector<IUnit> units =  new Vector<IUnit>();
		
		units.addAll(webserver.getPersistentConfig());
		units.addAll(db.getPersistentConfig());
		units.addAll(php.getPersistentConfig());
		
		units.addElement(new GitCloneUnit("yourls", "proceed", "https://github.com/YOURLS/YOURLS.git", "/media/data/www", "Could not download Yourls. This is fatal."));
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String nginxConf = "";
		nginxConf += "server {\n";
		nginxConf += "    listen *:80 default;\n";
		nginxConf += "    server_name _;\n";
		nginxConf += "    root /media/data/www;\n";
		nginxConf += "    index index.php;\n";
		nginxConf += "    sendfile off;\n";
		nginxConf += "    default_type text/plain;\n";
		nginxConf += "    server_tokens off;\n";
		nginxConf += "\n";
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
		
		units.addElement(new SimpleUnit("yourls_mysql_password", "proceed",
				"YOURLS_PASSWORD=`grep 'YOURLS_DB_PASS' /media/data/www/user/config.php 2>/dev/null | awk '{ print $2 }' | tr -d \"',);\");` [[ -z $YOURLS_PASSWORD ]] && YOURLS_PASSWORD=`openssl rand -hex 32`",
				"echo $YOURLS_PASSWORD", "", "fail",
				"Couldn't set a password for Yourl's database user. The installation will fail."));

		units.addElement(new SimpleUnit("yourl_cookie_salt", "proceed",
				"YOURLS_COOKIEKEY=`sudo grep 'YOURLS_COOKIEKEY' /media/data/www/user/config.php 2>/dev/null | grep -v \"[*#]\" | awk '{ print $3 }' | tr -d \"',;\"`; [[ -z $YOURLS_COOKIEKEY ]] && YOURLS_COOKIEKEY=`openssl rand -hex 75`",
				"echo $YOURLS_COOKIEKEY", "", "fail",
				"Couldn't set a cookie hash salt for Yourls. Your installation may not function correctly."));
		
		//Set up our database
		units.addAll(db.checkUserExists());
		units.addAll(db.checkDbExists());
				
		String yourlsConfig = "";
		yourlsConfig += "<?php\n";
		yourlsConfig += "	define('YOURLS_DB_USER', 'yourls');\n";
		yourlsConfig += "	define('YOURLS_DB_PASS', '$YOURLS_DB_PASS');\n";
		yourlsConfig += "	define('YOURLS_DB_NAME', 'yourls');\n";
		yourlsConfig += "	define('YOURLS_DB_HOST', 'localhost');\n";
		yourlsConfig += "	define('YOURLS_DB_PREFIX', 'yourls_');\n";
		yourlsConfig += "	define('YOURLS_SITE', '" + networkModel.getData().getDomain(me.getLabel()) + "');\n";
		yourlsConfig += "	define('YOURLS_HOURS_OFFSET', 0);\n";
		yourlsConfig += "	define('YOURLS_LANG', '');\n";
		yourlsConfig += "	define('YOURLS_UNIQUE_URLS', false);\n";
		yourlsConfig += "	define('YOURLS_PRIVATE', true);\n";
		yourlsConfig += "	define('YOURLS_COOKIEKEY', '$YOURLS_COOKIEKEY');\n";
		yourlsConfig += "	$yourls_user_passwords = array(\n";
		yourlsConfig += "	        'admin' => 'password',\n";
		yourlsConfig += "	);\n";
		yourlsConfig += "	define('YOURLS_DEBUG', false);\n";
		yourlsConfig += "	define('YOURLS_URL_CONVERT', 36);\n";
		yourlsConfig += "	$yourls_reserved_URL = array(\n";
		//The below line was fun to write, as we are children...
		yourlsConfig += "		'porn', 'fag', 'trannie', 'tranny', 'faggot', 'sex', 'nigger', 'fuck', 'cunt', 'dick', 'shit', 'spic', 'twat', 'pussy',\n";
		yourlsConfig += "	);";

		units.addElement(((ServerModel)me).getConfigsModel().addConfigFile("opensocial", "opensocial_installed", yourlsConfig, "/media/data/www/user/config.php"));
		
		return units;
	}
	
	public Vector<IUnit> getNetworking() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(webserver.getNetworking());

		me.addRequiredEgressDestination("github.com");
		
		return units;
	}

}