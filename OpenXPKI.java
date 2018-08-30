package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.FileUnit;

public class OpenXPKI extends AStructuredProfile {
	
	private ChrootJessie chroot;
	private MariaDB db;
	private String chrootCmd;
	
	public OpenXPKI() {
		super("openxpki");
		
		this.chroot = new ChrootJessie();
		this.chrootCmd = "sudo chroot " + chroot.getChrootDir();
		
		this.db = new MariaDB();
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(chroot.getInstalled(server, model));
		units.addAll(db.getInstalled(server, model));
		
		units.addAll(chroot.installPackage("chroot_locales", "chroot_installed", "locales"));
		units.addAll(chroot.installPackage("libdb_mysql_perl", "chroot_installed", "libdbd-mysql-perl"));
		units.addAll(chroot.installPackage("libapache2_mod_fcgid", "chroot_installed", "libapache2-mod-fcgid"));
		
		units.addElement(new FileUnit("openxpki_apt", "chroot_installed", "deb http://packages.openxpki.org/debian/ jessie release", chroot.getChrootDir() + "/etc/apt/sources.list.d/openxpki.list"));
		
		units.addElement(new SimpleUnit("openxpki_repo_gpg", "chroot_installed",
				"wget -O - https://packages.openxpki.org/debian/Release.key|"+ chrootCmd + " apt-key add -",
				chrootCmd + " apt-key list | grep 'OpenXPKI'", "", "fail"));

		units.addAll(chroot.installPackage("libopenxpki", "chroot_installed", "libopenxpki-perl"));
		units.addAll(chroot.installPackage("openxpki", "chroot_installed", "openxpki-i18n"));

		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units =  new Vector<IUnit>();
		
		units.addAll(db.getPersistentConfig(server, model));
		
		units.addElement(new FileUnit("chroot_available_locales", "chroot_installed", "en_US.UTF-8 UTF-8", chroot.getChrootDir() + "/etc/locale.gen"));
		
		units.addElement(new SimpleUnit("chroot_locale_en_us", "chroot_installed",
				chrootCmd + " localedef -i en_US -c -f UTF-8 en_US.UTF-8",
				chrootCmd + " locale -a 2>/dev/null | grep en_US.UTF-8", "", "fail"));
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(db.getLiveConfig(server, model));
		
		units.addElement(new SimpleUnit("openxpki_mysql_password", "proceed",
				"OPENXPKI_PASSWORD=`sudo grep \"passwd\" " + chroot.getChrootDir() + "/etc/openxpki/config.d/system/database.yaml 2>/dev/null | awk '{ print $2 }' | tr -d \"',\"`; [[ -z $OPENXPKI_PASSWORD ]] && OPENXPKI_PASSWORD=`openssl rand -hex 32`",
				"echo $OPENXPKI_PASSWORD", "", "fail",
				"Couldn't set a password for OpenXPKI's database user. The installation will fail."));
		
		units.addAll(db.createDb("openxpki", "openxpki", "ALL", "OPENXPKI_PASSWORD"));

		String databaseConfig = "";
		databaseConfig += "main:\n";
		databaseConfig += "	debug: 0\n";
		databaseConfig += "	type: MySQL\n";
		databaseConfig += "	name: openxpki\n";
		databaseConfig += "	host: localhost\n";
		databaseConfig += "	port: 3306\n";
		databaseConfig += "	user: openxpki\n";
		databaseConfig += "	passwd: ${OPENXPKI_PASSWORD}";
		
		units.addElement(new FileUnit("openxpki_database_creds", "openxpki_installed", databaseConfig, chroot.getChrootDir() + "/etc/openxpki/config.d/system/database.yaml"));

		units.addElement(new SimpleUnit("openxpki_tables", "mariadb_installed",
				"zcat " + chroot.getChrootDir() + "/usr/share/doc/libopenxpki-perl/examples/schema-mysql.sql.gz | mysql -uopenxpki -p${OPENXPKI_PASSWORD} openxpki",
				"mysql -uopenxpki -p${OPENXPKI_PASSWORD} -B -N -e \"SELECT USE openxpki; SHOW TABLES; SELECT FOUND_ROWS();\" 2>&1", "0", "fail"));

		String apacheConfig = "";
		apacheConfig += "Alias /pki /var/www/openxpki\n";
		apacheConfig += "ScriptAlias /scep  /usr/lib/cgi-bin/scep.fcgi\n";
		apacheConfig += "ScriptAlias /soap  /usr/lib/cgi-bin/soap.fcgi\n";
		apacheConfig += "ScriptAlias /rpc  /usr/lib/cgi-bin/rpc.fcgi\n";
		apacheConfig += "ScriptAlias /certep  /usr/lib/cgi-bin/certep.fcgi\n";
		apacheConfig += "ScriptAlias /.well-known/est  /usr/lib/cgi-bin/est.fcgi\n";
		apacheConfig += "ScriptAlias /cgi-bin/ /usr/lib/cgi-bin/\n";
		apacheConfig += "<Directory \\\"/usr/lib/cgi-bin/\\\">\n";
		apacheConfig += "	AllowOverride None\n";
		apacheConfig += "	Options +ExecCGI\n";
		apacheConfig += "	Order allow,deny\n";
		apacheConfig += "	Allow from all\n";
		apacheConfig += "</Directory>";

		units.addElement(new FileUnit("apache_config", "openxpki_installed", apacheConfig, chroot.getChrootDir() + "/etc/apache2/sites-enabled/000-default.conf"));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		model.getServerModel(server).addRouterEgressFirewallRule(server, model, "openxpki_packages", "packages.openxpki.org", new String[]{"80","443"});

		return units;
	}

}
