package core.unit.fs;

import core.unit.SimpleUnit;

public class FileUnit extends SimpleUnit {

	/**
	 * Unit test for creating/modifying a whole file, with custom fail message
	 * @param name         Name of unit test
	 * @param precondition Precondition unit test name
	 * @param text         Text to put into the file
	 * @param path         Path to the file
	 * @param message      Custom fail message
	 */
	public FileUnit(String name, String precondition, String text, String path, String message) {
		super(name, precondition,
				"sudo [ -f " + path + " ] || sudo touch " + path + ";"
				+ "echo \"" + text + "\" | sudo tee " + path + " > /dev/null;",
				"sudo cat " + path + " 2>&1;", text, "pass", message);
	}

	/**
	 * Unit test for creating/modifying a whole file, with default fail message
	 * @param name         Name of unit test
	 * @param precondition Precondition unit test name
	 * @param text         Text to put into the file
	 * @param path         Path to the file
	 */
	public FileUnit(String name, String precondition, String text, String path) {
		this(name, precondition, text, path, "Couldn't create " + path + ".  This is a pretty serious problem!");
	}

}