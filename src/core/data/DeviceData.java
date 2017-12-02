package core.data;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class DeviceData extends AData {

	private JsonObject data;
	private String[]   macs;
	private String     type;
	private Boolean    throttled;
	private Boolean    managed;
	
	public DeviceData(String label) {
		super(label);
	}

	public void read(JsonObject data) {
		this.data = data;
		
		type = data.getString("type", null);

		JsonArray jsonMacs = data.getJsonArray("macs");
		if (jsonMacs != null) {
			macs = new String[jsonMacs.size()];
			for (int i = 0; i < macs.length; ++i) {
				macs[i] = jsonMacs.getString(i);
			}
		}
		else {
			macs = new String[0];
		}
		
		throttled = data.getString("throttle", "true").equals("true");
		managed   = data.getString("managed", "false").equals("true");
	}

	public String getProperty(String property) {
		return data.getString(property, null);
	}
	
	public String[] getMacs() {
		return this.macs;
	}

	public String getType() {
		return this.type;
	}
	
	public Boolean getThrottled() {
		return this.throttled;
	}

	public Boolean getManaged() {
		return this.managed;
	}
	
}
