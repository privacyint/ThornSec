/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.profile.type;

import org.privacyinternational.thornsec.core.model.machine.UserDeviceModel;

/**
 * This is a User Device
 */
public class User extends AMachine {

	public User(UserDeviceModel me) {
		super(me);
	}
}
