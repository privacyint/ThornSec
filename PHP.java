package profile;

import java.util.Vector;
import java.util.regex.Pattern;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.fs.FileAppendUnit;
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
		
		String phpIni  = getConfigDirRoot() + "php.ini";
		String wwwConf = getConfigDirRoot() + "pool.d/www.conf";
		
		//php.ini changes
		units.addElement(new FileEditUnit("php_version_header_off", "php_fpm_installed",
				"expose_php = On",
				"expose_php = Off",
				phpIni,
				"Couldn't hide PHP's version.  This is potentially a minor security risk."));
		units.addElement(new FileEditUnit("php_max_execution", "php_fpm_installed",
				"max_execution_time = 30$", "max_execution_time = 300",
				phpIni,
				"Couldn't increase PHP's Max Execution Time.  This could potentially cause some long-running scripts to time out, giving you errors."));
		units.addElement(new FileEditUnit("php_memory_limit", "php_fpm_installed",
				"memory_limit = 128M",
				"memory_limit = 256M",
				phpIni,
				"Couldn't increase PHP's RAM limit.  This could potentially cause some RAM-intensive scripts to time out, giving you errors."));	

		//pool.d/www.conf changes
		units.addElement(new FileEditUnit("php_nginx_listen_user", "php_fpm_installed",
				"listen.owner = www-data",
				"listen.owner = nginx",
				wwwConf,
				"Couldn't get PHP-FPM listening as the webserver user.  This means your nginx will throw errors."));
		units.addElement(new FileEditUnit("php_nginx_listen_group", "php_fpm_installed",
				"listen.group = www-data",
				"listen.group = nginx",
				wwwConf,
				"Couldn't get PHP-FPM listening as the webserver user.  This means your nginx will throw errors."));		
		units.addElement(new FileEditUnit("php_nginx_user", "php_fpm_installed",
				"user = www-data",
				"user = nginx",
				wwwConf,
				"Couldn't get PHP-FPM listening as the webserver user.  This means your nginx will throw errors."));
		units.addElement(new FileEditUnit("php_nginx_group", "php_fpm_installed",
				"^group = www-data",
				"group = nginx",
				wwwConf,
				"Couldn't get PHP-FPM listening as the webserver user.  This means your nginx will throw errors."));	
		units.addElement(new FileEditUnit("php_env_path", "php_fpm_installed",
				"^;env\\\\[PATH\\\\]",
				"env\\\\[PATH\\\\]",
				wwwConf,
				"Couldn't set PHP's Path environment variable.  This may in some (very limited) instances cause some PHP software to act strangely."));
		units.addElement(new FileEditUnit("php_fpm_max_execution", "php_fpm_installed",
				"^;php_admin_value\\[memory_limit\\] = 32M",
				"php_admin_value\\[max_execution_time\\] = 300",
				wwwConf));
		
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
	
	private String getConfigDirRoot() {
		return "/etc/php/7.0/fpm/";
	}

}
