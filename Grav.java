package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.pkg.InstalledUnit;

public class Grav extends AStructuredProfile {

	private Nginx webserver;
	private PHP php;
	
	public Grav() {
		super("grav");
		
		this.webserver = new Nginx();
		this.php = new PHP();
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
				
		units.addAll(webserver.getInstalled(server, model));
		units.addAll(php.getInstalled(server, model));
		
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
	
	protected Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units =  new Vector<IUnit>();
		
		units.addAll(webserver.getPersistentConfig(server, model));
		units.addAll(php.getPersistentConfig(server, model));
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
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
		nginxConf += "}";
		
		webserver.addLiveConfig("default", nginxConf);
		
		units.addAll(webserver.getLiveConfig(server, model));
		units.addAll(php.getLiveConfig(server, model));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		model.getServerModel(server).addRouterFirewallRule(server, model, "allow_grav", "getgrav.com", new String[]{"80","443"});
		units.addAll(webserver.getPersistentFirewall(server, model));

		return units;
	}

}