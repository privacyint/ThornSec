package core.data;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class NetworkData extends AData {

	private String ipClass, domain, dns, ip, adBlocking, gpg, autoGenPasswds, adminEmail, vpnOnly;

	private ServerData defaultServerData;

	private HashMap<String, ServerData> servers;
	private LinkedHashMap<String, ADeviceData> devices;
	
	public NetworkData(String label) {
		super(label);
	}

	public void read(JsonObject data) {
		String include = data.getString("include", null);
		if (include != null) {
			readInclude(include);
		} else {
			this.ipClass = data.getString("class", "a");
//			this.domain = data.getString("domain", null);
			this.dns = data.getString("dns", "8.8.8.8");
			this.ip = data.getString("ip", null);
			this.adBlocking = data.getString("adblocking", "no");
			this.gpg = data.getString("gpg", null);
			this.autoGenPasswds = data.getString("autogenpasswds", "false"); //Default to false
			this.adminEmail = data.getString("adminemail", null);
			this.vpnOnly = data.getString("vpnonly", "no");
			defaultServerData = new ServerData("");
			defaultServerData.read(data);
			servers = new HashMap<String, ServerData>();
			readServers(data.getJsonObject("servers"));
			devices = new LinkedHashMap<String, ADeviceData>();
			readInternalDevices(data.getJsonObject("internaldevices"));
			readExternalDevices(data.getJsonObject("externaldevices"));
			readUserDevices(data.getJsonObject("users"));
		}
	}

	private void readInclude(String include) {
		try {
			String text = new String(Files.readAllBytes(Paths.get(include)), StandardCharsets.UTF_8);
			this.read(text);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void readServers(JsonObject jsonServers) {
		//We've got to enforce the order here, or things may go a bit awry...
		Object[] servers = jsonServers.keySet().toArray();
		serverLabels = new String[servers.length];
		
		for (int i = 0; i < servers.length; ++i) {
			ServerData net = new ServerData(servers[i].toString());
			net.read(jsonServers.getJsonObject(servers[i].toString()));
			this.servers.put(servers[i].toString(), net);
			serverLabels[i] = servers[i].toString();
		}
	}

	private void readExternalDevices(JsonObject jsonDevices) {
		String[] devices = jsonDevices.keySet().toArray(new String[jsonDevices.size()]);
		
		for (String device : devices) {
			ExternalDeviceData dev = new ExternalDeviceData(device);
			dev.read(jsonDevices.getJsonObject(device));
			this.devices.put(device, dev);
		}
	}

	private void readInternalDevices(JsonObject jsonDevices) {
		String[] devices = jsonDevices.keySet().toArray(new String[jsonDevices.size()]);
		
		for (String device : devices) {
			InternalDeviceData dev = new InternalDeviceData(device);
			dev.read(jsonDevices.getJsonObject(device));
			this.devices.put(device, dev);
		}
	}
	
	private void readUserDevices(JsonObject jsonDevices) {
		String[] devices = jsonDevices.keySet().toArray(new String[jsonDevices.size()]);
		
		for (String device : devices) {
			UserDeviceData dev = new UserDeviceData(device);
			dev.read(jsonDevices.getJsonObject(device));
			this.devices.put(device, dev);
		}
	}

	public String[] getServerProfiles(String server) {
		return this.servers.get(server).getProfiles();
	}
	
	// Network only data
	
	public String getIPClass() {
		return ipClass;
	}

	public String getDNS() {
		return dns;
	}
	
	public String getGPG() {
		return gpg;
	}
	
	public String getAdminEmail() {
		return adminEmail;
	}
	
	public String getAutoGenPasswds() {
		return autoGenPasswds;
	}
	
	public String getNetmask() {
		return "255.255.255.252";
	}
	
	public String getIP() {
		return ip;
	}
	
	public boolean getAdBlocking() {
		return adBlocking.equals("yes");
	}

	public boolean getVpnOnly() {
		return vpnOnly.equals("yes");
	}
	
	public String[] getServerLabels() {
		return this.servers.keySet().toArray(new String[servers.size()]);
	}

	// Have to be provided by server only
	public String getProperty(String server, String property) {
		return this.servers.get(server).getProperty(property, null);
	}

	public String[] getPropertyArray(String server, String property) {
		return this.servers.get(server).getPropertyArray(property);
	}
	
	public JsonArray getPropertyObjectArray(String server, String property) {
		return this.servers.get(server).getPropertyObjectArray(property);
	}
	
	public String getSubnet(String server) {
		return this.servers.get(server).getSubnet();
	}
	
	public String getHostname(String server) {
		return this.servers.get(server).getHostname();
	}

	public String[] getTypes(String server) {
		return this.servers.get(server).getTypes();
	}
	
	public String[] getCnames(String server) {
		return this.servers.get(server).getCnames();
	}
	
	public String getExternalIp(String server) {
		return this.servers.get(server).getExternalIp();
	}
	
	public String getMac(String server) {
		return this.servers.get(server).getMac();
	}

	public String[] getPorts(String server) {
		return this.servers.get(server).getPorts();
	}
	
	// Can have default values
	public String[] getAllowedSSHSource(String server) {
		String[] val = this.servers.get(server).getSSHSources();
		if (val.length == 0) {
			return this.defaultServerData.getSSHSources();
		} else {
			return val;
		}
	}
		} else {
			return val;
		}
	}
	
	public String getDomain(String server) {
		String val = this.servers.get(server).getDomain();
		if (val == null) {
			return this.defaultServerData.getDomain();
		} else {
			return val;
		}
	}

	public String getAdminPort(String server) {
		String val = this.servers.get(server).getAdminPort();
		if (val == null) {
			val = this.defaultServerData.getAdminPort();
			if (val == null) {
				return "65422";
			}
		}
		
		return val;
	}
	
	public String getSSHPort(String server) {
		String val = this.servers.get(server).getSSHPort();
		if (val == null) {
			val = this.defaultServerData.getSSHPort();
			if (val == null) {
				return "65422";
			}
		}
		
		return val;
	}

	public String getConnection(String server) {
		String val = this.servers.get(server).getConnection();
		if (val == null) {
			return this.defaultServerData.getConnection();
		} else {
			return val;
		}
	}
	
	public String getUpdate(String server) {
		String val = this.servers.get(server).getUpdate();
		if (val == null) {
			val = this.defaultServerData.getUpdate();
			if (val == null) {
				return "false";
			}
		}
		
		return val;
	}
	
	public String getIface(String server) {
		String val = this.servers.get(server).getIface();
		if (val == null) {
			val = this.defaultServerData.getIface();
			if (val == null) {
				return "enp0s3";
			}
		}

		return val;
	}
	
	public String getExtIface(String server) {
		String val = this.servers.get(server).getExtIface();
		if (val == null) {
			return this.defaultServerData.getExtIface();
		} else {
			return val;
		}	
	}	
	public String getMetal(String server) {
		String val = this.servers.get(server).getMetal();
		if (val == null) {
			return this.defaultServerData.getMetal();
		} else {
			return val;
		}
	}
	
	public String getRam(String server) {
		String val = this.servers.get(server).getRam();
		if (val == null) {
			val = this.defaultServerData.getRam();
			if (val == null) {
				return "1024";
			}
		}
		
		return val;
	}

	public String getCpus(String server) {
		String val = this.servers.get(server).getCpus();
		if (val == null) {
			val = this.defaultServerData.getCpus();
			if (val == null) {
				return "1";
			}
		}
		
		return val;
	}
	
	public String getDiskSize(String server) {
		String val = this.servers.get(server).getDiskSize();
		if (val == null) {
			val = this.defaultServerData.getDiskSize();
			if (val == null) {
				return  "8096";
			}
		}
		
		return val;
	}
	
	public String getDataDiskSize(String server) {
		String val = this.servers.get(server).getDataDiskSize();
		if (val == null) {
			val = this.defaultServerData.getDataDiskSize();
			if (val == null) {
				return  "8096";
			}
		}
		
		return val;
	}
	
	public String getExtConn(String server) {
		String val = this.servers.get(server).getExtConn();
		if (val == null) {
			return this.defaultServerData.getExtConn();
		} else {
			return val;
		}
	}

	public String getDebianIsoUrl(String server) {
		String val = this.servers.get(server).getDebianIsoUrl();
		if (val == null) {
			val = this.defaultServerData.getDebianIsoUrl();
			if (val == null) {
				return "https://gensho.ftp.acc.umu.se/debian-cd/current/amd64/iso-cd/debian-9.4.0-amd64-netinst.iso";
			}
		}
		
		return val;
	}

	public String getDebianIsoSha512(String server) {
		String val = this.servers.get(server).getDebianIsoSha512();
		if (val == null) {
			val = this.defaultServerData.getDebianIsoSha512();
			if (val == null) {
				return "345c4e674dc10476e8c4f1571fbcdba4ce9788aa5584c5e2590ab3e89e7bb9acb370536f41a3ac740eb92b6aebe3cb8eb9734874dd1658c68875981b8351bc38";
			}
		}
		
		return val;
	}

	public String getVmBase(String server) {
		String val = this.servers.get(server).getVmBase();
		if (val == null) {
			val = this.defaultServerData.getVmBase();
			if (val == null) {
				return "/media/VMs";
			}
		}
	
		return val;
	}

	public String getDebianMirror(String server) {
		String val = this.servers.get(server).getDebianMirror();
		if (val == null) {
			val = this.defaultServerData.getDebianMirror();
			if (val == null) {
				return "ftp.uk.debian.org";
			}
		}
		
		return val;
	}

	public String getDebianDirectory(String server) {
		String val = this.servers.get(server).getDebianDirectory();
		if (val == null) {
			val = this.defaultServerData.getDebianDirectory();
			if (val == null) {
				return "/debian";
			}
		}
		
		return val;
	}
	
	//Device only stuff
	public String[] getDeviceLabels() {
		return this.devices.keySet().toArray(new String[devices.size()]);
	}
	
	public String getSSHKey(String device) {
		return this.devices.get(device).getSSHKey();
	}
	
	public String getFullName(String device) {
		return this.devices.get(device).getFullName();
	}
	
	public String[] getDeviceMacs(String device) {
		return this.devices.get(device).getMacs();
	}

	public Boolean getDeviceThrottled(String device) {
		return this.devices.get(device).getThrottled();
	}
	
	public Boolean getDeviceManaged(String device) {
		return this.devices.get(device).getManaged();
	}

	public String[] getDevicePorts(String device) {
		return this.devices.get(device).getPorts();
	}
	
	public String getDeviceType(String device) {
		return this.devices.get(device).getClass().getSimpleName().replace("DeviceData", "");
	}
}