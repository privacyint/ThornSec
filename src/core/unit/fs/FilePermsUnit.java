package core.unit.fs;

import core.unit.SimpleUnit;

public class FilePermsUnit extends SimpleUnit {

	/**
	 * Unit test for changing permissions on a file, with default fail message
	 * @param name         Name of the unit test (with _chmoded appended)
	 * @param precondition Precondition unit test
	 * @param file         File to change permissions of
	 * @param perms        Unix permissions to chmod to
	 */
	public FilePermsUnit(String name, String precondition, String file, String perms) {
		super(name, precondition,
				"sudo chmod " + perms + " " + file + ";",
				"sudo stat -c %a " + file + ";", perms, "pass",
				"Couldn't change the permissions of " + file + " to " + perms);

	}

	/**
	 * Unit test for changing permissions on a file, with custom fail message
	 * @param name         Name of the unit test (with _chmoded appended)
	 * @param precondition Precondition unit test
	 * @param file         File to change permissions of
	 * @param perms        Unix permissions to chmod to
	 * @param message      Custom fail message
	 */
	public FilePermsUnit(String name, String precondition, String file, String perms, String message) {
		super(name, precondition,
				"sudo chmod " + perms + " " + file + ";",
				"sudo stat -c %a " + file + ";", perms, "pass",
				message);

	}

}
