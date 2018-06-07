package core.data;

import javax.json.JsonObject;

public class ExternalDeviceData extends ADeviceData {

	public ExternalDeviceData(String label) {
		super(label);
	}

	public void read(JsonObject data) {
		super.data = data;
		
		super.macs      = getPropertyArray("macs");
		super.throttled = getProperty("throttle", "true").equals("true");
		super.managed   = getProperty("managed", "false").equals("true");
		super.ports     = getPropertyArray("ports");
	}
}
