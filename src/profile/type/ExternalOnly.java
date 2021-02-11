/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.type;

import core.model.machine.ExternalOnlyDeviceModel;

/**
 * This is an external-only device on our network
 */
public class ExternalOnly extends Device {

	public ExternalOnly(ExternalOnlyDeviceModel me) {
		super(me);
	}
}
