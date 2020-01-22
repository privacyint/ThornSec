/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.stack;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JOptionPane;

import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.network.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.DirOwnUnit;
import core.unit.fs.DirPermsUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileOwnUnit;
import core.unit.fs.FilePermsUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;

public class Virtualisation extends AStructuredProfile {

	public Virtualisation(String label, NetworkModel networkModel) {
		super(label, networkModel);
	}

	@Override
	public Collection<IUnit> getInstalled() {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("build_essential", "proceed", "build-essential"));
		units.add(new InstalledUnit("linux_headers", "build_essential_installed", "linux-headers-$(uname -r)"));

		units.add(new InstalledUnit("metal_virtualbox", "a2f683c52980aecf_pgp", "virtualbox-6.1"));
		units.add(new InstalledUnit("metal_genisoimage", "proceed", "genisoimage"));
		units.add(new InstalledUnit("metal_rsync", "proceed", "rsync"));
		units.add(new InstalledUnit("metal_guestfs_utils", "proceed", "libguestfs-tools"));

		return units;
	}

	@Override
	protected final Collection<IUnit> getPersistentConfig() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		getNetworkModel().getServerModel(getLabel()).addProcessString("/usr/lib/virtualbox/VBoxXPCOMIPCD$");
		getNetworkModel().getServerModel(getLabel()).addProcessString("/usr/lib/virtualbox/VBoxSVC --auto-shutdown$");
		getNetworkModel().getServerModel(getLabel()).addProcessString("\\[iprt-VBoxWQueue\\]$");
		getNetworkModel().getServerModel(getLabel()).addProcessString("\\[iprt-VBoxTscThr\\]$");
		getNetworkModel().getServerModel(getLabel()).addProcessString("\\[kvm-irqfd-clean\\]$");

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		getNetworkModel().getServerModel(getLabel()).getAptSourcesModel().addAptSource("virtualbox", "deb http://download.virtualbox.org/virtualbox/debian buster contrib", "keyserver.ubuntu.com", "a2f683c52980aecf");
		getNetworkModel().getServerModel(getLabel()).addEgress("download.virtualbox.org:80");

		return units;
	}

	Collection<IUnit> buildIso(String service, String preseed) throws InvalidServerException {

		final Collection<IUnit> units = new ArrayList<>();

		final String isoDir = getNetworkModel().getData().getHypervisorThornsecBase(getLabel()) + "/isos/" + service
				+ "/";

		String filename = null;
		String cleanedFilename = null;

		try {
			filename = Paths.get(new URI(getNetworkModel().getData().getDebianIsoUrl(service)).getPath()).getFileName()
					.toString();
			cleanedFilename = filename.replaceAll("[^A-Za-z0-9]", "_");
		} catch (final Exception e) {
			JOptionPane.showMessageDialog(null, "You shouldn't have been able to arrive here. Well done!");
			System.exit(1);
		}

		units.add(new DirUnit("iso_dir_" + service, "proceed", isoDir));
		units.add(new FileUnit("preseed_" + service, "debian_netinst_iso_" + cleanedFilename + "_downloaded", preseed,
				isoDir + "preseed.cfg"));
		units.add(new FileOwnUnit("preseed_" + service, "preseed_" + service, isoDir + "preseed.cfg", "root"));
		units.add(new FilePermsUnit("preseed_" + service, "preseed_" + service + "_chowned", isoDir + "preseed.cfg",
				"700"));

		String buildIso = "";
		buildIso += "sudo bash -c '";
		// Create a working copy of the iso for preseeding
		buildIso += " cd " + isoDir + ";";
		buildIso += " mkdir loopdir;";
		buildIso += " mount -o loop " + getNetworkModel().getData().getHypervisorThornsecBase(getLabel()) + "/"
				+ filename + " loopdir;";
		buildIso += " mkdir cd;";
		buildIso += " rsync -a -H --exclude=TRANS.TBL loopdir/ cd;";
		buildIso += " umount loopdir;";
		buildIso += " cd cd;";
		// Copy our preseed over to the working copy
		buildIso += " cp ../preseed.cfg .;";
		// Set the menu timeout to 1 second, otherwise it waits for user input
		buildIso += " sed -i \"s/timeout 0/timeout 1/g\" isolinux/isolinux.cfg;";
		// Switch off graphical menu
		buildIso += " sed -i \"s/^default/#default/g\" isolinux/isolinux.cfg;";
		// Append the preseed to the boot line
		buildIso += " sed -i \"s_append_append file=/cdrom/preseed.cfg auto=true_g\" isolinux/gtk.cfg;";
		// Switch off vga and add console
		buildIso += " sed -i \"s_vga=788_vga=none console=ttyS0,115200n8_g\" isolinux/gtk.cfg;";
		// Point at non-graphical installer
		buildIso += " sed -i \"s_/install.amd/gtk/initrd.gz_/install.amd/initrd.gz_g\" isolinux/gtk.cfg;";
		// Redirect output to console
		buildIso += " sed -i \"s_quiet_console=ttyS0,115200n8_g\" isolinux/gtk.cfg;";
		// Rebuild md5sums to reflect changes
		buildIso += " md5sum `find -follow -type f` > md5sum.txt;";
		buildIso += "' > /dev/null 2>&1;";
		// Create our new preseeded image
		buildIso += "sudo bash -c '";
		buildIso += "cd " + isoDir + ";";
		buildIso += " genisoimage -o " + service
				+ ".iso -r -J -no-emul-boot -boot-load-size 4 -boot-info-table -b isolinux/isolinux.bin -c isolinux/boot.cat ./cd;";
		buildIso += " rm -R cd loopdir;";
		buildIso += "'";

		units.add(new SimpleUnit("build_iso_" + service, "debian_netinst_iso_" + cleanedFilename + "_downloaded",
				buildIso, "test -f " + isoDir + service + ".iso && echo 'pass' || echo 'fail'", "pass", "pass",
				"Couldn't create the install ISO for " + service + ".  This service won't be able to install."));

		return units;
	}

	public Collection<IUnit> buildServiceVm(String service, String bridge)
			throws InvalidServerModelException, InvalidServerException {
		final File baseDir = getNetworkModel().getData().getHypervisorThornsecBase(getLabel());

		// Disks
		final String diskExtension = "vmdk";
		final String disksDir = baseDir + "/disks";
		final String bootDiskDir = disksDir + "/boot/" + service;
		final String bootDiskImg = bootDiskDir + "/" + service + "_boot.";
		final String bootLoopbackDir = bootDiskDir + "/live";
		final String dataDiskDir = disksDir + "/data/" + service;
		final String dataDiskImg = dataDiskDir + "/" + service + "_data.";
		final String dataLoopbackDir = dataDiskDir + "/live";

		final String logDir = baseDir + "/logs/" + service;
		final String backupTargetDir = baseDir + "/backups/" + service;
		final String ttySocketDir = baseDir + "/sockets/" + service;

		final String installIso = baseDir + "/isos/" + service + "/" + service + ".iso";
		final String user = "vboxuser_" + service;
		final String group = "vboxusers";
		final String osType = getNetworkModel().getData().getDebianIsoUrl(service).contains("amd64") ? "Debian_64"
				: "Debian";

		final Collection<IUnit> units = new ArrayList<>();

		// Metal user setup
		units.add(new SimpleUnit("metal_virtualbox_" + service + "_user", "metal_virtualbox_installed",
				"sudo adduser " + user + " --system --shell=/bin/false --disabled-login --ingroup " + group,
				"id -u " + user + " 2>&1 | grep 'no such user'", "", "pass", "Couldn't create the user for " + service
						+ " on its metal.  This is fatal, " + service + " will not be installed."));

		// Metal storage setup
		units.add(new DirUnit("boot_disk_dir_" + service, "proceed", bootDiskDir));
		units.add(new DirOwnUnit("boot_disk_dir_" + service, "boot_disk_dir_" + service + "_created", bootDiskDir, user,
				group));
		units.add(new DirPermsUnit("boot_disk_dir_" + service, "boot_disk_dir_" + service + "_chowned", bootDiskDir,
				"750"));

		units.add(new DirUnit("data_disk_dir_" + service, "proceed", dataDiskDir));
		units.add(new DirOwnUnit("data_disk_dir_" + service, "data_disk_dir_" + service + "_created", dataDiskDir, user,
				group));
		units.add(new DirPermsUnit("data_disk_dir_" + service, "data_disk_dir_" + service + "_chowned", dataDiskDir,
				"750"));

		units.add(new DirUnit("log_dir_" + service, "proceed", logDir));
		units.add(new DirOwnUnit("log_dir_" + service, "log_dir_" + service + "_created", logDir, user, group));
		units.add(new DirPermsUnit("log_dir_" + service, "log_dir_" + service + "_chowned", logDir, "750"));

		units.add(new DirUnit("backup_dir_" + service, "proceed", backupTargetDir));
		units.add(new DirOwnUnit("backup_dir_" + service, "backup_dir_" + service + "_created", backupTargetDir, user,
				group));
		units.add(new DirPermsUnit("backup_dir_" + service, "backup_dir_" + service + "_chowned", backupTargetDir,
				"750"));
		// Mark the backup destination directory as a valid destination
		units.add(new FileUnit(service + "_mark_backup_dir", "backup_dir_" + service + "_chmoded",
				"In memoriam Luke and Guy.  Miss you two!", backupTargetDir + "/backup.marker"));

		units.add(new DirUnit("socket_dir_" + service, "proceed", ttySocketDir));
		units.add(new DirOwnUnit("socket_dir_" + service, "socket_dir_" + service + "_created", ttySocketDir, user,
				group));
		units.add(new DirPermsUnit("socket_dir_" + service, "socket_dir_" + service + "_chowned", ttySocketDir, "750"));

		// Create the mount point for the boot disk
		units.add(new DirUnit("boot_disk_loopback_dir_" + service, "proceed", bootLoopbackDir + "/"));
		// units.add(new DirOwnUnit("boot_disk_loopback_dir_" + service,
		// "boot_disk_loopback_dir_" + service + "_created", bootLoopbackDir, "root",
		// "root"));
		// units.add(new DirPermsUnit("boot_disk_loopback_dir_" + service,
		// "boot_disk_loopback_dir_" + service + "_chowned", bootLoopbackDir, "755"));
		// And, more importantly, the data disk
		units.add(new DirUnit("data_disk_loopback_dir_" + service, "proceed", dataLoopbackDir + "/"));
		// units.add(new DirOwnUnit("data_disk_loopback_dir_" + service,
		// "data_disk_loopback_dir_" + service + "_created", dataLoopbackDir, "root",
		// "root"));
		// units.add(new DirPermsUnit("data_disk_loopback_dir_" + service,
		// "data_disk_loopback_dir_" + service + "_chowned", dataLoopbackDir, "755"));

		// VM setup
		units.add(new SimpleUnit(service + "_exists", "boot_disk_dir_" + service + "_chmoded",
				"sudo -u " + user + " VBoxManage createvm --name " + service + " --ostype \"" + osType
						+ "\" --register;" + "sudo -u " + user + " VBoxManage modifyvm " + service + " --description "
						+ "\"" + service + "." + getNetworkModel().getData().getDomain() + "\n"
						+ "ThornSec guest machine\n" + "Built with profile(s): "
						+ String.join(", ", getNetworkModel().getData().getProfiles(service)) + "\n"
						+ "Built at $(date)" + "\"",
				"sudo -u " + user + " VBoxManage list vms | grep " + service, "", "fail", "Couldn't create " + service
						+ " on its metal.  This is fatal, " + service + " will not be installed."));

		// HDD creation
		// TODO: iterate through the disks properly
//		units.add(new SimpleUnit(service + "_boot_disk", "boot_disk_dir_" + service + "_chmoded",
//				"sudo -u " + user + " VBoxManage createmedium --filename " + bootDiskImg + diskExtension + " --size "
//						+ getNetworkModel().getData().getBootDiskSize(service) + " --format VMDK",
//				"sudo [ -f " + bootDiskImg + diskExtension + " ] && echo pass;", "pass", "pass",
//				"Couldn't create the disk for " + service + "'s base filesystem.  This is fatal."));
//		units.add(new FileOwnUnit(service + "_boot_disk", service + "_boot_disk", bootDiskImg + diskExtension, user,
//				group));
//
//		units.add(new SimpleUnit(service + "_data_disk", "data_disk_dir_" + service + "_chmoded",
//				"sudo -u " + user + " VBoxManage createmedium --filename " + dataDiskImg + diskExtension + " --size "
//						+ getNetworkModel().getData().getDataDiskSize(service) + " --format VMDK",
//				"sudo [ -f " + dataDiskImg + diskExtension + " ] && echo pass;", "pass", "pass",
//				"Couldn't create the disk for " + service + "'s data.  This is fatal."));
//		units.add(new FileOwnUnit(service + "_data_disk", service + "_data_disk", dataDiskImg + diskExtension, user,
//				group));

		// Disk controller setup
		units.add(new SimpleUnit(service + "_sas_controller", service + "_exists",
				"sudo -u " + user + " VBoxManage storagectl " + service
						+ " --name \"SAS\" --add sas --controller LSILogicSAS --portcount 5 --hostiocache off",
				"sudo -u " + user + " VBoxManage showvminfo " + service
						+ " --machinereadable | grep ^storagecontrollername0=",
				"storagecontrollername0=\\\"SAS\\\"", "pass",
				"The SAS controller for " + service + " (where its disks are attached) Couldn't be created/attached to "
						+ service + ".  This is fatal, " + service + " will not be installed."));

		units.add(new SimpleUnit(service + "_boot_disk_attached", service + "_sas_controller",
				"sudo -u " + user + " VBoxManage storageattach " + service
						+ " --storagectl \"SAS\" --port 0 --device 0 --type hdd --medium " + bootDiskImg + diskExtension
						+ " --comment \\\"" + service + "BootDisk\\\"",
				"sudo -u " + user + " VBoxManage showvminfo " + service + " --machinereadable | grep \"SAS-0-0\"",
				"\\\"SAS-0-0\\\"=\\\"" + bootDiskImg + diskExtension + "\\\"", "pass",
				"Couldn't attach the disk for " + service + "'s base filesystem.  This is fatal."));

		units.add(new SimpleUnit(service + "_data_disk_attached", service + "_sas_controller",
				"sudo -u " + user + " VBoxManage storageattach " + service
						+ " --storagectl \"SAS\" --port 1 --device 0 --type hdd --medium " + dataDiskImg + diskExtension
						+ " --comment \\\"" + service + "DataDisk\\\"",
				"sudo -u " + user + " VBoxManage showvminfo " + service + " --machinereadable | grep \"SAS-1-0\"",
				"\\\"SAS-1-0\\\"=\\\"" + dataDiskImg + diskExtension + "\\\"", "pass",
				"Couldn't attach the disk for " + service + "'s data.  This is fatal."));

		units.add(new SimpleUnit(service + "_install_iso_attached", service + "_sas_controller",
				"sudo -u " + user + " VBoxManage storageattach " + service
						+ " --storagectl \"SAS\" --port 2 --device 0 --type dvddrive --medium " + installIso,
				"sudo -u " + user + " VBoxManage showvminfo " + service + " --machinereadable | grep \"SAS-2-0\"",
				"\\\"SAS-2-0\\\"=\\\"" + installIso + "\\\"", "pass",
				"Couldn't attach the preseeded installation disk for " + service
						+ ".  This service will not be installed."));

		units.add(new SimpleUnit(service + "_guest_additions_iso_attached", service + "_sas_controller", "sudo -u "
				+ user + " VBoxManage storageattach " + service
				+ " --storagectl \"SAS\" --port 3 --device 0 --type dvddrive --medium /usr/share/virtualbox/VBoxGuestAdditions.iso",
				"sudo -u " + user + " VBoxManage showvminfo " + service + " --machinereadable | grep \"SAS-3-0\"",
				"\\\"SAS-3-0\\\"=\\\"/usr/share/virtualbox/VBoxGuestAdditions.iso\\\"", "pass",
				"Couldn't attach the VirtualBox Guest Additions disk for " + service
						+ ".  Logs will not be pushed out to the hypervisor as expected."));

		// Architecture setup
		units.add(modifyVm(service, user, "paravirtprovider", "kvm")); // Default, make it explicit
		units.add(modifyVm(service, user, "chipset", "ich9"));
		units.add(modifyVm(service, user, "ioapic", "on", "IO APIC couldn't be enabled for " + service
				+ ".  This is required for 64-bit installations, and for more than 1 virtual CPU in a service."));
		units.add(modifyVm(service, user, "hwvirtex", "on"));
		units.add(modifyVm(service, user, "pae", "on"));
		units.add(modifyVm(service, user, "cpus", getNetworkModel().getData().getCPUs(service)));

		// RAM setup
		units.add(modifyVm(service, user, "memory", getNetworkModel().getData().getRAM(service)));
		units.add(modifyVm(service, user, "vram", "16"));
		units.add(modifyVm(service, user, "nestedpaging", "on"));
		units.add(modifyVm(service, user, "largepages", "on"));

		// Boot setup - DVD is second to stop machines being wiped every time they're
		// brought up
		units.add(modifyVm(service, user, "boot1", "disk",
				"Couldn't set the boot order for " + service + ".  This may mean the service will not be installed.",
				service + "_sas_controller"));
		units.add(modifyVm(service, user, "boot2", "dvd",
				"Couldn't set the boot order for " + service + ".  This may mean the service will not be installed.",
				service + "_sas_controller"));

		// Audio setup (switch it off)
		units.add(modifyVm(service, user, "audio", "none"));

		// Use high precision event timers instead of legacy
		units.add(modifyVm(service, user, "hpet", "on"));

		// Shared folders setup
		units.add(new SimpleUnit(service + "_log_sf_attached", service + "_exists",
				"sudo -u " + user + " VBoxManage sharedfolder add " + service + " --name log --hostpath " + logDir + ";"
						+ "sudo -u " + user + " VBoxManage setextradata " + service
						+ " VBoxInternal1/SharedFoldersEnableSymlinksCreate/log 1",
				"sudo -u " + user + " VBoxManage showvminfo " + service
						+ " --machinereadable | grep SharedFolderPathMachineMapping1",
				"SharedFolderPathMachineMapping1=\\\"" + logDir + "\\\"", "pass",
				"Couldn't attach the logs folder to " + service + ".  This means logs will only exist in the VM."));

		units.add(new SimpleUnit(service + "_backup_sf_attached", service + "_exists",
				"sudo -u " + user + " VBoxManage sharedfolder add " + service + " --name backup --hostpath "
						+ backupTargetDir,
				"sudo -u " + user + " VBoxManage showvminfo " + service
						+ " --machinereadable | grep SharedFolderPathMachineMapping2",
				"SharedFolderPathMachineMapping2=\\\"" + backupTargetDir + "\\\"", "pass"));

		// Clock setup to try and stop drift between host and guest
		// https://www.virtualbox.org/manual/ch09.html#changetimesync
		units.add(guestPropertySet(service, user, "timesync-interval", "10000", "Couldn't sync the clock between "
				+ service + " and its metal.  You'll probably see some clock drift in " + service + " as a result."));
		units.add(guestPropertySet(service, user, "timesync-min-adjust", "100", "Couldn't sync the clock between "
				+ service + " and its metal.  You'll probably see some clock drift in " + service + " as a result."));
		units.add(guestPropertySet(service, user, "timesync-set-on-restore", "1", "Couldn't sync the clock between "
				+ service + " and its metal.  You'll probably see some clock drift in " + service + " as a result."));
		units.add(guestPropertySet(service, user, "timesync-set-threshold", "1000", "Couldn't sync the clock between "
				+ service + " and its metal.  You'll probably see some clock drift in " + service + " as a result."));

		// tty0 socket
		units.add(new SimpleUnit(service + "_tty0_com_port", service + "_exists",
				"sudo -u " + user + " VBoxManage modifyvm " + service + " --uart1 0x3F8 4",
				"sudo -u " + user + " VBoxManage showvminfo " + service + " --machinereadable | grep ^uart1=",
				"uart1=\\\"0x03f8,4\\\"", "pass"));

		units.add(new SimpleUnit(service + "_tty0_socket", service + "_tty0_com_port",
				"sudo -u " + user + " VBoxManage modifyvm " + service + " --uartmode1 server " + ttySocketDir
						+ "/vboxttyS0",
				"sudo -u " + user + " VBoxManage showvminfo " + service + " --machinereadable | grep ^uartmode1=",
				"uartmode1=\\\"server," + ttySocketDir + "/vboxttyS0\\\"", "pass"));

		units.add(new SimpleUnit(service + "_tty1_com_port", service + "_exists",
				"sudo -u " + user + " VBoxManage modifyvm " + service + " --uart2 0x2F8 3",
				"sudo -u " + user + " VBoxManage showvminfo " + service + " --machinereadable | grep ^uart2=",
				"uart2=\\\"0x02f8,3\\\"", "pass"));

		units.add(new SimpleUnit(service + "_tty1_socket", service + "_tty1_com_port",
				"sudo -u " + user + " VBoxManage modifyvm " + service + " --uartmode2 server " + ttySocketDir
						+ "/vboxttyS1",
				"sudo -u " + user + " VBoxManage showvminfo " + service + " --machinereadable | grep ^uartmode2=",
				"uartmode2=\\\"server," + ttySocketDir + "/vboxttyS1\\\"", "pass"));

		// Ready to go!
		// units.add(new SimpleUnit(service + "_running", service + "_exists",
		// "sudo -u " + user + " bash -c 'VBoxManage startvm " + service + " --type
		// headless'",
		// "sudo -u " + user + " bash -c 'VBoxManage list runningvms | grep " + service
		// + "'", "", "fail"));

		getNetworkModel().getServerModel(getLabel())
				.addProcessString("/usr/lib/virtualbox/VBoxHeadless --comment " + service + " --startvm `if id '" + user
						+ "' >/dev/null 2>&1; then sudo -u " + user + " bash -c 'VBoxManage list runningvms | grep "
						+ service + "' | awk '{ print $2 }' | tr -d '{}'; else echo ''; fi` --vrde config *$");
		getNetworkModel().getServerModel(getLabel()).addProcessString("awk \\{");
		getNetworkModel().getServerModel(getLabel()).addProcessString("tr -d \\{\\}$");
		getNetworkModel().getServerModel(getLabel()).getUserModel().addUsername(user);

		return units;
	}

	private SimpleUnit modifyVm(String service, String user, String setting, String value, String errorMsg,
			String prerequisite) {

		String check = "";

		// Integers aren't quoted...
		if (value.matches("-?(0|[1-9]\\d*)")) {
			check = setting + "=" + value;
		} else {
			check = setting + "=\\\"" + value + "\\\"";
		}

		return new SimpleUnit(service + "_" + setting + "_" + value, prerequisite,
				"sudo -u " + user + " VBoxManage modifyvm " + service + " --" + setting + " " + value,
				"sudo -u " + user + " VBoxManage showvminfo " + service + " --machinereadable | grep ^" + setting + "=",
				check, "pass", errorMsg);
	}

	private SimpleUnit modifyVm(String service, String user, String setting, String value, String errorMsg) {
		return modifyVm(service, user, setting, value, errorMsg, service + "_exists");
	}

	private SimpleUnit modifyVm(String service, String user, String setting, String value) {
		return modifyVm(service, user, setting, value, "Couldn't change " + setting + " to " + value);
	}

	private SimpleUnit modifyVm(String service, String user, String setting, Integer value) {
		return modifyVm(service, user, setting, value + "", "Couldn't change " + setting + " to " + value);
	}

	private SimpleUnit guestPropertySet(String service, String user, String property, String value, String errorMsg,
			String prerequisite) {
		return new SimpleUnit(service + "_" + property.replaceAll("-", "_") + "_" + value, prerequisite,
				"sudo -u " + user + " VBoxManage guestproperty set " + service
						+ " \"/VirtualBox/GuestAdd/VBoxService/--" + property + "\" " + value,
				"sudo -u " + user + " VBoxManage guestproperty enumerate " + service
						+ " | grep \"Name: /VirtualBox/GuestAdd/VBoxService/--" + property + ", value: " + value + "\"",
				"", "fail", errorMsg);
	}

	private SimpleUnit guestPropertySet(String service, String user, String property, String value, String errorMsg) {
		return guestPropertySet(service, user, property, value, errorMsg, service + "_exists");
	}
}
