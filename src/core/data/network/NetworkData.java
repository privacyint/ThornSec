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
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.stream.JsonParsingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import core.data.AData;
import core.data.machine.ADeviceData;
import core.data.machine.AMachineData;
import core.data.machine.AMachineData.Encapsulation;
import core.data.machine.AMachineData.MachineType;
import core.data.machine.ExternalDeviceData;
import core.data.machine.HypervisorData;
import core.data.machine.InternalDeviceData;
import core.data.machine.ServerData;
import core.data.machine.ServiceData;
import core.data.machine.UserDeviceData;
import core.data.machine.configuration.DiskData;
import core.data.machine.configuration.NetworkInterfaceData;
import core.data.machine.configuration.NetworkInterfaceData.Direction;
import core.data.machine.configuration.NetworkInterfaceData.Inet;
import core.exception.data.ADataException;
import core.exception.data.InvalidIPAddressException;
import core.exception.data.NoValidUsersException;
import core.exception.data.machine.InvalidMachineException;
import core.exception.data.machine.InvalidServerException;
import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;

/**
 * This class represents the state of our network *AS DEFINED IN THE JSON*
 *
 * If something isn't defined in our JSON, we will _return_ default values, as
 * required, or otherwise null.
 *
 * This is our "interface" between the data and the models ThornSec will build.
 */
public class NetworkData extends AData {
	/*
	 * To make this easier to read, please use a prefix which shows how far down the
	 * inheritance chain we can override this
	 */
	private static final String NETWORK_SERVER_SUBNET = "10.0.0.0/8";
	private static final String NETWORK_USER_SUBNET = "172.16.0.0/16";
	private static final String NETWORK_ADMIN_SUBNET = "172.20.0.0/16";
	private static final String NETWORK_INTERNAL_SUBNET = "172.24.0.0/16";
	private static final String NETWORK_EXTERNAL_SUBNET = "172.28.0.0/16";
	private static final String NETWORK_GUEST_SUBNET = "172.31.0.0/16";

	private static final Boolean NETWORK_ADBLOCKING = false;
	private static final Boolean NETWORK_AUTOGENPASSWDS = false;
	private static final Boolean NETWORK_VPNONLY = false;
	private static final Boolean NETWORK_AUTOGUEST = false;

	private static final String HYPERVISOR_THORNSECBASE = "/srv/ThornSec";

	private static final Boolean MACHINE_UPDATE = true;
	private static final Boolean MACHINE_IS_MANAGED = false;
	private static final Boolean MACHINE_IS_THROTTLED = true;

	private static final String SERVICE_DEBIAN_ISO_DIR = "https://gensho.ftp.acc.umu.se/debian-cd/current/amd64/iso-cd/";
	private static final String SERVER_DEBIANMIRROR = "free.hands.com";
	private static final String SERVER_DEBIANDIR = "/debian";
	private static final String MACHINE_NETMASK = "/32";
	private static final String MACHINE_KEEPASS_DB = "ThornSec.kdbx";
	private static final String MACHINE_DOMAIN = "lan";
	private static final String MACHINE_NETWORK_INTERFACE = "enp0s7"; // TODO: this is from memory

	private static final Inet MACHINE_INET = Inet.STATIC;

	private static final Integer MACHINE_RAM = 2048;
	private static final Integer MACHINE_CPUS = 1;

	private static final Integer MACHINE_SSH_PORT = 65422;
	private static final Integer MACHINE_ADMIN_PORT = 65422;

	private String myUser;
	private IPAddress configIP;
	private String domain;

	private Boolean adBlocking;
	private Boolean autoGenPassphrases;

	private Boolean vpnOnly;
	private Boolean autoGuest;

	private Collection<HostName> upstreamDNS;

	private final Map<String, String> subnets;

	private final ServiceData defaultServiceData;
	private final HypervisorData defaultHypervisorData;

	private Map<MachineType, Map<String, AMachineData>> machines;

	/**
	 * Create a new Network, populated with null values.
	 *
	 * @param label the network's name
	 */
	public NetworkData(String label) {
		super(label);

		this.myUser = null;
		this.domain = null;

		this.configIP = null;

		this.adBlocking = null;
		this.autoGenPassphrases = null;
		this.vpnOnly = null;
		this.autoGuest = null;

		this.upstreamDNS = null;

		this.subnets = new Hashtable<>();

		this.defaultServiceData = new ServiceData("");
		this.defaultHypervisorData = new HypervisorData("");

		this.machines = null;
	}

	private void readInclude(String include) throws IOException, JsonParsingException, ADataException, URISyntaxException {
		String rawIncludeData = null;

		JsonReader jsonReader = null;

		rawIncludeData = new String(Files.readAllBytes(Paths.get(include)), StandardCharsets.UTF_8);
		jsonReader = Json.createReader(new StringReader(rawIncludeData));

		read(jsonReader.readObject());
	}

	private ServerData readServer(String label, JsonObject dataObject) throws JsonParsingException, ADataException, IOException, URISyntaxException {
		// We have to read it in first to find out what it is - we can then replace it
		// with a specialised version
		ServerData serverData = new ServerData(label);
		serverData.read(dataObject);

		// Some servers don't have types set, as they inherit them. Let's make sure they
		// actually do inherit them.
		Collection<MachineType> serverTypes = serverData.getTypes();
		if (serverTypes == null) {
			serverTypes = this.defaultServiceData.getTypes();

			if (serverTypes == null) {
				serverTypes = new HashSet<>();
			}
		}

		// If we've just hit a hypervisor machine, we need to dig a little.
		if (serverTypes.contains(MachineType.HYPERVISOR)) {
			serverData = new HypervisorData(label);
			// Read in data to the newly specialised object
			serverData.read(dataObject);

			// They *should* contain information about their services
			if (dataObject.containsKey("services")) {
				final JsonObject servicesData = dataObject.getJsonObject("services");

				for (final String serviceLabel : servicesData.keySet()) {
					final ServerData service = readServer(serviceLabel, servicesData.getJsonObject(serviceLabel));

					// Register the service across various parts of our network...
					//putMachine(MachineType.SERVICE, service);
					((HypervisorData) serverData).addService(service);
				}
			} else {
				System.out.println("No services found on " + label);
			}
		} else if (serverTypes.contains(MachineType.SERVICE)) {
			serverData = new ServiceData(label);
			// Read in data to the newly specialised object
			serverData.read(dataObject);
		}

		for (final MachineType type : serverTypes) {
			putMachine(type, serverData);
		}

		putMachine(MachineType.SERVER, serverData);

		return serverData;
	}

	/**
	 * This is where we build the objects for our network.
	 */
	@Override
	public void read(JsonObject networkJSONData) throws ADataException, JsonParsingException, IOException, URISyntaxException {
		super.setData(networkJSONData);

		// Read in any include files, if we have them.
		if (networkJSONData.containsKey("includes")) {
			for (final JsonValue include : networkJSONData.getJsonArray("includes")) {
				readInclude((((JsonString) include).getString()));
			}
		}

		this.defaultServiceData.read(networkJSONData);

		if (networkJSONData.containsKey("upstream_dns")) {
			this.upstreamDNS = getHostNameArray("upstream_dns");
		}

		if (networkJSONData.containsKey("network_config_ip")) {
			this.configIP = new IPAddressString(networkJSONData.getString("network_config_ip").replaceAll("[^\\.0-9]", "")).getAddress();
		}

		if (networkJSONData.containsKey("my_ssh_user")) {
			this.myUser = networkJSONData.getJsonString("my_ssh_user").getString();
		}
		if (networkJSONData.containsKey("domain")) {
			this.domain = networkJSONData.getJsonString("domain").getString();
		} else {
			this.domain = MACHINE_DOMAIN;
		}

		this.adBlocking = networkJSONData.getBoolean("adblocking", NETWORK_ADBLOCKING);
		this.autoGenPassphrases = networkJSONData.getBoolean("autogen_passwds", NETWORK_AUTOGENPASSWDS);
		this.vpnOnly = networkJSONData.getBoolean("vpn_only", NETWORK_VPNONLY);
		this.autoGuest = networkJSONData.getBoolean("guest_network", NETWORK_AUTOGUEST);

		// Set subnets as required
		if (networkJSONData.containsKey("subnets")) {
			final JsonObject jsonSubnets = networkJSONData.getJsonObject("subnets");

			for (final String subnet : jsonSubnets.keySet()) {
				this.subnets.put(subnet, jsonSubnets.getJsonString(subnet).getString());
			}
		}

		// Look for "servers" first
		if (networkJSONData.containsKey("servers")) {
			final JsonObject jsonServerData = networkJSONData.getJsonObject("servers");

			for (final String label : jsonServerData.keySet()) {
				readServer(label, jsonServerData.getJsonObject(label));
			}
		}

		// Then
		if (networkJSONData.containsKey("peripherals")) {
			final JsonObject jsonDevices = networkJSONData.getJsonObject("peripherals");

			for (final String jsonDevice : jsonDevices.keySet()) {
				final InternalDeviceData device = new InternalDeviceData(jsonDevice);
				device.read(jsonDevices.getJsonObject(jsonDevice));

				putMachine(MachineType.DEVICE, device);
				putMachine(MachineType.INTERNAL_ONLY, device);
			}
		}

		if (networkJSONData.containsKey("guests")) {
			final JsonObject jsonDevices = networkJSONData.getJsonObject("guests");

			for (final String jsonDevice : jsonDevices.keySet()) {
				final ExternalDeviceData device = new ExternalDeviceData(jsonDevice);
				device.read(jsonDevices.getJsonObject(jsonDevice));

				putMachine(MachineType.DEVICE, device);
				putMachine(MachineType.EXTERNAL_ONLY, device);
			}
		}

		if (networkJSONData.containsKey("users")) {
			final JsonObject jsonDevices = networkJSONData.getJsonObject("users");

			for (final String jsonDevice : jsonDevices.keySet()) {
				final UserDeviceData device = new UserDeviceData(jsonDevice);
				device.read(jsonDevices.getJsonObject(jsonDevice));

				putMachine(MachineType.DEVICE, device);
				putMachine(MachineType.USER, device);
			}
		} else { // We will *always* need user devices, or we will have no way to SSH in!
			throw new NoValidUsersException();
		}
	}

	private void putMachine(MachineType type, AMachineData... newMachineData) {
		Map<MachineType, Map<String, AMachineData>> currentMachines = getMachines();
		if (currentMachines == null) {
			currentMachines = new Hashtable<>();
			this.machines = new Hashtable<>();
		}
		assert (this.machines != null);

		Map<String, AMachineData> machines = currentMachines.get(type);
		if (machines == null) {
			machines = new LinkedHashMap<>(); // Let's keep this is a way which can be predictably iterated!
		}

		for (final AMachineData machineData : newMachineData) {
			machines.put(machineData.getLabel(), machineData);
		}

		currentMachines.put(type, machines);

		this.machines = currentMachines;
	}

	/**
	 * @return the current network machines, or {@code null}
	 */
	private Map<MachineType, Map<String, AMachineData>> getMachines() {
		return this.machines;
	}

	/**
	 * A given machine, or {@code null} if it's not there
	 *
	 * @param type
	 * @param label
	 * @return
	 */
	public AMachineData getMachine(MachineType type, String label) {
		return getMachines().get(type).get(label);
	}

	/**
	 * Returns a given machine from "somewhere" in the network.
	 *
	 * Treat with caution.
	 *
	 * @param labelToFind
	 * @return
	 */
	private AMachineData getMachine(String label) {
		AMachineData machine = null;

		for (final MachineType type : getMachines().keySet()) {
			machine = getMachine(type, label);

			if (machine != null) { // Found it!
				break;
			}
		}

		return machine;
	}

	/**
	 * Returns machines of a given type
	 *
	 * @param type
	 * @return
	 */
	public Map<String, AMachineData> getMachines(MachineType type) {
		return getMachines().get(type);
	}

	private Collection<HostName> getHostNameArray(String key) throws UnknownHostException {
		Collection<HostName> hosts = null;

		if (getData().containsKey(key)) {
			hosts = new ArrayList<>();
			final JsonArray jsonHosts = getData().getJsonArray(key);

			for (final JsonValue host : jsonHosts) {
				hosts.add((new HostName(((JsonString) host).getString())));
			}
		}

		return hosts;
	}

	// Network only data
	public final String getUser() throws NoValidUsersException {
		if (this.myUser == null) {
			throw new NoValidUsersException();
		}

		return this.myUser;
	}

	/**
	 * @return the upstream DNS server addresses
	 */
	public final Collection<HostName> getUpstreamDNSServers() {
		return this.upstreamDNS;
	}

	/**
	 * Should we build an auto guest network?
	 */
	public final Boolean buildAutoGuest() {
		return this.autoGuest;
	}

	/**
	 * Should we autogenerate passphrases for users who haven't set a default?
	 */
	public final Boolean autoGenPassphrasess() {
		return this.autoGenPassphrases;
	}

	/**
	 * Gets the netmask - hardcoded as /30.
	 *
	 * @return the netmask (255.255.255.252)
	 */
	public final IPAddress getNetmask() {
		return new IPAddressString(MACHINE_NETMASK).getAddress();
	}

	/**
	 * This is either the IP of our router (if we're inside) or the public IP
	 * address (if it's an external resource)
	 */
	public final IPAddress getIP() throws InvalidIPAddressException {
		if (this.configIP == null) {
			throw new InvalidIPAddressException("You must set a valid IP address for this network");
		}

		return this.configIP;
	}

	/**
	 * Should we do ad blocking at the router?
	 */
	public final Boolean adBlocking() {
		return this.adBlocking;
	}

	/**
	 * Do we require users to be on a VPN connection to use our services? (This is
	 * only useful for internal services...)
	 */
	public final boolean vpnOnly() {
		return this.vpnOnly;
	}

	/**
	 * @return the domain which applies to this network
	 */
	public final String getDomain() {
		return this.domain;
	}

	/**
	 *
	 * @param server Server's name
	 * @return All types assigned to the given machine, or default values if not
	 *         explicitly set
	 */
	final public Collection<MachineType> getTypes(String server) {
		Collection<MachineType> types = ((ServerData) getMachine(MachineType.SERVER, server)).getTypes();

		if (types == null) {
			types = this.defaultServiceData.getTypes();
		}

		assert (types != null);

		return types;
	}

	/**
	 * Get a given machine's NICs as an iterable
	 *
	 * @param machine Machine's name
	 * @return NICs
	 * @throws JsonParsingException
	 * @throws ADataException
	 * @throws IOException
	 */
	final public Map<Direction, Collection<NetworkInterfaceData>> getNetworkInterfaces(String machine) throws JsonParsingException, ADataException, IOException {
		Map<Direction, Collection<NetworkInterfaceData>> interfaces = getMachine(machine).getNetworkInterfaces();

		if (interfaces == null) {
			interfaces = this.defaultServiceData.getNetworkInterfaces();
			if (interfaces == null) {
				interfaces = new Hashtable<>();

				final HashSet<NetworkInterfaceData> automagic = new HashSet<>();
				final NetworkInterfaceData defaultIface = new NetworkInterfaceData(machine);

				final JsonObjectBuilder defaultNetworkInterfaceData = Json.createObjectBuilder();
				defaultNetworkInterfaceData.add("iface", NetworkData.MACHINE_NETWORK_INTERFACE);
				defaultNetworkInterfaceData.add("inet", Inet.STATIC.toString());
				defaultNetworkInterfaceData.add("comment", "This NIC was automagically built using default values.");

				defaultIface.read(defaultNetworkInterfaceData.build());
				automagic.add(defaultIface);

				interfaces.put(Direction.LAN, automagic);
			}
		}

		return interfaces;
	}

	public Collection<String> getCNAMEs(String machine) throws InvalidMachineException {
		return getMachine(machine).getCNAMEs();
	}

	public Collection<IPAddress> getExternalIPs(String machine) throws InvalidMachineException {
		return getMachine(machine).getExternalIPs();
	}

	final public Collection<String> getAdmins(String server) throws InvalidServerException {
		Collection<String> admins = ((ServerData) getMachine(MachineType.SERVER, server)).getAdmins();

		if (admins == null) {
			admins = this.defaultServiceData.getAdmins();
		}

		return admins;
	}

	public Integer getAdminPort(String server) {
		Integer port = ((ServerData) getMachine(MachineType.SERVER, server)).getAdminPort();

		if (port == null) {
			port = this.defaultServiceData.getAdminPort();
			if (port == null) {
				port = NetworkData.MACHINE_ADMIN_PORT;
			}
		}

		return port;
	}

	public Map<String, String> getSubnets() {
		return this.subnets;
	}

	public String getSubnet(MachineType subnet, String defaultSubnet) {
		return this.subnets.getOrDefault(subnet.toString(), defaultSubnet);
	}

	public String getUserSubnet() {
		return getSubnet(MachineType.USER, NETWORK_USER_SUBNET);
	}

	public String getAdminSubnet() {
		return getSubnet(MachineType.ADMIN, NETWORK_ADMIN_SUBNET);
	}

	public String getGuestSubnet() {
		return getSubnet(MachineType.GUEST, NETWORK_GUEST_SUBNET);
	}

	public String getServerSubnet() {
		return getSubnet(MachineType.SERVER, NETWORK_SERVER_SUBNET);
	}

	public String getInternalSubnet() {
		return getSubnet(MachineType.INTERNAL_ONLY, NETWORK_INTERNAL_SUBNET);
	}

	public String getExternalSubnet() {
		return getSubnet(MachineType.EXTERNAL_ONLY, NETWORK_EXTERNAL_SUBNET);
	}

	public Integer getSSHPort(String server) {
		Integer port = ((ServerData) getMachine(MachineType.SERVER, server)).getSSHPort();

		if (port == null) {
			port = this.defaultServiceData.getSSHPort();
			if (port == null) {
				port = NetworkData.MACHINE_SSH_PORT;
			}
		}

		return port;
	}

	public Boolean getAutoUpdate(String server) {
		Boolean update = ((ServerData) getMachine(MachineType.SERVER, server)).getUpdate();

		if (update == null) {
			update = this.defaultServiceData.getUpdate();
			if (update == null) {
				update = NetworkData.MACHINE_UPDATE;
			}
		}

		return update;
	}

	public String getKeePassDB(String server) throws URISyntaxException {
		String db = ((ServerData) getMachine(MachineType.SERVER, server)).getKeePassDB();

		if (db == null) {
			db = this.defaultServiceData.getKeePassDB();
			if (db == null) {
				db = NetworkData.MACHINE_KEEPASS_DB;
			}
		}

		return db;
	}

	public Integer getRAM(String server) throws InvalidServerException {
		Integer ram = ((ServerData) getMachine(MachineType.SERVER, server)).getRAM();

		if (ram == null) {
			ram = this.defaultServiceData.getRAM();
			if (ram == null) {
				ram = NetworkData.MACHINE_RAM;
			}
		}

		return ram;
	}

	public Integer getCPUs(String server) throws InvalidServerException {
		Integer cpus = ((ServerData) getMachine(MachineType.SERVER, server)).getCPUs();

		if (cpus == null) {
			cpus = this.defaultServiceData.getCPUs();
			if (cpus == null) {
				cpus = NetworkData.MACHINE_CPUS;
			}
		}

		return cpus;
	}

	public String getDebianIsoUrl(String service) throws InvalidServerException {
		String url = ((ServiceData) getMachine(MachineType.SERVICE, service)).getDebianIsoUrl();

		if (url == null) {
			url = this.defaultServiceData.getDebianIsoUrl();
			if (url == null) {
				try {
					url = NetworkData.SERVICE_DEBIAN_ISO_DIR;

					final URL isoVersion = new URL(url + "SHA512SUMS");
					final BufferedReader line = new BufferedReader(new InputStreamReader(isoVersion.openStream()));
					String inputLine;
					if ((inputLine = line.readLine()) != null) {
						final String[] netInst = inputLine.split(" ");
						url += netInst[2];
					}
					line.close();
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		}

		return url;
	}

	public String getDebianIsoSha512(String service) throws InvalidServerException {
		String hash = ((ServiceData) getMachine(MachineType.SERVICE, service)).getDebianIsoSha512();

		if (hash == null) {
			hash = this.defaultServiceData.getDebianIsoSha512();
			if (hash == null) {
				try {
					final String url = NetworkData.SERVICE_DEBIAN_ISO_DIR;

					final URL isoVersion = new URL(url + "SHA512SUMS");
					final BufferedReader line = new BufferedReader(new InputStreamReader(isoVersion.openStream()));
					String inputLine;
					if ((inputLine = line.readLine()) != null) {
						final String[] netInst = inputLine.split(" ");
						hash = netInst[0];
					}
					line.close();
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		}

		return hash;
	}

	public File getHypervisorThornsecBase(String hypervisor) throws InvalidServerException {
		final ServerData data = (ServerData) getMachine(MachineType.HYPERVISOR, hypervisor);
		File baseDir = ((HypervisorData) data).getVmBase();

		if (baseDir == null) {
			baseDir = this.defaultHypervisorData.getVmBase();
			if (baseDir == null) {
				baseDir = new File(NetworkData.HYPERVISOR_THORNSECBASE);
			}
		}

		return baseDir;
	}

	public URL getDebianMirror(String server) throws URISyntaxException, MalformedURLException {
		URL mirror = ((ServerData) getMachine(MachineType.SERVER, server)).getDebianMirror();

		if (mirror == null) {
			mirror = this.defaultServiceData.getDebianMirror();
			if (mirror == null) {
				mirror = new URL(NetworkData.SERVER_DEBIANMIRROR);
			}
		}

		return mirror;
	}

	public String getDebianDirectory(String server) {
		String directory = ((ServerData) getMachine(MachineType.SERVER, server)).getDebianDirectory();

		if (directory == null) {
			directory = this.defaultServiceData.getDebianDirectory();
			if (directory == null) {
				directory = NetworkData.SERVER_DEBIANDIR;
			}
		}

		return directory;
	}

	public Boolean isManaged(String label) {
		Boolean managed = ((ADeviceData) getMachine(MachineType.DEVICE, label)).isManaged();

		if (managed == null) {
			managed = NetworkData.MACHINE_IS_MANAGED;
		}

		return managed;
	}

	public InternetAddress getEmailAddress(String label) throws AddressException {
		final AMachineData machine = getMachine(label);
		InternetAddress address = machine.getEmailAddress();

		if (address == null) {
			address = new InternetAddress(label + "@" + getDomain(label).getHost());
		}

		return address;
	}

	public HostName getDomain(String label) {
		HostName domain = getMachine(label).getDomain();

		if (domain == null) {
			domain = this.defaultServiceData.getDomain();
		}

		return domain;
	}

	public Boolean isThrottled(String label) {
		Boolean throttled = getMachine(label).isThrottled();

		if (throttled == null) {
			throttled = this.defaultServiceData.isThrottled();
			if (throttled == null) {
				throttled = NetworkData.MACHINE_IS_THROTTLED;
			}
		}

		return throttled;
	}

	public Map<Encapsulation, Collection<Integer>> getListens(String label) {
		Map<Encapsulation, Collection<Integer>> listens = getMachine(label).getListens();

		if (listens == null) {
			listens = this.defaultServiceData.getListens();
			if (listens == null) {
				return new Hashtable<>();
			}
		}
		return listens;
	}

	public Collection<HostName> getIngresses(String label) {
		Collection<HostName> ingresses = getMachine(label).getIngresses();

		if (ingresses == null) {
			ingresses = this.defaultServiceData.getIngresses();
			if (ingresses == null) {
				return new HashSet<>();
			}
		}
		return ingresses;
	}

	public Collection<HostName> getEgresses(String label) {
		Collection<HostName> egresses = getMachine(label).getEgresses();

		if (egresses == null) {
			egresses = this.defaultServiceData.getEgresses();
			if (egresses == null) {
				return new HashSet<>();
			}
		}
		return egresses;
	}

	public Collection<String> getForwards(String label) {
		Collection<String> forwards = getMachine(label).getForwards();

		if (forwards == null) {
			forwards = this.defaultServiceData.getForwards();
			if (forwards == null) {
				return new HashSet<>();
			}
		}
		return forwards;
	}

	public Map<String, Collection<Integer>> getDNATs(String label) {
		final Map<String, Collection<Integer>> dnats = getMachine(label).getDNATs();

		if (dnats == null) {
			return new LinkedHashMap<>();
		}

		return dnats;
	}

	public String getFirewallProfile(String label) {
		String profile = ((ServerData) getMachine(MachineType.SERVER, label)).getFirewallProfile();

		if (profile == null) {
			profile = this.defaultServiceData.getFirewallProfile();
		}
		return profile;
	}

	public Collection<String> getProfiles(String label) {
		Collection<String> profiles = ((ServerData) getMachine(MachineType.SERVER, label)).getProfiles();

		if (profiles == null) {
			profiles = this.defaultServiceData.getProfiles();
			if (profiles == null) {
				return new HashSet<>();
			}
		}
		return profiles;
	}

	public String getSSHKey(String admin) {
		return ((UserDeviceData) getMachine(MachineType.USER, admin)).getSSHKey();
	}

	public String getProperty(String label, String property) {
		return getMachine(label).getData().getString(property, null);
	}

	public Map<String, AMachineData> getExternalOnlyDevices() {
		return this.getMachines(MachineType.EXTERNAL_ONLY);
	}

	public Map<String, AMachineData> getInternalOnlyDevices() {
		return this.getMachines(MachineType.INTERNAL_ONLY);
	}

	public Map<String, AMachineData> getUserDevices() {
		return this.getMachines(MachineType.USER);
	}

	public Map<String, AMachineData> getServers() {
		return this.getMachines(MachineType.SERVER);
	}
	
	public ServerData getServer(String label) {
		return (ServerData) this.getServers().get(label);
	}

	public Collection<DiskData> getDisks(String label) {
		return ((ServiceData)this.getServer(label)).getDisks();
	}

	public ServiceData getService(String label) {
		return (ServiceData) this.getServer(label);
	}
}
