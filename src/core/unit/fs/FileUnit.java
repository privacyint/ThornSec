/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.unit.fs;

import java.util.ArrayList;
import java.util.Collection;

import core.unit.SimpleUnit;

/**
 * This Unit is designed for whole files.
 *
 * It will perform tests on any property which is set.
 */
public class FileUnit extends SimpleUnit {

	private String path;
	private Collection<String> lines;
	private int permissions;
	private String owner;
	private String group;

	/**
	 * Unit for writing out a whole file, with custom fail message
	 *
	 * @param name         Name of unit test
	 * @param precondition Precondition unit test name
	 * @param path         Path to the file
	 * @param message      Custom fail message
	 * @param permissions  File permissions, as an octal
	 */
	public FileUnit(String name, String precondition, String path, String owner, String group, int permissions, String message) {
		super(name, precondition, "sudo touch " + path, "sudo cat " + path + " 2>&1;", "", "pass", message);

		this.lines = new ArrayList<>();
		this.path = path;
		this.permissions = permissions;
		this.owner = owner;
		this.group = group;
	}

	/**
	 * Unit for writing out a whole file, with custom fail message
	 *
	 * @param name         Name of unit test
	 * @param precondition Precondition unit test name
	 * @param path         Path to the file
	 */
	public FileUnit(String name, String precondition, String path, String message) {
		this(name, precondition, path, "root", "root", 0660, message);
	}

	/**
	 * Unit for writing out a whole file, with custom fail message
	 *
	 * @param name         Name of unit test
	 * @param precondition Precondition unit test name
	 * @param path         Path to the file
	 */
	public FileUnit(String name, String precondition, String path, int permissions, String message) {
		this(name, precondition, path, "root", "root", permissions, message);
	}
	
	public FileUnit(String name, String precondition, String path, int permissions) {
		this(name, precondition, path, "root", "root", permissions, "Couldn't create " + path + ".  This is a pretty serious problem!");
	}
	
	/**
	 * Unit for writing out a whole file, with default fail message
	 *
	 * @param name         Name of unit test
	 * @param precondition Precondition unit test name
	 * @param path         Path to the file
	 */
	public FileUnit(String name, String precondition, String path) {
		this(name, precondition, path, "Couldn't create " + path + ".  This is a pretty serious problem!");
	}

	private void rebuildUnit() {
		String body = String.join("", this.lines);

		// Remove any trailing newline when echoing out...
		while (body.endsWith("\n")) {
			body = body.substring(0, body.length() - 1);
		}

		super.config = "sudo [ -f " + this.path + " ] || sudo touch " + this.path + ";" + "echo \"${" + this.label + "_expected}\" | sudo tee " + this.path + " > /dev/null";

		super.test = body;
	}

	/**
	 * Check the FileUnit for an occurrence of a given line of text
	 *
	 * @param line The text to search for
	 * @return true if the FileUnit contains at least one occurrence of the given
	 *         text; false otherwise
	 */
	public final Boolean containsLine(String line) {
		return this.lines.contains(line);
	}

	/**
	 * Append text to this FileUnit, without a trailing carriage return
	 * 
	 * @param text
	 */
	public final void appendText(String text) {
		this.appendText(text, false);
	}

	/**
	 * Append a line of text to this FileUnit.
	 *
	 * @param line
	 * @param endWithCarriageReturn
	 */
	public final void appendText(String line, Boolean endWithCarriageReturn) {
		if (endWithCarriageReturn) {
			line += "\n";
		}
		this.lines.add(line);
		rebuildUnit();
	}

	/**
	 * Append line(s) of text to this FileUnit, each ending with a carriage return
	 *
	 * @param line
	 */
	public final void appendLine(String... lines) {
		for (final String line : lines) {
			this.appendText(line, true);
		}
	}
	
	/**
	 * Append line(s) of text to this FileUnit, each ending with a carriage return
	 *
	 * @param line
	 */
	public final void appendLine(Collection<String> lines) {
		appendLine(lines.toArray(String[]::new));
	}

	/**
	 * Append a carriage return to this FileUnit
	 */
	public final void appendCarriageReturn() {
		this.appendText("", true);
	}

	public Collection<String> getLines() {
		return lines;
	}
	
	protected void setLines(Collection<String> lines) {
		this.lines = lines;
	}

	public void setPermissions(int permissions) {
		this.permissions = permissions;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public int getPermissions() {
		return permissions;
	}

	public String getOwner() {
		return owner;
	}

	public String getGroup() {
		return group;
	}

	public String getPath() {
		return this.path;
	}

	protected void setPath(String path) {
		this.path = path;
	}
}
