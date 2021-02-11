/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.core.exception.data;

import org.privacyinternational.thornsec.core.exception.AThornSecException;

/**
 * An abstract error, representing something wrong with data ingestion
 */
public abstract class ADataException extends AThornSecException {
	private static final long serialVersionUID = 7533173446854104304L;

	public ADataException(String message) {
		super(message);
	}

}
