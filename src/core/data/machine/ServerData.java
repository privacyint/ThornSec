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

import java.util.Set;

import javax.json.JsonObject;
import javax.json.stream.JsonParsingException;

import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;

import core.exception.data.ADataException;

/**
 * This class represents a "Server" on our network - that is,
 * a computer which is providing a function on your network.
 * 
 *  I'd not be expecting you to instantiate this directly,
 *  (although you may, of course!) rather one of its descendants.
 */
public class ServerData extends AMachineData {

	public enum SSHConnection { DIRECT, TUNNELLED; }
	public enum WANConnection { PPP, DHCP, STATIC; }
	
	private Set<String> types;
	private Set<String> profiles;

	private Set<String>    adminUsernames;
	private Set<IPAddress> remoteAdminIPAddresses;

	private Integer adminSSHConnectPort;
	private Integer sshListenPort;

	private Boolean update;

	private SSHConnection sshConnection;
	private WANConnection wanConnection;

	private HostName debianMirror;
	private String   debianDirectory;

	private String keePassDB;
	
	public ServerData(String label) {
		super(label);
		
		this.adminUsernames         = null;
		this.remoteAdminIPAddresses = null;
		this.types                  = null;
		this.profiles               = null;
		
		this.adminSSHConnectPort = null;
		this.sshListenPort       = null;
				
		this.update = null;
		
		this.sshConnection = null;
		this.wanConnection    = null;
		
		this.debianDirectory = null;
		this.debianMirror    = null;
		
		this.keePassDB = null;
	}

	public void read(JsonObject data)
	throws ADataException, JsonParsingException, IOException, URISyntaxException {
		super.read(data);
		
		this.adminUsernames = getPropertyArray("admins");
		Set<String> sources = getPropertyArray("sshsource");
		if (sources != null) { //There must be a way to make this better
			for (String source : sources) {
				this.remoteAdminIPAddresses.add(new IPAddressString(source).getAddress());
			}
		}
		this.types           = getPropertyArray("types");
		this.profiles        = getPropertyArray("profiles");

		this.adminSSHConnectPort = getIntegerProperty("adminport");
		this.sshListenPort       = getIntegerProperty("sshport");

		this.update = Boolean.valueOf(super.getStringProperty("update", null));

		this.sshConnection   = SSHConnection.valueOf(getStringProperty("connection"));
		this.wanConnection   = WANConnection.valueOf(getStringProperty("extconnection"));

		this.debianMirror    = new HostName(getStringProperty("debianmirror"));
		this.debianDirectory = getStringProperty("debiandirectory");

		this.keePassDB = getStringProperty("keepassdb");
	}

	public final Set<String> getAdminUsernames() {
		return adminUsernames;
	}

	public final Set<IPAddress> getRemoteAdminIPAddresses() {
		return remoteAdminIPAddresses;
	}

	public final Integer getAdminSSHConnectPort() {
		return adminSSHConnectPort;
	}

	public final Integer getSshListenPort() {
		return sshListenPort;
	}

	public final SSHConnection getSshConnection() {
		return sshConnection;
	}

	public final WANConnection getWanConnection() {
		return wanConnection;
	}

	public final HostName getDebianMirror() {
		return debianMirror;
	}

	public final String getDebianDirectory() {
		return debianDirectory;
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
