package core.unit.fs;

import core.unit.SimpleUnit;

public class DirOwnUnit extends SimpleUnit {

	/**
	 * Unit test for changing ownership of a directory
	 * @param name         Name of the unit test (with _chowned appended)
	 * @param precondition Precondition unit test
	 * @param dir          Directory to change ownership of
	 * @param user         User/Group to change ownership to
	 */
	public DirOwnUnit(String name, String precondition, String dir, String user) {
		super(name + "_chowned", precondition, "sudo chown -R " + user + ":" + user + " " + dir + ";",
				"sudo stat -c %U:%G " + dir + ";", user + ":" + user, "pass",
				"Couldn't change the ownership of " + dir + " to " + user + ":" + user);
	}

	/**
	 * Unit test for changing ownership of a directory
	 * @param name         Name of the unit test (with _chowned appended)
	 * @param precondition Precondition unit test
	 * @param dir          Directory to change ownership of
	 * @param user         User to change ownership to
	 * @param group        Group to change ownership to
	 */
	public DirOwnUnit(String name, String precondition, String dir, String user, String group) {
		super(name + "_chowned", precondition, "sudo chown -R " + user + ":" + group + " " + dir + ";",
				"sudo stat -c %U:%G " + dir + ";", user + ":" + group, "pass",
				"Couldn't change the ownership of " + dir + " to " + user + ":" + group);
	}
}
