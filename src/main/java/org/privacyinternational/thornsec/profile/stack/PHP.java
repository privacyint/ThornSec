/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.profile.stack;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.privacyinternational.thornsec.core.exception.data.machine.InvalidServerException;
import org.privacyinternational.thornsec.core.exception.runtime.InvalidMachineModelException;
import org.privacyinternational.thornsec.core.iface.IUnit;
import org.privacyinternational.thornsec.core.model.machine.ServerModel;
import org.privacyinternational.thornsec.core.model.machine.ServiceModel;
import org.privacyinternational.thornsec.core.profile.AStructuredProfile;
import org.privacyinternational.thornsec.core.unit.fs.FileUnit;
import org.privacyinternational.thornsec.core.unit.pkg.InstalledUnit;
import org.privacyinternational.thornsec.core.unit.pkg.RunningUnit;

/**
 * Create and configure PHP7.0-FPM for a given server
 */
public class PHP extends AStructuredProfile {
	public static final File SOCK_PATH = new File("/var/run/php/php7.0-fpm.sock");
	public static final File CONFIG_ROOT = new File("/etc/php/7.0/fpm/");

	public PHP(ServerModel me) {
		super(me);
	}

	@Override
	public Collection<IUnit> getInstalled() {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("php_fpm", "proceed", "php-fpm"));
		units.add(new InstalledUnit("php_base", "proceed", "php"));
		units.add(new InstalledUnit("php_apcu", "proceed", "php-apcu"));

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentConfig() throws InvalidServerException {
		final Collection<IUnit> units = new ArrayList<>();

		final FileUnit iniConf = new FileUnit("php_ini", "php_fpm_installed", CONFIG_ROOT + "php.ini");
		final FileUnit poolConf = new FileUnit("php_pool", "php_fpm_installed", CONFIG_ROOT + "pool.d/www.conf");

		units.add(iniConf);
		units.add(poolConf);

		iniConf.appendLine("[PHP]");
		iniConf.appendLine("engine = On");
		iniConf.appendLine("short_open_tag = Off");
		iniConf.appendLine("precision = 14");
		iniConf.appendLine("output_buffering = 4096");
		iniConf.appendLine("zlib.output_compression = Off");
		iniConf.appendLine("implicit_flush = Off");
		iniConf.appendLine("unserialize_callback_func =");
		iniConf.appendLine("serialize_precision = 17");
		iniConf.appendLine(
				"disable_functions = pcntl_alarm,pcntl_fork,pcntl_waitpid,pcntl_wait,pcntl_wifexited,pcntl_wifstopped,pcntl_wifsignaled,pcntl_wifcontinued,pcntl_wexitstatus,pcntl_wtermsig,pcntl_wstopsig,pcntl_signal,pcntl_signal_dispatch,pcntl_get_last_error,pcntl_strerror,pcntl_sigprocmask,pcntl_sigwaitinfo,pcntl_sigtimedwait,pcntl_exec,pcntl_getpriority,pcntl_setpriority,");
		iniConf.appendLine("disable_classes =");
		iniConf.appendLine("zend.enable_gc = On");
		iniConf.appendLine("expose_php = Off");
		iniConf.appendLine("max_execution_time = 300");
		iniConf.appendLine("max_input_time = 60");
		iniConf.appendLine("memory_limit = 256M");
		iniConf.appendLine("error_reporting = E_ALL & ~E_DEPRECATED & ~E_STRICT");
		iniConf.appendLine("display_errors = Off");
		iniConf.appendLine("display_startup_errors = Off");
		iniConf.appendLine("log_errors = On");
		iniConf.appendLine("log_errors_max_len = 1024");
		iniConf.appendLine("ignore_repeated_errors = Off");
		iniConf.appendLine("ignore_repeated_source = Off");
		iniConf.appendLine("report_memleaks = On");
		iniConf.appendLine("track_errors = Off");
		iniConf.appendLine("html_errors = On");
		iniConf.appendLine("variables_order = \\\"GPCS\\\"");
		iniConf.appendLine("request_order = \\\"GP\\\"");
		iniConf.appendLine("register_argc_argv = Off");
		iniConf.appendLine("auto_globals_jit = On");
		iniConf.appendLine("post_max_size = 500M");
		iniConf.appendLine("auto_prepend_file =");
		iniConf.appendLine("auto_append_file =");
		iniConf.appendLine("default_mimetype = \\\"text/html\\\"");
		iniConf.appendLine("default_charset = \\\"UTF-8\\\"");
		iniConf.appendLine("doc_root =");
		iniConf.appendLine("user_dir =");
		iniConf.appendLine("enable_dl = Off");
		iniConf.appendLine("file_uploads = On");
		iniConf.appendLine("upload_max_filesize = 500M");
		iniConf.appendLine("max_file_uploads = 20");
		iniConf.appendLine("allow_url_fopen = On");
		iniConf.appendLine("allow_url_include = Off");
		iniConf.appendLine("default_socket_timeout = 60");
		iniConf.appendLine("emergency_restart_threshold 10"); // If 10 child processes exit...
		iniConf.appendLine("emergency_restart_interval 1m"); // ...within a minute, restart PHP-FPM.
		iniConf.appendLine("process_control_timeout 10s"); // Allow 10 seconds for our child procs to get a response
		iniConf.appendCarriageReturn();
		iniConf.appendLine("[CLI Server]");
		iniConf.appendLine("cli_server.color = On");
		iniConf.appendCarriageReturn();
		iniConf.appendLine("[Pdo_mysql]");
		iniConf.appendLine("pdo_mysql.cache_size = 2000");
		iniConf.appendLine("pdo_mysql.default_socket=");
		iniConf.appendCarriageReturn();
		iniConf.appendLine("[mail function]");
		iniConf.appendLine("SMTP = localhost");
		iniConf.appendLine("smtp_port = 25");
		iniConf.appendLine("mail.add_x_header = On");
		iniConf.appendCarriageReturn();
		iniConf.appendLine("[SQL]");
		iniConf.appendLine("sql.safe_mode = Off");
		iniConf.appendCarriageReturn();
		iniConf.appendLine("[ODBC]");
		iniConf.appendLine("odbc.allow_persistent = On");
		iniConf.appendLine("odbc.check_persistent = On");
		iniConf.appendLine("odbc.max_persistent = -1");
		iniConf.appendLine("odbc.max_links = -1");
		iniConf.appendLine("odbc.defaultlrl = 4096");
		iniConf.appendLine("odbc.defaultbinmode = 1");
		iniConf.appendCarriageReturn();
		iniConf.appendLine("[Interbase]");
		iniConf.appendLine("ibase.allow_persistent = 1");
		iniConf.appendLine("ibase.max_persistent = -1");
		iniConf.appendLine("ibase.max_links = -1");
		iniConf.appendLine("ibase.timestampformat = \\\"%Y-%m-%d %H:%M:%S\\\"");
		iniConf.appendLine("ibase.dateformat = \\\"%Y-%m-%d\\\"");
		iniConf.appendLine("ibase.timeformat = \\\"%H:%M:%S\\\"");
		iniConf.appendCarriageReturn();
		iniConf.appendLine("[MySQLi]");
		iniConf.appendLine("mysqli.max_persistent = -1");
		iniConf.appendLine("mysqli.allow_persistent = On");
		iniConf.appendLine("mysqli.max_links = -1");
		iniConf.appendLine("mysqli.cache_size = 2000");
		iniConf.appendLine("mysqli.default_port = 3306");
		iniConf.appendLine("mysqli.default_socket =");
		iniConf.appendLine("mysqli.default_host =");
		iniConf.appendLine("mysqli.default_user =");
		iniConf.appendLine("mysqli.default_pw =");
		iniConf.appendLine("mysqli.reconnect = Off");
		iniConf.appendCarriageReturn();
		iniConf.appendLine("[mysqlnd]");
		iniConf.appendLine("mysqlnd.collect_statistics = On");
		iniConf.appendLine("mysqlnd.collect_memory_statistics = Off");
		iniConf.appendCarriageReturn();
		iniConf.appendLine("[bcmath]");
		iniConf.appendLine("bcmath.scale = 0");
		iniConf.appendCarriageReturn();
		iniConf.appendLine("[Session]");
		iniConf.appendLine("session.save_handler = files");
		iniConf.appendLine("session.use_strict_mode = 0");
		iniConf.appendLine("session.use_cookies = 1");
		iniConf.appendLine("session.use_only_cookies = 1");
		iniConf.appendLine("session.name = PHPSESSID");
		iniConf.appendLine("session.auto_start = 0");
		iniConf.appendLine("session.cookie_lifetime = 0");
		iniConf.appendLine("session.cookie_path = /");
		iniConf.appendLine("session.cookie_domain =");
		iniConf.appendLine("session.cookie_httponly =");
		iniConf.appendLine("session.serialize_handler = php");
		iniConf.appendLine("session.gc_probability = 0");
		iniConf.appendLine("session.gc_divisor = 1000");
		iniConf.appendLine("session.gc_maxlifetime = 1440");
		iniConf.appendLine("session.referer_check =");
		iniConf.appendLine("session.cache_limiter = nocache");
		iniConf.appendLine("session.cache_expire = 180");
		iniConf.appendLine("session.use_trans_sid = 0");
		iniConf.appendLine("session.hash_function = 0");
		iniConf.appendLine("session.hash_bits_per_character = 5");
		iniConf.appendLine("url_rewriter.tags = \\\"a=href,area=href,frame=src,input=src,form=fakeentry\\\"");
		iniConf.appendCarriageReturn();
		iniConf.appendLine("[Assertion]");
		iniConf.appendLine("zend.assertions = -1");
		iniConf.appendCarriageReturn();
		iniConf.appendLine("[Tidy]");
		iniConf.appendLine("tidy.clean_output = Off");
		iniConf.appendCarriageReturn();
		iniConf.appendLine("[soap]");
		iniConf.appendLine("soap.wsdl_cache_enabled=1");
		iniConf.appendLine("soap.wsdl_cache_dir=\\\"/tmp\\\"");
		iniConf.appendLine("soap.wsdl_cache_ttl=86400");
		iniConf.appendLine("soap.wsdl_cache_limit = 5");
		iniConf.appendCarriageReturn();
		iniConf.appendLine("[ldap]");
		iniConf.appendLine("ldap.max_links = -1");
		iniConf.appendCarriageReturn();
		iniConf.appendLine("[OPcache]");
		iniConf.appendLine("opcache.enable=1");
		iniConf.appendLine("opcache.enable_cli=1");
		iniConf.appendLine("opcache.interned_strings_buffer=8");
		iniConf.appendLine("opcache.max_accelerated_files=10000");
		iniConf.appendLine("opcache.memory_consumption=128");
		iniConf.appendLine("opcache.save_comments=1");
		iniConf.appendLine("opcache.revalidate_freq=1");

		// This is approximate, and very(!!!) generous(!!!) to give the box some
		// breathing room.
		// On most VMs I have configured, it's usually below 40M.
		final int mbRamPerProcess = 75;
		final int serverTotalRam = ((ServiceModel)getMachineModel()).getRAM();

		final int maxChildren = serverTotalRam / mbRamPerProcess;
		final int minSpareServers = maxChildren / 3;
		final int maxSpareServers = minSpareServers * 2;
		final int startServers = (minSpareServers + (maxSpareServers - minSpareServers)) / 2;

		poolConf.appendLine("[www]");
		poolConf.appendLine("user = nginx");
		poolConf.appendLine("group = nginx");
		poolConf.appendLine("listen = /run/php/php7.0-fpm.sock");
		poolConf.appendLine("listen.owner = nginx");
		poolConf.appendLine("listen.group = nginx");
		poolConf.appendLine("pm = dynamic");
		poolConf.appendLine("pm.max_children = " + maxChildren + "");
		poolConf.appendLine("pm.start_servers = " + startServers + "");
		poolConf.appendLine("pm.min_spare_servers = " + minSpareServers + "");
		poolConf.appendLine("pm.max_spare_servers = " + maxSpareServers + "");
		poolConf.appendLine("pm.max_requests = 200");
		poolConf.appendLine("env[PATH] = /usr/local/bin:/usr/bin:/bin");
		poolConf.appendLine("php_admin_value[max_execution_time] = 300");

		return units;
	}

	@Override
	public Collection<IUnit> getLiveConfig() throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new RunningUnit("php_fpm", "php7.0-fpm", "php7.0-fpm"));

		getServerModel().addProcessString("php-fpm: master process \\(/etc/php/7.0/fpm/php-fpm\\.conf\\) *$");
		getServerModel().addProcessString("php-fpm: pool www *$");

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() {
		return new ArrayList<>(); // Nothing to see here (yet?)
	}

	@Override
	public Collection<IUnit> getLiveFirewall() {
		return new ArrayList<>(); // Empty (for now?)
	}
}
