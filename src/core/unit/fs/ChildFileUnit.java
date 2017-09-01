package core.unit.fs;

import core.unit.ChildSimpleUnit;

public class ChildFileUnit extends ChildSimpleUnit {

	public ChildFileUnit(String parent, String name, String precondition, String text, String path) {
		super(parent, name, precondition, "echo \"" + text + "\" | sudo tee " + path + " > /dev/null;",
				"cat " + path + ";", text, "pass");
	}

}
