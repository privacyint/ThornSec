/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.core.exception.data;

public class NoValidUsersException extends ADataException {
	private static final long serialVersionUID = 2656177660603769643L;

	public NoValidUsersException() {
		super("A ThornSec network requires, at minimum, one user.");
	}

	public NoValidUsersException(String message) {
		super(message);
	}
}
