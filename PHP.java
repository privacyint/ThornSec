package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.model.ServerModel;
import core.profile.AStructuredProfile;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;

public class PHP extends AStructuredProfile {
	
	public PHP(ServerModel me, NetworkModel networkModel) {
		super("php_fpm", me, networkModel);
	}

	protected Vector<IUnit> getInstalled() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new InstalledUnit("php_fpm", "proceed", "php-fpm"));
		units.addElement(new InstalledUnit("php_base", "proceed", "php"));
		units.addElement(new InstalledUnit("php_apcu", "proceed", "php-apcu"));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig() {
		Vector<IUnit> units =  new Vector<IUnit>();
		
		String iniPath  = getConfigDirRoot() + "php.ini";
		String poolPath = getConfigDirRoot() + "pool.d/www.conf";
		
		String iniConf = "";
		iniConf += "[PHP]\n";
		iniConf += "engine = On\n";
		iniConf += "short_open_tag = Off\n";
		iniConf += "precision = 14\n";
		iniConf += "output_buffering = 4096\n";
		iniConf += "zlib.output_compression = Off\n";
		iniConf += "implicit_flush = Off\n";
		iniConf += "unserialize_callback_func =\n";
		iniConf += "serialize_precision = 17\n";
		iniConf += "disable_functions = pcntl_alarm,pcntl_fork,pcntl_waitpid,pcntl_wait,pcntl_wifexited,pcntl_wifstopped,pcntl_wifsignaled,pcntl_wifcontinued,pcntl_wexitstatus,pcntl_wtermsig,pcntl_wstopsig,pcntl_signal,pcntl_signal_dispatch,pcntl_get_last_error,pcntl_strerror,pcntl_sigprocmask,pcntl_sigwaitinfo,pcntl_sigtimedwait,pcntl_exec,pcntl_getpriority,pcntl_setpriority,\n";
		iniConf += "disable_classes =\n";
		iniConf += "zend.enable_gc = On\n";
		iniConf += "expose_php = Off\n";
		iniConf += "max_execution_time = 300\n";
		iniConf += "max_input_time = 60\n";
		iniConf += "memory_limit = 256M\n";
		iniConf += "error_reporting = E_ALL & ~E_DEPRECATED & ~E_STRICT\n";
		iniConf += "display_errors = Off\n";
		iniConf += "display_startup_errors = Off\n";
		iniConf += "log_errors = On\n";
		iniConf += "log_errors_max_len = 1024\n";
		iniConf += "ignore_repeated_errors = Off\n";
		iniConf += "ignore_repeated_source = Off\n";
		iniConf += "report_memleaks = On\n";
		iniConf += "track_errors = Off\n";
		iniConf += "html_errors = On\n";
		iniConf += "variables_order = \\\"GPCS\\\"\n";
		iniConf += "request_order = \\\"GP\\\"\n";
		iniConf += "register_argc_argv = Off\n";
		iniConf += "auto_globals_jit = On\n";
		iniConf += "post_max_size = 500M\n";
		iniConf += "auto_prepend_file =\n";
		iniConf += "auto_append_file =\n";
		iniConf += "default_mimetype = \\\"text/html\\\"\n";
		iniConf += "default_charset = \\\"UTF-8\\\"\n";
		iniConf += "doc_root =\n";
		iniConf += "user_dir =\n";
		iniConf += "enable_dl = Off\n";
		iniConf += "file_uploads = On\n";
		iniConf += "upload_max_filesize = 500M\n";
		iniConf += "max_file_uploads = 20\n";
		iniConf += "allow_url_fopen = On\n";
		iniConf += "allow_url_include = Off\n";
		iniConf += "default_socket_timeout = 60\n";
		iniConf += "emergency_restart_threshold 10\n"; //If 10 child processes exit...
		iniConf += "emergency_restart_interval 1m\n"; //...within a minute, restart PHP-FPM. 
		iniConf += "process_control_timeout 10s\n"; //Allow 10 seconds for our child procs to get a response
		iniConf += "\n";
		iniConf += "[CLI Server]\n";
		iniConf += "cli_server.color = On\n";
		iniConf += "\n";
		iniConf += "[Pdo_mysql]\n";
		iniConf += "pdo_mysql.cache_size = 2000\n";
		iniConf += "pdo_mysql.default_socket=\n";
		iniConf += "\n";
		iniConf += "[mail function]\n";
		iniConf += "SMTP = localhost\n";
		iniConf += "smtp_port = 25\n";
		iniConf += "mail.add_x_header = On\n";
		iniConf += "\n";
		iniConf += "[SQL]\n";
		iniConf += "sql.safe_mode = Off\n";
		iniConf += "\n";
		iniConf += "[ODBC]\n";
		iniConf += "odbc.allow_persistent = On\n";
		iniConf += "odbc.check_persistent = On\n";
		iniConf += "odbc.max_persistent = -1\n";
		iniConf += "odbc.max_links = -1\n";
		iniConf += "odbc.defaultlrl = 4096\n";
		iniConf += "odbc.defaultbinmode = 1\n";
		iniConf += "\n";
		iniConf += "[Interbase]\n";
		iniConf += "ibase.allow_persistent = 1\n";
		iniConf += "ibase.max_persistent = -1\n";
		iniConf += "ibase.max_links = -1\n";
		iniConf += "ibase.timestampformat = \\\"%Y-%m-%d %H:%M:%S\\\"\n";
		iniConf += "ibase.dateformat = \\\"%Y-%m-%d\\\"\n";
		iniConf += "ibase.timeformat = \\\"%H:%M:%S\\\"\n";
		iniConf += "\n";
		iniConf += "[MySQLi]\n";
		iniConf += "mysqli.max_persistent = -1\n";
		iniConf += "mysqli.allow_persistent = On\n";
		iniConf += "mysqli.max_links = -1\n";
		iniConf += "mysqli.cache_size = 2000\n";
		iniConf += "mysqli.default_port = 3306\n";
		iniConf += "mysqli.default_socket =\n";
		iniConf += "mysqli.default_host =\n";
		iniConf += "mysqli.default_user =\n";
		iniConf += "mysqli.default_pw =\n";
		iniConf += "mysqli.reconnect = Off\n";
		iniConf += "\n";
		iniConf += "[mysqlnd]\n";
		iniConf += "mysqlnd.collect_statistics = On\n";
		iniConf += "mysqlnd.collect_memory_statistics = Off\n";
		iniConf += "\n";
		iniConf += "[bcmath]\n";
		iniConf += "bcmath.scale = 0\n";
		iniConf += "\n";
		iniConf += "[Session]\n";
		iniConf += "session.save_handler = files\n";
		iniConf += "session.use_strict_mode = 0\n";
		iniConf += "session.use_cookies = 1\n";
		iniConf += "session.use_only_cookies = 1\n";
		iniConf += "session.name = PHPSESSID\n";
		iniConf += "session.auto_start = 0\n";
		iniConf += "session.cookie_lifetime = 0\n";
		iniConf += "session.cookie_path = /\n";
		iniConf += "session.cookie_domain =\n";
		iniConf += "session.cookie_httponly =\n";
		iniConf += "session.serialize_handler = php\n";
		iniConf += "session.gc_probability = 0\n";
		iniConf += "session.gc_divisor = 1000\n";
		iniConf += "session.gc_maxlifetime = 1440\n";
		iniConf += "session.referer_check =\n";
		iniConf += "session.cache_limiter = nocache\n";
		iniConf += "session.cache_expire = 180\n";
		iniConf += "session.use_trans_sid = 0\n";
		iniConf += "session.hash_function = 0\n";
		iniConf += "session.hash_bits_per_character = 5\n";
		iniConf += "url_rewriter.tags = \\\"a=href,area=href,frame=src,input=src,form=fakeentry\\\"\n";
		iniConf += "\n";
		iniConf += "[Assertion]\n";
		iniConf += "zend.assertions = -1\n";
		iniConf += "\n";
		iniConf += "[Tidy]\n";
		iniConf += "tidy.clean_output = Off\n";
		iniConf += "\n";
		iniConf += "[soap]\n";
		iniConf += "soap.wsdl_cache_enabled=1\n";
		iniConf += "soap.wsdl_cache_dir=\\\"/tmp\\\"\n";
		iniConf += "soap.wsdl_cache_ttl=86400\n";
		iniConf += "soap.wsdl_cache_limit = 5\n";
		iniConf += "\n";
		iniConf += "[ldap]\n";
		iniConf += "ldap.max_links = -1\n";
		iniConf += "\n";
		iniConf += "[OPcache]\n";
		iniConf += "opcache.enable=1\n";
		iniConf += "opcache.enable_cli=1\n";
		iniConf += "opcache.interned_strings_buffer=8\n";
		iniConf += "opcache.max_accelerated_files=10000\n";
		iniConf += "opcache.memory_consumption=128\n";
		iniConf += "opcache.save_comments=1\n";
		iniConf += "opcache.revalidate_freq=1";
		units.addElement(((ServerModel)me).getConfigsModel().addConfigFile("php_ini", "php_base_installed", iniConf, iniPath));

		//This is approximate, and very(!!!) generous(!!!) to give the box some breathing room.
		//On most VMs I have configured, it's usually below 40M.
		int mbRamPerProcess = 75;
		int serverTotalRam  = networkModel.getData().getRam(me.getLabel());
		
		int maxChildren     = serverTotalRam / mbRamPerProcess; 
		int minSpareServers = maxChildren / 3;
		int maxSpareServers = minSpareServers * 2;
		int startServers    = (minSpareServers + (maxSpareServers - minSpareServers)) / 2;
		
		String poolConf = "";
		poolConf += "[www]\n";
		poolConf += "user = nginx\n";
		poolConf += "group = nginx\n";
		poolConf += "listen = /run/php/php7.0-fpm.sock\n";
		poolConf += "listen.owner = nginx\n";
		poolConf += "listen.group = nginx\n";
		poolConf += "pm = dynamic\n";
		poolConf += "pm.max_children = " + maxChildren + "\n";
		poolConf += "pm.start_servers = " + startServers + "\n";
		poolConf += "pm.min_spare_servers = " + minSpareServers + "\n";
		poolConf += "pm.max_spare_servers = " + maxSpareServers + "\n";
		poolConf += "pm.max_requests = 200\n";
		poolConf += "env[PATH] = /usr/local/bin:/usr/bin:/bin\n";
		poolConf += "php_admin_value[max_execution_time] = 300";
		units.addElement(((ServerModel)me).getConfigsModel().addConfigFile("php_www_pool", "php_fpm_installed", poolConf, poolPath));

		return units;
	}

	protected Vector<IUnit> getLiveConfig() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new RunningUnit("php_fpm", "php7.0-fpm", "php7.0-fpm"));
		
		((ServerModel)me).getProcessModel().addProcess("php-fpm: master process \\(/etc/php/7.0/fpm/php-fpm\\.conf\\) *$");
		((ServerModel)me).getProcessModel().addProcess("php-fpm: pool www *$");
		
		return units;
	}
	
	public String getSockPath() {
		return "/var/run/php/php7.0-fpm.sock";
	}
	
	private String getConfigDirRoot() {
		return "/etc/php/7.0/fpm/";
	}
}
