package core.data;

import javax.json.JsonObject;

/**
 * The Class InternalDeviceData
 * Represents an internal-only device on our network.
 */
class InternalDeviceData extends ADeviceData {

	/**
	 * Instantiates a new internal-only device's data.
	 *
	 * @param label the device label
	 */
	InternalDeviceData(String label) {
		super(label);
	}

	/**
	 * Reads in and populates this device's data
	 */
	@Override
	public void read(JsonObject data) {
		super.setData(data);
		
		super.setMacs(super.getPropertyArray("macs"));
		super.setIsThrottled(Boolean.parseBoolean(super.getProperty("throttle", "true")));
		super.setIsManaged(Boolean.parseBoolean(super.getProperty("managed", "false")));
		super.setListenPorts(getProperty("ports", null));
		super.setCnames(super.getPropertyArray("cnames"));

		super.setFirstOctet(10);
		super.setSecondOctet(super.getIsManaged() ? 151 : 150);
		
		super.setEmailAddress(getProperty("email", getLabel() + "@" + getDomain()));
	}
}
