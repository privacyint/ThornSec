package core.data;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Set;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class ServerData extends ADeviceData {

	private String[] adminUsers;
	private String[] remoteAdminIps;
	private String[] types;
	private String[] profiles;

	private HashMap<String, String> wanIfaces;
	private HashMap<String, String> lanIfaces;

	private Integer adminPort;
	private Integer sshPort;
	private Integer ram;
	private Integer cpus;
	private Integer bootDiskSize;
	private Integer dataDiskSize;

	private Boolean update;

	private Inet4Address externalIp;

	private String connection;
	private String extConn;
	private String metalIface;
	
	private String metal;
	private String debianIsoUrl;
	private String debianIsoSha512;
	private String vmBase;
	private String debianMirror;
	private String debianDirectory;
	private String adminEmail;
	
	ServerData(String label) {
		super(label);
		
		this.adminUsers     = null;
		this.remoteAdminIps = null;
		this.types          = null;
		this.profiles       = null;
		
		this.lanIfaces = new HashMap<String, String>();
		this.wanIfaces = new HashMap<String, String>();
		
		this.adminPort    = null;
		this.sshPort      = null;
		this.ram          = null;
		this.cpus         = null;
		this.bootDiskSize = null;
		this.dataDiskSize = null;
		
		this.update = null;
		
		this.externalIp = null;
		
		this.connection = null;
		this.extConn    = null;
		this.metalIface = null;
		
		this.metal           = null;
		this.debianIsoUrl    = null;
		this.debianIsoSha512 = null;
		this.vmBase          = null;
		this.debianMirror    = null;
		this.debianDirectory = null;
		this.adminEmail      = null;
	}

	public void read(JsonObject data) {
		super.setData(data);

		super.setFirstOctet(10);
		super.setDomain(super.getProperty("domain", null));
		super.setListenPorts(getProperty("ports", null));
		super.setCnames(super.getPropertyArray("cnames"));
		super.setHostname(super.getProperty("hostname", null));
		super.setEmailAddress(getProperty("email", null));

		this.adminUsers     = super.getPropertyArray("admins");
		this.remoteAdminIps = super.getPropertyArray("sshsource");
		this.types          = super.getPropertyArray("types");
		this.profiles       = super.getPropertyArray("profiles");

		this.connection      = super.getProperty("connection", "direct");
		this.metal           = super.getProperty("metal", null);
		this.extConn         = super.getProperty("extconnection", null);
		this.debianIsoUrl    = super.getProperty("debianisourl", null);
		this.debianIsoSha512 = super.getProperty("debianisosha512", null);
		this.vmBase          = super.getProperty("vmbase", null);
		this.debianMirror    = super.getProperty("debianmirror", null);
		this.debianDirectory = super.getProperty("debiandirectory", null);
		this.adminEmail      = super.getProperty("adminemail", null);

		this.externalIp = super.stringToIP(super.getProperty("externalip", null));

		this.sshPort      = super.parseInt(super.getProperty("sshport", null));
		this.adminPort    = super.parseInt(super.getProperty("adminport", null));
		this.bootDiskSize = super.parseInt(super.getProperty("bootdisksize", null));
		this.dataDiskSize = super.parseInt(super.getProperty("datadisksize", null));
		this.ram          = super.parseInt(super.getProperty("ram", null));
		this.cpus         = super.parseInt(super.getProperty("cpus", null));

		this.update = Boolean.valueOf(super.getProperty("update", null));
		
		JsonArray lanIfaces = (JsonArray) super.getPropertyObjectArray("lan");
		if (lanIfaces != null) {
			for (int i = 0; i < lanIfaces.size(); ++i) {
				JsonObject row = lanIfaces.getJsonObject(i);
				String iface = row.getString("iface", null);
				String mac = row.getString("mac", null);
	
				this.lanIfaces.put(iface, mac);
			}
		}

		JsonArray wanIfaces = (JsonArray) super.getPropertyObjectArray("wan");
		if (wanIfaces != null) {
			for (int i = 0; i < wanIfaces.size(); ++i) {
				JsonObject row = wanIfaces.getJsonObject(i);
				String iface = row.getString("iface", null);
				String mac = row.getString("mac", null);
	
				this.wanIfaces.put(iface, mac);
			}
		}
		
		JsonArray requiredEgress = (JsonArray) getPropertyObjectArray("allowegress");
		if (requiredEgress != null) {
			for (int i = 0; i < requiredEgress.size(); ++i) {
				JsonObject row = requiredEgress.getJsonObject(i);
				String destination = row.getString("destination", null);
				Set<Integer> ports = super.parseIntList(row.getString("ports", "80,443"));
				Integer cidr = super.parseInt(row.getString("cidr", "32"));
				
				super.addRequiredEgress(destination, cidr, ports);
			}
		}

		JsonArray requiredForward = (JsonArray) getPropertyObjectArray("allowforward");
		if (requiredForward != null) {
			for (int i = 0; i < requiredForward.size(); ++i) {
				JsonObject row = requiredForward.getJsonObject(i);
				String destination = row.getString("destination", null);
	
				super.addRequiredForward(destination);
			}
		}

		JsonArray requiredIngress = (JsonArray) getPropertyObjectArray("allowingress");
		if (requiredIngress != null) {
			for (int i = 0; i < requiredIngress.size(); ++i) {
				JsonObject row = requiredIngress.getJsonObject(i);
				String source = row.getString("source", null);
	
				super.addRequiredIngress(source);
			}
		}
	}

	public String[] getAdmins() {
		return this.adminUsers;
	}

	public String getConnection() {
		return this.connection;
	}
	
	public String getMetalIface() {
		return this.metalIface;
	}
	
	public void setMetalIface(String metalIface) {
		this.metalIface = metalIface;
	}

	public Integer getAdminPort() {
		return this.adminPort;
	}
	
	public Integer getSSHPort() {
		return this.sshPort;
	}
	
	public Boolean getUpdate() {
		return this.update;
	}
	
	public String[] getTypes() {
		return this.types;
	}

	public HashMap<String, String> getLanIfaces() {
		return this.lanIfaces;
	}

	public HashMap<String, String> getWanIfaces() {
		return this.wanIfaces;
	}
	
	public String getMetal() {
		return this.metal;
	}
	
	public Integer getRam() {
		return this.ram;
	}
	
	public Integer getCpus() {
		return this.cpus;
	}
	
	public Integer getBootDiskSize() {
		return this.bootDiskSize;
	}	
	
	public String getExtConn() {
		return this.extConn;
	}
	
	public void setDebianIsoUrl(String url) {
		this.debianIsoUrl = url;
	}
	
	public String getDebianIsoUrl() {
		return this.debianIsoUrl;
	}
	
	public void setDebianIsoSha512(String sha512) {
		this.debianIsoSha512 = sha512;
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
	
	public InetAddress getExternalIp() {
		return this.externalIp;
	}
	
	public String getDebianMirror() {
		return this.debianMirror;
	}

	public String getDebianDirectory() {
		return this.debianDirectory;
	}
	
	public Integer getDataDiskSize() {
		return this.dataDiskSize;
	}
	
	public String getAdminEmail() {
		return this.adminEmail;
	}

	public String[] getSSHSources() {
		return this.remoteAdminIps;
	}
}
