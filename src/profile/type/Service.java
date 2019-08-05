/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.type;

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
import core.unit.pkg.RunningUnit;

/**
 * This is a Service, which represents a VM on a HyperVisor
 */
public class Service extends AStructuredProfile {

	public Service(String label, NetworkModel networkModel) {
		super(label, networkModel);
	}

	@Override
	protected Set<IUnit> getInstalled() throws InvalidServerModelException {
		final Set<IUnit> units = new HashSet<>();

		// First, we need to be sure we're actually in a VirtualBox guest, or the rest
		// of this is moot
		units.add(new SimpleUnit("is_virtualbox_guest", "proceed", "", "sudo dmidecode -s system-product-name",
				"VirtualBox", "pass", "It seems that " + getLabel()
						+ " isn't actually a VM.  This will cause a bunch of misconfigurations, please fix your config file."));

		units.add(new InstalledUnit("build_essential", "is_virtualbox_guest", "build-essential"));
		units.add(new InstalledUnit("linux_headers", "build_essential_installed", "linux-headers-$(uname -r)"));

		units.add(new SimpleUnit("guest_additions_installed", "linux_headers_installed",
				"sudo bash -c '" + "mount /dev/sr1 /mnt;" + "sh /mnt/VBoxLinuxAdditions.run --nox11;"
						+ "echo vboxsf >> /etc/initramfs-tools/modules;" + "update-initramfs -u;" + "'",
				"lsmod | grep vboxsf", "", "fail",
				"Couldn't get the VirtualBox additions to install/load.  This will stop external logging from working."));

		units.add(new SimpleUnit("guest_additions_are_latest", "guest_additions_installed",
				"sudo mount /dev/sr1 /mnt;" + "sudo /mnt/VBoxLinuxAdditions.run;" + "sudo umount /mnt;",
				"sudo mount /dev/sr1 /mnt &>/dev/null;grep -a 'INSTALLATION_VER=' /mnt/VBoxLinuxAdditions.run | tr -d \"\\\"[A-Z]\\=_\";sudo umount /mnt &>/dev/null;",
				// This is the currently running version, which isn't useful if it has already
				// been updated pending reboot
				// "lsmod | grep -io vboxguest | xargs sudo modinfo | grep -iw version | awk '{
				// print $2 }'",
				"$(ls /opt | tr -d \\\"[A-Za-z\\-]\\\";)", "pass",
				"This server is running an outdated version of the guest additions.  If you're running a configuration, this can be fixed by restarting the VM."));

		getNetworkModel().getServerModel(getLabel()).getUserModel().addUsername("vboxadd");
		getNetworkModel().getServerModel(getLabel()).addProcessString("\\[iprt-VBoxWQueue\\]$");
		getNetworkModel().getServerModel(getLabel())
				.addProcessString("/usr/sbin/VBoxService --pidfile /var/run/vboxadd-service.sh$");

		// haveged is not perfect, but according to
		// https://security.stackexchange.com/questions/34523/is-it-appropriate-to-use-haveged-as-a-source-of-entropy-on-virtual-machines
		// is OK for producing entropy in VMs.
		// It is also recommended by novell for VM entropy
		// http://www.novell.com/support/kb/doc.php?id=7011351
		units.add(new InstalledUnit("entropy_generator", "proceed", "haveged"));
		units.add(new RunningUnit("entropy_generator", "haveged", "haveged"));
		// Block until we have enough entropy to continue
		units.add(new SimpleUnit("enough_entropy_available", "entropy_generator_installed",
				"while [ $(cat /proc/sys/kernel/random/entropy_avail) -le 600 ]; do sleep 2; done;",
				"(( $(cat /proc/sys/kernel/random/entropy_avail) > 600 )) && echo pass || echo fail", "pass", "pass"));

		getNetworkModel().getServerModel(getLabel())
				.addProcessString("/usr/sbin/haveged --Foreground --verbose=1 -w 1024$");

		return units;
	}

	@Override
	protected Set<IUnit> getPersistentConfig() {
		final Set<IUnit> units = new HashSet<>();

		units.add(new SimpleUnit("data_drive_is_partitioned", "proceed", "(\n" + "	echo o\n" // Create a new empty DOS
																								// partition table
				+ "	echo n\n" // Add a new partition
				+ "	echo p\n" // Primary partition
				+ "	echo 1\n" // Partition number
				+ "	echo  \n" // First sector (Accept default: 1)
				+ "	echo  \n" // Last sector (Accept default: varies)
				+ "	echo w\n" // Write changes
				+ ") | sudo fdisk /dev/sdb;" + "sudo mkfs.ext4 /dev/sdb1;", "sudo fdisk -l | grep '/dev/sdb1'", "",
				"fail",
				"Couldn't correctly partition the data disk.  This will cause a whole bunch of errors in further config."));

		units.add(new FileAppendUnit("data_drive_fstab", "data_drive_is_partitioned",
				"/dev/sdb1 /media/metaldata   ext4   defaults 0 0", "/etc/fstab",
				"Couldn't create the mount for the data disk at /media/metaldata.  This will cause a whole bunch of errors in further config."));

		// Mount /media/metaldata
		units.add(new DirUnit("metaldata_bindpoint", "is_virtualbox_guest", "/media/metaldata"));
		units.add(new DirMountedUnit("metaldata", "is_virtualbox_guest", "/media/metaldata",
				"Couldn't mount the data disk at /media/metaldata.  This will cause a whole bunch of errors in further config."));

		// "mount | grep 'data on /media/metaldata type vboxsf
		// (rw,nodev,relatime,_netdev)'", "", "fail"));

		// Create /media/data bindfs point
		units.add(new DirUnit("data_dir_exists", "is_virtualbox_guest", "/media/data/"));

		// Mount /media/backup
		units.add(new FileAppendUnit("backup_fstab", "is_virtualbox_guest",
				"backup    /media/backup      vboxsf defaults,_netdev,ro 0 0", "/etc/fstab",
				"Couldn't create the mount for the backup at /media/backup.  Meh."));
		units.add(new DirUnit("backup_bindpoint", "is_virtualbox_guest", "/media/backup"));
		units.add(new DirMountedUnit("backup", "backup_fstab_appended", "backup",
				"Couldn't mount the backup directory."));

		// Mount /log
		units.add(new FileAppendUnit("log_fstab", "is_virtualbox_guest",
				"log       /var/log           vboxsf defaults,dmode=751,_netdev 0 0", "/etc/fstab",
				"Couldn't create the mount for /var/log.  Meh."));
		units.add(new SimpleUnit("log_mounted", "log_fstab_appended",
				"sudo mkdir /tmp/log;" + "sudo mv /var/log/* /tmp/log;" + "sudo mount log;"
						+ "sudo mv /tmp/log/* /var/log;",
				"mount | grep 'log on /var/log' 2>&1", "", "fail",
				"Couldn't move & remount the logs.  This is usually caused by logs already being in the hypervisor, on the first config of a service.  This can be fixed by rebooting the service (though you will lose any logs from the installation)"));

		return units;
	}

	@Override
	public Set<IUnit> getPersistentFirewall() throws InvalidServerModelException {
		final Set<IUnit> units = new HashSet<>();

		getNetworkModel().getServerModel(getLabel()).addEgress("download.virtualbox.org");

		return units;
	}

	/*
	 * public Set<IUnit> getPersistentFirewall() { Set<IUnit> units = new
	 * HashSet<IUnit>();
	 *
	 * String metal = networkModel.getData().getMetal(getLabel());
	 *
	 *
	 * Hashtable<String, InterfaceData> lanIfaces =
	 * networkModel.getData().getLanIfaces(getLabel());
	 *
	 * if (lanIfaces.isEmpty()) { //That's OK, we'll generate one here
	 * lanIfaces.put("enp0s17", null); }
	 *
	 * int i = 0;
	 *
	 * for (Map.Entry<String, String> lanIface : lanIfaces.entrySet() ) {
	 * NetworkInterfaceModel im = me.getLANInterfaces();
	 *
	 * InetAddress subnet = networkModel.stringToIP(me.getFirstOctet() + "." +
	 * me.getSecondOctet() + "." + me.getThirdOctet() + "." + (i * 4)); InetAddress
	 * router = networkModel.stringToIP(me.getFirstOctet() + "." +
	 * me.getSecondOctet() + "." + me.getThirdOctet() + "." + ((i * 4) + 1));
	 * InetAddress address = networkModel.stringToIP(me.getFirstOctet() + "." +
	 * me.getSecondOctet() + "." + me.getThirdOctet() + "." + ((i * 4) + 2));
	 * InetAddress broadcast = networkModel.stringToIP(me.getFirstOctet() + "." +
	 * me.getSecondOctet() + "." + me.getThirdOctet() + "." + ((i * 4) + 3));
	 * InetAddress netmask = networkModel.getData().getNetmask();
	 *
	 * String mac = lanIface.getValue(); if (mac == null || mac.equals("")) { mac =
	 * "08:00:27:"; mac += String.format("%02x", me.getSecondOctet()) + ":"; mac +=
	 * String.format("%02x", me.getThirdOctet()) + ":"; mac += String.format("%02x",
	 * i); }
	 *
	 * im.addIface(new InterfaceData( getLabel(), lanIface.getKey(), mac, "static",
	 * null, subnet, address, netmask, broadcast, router, "comment goes here") );
	 *
	 * ++i; }
	 *
	 * networkModel.getServerModel(getLabel()).addEgressDestination(
	 * "download.virtualbox.org");
	 *
	 * return units; }
	 */
}
