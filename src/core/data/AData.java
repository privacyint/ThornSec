/*
 * This code is part of the ThornSec project.
 * 
 * To learn more, please head to its GitHub repo: @privacyint
 * 
 * Pull requests encouraged.
 */
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
	 * Gets an arbitrary property from the object's data.
	 *
	 * You should avoid using this method directly where possible, but we keep it
	 * public in case a profile wishes to use it.
	 *
	 * @param property   the property to read
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
	public final Set<String> getPropertyArray(String property) throws InvalidPropertyArrayException {
		final JsonArray jsonProperties = getPropertyObjectArray(property);
		final Set<String> properties = new HashSet<>();

		if (jsonProperties == null) {
			throw new InvalidPropertyArrayException();
		}

		for (final JsonValue jsonProperty : jsonProperties) {
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
