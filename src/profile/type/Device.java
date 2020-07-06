/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.type;

import core.model.machine.ADeviceModel;

/**
 * This is a device on our network
 */
public class Device extends AMachine {

	public Device(ADeviceModel me) {
		super(me);
	}
}
