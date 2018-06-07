package core.data;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class ServerData extends AData {

	private JsonObject data;

	private String[] adminUsers;
	private String[] remoteAdminIps;
	private String[] types;
	private String[] cnames;
	private String[] profiles;
	private String[] ports;

	private String connection;
	private String mac;
	private String subnet;
	private String adminPort;
	private String sshPort;
	private String update;
	private String hostname;
	private String iface;
	private String extIface;
	private String metal;
	private String ram;
	private String diskSize;
	private String dataDiskSize;
	private String cpus;
	private String extConn;
	private String debianIsoUrl;
	private String debianIsoSha512;
	private String vmBase;
	private String externalIp;
	private String debianMirror;
	private String debianDirectory;
	private String adminEmail;
	private String domain;
	
	public ServerData(String label) {
		super(label);
	}

	public void read(JsonObject data) {
		this.data = data;
		
		adminUsers      = getPropertyArray("admins");
		remoteAdminIps  = getPropertyArray("sshsource");
		connection      = getProperty("connection", "direct");
		subnet          = getProperty("subnet", null);
		adminPort       = getProperty("adminport", null);
		sshPort         = getProperty("sshport", null);
		update          = getProperty("update", null);
		hostname        = getProperty("hostname", null);
		types           = getPropertyArray("types");
		iface           = getProperty("iface", null);
		extIface        = getProperty("extiface", null);
		metal           = getProperty("metal", null);
		ram             = getProperty("ram", null);
		diskSize        = getProperty("disksize", null);
		dataDiskSize    = getProperty("datadisksize", null);
		mac             = getProperty("mac", null);
		cpus            = getProperty("cpus", null);
		extConn         = getProperty("extconnection", null);
		cnames          = getPropertyArray("cnames");
		debianIsoUrl    = getProperty("debianisourl", null);
		debianIsoSha512 = getProperty("debianisosha512", null);
		vmBase          = getProperty("vmbase", null);
		profiles        = getPropertyArray("profiles");
		externalIp      = getProperty("externalip", null);
		debianMirror    = getProperty("debianmirror", null);
		debianDirectory = getProperty("debiandirectory", null);
		adminEmail      = getProperty("adminemail", null);
		domain          = getProperty("domain", null);
		ports           = getPropertyArray("ports");
	}

	public String getProperty(String property, String defaultVal) {
		return data.getString(property, defaultVal);
	}
	
	public String[] getPropertyArray(String property) {
		JsonArray jsonProperties = getPropertyObjectArray(property);

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
	
	public JsonArray getPropertyObjectArray(String property) {
		return data.getJsonArray(property);
	}
	
	public String[] getAdmins() {
		return this.adminUsers;
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
	
	public String getDebianMirror() {
		return this.debianMirror;
	}

	public String getDebianDirectory() {
		return this.debianDirectory;
	}
	
	public String getDataDiskSize() {
		return this.dataDiskSize;
	}
	
	public String getAdminEmail() {
		return this.adminEmail;
	}

	public String getDomain() {
		return this.domain;
	}

	public String[] getSSHSources() {
		return this.remoteAdminIps;
	}
	
	public String[] getPorts() {
		return this.ports;
	}
}
