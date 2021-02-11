/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.exception.data;

public class InvalidIngressSourceAddressException extends ADataException {
	private static final long serialVersionUID = -8454930124786177115L;

	public InvalidIngressSourceAddressException(String message) {
		super(message);
	}
}
