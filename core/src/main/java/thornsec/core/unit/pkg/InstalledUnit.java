/*
 * 
 */
package core.unit.pkg;

import core.unit.SimpleUnit;

/**
 * This Unit manages the installation of a given package on Debian
 */
public class InstalledUnit extends SimpleUnit {

	public InstalledUnit(String name, String precondition, String pkg, String message) {
		super(name + "_installed", precondition,
				"export DEBIAN_FRONTEND=noninteractive; "
				+ "sudo apt-get update;"
				+ "sudo -E apt-get install --assume-yes " + pkg + ";",
				"dpkg-query --status " + pkg + " 2>&1 | grep -E \"Status: (install|hold) ok installed\";", "", "fail",
				message);
	}

	@Deprecated
	public InstalledUnit(String name, String pkg) {
		this(name, "proceed", pkg, "Couldn't install " + pkg + ".  This is pretty serious.");
	}

	public InstalledUnit(String name, String precondition, String pkg) {
		this(name, precondition, pkg, "Couldn't install " + pkg + ".  This is pretty serious.");
	}

}