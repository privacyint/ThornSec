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
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.stream.JsonParsingException;
import javax.mail.internet.AddressException;

import core.data.network.NetworkData;
import core.exception.AThornSecException;

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

	public void read(String filePath) throws JsonParsingException, IOException, URISyntaxException, AddressException, AThornSecException {
		JsonReader jsonReader = null;
		JsonObject networks = null;

		// Start by stripping comments out of the JSON
		final String rawText = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8).replaceAll("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)", "");

		jsonReader = Json.createReader(new StringReader(rawText));
		networks = jsonReader.readObject();

		for (final String networkName : networks.keySet()) {
			final NetworkModel networkModel = new NetworkModel(networkName);
			final NetworkData networkData = new NetworkData(networkName);

			networkData.read(networks.getJsonObject(networkName));
			networkModel.setData(networkData);

			this.networks.put(networkName, networkModel);
		}
	}

	public void init() throws AddressException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
			SecurityException, ClassNotFoundException, URISyntaxException, IOException, JsonParsingException, AThornSecException {
		for (final String networkName : this.networks.keySet()) {
			this.networks.get(networkName).init();
		}
	}

	public Collection<String> getNetworkLabels() {
		return this.networks.keySet();
	}

	public NetworkModel getNetwork(String label) {
		return this.networks.get(label);
	}

}
