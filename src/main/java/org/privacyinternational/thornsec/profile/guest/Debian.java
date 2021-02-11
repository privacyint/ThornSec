package org.privacyinternational.thornsec.profile.guest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.privacyinternational.thornsec.core.exception.AThornSecException;
import org.privacyinternational.thornsec.core.exception.data.ADataException;
import org.privacyinternational.thornsec.core.exception.data.NoValidUsersException;
import org.privacyinternational.thornsec.core.exception.data.machine.InvalidServiceException;
import org.privacyinternational.thornsec.core.exception.data.machine.InvalidUserException;
import org.privacyinternational.thornsec.core.exception.runtime.ARuntimeException;
import org.privacyinternational.thornsec.core.exception.runtime.InvalidGuestOSException;
import org.privacyinternational.thornsec.core.exception.runtime.InvalidMachineModelException;
import org.privacyinternational.thornsec.core.exec.network.APassphrase;
import org.privacyinternational.thornsec.core.exec.network.OpenKeePassPassphrase;
import org.privacyinternational.thornsec.core.iface.IUnit;
import org.privacyinternational.thornsec.core.model.machine.ServerModel;
import org.privacyinternational.thornsec.core.model.network.UserModel;
import org.privacyinternational.thornsec.core.unit.SimpleUnit;
import org.privacyinternational.thornsec.core.unit.fs.FileUnit;
import org.privacyinternational.thornsec.core.unit.pkg.InstalledUnit;
import inet.ipaddr.HostName;
import org.privacyinternational.thornsec.profile.machine.configuration.AptSources;
import org.privacyinternational.thornsec.profile.service.machine.SSH;

class PreseedFile extends FileUnit {
	
	private Set<String> lateCommand;
	private Set<String> debianInstaller;
	
	public PreseedFile() {
		super("preseed", "proceed", null);
		this.lateCommand = new LinkedHashSet<String>();
		this.debianInstaller = new LinkedHashSet<String>();
	}

	private void buildSuper() {
		super.setLines(new ArrayList<>());
		super.appendLine(lateCommand);
		super.appendLine(debianInstaller);
	}
	
	public void putLateCommand(String command) {
		this.lateCommand.add(command);
		
		buildSuper();
	}
	
	protected Set<String> getLateCommand() {
		return this.lateCommand;
	}
	
	public void putDebianInstallSetting(String setting, String type, String value) {
		this.debianInstaller.add("d-i " + setting + " " + type + " " + value);
		
		buildSuper();
	}
	
	protected Set<String> getDebianInstallerSettings() {
		return this.debianInstaller;
	}
	
	public void setPath(String path) {
		super.setPath(path);
	}
}

public class Debian extends AOS {

	private final AptSources aptSources;
	private PreseedFile preseed;
	
	public Debian(ServerModel me) throws AThornSecException {
		super(me);

		this.aptSources = new AptSources(me);
		this.preseed = new PreseedFile();
	}
	
	@Override
	public Collection<IUnit> getInstalled() throws AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		// Should we be autoupdating?
		String aptCommand = "";
		if (getServerModel().getAutoUpdate()) {
			aptCommand = "sudo apt-get --assume-yes upgrade;";
		} else {
			aptCommand = "echo \"There are $(sudo apt-get upgrade -s | grep -P '^\\d+ upgraded'| cut -d' ' -f1) updates available, of which $(sudo apt-get upgrade -s | grep ^Inst | grep Security | wc -l) are security updates\"";
		}
		units.add(new SimpleUnit("update", "proceed", aptCommand,
				"sudo apt-get update > /dev/null; sudo apt-get --assume-no upgrade | grep \"[0-9] upgraded, [0-9] newly installed, [0-9] to remove and [0-9] not upgraded.\";",
				"0 upgraded, 0 newly installed, 0 to remove and 0 not upgraded.", "pass",
				"There are $(sudo apt-get upgrade -s | grep -P '^\\d+ upgraded'| cut -d' ' -f1) updates available, of which $(sudo apt-get upgrade -s | grep ^Inst | grep Security | wc -l) are security updates"));

		final SSH ssh = new SSH((ServerModel)getMachineModel());
		units.addAll(ssh.getUnits());

		// Useful packages
		units.add(new InstalledUnit("sysstat", "proceed", "sysstat"));
		units.add(new InstalledUnit("lsof", "proceed", "lsof"));
		units.add(new InstalledUnit("net_tools", "proceed", "net-tools"));
		units.add(new InstalledUnit("htop", "proceed", "htop"));

		// Before we go any further... now the machine is at least up to date, and has a
		// couple of useful diagnostics packages installed...
		getMachineModel().getNetworkInterfaces().forEach(nic -> {
			nic.getNetworkFile().ifPresent(file -> units.add(file));
			nic.getNetDevFile().ifPresent(file -> units.add(file));
		});

		units.addAll(this.aptSources.getUnits());

		units.addAll(super.getInstalled());
		
		units.add(new SimpleUnit("apt_autoremove", "proceed", "sudo apt-get autoremove --purge --assume-yes", "sudo apt-get autoremove --purge --assume-no | grep \"0 to remove\"",
				"", "fail"));
		
		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		getMachineModel().addEgress(new HostName(getPackageMirror() + ":80"));
		getMachineModel().addEgress(new HostName("cdn.debian.net:80"));
		getMachineModel().addEgress(new HostName("security-cdn.debian.org:80"));
		getMachineModel().addEgress(new HostName("prod.debian.map.fastly.net:80"));
		getMachineModel().addEgress(new HostName("cdn.debian.net:443"));
		getMachineModel().addEgress(new HostName("security-cdn.debian.org:443"));
		getMachineModel().addEgress(new HostName("prod.debian.map.fastly.net:443"));

		return units;
	}

	public AptSources getAptSourcesModel() {
		return this.aptSources;
	}

	@Override
	public Collection<IUnit> buildIso() throws InvalidServiceException, InvalidUserException, NoValidUsersException, InvalidMachineModelException {

		final Collection<IUnit> units = new ArrayList<>();

//		final String baseDir = getServerModel().getHypervisorModel().getVMBase().getAbsolutePath();
//		final String isoDir = baseDir + "/iso/" + getServerModel().getLabel();
//		String filename = null;
//		String checksum = null;
//		String cleanedFilename = null;
//
//		try {
//			URI isoURL = new URI(getServerModel()
//									.getIsoUrl()
//									.orElseGet(() ->
//										getIsoURLFromLatest()
//									)
//								);
//			
//			filename = Path.of(isoURL).getFileName().toString();
//			cleanedFilename = StringUtils.stringToAlphaNumeric(filename, "_");
//		} catch (final Exception e) {
//			JOptionPane.showMessageDialog(null, "You shouldn't have been able to arrive here. Well done!");
//			System.exit(1);
//		}
//
//		units.add(new DirUnit("iso_dir_" + getServerModel().getLabel(), "proceed", isoDir));
//
//		units.add(getPreseedFile());
//
//		String buildIso = "\n";
//		buildIso += "\tsudo bash -c '\n";
//		// Create a working copy of the iso for preseeding
//		buildIso += "\t\tcd " + isoDir + ";\n";
//		buildIso += "\t\tmkdir loopdir;\n";
//		buildIso += "\t\tmount -o loop " + baseDir + "/" + filename + " loopdir;\n";
//		buildIso += "\t\tmkdir cd;\n";
//		buildIso += "\t\trsync -a -H --exclude=TRANS.TBL loopdir/ cd;\n";
//		buildIso += "\t\tumount loopdir;\n";
//		buildIso += "\t\tcd cd;\n";
//		// Add our preseed directly to the initrd as per https://wiki.debian.org/DebianInstaller/Preseed/EditIso
//		buildIso += "\t\tgunzip install.*/initrd.gz;\n";
//		buildIso += "\t\techo ../preseed.cfg | cpio -H newc -o -A -F install.*/initrd;\n";
//		buildIso += "\t\tgzip install.*/initrd;\n";
//		// Set the menu timeout to 1 second, otherwise it waits for user input
//		buildIso += "\t\tsed -i \"s/timeout 0/timeout 1/g\" isolinux/isolinux.cfg;\n";
//		// Switch off default menu
//		buildIso += "\t\tsed -i \"s/^default/#default/g\" isolinux/isolinux.cfg;\n";
//		// Switch off vga and add console to *all* boot lines
//		buildIso += "\t\tsed -i \"s_vga=788_vga=none console=ttyS0,115200n8_g\" isolinux/*.cfg;\n";
//		// Rebuild md5sums to reflect changes
//		buildIso += "\t\tmd5sum `find -follow -type f` > md5sum.txt;\n";
//		buildIso += "\t' > /dev/null 2>&1;\n";
//		// Create our new preseeded image
//		buildIso += "\tsudo bash -c '\n";
//		buildIso += "\t\tcd " + isoDir + ";\n";
//		buildIso += "\t\tgenisoimage -o " + getServerModel().getLabel() + ".iso -r -J -no-emul-boot -boot-load-size 4 -boot-info-table -b isolinux/isolinux.bin -c isolinux/boot.cat ./cd;\n";
//		buildIso += "\t\trm -R cd loopdir;\n";
//		buildIso += "\t'";
//
//		units.add(new SimpleUnit("build_iso_" + getServerModel().getLabel(), cleanedFilename + "_downloaded",
//				buildIso, "test -f " + isoDir + "/" + getServerModel().getLabel() + ".iso && echo 'pass' || echo 'fail'", "pass", "pass",
//				"Couldn't create the install ISO for " + getServerModel().getLabel() + ".  This service won't be able to install."));

		return units;
	}

	protected String setIsoURLFromLatest() {
		return null;
		
	}

	private Collection<IUnit> getUserPasswordUnits() throws ARuntimeException, ADataException {
		final Collection<IUnit> units = new ArrayList<>();

		String password = "";
		final APassphrase pass = new OpenKeePassPassphrase(getServerModel());

		if (pass.init()) {
			password = pass.getPassphrase();
			password = Pattern.quote(password); // Turn special characters into literal so they don't get parsed out
			password = password.substring(2, password.length() - 2).trim(); // Remove '\Q' and '\E' from beginning/end since we're not using this as a regex
			password = password.replace("\"", "\\\""); // Also, make sure quote marks are properly escaped!
		}
		else {
			password = getNetworkModel().getConfigUserModel().getDefaultPassphrase().orElse(null);
		}
		
		if (password == null || password.isEmpty()) {
			password = getServerModel().getLabel();
		}

		//if (pass.isADefaultPassphrase()) {
		//}

		units.add(new SimpleUnit(getServerModel().getLabel() + "_password", "proceed",
				getServerModel().getLabel().toUpperCase() + "_PASSWORD=$(printf \"" + password + "\" | mkpasswd -s -m md5)",
				"echo $" + getServerModel().getLabel().toUpperCase() + "_PASSWORD", "", "fail",
				"Couldn't set the passphrase for " + getServerModel().getLabel() + "."
				+ " You won't be able to configure this service."));
		
		return units;
	}
	

	private FileUnit getPreseedFile() throws InvalidUserException, NoValidUsersException, InvalidGuestOSException {
		//this.preseed = new FileUnit("preseed_" + getServerModel().getLabel(), cleanedFilename + "_downloaded", isoDir + "/preseed.cfg", "root", "root", 0700, "");
		
		String username = getNetworkModel().getData().getUser();
		UserModel user = getNetworkModel().getUser(username)
							.orElseThrow(() -> new InvalidUserException(username));

		String domain = getServerModel().getDomain().getHost();
		String mirror = getServerModel().getPackageMirror();
		String mirrorDirectory = getServerModel().getPackageDirectory();

		buildPreseedLateCommand(user);
		
			//Force the user to change their passphrase on first login if they haven't set a passwd
			preseed.appendText("\tin-target passwd -e " + user + ";");
		

		
		//Change the SSHD to be on the expected port
		
		//Debian installer options.
		preseed.appendLine("d-i debian-installer/locale string en_GB.UTF-8");
		preseed.appendLine("d-i keyboard-configuration/xkb-keymap select gb");
		preseed.appendLine("d-i netcfg/target_network_config select ifupdown");
		preseed.appendLine("d-i netcfg/choose_interface select auto");
		preseed.appendLine("d-i netcfg/get_hostname string " + getServerModel().getHostName());
		preseed.appendLine("d-i netcfg/get_domain string " + getServerModel().getDomain().getHost());
		preseed.appendLine("d-i netcfg/hostname string " + getServerModel().getHostName());
		preseed.appendLine("d-i mirror/country string manual");
		preseed.appendLine("d-i mirror/http/hostname string " + mirror);
		preseed.appendLine("d-i mirror/http/directory string " + mirrorDirectory);
		preseed.appendLine("d-i mirror/http/proxy string ");
		preseed.appendLine("d-i passwd/root-password-crypted password ${" + getServerModel().getLabel().toUpperCase() + "_PASSWORD}");
		preseed.appendLine("d-i passwd/user-fullname string " + user.getFullName());
		preseed.appendLine("d-i passwd/username string " + user.getUsername());
		preseed.appendLine("d-i passwd/user-password-crypted password ${" + getServerModel().getLabel().toUpperCase() + "_PASSWORD}");
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
		//preseed.appendLine("discover discover/install_hw_packages multiselect virtualbox-ose-guest-x11");
		preseed.appendLine("popularity-contest popularity-contest/participate boolean false");
		preseed.appendLine("d-i grub-installer/only_debian boolean true");
		preseed.appendLine("d-i grub-installer/with_other_os boolean false");
		preseed.appendLine("d-i grub-installer/bootdev string default");
		preseed.appendLine("d-i finish-install/reboot_in_progress note");

		return preseed;
	}

	/**
	 * @param user
	 * @param sshDir
	 * @param pubKey
	 * @throws InvalidUserException 
	 */
	private void buildPreseedLateCommand(UserModel user) throws InvalidUserException {
		//Set up new box before rebooting. Sometimes you need to echo out in chroot;
		//in-target just doesn't work reliably for many things (most likely due to the shell it uses) :(
		preseed.appendText("d-i preseed/late_command string");
		getLateCommandUserSSHKeySettings(user);
		getPreseedRootAccountSettings();
	}

	/**
	 * @param preseed
	 * @param user
	 * @param sshDir
	 * @param pubKey
	 * @return 
	 * @throws InvalidUserException 
	 */
	private void getLateCommandUserSSHKeySettings(UserModel user) throws InvalidUserException {
		String sshDir = user.getHomeDirectory() + "/.ssh";
		String pubKey = user.getSSHPublicKey()
							.orElseThrow(()
									-> new InvalidUserException(user.getUsername()
										+ " doesn't have an SSH public key")
							);

		preseed.appendText("in-target mkdir " + sshDir + ";");
		preseed.appendText("in-target touch " + sshDir + "/authorized_keys;");
		preseed.appendText("echo \\\"echo '" + pubKey + "' >> " + sshDir + "/authorized_keys; \\\" | chroot /target /bin/bash;");
		
		preseed.appendText("in-target chmod 700 " + sshDir + ";");
		preseed.appendText("in-target chmod 400 " + sshDir + "/authorized_keys;");
		preseed.appendText("in-target chown -R root:root " + sshDir + ";");
	}
	
	private void getPreseedRootAccountSettings() {
		// TODO Auto-generated method stub
	}

	
	@Override
	protected String getIsoURLFromLatest() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String getIsoSHA512FromLatest() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPackageMirror() {
		return getServerModel().getData()
					.getPackageMirror()
					.orElseGet(() -> "mirrorservice.org");
	}

	@Override
	public String getPackageDirectory() {
		return getServerModel().getData().getPackageMirrorDirectory().orElseGet(() -> "/sites/ftp.debian.org/debian");
	}
}
