/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.core.exception.runtime;

public class InvalidMachineModelException extends ARuntimeException {
	private static final long serialVersionUID = 3473443319096462509L;

	public InvalidMachineModelException(String message) {
		super(message);
	}
}
