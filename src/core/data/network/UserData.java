/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.data.network;

import java.util.Optional;
import javax.json.JsonObject;
import core.data.AData;

/**
 * This class represents a User.
 * 
 * Users have several properties
 */
public class UserData extends AData {

	private String username;
	private String fullName;
	private String sshKey;
	private String homeDir;
	private String defaultPassword;
	private String wireguardKey;
	

	/**
	 * Create a new UserData populated with null values
	 * @param label
	 */
	public UserData(String label) {
		super(label);

		this.username = null;
		this.fullName = null;
		this.sshKey = null;
		this.homeDir = null;
		this.defaultPassword = null;
		this.wireguardKey = null;
	}

	@Override
	public void read(JsonObject data) {
		this.username = data.getString("username", null);
		this.fullName = data.getString("fullname", "Dr McNuggets");
		this.sshKey = data.getString("ssh", null);
		this.homeDir = data.getString("home_dir", null);
		this.defaultPassword = data.getString("defaultpw", null);
		this.wireguardKey = data.getString("wireguard", null);
	}

	/**
	 * Get a User's full name, if set
	 * @return 
	 */
	public final Optional<String> getFullName() {
		return Optional.ofNullable(this.fullName);
	}

	/**
	 * Get a User's home directory, if set
	 */
	public final Optional<String> getHomeDirectory() {
		return Optional.ofNullable(this.homeDir);
	}
	
	/**
	 * Get a User's WireGuard public key, if set
	 * @return
	 */
	public final Optional<String> getWireGuardKey() {
		return Optional.ofNullable(this.wireguardKey);
	}

	/**
	 * Get a User's SSH public key, if set
	 * @return
	 */
	public final Optional<String> getSSHKey() {
		return Optional.ofNullable(this.sshKey);
	}

	public final Optional<String> getDefaultPassphrase() {
		return Optional.ofNullable(this.defaultPassword);
	}

	public Optional<String> getUsername() {
		return Optional.ofNullable(this.username);
	}
}
