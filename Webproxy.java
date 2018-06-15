package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.FirewallModel;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.fs.DirUnit;
import core.unit.pkg.InstalledUnit;

public class Webproxy extends AStructuredProfile {

	private Nginx webserver;
	private String liveConfig;
	
	public Webproxy() {
		super("webproxy");

		this.webserver = new Nginx();
		this.liveConfig = "";
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(webserver.getInstalled(server, model));
		units.addElement(new InstalledUnit("openssl", "openssl"));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units =  new Vector<IUnit>();
		
		units.addAll(webserver.getPersistentConfig(server, model));

		String sslConfig = "";
		sslConfig += "server {\n";
		sslConfig += "    listen 80 default;\n";
		sslConfig += "    return 301 https://\\$host\\$request_uri;\n";
		sslConfig += "}";

		webserver.addLiveConfig("default", sslConfig);
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		//First, build our ssl config
		units.addElement(new DirUnit("nginx_ssl_include_dir", "proceed", "/etc/nginx/includes"));
		
		String sslConf = "";
		sslConf += "    ssl_session_timeout 1d;\n";
		sslConf += "    ssl_session_cache shared:SSL:10m;\n";
		sslConf += "    ssl_session_tickets off;\n";
		sslConf += "\n";
		sslConf += "    ssl_protocols TLSv1 TLSv1.1 TLSv1.2;\n";
		sslConf += "    ssl_ciphers 'EECDH+AESGCM:EDH+AESGCM:AES256+EECDH:AES256+EDH';\n";
		sslConf += "    ssl_prefer_server_ciphers on;\n";
		sslConf += "    ssl_ecdh_curve auto;\n";
		sslConf += "\n";
		sslConf += "    add_header Strict-Transport-Security 'max-age=63072000; includeSubDomains; preload';\n";
		sslConf += "\n";
		sslConf += "    #ssl_stapling on;\n";
		sslConf += "    #ssl_stapling_verify on;\n";
		sslConf += "    resolver " + model.getData().getDNS() + " valid=300s;\n";
		sslConf += "    resolver_timeout 5s;\n";
		sslConf += "\n";
		sslConf += "    add_header X-Frame-Options 'SAMEORIGIN' always;\n";
		sslConf += "    add_header X-Content-Type-Options 'nosniff' always;\n";
		sslConf += "    add_header X-XSS-Protection '1; mode=block' always;\n";
		sslConf += "    add_header X-Robots-Tag 'none' always;\n";
		sslConf += "    add_header X-Download-Options 'noopen' always;\n";
		sslConf += "    add_header X-Permitted-Cross-Domain-Policies 'none' always;";
	    
		units.addElement(model.getServerModel(server).getConfigsModel().addConfigFile("nginx_ssl", "proceed", sslConf, "/etc/nginx/includes/ssl_params"));

		if (this.liveConfig.equals("")) {
			//Now build per-host specific shit
			String[] backends = model.getData().getPropertyArray(server, "proxy");
			
			for (int i = 0; i < backends.length; ++i) {
				String canonicalName = backends[i];
				String[] cnames = model.getData().getCnames(canonicalName);
				String domain = model.getData().getDomain(canonicalName);
				String nginxConf = "";
			
				units.addAll(model.getServerModel(server).getBindFsModel().addBindPoint(server, model, canonicalName + "_tls_certs", "proceed", "/media/metaldata/tls/" + canonicalName, "/media/data/tls/" + canonicalName, "root", "root", "600", "/media/metaldata", false));
								
				//Generated from https://mozilla.github.io/server-side-tls/ssl-config-generator/ (Nginx/Modern) & https://cipherli.st/
				nginxConf = "server {\n";
				nginxConf += "    listen 443 ssl http2";
				if (i == 0) {
					nginxConf += " default"; //We need this to be here, or it'll crap out :'(
				}
				nginxConf += ";\n";

				nginxConf += "    server_name " + canonicalName + "." + domain;
				
				for (String cname : cnames) {
					nginxConf += (cname.equals("")) ? " " : " " + cname + ".";
					nginxConf += domain;
				}
				
				nginxConf += ";\n";
			
				//We use the Let's Encrypt naming convention here, in case we want to install it later
				nginxConf += "    include /etc/nginx/includes/ssl_params;\n";
				nginxConf += "    ssl_certificate /media/data/tls/" + canonicalName + "/fullchain.pem;\n"; 
				nginxConf += "    ssl_certificate_key /media/data/tls/" + canonicalName + "/privkey.pem;\n";
				nginxConf += "    ssl_trusted_certificate /media/data/tls/" + canonicalName + "/stapling.pem;\n";
				nginxConf += "\n";
				nginxConf += "    location / {\n";
				nginxConf += "        proxy_pass              http://" + model.getServerModel(canonicalName).getIP() + ";\n";
				nginxConf += "        proxy_set_header        Host \\$host;\n";
				nginxConf += "        proxy_set_header        X-Real-IP \\$remote_addr;\n";
				nginxConf += "        proxy_set_header        X-Forwarded-For \\$remote_addr;\n";
				nginxConf += "        proxy_request_buffering off;\n";
				nginxConf += "        proxy_buffering         off;\n";
				nginxConf += "        client_max_body_size    0;\n";
				nginxConf += "        proxy_http_version      1.1;\n";
				nginxConf += "        proxy_connect_timeout   3000;\n";
				nginxConf += "        proxy_send_timeout      3000;\n";
				nginxConf += "        proxy_read_timeout      3000;\n";
				nginxConf += "        send_timeout            3000;\n";
				nginxConf += "    }\n";
				nginxConf += "}";
				
				webserver.addLiveConfig(canonicalName, nginxConf);
			}
		}
		else {
			webserver.addLiveConfig("default", this.liveConfig);
		}
		
		units.addAll(webserver.getLiveConfig(server, model));

		return units;
	}
	
	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(webserver.getPersistentFirewall(server, model));
		
		model.getServerModel(server).addRouterFirewallRule(server, model, "allow_tor_check_for_upgrade", "check.torproject.org", new String[]{"80","443"});

		String[] backends = model.getData().getPropertyArray(server, "proxy");
		
		//DNAT the external IP if it's given
		if (model.getData().getExternalIp(server) != null) {
			for (String router : model.getRouters()) {
				FirewallModel fm = model.getServerModel(router).getFirewallModel();
				
				fm.addNatPrerouting("dnat_" + model.getData().getExternalIp(server),
						"-d " + model.getData().getExternalIp(server)
						+ " -p tcp"
						+ " -m multiport"
						+ " --dports 80,443"
						+ " -j DNAT --to-destination " + model.getServerModel(server).getIP());
			}
		}
		
		//Do we have any users?
		int users = 0;
		for (String device : model.getDeviceLabels()) {
			switch (model.getDeviceModel(device).getType()) {
				case "superuser":
				case "user":
					++users;
					break;
			}
		}
		
		//If so, they should be able to access these services internally, too, so DNAT accordingly
		if (users > 0){
			for (String router : model.getRouters()) {
				for (String backend : backends) {
					String lbIP      = model.getServerModel(server).getIP();
					String backendIP = model.getServerModel(backend).getIP();
					
					//DNAT all traffic to our lb
					model.getServerModel(router).getFirewallModel().addNatPrerouting("dnat_" + backend,
							"-d " + backendIP
							+ " ! -s " + lbIP //Make sure it doesn't end up in a DNAT loop!
							+ " -p tcp"
							+ " -m multiport"
							+ " --dports 80,443"
							+ " -j DNAT --to-destination " + lbIP);
				}
			}
		}

		for (String router : model.getRouters()) {
			
			FirewallModel routerFm = model.getServerModel(router).getFirewallModel();
			
			for (String backend : backends) {
				String backendIP       = model.getServerModel(backend).getIP();
				String backendFwdChain = backend + "_fwd";
				
				String lb         = server;
				String lbIP       = model.getServerModel(server).getIP();
				String lbFwdChain = lb + "_fwd";
				
				//Forward Chains
				routerFm.addFilter(lb + "_allow_lb_out_traffic_" + backend, lbFwdChain,
						"-d " + backendIP
						+ " -p tcp"
						+ " --dport 80"
						+ " -j ACCEPT");

				routerFm.addFilter(lb + "_allow_lb_reply_traffic_" + backend, lbFwdChain,
						"-s " + backendIP
						+ " -p tcp"
						+ " -m state --state ESTABLISHED,RELATED"
						+ " -j ACCEPT");
				
				routerFm.addFilter(backend + "_allow_lb_in_traffic_" + lb, backendFwdChain,
						"-s " + lbIP
						+ " -p tcp"
						+ " --dport 80"
						+ " -j ACCEPT");

				routerFm.addFilter(backend + "_allow_lb_reply_traffic_" + lb, backendFwdChain,
						"-d " + lbIP
						+ " -p tcp"
						+ " -m state --state ESTABLISHED,RELATED"
						+ " -j ACCEPT");
			}
		}
		
		return units;
	}
	
	public void setLiveConfig(String config) {
		liveConfig = config;
	}

}