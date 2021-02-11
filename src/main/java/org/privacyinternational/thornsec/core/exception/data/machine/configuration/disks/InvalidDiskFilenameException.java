/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.core.exception.data.machine.configuration.disks;

import java.io.File;

public class InvalidDiskFilenameException extends DiskModelException {
	private static final long serialVersionUID = -4978682429685931190L;

	public InvalidDiskFilenameException(File filename) {
		super(filename.toString());
	}
}
