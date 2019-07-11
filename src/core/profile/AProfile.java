/*
 * This code is part of the ThornSec project.
 * 
 * To learn more, please head to its GitHub repo: @privacyint
 * 
 * Pull requests encouraged.
 */
package core.profile;

import java.util.HashSet;
import java.util.Set;

import core.iface.IProfile;
import core.iface.IUnit;
import core.model.network.NetworkModel;

public abstract class AProfile implements IProfile {

	private final String label;
	protected NetworkModel networkModel;

	protected AProfile(String label, NetworkModel networkModel) {
		this.label = label;
		this.networkModel = networkModel;
	}

	@Override
	public final String getLabel() {
		return this.label;
	}

	public final NetworkModel getNetworkModel() {
		return this.networkModel;
	}

	public Set<IUnit> getNetworking() {
		return new HashSet<>();
	}
}
