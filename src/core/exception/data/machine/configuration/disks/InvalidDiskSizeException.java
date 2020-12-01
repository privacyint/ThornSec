/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.exception.data.machine.configuration.disks;

public class InvalidDiskSizeException extends DiskModelException {
	private static final long serialVersionUID = -4978682429685931190L;

	public InvalidDiskSizeException(String message) {
		super(message);
	}

	public InvalidDiskSizeException(Integer size) {
		super(size + " is an invalid disk size. The minimum value is 512, but we recommend much bigger than that.");
	}
}
