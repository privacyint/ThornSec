/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.exception.runtime;

/**
 * Thrown when a given ServerModel isn't found
 */
public class InvalidServerModelException extends InvalidMachineModelException {
	private static final long serialVersionUID = 3473443319096462509L;

	public InvalidServerModelException(String message) {
		super(message);
	}
}
