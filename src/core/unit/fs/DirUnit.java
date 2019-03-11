package core.unit.fs;

import core.unit.SimpleUnit;

public class DirUnit extends SimpleUnit {

	/**
	 * Unit test for recursively creating a directory, with custom fail messsage
	 * @param name         Name of the unit test (with _created appended)
	 * @param precondition Precondition unit test
	 * @param dir          Directory to change ownership of
	 * @param message      Custom fail message
	 */
	public DirUnit(String name, String precondition, String dir, String message) {
		super(name + "_created", precondition, "sudo mkdir -p " + dir + ";", "sudo [ -d " + dir + " ] && echo pass || echo fail;", "pass", "pass", message);
	}
	
	/**
	 * Unit test for recursively creating a directory, with default fail message
	 * @param name         Name of the unit test (with _created appended)
	 * @param precondition Precondition unit test
	 * @param dir          Directory to change ownership of
	 */
	public DirUnit(String name, String precondition, String dir) {
		this(name, precondition, dir, "Couldn't create " + dir + ".  This is pretty serious!");
	}

}
