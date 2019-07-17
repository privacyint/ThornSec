/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.profile;

import core.iface.IProfile;
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
}
