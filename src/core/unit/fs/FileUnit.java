package core.unit.fs;

import core.unit.SimpleUnit;

public class FileUnit extends SimpleUnit {

	private String text;
	private String path;

	/**
	 * Unit test for creating/modifying a whole file, with custom fail message
	 * @param name         Name of unit test
	 * @param precondition Precondition unit test name
	 * @param text         Text to put into the file
	 * @param path         Path to the file
	 * @param message      Custom fail message
	 */
	public FileUnit(String name, String precondition, String text, String path, String message) {
		super(name, precondition, "", "sudo cat " + path + ";", text, "pass", message);
		this.text = text;
		this.path = path;
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
	
	public String getConfig() {
		String config = "";
		config += "sudo [ -f " + path + " ] || sudo touch " + path + ";";
		config += "echo \"" + getText() + "\" | sudo tee " + path + " > /dev/null;";
		
		return config;
	}

	public String getText() {
		return this.text;
	}

}