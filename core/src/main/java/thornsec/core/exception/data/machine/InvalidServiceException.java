/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.exception.data.machine;

/**
 * Thrown when a service doesn't exist in the JSON or, by implication,
 * in our Data or Network.
 */
public class InvalidServiceException extends InvalidMachineException {
	private static final long serialVersionUID = -4978682429685931190L;

	public InvalidServiceException(String message) {
		super(message);
	}
}
