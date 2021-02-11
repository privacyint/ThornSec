/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.core.exception.data;

public class InvalidHostException extends ADataException {
	private static final long serialVersionUID = -6084824352999134898L;

	public InvalidHostException(String message) {
		super(message);
	}
}
