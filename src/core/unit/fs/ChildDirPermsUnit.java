package core.unit.fs;

import core.unit.ChildSimpleUnit;

public class ChildDirPermsUnit extends ChildSimpleUnit {

	public ChildDirPermsUnit(String parent, String name, String precondition, String dir, String perms) {
		super(parent, name, precondition, "sudo chmod -R " + perms + " " + dir + ";", "sudo stat -c %%a " + dir + ";",
				perms, "pass");
	}

}
