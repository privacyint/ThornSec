package core.unit.fs;

import core.unit.SimpleUnit;

public class CustomFileUnit extends SimpleUnit {

	/**
	 * Unit test checks for/touches an arbitrary file path, with custom fail message
	 * @param name         Name of unit test
	 * @param precondition Precondition unit test name
	 * @param path         Path to the file
	 * @param message      Custom fail message
	 */
	public CustomFileUnit(String name, String precondition, String path, String message) {
		super(name, precondition,
				"sudo touch " + path + ";",
				"sudo [ -f " + path + " ] && echo pass || echo fail", "pass", "pass", message);
	}

	/**
	 * Unit test checks for/touches an arbitrary file path, with default fail message
	 * @param name         Name of unit test
	 * @param precondition Precondition unit test name
	 * @param path         Path to the file
	 */
	public CustomFileUnit(String name, String precondition, String path) {
		this(name, precondition, path, "Couldn't create " + path + ".  This is a pretty serious problem!");
	}

}
