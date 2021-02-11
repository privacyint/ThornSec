/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.core.profile;

import org.privacyinternational.thornsec.core.iface.IProfile;
import org.privacyinternational.thornsec.core.model.machine.AMachineModel;
import org.privacyinternational.thornsec.core.model.network.NetworkModel;

/**
 */
public abstract class AProfile implements IProfile {
	private final AMachineModel me;

	protected AProfile(AMachineModel me) {
//		//assertNotNull(me);

		this.me = me;
	}

	/**
	 * Get the network in which this Profile exists
	 * @return
	 */
	public final NetworkModel getNetworkModel() {
		return me.getNetworkModel();
	}

	/**
	 * Get the Machine on which this Profile exists
	 * 
	 * @return AMachineModel representation of this Profile's machine
	 */
	public AMachineModel getMachineModel() {
		return me;
	}
}
