package profile;

import java.net.URI;
import java.nio.file.Paths;
import java.util.Vector;

import javax.swing.JOptionPane;

import core.data.InterfaceData;
import core.iface.IUnit;
import core.model.NetworkModel;
import core.model.ServerModel;
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
	
	public Virtualisation(ServerModel me, NetworkModel networkModel) {
		super("virtualisation", me, networkModel);

		me.getAptSourcesModel().addAptSource("virtualbox", "proceed", "deb http://download.virtualbox.org/virtualbox/debian stretch contrib", "keyserver.ubuntu.com", "0xa2f683c52980aecf");
	
		me.getProcessModel().addProcess("/usr/lib/virtualbox/VBoxXPCOMIPCD$");
		me.getProcessModel().addProcess("/usr/lib/virtualbox/VBoxSVC --auto-shutdown$");
		me.getProcessModel().addProcess("\\[iprt-VBoxWQueue\\]$");
		me.getProcessModel().addProcess("\\[iprt-VBoxTscThr\\]$");
		me.getProcessModel().addProcess("\\[kvm-irqfd-clean\\]$");
	}
	
	protected Vector<IUnit> getInstalled() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new InstalledUnit("build_essential", "build-essential"));
		units.addElement(new InstalledUnit("linux_headers", "build_essential_installed", "linux-headers-$(uname -r)"));
		
		units.addElement(new InstalledUnit("metal_virtualbox", "virtualbox_pgp", "virtualbox-6.0"));
		units.addElement(new InstalledUnit("metal_genisoimage", "genisoimage"));
		units.addElement(new InstalledUnit("metal_rsync", "rsync"));
		units.addElement(new InstalledUnit("metal_guestfs_utils", "libguestfs-tools"));

		return units;
	}

	protected Vector<IUnit> getLiveConfig() {
		Vector<IUnit> units = new Vector<IUnit>();


		
		return units;
	}
	
	public Vector<IUnit> getNetworking() {
		Vector<IUnit> units = new Vector<IUnit>();

		me.addRequiredEgress("download.virtualbox.org", new Integer[]{80});

		return units;
	}
	
	String preseed(String service, Boolean expirePasswords) {

		String user            = networkModel.getData().getUser();
		String sshDir          = "/home/" + user + "/.ssh";
		String pubKey          = networkModel.getData().getUserSSHKey(user);
		String hostname        = networkModel.getData().getHostname(service);
		String domain          = networkModel.getData().getDomain(service);
		String fullName        = networkModel.getData().getUserFullName(user);
		String debianMirror    = networkModel.getData().getDebianMirror(service);
		String debianDirectory = networkModel.getData().getDebianDirectory(service);

		String preseed = "";
		//Set up new box before rebooting. Sometimes you need to echo out in chroot;
		//in-target just doesn't work reliably for many things (most likely due to the shell it uses) :(
		preseed += "d-i preseed/late_command string";
		//Echo out public keys, make sure it's all secured properly
		preseed += "	in-target mkdir " + sshDir + ";";
		preseed += "    in-target touch " + sshDir + "/authorized_keys;";
		preseed += "	echo \\\"echo '" + pubKey + "' >> " + sshDir + "/authorized_keys; \\\" | chroot /target /bin/bash;";
		
		preseed += "	in-target chmod 700 " + sshDir + ";";
		preseed += "	in-target chmod 400 " + sshDir + "/authorized_keys;";
		preseed += "	in-target chown -R " + user + ":" + user + " " + sshDir + ";";
		
		if (expirePasswords) {
			//Force the user to change their passphrase on first login if they haven't set a passwd
			preseed += "	in-target passwd -e " + user + ";";
		}
		
		//Lock the root account
		preseed += "	in-target passwd -l root;";
		
		//Change the SSHD to be on the expected port
		preseed += "    in-target sed -i 's/#Port 22/Port " + networkModel.getData().getSSHPort(service) + "/g' /etc/ssh/sshd_config;";
		
		//Debian installer options.
		preseed += "\n";
		preseed += "d-i debian-installer/locale string en_US\n";
		preseed += "d-i keyboard-configuration/xkb-keymap select us\n";
		preseed += "d-i clock-setup/ntp boolean false\n";
		preseed += "d-i netcfg/target_network_config select ifupdown\n";
		preseed += "d-i netcfg/choose_interface select auto\n";
		preseed += "d-i netcfg/get_hostname string " + hostname + "\n";
		preseed += "d-i netcfg/get_domain string " + domain + "\n";
		preseed += "d-i netcfg/hostname string " + hostname + "\n";
		preseed += "d-i mirror/country string manual\n";
		preseed += "d-i mirror/http/hostname string " + debianMirror + "\n";
		preseed += "d-i mirror/http/directory string " + debianDirectory + "\n";
		preseed += "d-i mirror/http/proxy string\n";
		preseed += "d-i passwd/root-password-crypted password ${" + service.toUpperCase() + "_PASSWORD}\n";
		preseed += "d-i passwd/user-fullname string " + fullName + "\n";
		preseed += "d-i passwd/username string " + user + "\n";
		preseed += "d-i passwd/user-password-crypted password ${" + service.toUpperCase() + "_PASSWORD}\n";
		preseed += "d-i passwd/user-default-groups string sudo\n";
		preseed += "d-i clock-setup/utc boolean true\n";
		preseed += "d-i time/zone string Europe/London\n";
		preseed += "d-i clock-setup/ntp boolean true\n";
		preseed += "d-i partman-auto/disk string /dev/sda\n";
		preseed += "d-i grub-installer/bootdev string /dev/sda\n";
		preseed += "d-i partman-auto/method string regular\n";
		preseed += "d-i partman-auto/choose_recipe select atomic\n";
		preseed += "d-i partman-partitioning/confirm_write_new_label boolean true\n";
		preseed += "d-i partman/choose_partition select finish\n";
		preseed += "d-i partman/confirm boolean true\n";
		preseed += "d-i partman/confirm_nooverwrite boolean true\n";
		preseed += "iptasksel tasksel/first multiselect none\n";
		preseed += "d-i apt-setup/cdrom/set-first boolean false\n";
		preseed += "d-i apt-setup/cdrom/set-next boolean false\n";
		preseed += "d-i apt-setup/cdrom/set-failed boolean false\n";
		preseed += "d-i pkgsel/include string sudo openssh-server dkms gcc bzip2\n";
		preseed += "openssh-server openssh-server/permit-root-login boolean false\n";
		preseed += "discover discover/install_hw_packages multiselect virtualbox-ose-guest-x11\n";
		preseed += "popularity-contest popularity-contest/participate boolean false\n";
		preseed += "d-i grub-installer/only_debian boolean true\n";
		preseed += "d-i grub-installer/with_other_os boolean true\n";
		preseed += "d-i grub-installer/bootdev string default\n";
		preseed += "d-i finish-install/reboot_in_progress note";

		return preseed;
	}

	Vector<IUnit> buildIso(String service, String preseed) {

		Vector<IUnit> units = new Vector<IUnit>();
		
		String isoDir =  networkModel.getData().getHypervisorThornsecBase(me.getLabel()) + "/isos/" + service + "/";

		String filename = null;
		String cleanedFilename = null;
		
		try {
			filename = Paths.get(new URI(networkModel.getData().getDebianIsoUrl(service)).getPath()).getFileName().toString();
			cleanedFilename = filename.replaceAll("[^A-Za-z0-9]", "_");
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(null, "You shouldn't have been able to arrive here. Well done!");
			System.exit(1);
		}
		
		units.addElement(new DirUnit("iso_dir_" + service, "proceed", isoDir));
		units.addElement(new FileUnit("preseed_" + service, "debian_netinst_iso_" + cleanedFilename + "_downloaded", preseed, isoDir + "preseed.cfg"));
		units.addElement(new FileOwnUnit("preseed_" + service, "preseed_" + service, isoDir + "preseed.cfg", "root"));
		units.addElement(new FilePermsUnit("preseed_" + service, "preseed_" + service + "_chowned", isoDir + "preseed.cfg", "700"));
		
		String buildIso = "";
		buildIso += "sudo bash -c '";
		//Create a working copy of the iso for preseeding
		buildIso += 	" cd " + isoDir + ";";
		buildIso += 	" mkdir loopdir;";
		buildIso += 	" mount -o loop " + networkModel.getData().getHypervisorThornsecBase(me.getLabel()) + "/" + filename + " loopdir;";
		buildIso += 	" mkdir cd;";
		buildIso += 	" rsync -a -H --exclude=TRANS.TBL loopdir/ cd;";
		buildIso += 	" umount loopdir;";
		buildIso += 	" cd cd;";
		//Copy our preseed over to the working copy
		buildIso += 	" cp ../preseed.cfg .;";
		//Set the menu timeout to 1 second, otherwise it waits for user input
		buildIso += 	" sed -i \"s/timeout 0/timeout 1/g\" isolinux/isolinux.cfg;";
		//Switch off graphical menu
		buildIso += 	" sed -i \"s/^default/#default/g\" isolinux/isolinux.cfg;";
		//Append the preseed to the boot line
		buildIso += 	" sed -i \"s_append_append file=/cdrom/preseed.cfg auto=true_g\" isolinux/gtk.cfg;";
		//Switch off vga and add console
		buildIso += 	" sed -i \"s_vga=788_vga=none console=ttyS0,115200n8_g\" isolinux/gtk.cfg;";
		//Point at non-graphical installer
		buildIso += 	" sed -i \"s_/install.amd/gtk/initrd.gz_/install.amd/initrd.gz_g\" isolinux/gtk.cfg;";
		//Redirect output to console
		buildIso += 	" sed -i \"s_quiet_console=ttyS0,115200n8_g\" isolinux/gtk.cfg;";
		//Rebuild md5sums to reflect changes
		buildIso += 	" md5sum `find -follow -type f` > md5sum.txt;";
		buildIso += "' > /dev/null 2>&1;";
		//Create our new preseeded image
		buildIso += "sudo bash -c '";
		buildIso +=		"cd " + isoDir + ";";
		buildIso += 	" genisoimage -o " + service + ".iso -r -J -no-emul-boot -boot-load-size 4 -boot-info-table -b isolinux/isolinux.bin -c isolinux/boot.cat ./cd;";
		buildIso += 	" rm -R cd loopdir;";
		buildIso += "'";
		
		units.addElement(new SimpleUnit("build_iso_" + service, "debian_netinst_iso_" + cleanedFilename + "_downloaded",
				buildIso,
				"test -f " + isoDir + service + ".iso && echo 'pass' || echo 'fail'", "pass", "pass",
				"Couldn't create the install ISO for " + service + ".  This service won't be able to install."));
		
		return units;
	}

	Vector<IUnit> buildServiceVm(String service, String bridge) {
		String baseDir = networkModel.getData().getHypervisorThornsecBase(me.getLabel());

		//Disks
		String diskExtension   = "vmdk";
		String disksDir        = baseDir + "/disks";
		String bootDiskDir     = disksDir + "/boot/" + service;
		String bootDiskImg     = bootDiskDir + "/" + service + "_boot.";
		String bootLoopbackDir = bootDiskDir + "/live";
		String dataDiskDir     = disksDir + "/data/" + service;
		String dataDiskImg     = dataDiskDir + "/" + service + "_data.";
		String dataLoopbackDir = dataDiskDir + "/live";
		
		String logDir          = baseDir + "/logs/" + service;
		String backupTargetDir = baseDir + "/backups/" + service;
		String ttySocketDir    = baseDir + "/sockets/" + service; 
		
		String installIso = baseDir + "/isos/" + service + "/" + service + ".iso";
		String user       = "vboxuser_" + service;
		String group      = "vboxusers";
		String osType     = networkModel.getData().getDebianIsoUrl(service).contains("amd64") ? "Debian_64" : "Debian";
		
		Vector<IUnit> units = new Vector<IUnit>();
		
		//Metal user setup
		units.addElement(new SimpleUnit("metal_virtualbox_" + service + "_user", "metal_virtualbox_installed",
				"sudo adduser " + user + " --system --shell=/bin/false --disabled-login --ingroup " + group,
				"id -u " + user + " 2>&1 | grep 'no such user'", "", "pass",
				"Couldn't create the user for " + service + " on its metal.  This is fatal, " + service + " will not be installed."));
		
		//Metal storage setup
		units.addElement(new DirUnit("boot_disk_dir_" + service, "proceed", bootDiskDir));
		units.addElement(new DirOwnUnit("boot_disk_dir_" + service, "boot_disk_dir_" + service + "_created", bootDiskDir, user, group));
		units.addElement(new DirPermsUnit("boot_disk_dir_" + service, "boot_disk_dir_" + service + "_chowned", bootDiskDir, "750"));
	
		units.addElement(new DirUnit("data_disk_dir_" + service, "proceed", dataDiskDir));
		units.addElement(new DirOwnUnit("data_disk_dir_" + service , "data_disk_dir_" + service + "_created", dataDiskDir, user, group));
		units.addElement(new DirPermsUnit("data_disk_dir_" + service, "data_disk_dir_" + service + "_chowned", dataDiskDir, "750"));

		units.addElement(new DirUnit("log_dir_" + service, "proceed", logDir));
		units.addElement(new DirOwnUnit("log_dir_" + service, "log_dir_" + service + "_created", logDir, user, group));
		units.addElement(new DirPermsUnit("log_dir_" + service, "log_dir_" + service + "_chowned", logDir, "750"));
		
		units.addElement(new DirUnit("backup_dir_" + service, "proceed", backupTargetDir));
		units.addElement(new DirOwnUnit("backup_dir_" + service, "backup_dir_" + service + "_created", backupTargetDir, user, group));
		units.addElement(new DirPermsUnit("backup_dir_" + service, "backup_dir_" + service + "_chowned", backupTargetDir, "750"));
		//Mark the backup destination directory as a valid destination
		units.addElement(new FileUnit(service + "_mark_backup_dir", "backup_dir_" + service + "_chmoded" , "In memoriam Luke and Guy.  Miss you two!", backupTargetDir + "/backup.marker"));
		
		units.addElement(new DirUnit("socket_dir_" + service, "proceed", ttySocketDir));
		units.addElement(new DirOwnUnit("socket_dir_" + service, "socket_dir_" + service + "_created", ttySocketDir, user, group));
		units.addElement(new DirPermsUnit("socket_dir_" + service, "socket_dir_" + service + "_chowned", ttySocketDir, "750"));
		
		//Create the mount point for the boot disk
		units.addElement(new DirUnit("boot_disk_loopback_dir_" + service, "proceed", bootLoopbackDir + "/"));
		//units.addElement(new DirOwnUnit("boot_disk_loopback_dir_" + service, "boot_disk_loopback_dir_" + service + "_created", bootLoopbackDir, "root", "root"));
		//units.addElement(new DirPermsUnit("boot_disk_loopback_dir_" + service, "boot_disk_loopback_dir_" + service + "_chowned", bootLoopbackDir, "755"));
		//And, more importantly, the data disk
		units.addElement(new DirUnit("data_disk_loopback_dir_" + service, "proceed", dataLoopbackDir + "/"));
		//units.addElement(new DirOwnUnit("data_disk_loopback_dir_" + service, "data_disk_loopback_dir_" + service + "_created", dataLoopbackDir, "root", "root"));
		//units.addElement(new DirPermsUnit("data_disk_loopback_dir_" + service, "data_disk_loopback_dir_" + service + "_chowned", dataLoopbackDir, "755"));
		
		//VM setup
		units.addElement(new SimpleUnit(service + "_exists", "boot_disk_dir_" + service + "_chmoded",
				"sudo -u " + user + " VBoxManage createvm --name " + service + " --ostype \"" + osType + "\" --register;"
				+ "sudo -u " + user + " VBoxManage modifyvm " + service + " --description "
				+ "\""
					+ service + "." + networkModel.getData().getDomain(service) + "\n"
					+ "ThornSec guest machine\n"
					+ "Built with profile(s): " + String.join(", ", networkModel.getServerModel(service).getProfiles()) + "\n"
					+ "Built at $(date)"
				+ "\"",
				"sudo -u " + user + " VBoxManage list vms | grep " + service, "", "fail",
				"Couldn't create " + service + " on its metal.  This is fatal, " + service + " will not be installed."));
		
		//HDD creation
		units.addElement(new SimpleUnit(service + "_boot_disk", "boot_disk_dir_" + service + "_chmoded",
				"sudo -u " + user + " VBoxManage createmedium --filename " + bootDiskImg + diskExtension + " --size " + networkModel.getData().getBootDiskSize(service) + " --format VMDK",
				"sudo [ -f " + bootDiskImg + diskExtension + " ] && echo pass;", "pass", "pass",
				"Couldn't create the disk for " + service + "'s base filesystem.  This is fatal."));
		units.addElement(new FileOwnUnit(service + "_boot_disk", service + "_boot_disk", bootDiskImg + diskExtension, user, group));
		
		units.addElement(new SimpleUnit(service + "_data_disk", "data_disk_dir_" + service + "_chmoded",
				"sudo -u " + user + " VBoxManage createmedium --filename " + dataDiskImg + diskExtension + " --size " + networkModel.getData().getDataDiskSize(service) + " --format VMDK",
				"sudo [ -f " + dataDiskImg + diskExtension + " ] && echo pass;", "pass", "pass",
				"Couldn't create the disk for " + service + "'s data.  This is fatal."));
		units.addElement(new FileOwnUnit(service + "_data_disk", service + "_data_disk", dataDiskImg + diskExtension, user, group));
		
		//Disk controller setup
		units.addElement(new SimpleUnit(service + "_sas_controller", service + "_exists",
				"sudo -u " + user + " VBoxManage storagectl " + service + " --name \"SAS\" --add sas --controller LSILogicSAS --portcount 5 --hostiocache off",
				"sudo -u " + user + " VBoxManage showvminfo " + service + " --machinereadable | grep ^storagecontrollername0=", "storagecontrollername0=\\\"SAS\\\"", "pass",
				"The SAS controller for " + service + " (where its disks are attached) Couldn't be created/attached to " + service + ".  This is fatal, " + service + " will not be installed."));

		units.addElement(new SimpleUnit(service + "_boot_disk_attached", service + "_sas_controller",
				"sudo -u " + user + " VBoxManage storageattach " + service + " --storagectl \"SAS\" --port 0 --device 0 --type hdd --medium " + bootDiskImg + diskExtension + " --comment \\\"" + service + "BootDisk\\\"",
				"sudo -u " + user + " VBoxManage showvminfo " + service + " --machinereadable | grep \"SAS-0-0\"", "\\\"SAS-0-0\\\"=\\\"" + bootDiskImg + diskExtension + "\\\"", "pass",
				"Couldn't attach the disk for " + service + "'s base filesystem.  This is fatal."));

		units.addElement(new SimpleUnit(service + "_data_disk_attached", service + "_sas_controller",
				"sudo -u " + user + " VBoxManage storageattach " + service + " --storagectl \"SAS\" --port 1 --device 0 --type hdd --medium " + dataDiskImg + diskExtension + " --comment \\\"" + service + "DataDisk\\\"",
				"sudo -u " + user + " VBoxManage showvminfo " + service + " --machinereadable | grep \"SAS-1-0\"", "\\\"SAS-1-0\\\"=\\\"" + dataDiskImg + diskExtension + "\\\"", "pass",
				"Couldn't attach the disk for " + service + "'s data.  This is fatal."));
		
		units.addElement(new SimpleUnit(service + "_install_iso_attached", service + "_sas_controller",
				"sudo -u " + user + " VBoxManage storageattach " + service + " --storagectl \"SAS\" --port 2 --device 0 --type dvddrive --medium " + installIso,
				"sudo -u " + user + " VBoxManage showvminfo " + service + " --machinereadable | grep \"SAS-2-0\"", "\\\"SAS-2-0\\\"=\\\"" + installIso + "\\\"", "pass",
				"Couldn't attach the preseeded installation disk for " + service + ".  This service will not be installed."));
		
		units.addElement(new SimpleUnit(service + "_guest_additions_iso_attached", service + "_sas_controller",
				"sudo -u " + user + " VBoxManage storageattach " + service + " --storagectl \"SAS\" --port 3 --device 0 --type dvddrive --medium /usr/share/virtualbox/VBoxGuestAdditions.iso",
				"sudo -u " + user + " VBoxManage showvminfo " + service + " --machinereadable | grep \"SAS-3-0\"", "\\\"SAS-3-0\\\"=\\\"/usr/share/virtualbox/VBoxGuestAdditions.iso\\\"", "pass",
				"Couldn't attach the VirtualBox Guest Additions disk for " + service + ".  Logs will not be pushed out to the hypervisor as expected."));
		
		//Architecture setup
		units.addElement(modifyVm(service, user, "paravirtprovider", "kvm")); //Default, make it explicit
		units.addElement(modifyVm(service, user, "chipset", "ich9"));
		units.addElement(modifyVm(service, user, "ioapic", "on", "IO APIC couldn't be enabled for " + service + ".  This is required for 64-bit installations, and for more than 1 virtual CPU in a service."));
		units.addElement(modifyVm(service, user, "hwvirtex", "on"));
		units.addElement(modifyVm(service, user, "pae", "on"));
		units.addElement(modifyVm(service, user, "cpus", networkModel.getData().getCpus(service)));

		//RAM setup
		units.addElement(modifyVm(service, user, "memory", networkModel.getData().getRam(service)));
		units.addElement(modifyVm(service, user, "vram", "16"));
		units.addElement(modifyVm(service, user, "nestedpaging", "on"));
		units.addElement(modifyVm(service, user, "largepages", "on"));

		//Boot setup - DVD is second to stop machines being wiped every time they're brought up
		units.addElement(modifyVm(service, user, "boot1", "disk", "Couldn't set the boot order for " + service + ".  This may mean the service will not be installed.", service + "_sas_controller"));
		units.addElement(modifyVm(service, user, "boot2", "dvd", "Couldn't set the boot order for " + service + ".  This may mean the service will not be installed.", service + "_sas_controller"));
		
		int i = 1;
		//Networking setup
		for (InterfaceData lanIface : networkModel.getMachineModel(service).getInterfaces()) { //networkModel.getData().getLanIfaces(service).entrySet() ) {
			units.addElement(modifyVm(service, user, "nic" + i, "bridged", "Couldn't give " + service + " a connection to the network.  This means the service will not be able to talk to the router or network, and will not be installed."));
			units.addElement(modifyVm(service, user, "bridgeadapter" + i, bridge, "Couldn't give " + service + " a connection to the network.  This means the service will not be able to talk to the router or network, and will not be installed.", service + "_nic1_bridged"));
			units.addElement(modifyVm(service, user, "nictype" + i, "82545EM", "Couldn't set " + service + "'s network adapter to use the 82545EM model.", service + "_bridgeadapter1_" + bridge));
			units.addElement(modifyVm(service, user, "macaddress" + i, lanIface.getMac().replace(":", "").toUpperCase(), "Couldn't set " + service + "'s MAC address.  This means the service will not be able to get an IP address, and will not be installed."));
			++i;
		}
		
		//Audio setup (switch it off)
		units.addElement(modifyVm(service, user, "audio", "none"));

		//Use high precision event timers instead of legacy
		units.addElement(modifyVm(service, user, "hpet", "on"));
		
		//Shared folders setup
		units.addElement(new SimpleUnit(service + "_log_sf_attached", service + "_exists",
				"sudo -u " + user + " VBoxManage sharedfolder add " + service + " --name log --hostpath " + logDir + ";"
				+ "sudo -u " + user + " VBoxManage setextradata " + service + " VBoxInternal1/SharedFoldersEnableSymlinksCreate/log 1",
				"sudo -u " + user + " VBoxManage showvminfo " + service + " --machinereadable | grep SharedFolderPathMachineMapping1", "SharedFolderPathMachineMapping1=\\\"" + logDir + "\\\"", "pass",
				"Couldn't attach the logs folder to " + service + ".  This means logs will only exist in the VM."));
		
		units.addElement(new SimpleUnit(service + "_backup_sf_attached", service + "_exists",
				"sudo -u " + user + " VBoxManage sharedfolder add " + service + " --name backup --hostpath " + backupTargetDir,
				"sudo -u " + user + " VBoxManage showvminfo " + service + " --machinereadable | grep SharedFolderPathMachineMapping2", "SharedFolderPathMachineMapping2=\\\"" + backupTargetDir + "\\\"", "pass"));
		
		//Clock setup to try and stop drift between host and guest
		//https://www.virtualbox.org/manual/ch09.html#changetimesync
		units.addElement(guestPropertySet(service, user, "timesync-interval", "10000", "Couldn't sync the clock between " + service + " and its metal.  You'll probably see some clock drift in " + service + " as a result."));
		units.addElement(guestPropertySet(service, user, "timesync-min-adjust", "100", "Couldn't sync the clock between " + service + " and its metal.  You'll probably see some clock drift in " + service + " as a result."));
		units.addElement(guestPropertySet(service, user, "timesync-set-on-restore", "1", "Couldn't sync the clock between " + service + " and its metal.  You'll probably see some clock drift in " + service + " as a result."));
		units.addElement(guestPropertySet(service, user, "timesync-set-threshold", "1000", "Couldn't sync the clock between " + service + " and its metal.  You'll probably see some clock drift in " + service + " as a result."));
		
		//tty0 socket
		units.addElement(new SimpleUnit(service + "_tty0_com_port", service + "_exists",
				"sudo -u " + user + " VBoxManage modifyvm " + service + " --uart1 0x3F8 4",
				"sudo -u " + user + " VBoxManage showvminfo " + service + " --machinereadable | grep ^uart1=", "uart1=\\\"0x03f8,4\\\"", "pass"));

		units.addElement(new SimpleUnit(service + "_tty0_socket", service + "_tty0_com_port",
				"sudo -u " + user + " VBoxManage modifyvm " + service + " --uartmode1 server " + ttySocketDir + "/vboxttyS0",
				"sudo -u " + user + " VBoxManage showvminfo " + service + " --machinereadable | grep ^uartmode1=", "uartmode1=\\\"server," + ttySocketDir + "/vboxttyS0\\\"", "pass"));
		
		units.addElement(new SimpleUnit(service + "_tty1_com_port", service + "_exists",
				"sudo -u " + user + " VBoxManage modifyvm " + service + " --uart2 0x2F8 3",
				"sudo -u " + user + " VBoxManage showvminfo " + service + " --machinereadable | grep ^uart2=", "uart2=\\\"0x02f8,3\\\"", "pass"));

		units.addElement(new SimpleUnit(service + "_tty1_socket", service + "_tty1_com_port",
				"sudo -u " + user + " VBoxManage modifyvm " + service + " --uartmode2 server " + ttySocketDir + "/vboxttyS1",
				"sudo -u " + user + " VBoxManage showvminfo " + service + " --machinereadable | grep ^uartmode2=", "uartmode2=\\\"server," + ttySocketDir + "/vboxttyS1\\\"", "pass"));

		//Ready to go!
		//units.addElement(new SimpleUnit(service + "_running", service + "_exists",
		//		"sudo -u " + user + " bash -c 'VBoxManage startvm " + service + " --type headless'",
		//		"sudo -u " + user + " bash -c 'VBoxManage list runningvms | grep " + service + "'", "", "fail"));
		
		((ServerModel)me).getProcessModel().addProcess("/usr/lib/virtualbox/VBoxHeadless --comment " + service + " --startvm `if id '" + user + "' >/dev/null 2>&1; then sudo -u " + user + " bash -c 'VBoxManage list runningvms | grep " + service + "' | awk '{ print $2 }' | tr -d '{}'; else echo ''; fi` --vrde config *$");
		((ServerModel)me).getProcessModel().addProcess("awk \\{");
		((ServerModel)me).getProcessModel().addProcess("tr -d \\{\\}$");
		((ServerModel)me).getUserModel().addUsername(user);
		
		return units;
	}
	
	private SimpleUnit modifyVm(String service, String user, String setting, String value, String errorMsg, String prerequisite) {
		
		String check = "";
		
		//Integers aren't quoted...
		if (value.matches("-?(0|[1-9]\\d*)")) {
			check = setting + "=" + value;
		}
		else {
			check = setting + "=\\\"" + value + "\\\"";
		}
		
		return new SimpleUnit(service + "_" + setting + "_" + value, prerequisite,
				"sudo -u " + user + " VBoxManage modifyvm " + service + " --" + setting + " " + value,
				"sudo -u " + user + " VBoxManage showvminfo " + service + " --machinereadable | grep ^" + setting + "=",
				check, "pass",
				errorMsg);
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

	private SimpleUnit guestPropertySet(String service, String user, String property, String value, String errorMsg, String prerequisite) {
		return new SimpleUnit(service + "_" + property.replaceAll("-", "_") + "_" + value, prerequisite,
				"sudo -u " + user + " VBoxManage guestproperty set " + service + " \"/VirtualBox/GuestAdd/VBoxService/--" + property + "\" " + value,
				"sudo -u " + user + " VBoxManage guestproperty enumerate " + service + " | grep \"Name: /VirtualBox/GuestAdd/VBoxService/--" + property + ", value: " + value + "\"", "", "fail",
				errorMsg);
	}
	
	private SimpleUnit guestPropertySet(String service, String user, String property, String value, String errorMsg) {
		return guestPropertySet(service, user, property, value, errorMsg, service + "_exists");
	}
}
