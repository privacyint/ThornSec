package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.DirOwnUnit;
import core.unit.fs.DirPermsUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileOwnUnit;
import core.unit.fs.FilePermsUnit;
import core.unit.fs.FileUnit;
import core.unit.fs.GitCloneUnit;
import core.unit.pkg.InstalledUnit;

public class Virtualisation extends AStructuredProfile {
	
	public Virtualisation() {
		super("virtualisation");
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		model.getServerModel(server).getAptSourcesModel().addAptSource(server, model, "virtualbox", "proceed", "deb http://download.virtualbox.org/virtualbox/debian stretch contrib", "keyserver.ubuntu.com", "0xa2f683c52980aecf");
		model.getServerModel(server).addRouterFirewallRule(server, model, "allow_virtualbox", "download.virtualbox.org", new String[]{"80"});

		units.addElement(new InstalledUnit("metal_virtualbox", "virtualbox_gpg", "virtualbox-5.2"));
		units.addElement(new InstalledUnit("metal_genisoimage", "genisoimage"));
		units.addElement(new InstalledUnit("metal_rsync", "rsync"));
		units.addElement(new InstalledUnit("metal_git", "git"));
		units.addElement(new InstalledUnit("metal_qemu_utils", "qemu-utils"));
		units.addElement(new InstalledUnit("metal_duplicity", "duplicity"));

		units.addElement(new SimpleUnit("metal_qemu_nbd_enabled", "metal_qemu_utils_installed",
				"sudo modprobe nbd",
				"sudo lsmod | grep nbd", "", "fail",
				"The nbd kernel module couldn't be loaded.  Backups won't work."));
		
		model.getServerModel(server).getUserModel().addUsername("nbd");
		
		//This is for our internal backups
		units.addElement(new GitCloneUnit("backup_script", "metal_git_installed", "https://github.com/JohnKaul/rsync-time-backup.git", model.getData().getVmBase(server) + "/backup/rsync-time-backup",
				"The backup script couldn't be retrieved from github.  Backups won't work."));

		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		model.getServerModel(server).getProcessModel().addProcess("/usr/lib/virtualbox/VBoxXPCOMIPCD");
		model.getServerModel(server).getProcessModel().addProcess("/usr/lib/virtualbox/VBoxSVC --auto-shutdown");
		model.getServerModel(server).getProcessModel().addProcess("\\[iprt-VBoxWQueue\\]");
		model.getServerModel(server).getProcessModel().addProcess("\\[iprt-VBoxTscThr\\]");
		model.getServerModel(server).getProcessModel().addProcess("\\[kvm-irqfd-clean\\]");
		
		return units;
	}
	
	public String preseed(String server, String service, NetworkModel model, Boolean expirePasswords) {

		String   user            = model.getData().getUser(service);
		String   sshDir          = "/home/" + user + "/.ssh";
		String[] pubKeys         = model.getData().getUserKeys(service);
		String   hostname        = model.getData().getHostname(service);
		String   domain          = model.getData().getDomain(service);
		String   fullName        = model.getData().getFullName(service);
		String   debianMirror    = model.getData().getDebianMirror(service);
		String   debianDirectory = model.getData().getDebianDirectory(service);

		String preseed = "";
		//Set up new box before rebooting. Sometimes you need to echo out in chroot;
		//in-target just doesn't work reliably for many things (most likely due to the shell it uses) :(
		preseed += "d-i preseed/late_command string";
		//Echo out public keys, make sure it's all secured properly
		preseed += "	in-target mkdir " + sshDir + ";";
		preseed += "    in-target touch " + sshDir + "/authorized_keys;";

		for (int i = 0; i < pubKeys.length; ++i) {
			preseed += "	echo \\\"echo '" + pubKeys[i] + "' >> " + sshDir + "/authorized_keys; \\\" | chroot /target /bin/bash;";
		}
		
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
		preseed += "    in-target sed -i 's/#Port 22/Port " + model.getData().getSSHPort(service) + "/g' /etc/ssh/sshd_config;";
		
		//Set up shared folders
		//Don't do this any more - Debian no longer boots if it can't mount them :'(
		//preseed += "	echo \\\""
		//						+ "echo '# Shared folders mount' >> /etc/fstab"
		//						+ "&& echo 'log       /var/log           vboxsf defaults,dmode=751 0 0' >> /etc/fstab"
		//						+ "&& echo 'backup    /media/backup      vboxsf defaults 0 0' >> /etc/fstab"
		//				+ "\\\"| chroot /target /bin/bash";
		
		//Debian installer options.
		preseed += "\n";
		preseed += "d-i debian-installer/locale string en_US\n";
		preseed += "d-i keyboard-configuration/xkb-keymap select us\n";
		preseed += "d-i netcfg/target_network_config select ifupdown\n";
		preseed += "d-i netcfg/choose_interface select auto\n";
		preseed += "d-i netcfg/get_hostname string " + hostname + "\n";
		preseed += "d-i netcfg/get_domain string " + domain + "\n";
		preseed += "d-i netcfg/hostname string " + hostname + "\n";
		preseed += "d-i mirror/country string GB\n";
		preseed += "d-i mirror/http/mirror string " + debianMirror + "\n";
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
		preseed += "popularity-contest popularity-contest/participate boolean false\n";
		preseed += "d-i grub-installer/only_debian boolean true\n";
		preseed += "d-i grub-installer/with_other_os boolean true\n";
		preseed += "d-i grub-installer/bootdev string default\n";
		preseed += "d-i finish-install/reboot_in_progress note";

		return preseed;
	}

	public Vector<IUnit> buildIso(String server, String service, NetworkModel model, String preseed) {

		Vector<IUnit> units = new Vector<IUnit>();
		
		String isoDir =  model.getData().getVmBase(server) + "/iso/" + service + "/";

		units.addElement(new DirUnit("iso_dir_" + service, "proceed", isoDir));
		units.addElement(new FileUnit("preseed_" + service, "debian_netinst_iso_downloaded", preseed, isoDir + "preseed.cfg"));
		units.addElement(new FileOwnUnit("preseed_" + service, "preseed_" + service, isoDir + "preseed.cfg", "root"));
		units.addElement(new FilePermsUnit("preseed_" + service, "preseed_" + service + "_chowned", isoDir + "preseed.cfg", "700"));
				
		units.addElement(new SimpleUnit("build_iso_" + service, "debian_netinst_iso_downloaded",
				"sudo bash -c '"
					+ "cd " + isoDir + ";"
					+ " mkdir loopdir;"
					+ " mount -o loop " + model.getData().getVmBase(server) + "/debian-netinst.iso loopdir;"
					+ " mkdir cd;"
					+ " rsync -a -H --exclude=TRANS.TBL loopdir/ cd;"
					+ " umount loopdir;"
					+ " cd cd;"
					+ " cp ../preseed.cfg .;"
					+ " sed -i \"s/timeout 0/timeout 1/g\" isolinux/isolinux.cfg;"
					+ " sed -i \"s_append_append file=/cdrom/preseed.cfg auto=true_g\" isolinux/gtk.cfg;"
					+ " sed -i \"s_/install.amd/gtk/initrd.gz_/install.amd/initrd.gz_g\" isolinux/gtk.cfg;"
					+ " md5sum `find -follow -type f` > md5sum.txt;"
				+ "' > /dev/null 2>&1;"
				+ "sudo bash -c '"
					+ "cd " + isoDir + ";"
					+ " genisoimage -o " + service + ".iso -r -J -no-emul-boot -boot-load-size 4 -boot-info-table -b isolinux/isolinux.bin -c isolinux/boot.cat ./cd;"
					+ " rm -R cd loopdir;"
				+ "'",
				"test -f " + isoDir + service + ".iso && echo 'pass' || echo 'fail'", "pass", "pass",
				"Couldn't create the install ISO for " + service + ".  This service won't be able to install."));
		
		return units;
	}

	public Vector<IUnit> buildVm(String server, String service, NetworkModel model, String bridge) {
		String baseDir    = model.getData().getVmBase(server);
		String storageDir = baseDir + "/storage/" + service;
		String dataDir    = baseDir + "/data/" + service;
		String logDir     = baseDir + "/log/" + service;
		String backupDir  = baseDir + "/backup/" + service;
		String storageVdi = storageDir + "/" + service + "_storage.vdi";
		String dataVdi    = dataDir + "/" + service + "_data.vdi";
		String iso        = baseDir + "/iso/" + service + "/" + service + ".iso";
		String user       = "vboxuser_" + service;
		String group      = "vboxusers";
		String osType     = model.getData().getDebianIsoUrl(service).contains("amd64") ? "Debian_64" : "Debian";
		
		Vector<IUnit> units = new Vector<IUnit>();
		
		//Metal user setup
		units.addElement(new SimpleUnit("metal_virtualbox_" + service + "_user", "metal_virtualbox_installed",
				"sudo adduser " + user + " --system --shell=/bin/false --disabled-login --ingroup " + group,
				"id " + user + " 2>&1", "id: ‘" + user + "’: no such user", "fail",
				"Couldn't create the user for " + service + " on its metal.  This is fatal, " + service + " will not be installed."));
		
		//Metal storage setup
		units.addElement(new DirUnit("storage_dir_" + service, "proceed", storageDir));
		units.addElement(new DirOwnUnit("storage_dir_" + service, "storage_dir_" + service + "_created", storageDir, user, group));
		units.addElement(new DirPermsUnit("storage_dir_" + service, "storage_dir_" + service + "_chowned", storageDir, "750"));
	
		units.addElement(new DirUnit("data_dir_" + service, "proceed", dataDir));
		units.addElement(new DirOwnUnit("data_dir_" + service , "data_dir_" + service + "_created", dataDir, user, group));
		units.addElement(new DirPermsUnit("data_dir_" + service, "data_dir_" + service + "_chowned", dataDir, "750"));

		units.addElement(new DirUnit("log_dir_" + service, "proceed", logDir));
		units.addElement(new DirOwnUnit("log_dir_" + service, "log_dir_" + service + "_created", logDir, user, group));
		units.addElement(new DirPermsUnit("log_dir_" + service, "log_dir_" + service + "_chowned", logDir, "750"));
		
		units.addElement(new DirUnit("backup_dir_" + service, "proceed", backupDir));
		units.addElement(new DirOwnUnit("backup_dir_" + service, "backup_dir_" + service + "_created", backupDir, user, group));
		units.addElement(new DirPermsUnit("backup_dir_" + service, "backup_dir_" + service + "_chowned", backupDir, "750"));
		
		//Mark the backup directory as a valid destination
		units.addElement(new FileUnit(service + "_mark_backup_dir", "backup_dir_" + service + "_chmoded" , "What a nutkin.  MC don alisha.  Chicken nuggets!", backupDir + "/backup.marker"));
		
		//VM setup
		units.addElement(new SimpleUnit(service + "_exists", "storage_dir_" + service + "_chmoded",
				"sudo -u " + user + " bash -c 'VBoxManage createvm --name " + service + " --ostype \"" + osType + "\" --register'",
				"sudo -u " + user + " bash -c 'VBoxManage list vms | grep " + service + "'", "", "fail",
				"Couldn't create " + service + " on its metal.  This is fatal, " + service + " will not be installed."));
		
		//Disk controller setup
		units.addElement(new SimpleUnit(service + "_sata_controller", service + "_exists",
				"sudo -u " + user + " bash -c 'VBoxManage storagectl " + service + " --name \"SATA Controller\" --add sata --controller IntelAHCI'",
				"sudo -u " + user + " bash -c 'VBoxManage showvminfo " + service + " --machinereadable | grep storagecontrollername0'", "storagecontrollername0=\\\"SATA Controller\\\"", "pass",
				"The SATA controller for " + service + " (where its disks are atached) Couldn't be created/attached to " + service + ".  This is fatal, " + service + " will not be installed."));
		
		units.addElement(new SimpleUnit(service + "_ide_controller", service + "_exists",
				"sudo -u " + user + " bash -c 'VBoxManage storagectl " + service + " --name \"IDE Controller\" --add ide'",
				"sudo -u " + user + " bash -c 'VBoxManage showvminfo " + service + " --machinereadable | grep storagecontrollername1'", "storagecontrollername1=\\\"IDE Controller\\\"", "pass",
				"The IDE controller for " + service + " (where its disks are atached) Couldn't be created/attached to " + service + ".  This is fatal, " + service + " will not be installed."));

		//Disk setup
		units.addElement(new SimpleUnit(service + "_disk", "storage_dir_" + service + "_chmoded",
				"sudo -u " + user + " bash -c 'VBoxManage createhd --filename " + storageVdi + " --size " + model.getData().getDiskSize(service) + "'",
				"sudo [ -f " + storageVdi + " ] && echo pass;", "pass", "pass",
				"Couldn't create the disk for " + service + "'s base filesystem.  This is fatal."));
		
		units.addElement(new SimpleUnit(service + "_disk_attached", service + "_sata_controller",
				"sudo -u " + user + " bash -c 'VBoxManage storageattach " + service + " --storagectl \"SATA Controller\" --port 0 --device 0 --type hdd --medium " + storageVdi +"'",
				"sudo -u " + user + " bash -c 'VBoxManage showvminfo " + service + " --machinereadable | grep \"SATA Controller-0-0\"'", "\\\"SATA Controller-0-0\\\"=\\\"" + storageVdi + "\\\"", "pass",
				"Couldn't attach the disk for " + service + "'s base filesystem.  This is fatal."));

		units.addElement(new SimpleUnit(service + "_data_disk", "data_dir_" + service + "_chmoded",
				"sudo -u " + user + " bash -c 'VBoxManage createhd --filename " + dataVdi + " --size " + model.getData().getDataDiskSize(service) + "'",
				"sudo [ -f " + dataVdi + " ] && echo pass;", "pass", "pass",
				"Couldn't create the disk for " + service + "'s data.  This is fatal."));
		
		units.addElement(new SimpleUnit(service + "_data_disk_attached", service + "_sata_controller",
				"sudo -u " + user + " bash -c 'VBoxManage storageattach " + service + " --storagectl \"SATA Controller\" --port 1 --device 0 --type hdd --medium " + dataVdi +"'",
				"sudo -u " + user + " bash -c 'VBoxManage showvminfo " + service + " --machinereadable | grep \"SATA Controller-1-0\"'", "\\\"SATA Controller-1-0\\\"=\\\"" + dataVdi + "\\\"", "pass",
				"Couldn't attach the disk for " + service + "'s data.  This is fatal."));
		
		units.addElement(new SimpleUnit(service + "_install_iso_attached", service + "_ide_controller",
				"sudo -u " + user + " bash -c 'VBoxManage storageattach " + service + " --storagectl \"IDE Controller\" --port 0 --device 0 --type dvddrive --medium " + iso + "'",
				"sudo -u " + user + " bash -c 'VBoxManage showvminfo " + service + " --machinereadable | grep \"IDE Controller-0-0\"'", "\\\"IDE Controller-0-0\\\"=\\\"" + iso + "\\\"", "pass",
				"Couldn't attach the preseeded installation disk for " + service + ".  This service will not be installed."));
		
		units.addElement(new SimpleUnit(service + "_guest_additions_iso_attached", service + "_ide_controller",
				"sudo -u " + user + " bash -c 'VBoxManage storageattach " + service + " --storagectl \"IDE Controller\" --port 0 --device 1 --type dvddrive --medium /usr/share/virtualbox/VBoxGuestAdditions.iso'",
				"sudo -u " + user + " bash -c 'VBoxManage showvminfo " + service + " --machinereadable | grep \"IDE Controller-0-1\"'", "\\\"IDE Controller-0-1\\\"=\\\"/usr/share/virtualbox/VBoxGuestAdditions.iso\\\"", "pass",
				"Couldn't attach the VirtualBox Guest Additions disk for " + service + ".  Logs will not be pushed out to the hypervisor as expected."));
		
		//Architecture setup
		units.addElement(new SimpleUnit(service + "_ioapic_on", service + "_exists",
				"sudo -u " + user + " bash -c 'VBoxManage modifyvm " + service + " --ioapic on'",
				"sudo -u " + user + " bash -c 'VBoxManage showvminfo " + service + " --machinereadable | grep ioapic'", "ioapic=\\\"on\\\"", "pass",
				"IO APIC couldn't be enabled for " + service + ".  This is required for 64-bit installations, and for more than 1 virtual CPU in a service."));
		
		units.addElement(new SimpleUnit(service + "_pae_on", service + "_exists",
				"sudo -u " + user + " bash -c 'VBoxManage modifyvm " + service + " --pae on'",
				"sudo -u " + user + " bash -c 'VBoxManage showvminfo " + service + " --machinereadable | grep pae'", "pae=\\\"on\\\"", "pass",
				"PAE (Physical Address Extensions) couldn't be enabled for " + service + ".  This isn't great, but isn't fatal."));
		
		units.addElement(new SimpleUnit(service + "_ram", service + "_exists",
				"sudo -u " + user + " bash -c 'VBoxManage modifyvm " + service + " --memory " + model.getData().getRam(service) + "'",
				"sudo -u " + user + " bash -c 'VBoxManage showvminfo " + service + " --machinereadable | grep memory'", "memory=" + model.getData().getRam(service), "pass",
				"Couldn't set the required amount of RAM for " + service + ".  This isn't great, but isn't fatal."));
		
		units.addElement(new SimpleUnit(service + "_vram", service + "_exists",
				"sudo -u " + user + " bash -c 'VBoxManage modifyvm " + service + " --vram 16'",
				"sudo -u " + user + " bash -c 'VBoxManage showvminfo " + service + " --machinereadable | grep vram'", "vram=16", "pass",
				"Couldn't reduce the RAM reserved for video in " + service + ".  This will reduce the amount of free RAM available in the service."));
		
		units.addElement(new SimpleUnit(service + "_cpus", service + "_exists",
				"sudo -u " + user + " bash -c 'VBoxManage modifyvm " + service + " --cpus " + model.getData().getCpus(service) + "'",
				"sudo -u " + user + " bash -c 'VBoxManage showvminfo " + service + " --machinereadable | grep cpus'", "cpus=" + model.getData().getCpus(service), "pass",
				"Couldn't set the number of CPUs for " + service + ".  This means the service will only have 1 CPU available for use."));
		
		//Boot setup - DVD is second to stop machines being wiped every time they're brought up
		units.addElement(new SimpleUnit(service + "_boot1_disk", service + "_disk_attached",
				"sudo -u " + user + " bash -c 'VBoxManage modifyvm " + service + " --boot1 disk'",
				"sudo -u " + user + " bash -c 'VBoxManage showvminfo " + service + " --machinereadable | grep boot1'", "boot1=\\\"disk\\\"", "pass",
				"Couldn't set the boot order for " + service + ".  This may mean the service will not be installed."));
		
		units.addElement(new SimpleUnit(service + "_boot2_dvd", service + "_ide_controller",
				"sudo -u " + user + " bash -c 'VBoxManage modifyvm " + service + " --boot2 dvd'",
				"sudo -u " + user + " bash -c 'VBoxManage showvminfo " + service + " --machinereadable | grep boot2'", "boot2=\\\"dvd\\\"", "pass",
				"Couldn't set the boot order for " + service + ".  This may mean the service will not be installed."));
		
		//Networking setup
		units.addElement(new SimpleUnit(service + "_nic_bridged", service + "_exists",
				"sudo -u " + user + " bash -c 'VBoxManage modifyvm " + service + " --nic1 bridged'",
				"sudo -u " + user + " bash -c 'VBoxManage showvminfo " + service + " --machinereadable | grep nic1'", "nic1=\\\"bridged\\\"", "pass",
				"Couldn't give " + service + " a connection to the network.  This means the service will not be able to talk to the router or network, and will not be installed."));

		units.addElement(new SimpleUnit(service + "_nic_bridge_adapter", service + "_nic_bridged",
				"sudo -u " + user + " bash -c 'VBoxManage modifyvm " + service + " --bridgeadapter1 " + bridge + "'",
				"sudo -u " + user + " bash -c 'VBoxManage showvminfo " + service + " --machinereadable | grep bridgeadapter1'", "bridgeadapter1=\\\"" + bridge + "\\\"", "pass",
				"Couldn't give " + service + " a connection to the network.  This means the service will not be able to talk to the router or network, and will not be installed."));
		
		units.addElement(new SimpleUnit(service + "_nic_type", service + "_nic_bridge_adapter",
				"sudo -u " + user + " bash -c 'VBoxManage modifyvm " + service + " --nictype1 virtio'",
				"sudo -u " + user + " bash -c 'VBoxManage showvminfo " + service + " --machinereadable | grep nictype1'", "nictype1=\\\"virtio\\\"", "pass",
				"Couldn't set " + service + "'s network adapter to use the virtio drivers.  This will result in a performance hit on the service, and means traffic will flow over the physical adapter."));
		
		units.addElement(new SimpleUnit(service + "_mac_address", service + "_nic_type",
				"sudo -u " + user + " bash -c 'VBoxManage modifyvm " + service + " --macaddress1 " + model.getData().getMac(service).replace(":", "").toUpperCase() + "'",
				"sudo -u " + user + " bash -c 'VBoxManage showvminfo " + service + " --machinereadable | grep macaddress1'", "macaddress1=\\\"" + model.getData().getMac(service).replace(":", "").toUpperCase() + "\\\"", "pass",
				"Couldn't set " + service + "'s MAC address.  This means the service will not be able to get an IP address, and will not be installed."));
		
		//Audio setup (switch it off)
		units.addElement(new SimpleUnit(service + "_no_audio", service + "_exists",
				"sudo -u " + user + " bash -c 'VBoxManage modifyvm " + service + " --audio none'",
				"sudo -u " + user + " bash -c 'VBoxManage showvminfo " + service + " --machinereadable | grep audio'", "audio=\\\"none\\\"", "pass",
				"Couldn't switch off " + service + "'s audio.  No biggie."));
		
		//Shared folders setup
		units.addElement(new SimpleUnit(service + "_log_sf_attached", service + "_exists",
				"sudo -u " + user + " bash -c 'VBoxManage sharedfolder add " + service + " --name log --hostpath " + logDir + "';"
				+ "sudo -u " + user + " bash -c 'VBoxManage setextradata " + service + " VBoxInternal1/SharedFoldersEnableSymlinksCreate/log 1'",
				"sudo -u " + user + " bash -c 'VBoxManage showvminfo " + service + " --machinereadable | grep SharedFolderPathMachineMapping1'", "SharedFolderPathMachineMapping1=\\\"" + logDir + "\\\"", "pass",
				"Couldn't attach the logs folder to " + service + ".  This means logs will only exist in the VM."));
		
		units.addElement(new SimpleUnit(service + "_backup_sf_attached", service + "_exists",
				"sudo -u " + user + " bash -c 'VBoxManage sharedfolder add " + service + " --name backup --hostpath " + backupDir + "'",
				"sudo -u " + user + " bash -c 'VBoxManage showvminfo " + service + " --machinereadable | grep SharedFolderPathMachineMapping2'", "SharedFolderPathMachineMapping2=\\\"" + backupDir + "\\\"", "pass"));
		
		//Clock setup to try and stop drift between host and guest
		//https://www.virtualbox.org/manual/ch09.html#changetimesync
		units.addElement(new SimpleUnit(service + "_timesync_interval", service + "_exists",
				"sudo -u " + user + " bash -c 'VBoxManage guestproperty set " + service + " \"/VirtualBox/GuestAdd/VBoxService/--timesync-interval\" 10000'",
				"sudo -u " + user + " bash -c 'VBoxManage guestproperty enumerate " + service + " | grep \"Name: /VirtualBox/GuestAdd/VBoxService/--timesync-interval, value: 10000\"'", "", "fail",
				"Couldn't sync the clock between " + service + " and its metal.  You'll probably see some clock drift in " + service + " as a result."));
		
		units.addElement(new SimpleUnit(service + "_timesync_min_adjust", service + "_exists",
				"sudo -u " + user + " bash -c 'VBoxManage guestproperty set " + service + " \"/VirtualBox/GuestAdd/VBoxService/--timesync-min-adjust\" 100'",
				"sudo -u " + user + " bash -c 'VBoxManage guestproperty enumerate " + service + " | grep \"Name: /VirtualBox/GuestAdd/VBoxService/--timesync-min-adjust, value: 100\"'", "", "fail",
				"Couldn't sync the clock between " + service + " and its metal.  You'll probably see some clock drift in " + service + " as a result."));

		units.addElement(new SimpleUnit(service + "_timesync_set_on_restore", service + "_exists",
				"sudo -u " + user + " bash -c 'VBoxManage guestproperty set " + service + " \"/VirtualBox/GuestAdd/VBoxService/--timesync-set-on-restore\" 1'",
				"sudo -u " + user + " bash -c 'VBoxManage guestproperty enumerate " + service + " | grep \"Name: /VirtualBox/GuestAdd/VBoxService/--timesync-set-on-restore, value: 1\"'", "", "fail",
				"Couldn't sync the clock between " + service + " and its metal.  You'll probably see some clock drift in " + service + " as a result."));

		units.addElement(new SimpleUnit(service + "_timesync_set_threshold", service + "_exists",
				"sudo -u " + user + " bash -c 'VBoxManage guestproperty set " + service + " \"/VirtualBox/GuestAdd/VBoxService/--timesync-set-threshold\" 1000'",
				"sudo -u " + user + " bash -c 'VBoxManage guestproperty enumerate " + service + " | grep \"Name: /VirtualBox/GuestAdd/VBoxService/--timesync-set-threshold, value: 1000\"'", "", "fail",
				"Couldn't sync the clock between " + service + " and its metal.  You'll probably see some clock drift in " + service + " as a result."));
		
		//Ready to go!
		//units.addElement(new SimpleUnit(service + "_running", service + "_exists",
		//		"sudo -u " + user + " bash -c 'VBoxManage startvm " + service + " --type headless'",
		//		"sudo -u " + user + " bash -c 'VBoxManage list runningvms | grep " + service + "'", "", "fail"));
		
		model.getServerModel(server).getProcessModel().addProcess("/usr/lib/virtualbox/VBoxHeadless --comment " + service + " --startvm `if id '" + user + "' >/dev/null 2>&1; then sudo -u " + user + " bash -c 'VBoxManage list runningvms | grep " + service + "' | awk '{ print $2 }' | tr -d '{}'; else echo ''; fi` --vrde config *$");
		model.getServerModel(server).getProcessModel().addProcess("awk \\{");
		model.getServerModel(server).getProcessModel().addProcess("tr -d \\{\\}$");
		model.getServerModel(server).getUserModel().addUsername(user);
		
		return units;
	}
}