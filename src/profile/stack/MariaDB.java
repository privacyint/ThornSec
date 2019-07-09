package profile.stack;

import java.util.HashSet;
import java.util.Set;

import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.network.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;

public class MariaDB extends AStructuredProfile {
	
	private String username;
	private String password;
	private String db;
	private String privileges;
	
	public MariaDB(String label, NetworkModel networkModel, String username, String password, String dbName, String privileges) {
		super(label, networkModel);
		
		this.setUsername(username);
		this.setUserPassword(password);
		this.setDb(dbName);
		this.setUserPrivileges(privileges);
	}

	public MariaDB(String label, NetworkModel networkModel) {
		this(label, networkModel, null, null, null, null);
	}

	public final String getDb() {
		return this.db;
	}
	
	public final String getUsername() {
		return this.username;
	}

	public final String getUserPassword() {
		return this.password;
	}

	public final String getUserPrivileges() {
		return this.privileges;
	}

	public final void setUserPrivileges(String privileges) {
		this.privileges = privileges;
	}
	
	public final void setUsername(String username) {
		this.username = username;
	}

	public final void setUserPassword(String password) {
		this.password = password;
	}

	public final void setDb(String db) {
		this.db = db;
	}

	@Override
	public Set<IUnit> getInstalled()
	throws InvalidServerModelException {
		Set<IUnit> units = new HashSet<IUnit>();

		units.add(new InstalledUnit("openssl", "proceed", "openssl"));

		units.add(new SimpleUnit("mysql_user_exists", "proceed",
				"sudo useradd -r -s /bin/false mysql",
				"id mysql -u 2>&1 | grep id", "", "pass",
				"The mysql user couldn't be added.  This will cause all sorts of errors."));
		
		networkModel.getServerModel(getLabel()).getUserModel().addUsername("mysql");
		
		units.addAll(networkModel.getServerModel(getLabel()).getBindFsModel().addLogBindPoint("mysql", "proceed", "mysql", "0750"));
		units.addAll(networkModel.getServerModel(getLabel()).getBindFsModel().addDataBindPoint("mysql", "proceed", "mysql", "mysql", "0750"));
		units.addAll(networkModel.getServerModel(getLabel()).getBindFsModel().addDataBindPoint("mysql_backups", "proceed", "root", "root", "0600"));

		units.add(new SimpleUnit("mariadb_root_password", "openssl_installed",
				"echo \"[client]\npassword=\\\"${MYSQL_PASSWORD}\\\"\" | sudo tee /root/.my.cnf > /dev/null;",
				"sudo [ -f /root/.my.cnf ] || echo '' && (${MYSQL_PASSWORD}=\\$(grep 'password' /root/.my.cnf | awk -F\\\" '{ print $2 }')", "", "fail") );
		
		//Generate a root password, if not already installed
		units.add(new SimpleUnit("mariadb_root_password", "openssl_installed",
				"MYSQL_PASSWORD=`openssl rand -hex 32`",
				"echo $MYSQL_PASSWORD && dpkg -l | grep '^.i' | grep 'mariadb-server'", "", "fail",
				"Couldn't set MariaDB's root password.  Best case scenario, it'll be installed with a blank root password - worst case, its installation will have failed.  Run \"sudo mysql-secure-installation\" to fix."));
		
		//Use our generated password for root, but only set if not already installed
		units.add(new SimpleUnit("mariadb_rootpass", "mariadb_root_password",
				"sudo debconf-set-selections <<< 'mariadb-server-10.2 mysql-server/root_password password ${MYSQL_PASSWORD}'",
				"sudo debconf-show mariadb-server-10.2 | grep 'mysql-server/root_password:' || dpkg -l | grep '^.i' | grep 'mariadb-server'", "", "fail",
				"Couldn't set MariaDB's root password.  Best case scenario, it'll be installed with a blank root password - worst case, its installation will have failed.  Run \"sudo mysql-secure-installation\" to fix."));
		units.add(new SimpleUnit("mariadb_rootpass_again", "mariadb_rootpass",
				"sudo debconf-set-selections <<< 'mariadb-server-10.2 mysql-server/root_password_again password ${MYSQL_PASSWORD}'",
				"sudo debconf-show mariadb-server-10.2 | grep 'mysql-server/root_password_again:' || dpkg -l | grep '^.i' | grep 'mariadb-server'", "", "fail",
				"Couldn't set MariaDB's root password.  Best case scenario, it'll be installed with a blank root password - worst case, its installation will have failed.  Run \"sudo mysql-secure-installation\" to fix."));
		
		units.add(new InstalledUnit("mariadb", "mariadb_rootpass_again", "mariadb-server"));
				
		return units;
	}
	
	@Override
	public Set<IUnit> getPersistentConfig() {
		Set<IUnit> units =  new HashSet<IUnit>();

		units.add(new SimpleUnit("mariadb_data_dir_moved", "mariadb_installed",
				//We only want to move over the files if they don't already exist
				"sudo [ -d /media/data/mysql/mysql ] || sudo mv /var/lib/mysql/* /media/data/mysql/;"
				//Either which way, remove the new ones
				+ "sudo rm -R /var/lib/mysql;",
				"sudo [ -d /var/lib/mysql ] && echo fail || echo pass", "pass", "pass",
				"Couldn't move MariaDB's data directory.  This means that the database files will be stored in the VM, and won't be backed up."));

		FileUnit myCnf = new FileUnit("mysql_conf", "mariadb_installed", "/etc/mysql/my.cnf");
		units.add(myCnf);
		
		myCnf.appendLine("[client]");
 		myCnf.appendLine("port                    = 3306");
		myCnf.appendLine("socket                  = /var/run/mysqld/mysqld.sock");
		myCnf.appendCarriageReturn();
		myCnf.appendLine("[mysqld_safe]");
		myCnf.appendLine("socket                  = /var/run/mysqld/mysqld.sock");
		myCnf.appendLine("nice                    = 0");
		myCnf.appendCarriageReturn();
		myCnf.appendLine("[mysqld]");
		myCnf.appendLine("user                    = mysql");
		myCnf.appendLine("pid-file                = /var/run/mysqld/mysqld.pid");
		myCnf.appendLine("socket                  = /var/run/mysqld/mysqld.sock");
		myCnf.appendLine("port                    = 3306");
		myCnf.appendLine("basedir                 = /usr");
		myCnf.appendLine("datadir                 = /media/data/mysql");
		myCnf.appendLine("tmpdir                  = /tmp");
		myCnf.appendLine("lc_messages_dir         = /usr/share/mysql");
		myCnf.appendLine("lc_messages             = en_US");
		myCnf.appendLine("skip-external-locking");
		myCnf.appendLine("bind-address            = 127.0.0.1");
		myCnf.appendLine("max_connections         = 100");
		myCnf.appendLine("connect_timeout         = 5");
		myCnf.appendLine("wait_timeout            = 600");
		myCnf.appendLine("max_allowed_packet      = 16M");
		myCnf.appendLine("thread_cache_size       = 128");
		myCnf.appendLine("sort_buffer_size        = 4M");
		myCnf.appendLine("bulk_insert_buffer_size = 16M");
		myCnf.appendLine("tmp_table_size          = 32M");
		myCnf.appendLine("max_heap_table_size     = 32M");
		myCnf.appendLine("myisam_recover_options  = BACKUP");
		myCnf.appendLine("key_buffer_size         = 128M");
		myCnf.appendLine("table_open_cache        = 400");
		myCnf.appendLine("myisam_sort_buffer_size = 512M");
		myCnf.appendLine("concurrent_insert       = 2");
		myCnf.appendLine("read_buffer_size        = 2M");
		myCnf.appendLine("read_rnd_buffer_size    = 1M");
		myCnf.appendLine("query_cache_limit       = 128K");
		myCnf.appendLine("query_cache_size        = 64M");
		myCnf.appendLine("log_warnings            = 2");
		myCnf.appendLine("slow_query_log_file     = /var/log/mysql/mariadb-slow.log");
		myCnf.appendLine("long_query_time         = 10");
		myCnf.appendLine("log_slow_verbosity      = query_plan");
		myCnf.appendLine("log_bin                 = /var/log/mysql/mariadb-bin");
		myCnf.appendLine("log_bin_index           = /var/log/mysql/mariadb-bin.index");
		myCnf.appendLine("expire_logs_days        = 10");
		myCnf.appendLine("max_binlog_size         = 100M");
		myCnf.appendLine("default_storage_engine  = InnoDB");
		myCnf.appendLine("innodb_buffer_pool_size = 256M");
		myCnf.appendLine("innodb_log_buffer_size  = 8M");
		myCnf.appendLine("innodb_file_per_table   = 1");
		myCnf.appendLine("innodb_open_files       = 400");
		myCnf.appendLine("innodb_io_capacity      = 400");
		myCnf.appendLine("innodb_flush_method     = O_DIRECT");
		//These are *probably* default, but let's be explicit about it...
		myCnf.appendLine("innodb_large_prefix     = true");
		myCnf.appendLine("innodb_file_format      = barracuda");
		myCnf.appendLine("innodb_file_per_table   = true");
		myCnf.appendCarriageReturn();
		myCnf.appendLine("[mysqldump]");
		myCnf.appendLine("quick");
		myCnf.appendLine("quote-names");
		myCnf.appendLine("max_allowed_packet      = 16M");
		myCnf.appendCarriageReturn();
		myCnf.appendLine("[isamchk]");
		myCnf.appendLine("key_buffer              = 16M");
		myCnf.appendCarriageReturn();
		myCnf.appendLine("!includedir /etc/mysql/conf.d/");
				
		units.add(new RunningUnit("mariadb", "mysql", "mysql"));

		//units.add(new CrontabUnit("mysqldump", "mariadb_installed", true, "root", "mysqldump -uroot -h localhost --all-databases | gzip -9 > /media/data/mysql_backups/\\$(date -u).sql.gz > /dev/null", "*", "*", "*", "*/3", "30")); 

		return units;
	}

	@Override
	public Set<IUnit> getLiveConfig()
	throws InvalidServerModelException {
		Set<IUnit> units = new HashSet<IUnit>();
		
		units.add(new RunningUnit("mariadb", "mysql", "mysql"));
		
		networkModel.getServerModel(getLabel()).addProcessString("/bin/bash /usr/bin/mysqld_safe$");
		networkModel.getServerModel(getLabel()).addProcessString("/usr/sbin/mysqld$");
		networkModel.getServerModel(getLabel()).addProcessString("logger -t mysqld -p daemon.error$");
		
		units.add(new SimpleUnit("mariadb_no_failed_logins", "mariadb_installed",
				"",
				"sudo grep \"[Warning] Access denied for user\" /var/log/syslog", "", "pass",
				"There are failed logins to your mysql server.  This implies someone is trying to log in through the command line, and could be an indicator of compromise."));
		
		return units;
	}

	public Set<IUnit> checkUserExists()
	throws InvalidServerModelException {
		Set<IUnit> units = new HashSet<IUnit>();
		
		String userCreateSql = "CREATE USER IF NOT EXISTS '" + getUsername() + "'@'localhost' IDENTIFIED BY '" + getUserPassword() + "';";
		
		units.add(new SimpleUnit(networkModel.getServerModel(getLabel()) + "_mariadb_db_exists", "mariadb_installed",
				"sudo mysql -uroot -B -N -e \"" + userCreateSql + "\" 2>&1",
				"sudo mysql -uroot -B -N -e \"SELECT EXISTS(SELECT 1 FROM mysql.user WHERE user = '" + getUsername() + "')\" 2>&1", "1", "pass",
				"Could not create/update the MYSQL user required for " + getLabel() + ". Don't expect anything to work, I'm afraid"));

		return units;
	}

	public Set<IUnit> checkDbExists() {
		Set<IUnit> units = new HashSet<IUnit>();
		
		String dbCreateSql = "";
		dbCreateSql += "CREATE DATABASE IF NOT EXISTS '" + getDb() + ";";
		dbCreateSql += "GRANT " + getUserPrivileges() + " ON " + getDb() + " TO '" + getUsername() + "'@'localhost';";
		dbCreateSql += "SET GLOBAL default_storage_engine = 'InnoDB';"; 
		dbCreateSql += "SET GLOBAL innodb_large_prefix=on;";

		units.add(new SimpleUnit(getLabel() + "_mariadb_db_exists", getLabel() + "_mariadb_user_exists",
				"sudo mysql -uroot -B -N -e \"" + dbCreateSql + "\" 2>&1",
				"mysql -u" + getUsername() + " -p" + getUserPassword() + " -B -N -e \"SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '" + getDb() + "'\" 2>&1", getDb(), "pass",
				"Could not create the MYSQL database required for " + getLabel() + ". Don't expect anything to work, I'm afraid"));
		
		return units;
	}

	public Set<IUnit> queryDb(String db, String username, String password, String query) {
		Set<IUnit> units = new HashSet<IUnit>();
	
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
	
	@Override
	public Set<IUnit> getPersistentFirewall()
	throws InvalidServerModelException {
		Set<IUnit> units = new HashSet<IUnit>();

		networkModel.getServerModel(getLabel()).getAptSourcesModel().addAptSource("mariadb", "deb http://mirror.sax.uk.as61049.net/mariadb/repo/10.2/debian stretch main", "keyserver.ubuntu.com", "0xF1656F24C74CD1D8");

		networkModel.getServerModel(getLabel()).addEgress("mirror.sax.uk.as61049.net");
		
		return units;
	}
	
	@Override
	public Set<IUnit> getLiveFirewall() {
		return new HashSet<IUnit>(); //Empty (for now?)
	}

}
