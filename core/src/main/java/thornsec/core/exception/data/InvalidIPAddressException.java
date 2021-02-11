/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.exception.data;

public class InvalidIPAddressException extends ADataException {
	private static final long serialVersionUID = -6084824352999134898L;

	public InvalidIPAddressException(String message) {
		super(message);
	}
}
