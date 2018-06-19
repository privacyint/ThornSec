package core.model;

import java.util.Vector;

import core.iface.IUnit;
import core.unit.SimpleUnit;
import core.unit.fs.FileUnit;

public class ConfigFileModel extends AModel {

	private Vector<String> configFiles;

	public ConfigFileModel(String label) {
		super(label);
	}

	public void init(NetworkModel model) {
		this.configFiles = new Vector<String>();
	}

	public Vector<IUnit> getUnits() {
		String grepString = "sudo dpkg -V";
		Vector<IUnit> units = new Vector<IUnit>();
				
		for (String file : configFiles) {
			grepString += " | egrep -v \"" + file + "\"";
		}

		units.addElement(new SimpleUnit("no_config_file_tampering", "proceed",
				"",
				grepString, "", "pass",
				"There are unexpected config file edits on this machine.  This is a sign that someone has been configuring this machine by hand..."));
		
		return units;
	}

	public IUnit addConfigFile(String name, String precondition, String config, String path) {

		configFiles.addElement(path);
		
		return new FileUnit(name + "_config", precondition, config, path);
	}
}
