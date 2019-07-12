package core.profile;

import core.iface.IProfile;

import core.model.NetworkModel;

public abstract class AProfile implements IProfile {

	private String       label;
	private NetworkModel networkModel;

	protected AProfile(String label, NetworkModel networkModel) {
		this.label        = label;
		this.networkModel = networkModel;
	}

	public final String getLabel() {
		return label;
	}
	
	protected NetworkModel getNetworkModel() {
		return networkModel;
	}
}
