package profile;

import java.util.Objects;
import java.util.Vector;

import core.iface.IUnit;
import core.model.MachineModel;
import core.model.NetworkModel;
import core.model.ServerModel;
import core.profile.AStructuredProfile;
import core.unit.fs.CustomFileUnit;
import core.unit.fs.DirUnit;
import core.unit.pkg.InstalledUnit;

public class Webproxy extends AStructuredProfile {

	private Nginx webserver;
	private String liveConfig;
	
	public Webproxy(ServerModel me, NetworkModel networkModel) {
		super("webproxy", me, networkModel);

		this.webserver = new Nginx(me, networkModel);
		this.liveConfig = "";
	}

	protected Vector<IUnit> getInstalled() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(webserver.getInstalled());
		units.addElement(new InstalledUnit("openssl", "openssl"));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig() {
		Vector<IUnit> units =  new Vector<IUnit>();

		units.addAll(webserver.getPersistentConfig());

		//Should we pass through real IPs?
		Boolean passThroughIps = Boolean.parseBoolean(networkModel.getData().getProperty(me.getLabel(), "passrealips", false));
		
		//First, build our ssl config
		units.addElement(new DirUnit("nginx_ssl_include_dir", "proceed", "/etc/nginx/includes"));
		
		String sslConf = "";
		sslConf += "    ssl_session_timeout 1d;\n";
		sslConf += "    ssl_session_cache shared:SSL:10m;\n";
		sslConf += "    ssl_session_tickets off;\n";
		sslConf += "\n";
		sslConf += "    ssl_protocols TLSv1.2 TLSv1.3;\n";
		sslConf += "    ssl_ciphers 'EECDH+CHACHA20:EECDH+AESGCM:EECDH+AES256';\n";
		sslConf += "    ssl_prefer_server_ciphers on;\n";
		sslConf += "    ssl_ecdh_curve auto;\n";
		sslConf += "\n";
		sslConf += "    add_header Strict-Transport-Security 'max-age=63072000; includeSubDomains; preload';\n";
		sslConf += "\n";
		sslConf += "    ssl_stapling on;\n";
		sslConf += "    ssl_stapling_verify on;\n";
		sslConf += "    resolver " + networkModel.getData().getDNS()[0].getHostAddress() + " valid=300s;\n";
		sslConf += "    resolver_timeout 5s;";
		
		units.addElement(((ServerModel)me).getConfigsModel().addConfigFile("nginx_ssl", "proceed", sslConf, "/etc/nginx/includes/ssl_params"));
		
		String headersConf = "";
		headersConf += "    add_header X-Frame-Options                   'SAMEORIGIN' always;\n";
		headersConf += "    add_header X-Content-Type-Options            'nosniff' always;\n";
		headersConf += "    add_header X-XSS-Protection                  '1; mode=block' always;\n";
		headersConf += "    add_header X-Download-Options                'noopen' always;\n";
		headersConf += "    add_header X-Permitted-Cross-Domain-Policies 'none' always;\n";
		headersConf += "    add_header Content-Security-Policy           'upgrade-insecure-requests; block-all-mixed-content; reflected-xss block;' always;";
		headersConf += "\n";
		headersConf += "    proxy_set_header Host               \\$host;\n";
		headersConf += "    proxy_set_header X-Forwarded-Host   \\$host:\\$server_port;\n";
		headersConf += "    proxy_set_header X-Forwarded-Server \\$host;\n";
		headersConf += "    proxy_set_header X-Forwarded-Port   \\$server_port;\n";
		headersConf += "    proxy_set_header X-Forwarded-Proto  https;";
		
		if (passThroughIps) {
			headersConf += "\n";
			headersConf += "\n#These headers pass real IP addresses to the backend - this may not be desired behaviour\n";
			headersConf += "    proxy_set_header X-Forwarded-For    \\$proxy_add_x_forwarded_for;\n";
			headersConf += "    proxy_set_header X-Real-IP          \\$remote_addr;";
		}
		
		units.addElement(((ServerModel)me).getConfigsModel().addConfigFile("nginx_headers", "proceed", headersConf, "/etc/nginx/includes/header_params"));
		
		String sslConfig = "";
		sslConfig += "server {\n";
		sslConfig += "    listen 80 default;\n";
		sslConfig += "    return 301 https://\\$host\\$request_uri;\n";
		sslConfig += "}";

		units.addAll(((ServerModel)me).getBindFsModel().addDataBindPoint("nginx_custom_blocks", "nginx_installed", "nginx", "nginx", "0750"));

		webserver.addLiveConfig("default", sslConfig);
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		if (this.liveConfig.equals("")) {
			String[] backends = networkModel.getData().getPropertyArray(me.getLabel(), "proxy");
			Boolean isDefault = true;
			
			for (String backend : backends) {

				MachineModel backendObj = networkModel.getMachineModel(backend);
				
				if (Objects.equals(backendObj, null)) {
					System.out.println("Server/device " + backend + " doesn't exist.  " + me.getLabel() + " cannot proxy to it...");
					continue; //Skip to the next
				}
				
				String[] cnames  = networkModel.getData().getCnames(backend);
				String   domain  = networkModel.getData().getDomain(backend);
				String   logDir  = "/var/log/nginx/" + backend + "." + domain + "/";

				String nginxConf = "";
				
				units.addElement(new DirUnit(backend + "_log_dir", "proceed", logDir, "Could not create the directory for " + backend + "'s logs. Nginx will refuse to start."));
				units.addAll(((ServerModel)me).getBindFsModel().addBindPoint(backend + "_tls_certs", "proceed", "/media/metaldata/tls/" + backend, "/media/data/tls/" + backend, "root", "root", "600", "/media/metaldata", false));
				
				//Generated from https://mozilla.github.io/server-side-tls/ssl-config-generator/ (Nginx/Modern) & https://cipherli.st/
				nginxConf = "server {\n";
				nginxConf += "    listen 443 ssl http2";
				if (isDefault) {
					nginxConf += " default"; //We need this to be here, or it'll crap out :'(
					isDefault = false;
				}
				nginxConf += ";\n";

				nginxConf += "    server_name " + backend + "." + domain;
				
				for (String cname : cnames) {
					nginxConf += (cname.equals("")) ? " " : " " + cname + ".";
					nginxConf += domain;
				}
				
				nginxConf += ";\n";
			
				//Let's separate the logs out...
				nginxConf += "    access_log " + logDir + "access.log;\n"; // main buffer=16k;\n";
				nginxConf += "    error_log  " + logDir + "error.log;\n"; //main buffer=16k;\n";
				nginxConf += "\n";
				//We use the Let's Encrypt naming convention here, in case we want to install it later
				nginxConf += "    include /etc/nginx/includes/ssl_params;\n";
				nginxConf += "    include /etc/nginx/includes/header_params;\n";
				nginxConf += "    ssl_certificate /media/data/tls/" + backend + "/fullchain.pem;\n"; 
				nginxConf += "    ssl_certificate_key /media/data/tls/" + backend + "/privkey.pem;\n";
				nginxConf += "    ssl_trusted_certificate /media/data/tls/" + backend + "/stapling.pem;\n";
				nginxConf += "\n";
				nginxConf += "    location / {\n";
				for (String source : networkModel.getData().getPropertyArray(backend, "allow")) {					
					nginxConf += "        allow " + source + ";\n";
					nginxConf += "        deny all;\n";
				}
				nginxConf += "        proxy_pass              http://" + backendObj.getIP().getHostAddress() + ";\n";
				nginxConf += "        proxy_request_buffering off;\n";
				nginxConf += "        proxy_buffering         off;\n";
				nginxConf += "        client_max_body_size    0;\n";
				nginxConf += "        proxy_http_version      1.1;\n";
				nginxConf += "        proxy_connect_timeout   3000;\n";
				nginxConf += "        proxy_send_timeout      3000;\n";
				nginxConf += "        proxy_read_timeout      3000;\n";
				nginxConf += "        send_timeout            3000;\n";
				nginxConf += "    }\n";
				nginxConf += "\n";
				nginxConf += "    include /media/data/nginx_custom_blocks/" + backend + ".conf;\n";
				nginxConf += "}";
				
				webserver.addLiveConfig(backend, nginxConf);
				
				units.addElement(new CustomFileUnit("nginx_custom_block_" + backend, "nginx_custom_blocks_data_bindpoint_created", "/media/data/nginx_custom_blocks/" + backend + ".conf"));
			}
		}
		else {
			webserver.addLiveConfig("default", this.liveConfig);
		}
		
		units.addAll(webserver.getLiveConfig());

		return units;
	}
	
	public Vector<IUnit> getNetworking() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String[] backends = networkModel.getData().getPropertyArray(me.getLabel(), "proxy");

		units.addAll(webserver.getNetworking());
		
		me.addRequiredEgress("check.torproject.org");
		me.addRequiredListen(443);
		
		for (String backend : backends) {
			me.addRequiredForward(backend, 80);
			//Only create these rules if we actually *have* users.
			if (!networkModel.getIPSet().isEmpty("user")) {
				me.addRequiredDnat(backend);
			}
		}
		
		return units;
	}
	
	public void setLiveConfig(String config) {
		liveConfig = config;
	}

}
