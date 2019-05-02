package core.model;

import java.util.Objects;
import java.util.Vector;

import core.iface.IUnit;
import core.unit.fs.DirMountedUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileAppendUnit;
import core.unit.pkg.InstalledUnit;

public class BindFsModel extends AModel {

	BindFsModel(String label, ServerModel me, NetworkModel networkModel) {
		super(label, me, networkModel);
	}

	public Vector<IUnit> getUnits() {
		Vector<IUnit> units = new Vector<IUnit>();

		units.addElement(new InstalledUnit("bindfs", "proceed", "bindfs"));
		
		return units;
	}

	public Vector<IUnit> addLogBindPoint(String name, String precondition, String username, String permissions) {
		return addBindPoint(name + "_log", precondition, "/var/log/." + name.replaceAll("-",  "_"), "/var/log/" + name.replaceAll("-",  "_"), username, username, permissions, "/var/log", true);
	}

	public Vector<IUnit> addDataBindPoint(String name, String precondition, String username, String group, String permissions) {
		return addBindPoint(name + "_data", precondition, "/media/metaldata/" + name.replaceAll("-",  "_"), "/media/data/" + name.replaceAll("-",  "_"), username, group, permissions, "/media/metaldata", false);
	}
	
	public Vector<IUnit> addBindPoint(String name, String precondition, String baseDirectory, String bindPoint, String username, String group, String permissions) {
		return addBindPoint(name, precondition, baseDirectory, bindPoint, username, group, permissions, "", false);
	}
		
	public Vector<IUnit> addBindPoint(String name, String precondition, String baseDirectory, String bindPoint, String username, String group, String permissions, String mountAfter, Boolean isNetDev) {
		Vector<IUnit> units = new Vector<IUnit>();

		String requires = (Objects.equals(mountAfter, "")) ? "" : ",x-systemd.after=" + mountAfter;
		
		if (isNetDev) {
			requires += ",_netdev";
		}
		
		//Make sure the directory exists
		units.addElement(new DirUnit(name + "_base_directory", precondition, baseDirectory));
		//Make sure the bind point exists
		units.addElement(new DirUnit(name + "_bindpoint", name + "_base_directory_created", bindPoint));
		//Add to our fstab
		units.addElement(new FileAppendUnit("fstab", name + "_bindpoint_created", baseDirectory + " " + bindPoint + " fuse.bindfs force-user=" + username + ",force-group=" + group + ",create-for-user=" + username + ",create-for-group=" + group + ",perms=" + permissions + requires + " 0 0", "/etc/fstab",
				"Couldn't add " + bindPoint + " to our fstab.  This means the directory will have the wrong permissions and there will be other failures."));
		
		//Mount!
		units.addElement(new DirMountedUnit(name, "fstab_appended", bindPoint, "Couldn't mount " + bindPoint + ".  This means the directory will have the wrong permissions and there will be other failures."));

		((ServerModel)me).getProcessModel().addProcess("bindfs " + baseDirectory + " " + bindPoint + " -o rw,force-user=" + username + ",force-group=" + group + ",create-for-user=" + username + ",create-for-group=" + group + ",perms=" + permissions + ",dev,suid$");
	
		return units;
	}

}
