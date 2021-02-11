/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.exception.data;

public class InvalidPortException extends ADataException {
	private static final long serialVersionUID = 7168821353315878465L;

	public InvalidPortException(String message) {
		super(message);
	}

	public InvalidPortException(Integer port) {
		super(port + " is an invalid port - valid ports are 1-65535 (inclusive)");
	}
}
