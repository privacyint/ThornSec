package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.model.ServerModel;
import core.profile.AStructuredProfile;
import core.unit.pkg.InstalledUnit;

public class Grav extends AStructuredProfile {

	private Nginx webserver;
	private PHP php;
	
	public Grav(ServerModel me, NetworkModel networkModel) {
		super("grav", me, networkModel);
		
		this.webserver = new Nginx(me, networkModel);
		this.php = new PHP(me, networkModel);
	}

	protected Vector<IUnit> getInstalled() {
		Vector<IUnit> units = new Vector<IUnit>();
				
		units.addAll(webserver.getInstalled());
		units.addAll(php.getInstalled());
		
		units.addElement(new InstalledUnit("php_cli", "php_fpm_installed", "php-cli"));
		units.addElement(new InstalledUnit("php_common", "php_fpm_installed", "php-common"));
		units.addElement(new InstalledUnit("php_curl", "php_fpm_installed", "php-curl"));
		units.addElement(new InstalledUnit("php_gd", "php_fpm_installed", "php-gd"));
		units.addElement(new InstalledUnit("php_json", "php_fpm_installed", "php-json"));
		units.addElement(new InstalledUnit("php_readline", "php_fpm_installed", "php-readline"));
		units.addElement(new InstalledUnit("php_mbstring", "php_fpm_installed", "php-mbstring"));
		units.addElement(new InstalledUnit("php_xml", "php_fpm_installed", "php-xml"));
		units.addElement(new InstalledUnit("php_zip", "php_fpm_installed", "php-zip"));

		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig() {
		Vector<IUnit> units =  new Vector<IUnit>();
		
		units.addAll(webserver.getPersistentConfig());
		units.addAll(php.getPersistentConfig());
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String nginxConf = "";
		nginxConf += "server {\n";
		nginxConf += "    listen 80;\n";
		nginxConf += "    server_name _;\n";
		nginxConf += "\n";
		nginxConf += "    root /media/data/www/grav/;\n";
        nginxConf += "    index  index.php;\n";
        nginxConf += "    try_files \\$uri \\$uri/ =404;\n";
		nginxConf += "\n";
		nginxConf += "    location / {\n";
		nginxConf += "        if (!-e \\$request_filename){ rewrite ^(.*)\\$ /index.php last; }\n";
        nginxConf += "    }\n";
        nginxConf += "\n";
        nginxConf += "    location /images/ {\n";
        nginxConf += "        #Serve images statically\n";
        nginxConf += "    }\n";
        nginxConf += "\n";
        nginxConf += "    location /user {\n";
        nginxConf += "        rewrite ^/user/accounts/(.*)\\$ /error redirect;\n";
        nginxConf += "        rewrite ^/user/config/(.*)\\$ /error redirect;\n";
        nginxConf += "        rewrite ^/user/(.*)\\.(txt|md|html|php|yaml|json|twig|sh|bat)\\$ /error redirect;\n";
        nginxConf += "     }\n";
        nginxConf += "\n";
        nginxConf += "    location /cache {\n";
        nginxConf += "        rewrite ^/cache/(.*) /error redirect;\n";
        nginxConf += "    }\n";
        nginxConf += "\n";
        nginxConf += "    location /bin {\n";
        nginxConf += "         rewrite ^/bin/(.*)\\$ /error redirect;\n";
        nginxConf += "    }\n";
        nginxConf += "\n";
        nginxConf += "    location /backup {\n";
        nginxConf += "        rewrite ^/backup/(.*) /error redirect;\n";
        nginxConf += "    }\n";
        nginxConf += "\n";
        nginxConf += "    location /system {\n";
        nginxConf += "        rewrite ^/system/(.*)\\.(txt|md|html|php|yaml|json|twig|sh|bat)\\$ /error redirect;\n";
        nginxConf += "    }\n";
        nginxConf += "\n";
        nginxConf += "    location /vendor {\n";
        nginxConf += "        rewrite ^/vendor/(.*)\\.(txt|md|html|php|yaml|json|twig|sh|bat)\\$ /error redirect;\n";
        nginxConf += "    }\n";
        nginxConf += "\n";
        nginxConf += "    location ~ \\.php\\$ {\n";
        nginxConf += "        try_files \\$uri =404;\n";
        nginxConf += "        fastcgi_split_path_info ^(.+\\.php)(/.+)\\$;\n";
        nginxConf += "        fastcgi_pass unix:" + php.getSockPath() + ";\n";
        nginxConf += "        fastcgi_index  index.php;\n";
        nginxConf += "        fastcgi_param  SCRIPT_FILENAME  \\$document_root\\$fastcgi_script_name;\n";
        nginxConf += "        include        fastcgi_params;\n";
        nginxConf += "    }\n";
        nginxConf += "\n";
        nginxConf += "    location ~ /\\.ht {\n";
        nginxConf += "        deny  all;\n";
		nginxConf += "    }\n";
		nginxConf += "    include /media/data/nginx_custom_conf_d/default.conf;\n";
		nginxConf += "}";
		
		webserver.addLiveConfig("default", nginxConf);
		
		units.addAll(webserver.getLiveConfig());
		units.addAll(php.getLiveConfig());
		
		return units;
	}
	
	public Vector<IUnit> getNetworking() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		me.addRequiredEgress("getgrav.com");
		
		units.addAll(webserver.getNetworking());

		return units;
	}
}
