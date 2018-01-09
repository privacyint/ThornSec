package profile;

import java.util.Iterator;
import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;
import java.util.Iterator;

public class OpenVPN extends AStructuredProfile {

	public OpenVPN() {
		super("openvpn");
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		
		units.addElement(new InstalledUnit("openssl", "proceed", "openssl"));
                units.addElement(new InstalledUnit("ca_certificates", "proceed", "ca-certificates"));
                
                		units.addElement(new SimpleUnit("openvpn_repo_gpg", "proceed",
				"wget -O - https://swupdate.openvpn.net/repos/repo-public.gpg|apt-key add -",
				"sudo apt-key list | grep 'OpenVPN'", "", "fail"));
                                
    //            model.getServerModel(server).getAptSourcesModel().addAptSource(server, model, "openvpn_repo", "proceed", "build.openvpn.net", "deb http://build.openvpn.net/debian/openvpn/testing jessie main");
		units.addElement(new InstalledUnit("openvpn", "openvpn"));
		return units;
	}

	protected Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		// https://community.openvpn.net/openvpn/wiki/OpenvpnSoftwareRepos
		units.addElement(new SimpleUnit("openvpn_user", "openvpn_installed",
				"sudo adduser openvpn --system --shell=/bin/false --disabled-login --ingroup nogroup --no-create-home",
				"id openvpn 2>&1", "id: openvpn: no such user", "fail"));
		
		units.addAll(model.getServerModel(server).getBindFsModel().addBindPoint(server, model, "openvpn_keys", "openvpn_installed", "/media/metaldata/openvpn", "/media/data/openvpn", "openvpn", "nogroup", "0700", "/media/metaldata"));
		
		units.addElement(new SimpleUnit("openvpn_dh_params", "openssl_installed",
				"sudo openssl dhparam -out /media/data/openvpn/dh4096.pem 4096",
				"[ -f /media/data/openvpn/dh4096.pem ] && echo pass || echo fail", "pass", "pass"));
		
		units.addElement(new SimpleUnit("openvpn_hmac", "openvpn_installed",
				"openvpn --genkey --secret /media/data/openvpn/ta.key",
				"[ -f /media/data/openvpn/ta.key ] && echo pass || echo fail", "pass", "pass"));
                
                
		units.addElement(new InstalledUnit("openvpn", "proceed", "openvpn"));
		//Config adapted from https://www.linode.com/docs/networking/vpn/set-up-a-hardened-openvpn-server
		
		String config = "";
                //config += "local "+ model.getData() +"\n";
		config += "port 1194\n";
		config += "proto udp\n";
		config += "dev tun\n";
		config += "ca /media/data/openvpn/ca.crt\n"; //This should be the Client CA Certificate Chain
		config += "cert /media/data/openvpn/server.crt\n"; //Server Certificate, signed by the Server CA
		config += "key /media/data/openvpn/server.key\n";
		config += "dh /media/data/openvpn/dh4096.pem\n";
                config += "topology subnet\n";
                // Server address needs a method from the JSON
		config += "server 100.64.32.0 255.255.255.0\n";
		
                /*/Pick a different subnet from the network's, here
		if (!model.getData().getIPClass().equals("c")) {
			config += "server 192.168.0.0 255.255.255.0\n";
		} else if (!model.getData().getIPClass().equals("b")) {
			config += "server 172.16.0.0 255.255.255.0\n";
		} else if (!model.getData().getIPClass().equals("a")) {
			config += "10.0.0.0 255.255.255.0\n";
		}
		*/
		config += "ifconfig-pool-persist /media/data/openvpn/ipp.txt\n";
		
		//Need to push our routes here
		String[] routes = model.getServerLabels();
		for (int i = 0; i < routes.length; ++i) {
			config += "push \\\"route " + model.getServerModel(routes[i]).getSubnet() + " " + model.getData().getNetmask() + "\\\"\n";
		}
		config += "push \"redirect-gateway def1 bypass-dhcp\"\n";
                config += "push \"dhcp-option DNS 10.8.0.1\"\n";
                config += "push \"dhcp-option DNS 10.0.1.1\"\n";
		config += "keepalive 10 70\n";
		config += "user openvpn\n";
		config += "group nogroup\n";
		config += "cipher AES-256-CBC\n";
		//config += "auth SHA256\n";
                config += "tls-auth ta.key 0\n";
                config += "persist-key\n";
                config += "persist-tun\n";
                config += "status /media/data/openvpn/status.log\n";
                config += "log-append /var/log/openvpn.log\n";
                config += "verb 6\n";
                config += "mute 6\n";
		config += "tls-cipher \"EECDH+AESGCM:EDH+AESGCM:AES256+EECDH:AES256+EDH\"\n";
		config += "compress lz4-v2\n";
                config += "push \"compress lz4-v2\"\n";
                config += "explicit-exit-notify 1\n";
		units.addElement(new FileUnit("openvpn_persistent_config", "openvpn_installed", config, "/etc/openvpn/server.conf"));

                units.addElement(new SimpleUnit("openvpn_systmctl", "openvpn_ready",
				"sudo systemctl enable openvpn.service && sudo systemctl start openvpn.service",
				"[ -f /var/log/openvpn-status.log ] && echo pass || echo fail", "pass", "pass"));
                
		return units;
	}

	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		

		if (model.getData().getExternalIp(server) != null) {
			Vector<String> routers = model.getRouters();
			Iterator<String> itr = routers.iterator();
			
			while (itr.hasNext()) {
				String router = itr.next();
				
				model.getServerModel(router).getFirewallModel().addNatPrerouting("dnat_" + model.getData().getExternalIp(server) + "_udp_1194",
						"-p udp --dport 1194 -j DNAT --to-destination " + model.getServerModel(server).getIP() + ":1194");

				model.getServerModel(router).getFirewallModel().addNatPrerouting("dnat_" + model.getData().getExternalIp(server) + "_tcp_1194",
						"-p tcp --dport 1194 -j DNAT --to-destination " + model.getServerModel(server).getIP() + ":1194");
			}
		}

		
		return units;
	}


}
