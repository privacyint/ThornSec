package core.unit.fs;

import core.unit.SimpleUnit;

public class DirPermsUnit extends SimpleUnit {

	/**
	 * Unit test for changing permissions on a directory
	 * @param name         Name of the unit test (with _chmoded appended)
	 * @param precondition Precondition unit test
	 * @param dir          Directory to change permissions of
	 * @param perms        Unix permissions to chmod to
	 */
	public DirPermsUnit(String name, String precondition, String dir, String perms) {
		super(name + "_chmoded", precondition,
				"sudo chmod -R " + perms + " " + dir + ";",
				"sudo stat -c %a " + dir + ";", perms + "", "pass",
				"Couldn't change the permissions of " + dir + " to " + perms + "");
	}

}
