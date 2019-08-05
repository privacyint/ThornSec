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

import javax.json.JsonObject;
import javax.json.stream.JsonParsingException;

import core.exception.data.ADataException;

/**
 * Abstract class for something representing "Data" on our network.
 * 
 * This is something which has been read() from a JSON.
 * 
 * Beware, {@code null} is valid data! 
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
}
