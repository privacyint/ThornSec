package org.privacyinternational.thornsec.core.unit.fs;

import org.privacyinternational.thornsec.core.unit.SimpleUnit;

public class DirUnit extends SimpleUnit {

	private String path;
	private String owner;
	private String group;
	private int permissions;
	
	/**
	 * Unit test for recursively creating a directory, with custom fail messsage
	 * @param name         Name of the unit test (with _created appended)
	 * @param precondition Precondition unit test
	 * @param path          Directory to change ownership of
	 * @param message      Custom fail message
	 */
	public DirUnit(String name, String precondition, String path, String owner, String group, int permissions, String message) {
		super(name + "_created", precondition, "sudo mkdir -p " + path + ";", "sudo [ -d " + path + " ] && echo pass || echo fail;", "pass", "pass", message);
		
		this.path = path;
		this.owner = owner;
		this.group = group;
		this.permissions = permissions;
	}
	
	/**
	 * Unit test for recursively creating a directory, with default fail message
	 * @param name         Name of the unit test (with _created appended)
	 * @param precondition Precondition unit test
	 * @param path          Directory to change ownership of
	 */
	public DirUnit(String name, String precondition, String path) {
		this(name, precondition, path, "root", "root", 0660, "Couldn't create " + path + ".  This is pretty serious!");
	}

	public DirUnit(String name, String precondition, String path, int permissions) {
		this(name, precondition, path, "root", "root", permissions, "Couldn't create " + path + ".  This is pretty serious!");
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public void setPermissions(int permissions) {
		this.permissions = permissions;
	}

	/**
	 * @return the permissions
	 */
	public int getPermissions() {
		return permissions;
	}

	/**
	 * @return the owner
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * @return the group
	 */
	public String getGroup() {
		return group;
	}

	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}
}
