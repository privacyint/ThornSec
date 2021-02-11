/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.core.model.network;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import org.privacyinternational.thornsec.core.data.network.NetworkData;

import org.privacyinternational.thornsec.core.exception.AThornSecException;
import org.privacyinternational.thornsec.core.exception.data.ADataException;
import org.privacyinternational.thornsec.core.exception.data.InvalidJSONException;

/**
 * This is the model at the very heart of ThornSec.
 *
 * This model initialises and populates our various networks.
 */
public class ThornsecModel {

	private final Map<String, NetworkModel> networks;
	private Path configPath;

	public ThornsecModel() {
		this.networks = new LinkedHashMap<>();
		this.configPath = null;
	}

	/**
	 * Read in JSON 
	 * @param filePath
	 * @throws ADataException 
	 */
	public void read(String filePath) throws ADataException {
		String rawText = null;
		Path configFilePath = Paths.get(filePath);

		// Start by stripping comments out of the JSON
		try {
			byte[] raw = Files.readAllBytes(configFilePath);
			rawText = new String(raw, StandardCharsets.UTF_8);
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
			networkData.read((JsonObject) network.getValue(), configFilePath);

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

	/**
	 * Get the path to config file this network was configured against, useful
	 * for 
	 * @return absolute Path
	 */
	public Path getConfigFilePath() {
		return this.configPath;
	}
}
