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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
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
import core.data.machine.ServerData.WANConnection;
import core.data.machine.ServiceData;
import core.data.machine.UserDeviceData;
import core.data.machine.configuration.NetworkInterfaceData;
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
 * If something isn't defined in our JSON, we will return default values.
 *
 * This is our "interface" between the data and the models ThornSec will build.
 */
public class NetworkData extends AData {
	private static final Boolean DEFAULT_DTLS = true;
	private static final Boolean DEFAULT_ADBLOCKING = false;
	private static final Boolean DEFAULT_AUTOGENPASSWDS = false;
	private static final Boolean DEFAULT_VPNONLY = false;
	private static final Boolean DEFAULT_AUTOGUEST = false;
	private static final Boolean DEFAULT_UPDATE = true;
	private static final Boolean DEFAULT_DEVICE_IS_MANAGED = true;
	private static final Boolean DEFAULT_MACHINE_IS_THROTTLED = true;

	private static final String DEFAULT_THORNSECBASE = "/srv/ThornSec";
	private static final String DEFAULT_DEBIAN_ISO_DIR = "https://gensho.ftp.acc.umu.se/debian-cd/current/amd64/iso-cd/";
	private static final String DEFAULT_DEBIANMIRROR = "free.hands.com";
	private static final String DEFAULT_DEBIANDIR = "/debian";
	private static final String DEFAULT_NETMASK = "/30";
	private static final String DEFAULT_KEEPASS_DB = "ThornSec.kdbx";
	private static final String DEFAULT_DOMAIN = "lan";

	private static final Integer DEFAULT_RAM = 2048;
	private static final Integer DEFAULT_CPUS = 1;

	private static final Integer DEFAULT_SSH_PORT = 65422;
	private static final Integer DEFAULT_ADMIN_PORT = 65422;

	private String myUser;
	private IPAddress configIP;
	private String domain;

	private Boolean adBlocking;
	private Boolean autoGenPassphrases;

	private Boolean vpnOnly;
	private Boolean dtls;
	private Boolean autoGuest;

	private Set<IPAddress> upstreamDNS;

	private final ServiceData defaultServiceData;
	private final HypervisorData defaultHypervisorData;

	private Map<MachineType, Map<String, AMachineData>> machines;

	public NetworkData(String label) {
		super(label);

		this.myUser = null;
		this.domain = null;

		this.configIP = null;

		this.adBlocking = null;
		this.autoGenPassphrases = null;
		this.vpnOnly = null;
		this.dtls = null;
		this.autoGuest = null;

		this.upstreamDNS = null;

		this.defaultServiceData = new ServiceData("");
		this.defaultHypervisorData = new HypervisorData("");

		this.machines = null;
	}

	@Override
	public void read(JsonObject data) throws ADataException, JsonParsingException, IOException, URISyntaxException {
		super.setData(data);

		final String include = data.getString("include", null);

		if (include != null) {
			readInclude(include);
		} else {
			this.defaultServiceData.read(data);

			if (data.containsKey("dns")) {
				this.upstreamDNS = getIPAddressArray("dns");
			}

			if (data.containsKey("configip")) {
				this.configIP = new IPAddressString(data.getString("configip")).getAddress();
			}

			this.myUser = data.getString("myuser", null);
			this.domain = data.getString("domain", DEFAULT_DOMAIN);

			this.dtls = data.getBoolean("dtls", DEFAULT_DTLS);
			this.adBlocking = data.getBoolean("adblocking", DEFAULT_ADBLOCKING);
			this.autoGenPassphrases = data.getBoolean("autogenpasswds", DEFAULT_AUTOGENPASSWDS);
			this.vpnOnly = data.getBoolean("vpnonly", DEFAULT_VPNONLY);
			this.autoGuest = data.getBoolean("autoguest", DEFAULT_AUTOGUEST);

			if (data.containsKey("servers")) {
				final JsonObject jsonServers = data.getJsonObject("servers");

				for (final String jsonServer : jsonServers.keySet()) {
					final ServerData server = new ServerData(jsonServer);
					server.read(jsonServers.getJsonObject(jsonServer));

					for (final MachineType type : server.getTypes()) {
						putMachine(type, server);
					}
				}
			}

			if (data.containsKey("internaldevices")) {
				final JsonObject jsonDevices = data.getJsonObject("internaldevices");

				for (final String jsonDevice : jsonDevices.keySet()) {
					final InternalDeviceData device = new InternalDeviceData(jsonDevice);
					device.read(jsonDevices.getJsonObject(jsonDevice));

					putMachine(MachineType.DEVICE, device);
					putMachine(MachineType.INTERNAL_ONLY, device);
				}
			}

			if (data.containsKey("externaldevices")) {
				final JsonObject jsonDevices = data.getJsonObject("externaldevices");

				for (final String jsonDevice : jsonDevices.keySet()) {
					final ExternalDeviceData device = new ExternalDeviceData(jsonDevice);
					device.read(jsonDevices.getJsonObject(jsonDevice));

					putMachine(MachineType.DEVICE, device);
					putMachine(MachineType.EXTERNAL_ONLY, device);
				}
			}

			if (data.containsKey("users")) {
				final JsonObject jsonDevices = data.getJsonObject("users");

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
		final AMachineData machine = null;

		for (final MachineType type : getMachines().keySet()) {
			getMachine(type, label);
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

	private Set<IPAddress> getIPAddressArray(String key) throws UnknownHostException {
		Set<IPAddress> addresses = null;

		if (getData().containsKey(key)) {
			addresses = new HashSet<>();
			final JsonArray jsonIPAddresses = getData().getJsonArray(key);

			for (final JsonValue ip : jsonIPAddresses) {
				addresses.add(new IPAddressString(ip.toString()).getAddress());
			}
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

	// Network only data
	public final String getUser() throws NoValidUsersException {
		if (this.myUser == null) {
			throw new NoValidUsersException();
		}

		return this.myUser;
	}

	public final Set<IPAddress> getUpstreamDNSServers() {
		return this.upstreamDNS;
	}

	public final Boolean upstreamDNSIsTLS() {
		return this.dtls;
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
		return new IPAddressString(DEFAULT_NETMASK).getAddress();
	}

	/**
	 * This is either the IP of our router (if we're inside) or the public IP
	 * address (if it's an external resource)
	 */
	public final IPAddress getIP() throws InvalidIPAddressException {
		if (this.configIP == null) {
			throw new InvalidIPAddressException();
		}

		return this.configIP;
	}

	/**
	 * Should we do ad blocking at the router?
	 */
	public final Boolean adBlocking() {
		return this.adBlocking;
	}

	public final boolean vpnOnly() {
		return this.vpnOnly;
	}

	/**
	 * @return the domain which applies to this network
	 */
	public final String getDomain() {
		return this.domain;
	}

	final public Set<MachineType> getTypes(String server) throws InvalidServerException {
		Set<MachineType> types = ((ServerData) getMachine(MachineType.SERVER, server)).getTypes();

		if (types.isEmpty()) {
			types = this.defaultServiceData.getTypes();
		}

		return types;
	}

	public Set<String> getCNAMEs(String machine) throws InvalidMachineException {
		return getMachine(machine).getCNAMEs();
	}

	public Set<IPAddress> getExternalIPs(String machine) throws InvalidMachineException {
		return getMachine(machine).getExternalIPs();
	}

	final public Set<String> getAdmins(String server) throws InvalidServerException {
		Set<String> admins = ((ServerData) getMachine(MachineType.SERVER, server)).getAdmins();

		if (admins.isEmpty()) {
			admins = this.defaultServiceData.getAdmins();
		}

		return admins;
	}

	public Integer getAdminPort(String server) {
		Integer port = ((ServerData) getMachine(MachineType.SERVER, server)).getAdminPort();

		if (port == null) {
			port = this.defaultServiceData.getAdminPort();
			if (port == null) {
				port = NetworkData.DEFAULT_ADMIN_PORT;
			}
		}

		return port;
	}

	public Integer getSSHPort(String server) {
		Integer port = ((ServerData) getMachine(MachineType.SERVER, server)).getSSHPort();

		if (port == null) {
			port = this.defaultServiceData.getSSHPort();
			if (port == null) {
				port = NetworkData.DEFAULT_SSH_PORT;
			}
		}

		return port;
	}

	public Boolean getAutoUpdate(String server) {
		Boolean update = ((ServerData) getMachine(MachineType.SERVER, server)).getUpdate();

		if (update == null) {
			update = this.defaultServiceData.getUpdate();
			if (update == null) {
				update = NetworkData.DEFAULT_UPDATE;
			}
		}

		return update;
	}

	public String getKeePassDB(String server) throws URISyntaxException {
		String db = ((ServerData) getMachine(MachineType.SERVER, server)).getKeePassDB();

		if (db == null) {
			db = this.defaultServiceData.getKeePassDB();
			if (db == null) {
				db = NetworkData.DEFAULT_KEEPASS_DB;
			}
		}

		return db;
	}

	public Set<NetworkInterfaceData> getLanIfaces(String machine) throws InvalidMachineException {
		return getMachine(machine).getLanInterfaces();
	}

	public Set<NetworkInterfaceData> getWanIfaces(String machine) throws InvalidMachineException {
		return getMachine(machine).getWanInterfaces();
	}

	public Integer getRam(String service) throws InvalidServerException {
		Integer ram = ((ServiceData) getMachine(MachineType.SERVICE, service)).getRAM();

		if (ram == null) {
			ram = this.defaultServiceData.getRAM();
			if (ram == null) {
				ram = NetworkData.DEFAULT_RAM;
			}
		}

		return ram;
	}

	public Integer getCpus(String service) throws InvalidServerException {
		Integer cpus = ((ServiceData) getMachine(MachineType.SERVICE, service)).getCPUs();

		if (cpus == null) {
			cpus = this.defaultServiceData.getCPUs();
			if (cpus == null) {
				cpus = NetworkData.DEFAULT_CPUS;
			}
		}

		return cpus;
	}

	public WANConnection getWanConnection(String server) {
		WANConnection connection = ((ServerData) getMachine(MachineType.SERVER, server)).getWanConnection();

		if (connection == null) {
			connection = this.defaultServiceData.getWanConnection();
		}

		return connection;
	}

	public String getDebianIsoUrl(String service) throws InvalidServerException {
		String url = ((ServiceData) getMachine(MachineType.SERVICE, service)).getDebianIsoUrl();

		if (url == null) {
			url = this.defaultServiceData.getDebianIsoUrl();
			if (url == null) {
				try {
					url = NetworkData.DEFAULT_DEBIAN_ISO_DIR;

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
					final String url = NetworkData.DEFAULT_DEBIAN_ISO_DIR;

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
		File baseDir = ((HypervisorData) getMachine(MachineType.HYPERVISOR, hypervisor)).getVmBase();

		if (baseDir == null) {
			baseDir = this.defaultHypervisorData.getVmBase();
			if (baseDir == null) {
				baseDir = new File(NetworkData.DEFAULT_THORNSECBASE);
			}
		}

		return baseDir;
	}

	public HostName getDebianMirror(String server) throws URISyntaxException {
		HostName mirror = ((ServerData) getMachine(MachineType.SERVER, server)).getDebianMirror();

		if (mirror == null) {
			mirror = this.defaultServiceData.getDebianMirror();
			if (mirror == null) {
				mirror = new HostName(NetworkData.DEFAULT_DEBIANMIRROR);
			}
		}

		return mirror;
	}

	public String getDebianDirectory(String server) {
		String directory = ((ServerData) getMachine(MachineType.SERVER, server)).getDebianDirectory();

		if (directory == null) {
			directory = this.defaultServiceData.getDebianDirectory();
			if (directory == null) {
				directory = NetworkData.DEFAULT_DEBIANDIR;
			}
		}

		return directory;
	}

	public Boolean isManaged(String label) {
		Boolean managed = ((ADeviceData) getMachine(MachineType.DEVICE, label)).isManaged();

		if (managed == null) {
			managed = NetworkData.DEFAULT_DEVICE_IS_MANAGED;
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
				throttled = NetworkData.DEFAULT_MACHINE_IS_THROTTLED;
			}
		}

		return throttled;
	}

	public Map<Encapsulation, Set<Integer>> getListens(String label) {
		Map<Encapsulation, Set<Integer>> listens = getMachine(label).getListens();

		if (listens == null) {
			listens = this.defaultServiceData.getListens();
			if (listens == null) {
				return new Hashtable<>();
			}
		}
		return listens;
	}

	public Set<HostName> getIngresses(String label) {
		Set<HostName> ingresses = getMachine(label).getIngresses();

		if (ingresses == null) {
			ingresses = this.defaultServiceData.getIngresses();
			if (ingresses == null) {
				return new HashSet<>();
			}
		}
		return ingresses;
	}

	public Set<HostName> getEgresses(String label) {
		Set<HostName> egresses = getMachine(label).getEgresses();

		if (egresses == null) {
			egresses = this.defaultServiceData.getEgresses();
			if (egresses == null) {
				return new HashSet<>();
			}
		}
		return egresses;
	}

	public Set<String> getForwards(String label) {
		Set<String> forwards = getMachine(label).getForwards();

		if (forwards == null) {
			forwards = this.defaultServiceData.getForwards();
			if (forwards == null) {
				return new HashSet<>();
			}
		}
		return forwards;
	}

	public String getFirewallProfile(String label) {
		// TODO Auto-generated method stub
		return null;
	}

	public Set<String> getProfiles(String label) {
		Set<String> profiles = ((ServerData) getMachine(MachineType.SERVER, label)).getProfiles();

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
}
