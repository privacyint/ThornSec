package core.data;

import javax.json.JsonObject;

public class DeviceData extends AData {

	private JsonObject data;
	private String wiredMac;
	private String wirelessMac;
	private String type;
	
	public DeviceData(String label) {
		super(label);
	}

	public void read(JsonObject data) {
		this.data = data;
		
		wiredMac = data.getString("wired", null);
		wirelessMac = data.getString("wireless", null);
		type = data.getString("type", null);
	}

	public String getProperty(String property) {
		return data.getString(property, null);
	}
	
	public String getWired() {
		return this.wiredMac;
	}

	public String getWireless() {
		return this.wirelessMac;
	}
	
	public String getType() {
		return this.type;
	}
	
}
