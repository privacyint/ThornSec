/*
 * This code is part of the ThornSec project.
 * 
 * To learn more, please head to its GitHub repo: @privacyint
 * 
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.core.data.machine;

/**
 * Represents an external-only device on our network.
 * 
 * This is a device which is allowed wider connection to the
 * Internet, but shouldn't be allowed to see any of our internal infra
 */
public class UserDeviceData extends ADeviceData {
	public UserDeviceData(String label) {
		super(label);
		
		this.putType(MachineType.USER);
	}
}
