/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.data.machine;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.stream.JsonParsingException;

import core.exception.data.ADataException;
import core.exception.data.InvalidPortException;
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
	private LinkedHashSet<MachineType> types;

	private Set<String> adminUsernames;
	private final Set<IPAddress> remoteAdminIPAddresses;

	private Integer adminSSHConnectPort;
	private Integer sshListenPort;

	private Boolean update;

	private SSHConnection sshConnection;

	private HostName debianMirror;
	private String debianDirectory;

	private String keePassDB;

	private final Integer ram;
	private final Integer cpus;

	public ServerData(String label) {
		super(label);

		this.sshSources = null;
		this.profiles = null;
		this.types = null;

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

	@Override
	public void read(JsonObject data) throws ADataException, JsonParsingException, IOException, URISyntaxException {
		super.read(data);

		if (data.containsKey("admins")) {
			final JsonArray admins = data.getJsonArray("admins");
			for (final JsonValue admin : admins) {
				putAdmin(((JsonString) admin).getString());
			}
		}
		if (data.containsKey("sshsources")) {
			final JsonArray sources = data.getJsonArray("sshsources");
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
		if (data.containsKey("adminport")) {
			setAdminPort(data.getInt("adminport"));
		}
		if (data.containsKey("sshport")) {
			setSSHListenPort(data.getInt("sshport"));
		}
		if (data.containsKey("update")) {
			this.update = data.getBoolean("update");
		}
		if (data.containsKey("sshconnection")) {
			this.sshConnection = SSHConnection.valueOf(data.getString("sshconnection").toUpperCase());
		}
		if (data.containsKey("debianmirror")) {
			setDebianMirror(new HostName(data.getString("debianmirror")));
		}
		if (data.containsKey("debiandirectory")) {
			setDebianDirectory(data.getString("debiandirectory"));
		}
		if (data.containsKey("keepassdb")) {
			setKeePassDB(data.getString("keepassdb"));
		}
	}

	private void setKeePassDB(String keePassDB) {
		this.keePassDB = keePassDB;
	}

	private void setDebianDirectory(String dir) {
		this.debianDirectory = dir;
	}

	private void setDebianMirror(HostName mirror) {
		this.debianMirror = mirror;
	}

	private void setSSHListenPort(Integer port) throws InvalidPortException {
		if ((port < 0) || (port > 65535)) {
			throw new InvalidPortException();
		}

		this.sshListenPort = port;
	}

	private void setAdminPort(Integer port) throws InvalidPortException {
		if ((port < 0) || (port > 65535)) {
			throw new InvalidPortException();
		}

		this.adminSSHConnectPort = port;
	}

	private void putProfile(String... profiles) {
		if (this.profiles == null) {
			this.profiles = new LinkedHashSet<>();
		}

		for (final String profile : profiles) {
			this.profiles.add(profile);
		}
	}

	private void putType(String... types) {
		if (this.types == null) {
			this.types = new LinkedHashSet<>();
		}

		for (String type : types) {
			type = type.replaceAll("[^a-zA-Z]", "").toUpperCase();
			this.types.add(MachineType.valueOf(type));
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

	private void putAdmin(String... admins) {
		if (this.adminUsernames == null) {
			this.adminUsernames = new LinkedHashSet<>();
		}

		for (final String admin : admins) {
			this.adminUsernames.add(admin);
		}
	}

	public final Set<String> getAdminUsernames() {
		return this.adminUsernames;
	}

	public final Set<IPAddress> getRemoteAdminIPAddresses() {
		return this.remoteAdminIPAddresses;
	}

	public final Integer getAdminSSHConnectPort() {
		return this.adminSSHConnectPort;
	}

	public final Integer getSshListenPort() {
		return this.sshListenPort;
	}

	public final SSHConnection getSshConnection() {
		return this.sshConnection;
	}

	public final HostName getDebianMirror() {
		return this.debianMirror;
	}

	public final String getDebianDirectory() {
		return this.debianDirectory;
	}

	public final Set<String> getAdmins() {
		return this.adminUsernames;
	}

	public final SSHConnection getConnection() {
		return this.sshConnection;
	}

	public final Integer getAdminPort() {
		return this.adminSSHConnectPort;
	}

	public final Integer getSSHPort() {
		return this.sshListenPort;
	}

	public final Boolean getUpdate() {
		return this.update;
	}

	public final Set<MachineType> getTypes() {
		return this.types;
	}

	public final Set<String> getProfiles() {
		return this.profiles;
	}

	public final Set<IPAddress> getSSHSources() {
		return this.remoteAdminIPAddresses;
	}

	public final String getKeePassDB() {
		return this.keePassDB;
	}

	/**
	 * @return the ram in megabytes
	 */
	public final Integer getRAM() {
		return this.ram;
	}

	/**
	 * @return the number of CPUs assigned to this service
	 */
	public final Integer getCPUs() {
		return this.cpus;
	}
}
