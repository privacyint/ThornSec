package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.model.ServerModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.CrontabUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;

public class MariaDB extends AStructuredProfile {
	
	private String username;
	private String password;
	private String dbName;
	private String privileges;
	
	public MariaDB(ServerModel me, NetworkModel networkModel, String username, String password, String dbName, String privileges) {
		super("mariadb", me, networkModel);
		
		this.setUsername(username);
		this.setUserPassword(password);
		this.setDb(dbName);
		this.setUserPrivileges(privileges);
	}

	public MariaDB(ServerModel me, NetworkModel networkModel) {
		this(me, networkModel, null, null, null, null);
	}

	public String getDb() {
		return this.dbName;
	}
	
	public String getUsername() {
		return this.username;
	}

	public String getUserPassword() {
		return this.password;
	}

	public String getUserPrivileges() {
		return this.privileges;
	}

	public void setUserPrivileges(String privileges) {
		this.privileges = privileges;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}

	public void setUserPassword(String password) {
		this.password = password;
	}

	public void setDb(String db) {
		this.dbName = db;
	}

	protected Vector<IUnit> getInstalled() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new SimpleUnit("mysql_user_exists", "proceed",
				"sudo useradd -r -s /bin/false mysql",
				"id mysql -u 2>&1 | grep id", "", "pass",
				"The mysql user couldn't be added.  This will cause all sorts of errors."));
		
		((ServerModel)me).getUserModel().addUsername("mysql");
		
		units.addElement(new InstalledUnit("openssl", "proceed", "openssl"));
		
		units.addAll(((ServerModel)me).getBindFsModel().addLogBindPoint("mysql", "proceed", "mysql", "0750"));
		units.addAll(((ServerModel)me).getBindFsModel().addDataBindPoint("mysql", "proceed", "mysql", "mysql", "0750"));
		units.addAll(((ServerModel)me).getBindFsModel().addDataBindPoint("mysql_backups", "proceed", "root", "root", "0600"));

		units.addElement(new SimpleUnit("mariadb_root_password", "openssl_installed",
				"echo \"[client]\npassword=\\\"${MYSQL_PASSWORD}\\\"\" | sudo tee /root/.my.cnf > /dev/null;",
				"sudo [ -f /root/.my.cnf ] || echo '' && (${MYSQL_PASSWORD}=\\$(grep 'password' /root/.my.cnf | awk -F\\\" '{ print $2 }')", "", "fail") );
		
		//Generate a root password, if not already installed
		units.addElement(new SimpleUnit("mariadb_root_password", "openssl_installed",
				"MYSQL_PASSWORD=`openssl rand -hex 32`",
				"echo $MYSQL_PASSWORD && dpkg -l | grep '^.i' | grep 'mariadb-server'", "", "fail",
				"Couldn't set MariaDB's root password.  Best case scenario, it'll be installed with a blank root password - worst case, its installation will have failed.  Run \"sudo mysql-secure-installation\" to fix."));
		
		//Use our generated password for root, but only set if not already installed
		units.addElement(new SimpleUnit("mariadb_rootpass", "mariadb_root_password",
				"sudo debconf-set-selections <<< 'mariadb-server-10.2 mysql-server/root_password password ${MYSQL_PASSWORD}'",
				"sudo debconf-show mariadb-server-10.2 | grep 'mysql-server/root_password:' || dpkg -l | grep '^.i' | grep 'mariadb-server'", "", "fail",
				"Couldn't set MariaDB's root password.  Best case scenario, it'll be installed with a blank root password - worst case, its installation will have failed.  Run \"sudo mysql-secure-installation\" to fix."));
		units.addElement(new SimpleUnit("mariadb_rootpass_again", "mariadb_rootpass",
				"sudo debconf-set-selections <<< 'mariadb-server-10.2 mysql-server/root_password_again password ${MYSQL_PASSWORD}'",
				"sudo debconf-show mariadb-server-10.2 | grep 'mysql-server/root_password_again:' || dpkg -l | grep '^.i' | grep 'mariadb-server'", "", "fail",
				"Couldn't set MariaDB's root password.  Best case scenario, it'll be installed with a blank root password - worst case, its installation will have failed.  Run \"sudo mysql-secure-installation\" to fix."));
		
		units.addElement(new InstalledUnit("mariadb", "mariadb_rootpass_again", "mariadb-server"));
				
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig() {
		Vector<IUnit> units =  new Vector<IUnit>();

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
		
		units.addElement(((ServerModel)me).getConfigsModel().addConfigFile("mysql", "mariadb_installed", conf, "/etc/mysql/mariadb.cnf"));
		
		units.addElement(new SimpleUnit("mariadb_data_dir_moved", "mariadb_installed",
				//We only want to move over the files if they don't already exist
				"sudo [ -d /media/data/mysql/mysql ] || sudo mv /var/lib/mysql/* /media/data/mysql/;"
				//Either which way, remove the new ones
				+ "sudo rm -R /var/lib/mysql;",
				"sudo [ -d /var/lib/mysql ] && echo fail || echo pass", "pass", "pass",
				"Couldn't move MariaDB's data directory.  This means that the database files will be stored in the VM, and won't be backed up."));
		
		units.addElement(new RunningUnit("mariadb", "mysql", "mysql"));

		units.addElement(new CrontabUnit("mysqldump", "mariadb_installed", true, "root", "mysqldump -uroot -h localhost --all-databases | gzip -9 > /media/data/mysql_backups/\\$(date -u).sql.gz > /dev/null", "*", "*", "*", "*/3", "30")); 

		return units;
	}

	protected Vector<IUnit> getLiveConfig() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new RunningUnit("mariadb", "mysql", "mysql"));
		
		((ServerModel)me).getProcessModel().addProcess("/bin/bash /usr/bin/mysqld_safe$");
		((ServerModel)me).getProcessModel().addProcess("/usr/sbin/mysqld$");
		((ServerModel)me).getProcessModel().addProcess("logger -t mysqld -p daemon.error$");
		
		units.addElement(new SimpleUnit("mariadb_no_failed_logins", "mariadb_installed",
				"",
				"sudo grep \"[Warning] Access denied for user\" /var/log/syslog", "", "pass",
				"There are failed logins to your mysql server.  This implies someone is trying to log in through the command line, and could be an indicator of compromise."));
		
		return units;
	}

	public Vector<IUnit> checkUserExists() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String userCreateSql = "CREATE USER IF NOT EXISTS '" + getUsername() + "'@'localhost' IDENTIFIED BY '" + getUserPassword() + "';";
		
		units.addElement(new SimpleUnit(me.getHostname() + "_mariadb_db_exists", "mariadb_installed",
				"sudo mysql -uroot -B -N -e \"" + userCreateSql + "\" 2>&1",
				"sudo mysql -uroot -B -N -e \"SELECT EXISTS(SELECT 1 FROM mysql.user WHERE user = '" + getUsername() + "')\" 2>&1", "1", "pass",
				"Could not create/update the MYSQL user required for " + me.getHostname() + ". Don't expect anything to work, I'm afraid"));

		return units;
	}

	public Vector<IUnit> checkDbExists() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String dbCreateSql = "";
		dbCreateSql += "CREATE DATABASE IF NOT EXISTS '" + getDb() + ";";
		dbCreateSql += "GRANT " + getUserPrivileges() + " ON " + getDb() + " TO '" + getUsername() + "'@'localhost';";
		dbCreateSql += "SET GLOBAL default_storage_engine = 'InnoDB';"; 
		dbCreateSql += "SET GLOBAL innodb_large_prefix=on;";

		units.addElement(new SimpleUnit(me.getHostname() + "_mariadb_db_exists", me.getHostname() + "_mariadb_user_exists",
				"sudo mysql -uroot -B -N -e \"" + dbCreateSql + "\" 2>&1",
				"mysql -u" + getUsername() + " -p" + getUserPassword() + " -B -N -e \"SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '" + getDb() + "'\" 2>&1", getDb(), "pass",
				"Could not create the MYSQL database required for " + me.getHostname() + ". Don't expect anything to work, I'm afraid"));
		
		return units;
	}

	
	public Vector<IUnit> queryDb(String db, String username, String password, String query) {
		Vector<IUnit> units = new Vector<IUnit>();
	
		return units;
	}
	
/*	private String stopMariaDb() {
		//Stop the service, and sleep until it stops to prevent race conditions
		return "sudo kill -SIGTERM $(sudo cat /var/run/mysqld/mysqld.pid);"
				+ "while sudo test -f /var/run/mysqld/mysqld.pid;" //Sleep until the pid file has gone (i.e. MySQL service has been killed)
					+ "do sleep 2;"
				+ "done;";
	}
	
	private String startMariaDbWithInit(String query) {
		return	stopMariaDb()
				+ "echo \"" + query + "\" | sudo tee /etc/mysql/query.sql > /dev/null;" //Echo our user creation query out
				+ "sudo -u mysql mysqld --init-file=/etc/mysql/query.sql --pid-file=/var/run/mysqld/mysqld.pid & disown;" //Start MariaDB, importing the sql query
//				+ "sleep 5;" //Wait until MariaDB has started again
				+ "while sudo test ! -f /var/run/mysqld/mysqld.pid;"
					+ "do sleep 2;"
				+ "done;"
				+ "sudo rm /etc/mysql/query.sql"; //Then delete the query
	}
	
	private String startMariaDbUser() {
		//Forcibly restart the service and sleep until it's actually restarted
		return "sudo service mysql stop && sudo service mysql start;"
				+ "while sudo test ! -f /var/run/mysqld/mysqld.pid;"
					+ "do sleep 2;"
				+ "done;";
	}
	*/
	public Vector<IUnit> getNetworking() {
		Vector<IUnit> units = new Vector<IUnit>();

		((ServerModel)me).getAptSourcesModel().addAptSource("mariadb", "proceed", "deb http://mirror.sax.uk.as61049.net/mariadb/repo/10.2/debian stretch main", "keyserver.ubuntu.com", "0xF1656F24C74CD1D8");

		me.addRequiredEgress("mirror.sax.uk.as61049.net");
		
		return units;
	}

}
