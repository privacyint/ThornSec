package core.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Vector;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.stream.JsonParsingException;
import javax.swing.JOptionPane;

// TODO: Auto-generated Javadoc
/**
 * The Class NetworkData.
 */
public class NetworkData extends AData {
	private String myUser;
	private String adminEmail;
	private String gpg;
	private InetAddress ip;

	private Boolean adBlocking;
	private Boolean autoGenPasswds;
	
	private Boolean vpnOnly;
	private Boolean dtls;
	private Boolean autoGuest;

	private InetAddress[] upstreamDNS;
	
	private ServerData defaultServerData;

	private LinkedHashMap<String, ServerData>  servers;
	private LinkedHashMap<String, ADeviceData> devices;
	
	/**
	 * Instantiates a new network data.
	 *
	 * @param label the network label
	 */
	public NetworkData(String label) {
		super(label);
		
		this.myUser     = null;
		this.adminEmail = null;
		this.gpg        = null;
		
		this.ip = null;
		
		this.adBlocking     = null;
		this.autoGenPasswds = null;
		this.vpnOnly        = null;
		this.dtls           = null;
		this.autoGuest      = null;
		
		this.upstreamDNS = null;
		
		this.defaultServerData = new ServerData("");
		
		this.servers = new LinkedHashMap<String, ServerData>();
		this.devices = new LinkedHashMap<String, ADeviceData>();
	}

	/**
	 * Reads in and populates this network's data
	 */
	@Override
	public void read(JsonObject data) {
		super.setData(data);
		
		String include = data.getString("include", null);
		if (include != null) {
			this.readInclude(include);
		}
		else {
			this.defaultServerData.read(data);
			
			this.upstreamDNS = getIPAddressArray(data, "dns");

			this.ip = stringToIP(data.getString("ip", null));

			this.myUser         = data.getString("myuser", null);
			this.gpg            = data.getString("gpg", null);
			this.adminEmail     = data.getString("adminemail", null);
			
			this.dtls           = Boolean.parseBoolean(data.getString("dtls", "false"));
			this.adBlocking     = Boolean.parseBoolean(data.getString("adblocking", "false"));
			this.autoGenPasswds = Boolean.parseBoolean(data.getString("autogenpasswds", "false"));
			this.vpnOnly        = Boolean.parseBoolean(data.getString("vpnonly", "false"));
			this.autoGuest      = Boolean.parseBoolean(data.getString("autoguest", "false"));

			readServers(data.getJsonObject("servers"));
			readInternalDevices(data.getJsonObject("internaldevices"));
			readExternalDevices(data.getJsonObject("externaldevices"));
			readUserDevices(data.getJsonObject("users"));
		}
	}

	/**
	 * Gets the IP address array.
	 *
	 * @param data the data
	 * @param property the property
	 * @return the IP address array (or empty array if nothing to convert)
	 */
	private InetAddress[] getIPAddressArray(JsonObject data, String property) {
		JsonArray jsonIPAddresses = data.getJsonArray(property);

		if (jsonIPAddresses != null) {
			InetAddress[] ipAddresses = new InetAddress[jsonIPAddresses.size()];
			
			for (int i = 0; i < ipAddresses.length; ++i) {
				ipAddresses[i] = super.stringToIP(jsonIPAddresses.getString(i));
			}
			
			return ipAddresses;
		}
		else {
			return new InetAddress[0];
		}
	}
	
	/**
	 * Read included JSON files.
	 *
	 * @param include the path to the file to include
	 */
	private void readInclude(String include) {
		String rawIncludeData = null;
		
		JsonReader jsonReader = null;
		
		try {
			rawIncludeData = new String(Files.readAllBytes(Paths.get(include)), StandardCharsets.UTF_8);
			jsonReader = Json.createReader(new StringReader(rawIncludeData));

			read(jsonReader.readObject());
		}
		catch (IOException e) {
			JOptionPane.showMessageDialog(null, "I was unable to read the JSON file from the disk.\n\nThe error reported was: " + e.getLocalizedMessage());
			System.exit(1);
		}
		catch (JsonParsingException e){
			JOptionPane.showMessageDialog(null, "I was unable to parse your JSON due to an error.\n\nThe error reported was: " + e.getLocalizedMessage());
			System.exit(1);
		}
	}

	/**
	 * Read servers.
	 *
	 * @param jsonServers the json servers
	 */
	private void readServers(JsonObject jsonServers) {
		String[] servers = jsonServers.keySet().toArray(new String[jsonServers.size()]);
		
		for (String server : servers) {
			ServerData net = new ServerData(server);
			net.read(jsonServers.getJsonObject(server));
			
			this.servers.put(server, net);
		}
	}

	/**
	 * Read external devices.
	 *
	 * @param jsonDevices the json devices
	 */
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

	/**
	 * Read internal devices.
	 *
	 * @param jsonDevices the json devices
	 */
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
	
	/**
	 * Read user devices.
	 *
	 * @param jsonDevices the json devices
	 */
	private void readUserDevices(JsonObject jsonDevices) {
		String[] devices = jsonDevices.keySet().toArray(new String[jsonDevices.size()]);

		//We will *always* need user devices, or we will have no way to SSH in!
		if (devices.length == 0) {
			JOptionPane.showMessageDialog(null, "You have not specified any users for your network.\n\nA network requires a minimum of one user");
			System.exit(1);
		}
		
		for (String device : devices) {
			UserDeviceData userDevice = new UserDeviceData(device);
			userDevice.read(jsonDevices.getJsonObject(device));
			
			this.devices.put(device, userDevice);
		}
	}

	/**
	 * Gets the server profiles.
	 *
	 * @param server the server
	 * @return the server profiles
	 */
	public String[] getServerProfiles(String server) {
		return this.servers.get(server).getProfiles();
	}
	
	/**
	 * Gets the user.
	 *
	 * @return the user
	 */
	// Network only data
	public String getUser() {
		if (this.myUser ==  null) {
			JOptionPane.showMessageDialog(null, "You must specify \"myuser\" for the network");
			System.exit(1);
		}
		
		return this.myUser;
	}

	/**
	 * Gets the dns.
	 *
	 * @return the dns
	 */
	public InetAddress[] getDNS() {
		return this.upstreamDNS;
	}

	/**
	 * Gets the dtls.
	 *
	 * @return the dtls
	 */
	public Boolean getDTLS() {
		return this.dtls;
	}
	
	/**
	 * Gets the gpg.
	 *
	 * @return the gpg
	 */
	public String getGPG() {
		return this.gpg;
	}
	
	/**
	 * Should we build an auto guest network?
	 *
	 * @return True if we should, False if not
	 */
	public Boolean getAutoGuest() {
		return this.autoGuest;
	}
	
	/**
	 * Gets the admin email.
	 *
	 * @return the admin email
	 */
	public String getAdminEmail() {
		return this.adminEmail;
	}
	
	/**
	 * Should we autogenerate passwords for users who haven't set a default?
	 *
	 * @return True if we should, False otherwise.
	 */
	public Boolean getAutoGenPasswds() {
		return this.autoGenPasswds;
	}
	
	/**
	 * Gets the netmask - hardcoded as /30.
	 *
	 * @return the netmask (255.255.255.252)
	 */
	public InetAddress getNetmask() {
		InetAddress netmask = null;
		try {
			netmask = InetAddress.getByName("255.255.255.252");
		}
		//It'll never *actually* throw this here...
		catch (UnknownHostException e) {
			//We'll never get here...
			//...famous last words...
			JOptionPane.showMessageDialog(null, "This error shouldn't have been possible to get. Well done!");
			System.exit(1);
		}
		
		return netmask;
	}
	
	/**
	 * Gets the configuration ip for this network.
	 * 
	 * This is either the IP of our router (if we're inside) or the public IP address (if it's an external resource)
	 *
	 * @return the ip
	 */
	public InetAddress getIP() {
		if (this.ip ==  null) {
			JOptionPane.showMessageDialog(null, "You must specify \"ip\" for the network.\n\nThis is the IP address ThornSec uses for configuration");
			System.exit(1);
		}

		return this.ip;
	}
	
	/**
	 * Should we do ad blocking at the router?
	 *
	 * @return True if we should, False otherwise
	 */
	public Boolean getAdBlocking() {
		return this.adBlocking;
	}

	/**
	 * Will we only allow users to access our resources over VPN?
	 *
	 * @return True if they can only access over VPN, False if clearnet is fine
	 */
	public boolean getVpnOnly() {
		return this.vpnOnly;
	}
	
	/**
	 * Gets all server labels on our network.
	 *
	 * @return the all server labels
	 */
	public String[] getAllServerLabels() {
		return this.servers.keySet().toArray(new String[this.servers.size()]);
	}

	/**
	 * Gets the property.
	 *
	 * @param server the server
	 * @param property the property
	 * @param isRequired the is required
	 * @return the property
	 */
	// Have to be provided by server only
	public String getProperty(String server, String property, Boolean isRequired) {
		String value = this.servers.get(server).getProperty(property, null); 
		
		if (value == null && isRequired) {
			JOptionPane.showMessageDialog(null, "I was expecting to see \"" + property + "\" to be declared for server " + server + ".\n\nPlease declare it!");
			System.exit(1);
		}

		return value;
	}

	/**
	 * Gets the data of a given device on our network.
	 *
	 * @param endpoint the endpoint's label
	 * @return the endpoint data
	 */
	private ADeviceData getEndpointData(String endpoint) {
		if (this.servers.containsKey(endpoint)) {
			return this.servers.get(endpoint);
		}
		else if (this.devices.containsKey(endpoint)) {
			return this.devices.get(endpoint);
		}
		
		return null;
	}
	
	/**
	 * Gets the property array.
	 *
	 * @param endpoint the endpoint
	 * @param property the property
	 * @return the property array
	 */
	public String[] getPropertyArray(String endpoint, String property) {
		ADeviceData endpointData = getEndpointData(endpoint);
		
		if (endpointData != null) {
			return endpointData.getPropertyArray(property);
		}
		
		return new String[0];
	}
	
	/**
	 * Gets the property object array.
	 *
	 * @param server the server
	 * @param property the property
	 * @return the property object array
	 */
	public JsonArray getPropertyObjectArray(String server, String property) {
		return this.servers.get(server).getPropertyObjectArray(property);
	}
	
	/**
	 * Gets the hostname.
	 *
	 * @param server the server
	 * @return the hostname
	 */
	public String getHostname(String server) {
		return this.servers.get(server).getHostname();
	}

	/**
	 * Gets the metal iface.
	 *
	 * @param server the server
	 * @return the metal iface
	 */
	public String getMetalIface(String server) {
		return this.servers.get(server).getMetalIface();
	}
	
	/**
	 * Gets the types.
	 *
	 * @param server the server
	 * @return the types
	 */
	public String[] getTypes(String server) {
		return this.servers.get(server).getTypes();
	}
	
	/**
	 * Gets the cnames.
	 *
	 * @param endpoint the endpoint
	 * @return the cnames
	 */
	public String[] getCnames(String endpoint) {
		ADeviceData endpointData = getEndpointData(endpoint);
		
		if (endpointData != null) {
			return endpointData.getCnames();
		}
		
		//We shouldn't get here.  Famous last words, as per...
		return null;
	}
	
	/**
	 * Gets the external ip.
	 *
	 * @param endpoint the endpoint
	 * @return the external ip
	 */
	public InetAddress getExternalIp(String endpoint) {
		InetAddress ip = null;
		
		if (this.servers.containsKey(endpoint)) {
			ip = this.servers.get(endpoint).getExternalIp();
		}
		
		return ip;
	}
	
	// Can have default values
	//public String[] getAllowedSSHSources(String server) {
	//	String[] sources = this.servers.get(server).getSSHSources();
	//	
	//	if (sources.length == 0) {
	//		return this.defaultServerData.getSSHSources();
	//	}
	//
	//	return sources;
	//}

	/**
	 * Gets the admins.
	 *
	 * @return the admins
	 */
	public String[] getAdmins() {
		return this.defaultServerData.getAdmins();
	}

	/**
	 * Gets the admins.
	 *
	 * @param server the server
	 * @return the admins
	 */
	public String[] getAdmins(String server) {
		String[] admins = getAdmins();
		
		if (admins.length == 0) {
			return this.defaultServerData.getAdmins();
		}

		return admins;
	}
	
	/**
	 * Gets the domain.
	 *
	 * @param endpoint the endpoint
	 * @return the domain
	 */
	public String getDomain(String endpoint) {
		String domain = null;
		
		if (this.servers.containsKey(endpoint)) {
			domain = this.servers.get(endpoint).getDomain();
		}
		else if (this.devices.containsKey(endpoint)) {
			domain = this.devices.get(endpoint).getDomain();
		}
	
		if (domain == null || domain.equals("")) {
			return this.defaultServerData.getDomain();
		}
		
		return domain;
	}

	/**
	 * Gets the admin port.
	 *
	 * @param server the server
	 * @return the admin port
	 */
	public Integer getAdminPort(String server) {
		Integer port = this.servers.get(server).getAdminPort();
		
		if (port == null) {
			port = this.defaultServerData.getAdminPort();
			if (port == null) {
				port = 65422;
			}
		}
		
		return port;
	}
	
	/**
	 * Gets the SSH port.
	 *
	 * @param server the server
	 * @return the SSH port
	 */
	public Integer getSSHPort(String server) {
		Integer port = this.servers.get(server).getSSHPort();
		
		if (port == null) {
			port = this.defaultServerData.getSSHPort();
			if (port == null) {
				port = 65422;
			}
		}
		
		return port;
	}

	/**
	 * Auto update.
	 *
	 * @param server the server
	 * @return the boolean
	 */
	public Boolean autoUpdate(String server) {
		Boolean update = this.servers.get(server).getUpdate();
		
		if (update == null) {
			update = this.defaultServerData.getUpdate();
			if (update == null) {
				update = false;
			}
		}
		
		return update;
	}
	
	/**
	 * Gets the lan ifaces.
	 *
	 * @param machine the machine
	 * @return the lan ifaces
	 */
	public HashMap<String, String> getLanIfaces(String machine) {
		HashMap<String, String> ifaces = null; 

		if (this.servers.containsKey(machine)) {
			ifaces = this.servers.get(machine).getLanIfaces();
			if (ifaces.isEmpty()) {
				ifaces = this.defaultServerData.getLanIfaces();
			}
		}
		else if (this.devices.containsKey(machine)) {
			ifaces = this.devices.get(machine).getLanIfaces();
		}
		
		return ifaces;
	}
	
	/**
	 * Gets the wan ifaces.
	 *
	 * @param server the server
	 * @return the wan ifaces
	 */
	public HashMap<String, String> getWanIfaces(String server) {
		HashMap<String, String> wanIfaces = this.servers.get(server).getWanIfaces();
		
		if (wanIfaces == null) {
			wanIfaces = this.defaultServerData.getWanIfaces();
		}
	
		return wanIfaces;
	}
	
	/**
	 * Gets the metal.
	 *
	 * @param server the server
	 * @return the metal
	 */
	public String getMetal(String server) {
		String metal = this.servers.get(server).getMetal();
		
		if (metal == null) {
			metal = this.defaultServerData.getMetal();
		}
		
		return metal;
	}
	
	/**
	 * Gets the ram.
	 *
	 * @param server the server
	 * @return the ram
	 */
	public Integer getRam(String server) {
		Integer ram = this.servers.get(server).getRam();
		
		if (ram == null) {
			ram = this.defaultServerData.getRam();
			if (ram == null) {
				ram = 1024;
			}
		}
		
		return ram;
	}

	/**
	 * Gets the cpus.
	 *
	 * @param server the server
	 * @return the cpus
	 */
	public Integer getCpus(String server) {
		Integer cpus = this.servers.get(server).getCpus();
		
		if (cpus == null) {
			cpus = this.defaultServerData.getCpus();
			if (cpus == null) {
				cpus = 1;
			}
		}
		
		return cpus;
	}
	
	/**
	 * Gets the boot disk size.
	 *
	 * @param server the server
	 * @return the boot disk size
	 */
	public Integer getBootDiskSize(String server) {
		Integer size = this.servers.get(server).getBootDiskSize();
		
		if (size == null) {
			size = this.defaultServerData.getBootDiskSize();
			if (size == null) {
				size = 8096;
			}
		}
		
		return size;
	}
	
	/**
	 * Gets the data disk size.
	 *
	 * @param server the server
	 * @return the data disk size
	 */
	public Integer getDataDiskSize(String server) {
		Integer size = this.servers.get(server).getDataDiskSize();
		
		if (size == null) {
			size = this.defaultServerData.getDataDiskSize();
			if (size == null) {
				size = 8096;
			}
		}
		
		return size;
	}
	
	/**
	 * Gets the ext connection type.
	 *
	 * @param server the server
	 * @return the ext connection type
	 */
	public String getExtConnectionType(String server) {
		String connection = this.servers.get(server).getExtConn();
		
		if (connection == null) {
			connection = this.defaultServerData.getExtConn();
		}
		
		return connection;
	}

	/**
	 * Gets the debian iso url.
	 *
	 * @param server the server
	 * @return the debian iso url
	 */
	public String getDebianIsoUrl(String server) {
		String url = this.servers.get(server).getDebianIsoUrl();
		
		if (url == null) {
			url = this.defaultServerData.getDebianIsoUrl();
			if (url == null) {
				try {
					url = "https://gensho.ftp.acc.umu.se/debian-cd/current/amd64/iso-cd/";

					URL isoVersion = new URL(url + "SHA512SUMS");
			        BufferedReader line = new BufferedReader(new InputStreamReader(isoVersion.openStream()));
			        String inputLine;
			        if ((inputLine = line.readLine()) != null) {
			            String[] netInst = inputLine.split(" ");
			            url += netInst[2];
			        }
			        line.close();

			        this.defaultServerData.setDebianIsoUrl(url);

				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		return url;
	}

	/**
	 * Gets the debian iso sha 512.
	 *
	 * @param server the server
	 * @return the debian iso sha 512
	 */
	public String getDebianIsoSha512(String server) {
		String hash = this.servers.get(server).getDebianIsoSha512();
		
		if (hash == null) {
			hash = this.defaultServerData.getDebianIsoSha512();
			if (hash == null) {
				try {
					String url = "https://gensho.ftp.acc.umu.se/debian-cd/current/amd64/iso-cd/";

					URL isoVersion = new URL(url + "SHA512SUMS");
			        BufferedReader line = new BufferedReader(new InputStreamReader(isoVersion.openStream()));
			        String inputLine;
			        if ((inputLine = line.readLine()) != null) {
			            String[] netInst = inputLine.split(" ");
			            hash = netInst[0];
			        }
			        line.close();
			        
		            this.defaultServerData.setDebianIsoSha512(hash);

				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		return hash;
	}

	/**
	 * Gets the hypervisor thornsec base.
	 *
	 * @param server the server
	 * @return the hypervisor thornsec base
	 */
	public String getHypervisorThornsecBase(String server) {
		String baseDir = this.servers.get(server).getVmBase();
		
		if (baseDir == null) {
			baseDir = this.defaultServerData.getVmBase();
			if (baseDir == null) {
				baseDir = "/srv/ThornSec";
			}
		}
	
		return baseDir;
	}

	/**
	 * Gets the debian mirror.
	 *
	 * @param server the server
	 * @return the debian mirror
	 */
	public String getDebianMirror(String server) {
		String mirror = this.servers.get(server).getDebianMirror();
		
		if (mirror == null) {
			mirror = this.defaultServerData.getDebianMirror();
			if (mirror == null) {
				mirror = "free.hands.com";
			}
		}
		
		return mirror;
	}

	/**
	 * Gets the debian directory.
	 *
	 * @param server the server
	 * @return the debian directory
	 */
	public String getDebianDirectory(String server) {
		String directory = this.servers.get(server).getDebianDirectory();
		
		if (directory == null) {
			directory = this.defaultServerData.getDebianDirectory();
			if (directory == null) {
				directory = "/debian";
			}
		}
		
		return directory;
	}
	
	/**
	 * Gets the all device labels.
	 *
	 * @return the all device labels
	 */
	//Device only stuff
	public String[] getAllDeviceLabels() {
		return this.devices.keySet().toArray(new String[devices.size()]);
	}
	
	/**
	 * Gets the user SSH key.
	 *
	 * @param user the user
	 * @return the user SSH key
	 */
	public String getUserSSHKey(String user) {
		return this.devices.get(user).getSSHKey();
	}
	
	/**
	 * Gets the user full name.
	 *
	 * @param user the user
	 * @return the user full name
	 */
	public String getUserFullName(String user) {
		return this.devices.get(user).getFullName();
	}
	
	/**
	 * Gets the device is throttled.
	 *
	 * @param device the device
	 * @return the device is throttled
	 */
	public Boolean getDeviceIsThrottled(String device) {
		return this.devices.get(device).getIsThrottled();
	}
	
	/**
	 * Gets the device is managed.
	 *
	 * @param device the device
	 * @return the device is managed
	 */
	public Boolean getDeviceIsManaged(String device) {
		return this.devices.get(device).getIsManaged();
	}

	/**
	 * Gets the device ports.
	 *
	 * @param device the device
	 * @return the device ports
	 */
	public Set<Integer> getDevicePorts(String device) {
		return this.devices.get(device).getListenPorts();
	}
	
	/**
	 * Gets the device type.
	 *
	 * @param device the device
	 * @return the device type
	 */
	public String getDeviceType(String device) {
		String type = this.devices.get(device).getClass().getSimpleName().replace("DeviceData", "");
		
		assert !type.isEmpty();
		
		return type;
	}
	
	/**
	 * Gets the user default password.
	 *
	 * @param user the user
	 * @return the user default password
	 */
	public String getUserDefaultPassword(String user) {
		return this.devices.get(user).getDefaultPassword();
	}
	
	/**
	 * Gets the email address.
	 *
	 * @param machine the machine
	 * @return the email address
	 */
	public String getEmailAddress(String machine) {
		String emailAddress = null;
		
		if (this.servers.containsKey(machine)) {
			emailAddress = this.servers.get(machine).getEmailAddress();
		}
		else if (this.devices.containsKey(machine)) {
			emailAddress = this.devices.get(machine).getEmailAddress();
		}
		
		assert emailAddress != null;
		
		return emailAddress;
	}
	
	/**
	 * Gets the required egress.
	 *
	 * @param endpoint the endpoint
	 * @return the required egress
	 */
	public HashMap<String, HashMap<Integer, Set<Integer>>> getRequiredEgress(String endpoint) {
		ADeviceData endpointData = getEndpointData(endpoint);
		
		if (endpointData != null) {
			return endpointData.getRequiredEgress();
		}
		
		return null;
	}
	
	/**
	 * Gets the required forward.
	 *
	 * @param endpoint the endpoint
	 * @return the required forward
	 */
	public Vector<String> getRequiredForward(String endpoint) {
		ADeviceData endpointData = getEndpointData(endpoint);
		
		if (endpointData != null) {
			return endpointData.getRequiredForward();
		}
		
		return null;
	}

	/**
	 * Gets the required ingress.
	 *
	 * @param endpoint the endpoint
	 * @return the required ingress
	 */
	public Vector<String> getRequiredIngress(String endpoint) {
		ADeviceData endpointData = getEndpointData(endpoint);
		
		if (endpointData != null) {
			return endpointData.getRequiredIngress();
		}
		
		return null;
	}
}
