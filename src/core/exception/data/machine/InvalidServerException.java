/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.exception.data.machine;

public class InvalidServerException extends InvalidMachineException {
	private static final long serialVersionUID = -4978682429685931190L;

	public InvalidServerException(String message) {
		super(message);
	}
}
