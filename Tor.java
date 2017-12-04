package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
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
		
		model.getServerModel(server).getAptSourcesModel().addAptSource(server, model, "tor", "proceed", "deb http://deb.torproject.org/torproject.org jessie main", "keys.gnupg.net", "A3C4F0F979CAA22CDBA8F512EE8CBC9E886DDD89");

		units.addElement(new InstalledUnit("tor_keyring", "tor_gpg", "deb.torproject.org-keyring"));
		units.addElement(new InstalledUnit("tor", "tor_keyring", "tor"));
		
		model.getServerModel(server).getUserModel().addUsername("debian-tor");
		
		units.addAll(proxy.getInstalled(server, model));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(model.getServerModel(server).getBindFsModel().addBindPoint(server, model, "tor", "tor_installed", "/media/metaldata/tor", "/media/data/tor", "debian-tor", "debian-tor", "0755"));

		units.addAll(proxy.getPersistentConfig(server, model));
		
		//Configs here based on the eotk (c) Alec Muffet
		//https://github.com/alecmuffett/eotk
		//Released under GPL v3 https://github.com/alecmuffett/eotk/blob/master/LICENSE
		
		String torConfig = "";
		torConfig += "DataDirectory /media/data/tor/\n";
		torConfig += "ControlPort unix:/media/data/tor/tor-control.sock\n";
		torConfig += "PidFile /media/data/tor/tor.pid\n";
		torConfig += "Log info file /var/log/tor.log\n";
		torConfig += "SafeLogging 1\n";
		torConfig += "HeartbeatPeriod 60 minutes\n";
		torConfig += "LongLivedPorts 80,443\n";
		torConfig += "RunAsDaemon 1\n";
		torConfig += "SocksPort 0 # frankly we don't want SOCKS anyway\n";
		torConfig += "HiddenServiceDir /media/data/tor/\n";
		torConfig += "HiddenServicePort 80 unix:/media/data/tor/port-80.sock\n";
		torConfig += "HiddenServicePort 443 unix:/media/data/tor/port-443.sock";

		units.add(new FileUnit("tor_config", "tor_installed", torConfig, "/etc/tor/torrc"));
		
		
		String proxyConfig = "";
		proxyConfig += "http {\n";
		proxyConfig += "  proxy_buffering on;\n";
		proxyConfig += "  proxy_buffers 16 64k;\n";
		proxyConfig += "  proxy_buffer_size 64k;\n";
		proxyConfig += "  proxy_busy_buffers_size 512k;\n";
		proxyConfig += "  proxy_max_temp_file_size 2048k;\n";
		proxyConfig += "  proxy_temp_file_write_size 64k;\n";
		proxyConfig += "  proxy_temp_path \\\"/tmp\\\";\n";
		proxyConfig += "\n";
		proxyConfig += "  server_tokens off;\n";
		proxyConfig += "\n";
		proxyConfig += "  allow \\\"unix:\\\";\n";
		proxyConfig += "  deny all;\n";
		proxyConfig += "\n";
		proxyConfig += "  proxy_read_timeout 60;\n";
		proxyConfig += "  proxy_connect_timeout 60;\n";
		proxyConfig += "\n";
		proxyConfig += "  ssl_certificate /media/data/tls/fullchain.pem;\n"; 
		proxyConfig += "  ssl_certificate_key /media/data/tls/privkey.pem;\n";
		proxyConfig += "  ssl_ciphers 'EECDH+CHACHA20:EECDH+AESGCM:EECDH+AES256';\n";
		proxyConfig += "  ssl_protocols TLSv1 TLSv1.1 TLSv1.2;\n";
		proxyConfig += "  ssl_session_cache shared:SSL:10m;\n";
		proxyConfig += "  ssl_session_timeout 10m;\n";
		proxyConfig += "  ssl_buffer_size 4k;\n";
		proxyConfig += "  ssl_prefer_server_ciphers on;\n";
		proxyConfig += "  ssl_ecdh_curve prime256v1;\n";
		proxyConfig += "\n";
		proxyConfig += "  map \\$http_upgrade $connection_upgrade {\n";
		proxyConfig += "    default \\\"upgrade\\\";\n";
		proxyConfig += "  }\n";
		proxyConfig += "\n";
		proxyConfig += "  server {\n";
		proxyConfig += "    listen unix:/media/data/tor/port-80.sock;\n";
		proxyConfig += "    listen unix:/media/data/tor/port-443.sock ssl;\n";
		proxyConfig += "\n";
		proxyConfig += "    location ~ ^/hello[-_]onion/?\\$ {\n";
		proxyConfig += "      return 200 \\\"Hello, Onion User!\\\";\n";
		proxyConfig += "    }\n";
		proxyConfig += "\n";
		proxyConfig += "    location / {\n";
		proxyConfig += "      proxy_pass \\\"\\$scheme://" + model.getServerModel(model.getData().getPropertyArray(server, "proxy")[0]).getIP() + "\\\";\n";
		proxyConfig += "      proxy_http_version 1.1;\n";
		proxyConfig += "      proxy_set_header Accept-Encoding \\\"\\\";\n";
		proxyConfig += "      proxy_set_header Connection $connection_upgrade;\n";
		proxyConfig += "      proxy_set_header Upgrade $http_upgrade;\n";
		proxyConfig += "      proxy_ssl_server_name on;\n";
		proxyConfig += "\n";
		proxyConfig += "      proxy_set_header Referer \\$referer2;\n";
		proxyConfig += "\n";
		proxyConfig += "      proxy_set_header Origin \\$origin2;\n";
		proxyConfig += "    }\n";
		proxyConfig += "  }\n";
		proxyConfig += "\n";
		proxyConfig += "  more_clear_headers \\\"Age\\\";\n";
		proxyConfig += "  more_clear_headers \\\"Server\\\";\n";
		proxyConfig += "  more_clear_headers \\\"Via\\\";\n";
		proxyConfig += "  more_clear_headers \\\"X-From-Nginx\\\";\n";
		proxyConfig += "  more_clear_headers \\\"X-NA\\\";\n";
		proxyConfig += "  more_clear_headers \\\"X-Powered-By\\\";\n";
		proxyConfig += "  more_clear_headers \\\"X-Request-Id\\\";\n";
		proxyConfig += "  more_clear_headers \\\"X-Runtime\\\";\n";
		proxyConfig += "  more_clear_headers \\\"X-Varnish\\\";\n";
		proxyConfig += "}";
		
		proxy.setLiveConfig(proxyConfig);
		units.addAll(proxy.getLiveConfig(server, model));

		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new RunningUnit("tor", "tor", "/usr/bin/tor"));
		model.getServerModel(server).getProcessModel().addProcess("/usr/bin/tor --defaults-torrc /usr/share/tor/tor-service-defaults-torrc -f /etc/tor/torrc --RunAsDaemon 0$");
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(proxy.getPersistentFirewall(server, model));
		
		return units;
	}
	
}