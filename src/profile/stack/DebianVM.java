package profile.stack;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import core.StringUtils;
import core.data.machine.UserDeviceData;
import core.data.machine.configuration.DiskData.Medium;
import core.exception.data.ADataException;
import core.exception.data.NoValidUsersException;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.ARuntimeException;
import core.exception.runtime.InvalidMachineModelException;
import core.exception.runtime.InvalidServerModelException;
import core.exec.network.APassphrase;
import core.exec.network.OpenKeePassPassphrase;
import core.iface.IUnit;
import core.model.machine.configuration.disks.ADiskModel;
import core.model.network.NetworkModel;
import core.unit.SimpleUnit;
import core.unit.fs.DirOwnUnit;
import core.unit.fs.DirPermsUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileChecksumUnit;
import core.unit.fs.FileChecksumUnit.Checksum;
import core.unit.fs.FileDownloadUnit;
import core.unit.fs.FileOwnUnit;
import core.unit.fs.FilePermsUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;

public class DebianVM extends Virtualbox{

	public DebianVM(String label, NetworkModel networkModel) {
		super(label, networkModel);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public Collection<IUnit> getInstalled() {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("metal_genisoimage", "proceed", "genisoimage"));
		units.add(new InstalledUnit("metal_rsync", "proceed", "rsync"));

		units.addAll(super.getInstalled());
		
		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(super.getPersistentFirewall());
		
		return units;
	}

	private Collection<IUnit> buildIso(String service) throws InvalidServerException, InvalidServerModelException, NoValidUsersException, MalformedURLException, URISyntaxException {

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

		units.add(getPreseedFile(new FileUnit("preseed_" + service, cleanedFilename + "_downloaded", isoDir + "/preseed.cfg"), service, true));
		units.add(new FileOwnUnit("preseed_" + service, "preseed_" + service, isoDir + "/preseed.cfg", "root"));
		units.add(new FilePermsUnit("preseed_" + service, "preseed_" + service + "_chowned", isoDir + "/preseed.cfg", "700"));

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

	private FileUnit getPreseedFile(FileUnit preseed, String service, Boolean expirePasswords) throws InvalidServerModelException, NoValidUsersException, MalformedURLException, URISyntaxException {
		String user            = getNetworkModel().getData().getUser();
		String sshDir          = "/home/" + user + "/.ssh";
		String pubKey          = getNetworkModel().getData().getSSHKey(user);
		String hostname        = StringUtils.stringToAlphaNumeric(service, "-");
		String domain          = getNetworkModel().getServerModel(service).getDomain().getHost();
		String fullName        = ((UserDeviceData) getNetworkModel().getData().getUserDevices().get(user)).getFullName();
		String debianMirror    = getNetworkModel().getData().getDebianMirror(service).getHost();
		String debianDirectory = getNetworkModel().getData().getDebianDirectory(service);

		//Set up new box before rebooting. Sometimes you need to echo out in chroot;
		//in-target just doesn't work reliably for many things (most likely due to the shell it uses) :(
		preseed.appendLine("d-i preseed/late_command string", false);
		//Echo out public keys, make sure it's all secured properly
		preseed.appendLine("\tin-target mkdir " + sshDir + ";", false);
		preseed.appendLine("\tin-target touch " + sshDir + "/authorized_keys;", false);
		preseed.appendLine("\techo \\\"echo '" + pubKey + "' >> " + sshDir + "/authorized_keys; \\\" | chroot /target /bin/bash;", false);
		
		preseed.appendLine("\tin-target chmod 700 " + sshDir + ";", false);
		preseed.appendLine("\tin-target chmod 400 " + sshDir + "/authorized_keys;", false);
		preseed.appendLine("\tin-target chown -R " + user + ":" + user + " " + sshDir + ";", false);
		
		if (expirePasswords) {
			//Force the user to change their passphrase on first login if they haven't set a passwd
			preseed.appendLine("\tin-target passwd -e " + user + ";", false);
		}
		
		//Lock the root account
		preseed.appendLine("\tin-target passwd -l root;", false);
		
		//Change the SSHD to be on the expected port
		preseed.appendLine("\tin-target sed -i 's/#Port 22/Port " + getNetworkModel().getData().getSSHPort(service) + "/g' /etc/ssh/sshd_config;");
		
		//Debian installer options.
		preseed.appendLine("d-i debian-installer/locale string en_GB.UTF-8");
		preseed.appendLine("d-i keyboard-configuration/xkb-keymap select gb");
		preseed.appendLine("d-i netcfg/target_network_config select ifupdown");
		preseed.appendLine("d-i netcfg/choose_interface select auto");
		preseed.appendLine("d-i netcfg/get_hostname string " + hostname);
		preseed.appendLine("d-i netcfg/get_domain string " + domain);
		preseed.appendLine("d-i netcfg/hostname string " + hostname);
		preseed.appendLine("d-i mirror/country string manual");
		preseed.appendLine("d-i mirror/http/hostname string " + debianMirror);
		preseed.appendLine("d-i mirror/http/directory string " + debianDirectory);
		preseed.appendLine("d-i mirror/http/proxy string ");
		preseed.appendLine("d-i passwd/root-password-crypted password ${" + service.toUpperCase() + "_PASSWORD}");
		preseed.appendLine("d-i passwd/user-fullname string " + fullName);
		preseed.appendLine("d-i passwd/username string " + user);
		preseed.appendLine("d-i passwd/user-password-crypted password ${" + service.toUpperCase() + "_PASSWORD}");
		preseed.appendLine("d-i passwd/user-default-groups string sudo");
		preseed.appendLine("d-i clock-setup/utc boolean true");
		preseed.appendLine("d-i time/zone string Europe/London");
		preseed.appendLine("d-i clock-setup/ntp boolean true");
		preseed.appendLine("d-i partman-auto/disk string /dev/sda");
		preseed.appendLine("d-i grub-installer/bootdev string /dev/sda");
		preseed.appendLine("d-i partman-auto/method string regular");
		preseed.appendLine("d-i partman-auto/choose_recipe select atomic");
		preseed.appendLine("d-i partman-partitioning/confirm_write_new_label boolean true");
		preseed.appendLine("d-i partman/choose_partition select finish");
		preseed.appendLine("d-i partman/confirm boolean true");
		preseed.appendLine("d-i partman/confirm_nooverwrite boolean true");
		preseed.appendLine("iptasksel tasksel/first multiselect none");
		preseed.appendLine("d-i apt-setup/cdrom/set-first boolean false");
		preseed.appendLine("d-i apt-setup/cdrom/set-next boolean false");
		preseed.appendLine("d-i apt-setup/cdrom/set-failed boolean false");
		preseed.appendLine("d-i pkgsel/include string sudo openssh-server dkms gcc bzip2");
		preseed.appendLine("openssh-server openssh-server/permit-root-login boolean false");
		preseed.appendLine("discover discover/install_hw_packages multiselect virtualbox-ose-guest-x11");
		preseed.appendLine("popularity-contest popularity-contest/participate boolean false");
		preseed.appendLine("d-i grub-installer/only_debian boolean true");
		preseed.appendLine("d-i grub-installer/with_other_os boolean false");
		preseed.appendLine("d-i grub-installer/bootdev string default");
		preseed.appendLine("d-i finish-install/reboot_in_progress note");

		return preseed;
	}

	private Collection<IUnit> getUserPasswordUnits(String service) throws ARuntimeException, ADataException {
		final Collection<IUnit> units = new ArrayList<>();

		String password = "";
		final APassphrase pass = new OpenKeePassPassphrase(service, getNetworkModel());

		if (pass.init()) {
			password = pass.getPassphrase();
			password = Pattern.quote(password); // Turn special characters into literal so they don't get parsed out
			password = password.substring(2, password.length() - 2).trim(); // Remove '\Q' and '\E' from beginning/end since we're not using this as a regex
			password = password.replace("\"", "\\\""); // Also, make sure quote marks are properly escaped!
		}
		else {
			password = ((UserDeviceData)getNetworkModel().getData().getUserDevices().get(getNetworkModel().getData().getUser())).getDefaultPassphrase();
		}
		
		if (password == null || password.isEmpty()) {
			password = service;
		}

		//if (pass.isADefaultPassphrase()) {
		//}

		units.add(new SimpleUnit(service + "_password", "proceed",
				service.toUpperCase() + "_PASSWORD=$(printf \"" + password + "\" | mkpasswd -s -m md5)",
				"echo $" + service.toUpperCase() + "_PASSWORD", "", "fail",
				"Couldn't set the passphrase for " + service + "."
				+ " You won't be able to configure this service."));
		
		return units;
	}
	
	@Override
	public Collection<IUnit> buildServiceVm(String service, String bridge)
			throws InvalidServerException, InvalidMachineModelException {
		final String baseDir = getNetworkModel().getData().getHypervisorThornsecBase(getLabel());

		final String logDir = baseDir + "/logs/" + service;
		final String backupDir = baseDir + "/backups/" + service;
		final String ttySocketDir = baseDir + "/sockets/" + service;

		final String installIso = baseDir + "/isos/" + service + "/" + service + ".iso";
		final String user = "vboxuser_" + service;
		final String group = "vboxusers";
		final String osType = getNetworkModel().getData().getDebianIsoUrl(service).contains("amd64") ? "Debian_64"
				: "Debian";

		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(getISODownloadUnits(getNetworkModel().getData().getDebianIsoUrl(service), getNetworkModel().getData().getDebianIsoSha512(service)));
		try {
			units.addAll(getUserPasswordUnits(service));
		} catch (ARuntimeException | ADataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		units.addAll(super.buildServiceVm(service, bridge));

		// Boot setup - DVD is second to stop machines being wiped every time they're
		// brought up
		units.add(modifyVm(service, user, "boot1", "disk",
				"Couldn't set the boot order for " + service + ".  This may mean the service will not be installed.",
				service + "_hdds_sas_controller"));
		units.add(modifyVm(service, user, "boot2", "dvd",
				"Couldn't set the boot order for " + service + ".  This may mean the service will not be installed.",
				service + "_dvds_ide_controller"));

		return units;
	}
	
	private Collection<IUnit> getISODownloadUnits(String url, String checksum) throws InvalidServerException {
		final Collection<IUnit> units = new ArrayList<>();
	
		String filename = null;
		String cleanedFilename = null;
	
		try {
			filename = Paths.get(new URI(url).getPath()).getFileName().toString();
			cleanedFilename = StringUtils.stringToAlphaNumeric(filename, "_");
		}
		catch (final Exception e) {
			JOptionPane.showMessageDialog(null, "It doesn't appear that " + url + " is a valid link to a Debian ISO.\n\nPlease fix this in your JSON");
			System.exit(1);
		}
	
		units.add(new FileDownloadUnit(cleanedFilename, "metal_genisoimage_installed", url,
				getNetworkModel().getData().getHypervisorThornsecBase(getLabel()) + "/" + filename,
				"The Debian net install ISO couldn't be downloaded.  Please check the URI in your config."));
		units.add(new FileChecksumUnit(cleanedFilename, cleanedFilename + "_downloaded", Checksum.SHA512,
				getNetworkModel().getData().getHypervisorThornsecBase(getLabel()) + "/" + filename, checksum,
				"The sha512 sum of the Debian net install in your config doesn't match what has been downloaded."
				+ " This could mean your connection is man-in-the-middle'd, that the download was corrupted,"
				+ " or it could just be that the file has been updated on the server."
				+ " Please check http://cdimage.debian.org/debian-cd/current/amd64/iso-cd/SHA512SUMS (64 bit)"
				+ " or http://cdimage.debian.org/debian-cd/current/i386/iso-cd/SHA512SUMS (32 bit) for the correct checksum."));
		
		return units;
	}

}
