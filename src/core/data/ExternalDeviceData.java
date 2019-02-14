package core.data;

import javax.json.JsonObject;

class ExternalDeviceData extends ADeviceData {

	ExternalDeviceData(String label) {
		super(label);
	}

	public void read(JsonObject data) {
		super.setData(data);
		
		super.setMacs(super.getPropertyArray("macs"));
		super.setPorts(super.getPropertyArray("ports"));

		super.setIsThrottled(Boolean.parseBoolean(getProperty("throttle", "true")));
		super.setIsManaged(Boolean.parseBoolean(getProperty("managed", "false")));
		
		super.setFirstOctet(10);
		super.setSecondOctet(super.getIsManaged() ? 101 : 100);
	}
}
