package core.model;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Vector;

import core.data.NetworkData;
import core.iface.IUnit;

public class DeviceModel extends AModel {

	private NetworkData networkData;
	
	private int subnetOffset;
	
	public DeviceModel(String label) {
		super(label);
		this.subnetOffset = 100;
	}

	public void setData(NetworkData data) {
		this.networkData = data;
	}

	public void init(NetworkModel model) {
	}
	
	public Vector<IUnit> getUnits() {		
		Vector<IUnit> units = new Vector<IUnit>();
				
		//Make sure we have no duplication in our unit tests (this can happen occasionally)
		units = new Vector<IUnit>(new LinkedHashSet<IUnit>(units));
		
		return units;
	}

	private String ipFromClass() {
		String subnet = (subnetOffset + Arrays.asList(this.networkData.getDeviceLabels()).indexOf(getLabel())) + "";
		
		if (this.networkData.getIPClass().equals("c")) {
			return "192.168." + subnet;
		} else if (this.networkData.getIPClass().equals("b")) {
			return "172.16." + subnet;
		} else if (this.networkData.getIPClass().equals("a")) {
			return "10.0." + subnet;
		} else {
			return "0.0.0";
		}
	}
	
	public String getWirelessIP() {
		return ipFromClass() + ".6";
	}
	
	public String getWirelessGateway() {
		return ipFromClass() + ".5";
	}
	
	public String getWirelessBroadcast() {
		return ipFromClass() + ".4";
	}
	
	public String getWiredIP() {
		return ipFromClass() + ".2";
	}
	
	public String getWiredGateway() {
		return ipFromClass() + ".1";
	}

	public String getWiredBroadcast() {
		return ipFromClass() + ".0";
	}
	
	public String getNetmask() {
		return "255.255.255.252";
	}
	
	public String getWiredMac() {
		return this.networkData.getDeviceWiredMac(this.getLabel());
	}

	public String getWirelessMac() {
		return this.networkData.getDeviceWirelessMac(this.getLabel());
	}
	
	public String getType() {
		return this.networkData.getDeviceType(this.getLabel());
	}
}
