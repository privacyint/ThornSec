package profile;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import core.iface.IUnit;
import core.model.FirewallModel;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.DirUnit;
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
		
		units.addAll(model.getServerModel(server).getBindFsModel().addDataBindPoint(server, model, "www", "proceed", "nginx", "nginx", "0750"));
		units.addAll(model.getServerModel(server).getBindFsModel().addLogBindPoint(server, model, "nginx", "proceed", "nginx", "0600"));

		model.getServerModel(server).getAptSourcesModel().addAptSource(server, model, "nginx", "proceed", "deb http://nginx.org/packages/mainline/debian/ stretch nginx", "keyserver.ubuntu.com", "ABF5BD827BD9BF62");
		units.addElement(new InstalledUnit("nginx", "nginx_gpg", "nginx"));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units =  new Vector<IUnit>();
		
		String nginxConf = "";
		nginxConf += "user nginx;\n";
		nginxConf += "\n";
		nginxConf += "worker_processes " + model.getData().getCpus(server) + ";\n";
		nginxConf += "\n";
		nginxConf += "error_log  /var/log/nginx/error.log warn;\n";
		nginxConf += "pid        /var/run/nginx.pid;\n";
		nginxConf += "\n";
		nginxConf += "include includes/customNginxBlockParams;\n";
		nginxConf += "\n";
		nginxConf += "events {\n";
		nginxConf += "    worker_connections $(ulimit -n);\n";
		nginxConf += "    multi_accept       on;\n";
		nginxConf += "}\n";
		nginxConf += "\n";
		nginxConf += "http {\n";                                                                                                                       
		nginxConf += "    include       /etc/nginx/mime.types;\n";                                                                                     
		nginxConf += "    default_type  application/octet-stream;\n";                                                                                  
		nginxConf += "\n";		                                                                                                                             
		nginxConf += "    log_format  main  '\\$remote_addr - \\$remote_user [\\$time_local] \"\\$request\" '\n";                                                
		nginxConf += "                      '\\$status \\$body_bytes_sent \"\\$http_referer\" '\n";
		nginxConf += "                      '\"\\$http_user_agent\" \"\\$http_x_forwarded_for\"';\n";
		nginxConf += "\n";
		nginxConf += "    access_log  /var/log/nginx/access.log main buffer=16k;\n";
		nginxConf += "\n";
		nginxConf += "    sendfile on;\n";
		nginxConf += "\n";
		nginxConf += "    keepalive_timeout 65;\n";
		nginxConf += "\n";
		nginxConf += "    server_tokens off;\n";
		nginxConf += "\n";
		nginxConf += "    include includes/customHttpBlockParams;\n";
		nginxConf += "\n";
		nginxConf += "    include /etc/nginx/conf.d/*.conf;\n";
		nginxConf += "}";
		
		units.addElement(model.getServerModel(server).getConfigsModel().addConfigFile("nginx_conf", "nginx_installed", nginxConf, "/etc/nginx/nginx.conf"));
		
		units.addElement(new SimpleUnit("nginx_custom_nginx", "nginx_installed",
				"sudo touch /etc/nginx/includes/customNginxBlockParams",
				"[ -f /etc/nginx/includes/customNginxBlockParams ] && echo pass || echo fail", "pass", "pass"));

		units.addElement(new SimpleUnit("nginx_custom_http", "nginx_installed",
				"sudo touch /etc/nginx/includes/customHttpBlockParams",
				"[ -f /etc/nginx/includes/customHttpBlockParams ] && echo pass || echo fail", "pass", "pass"));

		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new RunningUnit("nginx", "nginx", "nginx"));
		
		model.getServerModel(server).getProcessModel().addProcess("nginx: master process /usr/sbin/nginx -g daemon on; master_process on;$");
		model.getServerModel(server).getProcessModel().addProcess("nginx: master process /usr/sbin/nginx -c /etc/nginx/nginx.conf$");
		model.getServerModel(server).getProcessModel().addProcess("nginx: worker process *$");
		
		units.addElement(new DirUnit("nginx_custom_conf_dir", "proceed", "/etc/nginx/conf.d/custom"));
		
		if (liveConfig.size() > 0) {		
			for (Map.Entry<String, String> config : liveConfig.entrySet()) {
				units.addElement(model.getServerModel(server).getConfigsModel().addConfigFile("nginx_live_" + config.getKey(), "nginx_installed", config.getValue(), "/etc/nginx/conf.d/" + config.getKey() + ".conf"));
				
				units.addElement(new SimpleUnit("nginx_custom_" + config.getKey(), "nginx_custom_conf_dir",
						"sudo touch /etc/nginx/conf.d/custom/" + config.getKey() + ".conf",
						"[ -f /etc/nginx/conf.d/custom/" + config.getKey() + ".conf ] && echo pass || echo fail", "pass", "pass"));
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
			conf += "\n";
			conf += "    include custom/default.conf;\n";
			conf += "}";
			
			units.addElement(model.getServerModel(server).getConfigsModel().addConfigFile("nginx_default", "nginx_installed", conf, "/etc/nginx/conf.d/default.conf"));
			
			units.addElement(new SimpleUnit("nginx_custom_default", "nginx_custom_conf_dir",
					"sudo touch /etc/nginx/conf.d/custom/default.conf",
					"[ -f /etc/nginx/conf.d/custom/default.conf ] && echo pass || echo fail", "pass", "pass"));
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
		model.getServerModel(server).addRouterEgressFirewallRule(server, model, "allow_nginx", "nginx.org", new String[]{"80","443"});

		return units;
	}
}