package core.unit.fs;

import core.unit.SimpleUnit;

public class FileAppendUnit extends SimpleUnit {

	/**
	 * Unit test for appending arbitrary text to a given file, with custom fail message
	 * @param name         Name of the unit test (with _appended appended)
	 * @param precondition Precondition unit test
	 * @param text         Text to append
	 * @param file         File to append to
	 * @param message      Custom fail message
	 */
	public FileAppendUnit(String name, String precondition, String text, String file, String message) {
		super(name + "_appended", precondition,
				"sudo bash -c '"
						+ "echo \"" + text + "\" >> " + file + ";"
				+ "';",
				"grep '^" + text + "' " + file + ";", "", "fail",
				message);
	}
	
	/**
	 * Unit test for appending arbitrary text to a given file, with default fail message
	 * @param name         Name of the unit test (with _appended appended)
	 * @param precondition Precondition unit test
	 * @param text         Text to append
	 * @param file         File to append to
	 */
	public FileAppendUnit(String name, String precondition, String text, String file) {
		this(name, precondition, text, file, "Couldn't append \"" + text + "\" to " + file);
	}

}
