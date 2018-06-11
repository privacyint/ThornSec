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
		String subnet = get3rdOctet();
		
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
	
	public String get3rdOctet() {
		return (subnetOffset + Arrays.asList(this.networkData.getDeviceLabels()).indexOf(getLabel())) + "";
	}
	
	public String[] getSubnets() {
		String[] macs    = getMacs();
		String[] subnets = new String[macs.length];
		
		for (int i = 0; i < macs.length; ++i) {
			subnets[i] = ipFromClass() + "." + (i*4);
		}
		
		return subnets;
	}
	
	public String[] getGateways() {
		String[] macs     = getMacs();
		String[] gateways = new String[macs.length];
		
		for (int i = 0; i < macs.length; ++i) {
			gateways[i] = ipFromClass() + "." + ((i*4)+1);
		}
		
		return gateways;
	}
	
	public String[] getIPs() {
		String[] macs = getMacs();
		String[] ips  = new String[macs.length];
		
		for (int i = 0; i < macs.length; ++i) {
			ips[i] = ipFromClass() + "." + ((i*4)+2);
		}
		
		return ips;
	}
	
	public String[] getBroadcasts() {
		String[] macs       = getMacs();
		String[] broadcasts = new String[macs.length];
		
		for (int i = 0; i < macs.length; ++i) {
			broadcasts[i] = ipFromClass() + "." + ((i*4)+3);
		}
		
		return broadcasts;
	}
	
	public String getNetmask() {
		return "255.255.255.252";
	}
	
	public String[] getMacs() {
		return this.networkData.getDeviceMacs(this.getLabel());
	}
	
	public String getType() {
		return this.networkData.getDeviceType(this.getLabel());
	}
	
	public Boolean isThrottled() {
		return this.networkData.getDeviceThrottled(this.getLabel());
	}

	public Boolean isManaged() {
		return this.networkData.getDeviceManaged(this.getLabel());
	}
	
	public String[] getPorts() {
		return this.networkData.getDevicePorts(this.getLabel());
	}
}
