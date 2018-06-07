package core.data;

import javax.json.JsonObject;

public class InternalDeviceData extends ADeviceData {

	public InternalDeviceData(String label) {
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
