/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.profile.type;

import org.privacyinternational.thornsec.core.model.machine.ADeviceModel;

/**
 * This is a device on our network
 */
public class Device extends AMachine {

	public Device(ADeviceModel me) {
		super(me);
	}
}
