package core.data;

import javax.json.JsonObject;

class InternalDeviceData extends ADeviceData {

	InternalDeviceData(String label) {
		super(label);
	}

	@Override
	public void read(JsonObject data) {
		super.setData(data);
		
		super.setMacs(super.getPropertyArray("macs"));
		super.setIsThrottled(Boolean.parseBoolean(super.getProperty("throttle", "true")));
		super.setIsManaged(Boolean.parseBoolean(super.getProperty("managed", "false")));
		super.setPorts(getProperty("ports", null));
		super.setCnames(super.getPropertyArray("cnames"));

		super.setFirstOctet(10);
		super.setSecondOctet(super.getIsManaged() ? 151 : 150);
	}
}
