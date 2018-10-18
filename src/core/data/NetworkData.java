package core.data;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Objects;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class NetworkData extends AData {

	private String user;
	private String ipClass;
	private String dtls;
	private String ip;
	private String adBlocking;
	private String gpg;
	private String autoGenPasswds;
	private String adminEmail;
	private String vpnOnly;

	private String[] dns;
	
	private ServerData defaultServerData;

	private LinkedHashMap<String, ServerData>  servers;
	private LinkedHashMap<String, ADeviceData> devices;
	
	public NetworkData(String label) {
		super(label);
	}

	public void read(JsonObject data) {
		String include = data.getString("include", null);
		if (include != null) {
			readInclude(include);
		} else {
			this.user           = data.getString("myuser", null);
			this.ipClass        = data.getString("class", "a");
			this.dns            = getPropertyArray(data, "dns");
			this.dtls           = data.getString("dtls", "false");
			this.ip             = data.getString("ip", null);
			this.adBlocking     = data.getString("adblocking", "false");
			this.gpg            = data.getString("gpg", null);
			this.autoGenPasswds = data.getString("autogenpasswds", "false"); //Default to false
			this.adminEmail     = data.getString("adminemail", null);
			this.vpnOnly        = data.getString("vpnonly", "false");
			
			defaultServerData = new ServerData("");
			defaultServerData.read(data);
			
			servers = new LinkedHashMap<String, ServerData>();
			devices = new LinkedHashMap<String, ADeviceData>();

			readServers(data.getJsonObject("servers"));
			readInternalDevices(data.getJsonObject("internaldevices"));
			readExternalDevices(data.getJsonObject("externaldevices"));
			readUserDevices(data.getJsonObject("users"));
		}
	}

	private String[] getPropertyArray(JsonObject data, String property) {
		JsonArray jsonProperties = data.getJsonArray(property);

		if (jsonProperties != null) {
			String[] properties = new String[jsonProperties.size()];
			for (int i = 0; i < properties.length; ++i) {
				properties[i] = jsonProperties.getString(i);
			}
			
			return properties;
		}
		else {
			return new String[0];
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
		String[] servers = jsonServers.keySet().toArray(new String[jsonServers.size()]);
		
		for (String server : servers) {
			ServerData net = new ServerData(server);
			net.read(jsonServers.getJsonObject(server));
			this.servers.put(server, net);
		}
	}

	private void readExternalDevices(JsonObject jsonDevices) {
		if (jsonDevices != null) {
			String[] devices = jsonDevices.keySet().toArray(new String[jsonDevices.size()]);
			
			for (String device : devices) {
				ExternalDeviceData dev = new ExternalDeviceData(device);
				dev.read(jsonDevices.getJsonObject(device));
				this.devices.put(device, dev);
			}
		}
	}

	private void readInternalDevices(JsonObject jsonDevices) {
		if (jsonDevices != null) {
			String[] devices = jsonDevices.keySet().toArray(new String[jsonDevices.size()]);
			
			for (String device : devices) {
				InternalDeviceData dev = new InternalDeviceData(device);
				dev.read(jsonDevices.getJsonObject(device));
				this.devices.put(device, dev);
			}
		}
	}
	
	private void readUserDevices(JsonObject jsonDevices) {
		//We will *always* need user devices, or we will have no way to SSH in!
		if (jsonDevices == null) {
			System.out.println("You must specify at least one user!");
		}
		
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
	public String getUser() {
		return user;
	}
	
	public String getIPClass() {
		return ipClass;
	}

	public String[] getDNS() {
		return dns;
	}

	public boolean getDTLS() {
		return Objects.equals(dtls, "true");
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
		return Objects.equals(adBlocking, "true");
	}

	public boolean getVpnOnly() {
		return Objects.equals(vpnOnly, "true");
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

	public String[] getAdmins() {
		return this.defaultServerData.getAdmins();
	}

	public String[] getAdmins(String server) {
		String[] val = this.servers.get(server).getAdmins();
		if (val.length == 0) {
			return this.defaultServerData.getAdmins();
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
	
	public String getUserDefaultPassword(String userDevice) {
		return this.devices.get(userDevice).getDefaultPw();
	}
}