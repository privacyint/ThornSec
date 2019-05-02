package core.data;

import java.net.Inet4Address;
import java.net.URL;
import java.net.UnknownHostException;

import java.util.HashSet;
import java.util.Set;

import javax.json.JsonArray;
import javax.json.JsonObject;

import javax.swing.JOptionPane;

/**
 * Abstract class for something representing "Data" on our network 
 */
public abstract class AData {

	private final String label;
	
	private JsonObject data;

	/**
	 * Instantiates a new data object.
	 *
	 * @param label the label for this object
	 */
	protected AData(String label) {
		this.label = label;
	}

	/**
	 * Abstract JSON read method - must be overridden by descendants
	 *
	 * @param data the JSON data
	 */
	public abstract void read(JsonObject data);

	/**
	 * Gets the object label.
	 *
	 * @return the object label
	 */
	public final String getLabel() {
		return this.label;
	}
	
	/**
	 * Gets the object's data.
	 *
	 * @return the data
	 */
	public final JsonObject getData() {
		return this.data;
	}
	
	/**
	 * Sets the object's data.
	 *
	 * @param data the new data
	 */
	protected final void setData(JsonObject data) {
		this.data = data;
	}

	/**
	 * Parses an arbitrary list of Integers from a string representation.
	 *
	 * @param toParse the string of Integers to parse, with any non-numeric
	 *                character used as a delimiter
	 * @return Integers
	 */
	protected final Set<Integer> parseIntList(String toParse) {
		Set<Integer> integers = new HashSet<Integer>();
		
		if (toParse != null && !toParse.isEmpty()) {
			//Don't really care what delimiters people use, tbh
			String[] intStrings = toParse.trim().split("[^0-9]");
			
			for (String intString : intStrings) {
				Integer parsed = parseInt(intString);
				
				if (parsed != null) {
					integers.add(parsed);
				}
				else {
					JOptionPane.showMessageDialog(null, "I've tried to parse a non-integer.\n\nThere's a problem with " + toParse + "\n\nPlease fix this in your JSON");
					System.exit(1);
				}
			}
		}
		
		return integers;
	}
	
	/**
	 * Parses a String representation of a single Integer.
	 *
	 * @param toParse the Integer to parse
	 * @return the Integer
	 */
	protected final Integer parseInt(String toParse) {
		Integer dave;
		
		try {
			dave = Integer.parseInt(toParse);
		}
		catch (NumberFormatException e) {
			dave = null; //This is OK, we check for null values elsewhere
		}
		
		return dave;
	}
	
	/**
	 * String to Inet4Address.
	 *
	 * @param toParse the string (IP/Hostname) to parse
	 * @return an Inet4Address
	 */
	protected final Inet4Address stringToIP(String toParse) {
		Object ip = null;
		
		//If we don't check this, null == 127.0.0.1, which throws everything :)
		if (toParse == null || toParse.isEmpty()) { return null; }
		
		try {
			ip = Inet4Address.getByName(toParse);
		}
		catch (UnknownHostException e) {
			JOptionPane.showMessageDialog(null, toParse + " appears to be an invalid address, or you're currently offline. Please check your network connection and try again.");
			System.exit(1);
		}
		
		return (Inet4Address) ip;
	}
	
	/**
	 * Gets an arbitrary property from the object's data.
	 * 
	 * You should avoid using this method directly where possible, but
	 * we keep it public in case a profile wishes to use it.
	 *
	 * @param property the property to read
	 * @param defaultVal the default value
	 * @return the property's value
	 */
	public final String getProperty(String property, String defaultVal) {
		return getData().getString(property, defaultVal);
	}
	
	/**
	 * Gets an arbitrary array of properties from the object's data.
	 *
	 * @param property the property array to read
	 * @return the property values array, or empty array if unset
	 */
	public final String[] getPropertyArray(String property) {
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
	
	/**
	 * Gets the property's object array.
	 *
	 * @param property the property
	 * @return the property object array
	 */
	public final JsonArray getPropertyObjectArray(String property) {
		return getData().getJsonArray(property);
	}
	
	
	/**
	 * Checks if a string is a valid URI.
	 *
	 * @param uriToCheck the uri to check
	 * @return True if valid, False otherwise
	 */
	public final Boolean isValidURI(String uriToCheck) {
        try { 
            new URL(uriToCheck).toURI(); 
        } 
        catch (Exception e) { 
            return false; 
        } 

        return true; 
	}
	
	/**
	 * Checks if a string is a valid IP.
	 *
	 * @param ipToCheck the ip to check
	 * @return True if valid, False otherwise
	 */
	public final Boolean isValidIP(String ipToCheck){
		return stringToIP(ipToCheck) != null; 
	}
}
