package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;

public class SVN extends AStructuredProfile {

	
	public SVN() {
		super("svn");

	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new InstalledUnit("apache", "apache2"));
		units.addElement(new RunningUnit("apache", "apache2", "apache2"));
		model.getServerModel(server).getProcessModel().addProcess("/usr/sbin/apache2 -k start$");
		
		units.addElement(new InstalledUnit("svn", "proceed", "subversion"));
		units.addElement(new InstalledUnit("ca_certificates", "proceed", "ca-certificates"));
		units.addElement(new InstalledUnit("libapache2_svn", "apache_installed", "libapache2-svn"));
		units.addElement(new InstalledUnit("unzip", "proceed", "unzip"));

		
		units.addElement(new InstalledUnit("php5_fpm", "php5-fpm"));
		units.addElement(new InstalledUnit("php5_base", "php5"));
		units.addElement(new InstalledUnit("php5_apcu", "php5-apcu"));
				
		model.getServerModel(server).getProcessModel().addProcess("php-fpm: master process \\(/etc/php5/fpm/php-fpm\\.conf\\) *$");
		model.getServerModel(server).getProcessModel().addProcess("php-fpm: pool www *$");
		
		units.addElement(new InstalledUnit("php5_apache", "apache_installed", "libapache2-mod-php5"));

		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units =  new Vector<IUnit>();
		
		units.addAll(model.getServerModel(server).getBindFsModel().addBindPoint(server, model, "apache_webroot", "proceed", "/media/metaldata/www", "/media/data/www", "www-data", "www-data", "0755", "/media/metaldata"));
		
		units.addElement(new SimpleUnit("apache_mod_headers_enabled", "apache_installed",
				"sudo a2enmod headers;",
				"sudo apache2ctl -M | grep headers", "", "fail"));
		
		units.addElement(new SimpleUnit("apache_mod_dav_enabled", "libapache2_svn_installed",
				"sudo a2enmod dav;",
				"sudo apache2ctl -M | grep dav", "", "fail"));

		units.addElement(new SimpleUnit("apache_mod_dav_fs_enabled", "libapache2_svn_installed",
				"sudo a2enmod dav_fs;",
				"sudo apache2ctl -M | grep dav_fs", "", "fail"));

		units.addElement(new SimpleUnit("apache_mod_auth_digest_enabled", "apache_installed",
				"sudo a2enmod auth_digest;",
				"sudo apache2ctl -M | grep auth_digest", "", "fail"));

		units.addElement(new SimpleUnit("apache_mod_dav_svn_enabled", "libapache2_svn_installed",
				"sudo a2enmod dav_svn;",
				"sudo apache2ctl -M | grep dav_svn", "", "fail"));
		
		//Turn off Apache version on error pages
		units.addElement(new SimpleUnit("hide_apache_version", "apache_installed",
				"sudo bash -c '"
								+ "echo \"ServerSignature Off\" >> /etc/apache2/apache2.conf;"
				+ "'",
				"grep 'ServerSignature' /etc/apache2/apache2.conf | awk '{ print $2 }'","Off","pass"));

		//Turn off Apache version in headers
		units.addElement(new SimpleUnit("hide_apache_version_headers", "apache_installed",
				"sudo bash -c '"
								+ "echo \"ServerTokens Prod\" >> /etc/apache2/apache2.conf;"
				+ "'",
				"grep 'ServerTokens' /etc/apache2/apache2.conf | awk '{ print $2 }'","Prod","pass"));
		
		units.addElement(new SimpleUnit("php5_version_header_off", "apache_installed",
				"sudo sed -i 's/expose_php = On/expose_php = Off/g' /etc/php5/apache2/php.ini;"
				+ "sudo service apache2 restart; sudo service php5-fpm restart;",
				"grep 'expose_php' /etc/php5/apache2/php.ini | awk '{ print $3 }'","Off","pass"));
		
		units.addAll(model.getServerModel(server).getBindFsModel().addBindPoint(server, model, "svn_base_dir", "proceed", "/media/metaldata/svn", "/media/data/svn", "www-data", "www-data", "0755", "/media/metaldata"));

		units.addElement(new DirUnit("svn_repo_dir", "svn_base_dir_mounted", "/media/data/svn/repos"));
		units.addElement(new DirUnit("svn_credentials_dir", "svn_base_dir_mounted", "/media/data/svn/credentials"));
		
		units.addElement(new SimpleUnit("svn_admin_downloaded", "apache_installed",
				"sudo wget 'http://downloads.sourceforge.net/project/ifsvnadmin/svnadmin-1.6.2.zip?ts=1479474964&use_mirror=kent' -O /root/svnadmin.zip;",
				"sudo [ -f /root/svnadmin.zip ] && echo pass;", "pass", "pass"));

		units.addElement(new SimpleUnit("svn_admin_checksum", "svn_admin_downloaded",
				"",
				"sudo sha512sum /root/svnadmin.zip | awk '{print $1}';", "065666dcddb96990b4bef37b5d6bf1689811eb3916a8107105935d9e6f8e05b9f99e6fdd8b4522dffab0ae8b17cfade80db891bd2a7ba7f49758f2133e4d26fa", "pass"));

		units.addElement(new SimpleUnit("svn_admin_unzipped", "svn_admin_checksum",
				"sudo unzip /root/svnadmin.zip -d /media/data/www/;"
				+ "sudo mv /media/data/www/iF.SVNAdmin-stable-1.6.2 /media/data/www/admin;",
				"[ -d /media/data/www/admin ] && echo pass;", "pass", "pass"));
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String apacheConf = "";
		apacheConf += "<VirtualHost *:80>\n";
		apacheConf += "    DocumentRoot /media/data/www\n";
		apacheConf += "    EnableSendfile off\n";
        apacheConf += "    <Directory \"/media/data/www\">\n";
        apacheConf += "        AllowOverride All\n";
        apacheConf += "        Require all granted\n";
        apacheConf += "    </Directory>\n";
        apacheConf += "</VirtualHost>";
        
		units.addElement(new FileUnit("default_apache_conf", "apache_installed", apacheConf, "/etc/apache2/sites-available/000-default.conf"));
		
		String davConf = "";
		davConf = "<Location /repos >\n";
		davConf += "	DAV svn\n";
		davConf += "	SVNListParentPath On\n";
		davConf += "	SVNParentPath /media/data/svn/repos\n";
		davConf += "	AuthType Digest\n";
		davConf += "	AuthName pisvn\n";
		davConf += "	AuthUserFile /media/data/svn/credentials/svn.pass\n";
		davConf += "	AuthzSVNAccessFile /media/data/svn/credentials/svn.auth\n";
		davConf += "	Require valid-user\n";
		davConf += "</Location>";

		units.addElement(new FileUnit("apache_svn_conf", "apache_mod_dav_svn_enabled", davConf, "/etc/apache2/mods-available/dav_svn.conf"));
		
		units.addElement(new SimpleUnit("svn_admin_password", "apache_installed",
				"SVN_ADMIN_PASSWORD=`openssl rand -hex 32`;"
				+ "SVN_DIGEST=`echo -n \"admin:pisvn:\" && echo -n \"admin:pisvn:${SVN_ADMIN_PASSWORD}\" | md5sum | awk '{print $1}'`;" //Can't pass to htdigest so do it like this
				+ "echo 'SVN Admin password is:' ${SVN_ADMIN_PASSWORD};"
				+ "echo ${SVN_DIGEST} | sudo tee /media/data/svn/credentials/svn.pass > /dev/null;"
				+ "sudo chown www-data:www-data /media/data/svn/credentials/svn.pass;",
				"[ -f /media/data/svn/credentials/svn.pass ] && echo pass;", "pass", "pass"));

		units.addElement(new SimpleUnit("svn_auth_file", "apache_installed",
			"sudo touch /media/data/svn/credentials/svn.auth;"
			+ "sudo chown www-data:www-data /media/data/svn/credentials/svn.auth;",
			"sudo stat -c %U /media/data/svn/credentials/svn.auth;", "www-data", "pass"));
		
		String svnConf = "";
		svnConf += "[Common]\n";
		svnConf += "FirstStart=0\n";
		svnConf += "BackupFolder=./data/backup/\n";
		svnConf += "\n";
		svnConf += "[Translation]\n";
		svnConf += "Directory=./translations/\n";
		svnConf += "\n";
		svnConf += "[Engine:Providers]\n";
		svnConf += "AuthenticationStatus=basic\n";
		svnConf += "UserViewProviderType=digest\n";
		svnConf += "UserEditProviderType=digest\n";
		svnConf += "GroupViewProviderType=svnauthfile\n";
		svnConf += "GroupEditProviderType=svnauthfile\n";
		svnConf += "AccessPathViewProviderType=svnauthfile\n";
		svnConf += "AccessPathEditProviderType=svnauthfile\n";
		svnConf += "RepositoryViewProviderType=svnclient\n";
		svnConf += "RepositoryEditProviderType=svnclient\n";
		svnConf += "\n";
		svnConf += "[ACLManager]\n";
		svnConf += "UserRoleAssignmentFile=./data/userroleassignments.ini\n";
		svnConf += "\n";
		svnConf += "[Subversion]\n";
		svnConf += "SVNAuthFile=/media/data/svn/credentials/svn.auth\n";
		svnConf += "\n";
		svnConf += "[Repositories:svnclient]\n";
		svnConf += "SVNParentPath=/media/data/svn/repos\n";
		svnConf += "SvnExecutable=/usr/bin/svn\n";
		svnConf += "SvnAdminExecutable=/usr/bin/svnadmin\n";
		svnConf += "\n";
		svnConf += "[Users:digest]\n";
		svnConf += "SVNUserDigestFile=/media/data/svn/credentials/svn.pass\n";
		svnConf += "SVNDigestRealm=pisvn\n";
		svnConf += "\n";
		svnConf += "[GUI]\n";
		svnConf += "RepositoryDeleteEnabled=false\n";
		svnConf += "RepositoryDumpEnabled=false\n";
		svnConf += "AllowUpdateByGui=true";

		units.addElement(new FileUnit("svn_admin_config", "svn_admin_unzipped", svnConf, "/media/data/www/admin/data/config.ini"));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.add(model.getServerModel(server).getFirewallModel().addFilterInput(server, "-p tcp --dport 80 -m state --state NEW,ESTABLISHED -j ACCEPT"));
		units.add(model.getServerModel(server).getFirewallModel().addFilterInput(server, "-p tcp --dport 443 -m state --state NEW,ESTABLISHED -j ACCEPT"));
		units.add(model.getServerModel(server).getFirewallModel().addFilterOutput(server, "-p tcp --sport 80 -m state --state ESTABLISHED -j ACCEPT"));
		units.add(model.getServerModel(server).getFirewallModel().addFilterOutput(server, "-p tcp --sport 443 -m state --state ESTABLISHED -j ACCEPT"));

		return units;
	}

}