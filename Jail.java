package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileAppendUnit;
import core.unit.pkg.InstalledUnit;

public class Jail extends AStructuredProfile {

	public Jail() {
		super("jail");
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new InstalledUnit("libcap2_bin", "proceed", "libcap2-bin"));
		
		//a=D   : ALL, execute bit only on dirs only
		//fu=rw : Files; user, R+W
		//fog=r : Files; group + others, R
		model.getServerModel(server).getBindFsModel().addDataBindPoint(server, model, "jails", "proceed", "root", "root", "a=D:fu=rw:fog=r");
		
		return units;
	}

	public Vector<IUnit> buildJail(String server, NetworkModel model, String jail) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String   jailRoot    = "/media/data/jails/" + jail;
		String[] directories = { "/dev",
								 "/etc",
								 "/bin",
								 "/usr",
								 "/usr/lib",
								 "/usr/sbin",
								 "/usr/bin",
								 "/var",
								 "/var/run",
								 "/run",
								 "/tmp",
								};
		String[] etcFiles    = { "services",
								 "localtime",
								 "nsswitch.conf",
								 "protocols",
								 "hosts",
								 "ld.so.cache",
								 "ld.so.conf",
								 "resolv.conf",
								 "host.conf",
								};
		
		units.addElement(new DirUnit(jail + "_jail_root", "proceed", jailRoot, "Couldn't create a base directory for your jail. Everything past here will fail."));
		
		for (String dir : directories) {
			units.addElement(new DirUnit(jail + "_jail" + dir.replace("/", "_"), jail + "_jail_root_created", jailRoot + dir, "Couldn't create a the " + dir + " directory for your jail. Expct weird behaviour."));
		}
		
		units.addElement(new SimpleUnit("libraries_symlink", jail + "_jail_root_created",
			    "cd " + jailRoot + ";"
			    + "if [ \\$(uname -m) = \"x86_64\" ]; then;"
			    	+ "sudo ln -s usr/lib lib64;"
			    	+ "sudo ln -s lib lib64;"
			    + "else;"
			    	+ "sudo ln -s usr/lib lib;"
			    + "fi",
				"sudo readlink " + jailRoot + "/usr/lib ", "", "fail"));

		units.addElement(new SimpleUnit(jail + "_dev_null", jail + "_jail_dev_created",
				"sudo mknod -m 0666 " + jailRoot + "/dev/null c 1 3",
				"sudo ls " + jailRoot + "/dev/null", jailRoot + "/dev/null", "pass"));
		units.addElement(new SimpleUnit(jail + "_dev_random", jail + "_jail_dev_created",
				"sudo mknod -m 0666 " + jailRoot + "/dev/random c 1 8",
				"sudo ls " + jailRoot + "/dev/random", jailRoot + "/dev/random", "pass"));
		units.addElement(new SimpleUnit(jail + "_dev_urandom", jail + "_jail_dev_created",
				"sudo mknod -m 0444 " + jailRoot + "/dev/urandom c 1 9",
				"sudo ls " + jailRoot + "/dev/urandom", jailRoot + "/dev/urandom", "pass"));
		
		for (String file : etcFiles) {
			units.addElement(new SimpleUnit(jail + "_jail_" + file.replace(".", "_"), jail + "_jail_etc_created",
					"sudo cp -rfL /etc/" + file + " " + jailRoot + "/etc",
					"sudo [ -f " + jailRoot + "/" + file + " ] && echo pass", "pass", "pass"));
		}

		units.addAll(addUser(server, model, jail, "nobody", "99", "99"));
		
		return units;
	}

	public Vector<IUnit> addUser(String server, NetworkModel model, String jail, String user, String uid, String gid) {
		Vector<IUnit> units = new Vector<IUnit>();

		String jailRoot = "/media/data/jails/" + jail;

		units.addElement(new FileAppendUnit(jail + "_jail_adduser_" + user + "_passwd", jail + "_jail_etc_created", user + ":x:" + uid + ":" + gid + ":" + user + ":/:/bin/false", jailRoot + "/etc/passwd", "Could not add the user " + user + " to the jail " + jail));
		units.addElement(new FileAppendUnit(jail + "_jail_addgroup_" + user, jail + "_jail_etc_created", user + ":x:" + gid + ":", jailRoot + "/etc/group", "Could not add the group " + user + " to the jail " + jail));
		units.addElement(new FileAppendUnit(jail + "_jail_adduser_" + user + "_shadow", jail + "_jail_etc_created", user + ":x:14871::::::", jailRoot + "/etc/shadow", "Could not add the user " + user + " to the jail " + jail));
		units.addElement(new FileAppendUnit(jail + "_jail_adduser_" + user + "_gshadow", jail + "_jail_etc_created", user + ":::", jailRoot + "/etc/gshadow", "Could not add the user " + user + " to the jail " + jail));
		
		return units;
	}
}
