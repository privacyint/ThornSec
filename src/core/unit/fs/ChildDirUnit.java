package core.unit.fs;

import core.unit.ChildSimpleUnit;

public class ChildDirUnit extends ChildSimpleUnit {

	public ChildDirUnit(String parent, String name, String precondition, String dir) {
		super(parent, name, precondition, "sudo mkdir " + dir + ";", "sudo [ -d " + dir + " ] && echo pass;", "pass",
				"pass");
	}

}
