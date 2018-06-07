package core.data;

import javax.json.JsonObject;

public class UserDeviceData extends ADeviceData {
	private String fullname;
	private String sshKey;
	
	public UserDeviceData(String label) {
		super(label);
	}

	public void read(JsonObject data) {
		this.data = data;
		
		fullname  = getProperty("fullname", "Dr McNuggets");
		sshKey    = getProperty("sshkey", null);
		
		super.macs      = getPropertyArray("macs");
		super.throttled = getProperty("throttle", "true").equals("true");
		super.ports     = getPropertyArray("ports");
	}

	public String getFullName() {
		return this.fullname;
	}
	
	public String getSSHKey() {
		return this.sshKey;
	}
	
}
