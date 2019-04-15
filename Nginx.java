package profile;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.model.ServerModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.CustomFileUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;

public class Nginx extends AStructuredProfile {
	private HashMap<String, String> liveConfig;
	//private Jail jail;
	
	public Nginx(ServerModel me, NetworkModel networkModel) {
		super("nginx", me, networkModel);
		
		this.liveConfig = new HashMap<String, String>();
	}

	protected Vector<IUnit> getInstalled() {
		Vector<IUnit> units = new Vector<IUnit>();

		//If we don't give the nginx user a home dir, it can cause problems with npm etc
		units.addElement(new SimpleUnit("nginx_user", "proceed",
				"sudo useradd -r -d /media/data/www nginx",
				"id nginx 2>&1", "id: ‘nginx’: no such user", "fail",
				"The nginx user couldn't be added.  This will cause all sorts of errors."));

		((ServerModel)me).getUserModel().addUsername("nginx");
		
		units.addAll(((ServerModel)me).getBindFsModel().addLogBindPoint("nginx", "proceed", "nginx", "0600"));

		units.addElement(new InstalledUnit("nginx", "nginx_pgp", "nginx"));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig() {
		Vector<IUnit> units =  new Vector<IUnit>();
		
		units.addAll(((ServerModel)me).getBindFsModel().addDataBindPoint("www", "proceed", "nginx", "nginx", "0750"));
		
		units.addAll(((ServerModel)me).getBindFsModel().addDataBindPoint("nginx_includes", "nginx_installed", "nginx", "nginx", "0750"));
		units.addAll(((ServerModel)me).getBindFsModel().addDataBindPoint("nginx_modules", "nginx_installed", "nginx", "nginx", "0750"));
		units.addElement(new SimpleUnit("nginx_modules_symlink", "nginx_modules_data_bindpoint_created",
				"sudo rm /etc/nginx/modules;"
				+ "sudo ln -s /media/data/nginx_modules/ /etc/nginx/modules",
				"readlink /etc/nginx/modules", "/media/data/nginx_modules/", "pass"));
		
		units.addElement(new CustomFileUnit("nginx_custom_nginx", "nginx_includes_data_bindpoint_created", "/media/data/nginx_includes/customNginxBlockParams"));
		units.addElement(new CustomFileUnit("nginx_custom_http", "nginx_includes_data_bindpoint_created", "/media/data/nginx_includes/customHttpBlockParams"));
		
		String nginxConf = "";
		nginxConf += "user nginx;\n";
		nginxConf += "\n";
		nginxConf += "worker_processes " + networkModel.getData().getCpus(me.getLabel()) + ";\n";
		nginxConf += "\n";
		nginxConf += "error_log  /var/log/nginx/error.log warn;\n";
		nginxConf += "pid        /var/run/nginx.pid;\n";
		nginxConf += "\n";
		nginxConf += "include /media/data/nginx_includes/customNginxBlockParams;\n";
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
		nginxConf += "    client_max_body_size 0;\n";
		nginxConf += "\n";
		nginxConf += "    include /media/data/nginx_includes/customHttpBlockParams;\n";
		nginxConf += "\n";
		nginxConf += "    include /etc/nginx/conf.d/*.conf;\n";
		nginxConf += "}";
		
		units.addElement(((ServerModel)me).getConfigsModel().addConfigFile("nginx_conf", "nginx_installed", nginxConf, "/etc/nginx/nginx.conf"));
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		((ServerModel)me).getProcessModel().addProcess("nginx: master process /usr/sbin/nginx -g daemon on; master_process on;$");
		((ServerModel)me).getProcessModel().addProcess("nginx: master process /usr/sbin/nginx -c /etc/nginx/nginx.conf$");
		((ServerModel)me).getProcessModel().addProcess("nginx: worker process *$");
		
		units.addAll(((ServerModel)me).getBindFsModel().addDataBindPoint("nginx_custom_conf_d", "nginx_installed", "nginx", "nginx", "0750"));
		
		if (liveConfig.size() > 0) {		
			for (Map.Entry<String, String> config : liveConfig.entrySet()) {
				units.addElement(((ServerModel)me).getConfigsModel().addConfigFile("nginx_live_" + config.getKey(), "nginx_installed", config.getValue(), "/etc/nginx/conf.d/" + config.getKey() + ".conf"));

				units.addElement(new CustomFileUnit("nginx_custom_" + config.getKey(), "nginx_custom_conf_d_data_bindpoint_created", "/media/data/nginx_custom_conf_d/" + config.getKey() + ".conf"));
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
			conf += "    include /media/data/nginx_custom_conf_d/default.conf;\n";
			conf += "}";
			
			units.addElement(((ServerModel)me).getConfigsModel().addConfigFile("nginx_default", "nginx_installed", conf, "/etc/nginx/conf.d/default.conf"));
			
			units.addElement(new CustomFileUnit("nginx_custom_default", "nginx_custom_conf_d_data_bindpoint_created", "/media/data/nginx_custom_conf_d/default.conf"));
		}
		
		units.addElement(new RunningUnit("nginx", "nginx", "nginx"));

		return units;
	}
	
	public void addLiveConfig(String file, String config) {
		liveConfig.put(file, config);
	}

	public Vector<IUnit> getNetworking() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		me.addRequiredListen(80);

		((ServerModel)me).getAptSourcesModel().addAptSource("nginx", "proceed", "deb http://nginx.org/packages/mainline/debian/ stretch nginx", "keyserver.ubuntu.com", "ABF5BD827BD9BF62");

		//Allow the server to call out to nginx.org to download mainline
		me.addRequiredEgress("nginx.org");
		
		return units;
	}
}
