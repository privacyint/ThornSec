package core.data;

import javax.json.JsonObject;

class UserDeviceData extends ADeviceData {

	UserDeviceData(String label) {
		super(label);
	}

	public void read(JsonObject data) {
		super.setData(data);
		
		super.setFullName(getProperty("fullname", "Dr McNuggets"));
		super.setSSHKey(getProperty("sshkey", null));
		super.setDefaultPassword(getProperty("defaultpw", "secret"));
		
		super.setMacs(getPropertyArray("macs"));
		super.setIsThrottled(Boolean.parseBoolean(getProperty("throttle", "true")));
		super.setIsManaged(Boolean.parseBoolean(getProperty("managed", "false")));
		super.setPorts(getPropertyArray("ports"));
		
		super.setFirstOctet(10);;
		super.setSecondOctet(getIsManaged() ? 151 : 150);
	}}
