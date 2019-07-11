/*
 * This code is part of the ThornSec project.
 * 
 * To learn more, please head to its GitHub repo: @privacyint
 * 
 * Pull requests encouraged.
 */
package core.data.network;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;

import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.stream.JsonParsingException;
import javax.mail.internet.InternetAddress;

import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;

import core.data.AData;

import core.data.machine.ADeviceData;
import core.data.machine.AMachineData;
import core.data.machine.AMachineData.Encapsulation;
import core.data.machine.ExternalDeviceData;
import core.data.machine.HypervisorData;
import core.data.machine.InternalDeviceData;
import core.data.machine.UserDeviceData;
import core.data.machine.configuration.NetworkInterfaceData;
import core.data.machine.ServerData;
import core.data.machine.ServerData.WANConnection;
import core.data.machine.ServiceData;

import core.exception.data.ADataException;
import core.exception.data.InvalidIPAddressException;
import core.exception.data.InvalidPropertyArrayException;
import core.exception.data.InvalidPropertyException;
import core.exception.data.NoValidUsersException;
import core.exception.data.machine.InvalidDeviceException;
import core.exception.data.machine.InvalidMachineException;
import core.exception.data.machine.InvalidServerException;
import core.exception.data.machine.InvalidUserException;

/**
 * This class represents the state of our network *AS DEFINED IN THE JSON*
 * 
 * If something isn't defined in our JSON, we will return default values.
 * 
 * This is our "interface" between the data and the models ThornSec will build.
 */
public class NetworkData extends AData {
	private static final String DEFAULT_DTLS           = "true";
	private static final String DEFAULT_ADBLOCKING     = "false";
	private static final String DEFAULT_AUTOGENPASSWDS = "false";
	private static final String DEFAULT_VPNONLY        = "false";
	private static final String DEFAULT_AUTOGUEST      = "false";
	private static final String DEFAULT_THORNSECBASE   = "/srv/ThornSec";
	private static final String DEFAULT_DEBIAN_ISO_DIR = "https://gensho.ftp.acc.umu.se/debian-cd/current/amd64/iso-cd/";
	private static final String DEFAULT_DEBIANMIRROR   = "free.hands.com";
	private static final String DEFAULT_DEBIANDIR      = "/debian";
	private static final String DEFAULT_NETMASK        = "/30";
	private static final String DEFAULT_KEEPASS_DB   = "ThornSec.kdbx";
	
	private static final Integer DEFAULT_RAM          = 2048;
	private static final Integer DEFAULT_CPUS         = 1;
	
	private static final Integer DEFAULT_SSH_PORT   = 65422;
	private static final Integer DEFAULT_ADMIN_PORT = 65422;
	
	private static final Boolean DEFAULT_UPDATE = true;
	
	private String myUser;
	private String pgp;
	private IPAddress ip;
	private String domain;

	private Boolean adBlocking;
	private Boolean autoGenPassphrases;
	
	private Boolean vpnOnly;
	private Boolean dtls;
	private Boolean autoGuest;

	private Set<IPAddress> upstreamDNS;
	
	private ServiceData    defaultServiceData;
	private HypervisorData defaultHypervisorData;

	private Hashtable<String, ServerData>  servers;
	private Hashtable<String, ADeviceData> devices;
	
	public NetworkData(String label) {
		super(label);
		
		this.myUser = null;
		this.pgp    = null;
		this.domain = null;
		
		this.ip = null;
		
		this.adBlocking     = null;
		this.autoGenPassphrases = null;
		this.vpnOnly        = null;
		this.dtls           = null;
		this.autoGuest      = null;
		
		this.upstreamDNS = null;
		
		this.defaultServiceData    = new ServiceData("");
		this.defaultHypervisorData = new HypervisorData("");
		
		this.servers = new Hashtable<String, ServerData>();
		this.devices = new Hashtable<String, ADeviceData>();
	}

	@Override
	public void read(JsonObject data)
	throws ADataException, JsonParsingException, IOException, URISyntaxException {
		super.setData(data);
		
		String include = data.getString("include", null);
		if (include != null) {
			this.readInclude(include);
		}
		else {
			this.defaultServiceData.read(data);
			
			this.upstreamDNS = getIPAddressArray(data, "dns");

			this.ip = new IPAddressString(data.getString("ip", null)).getAddress();

			this.myUser = data.getString("myuser", null);
			this.pgp    = data.getString("gpg", null);
			this.domain = data.getString("domain", "lan");
			
			this.dtls           = Boolean.parseBoolean(data.getString("dtls", DEFAULT_DTLS));
			this.adBlocking     = Boolean.parseBoolean(data.getString("adblocking", DEFAULT_ADBLOCKING));
			this.autoGenPassphrases = Boolean.parseBoolean(data.getString("autogenpasswds", DEFAULT_AUTOGENPASSWDS));
			this.vpnOnly        = Boolean.parseBoolean(data.getString("vpnonly", DEFAULT_VPNONLY));
			this.autoGuest      = Boolean.parseBoolean(data.getString("autoguest", DEFAULT_AUTOGUEST));

			readServers(data.getJsonObject("servers"));
			readInternalDevices(data.getJsonObject("internaldevices"));
			readExternalDevices(data.getJsonObject("externaldevices"));
			readUserDevices(data.getJsonObject("users"));
		}
	}

	private Set<IPAddress> getIPAddressArray(JsonObject data, String property)
	throws UnknownHostException {
		Set<IPAddress> addresses = new HashSet<IPAddress>();

		JsonArray jsonIPAddresses = data.getJsonArray(property);
		
		for (JsonValue ip : jsonIPAddresses) {
			addresses.add(new IPAddressString(ip.toString()).getAddress());
		}
		
		return addresses;
	}
	
	private void readInclude(String include)
	throws IOException, JsonParsingException, ADataException, URISyntaxException {
		String rawIncludeData = null;
		
		JsonReader jsonReader = null;
		
		rawIncludeData = new String(Files.readAllBytes(Paths.get(include)), StandardCharsets.UTF_8);
		jsonReader = Json.createReader(new StringReader(rawIncludeData));

		read(jsonReader.readObject());
	}

	private void readServers(JsonObject jsonServers)
	throws ADataException, JsonParsingException, IOException, URISyntaxException {
		for (String server : jsonServers.keySet()) {
			ServerData net = new ServerData(server);
			net.read(jsonServers.getJsonObject(server));
			
			this.servers.put(server, net);
		}
	}

	private void readExternalDevices(JsonObject jsonDevices)
	throws ADataException, JsonParsingException, IOException, URISyntaxException {
		for (String device : jsonDevices.keySet()) {
			ExternalDeviceData dev = new ExternalDeviceData(device);
			dev.read(jsonDevices.getJsonObject(device));
			
			this.devices.put(device, dev);
		}
	}

	private void readInternalDevices(JsonObject jsonDevices)
	throws ADataException, JsonParsingException, IOException, URISyntaxException {
		for (String device : jsonDevices.keySet()) {
			InternalDeviceData dev = new InternalDeviceData(device);
			dev.read(jsonDevices.getJsonObject(device));
			
			this.devices.put(device, dev);
		}
	}
	
	private void readUserDevices(JsonObject jsonDevices)
	throws ADataException, JsonParsingException, IOException, URISyntaxException {
		Set<String> devices = jsonDevices.keySet();

		//We will *always* need user devices, or we will have no way to SSH in!
		if (devices.isEmpty()) { throw new NoValidUsersException(); }
		
		for (String device : devices) {
			UserDeviceData userDevice = new UserDeviceData(device);
			userDevice.read(jsonDevices.getJsonObject(device));
			
			this.devices.put(device, userDevice);
		}
	}

	public final Set<String> getServerProfiles(String server)
	throws InvalidServerException {
		return getServerData(server).getProfiles();
	}
	

	// Network only data
	public final String getUser()
	throws NoValidUsersException {
		if (this.myUser ==  null) {
			throw new NoValidUsersException();
		}
		
		return this.myUser;
	}

	public final Set<IPAddress> getUpstreamDNSServers() {
		return this.upstreamDNS;
	}

	public final Boolean getUpstreamDNSIsTLS() {
		return this.dtls;
	}
	
	public final String getPGP() {
		return this.pgp;
	}
	
	/**
	 * Should we build an auto guest network?
	 */
	public final Boolean getAutoGuest() {
		return this.autoGuest;
	}
	
	/**
	 * Should we autogenerate passphrases for users who haven't set a default?
	 */
	public final Boolean getAutoGenPassphrasess() {
		return this.autoGenPassphrases;
	}
	
	/**
	 * Gets the netmask - hardcoded as /30.
	 *
	 * @return the netmask (255.255.255.252)
	 */
	public final IPAddress getNetmask() {
		return new IPAddressString(DEFAULT_NETMASK).getAddress();
	}
	
	/**
	 * This is either the IP of our router (if we're inside) or the public IP address (if it's an external resource)
	 */
	public final IPAddress getIP() throws InvalidIPAddressException {
		if (this.ip == null) {
			throw new InvalidIPAddressException();
		}

		return this.ip;
	}
	
	/**
	 * Should we do ad blocking at the router?
	 */
	public final Boolean getAdBlocking() {
		return this.adBlocking;
	}

	public final boolean getVpnOnly() {
		return this.vpnOnly;
	}
	

	public final String getDomain() {
		return this.domain;
	}
	
	/**
	 * Gets an arbitrary property from a server's Data.
	 * 
	 * Use this method with great care, you should only really call a wrapper
	 */
	public final String getProperty(String server, String property, Boolean isRequired)
	throws InvalidPropertyException {
		String value = this.servers.get(server).getStringProperty(property, null); 
		
		if (value == null && isRequired) {
			throw new InvalidPropertyException(); 
		}

		return value;
	}
	
	/**
	 * Gets an arbitrary property array.
	 * 
	 * Use this method with great care, you should only really call a wrapper
	 */
	public final Set<String> getPropertyArray(String machine, String property)
	throws InvalidPropertyArrayException, InvalidMachineException {
		return this.getAMachineData(machine).getPropertyArray(property);
	}
	
	/**
	 * Gets an arbitrary property object array.
	 *
	 * Use this method with great care, you should only really call a wrapper
	 */
	public final JsonArray getPropertyObjectArray(String machine, String property)
	throws InvalidMachineException {
		return this.getAMachineData(machine).getPropertyObjectArray(property);
	}
	
	private final Boolean isServer(String machine) {
		return this.servers.containsKey(machine);
	}
	
	private final Boolean isDevice(String machine) {
		return this.devices.containsKey(machine);
	}
	
	private final AMachineData getAMachineData(String machine)
	throws InvalidMachineException {
		if (isServer(machine)) {
			return this.getServerData(machine);
		}
		if (isDevice(machine)) {
			return this.getDevice(machine);
		}
		
		throw new InvalidMachineException();
	}
	
	private final ServiceData getServiceData(String service)
	throws InvalidServerException {
		if (isServer(service)) {
			return (ServiceData) this.servers.get(service);
		}
		else {
			throw new InvalidServerException();
		}
	}

	private final HypervisorData getHypervisorData(String hypervisor)
	throws InvalidServerException {
		if (isServer(hypervisor)) {
			return (HypervisorData) this.servers.get(hypervisor);
		}
		else {
			throw new InvalidServerException();
		}
	}
	public HostName getFQDN(String machine)
	throws InvalidMachineException {
		return getAMachineData(machine).getFQDN();
	}

	final public Set<String> getTypes(String server)
	throws InvalidServerException {
		Set<String> types = getServerData(server).getTypes();
		
		if (types.isEmpty()) {
			types = this.defaultServiceData.getTypes();
		}
		
		return types;
	}
	
	public Set<HostName> getCnames(String machine)
	throws InvalidMachineException {
		return getAMachineData(machine).getCnames();
	}
	
	public IPAddress getExternalIp(String machine)
	throws InvalidMachineException {
		return getAMachineData(machine).getExternalIp();
	}
	
	final public Set<String> getAdmins(String server)
	throws InvalidServerException {
		Set<String> admins = getServerData(server).getAdmins();
		
		if (admins.isEmpty()) {
			admins = this.defaultServiceData.getAdmins();
		}

		return admins;
	}
	
	public Integer getAdminPort(String server) {
		Integer port = this.servers.get(server).getAdminPort();
		
		if (port == null) {
			port = this.defaultServiceData.getAdminPort();
			if (port == null) {
				port = NetworkData.DEFAULT_ADMIN_PORT;
			}
		}
		
		return port;
	}
	
	public Integer getSSHPort(String server) {
		Integer port = this.servers.get(server).getSSHPort();
		
		if (port == null) {
			port = this.defaultServiceData.getSSHPort();
			if (port == null) {
				port = NetworkData.DEFAULT_SSH_PORT;
			}
		}
		
		return port;
	}

	public Boolean getAutoUpdate(String server) {
		Boolean update = this.servers.get(server).getUpdate();
		
		if (update == null) {
			update = this.defaultServiceData.getUpdate();
			if (update == null) {
				update = NetworkData.DEFAULT_UPDATE;
			}
		}
		
		return update;
	}

	public String getKeePassDB(String server)
	throws URISyntaxException {
		String db = this.servers.get(server).getKeePassDB();
		
		if (db == null) {
			db = this.defaultServiceData.getKeePassDB();
			if (db == null) {
				db = NetworkData.DEFAULT_KEEPASS_DB;
			}
		}
		
		return db;
	}
	
	public Set<NetworkInterfaceData> getLanIfaces(String machine)
	throws InvalidMachineException {
		return getAMachineData(machine).getLanInterfaces();
	}
	
	public Set<NetworkInterfaceData> getWanIfaces(String machine)
	throws InvalidMachineException {
		return getAMachineData(machine).getWanInterfaces();
	}

	public Integer getRam(String service)
	throws InvalidServerException {
		Integer ram = getServiceData(service).getRAM();
		
		if (ram == null) {
			ram = this.defaultServiceData.getRAM();
			if (ram == null) {
				ram = NetworkData.DEFAULT_RAM;
			}
		}
		
		return ram;
	}

	public Integer getCpus(String service)
	throws InvalidServerException {
		Integer cpus = getServiceData(service).getCPUs(); 
		
		if (cpus == null) {
			cpus = this.defaultServiceData.getCPUs();
			if (cpus == null) {
				cpus = NetworkData.DEFAULT_CPUS;
			}
		}
		
		return cpus;
	}
	
	public WANConnection getWanConnection(String server) {
		WANConnection connection = this.servers.get(server).getWanConnection();
		
		if (connection == null) {
			connection = this.defaultServiceData.getWanConnection();
		}
		
		return connection;
	}

	public String getDebianIsoUrl(String service)
	throws InvalidServerException {
		String url = getServiceData(service).getDebianIsoUrl();
		
		if (url == null) {
			url = this.defaultServiceData.getDebianIsoUrl();
			if (url == null) {
				try {
					url = NetworkData.DEFAULT_DEBIAN_ISO_DIR;

					URL isoVersion = new URL(url + "SHA512SUMS");
			        BufferedReader line = new BufferedReader(new InputStreamReader(isoVersion.openStream()));
			        String inputLine;
			        if ((inputLine = line.readLine()) != null) {
			            String[] netInst = inputLine.split(" ");
			            url += netInst[2];
			        }
			        line.close();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		return url;
	}

	public String getDebianIsoSha512(String service)
	throws InvalidServerException {
		String hash = getServiceData(service).getDebianIsoSha512();
		
		if (hash == null) {
			hash = this.defaultServiceData.getDebianIsoSha512();
			if (hash == null) {
				try {
					String url = NetworkData.DEFAULT_DEBIAN_ISO_DIR;

					URL isoVersion = new URL(url + "SHA512SUMS");
			        BufferedReader line = new BufferedReader(new InputStreamReader(isoVersion.openStream()));
			        String inputLine;
			        if ((inputLine = line.readLine()) != null) {
			            String[] netInst = inputLine.split(" ");
			            hash = netInst[0];
			        }
			        line.close();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		return hash;
	}

	public File getHypervisorThornsecBase(String hypervisor)
	throws InvalidServerException {
		File baseDir = getHypervisorData(hypervisor).getVmBase();
		
		if (baseDir == null) {
			baseDir = this.defaultHypervisorData.getVmBase();
			if (baseDir == null) {
				baseDir = new File(NetworkData.DEFAULT_THORNSECBASE);
			}
		}
	
		return baseDir;
	}

	public HostName getDebianMirror(String server)
	throws URISyntaxException {
		HostName mirror = this.servers.get(server).getDebianMirror();
		
		if (mirror == null) {
			mirror = this.defaultServiceData.getDebianMirror();
			if (mirror == null) {
				mirror = new HostName(NetworkData.DEFAULT_DEBIANMIRROR);
			}
		}
		
		return mirror;
	}

	public String getDebianDirectory(String server) {
		String directory = this.servers.get(server).getDebianDirectory();
		
		if (directory == null) {
			directory = this.defaultServiceData.getDebianDirectory();
			if (directory == null) {
				directory = NetworkData.DEFAULT_DEBIANDIR;
			}
		}
		
		return directory;
	}
	
	/**
	 * Warning: can return null...
	 * @param machine
	 * @return
	 * @throws InvalidMachineException
	 */
	public final String getFirewallProfile(String machine)
	throws InvalidMachineException {
		return getAMachineData(machine).getFirewallProfile();
	}
	
	public final Set<String> getDeviceNames() {
		return this.devices.keySet();
	}
	
	public final Set<String> getServerNames() {
		return this.servers.keySet();
	}
	
	private final ADeviceData getDevice(String device)
	throws InvalidMachineException, InvalidDeviceException {
		Object dev = getAMachineData(device);
		
		if (dev instanceof ADeviceData) {
			return (ADeviceData) dev;
		}
		else {
			throw new InvalidDeviceException();
		}
	}
	
	private final UserDeviceData getUserDeviceData(String user)
	throws InvalidUserException, InvalidMachineException {
		Object device = getAMachineData(user);
		
		if (device instanceof UserDeviceData) {
			return (UserDeviceData) device;
		}
		else {
			throw new InvalidUserException();
		}
	}
	
	private final ServerData getServerData(String server)
	throws InvalidServerException {
		Object device = this.servers.get(server);
		
		if (device instanceof ServerData) {
			return (ServerData) device;
		}
		else {
			throw new InvalidServerException();
		}
	}
	
	public final String getUserSSHKey(String user)
	throws InvalidUserException, InvalidMachineException {
		return getUserDeviceData(user).getSSHKey();
	}
	
	public final String getUserFullName(String user)
	throws InvalidUserException, InvalidMachineException {
		return getUserDeviceData(user).getFullName();
	}
	
	public final Boolean getMachineIsThrottled(String machine)
	throws InvalidMachineException {
		return getAMachineData(machine).getIsThrottled();
	}
	
	public final Boolean getDeviceIsManaged(String device)
	throws InvalidMachineException, InvalidDeviceException {
		return getDevice(device).getIsManaged();
	}

	public final Hashtable<Encapsulation, Set<HostName>> getListens(String machine)
	throws InvalidMachineException {
		return getAMachineData(machine).getListens();
	}
	
	public final String getDeviceType(String device) {
		String type = this.devices.get(device).getClass().getSimpleName().replace("DeviceData", "");
		
		assert !type.isEmpty();
		
		return type;
	}
	
	public final String getUserDefaultPassphrase(String user)
	throws InvalidUserException, InvalidMachineException {
		return getUserDeviceData(user).getDefaultPassphrase();
	}
	
	public final InternetAddress getEmailAddress(String machine)
	throws InvalidMachineException {
		return getAMachineData(machine).getEmailAddress();
	}
	
	public final Set<HostName> getEgresses(String machine)
	throws InvalidMachineException {
		return getAMachineData(machine).getEgresses();
	}
	
	public final Set<HostName> getForwards(String machine)
	throws InvalidMachineException {
		return getAMachineData(machine).getForwards();
	}

	public final Set<HostName> getIngresses(String machine)
	throws InvalidMachineException {
		return getAMachineData(machine).getIngresses();
	}
}
