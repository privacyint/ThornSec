package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.fs.FileEditUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;

public class Git extends AStructuredProfile {

	private Nginx webserver;
	
	public Git() {
		super("git");
		
		this.webserver = new Nginx();
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new InstalledUnit("java", "proceed", "default-jre-headless"));
				
		model.getServerModel(server).getAptSourcesModel().addAptSource(server, model, "scm_manager", "proceed", "deb http://maven.scm-manager.org/nexus/content/repositories/releases ./", "hkp://keyserver.ubuntu.com:80", "D742B261");
		
		units.addElement(new InstalledUnit("scm_admin", "scm_manager_gpg", "scm-server"));
		
		units.addAll(webserver.getInstalled(server, model));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units =  new Vector<IUnit>();
				
		units.addAll(model.getServerModel(server).getBindFsModel().addBindPoint(server, model, "scm_base_dir", "proceed", "/media/metaldata/scm", "/media/data/scm", "scm", "scm", "0755"));
		
		units.addElement(new FileEditUnit("scm_admin_home", "scm_base_dir_mounted", "export SCM_HOME=/var/lib/scm", "export SCM_HOME=/media/data/scm", "/etc/default/scm-server",
				"Couldn't change scm-manager's data directory.  Its data will be stored in the VM only."));
		
		units.addElement(new RunningUnit("scm_server", "scm-server", "scm-server"));
		
		String nginxConf = "";
		nginxConf += "server {\n";
		nginxConf += "    listen 80;\n";
		nginxConf += "    server_name _;\n";
		nginxConf += "\n";
		nginxConf += "    location / {\n";
		nginxConf += "        proxy_pass          http://localhost:8080/scm/;\n";
		nginxConf += "        proxy_set_header    Host \\$host;\n";
		nginxConf += "        proxy_set_header    X-Real-IP \\$remote_addr;\n";
		nginxConf += "        proxy_next_upstream error timeout invalid_header http_500 http_502 http_503 http_504 http_404;\n";
		nginxConf += "        proxy_redirect      off;\n";
		nginxConf += "        proxy_cache_valid   200 120m;\n";
		nginxConf += "        proxy_buffering     on;\n";
		nginxConf += "        proxy_set_header    Accept-Encoding \"\";\n";
		nginxConf += "    }\n";
		nginxConf += "}";
		
		webserver.addLiveConfig("default", nginxConf);
		units.addAll(webserver.getPersistentConfig(server, model));
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(webserver.getLiveConfig(server, model));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(webserver.getPersistentFirewall(server, model));

		return units;
	}

}