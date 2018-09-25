package profile;

import java.util.Vector;

import javax.json.JsonArray;
import javax.json.JsonObject;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;

public class Wifi extends AStructuredProfile {

	
	public Wifi() {
		super("wifi");
	}
/*
	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new InstalledUnit("hostapd", "proceed", "hostapd"));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units =  new Vector<IUnit>();
		
		String hostapdconf = "DAEMON_CONF=\"/etc/hostapd/hostapd.conf\"";
		units.addElement(new FileUnit("wifi_set_config_location", "hostapd_installed", hostapdconf, "/etc/default/hostapd"));

		JsonArray interfaces = (JsonArray) model.getData().getPropertyObjectArray(server, "wifi");
		for (int i = 0; i < interfaces.size(); ++i) {
			JsonObject row = interfaces.getJsonObject(i);
			
			String iface = row.getString("iface", null);
			
			//We're declaring a real iface
			if (!iface.equals(null)) {
				units.addElement(model.getServerModel(server).getInterfaceModel().addIface("wifi_router_iface_" + i,
						"manual",
						iface,
						null,
						null,
						null,
						null,
						null));
				
				for (String router : model.getRouters()) {
					DHCP dhcp = model.getServerModel(router).getRouter().getDHCP();

					dhcp.addListeningIface(iface);
				}
			}
		}
		
		//Hostapd only really acts as a switch, so let's add it to a bridge to keep config overheads down
		units.addElement(model.getServerModel(server).getInterfaceModel().addIface("wifi_bridge",
				"manual",
				iface,
				null,
				null,
				null,
				null,
				null));
		
		for (String router : model.getRouters()) {
			DHCP dhcp = model.getServerModel(router).getRouter().getDHCP();

			dhcp.addListeningIface(iface);
		}
		
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String hostapdconf = "";
		
		JsonArray interfaces = (JsonArray) model.getData().getPropertyObjectArray(server, "wifi");
		
		for (int i = 0; i < interfaces.size(); ++i) {
			JsonObject row = interfaces.getJsonObject(i);
			
			String iface   = row.getString("iface", null);
			String ssid    = row.getString("ssid", model.getData().getDomain(server) + " WiFi " + i);
			String channel = row.getString("channel", "1");
			String mode    = row.getString("mode", "g");
			String passwd  = row.getString("passphrase", null);
			
			//This is a full wifi interface definition
			if (!iface.equals(null)) {
				hostapdconf += "interface=" + iface + "\n";
				hostapdconf += "hw_mode=" + mode + "\n";
				hostapdconf += "channel=" + channel + "\n";
				hostapdconf += "ieee80211n=1\n";
				hostapdconf += "ieee80211ac=1\n";
				hostapdconf += "wmm_enabled=1\n";
				hostapdconf += "\n";
				hostapdconf += "ssid=" + ssid + "\n";
				hostapdconf += "auth_algs=1\n";
				hostapdconf += "wpa=2\n";
				hostapdconf += "wpa_key_mgmt=WPA-PSK\n";
				hostapdconf += "rsn_pairwise=CCMP\n"; 
				hostapdconf += "wpa_passphrase=" + passwd;
			}
			else { //A virtual iface
				hostapdconf += "\n";
				hostapdconf += "bss=wlan" + i + "\n"; 
				hostapdconf += "ssid=" + ssid + "\n";
				hostapdconf += "auth_algs=1\n";
				hostapdconf += "wpa=2\n";
				hostapdconf += "wpa_key_mgmt=WPA-PSK\n";
				hostapdconf += "rsn_pairwise=CCMP\n"; 
				hostapdconf += "wpa_passphrase=" + passwd;
			}
		}

		units.addElement(new FileUnit("wifi_set_config", "hostapd_installed", hostapdconf, "/etc/hostapd/hostapd.conf"));

		return units;
	}
	
	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		

		return units;
	}
	*/
}