package core.data;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.stream.JsonParsingException;

import core.exception.data.ADataException;
import core.exception.data.InvalidPropertyArrayException;

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
	 * @throws ADataException 
	 * @throws IOException 
	 * @throws JsonParsingException 
	 * @throws URISyntaxException 
	 */
	protected abstract void read(JsonObject data)
	throws ADataException, JsonParsingException, IOException, URISyntaxException;

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
	protected final Set<Integer> parseIntList(String toParse)
	throws NumberFormatException {
		Set<Integer> integers = new HashSet<Integer>();
		
		if (toParse != null && !toParse.isEmpty()) {
			//Don't really care what delimiters people use, tbh
			String[] intStrings = toParse.trim().split("[^0-9]");
			
			assert intStrings.length > 0;
			
			for (String intString : intStrings) {		
				integers.add(Integer.parseInt(intString));
			}
		}
		
		return integers;
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
	public final String getStringProperty(String property, String defaultVal) {
		return getData().getString(property, defaultVal);
	}
	
	public final String getStringProperty(String property) {
		return getStringProperty(property, null);
	}
	
	public final Boolean getBooleanProperty(String property) {
		if (getStringProperty(property, null) != null) {
			return getData().getBoolean(property);
		}

		return null;
	}

	public final Integer getIntegerProperty(String property) {
		if (getStringProperty(property, null) != null) {
			return getData().getInt(property);
		}

		return null;
	}
	
	/**
	 * Gets an arbitrary array of properties from the object's data.
	 *
	 * @param property the property array to read
	 * @return the property values array, or empty array if unset
	 * @throws InvalidPropertyArrayException 
	 */
	public final Set<String> getPropertyArray(String property)
	throws InvalidPropertyArrayException {
		JsonArray jsonProperties = getPropertyObjectArray(property);
		Set<String> properties = new HashSet<String>();
		
		if (jsonProperties == null) { throw new InvalidPropertyArrayException(); }
			
		for (JsonValue jsonProperty : jsonProperties) {
			properties.add(jsonProperty.toString());
		}
		
		return properties;
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
}
