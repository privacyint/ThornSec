/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.profile.stack;

import java.util.ArrayList;
import java.util.Collection;
import org.privacyinternational.thornsec.core.exception.runtime.InvalidMachineModelException;
import org.privacyinternational.thornsec.core.iface.IUnit;
import org.privacyinternational.thornsec.core.model.machine.ServerModel;
import org.privacyinternational.thornsec.core.profile.AStructuredProfile;
import org.privacyinternational.thornsec.core.unit.SimpleUnit;
import org.privacyinternational.thornsec.core.unit.fs.FileUnit;
import org.privacyinternational.thornsec.core.unit.pkg.InstalledUnit;
import org.privacyinternational.thornsec.core.unit.pkg.RunningUnit;

public class MariaDB extends AStructuredProfile {

	private String username;
	private String password;
	private String db;
	private String privileges;

	public MariaDB(ServerModel me) {
		super(me);
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
	public Collection<IUnit> getInstalled() throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("openssl", "proceed", "openssl"));
		units.add(new InstalledUnit("mariadb", "proceed", "mariadb-server"));

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentConfig() {
		final Collection<IUnit> units = new ArrayList<>();

		final FileUnit myCnf = new FileUnit("mysql_conf", "mariadb_installed", "/etc/mysql/my.cnf");
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
		// These are *probably* default, but let's be explicit about it...
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

		return units;
	}

	@Override
	public Collection<IUnit> getLiveConfig() throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new RunningUnit("mariadb", "mysql", "mysql"));

		getServerModel().addProcessString("/bin/bash /usr/bin/mysqld_safe$");
		getServerModel().addProcessString("/usr/sbin/mysqld$");
		getServerModel().addProcessString("logger -t mysqld -p daemon.error$");

		units.add(new SimpleUnit("mariadb_no_failed_logins", "mariadb_installed", "",
				"sudo grep \"[Warning] Access denied for user\" /var/log/syslog", "", "pass",
				"There are failed logins to your mysql server.  This implies someone is trying to log in through the command line, and could be an indicator of compromise."));

		return units;
	}

	public Collection<IUnit> checkUserExists() throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		final String userCreateSql = "CREATE USER IF NOT EXISTS '" + getUsername() + "'@'localhost' IDENTIFIED BY '"
				+ getUserPassword() + "';";

		units.add(new SimpleUnit(getMachineModel().getLabel() + "_mariadb_db_exists",
				"mariadb_installed", "sudo mysql -uroot -B -N -e \"" + userCreateSql + "\" 2>&1",
				"sudo mysql -uroot -B -N -e \"SELECT EXISTS(SELECT 1 FROM mysql.user WHERE user = '" + getUsername()
						+ "')\" 2>&1",
				"1", "pass", "Could not create/update the MYSQL user required for " + getMachineModel().getLabel()
						+ ". Don't expect anything to work, I'm afraid"));

		return units;
	}

	public Collection<IUnit> checkDbExists() {
		final Collection<IUnit> units = new ArrayList<>();

		String dbCreateSql = "";
		dbCreateSql += "CREATE DATABASE IF NOT EXISTS '" + getDb() + ";";
		dbCreateSql += "GRANT " + getUserPrivileges() + " ON " + getDb() + " TO '" + getUsername() + "'@'localhost';";
		dbCreateSql += "SET GLOBAL default_storage_engine = 'InnoDB';";
		dbCreateSql += "SET GLOBAL innodb_large_prefix=on;";

		units.add(new SimpleUnit(getMachineModel().getLabel() + "_mariadb_db_exists", getMachineModel().getLabel() + "_mariadb_user_exists",
				"sudo mysql -uroot -B -N -e \"" + dbCreateSql + "\" 2>&1",
				"mysql -u" + getUsername() + " -p" + getUserPassword()
						+ " -B -N -e \"SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '"
						+ getDb() + "'\" 2>&1",
				getDb(), "pass", "Could not create the MYSQL database required for " + getMachineModel().getLabel()
						+ ". Don't expect anything to work, I'm afraid"));

		return units;
	}

}
