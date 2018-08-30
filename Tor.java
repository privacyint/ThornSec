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

public class Tor extends AStructuredProfile {
	
	private Webproxy proxy;
	
	public Tor() {
		super("tor");
		
		this.proxy = new Webproxy();
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		model.getServerModel(server).getAptSourcesModel().addAptSource(server, model, "tor", "proceed", "deb http://deb.torproject.org/torproject.org stretch main", "keys.gnupg.net", "A3C4F0F979CAA22CDBA8F512EE8CBC9E886DDD89");
		
		units.addElement(new InstalledUnit("tor_keyring", "tor_gpg", "deb.torproject.org-keyring"));
		units.addElement(new InstalledUnit("tor", "tor_keyring_installed", "tor"));
		
		model.getServerModel(server).getUserModel().addUsername("debian-tor");
		
		units.addAll(proxy.getInstalled(server, model));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(model.getServerModel(server).getBindFsModel().addDataBindPoint(server, model, "tor", "tor_installed", "debian-tor", "debian-tor", "0700"));
		units.addAll(model.getServerModel(server).getBindFsModel().addLogBindPoint(server, model, "tor", "tor_installed", "debian-tor", "0750"));

		units.addElement(new DirUnit("torhs_files_dir", "tor_installed", "/var/lib/tor/hidden_service"));
		
		units.addElement(new SimpleUnit("torhs_hostname", "tor_data_mounted",
				//Copy over the new hostname file if one doesn't already exist, or replace the new hostname if we already have one
				"sudo [ ! -f /media/data/tor/hostname ] && cp /var/lib/tor/hidden_service/hostname /media/data/tor/hostname || cp /media/data/tor/hostname /var/lib/tor/hidden_service/hostname",
				"sudo cmp --silent /media/data/tor/hostname /var/lib/tor/hidden_service/hostname && echo pass || echo fail", "pass", "pass"));

		units.addElement(new SimpleUnit("torhs_private_key", "tor_data_mounted",
				//Copy over the new private key file if one doesn't already exist, or replace the new private key if we already have one
				"sudo [ ! -f /media/data/tor/private_key ] && cp /var/lib/tor/hidden_service/private_key /media/data/tor/private_key || cp /media/data/tor/private_key /var/lib/tor/hidden_service/private_key",
				"sudo cmp --silent /media/data/tor/private_key /var/lib/tor/hidden_service/private_key && echo pass || echo fail", "pass", "pass"));
		
/*		String service = "";
		service += "[Unit]\n";
		service += "Description=nginx - high performance web server\n";
		service += "Documentation=http://nginx.org/en/docs/\n";
		service += "After=network-online.target remote-fs.target nss-lookup.target\n";
		service += "Wants=network-online.target\n";
		service += "\n";
		service += "[Service]\n";
		service += "Type=forking\n";
		service += "PIDFile=/var/run/nginx.pid\n";
		service += "ExecStartPre=/bin/rm -f /media/data/www/port-80.sock /media/data/www/port-443.sock\n";
		service += "ExecStart=/usr/sbin/nginx -c /etc/nginx/nginx.conf\n";
		service += "ExecReload=/bin/kill -s HUP $MAINPID\n";
		service += "ExecStop=/bin/kill -s TERM $MAINPID\n";
		service += "\n";
		service += "[Install]\n";
		service += "WantedBy=multi-user.target";

		units.addElement(new FileUnit("nginx_service", "nginx_installed", service, "/etc/systemd/system/multi-user.target.wants/nginx.service"));
*/	
		//Configs here loosely based on the eotk (c) Alec Muffet
		//https://github.com/alecmuffett/eotk
		//Released under GPL v3 https://github.com/alecmuffett/eotk/blob/master/LICENSE
		
		String torConfig = "";
		torConfig += "DataDirectory /var/lib/tor\n";
		torConfig += "ControlPort unix:/var/lib/tor/tor-control.sock\n";
		torConfig += "PidFile /var/lib/tor/tor.pid\n";
		torConfig += "SafeLogging 1\n";
		torConfig += "LongLivedPorts 80,443\n";
		torConfig += "HeartbeatPeriod 60 minutes\n";
		torConfig += "RunAsDaemon 1\n";
		torConfig += "\n";
		torConfig += "SocksPort 0\n";
		torConfig += "\n";
		torConfig += "HiddenServiceDir /var/lib/tor/hidden_service/\n";
		torConfig += "HiddenServicePort 80 unix:/var/lib/tor/port-80.sock\n";
		torConfig += "HiddenServicePort 443 unix:/var/lib/tor/port-443.sock\n";
		torConfig += "HiddenServiceNumIntroductionPoints 3\n";
		torConfig += "\n";
		torConfig += "FascistFirewall 1";

		units.add(new FileUnit("tor_config", "tor_installed", torConfig, "/etc/tor/torrc"));

		units.addElement(new SimpleUnit("tor_service_enabled", "tor_config",
				"sudo systemctl enable tor",
				"sudo systemctl is-enabled tor", "enabled", "pass",
				"Couldn't set tor to auto-start on boot.  You will need to manually start the service (\"sudo service tor start\") on reboot."));
		
		units.addAll(proxy.getPersistentConfig(server, model));

		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();

		units.addAll(model.getServerModel(server).getBindFsModel().addDataBindPoint(server, model, "tls", "proceed", "root", "root", "600"));
		
		String proxyConfig = "";
		proxyConfig += "include /etc/nginx/includes/ssl_params;\n";
		proxyConfig += "proxy_buffering on;\n";
		proxyConfig += "proxy_buffers 16 64k;\n";
		proxyConfig += "proxy_buffer_size 64k;\n";
		proxyConfig += "proxy_busy_buffers_size 512k;\n";
		proxyConfig += "proxy_max_temp_file_size 2048k;\n";
		proxyConfig += "proxy_temp_file_write_size 64k;\n";
		proxyConfig += "proxy_temp_path \\\"/tmp\\\";\n";
		proxyConfig += "\n";
		proxyConfig += "allow \\\"unix:\\\";\n";
		proxyConfig += "deny all;\n";
		proxyConfig += "\n";
		proxyConfig += "proxy_read_timeout 60;\n";
		proxyConfig += "proxy_connect_timeout 60;\n";
		proxyConfig += "\n";
		proxyConfig += "proxy_cache_path /tmp/nginx-cache-torhs levels=1:2 keys_zone=torhs:64m;\n";
		proxyConfig += "proxy_cache torhs;\n";
		proxyConfig += "proxy_cache_min_uses 1;\n";
		proxyConfig += "proxy_cache_revalidate on;\n";
		proxyConfig += "proxy_cache_use_stale timeout updating;\n";
		proxyConfig += "proxy_cache_valid any 60s;\n";
		proxyConfig += "\n";
		proxyConfig += "server {\n";
		proxyConfig += "    server_name _ default;\n";
		proxyConfig += "\n";
		//proxyConfig += "    listen unix:/media/data/www/port-80.sock;\n";
		proxyConfig += "    listen unix:/var/lib/tor/port-80.sock;\n";
		proxyConfig += "    return 307 https://\\$host\\$request_uri;\n";
		proxyConfig += "}\n";
		proxyConfig += "\n";
		proxyConfig += "server {\n";
		proxyConfig += "    server_name _ default;\n";
		proxyConfig += "\n";
		//proxyConfig += "    listen unix:/media/data/www/port-443.sock ssl;\n";
		proxyConfig += "    listen unix:/var/lib/tor/port-443.sock ssl;\n";
		proxyConfig += "\n";
		proxyConfig += "    ssl_certificate /media/data/tls/fullchain.pem;\n"; 
		proxyConfig += "    ssl_certificate_key /media/data/tls/privkey.pem;\n";
		proxyConfig += "    ssl_ciphers 'EECDH+CHACHA20:EECDH+AESGCM:EECDH+AES256';\n";
		proxyConfig += "    ssl_protocols TLSv1 TLSv1.1 TLSv1.2;\n";
		proxyConfig += "    ssl_session_cache shared:SSL:10m;\n";
		proxyConfig += "    ssl_session_timeout 10m;\n";
		proxyConfig += "    ssl_buffer_size 4k;\n";
		proxyConfig += "    ssl_prefer_server_ciphers on;\n";
		proxyConfig += "    ssl_ecdh_curve secp384r1:prime256v1;\n";
		proxyConfig += "\n";
		proxyConfig += "    location / {\n";
//needs to be ip address
		proxyConfig += "        proxy_pass \\\"\\$scheme://" + model.getServerModel((model.getData().getPropertyArray(server, "proxy")[0])).getIP() + "\\\";\n";
		proxyConfig += "        proxy_http_version 1.1;\n";
		proxyConfig += "        proxy_set_header Accept-Encoding \\\"identity\\\";\n";
		proxyConfig += "        proxy_set_header Connection \\\"upgrade\\\";\n";
		proxyConfig += "        proxy_set_header Upgrade \\\"upgrade\\\";\n";
		proxyConfig += "        proxy_ssl_server_name on;\n";
		proxyConfig += "        proxy_set_header Host $host;\n";
		proxyConfig += "    }\n";
		proxyConfig += "}";
		
		proxy.setLiveConfig(proxyConfig);
		
		units.addElement(new RunningUnit("tor", "tor", "/usr/bin/tor"));
		model.getServerModel(server).getProcessModel().addProcess("/usr/bin/tor --defaults-torrc /usr/share/tor/tor-service-defaults-torrc -f /etc/tor/torrc --RunAsDaemon 0$");
		units.addAll(proxy.getLiveConfig(server, model));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		//Allow the server to call out to torproject.org to download mainline
		model.getServerModel(server).addRouterEgressFirewallRule(server, model, "allow_torproject", "deb.torproject.org", new String[]{"80","443"});
		units.addAll(proxy.getPersistentFirewall(server, model));
		
		for (String router : model.getRouters()) {
			model.getServerModel(router).getFirewallModel().addFilter(server + "_allow_onion_out_traffic", server + "_egress",
					"-p tcp"
					+ " -m tcp -m multiport --dports 80,443"
					+ " -j ACCEPT");
		}
		
		return units;
	}
	
}