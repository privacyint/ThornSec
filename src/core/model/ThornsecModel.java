package core.model;

import java.io.StringReader;
import java.util.LinkedHashMap;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import core.data.NetworkData;

public class ThornsecModel {

	private LinkedHashMap<String, NetworkModel> networks;

	public void read(String data) {
		networks = new LinkedHashMap<String, NetworkModel>();

		JsonReader reader = Json.createReader(new StringReader(data));
		JsonObject nets = reader.readObject();
		
		for (String network : nets.keySet()) {
			NetworkModel net = new NetworkModel(network);
			NetworkData netData = new NetworkData(network);
			netData.read(nets.getJsonObject(network));
			net.setData(netData);
			networks.put(network, net);
		}
	}

	public void init() {
		for (String network : networks.keySet()) {
			networks.get(network).init();
		}
	}

	public String[] getNetworkLabels() {
		return networks.keySet().toArray(new String[networks.size()]);
	}

	public NetworkModel getNetworkModel(String label) {
		return networks.get(label);
	}

}
