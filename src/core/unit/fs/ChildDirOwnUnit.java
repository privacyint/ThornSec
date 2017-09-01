package core.unit.fs;

import core.unit.ChildSimpleUnit;

public class ChildDirOwnUnit extends ChildSimpleUnit {

	public ChildDirOwnUnit(String parent, String name, String precondition, String dir, String user) {
		super(parent, name, precondition, "sudo chown -R " + user + ":" + user + " " + dir + ";",
				"sudo stat -c %%U " + dir + ";", user, "pass");
	}

}
