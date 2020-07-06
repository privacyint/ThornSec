/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.data.machine;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.stream.JsonParsingException;

import core.StringUtils;
import core.data.machine.configuration.NetworkInterfaceData;
import core.data.machine.configuration.NetworkInterfaceData.Direction;
import core.exception.data.ADataException;
import core.exception.data.InvalidPortException;
import core.exception.data.InvalidPropertyException;
import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;

/**
 * This class represents a "Server" on our network - that is, a computer which
 * is providing a function on your network.
 *
 * I'd not be expecting you to instantiate this directly, (although you may, of
 * course!) rather one of its descendants.
 */
public class ServerData extends AMachineData {

	public enum SSHConnection {
		DIRECT, TUNNELLED
	}

	private Set<HostName> sshSources;
	private Set<String> profiles;
	private Set<String> adminUsernames;
	private Set<IPAddress> remoteAdminIPAddresses;

	private Integer adminSSHConnectPort;
	private Integer sshListenPort;

	private Boolean update;

	private SSHConnection sshConnection;

	private URL debianMirror;
	private String debianDirectory;

	private String keePassDB;

	private Integer ram;
	private Integer cpus;

	public ServerData(String label) {
		super(label);

		this.sshSources = null;
		this.profiles = null;

		this.putType(MachineType.SERVER);

		this.adminUsernames = null;
		this.remoteAdminIPAddresses = null;

		this.adminSSHConnectPort = null;
		this.sshListenPort = null;

		this.update = null;

		this.sshConnection = null;

		this.debianDirectory = null;
		this.debianMirror = null;

		this.keePassDB = null;

		this.ram = null;
		this.cpus = null;
	}

	private NetworkInterfaceData readNIC(Direction dir, JsonObject nic) throws JsonParsingException, ADataException, IOException {
		NetworkInterfaceData iface = new NetworkInterfaceData(getLabel());
		iface.read(nic);
		
		NetworkInterfaceData existingIface = getNetworkInterface(Direction.WAN, iface.getIface());
		if (existingIface != null) {
			iface = existingIface;
			iface.read(nic);
		}
		
		return iface;
	}
	
	
	private void readNICs(JsonObject data) throws JsonParsingException, ADataException, IOException {
		final JsonObject networkInterfaces = data.getJsonObject("network_interfaces");

		if (networkInterfaces.containsKey("wan")) {
			final JsonArray wanIfaces = networkInterfaces.getJsonArray("wan");
			for (int i = 0; i < wanIfaces.size(); ++i) {
				NetworkInterfaceData nic = readNIC(Direction.WAN, wanIfaces.getJsonObject(i));
				putWANNetworkInterface(nic);
			}
		}

		if (networkInterfaces.containsKey("lan")) {
			final JsonArray lanIfaces = networkInterfaces.getJsonArray("lan");
			for (int i = 0; i < lanIfaces.size(); ++i) {
				NetworkInterfaceData nic = readNIC(Direction.LAN, lanIfaces.getJsonObject(i));
				putLANNetworkInterface(nic);
			}
		}
	}
	
	@Override
	public void read(JsonObject data) throws ADataException {
		super.read(data);

		// Build network interfaces
		if (data.containsKey("network_interfaces")) {
			try {
				readNICs(data);
			} catch (JsonParsingException | ADataException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if (data.containsKey("admins")) {
			final JsonArray admins = data.getJsonArray("admins");
			for (final JsonValue admin : admins) {
				putAdmin(((JsonString) admin).getString());
			}
		}
		if (data.containsKey("ssh_sources")) {
			final JsonArray sources = data.getJsonArray("ssh_sources");
			for (final JsonValue source : sources) {
				putSSHSource(new HostName(((JsonString) source).getString()));
			}
		}
		if (data.containsKey("types")) {
			final JsonArray types = data.getJsonArray("types");
			for (final JsonValue type : types) {
				putType(((JsonString) type).getString());
			}
		}
		if (data.containsKey("profiles")) {
			final JsonArray profiles = data.getJsonArray("profiles");
			for (final JsonValue profile : profiles) {
				putProfile(((JsonString) profile).getString());
			}
		}
		if (data.containsKey("ssh_connect_port")) {
			setAdminPort(data.getInt("ssh_connect_port"));
		}
		if (data.containsKey("sshd_listen_port")) {
			setSSHListenPort(data.getInt("sshd_listen_port"));
		}
		if (data.containsKey("update")) {
			this.update = data.getBoolean("update");
		}
		if (data.containsKey("ssh_connection")) {
			this.sshConnection = SSHConnection.valueOf(data.getString("ssh_connection").toUpperCase());
		}
		if (data.containsKey("debian_mirror")) {
			try {
				setDebianMirror(new URL(data.getString("debian_mirror")));
			} catch (MalformedURLException e) {
				throw new InvalidPropertyException(data.getString("debian_mirror")
						+ " is not a valid debian mirror URL");
			}
		}
		if (data.containsKey("debian_directory")) {
			setDebianDirectory(data.getString("debian_directory"));
		}
		if (data.containsKey("keepassdb")) {
			setKeePassDB(data.getString("keepassdb"));
		}
		if (data.containsKey("cpus")) {
			setCPUs(data.getInt("cpus"));
		}
		if (data.containsKey("ram")) {
			setRAM(data.getString("ram"));
		}
	}

	private void setCPUs(Integer cpus) throws InvalidPropertyException {
		if (cpus < 1) {
			throw new InvalidPropertyException("You cannot have a machine with fewer than 1 CPUs!");
		}

		this.cpus = cpus;
	}

	private void setRAM(String ram) throws InvalidPropertyException {
		final Integer ramAsMB = StringUtils.stringToMegaBytes(ram);

		if (ramAsMB < 512) {
			throw new InvalidPropertyException("You cannot have a machine with less than 512mb of RAM");
		}

		this.ram = ramAsMB;
	}

	private void setKeePassDB(String keePassDB) {
		this.keePassDB = keePassDB;
	}

	private void setDebianDirectory(String dir) {
		this.debianDirectory = dir;
	}

	private void setDebianMirror(URL url) {
		this.debianMirror = url;
	}

	private void setSSHListenPort(Integer port) throws InvalidPortException {
		if ((port < 0) || (port > 65535)) {
			throw new InvalidPortException(port);
		}

		this.sshListenPort = port;
	}

	private void setAdminPort(Integer port) throws InvalidPortException {
		if ((port < 0) || (port > 65535)) {
			throw new InvalidPortException(port);
		}

		this.adminSSHConnectPort = port;
	}

	private void putProfile(String... profiles) {
		if (this.profiles == null) {
			this.profiles = new LinkedHashSet<>();
		}

		for (final String profile : profiles) {
			if (!this.profiles.contains(profile)) {
				this.profiles.add(profile);
			}
		}
	}

	private void putSSHSource(HostName... sources) {
		if (this.sshSources == null) {
			this.sshSources = new LinkedHashSet<>();
		}

		for (final HostName source : sources) {
			this.sshSources.add(source);
		}
	}

	private void putAdmin(String... admins) throws InvalidPropertyException {
		if (this.adminUsernames == null) {
			this.adminUsernames = new LinkedHashSet<>();
		}

		for (final String admin : admins) {
			if (!this.adminUsernames.contains(admin)) {
				this.adminUsernames.add(admin);
			}
			else {
				throw new InvalidPropertyException(admin + " is duplicated");
			}
		}
	}

	public final Optional<Collection<String>> getAdminUsernames() {
		return Optional.ofNullable(this.adminUsernames);
	}

	public final Optional<Collection<IPAddress>> getRemoteAdminIPAddresses() {
		return Optional.ofNullable(this.remoteAdminIPAddresses);
	}

	public final Optional<Integer> getAdminSSHConnectPort() {
		return Optional.ofNullable(this.adminSSHConnectPort);
	}

	public final Optional<Integer> getSshListenPort() {
		return Optional.ofNullable(this.sshListenPort);
	}

	public final Optional<SSHConnection> getSshConnection() {
		return Optional.ofNullable(this.sshConnection);
	}

	@Deprecated //TODO: Move to Debian
	public final Optional<URL> getDebianMirror() {
		return Optional.ofNullable(this.debianMirror);
	}

	@Deprecated //TODO: Move to Debian
	public final Optional<String> getDebianDirectory() {
		return Optional.ofNullable(this.debianDirectory);
	}

	public final Optional<Collection<String>> getAdmins() {
		return Optional.ofNullable(this.adminUsernames);
	}

	public final Optional<SSHConnection> getConnection() {
		return Optional.ofNullable(this.sshConnection);
	}

	public final Optional<Integer> getAdminPort() {
		return Optional.ofNullable(this.adminSSHConnectPort);
	}

	public final Optional<Integer> getSSHPort() {
		return Optional.ofNullable(this.sshListenPort);
	}

	public final Optional<Boolean> getUpdate() {
		return Optional.ofNullable(this.update);
	}

	public final Set<String> getProfiles() {
		return this.profiles;
	}

	public final Optional<Collection<IPAddress>> getSSHSources() {
		return Optional.ofNullable(this.remoteAdminIPAddresses);
	}

	public final Optional<String> getKeePassDB() {
		return Optional.ofNullable(this.keePassDB);
	}

	/**
	 * @return the ram in megabytes
	 */
	public final Optional<Integer> getRAM() {
		return Optional.ofNullable(this.ram);
	}

	/**
	 * @return the number of CPUs assigned to this service
	 */
	public final Optional<Integer> getCPUs() {
		return Optional.ofNullable(this.cpus);
	}
}
