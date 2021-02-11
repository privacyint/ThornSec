/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.profile.type;

import org.privacyinternational.thornsec.core.model.machine.InternalOnlyDeviceModel;

/**
 * This is an internal-only device on our network
 */
public class InternalOnly extends Device {

	public InternalOnly(InternalOnlyDeviceModel me) {
		super(me);
	}
}
