package core.data;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

public abstract class AData {

	private String label;

	public AData(String label) {
		this.label = label;
	}

	public String getLabel() {
		return this.label;
	}
	
	public void read(String data) {
		JsonReader reader = Json.createReader(new StringReader(data));
		this.read(reader.readObject());
	}
	
	public abstract void read(JsonObject data);

}
