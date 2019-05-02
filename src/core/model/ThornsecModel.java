package core.model;

import java.io.StringReader;
import java.util.LinkedHashMap;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.stream.JsonParsingException;
import javax.swing.JOptionPane;

import core.data.NetworkData;

public class ThornsecModel {

	private LinkedHashMap<String, NetworkModel> networks;

	public ThornsecModel() {
		this.networks = new LinkedHashMap<String, NetworkModel>();
	}
	
	public void read(String rawData) {
		JsonReader jsonReader = null;
		JsonObject networks   = null;
		
		try {
			jsonReader = Json.createReader(new StringReader(rawData));
			networks   = jsonReader.readObject();
		}
		catch (JsonParsingException e) {
			JOptionPane.showMessageDialog(null, "I was unable to parse your JSON due to an error.\n\nThe error reported was: " + e.getLocalizedMessage());
			System.exit(1);
		}
		
		for (String network : networks.keySet()) {
			NetworkModel net     = new NetworkModel(network);
			NetworkData  netData = new NetworkData(network);
			netData.read(networks.getJsonObject(network));
			net.setData(netData);
			
			this.networks.put(network, net);
		}
	}

	public void init() {
		for (String network : networks.keySet()) {
			this.networks.get(network).init();
		}
	}

	public String[] getNetworkLabels() {
		return this.networks.keySet().toArray(new String[networks.size()]);
	}

	public NetworkModel getNetworkModel(String label) {
		return this.networks.get(label);
	}

}
