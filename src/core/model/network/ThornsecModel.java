package core.model.network;

import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.stream.JsonParsingException;

import core.data.network.NetworkData;

import core.exception.data.ADataException;

public class ThornsecModel {

	private LinkedHashMap<String, NetworkModel> networks;

	public ThornsecModel() {
		this.networks = new LinkedHashMap<String, NetworkModel>();
	}
	
	public void read(String rawData)
	throws JsonParsingException, ADataException, IOException, URISyntaxException {
		JsonReader jsonReader = null;
		JsonObject networks   = null;
		
		jsonReader = Json.createReader(new StringReader(rawData));
		networks   = jsonReader.readObject();
		
		for (String networkName : networks.keySet()) {
			NetworkModel networkModel = new NetworkModel(networkName);
			NetworkData  networkData  = new NetworkData(networkName);
			
			networkData.read(networks.getJsonObject(networkName));
			networkModel.setData(networkData);
			
			this.networks.put(networkName, networkModel);
		}
	}

	public void init() {
		for (String networkName : networks.keySet()) {
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
