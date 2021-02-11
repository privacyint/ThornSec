/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.core.unit.pkg;

import org.privacyinternational.thornsec.core.unit.SimpleUnit;

public class EnabledServiceUnit extends SimpleUnit {

	public EnabledServiceUnit(String name, String precondition, String service, String message) {
		super(name + "_enabled", precondition, "sudo systemctl enable " + service + ";",
				"sudo systemctl is-enabled " + service + ";", "enabled", "pass", message);
	}

	public EnabledServiceUnit(String name, String service, String message) {
		this(name, name + "_installed", service, message);
	}

	public EnabledServiceUnit(String name, String service) {
		this(name, service, "I can't enable the " + service + ". This is bad.");
	}

}
