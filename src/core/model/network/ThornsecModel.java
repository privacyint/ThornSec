package core.model.network;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.stream.JsonParsingException;
import javax.mail.internet.AddressException;

import core.data.network.NetworkData;
import core.exception.data.ADataException;
import core.exception.data.machine.InvalidMachineException;
import core.exception.runtime.InvalidProfileException;
import core.exception.runtime.InvalidServerModelException;

public class ThornsecModel {

	private final LinkedHashMap<String, NetworkModel> networks;

	public ThornsecModel() {
		this.networks = new LinkedHashMap<>();
	}

	public void read(String rawData) throws JsonParsingException, ADataException, IOException, URISyntaxException {
		JsonReader jsonReader = null;
		JsonObject networks = null;

		jsonReader = Json.createReader(new StringReader(rawData));
		networks = jsonReader.readObject();

		for (final String networkName : networks.keySet()) {
			final NetworkModel networkModel = new NetworkModel(networkName);
			final NetworkData networkData = new NetworkData(networkName);

			networkData.read(networks.getJsonObject(networkName));
			networkModel.setData(networkData);

			this.networks.put(networkName, networkModel);
		}
	}

	public void init()
			throws InvalidMachineException, AddressException, InvalidServerModelException, InstantiationException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
			SecurityException, ClassNotFoundException, URISyntaxException, InvalidProfileException, IOException {
		for (final String networkName : this.networks.keySet()) {
			this.networks.get(networkName).init();
		}
	}

	public Set<String> getNetworkLabels() {
		return this.networks.keySet();
	}

	public NetworkModel getNetwork(String label) {
		return this.networks.get(label);
	}

}
