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

import javax.json.JsonObject;
import javax.json.stream.JsonParsingException;

import core.exception.data.ADataException;

/**
 * This class represents a User's device.
 * Users can move (relatively) freely around our network. 
 */
public class UserDeviceData extends ADeviceData {

	private String fullname;
	private String sshKey;
	private String defaultPassword;
	
	public UserDeviceData(String label) {
		super(label);
		
		this.fullname        = null;
		this.sshKey          = null;
		this.defaultPassword = null;
	}

	public void read(JsonObject data)
	throws ADataException, JsonParsingException, IOException, URISyntaxException {
		super.read(data);
		
		this.fullname        = getStringProperty("fullname", "Dr McNuggets");
		
		this.sshKey          = getStringProperty("sshkey", null);
		this.defaultPassword = getStringProperty("defaultpw", null);
	}

	public final void setFullName(String fullname) {
		this.fullname = fullname;
	}
	
	public final String getFullName() {
		return this.fullname;
	}
	
	public final String getSSHKey() {
		return this.sshKey;
	}

	public final String getDefaultPassword() {
		return this.defaultPassword;
	}
}
