package profile;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import core.iface.IUnit;
import core.model.FirewallModel;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;

public class Nginx extends AStructuredProfile {
	private HashMap<String, String> liveConfig;
	
	public Nginx() {
		super("nginx");
		
		this.liveConfig = new HashMap<String, String>();
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new SimpleUnit("nginx_user", "proceed",
				"sudo useradd -r -d /media/data/www nginx",
				"id nginx 2>&1", "id: ‘nginx’: no such user", "fail",
				"The nginx user couldn't be added.  This will cause all sorts of errors."));
		
		model.getServerModel(server).getUserModel().addUsername("nginx");
		
		units.addAll(model.getServerModel(server).getBindFsModel().addBindPoint(server, model, "nginx", "proceed", "/media/metaldata/www", "/media/data/www", "nginx", "nginx", "0755", "/media/metaldata"));

		model.getServerModel(server).getAptSourcesModel().addAptSource(server, model, "nginx", "proceed", "deb http://nginx.org/packages/mainline/debian/ stretch nginx", "keys.gnupg.net", "ABF5BD827BD9BF62");
		units.addElement(new InstalledUnit("nginx", "nginx_gpg", "nginx"));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units =  new Vector<IUnit>();
				
		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new RunningUnit("nginx", "nginx", "nginx"));
		
		model.getServerModel(server).getProcessModel().addProcess("nginx: master process /usr/sbin/nginx -g daemon on; master_process on;$");
		model.getServerModel(server).getProcessModel().addProcess("nginx: master process /usr/sbin/nginx -c /etc/nginx/nginx.conf$");
		model.getServerModel(server).getProcessModel().addProcess("nginx: worker process *$");
		
		if (liveConfig.size() > 0) {		
			for (Map.Entry<String, String> entry : liveConfig.entrySet()) {
				units.addElement(new FileUnit("nginx_live_config_" + entry.getKey(), "nginx_installed", entry.getValue(), "/etc/nginx/conf.d/" + entry.getKey() + ".conf"));
			}
		}
		else {
			String conf = "";
			conf += "server {\n";
			conf += "    listen 80;\n";
			conf += "    server_name _;\n";
			conf += "\n";
			conf += "    location / {\n";
			conf += "        root /media/data/www;\n";
			conf += "        index index.html index.htm;\n";
			conf += "    }\n";
			conf += "\n";
			conf += "    error_page 500 502 503 504 /50x.html;\n";
			conf += "    location = /50x.html {\n";
			conf += "        root /usr/share/nginx/html;\n";
			conf += "    }\n";
			conf += "}";
			
			units.addElement(new FileUnit("nginx_default_config", "nginx_installed", conf, "/etc/nginx/conf.d/default.conf"));
		}
		
		return units;
	}
	
	public void addLiveConfig(String file, String config) {
		liveConfig.put(file, config);
	}

	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		FirewallModel fm = model.getServerModel(server).getFirewallModel();
		//Allow the box to tx/rx on :80&&:443
		units.add(fm.addFilterInput(server,
				"-p tcp"
				+ " -m state --state NEW,ESTABLISHED"
				+ " -m tcp -m multiport --dports 80,443"
				+ " -j ACCEPT"));
		units.add(fm.addFilterOutput(server,
				"-p tcp"
				+ " -m state --state ESTABLISHED,RELATED"
				+ " -m tcp -m multiport --sports 80,443"
				+ " -j ACCEPT"));
		
		for (String router : model.getRouters()) {
			
			FirewallModel routerFm = model.getServerModel(router).getFirewallModel();
		
			routerFm.addFilter(server + "_ingress_80_443_allow", server + "_ingress",
					"-p tcp"
					+ " -m state --state NEW,ESTABLISHED,RELATED"
					+ " -m tcp -m multiport --dports 80,443"
					+ " -j ACCEPT");
			routerFm.addFilter(server + "_egress_80_443_allow", server + "_egress",
					"-p tcp"
					+ " -m state --state ESTABLISHED,RELATED"
					+ " -m tcp -m multiport --sports 80,443"
					+ " -j ACCEPT");
		}
		//Allow the server to call out to nginx.org to download mainline
		model.getServerModel(server).addRouterFirewallRule(server, model, "allow_nginx", "nginx.org", new String[]{"80","443"});

		return units;
	}
}