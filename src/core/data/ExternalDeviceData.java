package core.data;

import javax.json.JsonObject;

/**
 * The Class ExternalDeviceData
 * Represents an external-only device on our network.
 */
class ExternalDeviceData extends ADeviceData {

	/**
	 * Instantiates a new external-only device's data.
	 *
	 * @param label the device label
	 */
	ExternalDeviceData(String label) {
		super(label);
	}

	/**
	 * Reads in and populates this device's data
	 */
	@Override
	public void read(JsonObject data) {
		super.setData(data);
		
		super.setMacs(super.getPropertyArray("macs"));
		super.setListenPorts(getProperty("ports", null));

		super.setIsThrottled(Boolean.parseBoolean(getProperty("throttle", "true")));
		super.setIsManaged(Boolean.parseBoolean(getProperty("managed", "false")));
		super.setCnames(super.getPropertyArray("cnames"));

		super.setFirstOctet(10);
		super.setSecondOctet(super.getIsManaged() ? 101 : 100);
		
		super.setEmailAddress(getProperty("email", getLabel() + "@" + getDomain()));
	}
}
