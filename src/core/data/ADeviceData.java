package core.data;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

public abstract class ADeviceData {

	private String label;
	
	protected JsonObject data;
	protected String[]   macs;
	protected String[]   ports;
	protected Boolean    throttled;
	protected Boolean    managed;

	public ADeviceData(String label) {
		this.label = label;
	}

	public String getLabel() {
		return this.label;
	}
	
	public void read(String data) {
		JsonReader reader = Json.createReader(new StringReader(data));
		this.read(reader.readObject());
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

	public Boolean getThrottled() {
		return this.throttled;
	}

	public Boolean getManaged() {
		return this.managed;
	}
	
	public String[] getPorts() {
		return this.ports;
	}
	
	public abstract void read(JsonObject data);

	public String getSSHKey() {
		return null;
	}
	
	public String getFullName() {
		return null;
	}

}
