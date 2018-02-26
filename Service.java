package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.DirMountedUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileAppendUnit;
import core.unit.pkg.InstalledUnit;

public class Service extends AStructuredProfile {
	
	public Service() {
		super("service");
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		//First, we need to be sure we're actually in a VirtualBox guest, or the rest of this is moot
		units.addElement(new SimpleUnit("is_virtualbox_guest", "proceed", "",
				"sudo dmidecode -s system-product-name", "VirtualBox", "pass",
				"It seems that " + server + " isn't actually a VM.  This will cause a bunch of misconfigurations, please fix your config file."));
		
		units.addElement(new InstalledUnit("build_essential", "is_virtualbox_guest", "build-essential"));
		units.addElement(new InstalledUnit("linux_headers", "build_essential_installed", "linux-headers-$(uname -r)"));

		units.addElement(new SimpleUnit("guest_additions_installed", "linux_headers_installed",
				"sudo bash -c '"
								+ "mount /dev/sr1 /mnt;"
								+ "sh /mnt/VBoxLinuxAdditions.run --nox11;"
								+ "echo vboxsf >> /etc/initramfs-tools/modules;"
								+ "update-initramfs -u;"
				+ "'",
				"lsmod | grep vboxsf", "", "fail",
				"Couldn't get the VirtualBox additions to install/load.  This will stop external logging from working."));
		
		model.getServerModel(server).getUserModel().addUsername("vboxadd");
		model.getServerModel(server).getProcessModel().addProcess("\\[iprt-VBoxWQueue\\]$");
		model.getServerModel(server).getProcessModel().addProcess("/usr/sbin/VBoxService --pidfile /var/run/vboxadd-service.sh$");
		
		return units;
	}

	protected Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();

		units.addElement(new SimpleUnit("data_drive_is_partitioned", "proceed",
				"(\n"
					+ "	echo o\n" // Create a new empty DOS partition table
					+ "	echo n\n" // Add a new partition
					+ "	echo p\n" // Primary partition
					+ "	echo 1\n" // Partition number
					+ "	echo  \n" // First sector (Accept default: 1)
					+ "	echo  \n" // Last sector (Accept default: varies)
					+ "	echo w\n" // Write changes
				+ ") | sudo fdisk /dev/sdb;"
				+ "sudo mkfs.ext4 /dev/sdb1;",
				"sudo fdisk -l | grep '/dev/sdb1'", "", "fail",
				"Couldn't correctly partition the data disk.  This will cause a whole bunch of errors in further config."));
		
		units.addElement(new FileAppendUnit("data_drive_fstab", "data_drive_is_partitioned", "/dev/sdb1 /media/metaldata   ext4   defaults 0 0", "/etc/fstab",
				"Couldn't create the mount for the data disk at /media/metaldata.  This will cause a whole bunch of errors in further config."));
		
		//Mount /media/metaldata
		units.addElement(new DirUnit("metaldata_bindpoint", "is_virtualbox_guest", "/media/metaldata"));
		units.addElement(new DirMountedUnit("metaldata", "is_virtualbox_guest", "/media/metaldata",
				"Couldn't mount the data disk at /media/metaldata.  This will cause a whole bunch of errors in further config."));

		//"mount | grep 'data on /media/metaldata type vboxsf (rw,nodev,relatime,_netdev)'", "", "fail"));
		
		//Create /media/data bindfs point
		units.addElement(new DirUnit("data_dir_exists", "is_virtualbox_guest", "/media/data/"));
		
		//Mount /media/backup
		units.addElement(new FileAppendUnit("backup_fstab", "is_virtualbox_guest", "backup    /media/backup      vboxsf defaults,_netdev 0 0", "/etc/fstab",
				"Couldn't create the mount for the backup at /media/backup.  Meh."));
		units.addElement(new DirUnit("backup_bindpoint", "is_virtualbox_guest", "/media/backup"));
		units.addElement(new DirMountedUnit("backup", "backup_fstab_appended", "backup",
				"Couldn't mount the backup directory.  This isn't a problem, and this functionality should probably be scrubbed."));
		
		//Mount /log
		units.addElement(new FileAppendUnit("log_fstab", "is_virtualbox_guest", "log       /var/log           vboxsf defaults,dmode=751,_netdev 0 0", "/etc/fstab",
				"Couldn't create the mount for /var/log.  Meh."));
		units.addElement(new SimpleUnit("log_mounted", "log_fstab_appended",
				"sudo mkdir /tmp/log;"
				+ "sudo mv /var/log/* /tmp/log;"
				+ "sudo mount log;"
				+ "sudo mv /tmp/log/* /var/log;",
				"mount | grep 'log on /var/log' 2>&1", "log on /var/log type vboxsf (rw,nodev,relatime,_netdev)", "pass",
				"Couldn't move & remount the logs.  This is usually caused by logs already being in the hypervisor, on the first config of a service.  This can be fixed by rebooting the service (though you will lose any logs from the installation)"));

		units.addElement(model.getServerModel(server).getInterfaceModel().addIface(server.replace("-", "_") + "_primary_iface",
																				   "static",
																				   model.getData().getIface(server),
																				   null,
																				   model.getServerModel(server).getIP(),
																				   model.getData().getNetmask(),
																				   null,
																				   model.getServerModel(server).getGateway()));

		return units;
	}
	
	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		model.getServerModel(server).addRouterPoison(server, model, "cdn.debian.net", "130.89.148.14", new String[] {"80"});
		model.getServerModel(server).addRouterPoison(server, model, "security-cdn.debian.org", "151.101.0.204", new String[] {"80"});
		model.getServerModel(server).addRouterPoison(server, model, "prod.debian.map.fastly.net", "151.101.36.204", new String[] {"80"});
		
		
		
		model.getServerModel(server).addRouterFirewallRule(server, model, "virtualbox", "download.virtualbox.org", new String[]{"80"});
		//model.getServerModel(server).addRouterFirewallRule(server, model, "debian_cdn", "cdn.debian.net", new String[]{"80"});
		//model.getServerModel(server).addRouterFirewallRule(server, model, "debian_security_cdn", "security-cdn.debian.org", new String[]{"80"});
		
		return units;
	}
}