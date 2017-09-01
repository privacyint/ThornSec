package core.data;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class ServerData extends AData {

	private JsonObject data;
	private String adminUser;
	private String[] adminKeys;
	private String adminFullName;
	private String connection;
	private String subnet;
	private String adminPort;
	private String sshPort;
	private String update;
	private String hostname;
	private String[] types;
	private String iface;
	private String extIface;
	private String metal;
	private String ram;
	private String diskSize;
	private String dataDiskSize;
	private String mac;
	private String cpus;
	private String extConn;
	private String[] cnames;
	private String debianIsoUrl;
	private String debianIsoSha512;
	private String vmBase;
	private String[] profiles;
	private String externalIp;
	private String bridge;
	private String debianMirror;
	
	public ServerData(String label) {
		super(label);
	}

	public void read(JsonObject data) {
		this.data = data;
		
		adminUser = data.getString("user", null);
		JsonArray keys = data.getJsonArray("keys");
		if (keys != null) {
			adminKeys = new String[keys.size()];
			for (int i = 0; i < adminKeys.length; i++) {
				adminKeys[i] = keys.getString(i);
			}
		}
		adminFullName = data.getString("adminname", null);
		connection = data.getString("connection", null);
		subnet = data.getString("subnet", null);
		adminPort = data.getString("adminport", null);
		sshPort = data.getString("sshport", null);
		update = data.getString("update", null);
		hostname = data.getString("hostname", null);
		JsonArray jsonTypes = data.getJsonArray("types");
		if (jsonTypes != null) {
			types = new String[jsonTypes.size()];
			for (int i = 0; i < types.length; i++) {
				types[i] = jsonTypes.getString(i);
			}
		}
		iface = data.getString("iface", null);
		extIface = data.getString("extiface", null);
		metal = data.getString("metal", null);
		ram = data.getString("ram", null);
		diskSize = data.getString("disksize", null);
		dataDiskSize = data.getString("datadisksize", null);
		mac = data.getString("mac", null);
		cpus = data.getString("cpus", null);
		extConn = data.getString("extconnection", null);
		JsonArray jsonCnames = data.getJsonArray("cnames");
		if (jsonCnames != null) {
			cnames = new String[jsonCnames.size()];
			for (int i = 0; i < cnames.length; ++i) {
				cnames[i] = jsonCnames.getString(i);
			}
		}
		else {
			cnames = new String[0];
		}
		debianIsoUrl = data.getString("debianisourl", null);
		debianIsoSha512 = data.getString("debianisosha512", null);
		vmBase = data.getString("vmbase", null);
		JsonArray jsonProfiles = data.getJsonArray("profiles");
		if (jsonProfiles != null) {
			profiles = new String[jsonProfiles.size()];
			for (int i = 0; i < profiles.length; i++) {
				profiles[i] = jsonProfiles.getString(i);
			}
		}
		externalIp = data.getString("externalip", null);
		bridge = data.getString("bridge", null);
		debianMirror = data.getString("debianmirror", null);
	}

	public String getProperty(String property) {
		return data.getString(property, null);
	}
	
	public String[] getPropertyArray(String property) {
		JsonArray jsonProperties = data.getJsonArray(property);

		if (jsonProperties != null) {
			String[] properties = new String[jsonProperties.size()];
			for (int i = 0; i < properties.length; ++i) {
				properties[i] = jsonProperties.getString(i);
			}
			
			return properties;
		}
		else {
			return null;
		}
	}
	
	public JsonArray getPropertyObjectArray(String property) {
		return data.getJsonArray(property);
	}
	
	public String getUser() {
		return this.adminUser;
	}

	public String getFullName() {
		return this.adminFullName;
	}
	
	public String[] getUserKeys() {
		return this.adminKeys;
	}

	public String getConnection() {
		return this.connection;
	}

	public String getSubnet() {
		return this.subnet;
	}

	public String getAdminPort() {
		return this.adminPort;
	}
	
	public String getSSHPort() {
		return this.sshPort;
	}
	
	public String getUpdate() {
		return this.update;
	}
	
	public String getHostname() {
		if (this.hostname == null) {
			return this.getLabel().replace("_", "-");
		}
		return this.hostname;
	}

	public String[] getTypes() {
		return this.types;
	}
	
	public String getIface() {
		return this.iface;
	}
	
	public String getExtIface() {
		return this.extIface;
	}
	
	public String getMetal() {
		return this.metal;
	}
	
	public String getRam() {
		return this.ram;
	}
	
	public String getMac() {
		return this.mac;
	}
	
	public String getCpus() {
		return this.cpus;
	}
	
	public String getDiskSize() {
		return this.diskSize;
	}	
	
	public String getExtConn() {
		return this.extConn;
	}
	
	public String[] getCnames() {
		return this.cnames;
	}

	public String getDebianIsoUrl() {
		return this.debianIsoUrl;
	}
	
	public String getDebianIsoSha512() {
		return this.debianIsoSha512;
	}
	
	public String getVmBase() {
		return this.vmBase;
	}
	
	public String[] getProfiles() {
		return this.profiles;
	}
	
	public String getExternalIp() {
		return this.externalIp;
	}
	
	public String getBridge() {
		return this.bridge;
	}
	
	public String getDebianMirror() {
		return this.debianMirror;
	}

	public String getDataDiskSize() {
		return this.dataDiskSize;
	}
		
}
