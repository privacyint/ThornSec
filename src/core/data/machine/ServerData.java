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

	public enum WANConnection {
		PPP, DHCP, STATIC
	}

	private Set<HostName> sshSources;
	private Set<String> profiles;
	private LinkedHashSet<String> types;

	private final Set<String> adminUsernames;
	private final Set<IPAddress> remoteAdminIPAddresses;

	private Integer adminSSHConnectPort;
	private Integer sshListenPort;

	private Boolean update;

	private SSHConnection sshConnection;
	private WANConnection wanConnection;

	private HostName debianMirror;
	private String debianDirectory;

	private String keePassDB;

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
		this.wanConnection = null;

		this.debianDirectory = null;
		this.debianMirror = null;

		this.keePassDB = null;
	}

	@Override
	public void read(JsonObject data) throws ADataException, JsonParsingException, IOException, URISyntaxException {
		super.read(data);

		if (data.containsKey("admins")) {
			final JsonArray admins = data.getJsonArray("admins");
			for (final JsonValue admin : admins) {
				putAdmin(admin.toString());
			}
		}
		if (data.containsKey("sshsources")) {
			final JsonArray sources = data.getJsonArray("sshsources");
			for (final JsonValue source : sources) {
				putSSHSource(new HostName(source.toString()));
			}
		}
		if (data.containsKey("types")) {
			final JsonArray types = data.getJsonArray("types");
			for (final JsonValue type : types) {
				putType(type.toString());
			}
		}
		if (data.containsKey("profiles")) {
			final JsonArray profiles = data.getJsonArray("profiles");
			for (final JsonValue profile : profiles) {
				putProfile(profile.toString());
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
			this.sshConnection = SSHConnection.valueOf(data.getString("sshconnection"));
		}
		if (data.containsKey("wanconnection")) {
			this.wanConnection = WANConnection.valueOf(data.getString("wanconnection"));
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

	private void putProfile(String profile) {
		if (this.profiles == null) {
			this.profiles = new LinkedHashSet<>();
		}

		this.profiles.add(profile);
	}

	private void putType(String type) {
		if (this.types == null) {
			this.types = new LinkedHashSet<>();
		}

		this.types.add(type);
	}

	private void putSSHSource(HostName source) {
		if (this.sshSources == null) {
			this.sshSources = new LinkedHashSet<>();
		}

		this.sshSources.add(source);

	}

	private void putAdmin(String string) {
		// TODO Auto-generated method stub

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

	public final WANConnection getWanConnection() {
		return this.wanConnection;
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

	public final Set<String> getTypes() {
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
}
