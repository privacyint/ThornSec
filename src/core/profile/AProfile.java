package core.profile;

import java.util.HashSet;
import java.util.Set;

import core.iface.IProfile;
import core.iface.IUnit;

import core.model.network.NetworkModel;

public abstract class AProfile implements IProfile {

	protected String name;
	protected NetworkModel networkModel;

	protected AProfile(String name, NetworkModel networkModel) {
		this.name         = name;
		this.networkModel = networkModel;
	}

	public final String getLabel() {
		return name;
	}
	
	public final NetworkModel getNetworkModel() {
		return networkModel;
	}

	public Set<IUnit> getNetworking() {
		return new HashSet<IUnit>();
	}
}
