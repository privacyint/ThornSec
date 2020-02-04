/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.stack;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import javax.swing.JOptionPane;
import org.apache.commons.io.FilenameUtils;
import core.StringUtils;
import core.exception.data.NoValidUsersException;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.InvalidMachineModelException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.machine.configuration.DiskModel;
import core.model.network.NetworkModel;
import core.data.machine.UserDeviceData;
import core.data.machine.configuration.DiskData.Medium;
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
	public final Collection<IUnit> getPersistentConfig() throws InvalidServerModelException {
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

	public Collection<IUnit> buildIso(String service, String preseed) throws InvalidServerException {

		final Collection<IUnit> units = new ArrayList<>();

		final String isoDir = getNetworkModel().getData().getHypervisorThornsecBase(getLabel()) + "/isos/" + service;

		String filename = null;
		String cleanedFilename = null;

		try {
			filename = Paths.get(new URI(getNetworkModel().getData().getDebianIsoUrl(service)).getPath()).getFileName().toString();
			cleanedFilename = StringUtils.stringToAlphaNumeric(filename, "_");
		} catch (final Exception e) {
			JOptionPane.showMessageDialog(null, "You shouldn't have been able to arrive here. Well done!");
			System.exit(1);
		}

		units.add(new DirUnit("iso_dir_" + service, "proceed", isoDir));
		FileUnit preseedUnit = new FileUnit("preseed_" + service, cleanedFilename + "_downloaded", isoDir + "/preseed.cfg");
		preseedUnit.appendLine(preseed);
		units.add(preseedUnit);
		units.add(new FileOwnUnit("preseed_" + service, "preseed_" + service, isoDir + "/preseed.cfg", "root"));
		units.add(new FilePermsUnit("preseed_" + service, "preseed_" + service + "_chowned", isoDir + "/preseed.cfg",
				"700"));

		String buildIso = "\n";
		buildIso += "\tsudo bash -c '\n";
		// Create a working copy of the iso for preseeding
		buildIso += "\t\tcd " + isoDir + ";\n";
		buildIso += "\t\tmkdir loopdir;\n";
		buildIso += "\t\tmount -o loop " + getNetworkModel().getData().getHypervisorThornsecBase(getLabel()) + "/"	+ filename + " loopdir;\n";
		buildIso += "\t\tmkdir cd;\n";
		buildIso += "\t\trsync -a -H --exclude=TRANS.TBL loopdir/ cd;\n";
		buildIso += "\t\tumount loopdir;\n";
		buildIso += "\t\tcd cd;\n";
		// Add our preseed directly to the initrd as per https://wiki.debian.org/DebianInstaller/Preseed/EditIso
		buildIso += "\t\tgunzip install.*/initrd.gz;\n";
		buildIso += "\t\techo ../preseed.cfg | cpio -H newc -o -A -F install.*/initrd;\n";
		buildIso += "\t\tgzip install.*/initrd;\n";
		// Set the menu timeout to 1 second, otherwise it waits for user input
		buildIso += "\t\tsed -i \"s/timeout 0/timeout 1/g\" isolinux/isolinux.cfg;\n";
		// Switch off default menu
		buildIso += "\t\tsed -i \"s/^default/#default/g\" isolinux/isolinux.cfg;\n";
		// Switch off vga and add console to *all* boot lines
		buildIso += "\t\tsed -i \"s_vga=788_vga=none console=ttyS0,115200n8_g\" isolinux/*.cfg;\n";
		// Rebuild md5sums to reflect changes
		buildIso += "\t\tmd5sum `find -follow -type f` > md5sum.txt;\n";
		buildIso += "\t' > /dev/null 2>&1;\n";
		// Create our new preseeded image
		buildIso += "\tsudo bash -c '\n";
		buildIso += "\t\tcd " + isoDir + ";\n";
		buildIso += "\t\tgenisoimage -o " + service+ ".iso -r -J -no-emul-boot -boot-load-size 4 -boot-info-table -b isolinux/isolinux.bin -c isolinux/boot.cat ./cd;\n";
		buildIso += "\t\trm -R cd loopdir;\n";
		buildIso += "\t'";

		units.add(new SimpleUnit("build_iso_" + service, cleanedFilename + "_downloaded",
				buildIso, "test -f " + isoDir + "/" + service + ".iso && echo 'pass' || echo 'fail'", "pass", "pass",
				"Couldn't create the install ISO for " + service + ".  This service won't be able to install."));

		return units;
	}

	public String preseed(String service, Boolean expirePasswords) throws InvalidServerModelException, NoValidUsersException, MalformedURLException, URISyntaxException {

		String user            = getNetworkModel().getData().getUser();
		String sshDir          = "/home/" + user + "/.ssh";
		String pubKey          = getNetworkModel().getData().getSSHKey(user);
		String hostname        = service;
		String domain          = getNetworkModel().getServerModel(service).getDomain().getHost();
		String fullName        = ((UserDeviceData) getNetworkModel().getData().getUserDevices().get(user)).getFullName();
		String debianMirror    = getNetworkModel().getData().getDebianMirror(service).toString();
		String debianDirectory = getNetworkModel().getData().getDebianDirectory(service);

		String preseed = "";
		//Set up new box before rebooting. Sometimes you need to echo out in chroot;
		//in-target just doesn't work reliably for many things (most likely due to the shell it uses) :(
		preseed += "d-i preseed/late_command string";
		//Echo out public keys, make sure it's all secured properly
		preseed += "\tin-target mkdir " + sshDir + ";";
		preseed += "\tin-target touch " + sshDir + "/authorized_keys;";
		preseed += "\techo \\\"echo '" + pubKey + "' >> " + sshDir + "/authorized_keys; \\\" | chroot /target /bin/bash;";
		
		preseed += "\tin-target chmod 700 " + sshDir + ";";
		preseed += "\tin-target chmod 400 " + sshDir + "/authorized_keys;";
		preseed += "\tin-target chown -R " + user + ":" + user + " " + sshDir + ";";
		
		if (expirePasswords) {
			//Force the user to change their passphrase on first login if they haven't set a passwd
			preseed += "\tin-target passwd -e " + user + ";";
		}
		
		//Lock the root account
		preseed += "\tin-target passwd -l root;";
		
		//Change the SSHD to be on the expected port
		preseed += "\tin-target sed -i 's/#Port 22/Port " + getNetworkModel().getData().getSSHPort(service) + "/g' /etc/ssh/sshd_config;";
		
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

	public Collection<IUnit> buildServiceVm(String service, String bridge)
			throws InvalidServerException, InvalidMachineModelException {
		final String baseDir = getNetworkModel().getData().getHypervisorThornsecBase(getLabel());

		// Disks
		Map<String, DiskModel> disks = getNetworkModel().getServiceModel(service).getDisks();

		final String logDir = baseDir + "/logs/" + service;
		final String backupTargetDir = baseDir + "/backups/" + service;
		final String ttySocketDir = baseDir + "/sockets/" + service;

		//final String installIso = baseDir + "/isos/" + service + "/" + service + ".iso";
		final String user = "vboxuser_" + service;
		final String group = "vboxusers";
		final String osType = getNetworkModel().getData().getDebianIsoUrl(service).contains("amd64") ? "Debian_64" : "Debian";

		final Collection<IUnit> units = new ArrayList<>();

		// Create VirtualBox user
		units.add(new SimpleUnit("metal_virtualbox_" + service + "_user", "metal_virtualbox_installed",
				"sudo adduser " + user + " --system --shell=/bin/false --disabled-login --ingroup " + group,
				"id -u " + user + " 2>&1 | grep 'no such user'", "", "pass",
				"Couldn't create the user for " + service + " on its HyperVisor.  This is fatal, " + service + " will not be installed."));
		
		// Create VM itself
		units.add(new SimpleUnit(service + "_exists", "metal_virtualbox_" + service + "_user",
				"sudo -u " + user + " VBoxManage createvm --name " + service + " --ostype \"" + osType + "\"" + " --register;"
				+ "sudo -u " + user + " VBoxManage modifyvm " + service + " --description "
						+ "\"" + service + "." + getNetworkModel().getData().getDomain() + "\n"
						+ "ThornSec guest machine\n"
						+ "Built with profile(s): "	+ String.join(", ", getNetworkModel().getData().getProfiles(service)) + "\n"
						+ "Built at $(date)" + "\"",
				"sudo -u " + user + " VBoxManage list vms | grep " + service, "", "fail",
				"Couldn't create " + service + " on its HyperVisor.  This is fatal, " + service + " will not exist on your network."));
		
		// Set up VM's storage
		
		// Disk controller setup
		units.add(new SimpleUnit(service + "_hdds_sas_controller", service + "_exists",
				"sudo -u " + user + " VBoxManage storagectl " + service	+ " --name \"HDDs\""
						+ " --add sas"
						+ " --controller LSILogicSAS"
						+ " --portcount " + disks.values().stream().filter(disk -> disk.getMedium() == Medium.DISK).count()
						+ " --hostiocache off",
				"sudo -u " + user + " VBoxManage showvminfo " + service + " --machinereadable | grep ^storagecontrollername0=",
				"storagecontrollername0=\\\"HDDs\\\"", "pass",
				"The hard drive SAS controller for " + service + " (where its disks are attached) Couldn't be created/attached to "
						+ service + ".  This is fatal, " + service + " will not be installed."));
		
		units.add(new SimpleUnit(service + "_dvds_ide_controller", service + "_exists",
				"sudo -u " + user + " VBoxManage storagectl " + service	+ " --name \"DVDs\""
						+ " --add ide"
						+ " --controller PIIX4"
						//+ " --portcount " + disks.values().stream().filter(disk -> disk.getMedium() == Medium.DVD).count()
						+ " --hostiocache off",
				"sudo -u " + user + " VBoxManage showvminfo " + service + " --machinereadable | grep ^storagecontrollername1=",
				"storagecontrollername1=\\\"DVDs\\\"", "pass",
				"The DVD SAS controller for " + service + " (where its disks are attached) Couldn't be created/attached to "
						+ service + ".  This is fatal, " + service + " will not be installed."));
		
		int deviceCounter = 0;
		for (DiskModel disk : disks.values().stream().filter(disk -> disk.getMedium() == Medium.DISK).toArray(DiskModel[]::new)) {
			units.add(new DirUnit(disk.getLabel() + "_disk_dir_" + service, "proceed", disk.getFilePath()));
			units.add(new DirOwnUnit(disk.getLabel() + "_disk_dir_" + service, disk.getLabel() + "_disk_dir_" + service + "_created", disk.getFilePath(), user,	group));
			units.add(new DirPermsUnit(disk.getLabel() + "_disk_dir_" + service, disk.getLabel() + "_disk_dir_" + service + "_chowned", disk.getFilePath(),	"750"));
			
			units.add(new DirUnit(disk.getLabel() + "_disk_loopback_dir_" + service, "proceed", disk.getFilePath() + "/live/"));
			units.add(new DirOwnUnit(disk.getLabel() + "_disk_loopback_dir_" + service, disk.getLabel() + "_disk_loopback_dir_" + service + "_created", disk.getFilePath() + "/live/", "root"));
			units.add(new DirPermsUnit(disk.getLabel() + "_disk_loopback_dir_" + service, disk.getLabel() + "_disk_loopback_dir_" + service + "_chowned", disk.getFilePath() + "/live/", "700"));
			
			String diskCreation = "";
			diskCreation += "sudo -u " + user + " VBoxManage createmedium --filename " + disk.getFilename();
			diskCreation += " --size " + disk.getSize();
			diskCreation += " --format " + disk.getFormat();
			diskCreation += (disk.getDiffParent() != null) ? " --diffparent " + disk.getDiffParent() :"";
			
			units.add(new SimpleUnit(service + "_" + disk.getLabel() + "_disk", disk.getLabel() + "_disk_dir_" + service + "_chmoded",
					diskCreation,
					"sudo [ -f " + disk.getFilename() + " ] && echo pass;", "pass", "pass",
					"Couldn't create the disk " + disk.getLabel() + " for " + service + "."));
			units.add(new FileOwnUnit(service + "_" + disk.getLabel() + "_disk", service + "_" + disk.getLabel() + "_disk", disk.getFilename(), user, group));
			
			String diskAttach = "";
			diskAttach += "sudo -u " + user + " VBoxManage storageattach " + service;
			diskAttach += " --storagectl \"HDDs\"";
			diskAttach += " --port " + deviceCounter;
			diskAttach += " --device 0";
			diskAttach += " --type hdd";
			diskAttach += " --medium " + disk.getFilename();
			//diskAttach += (disk.getLabel().contentEquals("boot")) ? " --bootable on" : " --bootable -off";
			//diskAttach += " --comment \\\"" + disk.getComment() + "\\\";";
			
			units.add(new SimpleUnit(service + "_" + disk.getLabel() + "_disk_attached", service + "_hdds_sas_controller",
					diskAttach,
					"sudo -u " + user + " VBoxManage showvminfo " + service + " --machinereadable | grep \"HDDs-0-" + deviceCounter + "\"",
					"\\\"HDDs-0-" + deviceCounter + "\\\"=\\\"" + disk.getFilename() + "\\\"", "pass",
					"Couldn't attach disk " + disk.getLabel() + "for " + service + "."));

			deviceCounter++;
		};
		
		deviceCounter = 0;
		for (DiskModel disk : disks.values().stream().filter(disk -> disk.getMedium() == Medium.DVD).toArray(DiskModel[]::new)) {
			String diskAttach = "";
			diskAttach += "sudo -u " + user + " VBoxManage storageattach " + service;
			diskAttach += " --storagectl \"DVDs\"";
			diskAttach += " --port " + deviceCounter;
			diskAttach += " --device 0";
			diskAttach += " --type dvddrive";
			diskAttach += " --medium " + disk.getFilename();
			//diskAttach += (disk.getLabel().contentEquals("boot")) ? " --bootable on" : " --bootable -off";
			//diskAttach += " --comment \\\"" + disk.getComment() + "\\\";";
			
			units.add(new SimpleUnit(service + "_" + disk.getLabel() + "_disk_attached", service + "_dvds_ide_controller",
					diskAttach,
					"sudo -u " + user + " VBoxManage showvminfo " + service + " --machinereadable | grep \"DVDs-0-" + deviceCounter + "\"",
					"\\\"DVDs-" + deviceCounter + "-0\\\"=\\\"" + disk.getFilename() + "\\\"", "pass",
					"Couldn't attach disk " + disk.getLabel() + " for " + service + "."));

			deviceCounter++;
		};
		
		units.add(new DirUnit("log_dir_" + service, "proceed", logDir));
		units.add(new DirOwnUnit("log_dir_" + service, "log_dir_" + service + "_created", logDir, user, group));
		units.add(new DirPermsUnit("log_dir_" + service, "log_dir_" + service + "_chowned", logDir, "750"));

		units.add(new DirUnit("backup_dir_" + service, "proceed", backupTargetDir));
		units.add(new DirOwnUnit("backup_dir_" + service, "backup_dir_" + service + "_created", backupTargetDir, user, group));
		units.add(new DirPermsUnit("backup_dir_" + service, "backup_dir_" + service + "_chowned", backupTargetDir, "750"));
		// Mark the backup destination directory as a valid destination
		units.add(new FileUnit(service + "_mark_backup_dir", "backup_dir_" + service + "_chmoded",
				backupTargetDir + "/backup.marker", "In memoriam Luke and Guy.  Miss you two!"));

		units.add(new DirUnit("socket_dir_" + service, "proceed", ttySocketDir));
		units.add(new DirOwnUnit("socket_dir_" + service, "socket_dir_" + service + "_created", ttySocketDir, user,	group));
		units.add(new DirPermsUnit("socket_dir_" + service, "socket_dir_" + service + "_chowned", ttySocketDir, "750"));

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
				service + "_hdds_sas_controller"));
		units.add(modifyVm(service, user, "boot2", "dvd",
				"Couldn't set the boot order for " + service + ".  This may mean the service will not be installed.",
				service + "_dvds_ide_controller"));

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
