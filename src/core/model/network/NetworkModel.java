/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.network;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import core.data.network.NetworkData;
import core.exception.runtime.InvalidDeviceModelException;
import core.exception.runtime.InvalidMachineModelException;
import core.exception.runtime.InvalidServerModelException;
import core.exec.ManageExec;
import core.exec.network.OpenKeePassPassphrase;
import core.iface.IUnit;
import core.model.machine.ADeviceModel;
import core.model.machine.AMachineModel;
import core.model.machine.ExternalOnlyDeviceModel;
import core.model.machine.InternalOnlyDeviceModel;
import core.model.machine.ServerModel;
import core.model.machine.ServiceModel;

public class NetworkModel {
	public enum MachineType {
		ROUTER, SERVER, HYPERVISOR, DEDICATED, SERVICE, DEVICE, USER, INTERNAL_ONLY, EXTERNAL_ONLY
	}

	private final String label;
	private NetworkData data;
	private final Hashtable<MachineType, Hashtable<String, AMachineModel>> machines;
	private final Hashtable<String, Set<IUnit>> units;

	NetworkModel(String label) {
		this.label = label;

		this.machines = new Hashtable<>();

		this.units = new Hashtable<>();
	}

	final public String getLabel() {
		return this.label;
	}

	void init() {
		// for (AMachineData machine : data.)
//
//
//
//		//Create (and classify) all devices
////		for (String device : data.getAllDeviceLabels()) {
//			ADeviceModel deviceModel = null;
//			deviceModel.setData(this.data);
//
//			switch (deviceModel) {
//				case "User":
//					userDevices.add(deviceModel);
//					break;
//				case "Internal":
//					intOnlyDevices.add(deviceModel);
//					break;
//				case "External":
//					extOnlyDevices.add(deviceModel);
//					break;
//				default:
//					//In theory, we should never get here. Theory is a fine thing.
//					System.out.println("Encountered an unsupported device type for " + device);
//			}
//		}
//
//
//		//Now everything is classified and init()ed, get their networking requirements
//		//We want to do this in a certain order - backwards through the network
//		for(DeviceModel device : devices.values()) {
//			device.getNetworking();
//		}
//
//		//Allow external-only && users to call out to the web
//		for (DeviceModel device : userDevices) {
//			device.addRequiredEgress("255.255.255.255", 0);
//		}
//
//		for (DeviceModel device : extOnlyDevices) {
//			device.addRequiredEgress("255.255.255.255", 0);
//		}
//
//		for(ServerModel service : services) {
//			service.getNetworking();
//		}
//
//		for(ServerModel metal : metals) {
//			if (metal.isRouter()) { //If it's an external server...
//				continue; //Skip it, otherwise we'll be duplicating its ifaces!
//			}
//			metal.getNetworking();
//		}
//
//		for(ServerModel dedi : dedis) {
//			dedi.getNetworking();
//		}
//
//		//Now populate our ipsets before building our Router
//		this.ipsets.init();
//
//		for(ServerModel router : routers) { //This will also catch metal/routers
//			router.getNetworking();
//		}
//
//		//Finally, get all of the config units
//		for (MachineModel machine : getAllMachines()) {
//			units.put(machine.getLabel(), machine.getUnits());
//		}

	}

	public final Hashtable<MachineType, Hashtable<String, AMachineModel>> getAllMachineModels() {
		return this.machines;
	}

	public final Hashtable<String, ServerModel> getAllServerModels() {
		final Hashtable<String, ServerModel> servers = new Hashtable<>();

		for (final AMachineModel machine : getAllMachineModels().get(MachineType.SERVER).values()) {
			servers.put(machine.getLabel(), (ServerModel) machine);
		}

		return servers;
	}

	public final Hashtable<String, ADeviceModel> getAllUserDeviceModels() {
		final Hashtable<String, ADeviceModel> devicen = new Hashtable<>();

		for (final AMachineModel device : getAllMachineModels().get(MachineType.USER).values()) {
			devicen.put(device.getLabel(), (ADeviceModel) device);
		}

		return devicen;
	}

	public final Hashtable<String, InternalOnlyDeviceModel> getAllInternalOnlyDeviceModels() {
		final Hashtable<String, InternalOnlyDeviceModel> devicen = new Hashtable<>();

		for (final AMachineModel device : getAllMachineModels().get(MachineType.INTERNAL_ONLY).values()) {
			devicen.put(device.getLabel(), (InternalOnlyDeviceModel) device);
		}

		return devicen;
	}

	public final Hashtable<String, ExternalOnlyDeviceModel> getAllExternalOnlyDeviceModels() {
		final Hashtable<String, ExternalOnlyDeviceModel> devicen = new Hashtable<>();

		for (final AMachineModel device : getAllMachineModels().get(MachineType.INTERNAL_ONLY).values()) {
			devicen.put(device.getLabel(), (ExternalOnlyDeviceModel) device);
		}

		return devicen;
	}

	public final AMachineModel getMachineModel(String machine) throws InvalidMachineModelException {
		final Hashtable<MachineType, Hashtable<String, AMachineModel>> allMachines = this.getAllMachineModels();

		for (final Hashtable<String, AMachineModel> machines : allMachines.values()) {
			if (machines.containsKey(machine)) {
				return machines.get(machine);
			}
		}

		throw new InvalidMachineModelException();
	}

	public final ServerModel getServerModel(String server) throws InvalidServerModelException {
		if (this.getAllServerModels().containsKey(server)) {
			return this.getAllServerModels().get(server);
		}

		throw new InvalidServerModelException();
	}

	public final ADeviceModel getDeviceModel(String device) throws InvalidDeviceModelException {

		ADeviceModel model = null;

		if (this.getAllUserDeviceModels().containsKey(device)) {
			model = this.getAllUserDeviceModels().get(device);
		} else if (this.getAllInternalOnlyDeviceModels().containsKey(device)) {
			model = this.getAllInternalOnlyDeviceModels().get(device);
		} else if (this.getAllExternalOnlyDeviceModels().containsKey(device)) {
			model = this.getAllExternalOnlyDeviceModels().get(device);
		} else {
			throw new InvalidDeviceModelException();
		}

		return model;
	}

	public final void auditNonBlock(String server, OutputStream out, InputStream in, boolean quiet)
			throws InvalidServerModelException {
		final ManageExec exec = getManageExec(server, "audit", out, quiet);
		if (exec != null) {
			exec.manage();
		}
	}

	public final void auditAll(OutputStream out, InputStream in, boolean quiet) throws InvalidServerModelException {
		for (final String server : this.getAllServerModels().keySet()) {
			final ManageExec exec = getManageExec(server, "audit", out, quiet);
			if (exec != null) {
				exec.manage();
			}
		}
	}

	public final void configNonBlock(String server, OutputStream out, InputStream in)
			throws InvalidServerModelException {
		final ManageExec exec = getManageExec(server, "config", out, false);
		if (exec != null) {
			exec.manage();
		}
	}

	public final void dryrunNonBlock(String server, OutputStream out, InputStream in)
			throws InvalidServerModelException {
		final ManageExec exec = getManageExec(server, "dryrun", out, false);
		if (exec != null) {
			exec.manage();
		}
	}

	private final ManageExec getManageExec(String server, String action, OutputStream out, boolean quiet)
			throws InvalidServerModelException {
		// need to do a series of local checks eg known_hosts or expected
		// fingerprint
		final OpenKeePassPassphrase pass = new OpenKeePassPassphrase(server, this);

		final String audit = getScript(server, action, quiet);

		if (action.equals("dryrun")) {
			try {
				final Date now = new Date();
				final PrintWriter wr = new PrintWriter(
						new FileOutputStream("./" + server + "_" + now.toString() + ".sh"));
				wr.write(audit);
				wr.flush();
				wr.close();
			} catch (final FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		if (pass.isADefaultPassphrase()) {
			System.out.println("FAIL: no password in keychain for " + server);
			System.out.println("Using the default password instead (this almost certainly won't work!)");
			return null;
		}

		// ManageExec exec = new ManageExec(this.getData().getUser(),
		// pass.getPassphrase(), serverModel.getIP(), this.getData().getSSHPort(server),
		// audit, out);
		final ManageExec exec = new ManageExec(getServerModel(server), this, audit, out);
		return exec;
	}

	private String getScript(String server, String action, boolean quiet) {
		// TODO Auto-generated method stub
		return null;
	}

	public void setData(NetworkData data) {
		this.data = data;
	}

	public NetworkData getData() {
		return this.data;
	}

	// @TODO: This!
	/*
	 * public void genIsoServer(String server, String dir) { String currentUser =
	 * getData().getUser(); String sshDir = "/home/" + currentUser + "/.ssh"; String
	 * sshKey = getData().getUserSSHKey(currentUser);
	 *
	 * String preseed = ""; preseed += "d-i preseed/late_command staring"; preseed
	 * += "	in-target mkdir " + sshDir + ";"; preseed += "    in-target touch " +
	 * sshDir + "/authorized_keys;"; preseed += "	echo \\\"echo \\\'" + sshKey +
	 * "\\\' >> " + sshDir + "/authorized_keys; \\\" | chroot /target /bin/bash;";
	 *
	 * preseed += "	in-target chmod 700 " + sshDir + ";"; preseed +=
	 * "	in-target chmod 400 " + sshDir + "/authorized_keys;"; preseed +=
	 * "	in-target chown -R " + currentUser + ":" + currentUser + " " + sshDir +
	 * ";"; //Force the user to change their passphrase on first login, lock the
	 * root account preseed += "	in-target passwd -e " + currentUser + ";";
	 * preseed += "	in-target passwd -l root;\n";
	 *
	 * preseed += "d-i debian-installer/locale string en_GB.UTF-8\n"; preseed +=
	 * "d-i keyboard-configuration/xkb-keymap select uk\n"; preseed +=
	 * "d-i netcfg/target_network_config select ifupdown\n"; if
	 * (getData().getExtConnectionType(server) != null &&
	 * getData().getExtConnectionType(server).equals("static")) { preseed +=
	 * "d-i netcfg/disable_dhcp true\n"; preseed +=
	 * "d-i netcfg/choose_interface select " + getData().getWanIfaces(server) +
	 * "\n"; preseed += "d-i netcfg/disable_autoconfig boolean true\n"; preseed +=
	 * "d-i netcfg/get_ipaddress string " + getData().getProperty(server,
	 * "externaladdress", true) + "\n"; preseed += "d-i netcfg/get_netmask string "
	 * + getData().getProperty(server, "externalnetmask", true) + "\n"; preseed +=
	 * "d-i netcfg/get_gateway string " + getData().getProperty(server,
	 * "externalgateway", true) + "\n"; preseed +=
	 * "d-i netcfg/get_nameservers string " + getData().getDNS()[0] + "\n"; //Use
	 * the first DNS server preseed += "d-i netcfg/confirm_static boolean true\n"; }
	 * else { preseed += "d-i netcfg/choose_interface select auto\n"; } preseed +=
	 * "d-i netcfg/get_hostname string " + server + "\n"; preseed +=
	 * "d-i netcfg/get_domain string " + getData().getDomain(server) + "\n"; preseed
	 * += "d-i netcfg/hostname string " + server + "\n"; //preseed +=
	 * "d-i hw-detect/load_firmware boolean true\n"; //Always try to load non-free
	 * firmware preseed += "d-i mirror/country string GB\n"; preseed +=
	 * "d-i mirror/http/mirror string " + getData().getDebianMirror(server) + "\n";
	 * preseed += "d-i mirror/http/directory string /debian\n"; preseed +=
	 * "d-i mirror/http/proxy string\n"; preseed +=
	 * "d-i passwd/root-password password secret\n"; preseed +=
	 * "d-i passwd/root-password-again password secret\n"; preseed +=
	 * "d-i passwd/user-fullname string " + getData().getUserFullName(currentUser) +
	 * "\n"; preseed += "d-i passwd/username string " + currentUser + "\n"; preseed
	 * += "d-i passwd/user-password password secret\n"; preseed +=
	 * "d-i passwd/user-password-again password secret\n"; preseed +=
	 * "d-i passwd/user-default-groups string sudo\n"; preseed +=
	 * "d-i clock-setup/utc boolean true\n"; preseed +=
	 * "d-i time/zone string Europe/London\n"; preseed +=
	 * "d-i clock-setup/ntp boolean true\n"; preseed +=
	 * "d-i partman-auto/disk string /dev/sda\n"; preseed +=
	 * "d-i partman-auto/method string regular\n"; preseed +=
	 * "d-i partman-auto/purge_lvm_from_device boolean true\n"; preseed +=
	 * "d-i partman-lvm/device_remove_lvm boolean true\n"; preseed +=
	 * "d-i partman-md/device_remove_md boolean true\n"; preseed +=
	 * "d-i partman-lvm/confirm boolean true\n"; preseed +=
	 * "d-i partman-auto/choose_recipe select atomic\n"; preseed +=
	 * "d-i partman-partitioning/confirm_write_new_label boolean true\n"; preseed +=
	 * "d-i partman/choose_partition select finish\n"; preseed +=
	 * "d-i partman/confirm boolean true\n"; preseed +=
	 * "d-i partman/confirm_nooverwrite boolean true\n"; preseed +=
	 * "tasksel tasksel/first mualtiselect none\n"; preseed +=
	 * "d-i pkgsel/include string sudo openssh-server dkms gcc bzip2\n"; preseed +=
	 * "d-i preseed/late_command string sed -i '/^deb cdrom:/s/^/#/' /target/etc/apt/sources.list\n"
	 * ; preseed += "d-i apt-setup/use_mirror boolean false\n"; preseed +=
	 * "d-i apt-setup/cdrom/set-first boolean false\n"; preseed +=
	 * "d-i apt-setup/cdrom/set-next boolean false\n"; preseed +=
	 * "d-i apt-setup/cdrom/set-failed boolean false\n"; preseed +=
	 * "popularity-contest popularity-contest/participate boolean false\n"; preseed
	 * += "d-i grub-installer/only_debian boolean true\n"; preseed +=
	 * "d-i grub-installer/with_other_os boolean true\n"; preseed +=
	 * "d-i grub-installer/bootdev string default\n"; preseed +=
	 * "d-i finish-install/reboot_in_progress note";
	 *
	 * String script = "#!/bin/bash\n"; script += "cd " + dir + "\n"; script +=
	 * "umount -t cd9660 loopdir &>/dev/null\n"; script += "sudo rm -rf cd\n";
	 * script += "sudo rm -rf loopdir\n"; script +=
	 * "while [[ ! -f \"/tmp/debian-netinst.iso\" ]] || [[ $(shasum -a512 /tmp/debian-netinst.iso | awk '{print $1}') != '"
	 * + getData().getDebianIsoSha512(server) + "' ]]\n"; script += "do\n"; script
	 * += "    echo -e '\033[0;36m'\n"; script +=
	 * "    echo 'Please wait while I download the net-install ISO.  This may take some time.'\n"
	 * ; script += "    echo -e '\033[0m'\n"; script +=
	 * "    curl -L -o /tmp/debian-netinst.iso " + getData().getDebianIsoUrl(server)
	 * + "\n"; script += "done\n"; script +=
	 * "a=$(hdiutil attach -nobrowse -nomount /tmp/debian-netinst.iso | head -1 | awk {'print($1)'})\n"
	 * ; script += "mkdir loopdir\n"; script +=
	 * "mount -t cd9660 $a loopdir &>/dev/null\n"; script += "mkdir cd\n"; script +=
	 * "rsync -a -H --exclude=TRANS.TBL loopdir/ cd &>/dev/null\n"; script +=
	 * "cd cd\n"; script += "echo '" + preseed +
	 * "' | sudo tee preseed.cfg > /dev/null\n"; script +=
	 * "sed 's_timeout 0_timeout 10_' ../loopdir/isolinux/isolinux.cfg | sudo tee isolinux/isolinux.cfg > /dev/null\n"
	 * ; script +=
	 * "sed 's_append_append file=/cdrom/preseed.cfg auto=true_' ../loopdir/isolinux/txt.cfg | sudo tee isolinux/txt.cfg > /dev/null\n"
	 * ; script += "md5 -r ./ | sudo tee -a md5sum.txt > /dev/null\n"; script +=
	 * "cd ..\n"; script +=
	 * "sudo dd if=/tmp/debian-netinst.iso bs=512 count=1 of=/tmp/isohdpfx.bin &>/dev/null\n"
	 * ; script += "chmod +x /tmp/xorriso\n"; script +=
	 * "sudo /tmp/xorriso -as mkisofs -o " + dir + "/" + server +
	 * ".iso -r -J -R -no-emul-boot -iso-level 4 " +
	 * "-isohybrid-mbr /tmp/isohdpfx.bin -boot-load-size 4 -boot-info-table " +
	 * "-b isolinux/isolinux.bin -c isolinux/boot.cat ./cd\n"; script +=
	 * "sudo rm -rf initrd\n"; script += "umount -t cd9660 loopdir\n"; script +=
	 * "hdiutil detach $a\n"; script += "sudo rm -rf loopdir\n"; script +=
	 * "sudo rm -rf cd\n"; script += "echo -e '\033[0;36m'\n"; script +=
	 * "echo 'ISO generation complete!'\n"; script += "echo -e '\033[0m'\n"; script
	 * +=
	 * "read -r -p 'Would you like to dd it to a USB stick now? [Y/n] ' -n 1 response\n"
	 * ; script += "if [[  $response == 'n' || $response == 'N' ]];\n"; script +=
	 * "then\n"; script += "    echo ''\n"; script += "    exit 1\n"; script +=
	 * "else\n"; script +=
	 * "    read -r -p 'USB device name (e.g. disk2, sdc): ' usb\n"; script +=
	 * "    echo -e '\033[0;36m'\n"; script +=
	 * "    echo \"Writing to USB device /dev/$usb.  This will take some time.\"\n";
	 * script += "    echo -e '\033[0m'\n"; script +=
	 * "    diskutil unmountDisk /dev/$usb &>/dev/null\n"; script +=
	 * "    isosum=$(shasum -a512 " + dir + "/" + server +
	 * ".iso | awk '{ print $1}')\n"; script += "    sudo dd if=" + dir + "/" +
	 * server + ".iso of=/dev/$usb bs=10m &>/dev/null\n"; script +=
	 * "    usbsum=$(sudo dd if=/dev/disk2 | head -c `wc -c " + dir + "/" + server +
	 * ".iso` | shasum -a512 | awk '{ print $1 }')\n"; script +=
	 * "    diskutil eject /dev/$usb &>/dev/null\n"; script +=
	 * "    if [[ \"$usbsum\" == \"$isosum\" ]];\n"; script += "    then\n"; script
	 * += "        echo -e '\033[0;36m'\n"; script +=
	 * "        echo 'Checksums match!'\n"; script +=
	 * "        echo 'Done!  Now unplug the USB stick, insert into the target machine, and boot from it!'\n"
	 * ; script += "        echo -e '\033[0m'\n"; script += "    else\n"; script +=
	 * "        echo -e '\033[0;30m'\n"; script +=
	 * "        echo 'Something went wrong! :('\n"; script +=
	 * "        echo -e '\033[0m'\n"; script += "    fi\n"; script += "fi";
	 *
	 * try { PrintWriter wr = new PrintWriter(new FileOutputStream(dir + "/geniso-"
	 * + server + ".command")); Files.copy(new File("./misc/xorriso").toPath(), new
	 * File("/tmp/xorriso").toPath(),
	 * java.nio.file.StandardCopyOption.REPLACE_EXISTING); wr.write(script);
	 * wr.flush(); wr.close(); File file = new File(dir + "/geniso-" + server +
	 * ".command"); file.setExecutable(true);
	 * Runtime.getRuntime().exec("open --new /Applications/Utilities/Terminal.app "
	 * + dir + "/geniso-" + server + ".command"); } catch (IOException e) {
	 * e.printStackTrace(); } }
	 */

	public String getKeePassDBPassphrase() {
		return null;
	}

	public String getKeePassDBPath() {
		// TODO Auto-generated method stub
		return null;
	}

	public List<ServerModel> getDediServers() {
		// TODO Auto-generated method stub
		return null;
	}

	public List<ServerModel> getMetalServers() {
		// TODO Auto-generated method stub
		return null;
	}

	public List<ServiceModel> getServices(String hypervisorLabel) {
		// TODO Auto-generated method stub
		return null;
	}
}
