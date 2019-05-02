package core.profile;

import java.util.Vector;

import core.iface.IProfile;
import core.iface.IUnit;
import core.model.MachineModel;
import core.model.NetworkModel;

public abstract class AProfile implements IProfile {

	protected String name;
	protected NetworkModel networkModel;
	protected MachineModel me;

	protected AProfile(String name, MachineModel me, NetworkModel networkModel) {
		this.name         = name;
		this.networkModel = networkModel;
		this.me           = me;
	}

	public String getLabel() {
		return name;
	}
	
	public NetworkModel getNetworkModel() {
		return networkModel;
	}

	public MachineModel getMachineModel() {
		return me;
	}
	
	public abstract Vector<IUnit> getNetworking();
}
