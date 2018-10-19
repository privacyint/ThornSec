package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;

public class MariaDB extends AStructuredProfile {
	
	public MariaDB() {
		super("mariadb");
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new SimpleUnit("mysql_user", "proceed",
				"sudo useradd -r -s /bin/false mysql",
				"id mysql 2>&1", "id: ‘mysql’: no such user", "fail",
				"The mysql user couldn't be added.  This will cause all sorts of errors."));
		
		model.getServerModel(server).getUserModel().addUsername("mysql");
		
		units.addElement(new InstalledUnit("openssl", "proceed", "openssl"));
		
		model.getServerModel(server).getAptSourcesModel().addAptSource(server, model, "mariadb", "proceed", "deb http://mirror.sax.uk.as61049.net/mariadb/repo/10.2/debian stretch main", "keyserver.ubuntu.com", "0xF1656F24C74CD1D8");
		
		model.getServerModel(server).addRouterEgressFirewallRule(server, model, "allow_mariadb", "mirror.sax.uk.as61049.net", new String[]{"80","443"});
		
		units.addAll(model.getServerModel(server).getBindFsModel().addLogBindPoint(server, model, "mysql", "proceed", "mysql", "0750"));

		//Generate a root password, if not already installed
		units.addElement(new SimpleUnit("mariadb_generate_root_password", "openssl_installed",
				"MYSQL_PASSWORD=`openssl rand -hex 32`",
				"echo $MYSQL_PASSWORD && dpkg -l | grep '^.i' | grep 'mariadb-server'", "", "fail",
				"Couldn't set MariaDB's root password.  Best case scenario, it'll be installed with a blank root password - worst case, its installation will have failed.  Run \"sudo mysql-secure-installation\" to fix."));
		
		//Use our generated password for root, but only set if not already installed
		units.addElement(new SimpleUnit("mariadb_rootpass", "mariadb_generate_root_password",
				"sudo debconf-set-selections <<< 'mariadb-server-10.2 mysql-server/root_password password ${MYSQL_PASSWORD}'",
				"sudo debconf-show mariadb-server-10.2 | grep 'mysql-server/root_password:' || dpkg -l | grep '^.i' | grep 'mariadb-server'", "", "fail",
				"Couldn't set MariaDB's root password.  Best case scenario, it'll be installed with a blank root password - worst case, its installation will have failed.  Run \"sudo mysql-secure-installation\" to fix."));
		units.addElement(new SimpleUnit("mariadb_rootpass_again", "mariadb_rootpass",
				"sudo debconf-set-selections <<< 'mariadb-server-10.2 mysql-server/root_password_again password ${MYSQL_PASSWORD}'",
				"sudo debconf-show mariadb-server-10.2 | grep 'mysql-server/root_password_again:' || dpkg -l | grep '^.i' | grep 'mariadb-server'", "", "fail",
				"Couldn't set MariaDB's root password.  Best case scenario, it'll be installed with a blank root password - worst case, its installation will have failed.  Run \"sudo mysql-secure-installation\" to fix."));
		
		units.addElement(new InstalledUnit("mariadb", "mariadb_rootpass_again", "mariadb-server"));
		
		units.addAll(model.getServerModel(server).getBindFsModel().addDataBindPoint(server, model, "mysql", "mariadb_installed", "mysql", "mysql", "0750"));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units =  new Vector<IUnit>();

		units.addElement(new SimpleUnit("mariadb_stopped", "mysql_data_mounted",
				stopMariaDb(),
				"sudo [ -f /var/run/mysqld/mysqld.pid ] && echo fail || echo pass", "pass", "pass"));

		units.addElement(new SimpleUnit("mariadb_data_dir_moved", "mariadb_installed",
				//We only want to move over the files if they don't already exist
				"sudo [ -d /media/data/mysql/mysql ] || sudo mv /var/lib/mysql/* /media/data/mysql/;"
				//Either which way, remove the new ones
				+ "sudo rm -R /var/lib/mysql;",
				"sudo [ -d /var/lib/mysql ] && echo fail || echo pass", "pass", "pass",
				"Couldn't move MariaDB's data directory.  This means that the database files will be stored in the VM."));

		String conf = "";
		conf += "[client]\n";
		conf += "port                    = 3306\n";
		conf += "socket                  = /var/run/mysqld/mysqld.sock\n";
		conf += "\n";
		conf += "[mysqld_safe]\n";
		conf += "socket                  = /var/run/mysqld/mysqld.sock\n";
		conf += "nice                    = 0\n";
		conf += "\n";
		conf += "[mysqld]\n";
		conf += "user                    = mysql\n";
		conf += "pid-file                = /var/run/mysqld/mysqld.pid\n";
		conf += "socket                  = /var/run/mysqld/mysqld.sock\n";
		conf += "port                    = 3306\n";
		conf += "basedir                 = /usr\n";
		conf += "datadir                 = /media/data/mysql\n";
		conf += "tmpdir                  = /tmp\n";
		conf += "lc_messages_dir         = /usr/share/mysql\n";
		conf += "lc_messages             = en_US\n";
		conf += "skip-external-locking\n";
		conf += "bind-address            = 127.0.0.1\n";
		conf += "max_connections         = 100\n";
		conf += "connect_timeout         = 5\n";
		conf += "wait_timeout            = 600\n";
		conf += "max_allowed_packet      = 16M\n";
		conf += "thread_cache_size       = 128\n";
		conf += "sort_buffer_size        = 4M\n";
		conf += "bulk_insert_buffer_size = 16M\n";
		conf += "tmp_table_size          = 32M\n";
		conf += "max_heap_table_size     = 32M\n";
		conf += "myisam_recover_options  = BACKUP\n";
		conf += "key_buffer_size         = 128M\n";
		conf += "table_open_cache        = 400\n";
		conf += "myisam_sort_buffer_size = 512M\n";
		conf += "concurrent_insert       = 2\n";
		conf += "read_buffer_size        = 2M\n";
		conf += "read_rnd_buffer_size    = 1M\n";
		conf += "query_cache_limit       = 128K\n";
		conf += "query_cache_size        = 64M\n";
		conf += "log_warnings            = 2\n";
		conf += "slow_query_log_file     = /var/log/mysql/mariadb-slow.log\n";
		conf += "long_query_time         = 10\n";
		conf += "log_slow_verbosity      = query_plan\n";
		conf += "log_bin                 = /var/log/mysql/mariadb-bin\n";
		conf += "log_bin_index           = /var/log/mysql/mariadb-bin.index\n";
		conf += "expire_logs_days        = 10\n";
		conf += "max_binlog_size         = 100M\n";
		conf += "default_storage_engine  = InnoDB\n";
		conf += "innodb_buffer_pool_size = 256M\n";
		conf += "innodb_log_buffer_size  = 8M\n";
		conf += "innodb_file_per_table   = 1\n";
		conf += "innodb_open_files       = 400\n";
		conf += "innodb_io_capacity      = 400\n";
		conf += "innodb_flush_method     = O_DIRECT\n";
		//These are *probably* default, but let's be explicit about it...
		conf += "innodb_large_prefix     = true\n";
		conf += "innodb_file_format      = barracuda\n";
		conf += "innodb_file_per_table   = true\n";
		conf += "\n";
		conf += "[mysqldump]\n";
		conf += "quick\n";
		conf += "quote-names\n";
		conf += "max_allowed_packet      = 16M\n";
		conf += "\n";
		conf += "[isamchk]\n";
		conf += "key_buffer              = 16M\n";
		conf += "\n";
		conf += "!includedir /etc/mysql/conf.d/";
		
		units.addElement(model.getServerModel(server).getConfigsModel().addConfigFile("mysql", "mariadb_installed", conf, "/etc/mysql/my.cnf"));
		
		units.addElement(new RunningUnit("mariadb", "mysql", "mysql"));

		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new RunningUnit("mariadb", "mysql", "mysql"));
		
		model.getServerModel(server).getProcessModel().addProcess("/bin/bash /usr/bin/mysqld_safe$");
		model.getServerModel(server).getProcessModel().addProcess("/usr/sbin/mysqld$");
		model.getServerModel(server).getProcessModel().addProcess("logger -t mysqld -p daemon.error$");
		
		units.addElement(new SimpleUnit("mariadb_no_failed_logins", "mariadb_installed",
				"",
				"sudo grep \"[Warning] Access denied for user\" /var/log/syslog", "", "pass",
				"There are failed logins to your mysql server.  This implies someone is trying to log in through the command line, and could be an indicator of compromise."));
		
		return units;
	}
	
	public Vector<IUnit> createDb(String db, String username, String privileges, String passwordVariable) {
		return createDb(db, db, username, privileges, passwordVariable);
	}

	
	public Vector<IUnit> createDb(String db, String grantDb, String username, String privileges, String passwordVariable) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		//This is all in one unit test as it puts the database into a transient state for configuration. This is not a usual state which should be audited for.
		units.addElement(new SimpleUnit(db+"_mariadb_user_exists", "mariadb_installed",
				stopMariaDb()
				+ startMariaDbNoUser()
				//Do our stuff
				+ "mysql -e \""
						+ "FLUSH PRIVILEGES;" //Reload the grant tables now we've logged in https://dev.mysql.com/doc/refman/5.7/en/flush.html
						+ "CREATE USER '" + username + "'@'localhost' IDENTIFIED BY '${" + passwordVariable + "}';"
						+ "CREATE DATABASE " + db + ";"
						+ "GRANT " + privileges + " "
						+ "ON " + grantDb + ".* "
						+ "TO '" + username + "'@'localhost';"
						+ "SET GLOBAL default_storage_engine = 'InnoDB';"
						+ "SET GLOBAL innodb_large_prefix=on;"
				+ "\";"
				+ stopMariaDb()
				+ startMariaDbUser()
				,
				/********************************************************************************************************************************************************
				* Hackety hack!																																			*
				* Iff the password variable is set:																														*
				*  - This checks if it's a valid login without privileges on the user table (if config has been done) /or/ just blindly logs in (--skip-grant-tables)	*
				*  - 1142 is the error for "user exists, credentials are correct, but doesn't have permission to do that operation"										*
				*  - $1 otherwise returns the value of SELECT EXISTS, which returns '1' on true, '0' on false															*
				*  - Checking for both is required, as flushing privileges stops blind logging in, causing the retest to fail											*
				********************************************************************************************************************************************************/
				"[[ -z ${" + passwordVariable + "} ]] && echo 0 || mysql -u" + username + " -p${" + passwordVariable + "} -B -N -e \"SELECT EXISTS(SELECT 1 FROM mysql.user WHERE user = '" + username + "')\" 2>&1 | awk '{if ($2 == \"1142\") { print \"1\" } else { print $1 }}'", "1", "pass",
				"Couldn't create " + db + " database.  There could be multiple reasons for this error, please check the output to see what went wrong."));
		
		return units;
	}
	
//Stub for now
	public Vector<IUnit> queryDb(String db, String username, String password, String query) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		return units;
	}
	
	private String stopMariaDb() {
		//Stop the service, and sleep until it stops to prevent race conditions
		return "sudo kill -SIGTERM $(sudo cat /var/run/mysqld/mysqld.pid);"
				+ "while sudo test -f /var/run/mysqld/mysqld.pid;" //Sleep until the pid file has gone (i.e. MySQL service has been killed)
					+ "do sleep 2;"
				+ "done;";	
	}
	
	private String startMariaDbNoUser() {
				//Start the service without the grant tables, and _wait_ for it to start to stop race conditions
				//See https://dev.mysql.com/doc/refman/5.7/en/server-options.html#option_mysqld_skip-grant-tables
		return "sudo -u mysql bash -c 'mysqld_safe --skip-grant-tables --skip-networking &';"
				+ "while [ ! -S /var/run/mysqld/mysqld.sock ];" //Sleep until the socket has been opened (i.e. MySQL has started)
					+ "do sleep 2;"
				+ "done;";
	}
	
	private String startMariaDbUser() {
		//Forcibly restart the service and sleep until it's actually restarted
		return "sudo service mysql stop && sudo service mysql start;"
				+ "while sudo test ! -f /var/run/mysqld/mysqld.pid;"
					+ "do sleep 2;"
				+ "done;";
	}

}