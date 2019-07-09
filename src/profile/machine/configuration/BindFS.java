/*
 * This code is part of the ThornSec project.
 * 
 * To learn more, please head to its GitHub repo: @privacyint
 * 
 * Pull requests encouraged.
 */
package profile.machine.configuration;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import core.iface.IUnit;

import core.model.AModel;
import core.model.network.NetworkModel;

import core.unit.fs.DirMountedUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileAppendUnit;

import core.unit.pkg.InstalledUnit;

public class BindFS extends AModel {

	public BindFS(String label, NetworkModel networkModel) {
		super(label, networkModel);
	}

	public Set<IUnit> getUnits() {
		Set<IUnit> units = new HashSet<IUnit>();

		units.add(new InstalledUnit("bindfs", "proceed", "bindfs"));
		
		return units;
	}

	public Set<IUnit> addLogBindPoint(String name, String precondition, String username, String permissions) {
		return addBindPoint(name + "_log", precondition, "/var/log/." + name.replaceAll("-",  "_"), "/var/log/" + name.replaceAll("-",  "_"), username, username, permissions, "/var/log", true);
	}

	public Set<IUnit> addDataBindPoint(String name, String precondition, String username, String group, String permissions) {
		return addBindPoint(name + "_data", precondition, "/media/metaldata/" + name.replaceAll("-",  "_"), "/media/data/" + name.replaceAll("-",  "_"), username, group, permissions, "/media/metaldata", false);
	}
	
	public Set<IUnit> addBindPoint(String name, String precondition, String baseDirectory, String bindPoint, String username, String group, String permissions) {
		return addBindPoint(name, precondition, baseDirectory, bindPoint, username, group, permissions, "", false);
	}
		
	public Set<IUnit> addBindPoint(String name, String precondition, String baseDirectory, String bindPoint, String username, String group, String permissions, String mountAfter, Boolean isNetDev) {
		Set<IUnit> units = new HashSet<IUnit>();

		String requires = (Objects.equals(mountAfter, "")) ? "" : ",x-systemd.after=" + mountAfter;
		
		if (isNetDev) {
			requires += ",_netdev";
		}
		
		//Make sure the directory exists
		units.add(new DirUnit(name + "_base_directory", precondition, baseDirectory));
		//Make sure the bind point exists
		units.add(new DirUnit(name + "_bindpoint", name + "_base_directory_created", bindPoint));
		//Add to our fstab
		units.add(new FileAppendUnit("fstab", name + "_bindpoint_created", baseDirectory + " " + bindPoint + " fuse.bindfs force-user=" + username + ",force-group=" + group + ",create-for-user=" + username + ",create-for-group=" + group + ",perms=" + permissions + requires + " 0 0", "/etc/fstab",
				"Couldn't add " + bindPoint + " to our fstab.  This means the directory will have the wrong permissions and there will be other failures."));
		
		//Mount!
		units.add(new DirMountedUnit(name, "fstab_appended", bindPoint, "Couldn't mount " + bindPoint + ".  This means the directory will have the wrong permissions and there will be other failures."));

		//@TODO: FIXME
		//networkModel.getData().getServerData(getLabel()).getProcessModel().addProcess("bindfs " + baseDirectory + " " + bindPoint + " -o rw,force-user=" + username + ",force-group=" + group + ",create-for-user=" + username + ",create-for-group=" + group + ",perms=" + permissions + ",dev,suid$");
	
		return units;
	}

}
