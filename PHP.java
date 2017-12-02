package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.fs.FileEditUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;

public class PHP extends AStructuredProfile {
	
	public PHP() {
		super("php_fpm");
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new InstalledUnit("php_fpm", "proceed", "php-fpm"));
		units.addElement(new InstalledUnit("php_base", "proceed", "php"));
		units.addElement(new InstalledUnit("php_apcu", "proceed", "php-apcu"));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units =  new Vector<IUnit>();
		
		units.addElement(new FileEditUnit("php_version_header_off", "php_fpm_installed", "expose_php = On", "expose_php = Off", "/etc/php/7.0/fpm/php.ini",
				"Couldn't hide PHP's version.  This is potentially a minor security risk."));
		units.addElement(new FileEditUnit("php_nginx_listen_user", "php_fpm_installed", "listen.owner = www-data", "listen.owner = nginx", "/etc/php/7.0/fpm/pool.d/www.conf",
				"Couldn't get PHP-FPM listening as the webserver user.  This means your nginx will throw errors."));
		units.addElement(new FileEditUnit("php_nginx_listen_group", "php_fpm_installed", "listen.group = www-data", "listen.group = nginx", "/etc/php/7.0/fpm/pool.d/www.conf",
				"Couldn't get PHP-FPM listening as the webserver user.  This means your nginx will throw errors."));		
		units.addElement(new FileEditUnit("php_nginx_user", "php_fpm_installed", "user = www-data", "user = nginx", "/etc/php/7.0/fpm/pool.d/www.conf",
				"Couldn't get PHP-FPM listening as the webserver user.  This means your nginx will throw errors."));
		units.addElement(new FileEditUnit("php_nginx_group", "php_fpm_installed", "^group = www-data", "group = nginx", "/etc/php/7.0/fpm/pool.d/www.conf",
				"Couldn't get PHP-FPM listening as the webserver user.  This means your nginx will throw errors."));	
		units.addElement(new FileEditUnit("php_env_path", "php_fpm_installed", "^;env\\[PATH\\]", "env\\[PATH\\]", "/etc/php/7.0/fpm/pool.d/www.conf",
				"Couldn't set PHP's Path environment variable.  This may in some (very limited) instances cause some PHP software to act strangely."));
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new RunningUnit("php_fpm", "php7.0-fpm", "php7.0-fpm"));
		
		model.getServerModel(server).getProcessModel().addProcess("php-fpm: master process \\(/etc/php/7.0/fpm/php-fpm\\.conf\\) *$");
		model.getServerModel(server).getProcessModel().addProcess("php-fpm: pool www *$");
		
		return units;
	}
	
	public String getSockPath() {
		return "/var/run/php/php7.0-fpm.sock";
	}

}
