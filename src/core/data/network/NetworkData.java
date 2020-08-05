/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.data.network;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.IncompatibleAddressException;
import core.data.AData;
import core.data.machine.AMachineData;
import core.data.machine.AMachineData.MachineType;
import core.data.machine.ExternalDeviceData;
import core.data.machine.HypervisorData;
import core.data.machine.InternalDeviceData;
import core.data.machine.ServerData;
import core.data.machine.ServiceData;
import core.data.machine.UserDeviceData;
import core.exception.data.ADataException;
import core.exception.data.InvalidHostException;
import core.exception.data.InvalidIPAddressException;
import core.exception.data.InvalidPropertyException;
import core.exception.data.MissingPropertiesException;
import core.exception.data.NoValidUsersException;
import core.exception.data.machine.InvalidMachineException;
import core.exception.data.machine.InvalidUserException;
import core.exception.runtime.InvalidTypeException;

/**
 * This class represents the state of our network *AS DEFINED IN THE JSON*
 *
 * This is our "interface" between the data and the models ThornSec will build.
 */
public class NetworkData extends AData {
	private String myUser;
	private IPAddress configIP;
	private String domain;

	private Boolean adBlocking;
	private Boolean autoGenPassphrases;

	private Boolean vpnOnly;
	private Boolean autoGuest;

	private Set<HostName> upstreamDNS;

	private final Map<MachineType, IPAddress> subnets;

	private Map<String, AMachineData> machines;
	private Map<String, UserData> users;

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

		this.machines = new LinkedHashMap<>();
		this.users = new LinkedHashMap<>();
	}

	/**
	 * This is where we build the objects for our network.
	 */
	@Override
	public void read(JsonObject networkJSONData) throws ADataException {
		super.setData(networkJSONData);

		readIncludes();
		readUpstreamDNS();
		readNetworkDomain();
		readNetworkConfigIP();
		readNetworkConfigUser();
		readAdBlocking();
		readAutoGenPasswords();
		readVPNOnly();
		readAutoGuest();
		readSubnets();
		readUsers();
		readMachines();
	}

	private HypervisorData readHyperVisor(String label, JsonObject hypervisorData)
			throws ADataException {
		
		HypervisorData hv = new HypervisorData(label);
		hv.read(getData());
		hv.read(hypervisorData);
		
		// They *should* contain information about their services
		if (!hypervisorData.containsKey("services")) {
			throw new MissingPropertiesException(label +
					" doesn't contain any services. Please check your config");
		}
		
		JsonObject services = hypervisorData.getJsonObject("services");
		
		for (final String serviceLabel : services.keySet()) {
			ServiceData service = readService(serviceLabel, services.getJsonObject(serviceLabel));
			
			hv.addService(service);
			service.setHypervisor(hv);
			
			this.putMachine(service);
		}
		
		return hv;
	}
	
	private ServiceData readService(String label, JsonObject serviceData) throws ADataException {
		final ServiceData service = new ServiceData(label);
		service.read(getData());
		service.read(serviceData);

		return service;
	}
	
	/**
	 * Read in a given server object, adding it to our network.
	 * 
	 * If the object is a Hypervisor, interrogate it further for its nested
	 * services, read those in, and add those to the network too.
	 * 
	 * @param label The server's label
	 * @param serverDataObject
	 * @throws ADataException if attempting to add a machine with a duplicate label
	 */
	private void readServer(String label, JsonObject serverDataObject) throws ADataException {
		// We have to read it in first to find out what it is - we can then
		// replace it with a specialised version
		ServerData serverData = new ServerData(label);
		serverData.read(getData()); //Read in network-level defaults
		serverData.read(serverDataObject); //Read in server-specific settings

		// If we've just hit a hypervisor machine, we need to dig a little,
		// because the services are nested inside
		if (serverData.isType(MachineType.HYPERVISOR)) {
			serverData = readHyperVisor(label, serverDataObject);
		}
		
		this.putMachine(serverData);
	}

	private void readNetworkConfigUser() {
		if (!getData().containsKey("my_ssh_user")) {
			return;
		}
		
		this.myUser = getData().getJsonString("my_ssh_user").getString();
	}

	private void readMachines() throws ADataException {
		readServers();
		readInternalDevices();
		readExternalDevices();
		readUserDevices();
	}

	private void readUserDevices() throws ADataException {
		if (!getData().containsKey("users")) {
			return;
		}
		
		final JsonObject jsonDevices = getData().getJsonObject("users");

		for (final String jsonDevice : jsonDevices.keySet()) {
			final UserDeviceData device = new UserDeviceData(jsonDevice);
			device.read(jsonDevices.getJsonObject(jsonDevice));

			if (device.getNetworkInterfaces().isPresent()) {
				putMachine(device);
			}
		}
	}
	
	private void readExternalDevices() throws ADataException {
		if (!getData().containsKey("guests")) {
			return;
		}
		
		final JsonObject jsonDevices = getData().getJsonObject("guests");

		for (final String jsonDevice : jsonDevices.keySet()) {
			final ExternalDeviceData device = new ExternalDeviceData(jsonDevice);
			device.read(jsonDevices.getJsonObject(jsonDevice));

			putMachine(device);
		}
	}

	private void readInternalDevices() throws ADataException {
		if (getData().containsKey("peripherals")) {
			final JsonObject jsonDevices = getData().getJsonObject("peripherals");

			for (final String jsonDevice : jsonDevices.keySet()) {
				final InternalDeviceData device = new InternalDeviceData(jsonDevice);
				device.read(jsonDevices.getJsonObject(jsonDevice));

				putMachine(device);
			}
		}	
	}

	private void readServers() throws ADataException {
		if (!getData().containsKey("servers")) {
			return;
		}
		
		final JsonObject jsonServers = getData().getJsonObject("servers");
		
		for (final String label : jsonServers.keySet()) {
			readServer(label, jsonServers.getJsonObject(label));
		}
	}

	/**
	 * Read in any Subnet declarations made in the JSON
	 * @throws InvalidIPAddressException 
	 * @throws InvalidTypeException 
	 * @throws InvalidPropertyException 
	 */
	private void readSubnets() throws InvalidIPAddressException, InvalidPropertyException {
		if (!getData().containsKey("subnets")) {
			return;
		}

		final JsonObject jsonSubnets = getData().getJsonObject("subnets");
		
		for (final String label : jsonSubnets.keySet()) {
			String ip = ((JsonString)jsonSubnets.getJsonString(label)).getString();
			readSubnet(label, ip);
		}
	}
	
	private void readSubnet(String label, String ip) throws InvalidIPAddressException, InvalidPropertyException {
		try {
			this.subnets.put(MachineType.fromString(label), new IPAddressString(ip).toAddress());
		} catch (AddressStringException | IncompatibleAddressException e) {
			throw new InvalidIPAddressException(ip + " is an invalid subnet");
		} catch (InvalidTypeException e) {
			throw new InvalidPropertyException(label + " is not a valid Machine Type");
		}
	}

	@Deprecated //TODO: This is a property of a Router
	private void readAutoGuest() {
		if (!getData().containsKey("guest_network")) {
			return;
		}
		
		this.autoGuest = getData().getBoolean("guest_network");
	}

	@Deprecated //TODO: This is a property of a Router
	private void readVPNOnly() {
		if (!getData().containsKey("vpn_only")) {
			return;
		}

		this.vpnOnly = getData().getBoolean("vpn_only");
	}

	/**
	 * Read in whether we should autogenerate secure passwords, or set the
	 * default from {@link NETWORK_AUTOGENPASSWDS}
	 */
	private void readAutoGenPasswords() {
		if (!getData().containsKey("autogen_passwds")) {
			return;
		}

		this.autoGenPassphrases = getData().getBoolean("autogen_passwds");
	}

	/**
	 * Read in whether or not we should be doing network-level ad-blocking.
	 */
	@Deprecated //TODO: this should be in Router(), not here.
	private void readAdBlocking() {
		if (!getData().containsKey("adblocking")) {
			return;
		}

		this.adBlocking = getData().getBoolean("adblocking");
	}

	/**
	 * Get the gateway IP address for our configuration
	 * @throws InvalidIPAddressException 
	 */
	private void readNetworkConfigIP() throws InvalidIPAddressException {
		if (!getData().containsKey("network_config_ip")) {
			return;
		}
		
		this.configIP = new IPAddressString(getData().getString("network_config_ip")
				.replaceAll("[^\\.0-9]", ""))
				.getAddress();
		
		if (this.configIP == null) {
			throw new InvalidIPAddressException(getData().getString("network_config_ip"));
		}
	}

	/**
	 * Read in our Network-level domain from the JSON
	 */
	private void readNetworkDomain() {
		if (!getData().containsKey("domain")) {
			return;
		}
		
		this.domain = getData().getJsonString("domain").getString();
	}

	private void readUpstreamDNS() throws InvalidHostException {
		if (!getData().containsKey("upstream_dns")) {
			return;
		}
		
		this.upstreamDNS = getHostNameArray("upstream_dns");
	}

	/**
	 * Parse and merge a given file with this. The include argument
	 * must be an absolute path to a JSON file, in your Operating System's
	 * native path style.
	 * 
	 * @throws InvalidPropertyException if a path is invalid
	 */

	private void readIncludes() throws InvalidPropertyException {
		if (!getData().containsKey("includes")) {
			return;
		}
		
		for (JsonValue path : getData().getJsonArray("includes")) {
			readInclude(((JsonString) path).getString());
		}
	}

	/**
	 * Parse and merge a given file with this. The include argument
	 * must be an absolute path to a JSON file, in your Operating System's
	 * native path style.
	 * 
	 * @param includePath Absolute path to the JSON file to be read into our
	 * 		NetworkData
	 * @throws InvalidPropertyException if the path to the JSON is invalid
	 */
	private void readInclude(String includePath) throws InvalidPropertyException {
		try {
			Path file = Path.of(includePath);
			String rawUTF8Data = new String(Files.readAllBytes(file),
											StandardCharsets.UTF_8);
			JsonReader jsonReader = Json.createReader(new StringReader(rawUTF8Data));
			JsonObject includeData = jsonReader.readObject();
			JsonObject data = getData();
			data.putAll(includeData);
			
			setData(data);
		}
		catch (IOException e) {
			throw new InvalidPropertyException("Invalid path to include:"
					+ includePath);
		}
	}

	/**
	 * Creates UserData objects from a given JSON network
	 * @param networkJSONData the whole network
	 * @throws InvalidUserException If there are duplicate users declared
	 * in this network's data
	 * @throws NoValidUsersException If there aren't any Users to 
	 */
	private void readUsers() throws InvalidUserException, NoValidUsersException {
		if (!getData().containsKey("users") || getData().getJsonObject("users").isEmpty()) {
			throw new NoValidUsersException("There must be at least one user on"
					+ " your network");
		}
		
		JsonObject jsonUsers = getData().getJsonObject("users");
		
		for (final String userLabel : jsonUsers.keySet()) {
			UserData user = new UserData(userLabel);
			user.read(jsonUsers.getJsonObject(userLabel));
			
			if (this.users.put(user.getLabel(), user) != null) {
				throw new InvalidUserException("You have a duplicate user ("
						+ user.getLabel() + ") in your network");
			}
		}
	}

	/**
	 * Add (a) given machine(s) to your network. Machines must have unique labels
	 *  
	 * @param machinesData The machine to add to our network
	 * @throws InvalidMachineException on attempting to add a model with a
	 * 		duplicate label
	 */
	private void putMachine(AMachineData... machinesData) throws InvalidMachineException {
		for (AMachineData machineData : machinesData) {
			if (this.machines.put(machineData.getLabel(), machineData) != null) {
				throw new InvalidMachineException("You have a duplicate machine ("
						+ machineData.getLabel() + ") in your network");
			}
		}
	}

	/**
	 * Get the MachineData for all machines on this network.
	 * 
	 * @return a map of all machines' data on the network, indexed by label
	 */
	public Map<String, AMachineData> getMachinesData() {
		return this.machines;
	}

	/**
	 * Get a given machine's data. You're not guaranteed that this machine is
	 * there, if you're reading from a config file 
	 *
	 * @param label the label of the Machine you wish to get
	 * @return
	 */
	public AMachineData getMachineData(String label) {
		return getMachinesData().get(label);
	}
	
	private Set<HostName> getHostNameArray(String key) throws InvalidHostException {
		Set<HostName> hosts = null;

		if (getData().containsKey(key)) {
			hosts = new HashSet<>();
			final JsonArray jsonHosts = getData().getJsonArray(key);

			for (final JsonValue jsonHost : jsonHosts) {
				HostName host = new HostName(((JsonString) jsonHost).getString());
				
				if (!host.isValid()) {
					throw new InvalidHostException(((JsonString) jsonHost).getString()
							+ " is an invalid host");
				}
				
				hosts.add(host);
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
	@Deprecated //TODO: this is a property of a Router
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
		//TODO: THIS
		return new IPAddressString("255.255.255.252").getAddress();
	}

	/**
	 * This is either the IP of our router (if we're inside) or the public IP
	 * address (if it's an external resource)
	 */
	public final IPAddress getConfigIP() throws InvalidIPAddressException {
		if (this.configIP == null) {
			throw new InvalidIPAddressException("You must set a valid IP address for this network");
		}

		return this.configIP;
	}

	/**
	 * Should we do ad blocking at the router?
	 */
	public final Optional<Boolean> doAdBlocking() {
		return Optional.ofNullable(this.adBlocking);
	}

	/**
	 * Do we require users to be on a VPN connection to use our services?
	 * (This is only useful for internal services...)
	 */
	public final Optional<Boolean> isVPNOnly() {
		return Optional.ofNullable(this.vpnOnly);
	}

	/**
	 * @return the domain which applies to this network
	 */
	public final Optional<String> getDomain() {
		return Optional.ofNullable(this.domain);
	}

	public Optional<Map<MachineType, IPAddress>> getSubnets() {
		return Optional.ofNullable(this.subnets);
	}

	public Optional<IPAddress> getSubnet(MachineType subnet) {
		return Optional.ofNullable(this.subnets.get(subnet));
	}

	public Optional<String> getProperty(String label, String property) {
		return Optional.ofNullable(getMachineData(label).getData().getString(property, null));
	}

	public Optional<JsonObject> getProperties(String machine, String properties) {
		return Optional.ofNullable(getMachineData(machine).getData().getJsonObject(properties));
	}

	public Map<String, AMachineData> getMachines() {
		assertNotNull(this.machines);
		
		return this.machines;
	}
	
	/**
	 * Gets all machines which have a given Type declared in their Data
	 * @param type The type of machines to get
	 * @return Optionally a Map of machines, indexed by their label
	 */
	public Optional<Map<String, AMachineData>> getMachines(MachineType type) {
		Map<String, AMachineData> machines = getMachines().entrySet()
				.stream()
				.filter(Objects::nonNull)
				.filter(kvp -> kvp.getValue().isType(type))
				.collect(Collectors.toMap(kvp -> kvp.getKey(), kvp -> kvp.getValue()));
	
		return Optional.ofNullable(machines);
	}

	public Map<String, UserData> getUsers() {
		return this.users;
	}

}
