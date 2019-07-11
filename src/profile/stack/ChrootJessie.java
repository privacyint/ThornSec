package profile.stack;

import java.util.HashSet;
import java.util.Set;

import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.network.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.DirMountedUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileAppendUnit;
import core.unit.pkg.InstalledUnit;

public class ChrootJessie extends AStructuredProfile {

	private String CHROOT_DIR = "/media/metaldata/chroot";
	
	public ChrootJessie(String label, NetworkModel networkModel) {
		super(label, networkModel);
	}

	@Override
	protected Set<IUnit> getInstalled() {
		Set<IUnit> units = new HashSet<IUnit>();
		
		units.add(new InstalledUnit("debootstrap", "proceed", "debootstrap"));
		
		units.add(new DirUnit("chroot", "debootstrap_installed", getChrootDir()));
		
		units.add(new SimpleUnit("chroot_installed", "chroot_created",
				"sudo debootstrap jessie " + getChrootDir(),
				"[ -d " + getChrootDir() + "/home ] && echo pass || echo fail", "pass", "pass"));

		String configcmd = "";
		if (networkModel.getData().getAutoUpdate(getLabel())) {
			configcmd = "sudo chroot " + getChrootDir() + " apt-get --assume-yes upgrade;";
		}
		else {
			configcmd = "echo \"There are `sudo chroot " + getChrootDir() + " apt-get upgrade -s |grep -P '^\\\\d+ upgraded'|cut -d\\\" \\\" -f1` updates available, of which `sudo chroot " + getChrootDir() + " apt-get upgrade -s | grep ^Inst | grep Security | wc -l` are security updates\"";
		}
		units.add(new SimpleUnit("chroot_update", "chroot_installed", configcmd,
				"sudo chroot " + getChrootDir() + " apt-get update > /dev/null; sudo chroot " + getChrootDir() + " apt-get --assume-no upgrade | grep \"[0-9] upgraded, [0-9] newly installed, [0-9] to remove and [0-9] not upgraded.\";",
				"0 upgraded, 0 newly installed, 0 to remove and 0 not upgraded.", "pass",
				"There are `sudo chroot " + getChrootDir() + " apt-get upgrade -s |grep -P '^\\\\d+ upgraded'|cut -d\" \" -f1` updates available, of which `sudo chroot " + getChrootDir() + " apt-get upgrade -s | grep ^Inst | grep Security | wc -l` are security updates\""));

		units.add(new FileAppendUnit("chroot_proc", "chroot_installed",
				"/proc " + getChrootDir() + "/proc none rw,bind 0 0",
				"/etc/fstab",
				"Couldn't create the mount point for /proc in the chroot.  This will cause things to get a bit funky!!"));
		
		units.add(new DirMountedUnit("chroot_proc", "chroot_proc_appended", getChrootDir() + "/proc",
				"Couldn't mount /proc in the chroot.  This will cause things to get a bit funky!!"));
		
		return units;
	}
	
	@Override
	public Set<IUnit> getPersistentFirewall()
	throws InvalidServerModelException {
		Set<IUnit> units = new HashSet<IUnit>();
		
		networkModel.getServerModel(getLabel()).addEgress("deb.debian.org:80,443");

		return units;
	}
	
	public String getChrootDir() {
		return CHROOT_DIR;
	}
	
	public Set<IUnit> installPackage(String name, String precondition, String pkg) {
		Set<IUnit> units = new HashSet<IUnit>();

		units.add(new SimpleUnit(name + "_installed", precondition,
			"export DEBIAN_FRONTEND=noninteractive; "
			+ "sudo chroot " + getChrootDir() + " apt-get update;"
			+ "sudo chroot " + getChrootDir() + " apt-get install --assume-yes " + pkg + ";",
			"sudo chroot " +  getChrootDir() + " dpkg-query --status " + pkg + " | grep \"Status:\";", "Status: install ok installed", "pass",
			"Couldn't install " + pkg + " in your chroot.  This is pretty serious."));
		
		return units;
	}
}
