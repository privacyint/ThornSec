package profile;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Vector;

import core.StringUtils;
import core.iface.IUnit;
import core.model.AModel;
import core.model.DeviceModel;
import core.model.NetworkModel;
import core.model.ServerModel;
import core.unit.SimpleUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FilePermsUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;

public class IPSet extends AModel {

	private HashMap<String, Vector<InetAddress>> ipsets;
	private HashMap<String, Integer> cidrs;

	public IPSet(NetworkModel networkModel) {
		super("ipset", networkModel);
		
		ipsets = new HashMap<String, Vector<InetAddress>>();
		cidrs  = new HashMap<String, Integer>();
	}

	public void init() {
		//Add users
		for (DeviceModel user : networkModel.getUserDevices()) {
			addToSet("user", 32, user.getAddresses());
		}
		
		//Add external-only devices
		for (DeviceModel device : networkModel.getExternalOnlyDevices()) {
			addToSet("externalonly", 32, device.getAddresses());
		}

		//Add internal-only devices
		for (DeviceModel device : networkModel.getInternalOnlyDevices()) {
			addToSet("internalonly", 32, device.getAddresses());
		}
		
		//Add admins for each server
		for (ServerModel server : networkModel.getAllServers()) {
			String[] admins = networkModel.getData().getAdmins(server.getLabel());
			
			for (String admin : admins) {
				addToSet(server.getLabel() + "_admins", 32, networkModel.getDeviceModel(admin).getAddresses());
			}
		}

		//Managed devicen need admins, too
		for (DeviceModel device : networkModel.getAllDevices()) {
			if (!device.isManaged()) { continue; }

			String[] admins = networkModel.getData().getAdmins(device.getLabel());
			
			for (String admin : admins) {
				addToSet(device.getLabel() + "_admins", 32, networkModel.getDeviceModel(admin).getAddresses());
			}
		}
		
		//Now add all our servers
		for (ServerModel server : networkModel.getAllServers()) {
			addToSet("servers", 32, server.getAddresses());
		}
		
		if (networkModel.getData().getAutoGuest()) {
			addToSet("autoguest", 22, networkModel.stringToIP("10.250.0.0"));
		}
	}
	private Vector<IUnit> getInstalled() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new InstalledUnit("ipset", "proceed", "ipset", "I was unable to install your firewall. This is bad."));

		return units;
	}
	
	private Vector<IUnit> getPersistentConfig() {
		Vector<IUnit> units = new Vector<IUnit>();

		return units;
	}
	
	public Vector<IUnit> getLiveConfig() {
		Vector<IUnit> units = new Vector<IUnit>();

		units.addElement(new DirUnit("ipsets_dir", "ipset_installed",
				"/etc/ipsets",
				"Couldn't create the directory for your ipsets. Your firewall will not work, and you will get all sorts of errors."
				+ " Please try rebooting the machine and re-running this configuration."));
		
		for (String set : ipsets.keySet()) {
			units.addAll(getSet(set));
		}
		
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

		units.addElement(new SimpleUnit("ipset_applied", "ipsets_conf_shell_script_chmoded",
				"", //This is a forced ipset update.
				"sudo /etc/ipsets/ipsets.up.sh | sudo ipset -! restore", "", "pass"));
		
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
			
			conf += "\nadd " + set + " " + address.getHostAddress() + "/" + this.cidrs.get(set);

		}
		
		units.addElement(new FileUnit("ipset_" + set, "ipsets_dir_created", conf, "/etc/ipsets/ipset." + set + ".conf"));
		
		return units;
	}
	
	void addToSet(String set, Integer cidr, Vector<InetAddress> addresses) {
		for (InetAddress address : addresses) {
			addToSet(set, cidr, address);
		}
	}

	void addToSet(String set, Integer cidr, InetAddress address) {
		set = StringUtils.stringToAlphaNumeric(set, "_");
		Vector<InetAddress> extant = ipsets.get(set);
		Vector<InetAddress> addresses = new Vector<InetAddress>();
		
		if (extant != null) {
			addresses.addAll(extant);
		}

		addresses.add(address);
		
		addresses = new Vector<InetAddress>(new LinkedHashSet<InetAddress>(addresses)); //enforce uniqueness

		this.ipsets.put(set, addresses);
		this.cidrs.put(set, cidr);
	}
	
	public Vector<IUnit> getUnits() {
		Vector<IUnit> units = new Vector<IUnit>();

		units.addAll(this.getInstalled());
		units.addAll(this.getLiveConfig());
		units.addAll(this.getPersistentConfig());
		
		return units;
	}
	
	//ipsets will only allow a set name which is <= 30 chars
	public String getSetName(String name) {
		
		name = StringUtils.stringToAlphaNumeric(name.toLowerCase(), "_");
		
		if (name.length() > 25) {
			name = name.substring(0, 25);
		}
		
		return name;
	}

	public boolean isEmpty(String set) {
		return ( ! this.ipsets.containsKey(set) || ! this.ipsets.get(set).isEmpty() );
	}
}
