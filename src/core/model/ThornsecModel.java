package core.model;

import java.io.StringReader;
import java.util.Iterator;
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
		Iterator<?> iter = nets.keySet().iterator();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			NetworkModel net = new NetworkModel(key);
			NetworkData netData = new NetworkData(key);
			netData.read(nets.getJsonObject(key));
			net.setData(netData);
			networks.put(key, net);
		}
	}

	public void init() {
		Iterator<?> iter = networks.keySet().iterator();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			networks.get(key).init();
		}
	}

	public String[] getNetworkLabels() {
		String[] labs = new String[networks.keySet().size()];
		Iterator<?> iter = networks.keySet().iterator();
		int i = 0;
		while (iter.hasNext()) {
			String key = (String) iter.next();
			labs[i] = key;
			i++;
		}
		return labs;
	}

	public NetworkModel getNetworkModel(String label) {
		return networks.get(label);
	}

}
