package core.unit.fs;

import core.unit.SimpleUnit;

public class FileOwnUnit extends SimpleUnit {

	/**
	 * Unit test for changing ownership of a file
	 * @param name         Name of the unit test (with _chowned appended)
	 * @param precondition Precondition unit test
	 * @param file         File to change ownership of
	 * @param user         User to change ownership to
	 * @param group        Group to change ownership to
	 */
	public FileOwnUnit(String name, String precondition, String file, String user, String group) {
		super(name + "_chowned", precondition, "sudo chown " + user + ":" + group + " " + file + ";",
				"sudo stat -c %U:%G " + file + ";", user + ":" + group, "pass",
				"Couldn't change the ownership of " + file + " to " + user + ":" + group);
	}

	/**
	 * Unit test for changing ownership of a file
	 * @param name         Name of the unit test (with _chowned appended)
	 * @param precondition Precondition unit test
	 * @param file         File to change ownership of
	 * @param user         User/Group to change ownership to
	 */
	public FileOwnUnit(String name, String precondition, String file, String user) {
		this(name, precondition, file, user, user);
	}
}
