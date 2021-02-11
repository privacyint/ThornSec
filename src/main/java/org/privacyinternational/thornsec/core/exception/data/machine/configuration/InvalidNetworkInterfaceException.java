/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.core.exception.data.machine.configuration;

import org.privacyinternational.thornsec.core.exception.data.ADataException;

public class InvalidNetworkInterfaceException extends ADataException {
	private static final long serialVersionUID = 7168821353315878465L;

	public InvalidNetworkInterfaceException(String message) {
		super(message);
	}
}
