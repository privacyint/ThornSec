/*
 * This code is part of the ThornSec project.
 * 
 * To learn more, please head to its GitHub repo: @privacyint
 * 
 * Pull requests encouraged.
 */
package core.unit.fs;

import java.io.File;
import java.util.LinkedHashSet;

import core.unit.SimpleUnit;

/**
 * This Unit is designed for whole files.
 * 
 *  It will perform tests on any property which is set.
 */
public class FileUnit extends SimpleUnit {

	private File path;
	private LinkedHashSet<String> lines;
	
	/**
	 * Unit for writing out a whole file, with custom fail message
	 * @param name         Name of unit test
	 * @param precondition Precondition unit test name
	 * @param path         Path to the file
	 * @param message      Custom fail message
	 */
	public FileUnit(String name, String precondition, String path, String message) {
		super(name, precondition,
				null,
				"sudo cat " + path + " 2>&1;", "", "pass",
				message);
		
		this.lines = new LinkedHashSet<String>();
		this.path  = new File(path);
	}

	/**
	 * Unit for writing out a whole file, with default fail message
	 * @param name         Name of unit test
	 * @param precondition Precondition unit test name
	 * @param path         Path to the file
	 */
	public FileUnit(String name, String precondition, String path) {
		this(name, precondition, path, "Couldn't create " + path + ".  This is a pretty serious problem!");
	}
	
	private void rebuildUnit() {
		String body = String.join("", this.lines);
		
		//Remove any trailing newline when echoing out...
		if (body.endsWith("\n")) {
			body = body.substring(0, body.length() - 3);
		}
		
		super.config = "sudo [ -f " + this.path + " ] || sudo touch " + this.path + ";"
					 + "echo \"" + body + "\" | sudo tee " + this.path + " > /dev/null";
		
		super.test = body;
	}
	
	/**
	 * Append a line of text to this FileUnit.
	 * @param line
	 * @param endWithCarriageReturn
	 */
	public final void appendLine(String line, Boolean endWithCarriageReturn) {
		this.lines.add(line);
		this.rebuildUnit();
	}
	
	
	public final void appendLine(String line) {
		this.appendLine(line, true);		
	}

	public final void appendCarriageReturn() {
		this.appendLine("", true);
	}
}
