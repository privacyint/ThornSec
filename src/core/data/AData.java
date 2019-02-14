package core.data;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.swing.JOptionPane;

public abstract class AData {

	private String     label;
	private JsonObject data;

	protected AData(String label) {
		this.label = label;
	}

	public abstract void read(JsonObject data);

	/*
	 * Getters
	 */
	public String getLabel() {
		return this.label;
	}
	
	public JsonObject getData() {
		return this.data;
	}
	
	/*
	 * Setters
	 */
	protected void setData(JsonObject data) {
		this.data = data;
	}

	/*
	 * Helpers
	 */
	protected Set<Integer> parseIntList(String toParse) {
		Set<Integer> integers = new HashSet<Integer>();
		
		//Don't really care what delimiters people use, tbh
		String[] intStrings = toParse.split("[^0-9]");
		
		for (String intString : intStrings) {
			Integer parsed = parseInt(intString);
			
			if (parsed != null) {
				integers.add(parsed);
			}
		}
		
		return integers;
	}
	
	protected Integer parseInt(String toParse) {
		Integer dave;
		
		try {
			dave = Integer.parseInt(toParse);
		}
		catch (NumberFormatException e) {
			dave = null; //This is OK, we check for null values elsewhere
		}
		
		return dave;
	}
	
	protected Inet4Address stringToIP(String toParse) {
		Object ip = null;
		
		//If we don't check this, null == 127.0.0.1, which throws everything :)
		if (toParse == null) { return null; }
		
		try {
			ip = Inet4Address.getByName(toParse);
		}
		catch (UnknownHostException e) {
			JOptionPane.showMessageDialog(null, toParse + " appears to be an invalid address, or you're currently offline. Please check your network connection and try again.");
			System.exit(1);
		}
		
		return (Inet4Address) ip;
	}
	
	public String getProperty(String property, String defaultVal) {
		return this.getData().getString(property, defaultVal);
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
		return getData().getJsonArray(property);
	}
}
