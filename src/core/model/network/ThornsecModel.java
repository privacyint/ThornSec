/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.network;

import java.io.IOException;
import java.io.StringReader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import core.data.network.NetworkData;

import core.exception.AThornSecException;
import core.exception.data.ADataException;
import core.exception.data.InvalidJSONException;

/**
 * This is the model at the very heart of ThornSec.
 *
 * This model initialises and populates our various networks.
 */
public class ThornsecModel {

	private final Map<String, NetworkModel> networks;

	public ThornsecModel() {
		this.networks = new LinkedHashMap<>();
	}

	/**
	 * Read in JSON 
	 * @param filePath
	 * @throws ADataException 
	 */
	public void read(String filePath) throws ADataException {
		String rawText = null;
		
		// Start by stripping comments out of the JSON
		try {
			rawText = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
			rawText = rawText.replaceAll("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)", "");
		}
		catch (IOException e) {
			throw new InvalidJSONException("Unable to read the file at " + filePath);
		}

		JsonReader jsonReader = null;
		JsonObject networks = null;
		jsonReader = Json.createReader(new StringReader(rawText));
		networks = jsonReader.readObject();

		for (final Entry<String, JsonValue> network : networks.entrySet()) {
			final NetworkData networkData = new NetworkData(network.getKey());
			networkData.read((JsonObject) network.getValue());

			final NetworkModel networkModel = new NetworkModel(network.getKey());
			networkModel.setData(networkData);

			this.networks.put(network.getKey(), networkModel);
		}
	}

	public void init() throws AThornSecException {
		for (final NetworkModel network : this.networks.values()) {
			network.init();
		}
	}

	/**
	 * Returns the labels of our various networks
	 * @return
	 */
	public Collection<String> getNetworkLabels() {
		return this.networks.keySet();
	}

	/**
	 * Get a specific network by its label
	 * @param label
	 * @return
	 */
	public NetworkModel getNetwork(String label) {
		return this.networks.get(label);
	}

}
