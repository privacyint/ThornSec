package profile;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Vector;

import core.iface.IUnit;
import core.model.AModel;
import core.model.DeviceModel;
import core.model.NetworkModel;
import core.model.ServerModel;
import core.unit.fs.DirUnit;
import core.unit.fs.FilePermsUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;

public class IPSet extends AModel {

	private HashMap<String, Vector<InetAddress>> ipsets;
	
	public IPSet(NetworkModel networkModel) {
		super("ipset", networkModel);
		
		ipsets = new HashMap<String, Vector<InetAddress>>();
	}

	public void init() {
		//Add users
		Vector<InetAddress> addresses = new Vector<InetAddress>();
		
		for (DeviceModel user : networkModel.getUserDevices()) {
			addresses.addAll(user.getAddresses());
		}
		
		ipsets.put("user", addresses);
		
		//Add external-only devices
		addresses = new Vector<InetAddress>();

		for (DeviceModel device : networkModel.getExternalOnlyDevices()) {
			addresses.addAll(device.getAddresses());
		}
		
		ipsets.put("externalonly", addresses);

		//Add internal-only devices
		addresses = new Vector<InetAddress>();

		for (DeviceModel device : networkModel.getInternalOnlyDevices()) {
			addresses.addAll(device.getAddresses());
		}
		
		ipsets.put("internalonly", addresses);
		
		//Add admins for each server
		for (ServerModel server : networkModel.getAllServers()) {
			String[] admins = networkModel.getData().getAdmins(server.getLabel());
			addresses = new Vector<InetAddress>();
			
			for (String admin : admins) {
				addresses.addAll(networkModel.getDeviceModel(admin).getAddresses());
			}
			
			ipsets.put(server.getLabel() + "_admins", addresses);
		}

		addresses = new Vector<InetAddress>();
		
		for (ServerModel server : networkModel.getAllServers()) {
			addresses.addAll(server.getAddresses());			
		}

		ipsets.put("servers", addresses);
	}

	private Vector<IUnit> getInstalled() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new InstalledUnit("ipset", "proceed", "ipset", "I was unable to install your firewall. This is bad."));

		return units;
	}
	
	private Vector<IUnit> getPersistentConfig() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new DirUnit("ipsets_dir", "ipset_installed", "/etc/ipsets", "Couldn't create your ipsets. Your firewall will not work."));

		String ipsetsConfSh = "";
		ipsetsConfSh += "#!/bin/bash\n";
		ipsetsConfSh += "cd /etc/ipsets/\n";
		ipsetsConfSh += "{\n";
		
		for (String set : ipsets.keySet()) {
			ipsetsConfSh += "    cat ipset." + set + ".conf | awk /./;\n";
		}
		
		ipsetsConfSh += "}";
		units.addElement(new FileUnit("ipsets_conf_shell_script", "ipsets_dir_created", ipsetsConfSh, "/etc/ipsets/ipsets.up.sh"));
		units.addElement(new FilePermsUnit("ipsets_conf_shell_script", "ipsets_conf_shell_script", "/etc/ipsets/ipsets.up.sh", "750"));
		
		return units;
	}
	
	public Vector<IUnit> getLiveConfig() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		for (String set : ipsets.keySet()) {
			units.addAll(getSet(set));
		}
		
		return units;
	}

	private Vector<IUnit> getSet(String set) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		Vector<InetAddress> addresses = ipsets.get(set);

		String conf = "";
		conf += "create " + set + " hash:net family inet hashsize 1024 maxelem 65536";
		
		for (InetAddress address : addresses) {
			if (address == null) {
				continue;
			}
			else if (address.getHostAddress().equals("255.255.255.255")) {
				conf += "\nadd " + set + " 255.255.255.255";
				break; //We've hit wildcard, zero point in continuing!
			}
			
			conf += "\nadd " + set + " " + address.getHostAddress() + "/32";

		}
		
		units.addElement(new FileUnit("ipset_" + set, "ipsets_dir_created", conf, "/etc/ipsets/ipset." + set + ".conf"));
		
		return units;
	}
	
	void addSet(String set, Vector<InetAddress> addresses) {
		Vector<InetAddress> extant = ipsets.get(set);
		
		if (extant == null) {
			ipsets.put(set, addresses);
		}
		else {
			//We've already resolved this set, meh!
			return;
		}
	}

	public Vector<IUnit> getUnits() {
		Vector<IUnit> units = new Vector<IUnit>();

		units.addAll(this.getInstalled());
		units.addAll(this.getPersistentConfig());
		units.addAll(this.getLiveConfig());
		
		return units;
	}
	
	//ipsets will only allow a set name which is <= 30 chars
	public String getSetName(String name) {
		
		name = name.toLowerCase().replaceAll("[^0-9a-z]", "_");
		
		if (name.length() > 25) {
			name = name.substring(0, 25);
		}
		
		return name;
	}
}
