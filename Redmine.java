package profile;

import java.util.Iterator;
import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.DirOwnUnit;
import core.unit.fs.DirPermsUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileOwnUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;

public class Redmine extends AStructuredProfile {
	
	private Nginx webserver;
	private MariaDB db;
	
	public Redmine() {
		super("redmine");
		
		this.webserver = new Nginx();
		this.db = new MariaDB();
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
				
		units.addAll(webserver.getInstalled(server, model));
		units.addAll(db.getInstalled(server, model));
				
		units.addElement(new InstalledUnit("redmine", "proceed", "redmine-mysql"));
		units.addElement(new InstalledUnit("thin", "redmine_installed", "thin"));
		units.addElement(new InstalledUnit("sendmail", "proceed", "sendmail"));
		
		model.getServerModel(server).getProcessModel().addProcess("sendmail: MTA: accepting connections$");
		model.getServerModel(server).getUserModel().addUsername("smmta");
		model.getServerModel(server).getUserModel().addUsername("smmpa");
		model.getServerModel(server).getUserModel().addUsername("smmsp");
		
		model.getServerModel(server).getProcessModel().addProcess("thin server \\(/var/run/thin/sockets/thin.[0-3].sock\\)$");
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units =  new Vector<IUnit>();
		
		units.addAll(model.getServerModel(server).getBindFsModel().addDataBindPoint(server, model, "redmine_logs", "proceed", "nginx", "nginx", "0644"));
		units.addAll(model.getServerModel(server).getBindFsModel().addDataBindPoint(server, model, "redmine_files", "proceed", "nginx", "nginx", "0755"));
		units.addAll(model.getServerModel(server).getBindFsModel().addDataBindPoint(server, model, "redmine_data", "proceed", "nginx", "nginx", "0755"));

		units.addElement(new SimpleUnit("logs_symlinked", "redmine_installed",
				//We don't really care about logs at this point
				"sudo mv /var/log/redmine/* /media/data/redmine_logs;"
				//Then symlink
				+ "sudo rm -R /var/log/redmine;"
				+ "sudo ln -s /media/data/redmine_logs /var/log/redmine;",
				"sudo [ -L /var/log/redmine ] && echo pass || echo fail", "pass", "pass"));
		
		units.addElement(new SimpleUnit("files_symlinked", "redmine_installed",
				//Move over fresh installation if the files aren't already there
				"if [ ! -d /media/data/redmine_files/default ]; then sudo mv /var/lib/redmine/* /media/data/redmine_files ; fi ;"
				//Then symlink
				+ "sudo rm -R /var/lib/redmine ; sudo ln -s /media/data/redmine_files /var/lib/redmine;",
				"[ -L /var/lib/redmine ] && echo pass || echo fail", "pass", "pass"));

		units.addElement(new SimpleUnit("data_symlinked", "redmine_installed",
				//Move over fresh installation if the files aren't already there
				"if [ ! -d /media/data/redmine_data/config ] ; then sudo mv /usr/share/redmine/* /media/data/redmine_data ; fi ;"
				//Then symlink
				+ "sudo rm -R /usr/share/redmine ; sudo ln -s /media/data/redmine_data /usr/share/redmine;",
				"[ -L /usr/share/redmine ] && echo pass || echo fail", "pass", "pass"));

		units.addElement(new FileOwnUnit("database_config", "redmine_installed", "/etc/redmine/default/database.yml", "nginx"));
		units.addElement(new FileOwnUnit("secret_key", "redmine_installed", "/etc/redmine/default/secret_key.txt", "nginx"));
		
		units.addElement(new SimpleUnit("redmine_mysql_password", "proceed",
				"REDMINE_PASSWORD=`sudo grep \"password\" /usr/share/redmine/instances/default/config/database.yml 2>/dev/null | grep -v \"[*#]\" | awk '{ print $2 }'`; [[ -z $REDMINE_PASSWORD ]] && REDMINE_PASSWORD=`openssl rand -hex 32`",
				"echo $REDMINE_PASSWORD", "", "fail",
				"Couldn't set the Redmine database user's password.  Redmine will be left in a broken state."));
		
		units.addAll(db.createDb("redmine", "redmine", "ALL", "REDMINE_PASSWORD"));

		String dbConfig = "";
		dbConfig += "production:\n";
		dbConfig += "  adapter: mysql2\n";
		dbConfig += "  database: redmine\n";
		dbConfig += "  host: localhost\n";
		dbConfig += "  username: redmine\n";
		dbConfig += "  password: ${REDMINE_PASSWORD}\n"; 
		dbConfig += "  encoding: utf8";
		
		units.addElement(new DirUnit("database_config_dir", "proceed", "/usr/share/redmine/instances/default/config/"));
		units.addElement(new FileUnit("database_config", "mariadb_installed", dbConfig, "/usr/share/redmine/instances/default/config/database.yml"));	
		
		units.addElement(new DirUnit("thin_pid_dir", "thin_installed", "/var/run/thin"));
		units.addElement(new DirOwnUnit("thin_pid_dir", "thin_pid_dir_created", "/var/run/thin", "nginx"));
		units.addElement(new DirPermsUnit("thin_pid_dir_perms", "thin_pid_dir_chowned", "/var/run/thin", "744"));
		
		units.addElement(new DirUnit("thin_sockets_dir", "thin_installed", "/var/run/thin/sockets"));
		units.addElement(new DirOwnUnit("thin_sockets_dir_permissions", "thin_sockets_dir_created", "/var/run/thin/sockets", "nginx", "nginx"));
		
		String thinConfig = "";
		thinConfig += "---\n";
		thinConfig += "chdir: \"/usr/share/redmine\"\n";
		thinConfig += "environment: production\n";
		thinConfig += "timeout: 30\n";
		thinConfig += "log: \"/media/data/redmine-logs/redmine.log\"\n";
		thinConfig += "pid: \"/var/run/thin/redmine.pid\"\n";
		thinConfig += "max_conns: 1024\n";
		thinConfig += "max_persistent_conns: 100\n";
		thinConfig += "require: []\n";
		thinConfig += "wait: 30\n";
		thinConfig += "threadpool_size: 20\n";
		thinConfig += "socket: \"/var/run/thin/sockets/thin.sock\"\n";
		thinConfig += "daemonize: true\n";
		thinConfig += "user: nginx\n";
		thinConfig += "group: nginx\n";
		thinConfig += "servers: 4";
		
		units.addElement(new FileUnit("thin_config", "thin_installed", thinConfig, "/etc/thin2.3/redmine.yml"));
		
		units.addElement(new SimpleUnit("secret_key_base", "proceed",
				"SECRET_KEY_BASE=`grep \"secret_key_base:\" /usr/share/redmine/config/secrets.yml 2>/dev/null | awk '{ print $2 }'`; [[ -z $SECRET_KEY_BASE ]] && SECRET_KEY_BASE=`cd /usr/share/redmine; sudo rake secret`",
				"echo $SECRET_KEY_BASE", "", "fail"));
		
		String thinSecretKey = "";
		thinSecretKey += "production:\n";
		thinSecretKey += "  secret_key_base: ${SECRET_KEY_BASE}";
		
		units.addElement(new FileUnit("thin_secret_key", "thin_installed", thinSecretKey, "/usr/share/redmine/config/secrets.yml"));
		
		String emailConf = "";
		emailConf += "default:\n";
		emailConf += "  email_delivery:\n";
		emailConf += "    delivery_method: :sendmail";
		units.addElement(new FileUnit("redmine_allow_email", "sendmail_installed", emailConf, "/usr/share/redmine/config/secrets.yml"));

		String nginxConf = "";
		nginxConf += "upstream thin_cluster {\n";
		nginxConf += "    server unix:/var/run/thin/sockets/thin.0.sock;\n";
		nginxConf += "    server unix:/var/run/thin/sockets/thin.1.sock;\n";
		nginxConf += "    server unix:/var/run/thin/sockets/thin.2.sock;\n";
		nginxConf += "    server unix:/var/run/thin/sockets/thin.3.sock;\n";
		nginxConf += "}\n";
		nginxConf += "\n";
		nginxConf += "server {\n";
		nginxConf += "    listen *:80 default;\n"; 
		nginxConf += "    server_name _;\n";
		nginxConf += "\n";
		nginxConf += "    root /usr/share/redmine/public;\n"; 
		nginxConf += "\n";
		nginxConf += "    location / {\n"; 
		nginxConf += "        try_files $uri/index.html $uri.html $uri @cluster;\n"; 
		nginxConf += "    }\n";
		nginxConf += "\n";
		nginxConf += "    location @cluster {\n"; 
		nginxConf += "        proxy_pass http://thin_cluster;\n"; 
		nginxConf += "    }\n";
		nginxConf += "}";
		
		webserver.addLiveConfig("default", nginxConf);
		units.addAll(webserver.getPersistentConfig(server, model));
		units.addAll(db.getPersistentConfig(server, model));
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(webserver.getLiveConfig(server, model));
		units.addAll(db.getLiveConfig(server, model));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units    = new Vector<IUnit>();
		Vector<String> routers = model.getRouters();
		Iterator<String> itr   = routers.iterator();

		String cleanName   = server.replaceAll("-",  "_");
		String egressChain = cleanName + "_egress";
		
		while (itr.hasNext()) {
			String router = itr.next();

			model.getServerModel(router).getFirewallModel().addFilter(server + "_allow_email", egressChain,
				"-p tcp"
				+ " --dport 25"
				+ " -j ACCEPT");
		}
		
		units.addAll(webserver.getPersistentFirewall(server, model));

		return units;
	}

}