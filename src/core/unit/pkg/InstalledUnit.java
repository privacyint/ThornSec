package core.unit.pkg;

import core.unit.SimpleUnit;

public class InstalledUnit extends SimpleUnit {

	public InstalledUnit(String name, String pkg) {
		super(name + "_installed", "proceed",
				"export DEBIAN_FRONTEND=noninteractive; "
				+ "sudo apt-get update;"
				+ "sudo -E apt-get install --assume-yes " + pkg + ";",
				"dpkg-query --status " + pkg + " | grep \"Status:\";", "Status: install ok installed", "pass",
				"Couldn't install " + pkg + ".  This is pretty serious.");
	}

	public InstalledUnit(String name, String precondition, String pkg) {
		super(name + "_installed", precondition,
				"export DEBIAN_FRONTEND=noninteractive; "
				+ "sudo apt-get update;"
				+ "sudo -E apt-get install --assume-yes " + pkg + ";",
				"dpkg-query --status " + pkg + " | grep \"Status:\";", "Status: install ok installed", "pass",
				"Couldn't install " + pkg + ".  This is pretty serious.");
	}

	public InstalledUnit(String name, String precondition, String pkg, String message) {
		super(name + "_installed", precondition,
				"export DEBIAN_FRONTEND=noninteractive; "
				+ "sudo apt-get update;"
				+ "sudo -E apt-get install --assume-yes " + pkg + ";",
				"dpkg-query --status " + pkg + " | grep \"Status:\";", "Status: install ok installed", "pass",
				message);
	}
}