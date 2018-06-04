package core.data;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class DeviceData extends AData {

	private JsonObject data;
	private String[]   macs;
	private String[]   ports;
	private String     type;
	private Boolean    throttled;
	private Boolean    managed;
	
	public DeviceData(String label) {
		super(label);
	}

	public void read(JsonObject data) {
		this.data = data;
		
		type      = getProperty("type", null);
		macs      = getPropertyArray("macs");
		throttled = getProperty("throttle", "true").equals("true");
		managed   = getProperty("managed", "false").equals("true");
		ports     = getPropertyArray("ports");
	}

	public String[] getPropertyArray(String property) {
		JsonArray jsonProperties = getPropertyObjectArray(property);

		if (jsonProperties != null) {
			String[] properties = new String[jsonProperties.size()];
			for (int i = 0; i < properties.length; ++i) {
				properties[i] = jsonProperties.getString(i);
			}
			
			return properties;
		}
		else {
			return new String[0];
		}
	}
	
	public JsonArray getPropertyObjectArray(String property) {
		return data.getJsonArray(property);
	}
	
	public String getProperty(String property, String defaultVal) {
		return data.getString(property, defaultVal);
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
	
	public String[] getPorts() {
		return this.ports;
	}
	
}
