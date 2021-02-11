/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.core.exception.data.machine;

/**
 * Thrown when a server doesn't exist in the JSON or, by implication, in our Data.
 */
public class InvalidServerException extends InvalidMachineException {
	private static final long serialVersionUID = -4978682429685931190L;

	public InvalidServerException(String message) {
		super(message);
	}
}
