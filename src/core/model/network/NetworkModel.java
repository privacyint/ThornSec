/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.network;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.json.stream.JsonParsingException;
import javax.mail.internet.AddressException;

import core.data.machine.AMachineData;
import core.data.machine.AMachineData.MachineType;
import core.data.network.NetworkData;
import core.exception.AThornSecException;
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
import core.model.machine.UserDeviceModel;
import core.model.machine.configuration.NetworkInterfaceModel;

/**
 * Below the ThornsecModel comes the getNetworkModel().
 *
 * This model represents a given network;
 */
public class NetworkModel {
	private final String label;
	private NetworkData data;
	private Map<MachineType, Map<String, AMachineModel>> machines;
	private Map<String, Set<IUnit>> networkUnits;

	NetworkModel(String label) {
		this.label = label;

		this.machines = null;
		this.networkUnits = null;
	}

	final public String getLabel() {
		return this.label;
	}

	/**
	 * Initialises the various models across our network, building and initialising
	 * all of our machines
	 *
	 * @throws AddressException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws ClassNotFoundException
	 * @throws URISyntaxException
	 * @throws IOException
	 * @throws JsonParsingException
	 * @throws AThornSecException
	 */
	void init() throws AddressException, InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException,
			URISyntaxException, IOException, JsonParsingException, AThornSecException {

		// Start by building our network
		final Map<String, AMachineData> externals = this.data.getExternalOnlyDevices();
		if (externals != null) {
			for (final String label : externals.keySet()) {
				final AMachineModel device = new ExternalOnlyDeviceModel(label, this);
				addMachineToNetwork(MachineType.EXTERNAL_ONLY, label, device);
				addMachineToNetwork(MachineType.DEVICE, label, device);
			}
		}

		final Map<String, AMachineData> internals = this.data.getInternalOnlyDevices();
		if (internals != null) {
			for (final String label : internals.keySet()) {
				final AMachineModel device = new InternalOnlyDeviceModel(label, this);
				addMachineToNetwork(MachineType.INTERNAL_ONLY, label, device);
				addMachineToNetwork(MachineType.DEVICE, label, device);
			}
		}

		final Map<String, AMachineData> users = this.data.getUserDevices();
		if (users != null) {
			for (final String label : users.keySet()) {
				final AMachineModel device = new UserDeviceModel(label, this);
				addMachineToNetwork(MachineType.USER, label, device);
				addMachineToNetwork(MachineType.DEVICE, label, device);
			}
		}

		final Map<String, AMachineData> servers = this.data.getServers();
		if (servers != null) {
			for (final String label : servers.keySet()) {
				final ServerModel server = new ServerModel(label, this);
				addMachineToNetwork(MachineType.SERVER, label, server);
				for (final MachineType type : getData().getTypes(label)) {
					addMachineToNetwork(type, label, server);
				}
			}
		}

		// Now, step through our devices, initialise them, and run through their units.
		for (final ADeviceModel device : getDevices().values()) {
			device.init();
			putUnits(device.getLabel(), device.getUnits());
		}

		// We want to initialise the whole network first before we start getting units
		for (final ServerModel server : getServers().values()) {
			server.init();
		}

		// We want to separate the Routers out; they need to be the very last thing, as
		// it relies on everythign else being inited & configured
		for (final ServerModel server : getServers().values()) {
			if (!server.isRouter()) {
				putUnits(server.getLabel(), server.getUnits());
			}
		}

		// Finally, let's build our Routers
		for (final AMachineModel router : getMachines(MachineType.ROUTER).values()) {
			putUnits(router.getLabel(), router.getUnits());
		}

	}

	private void putUnits(String label, Set<IUnit> units) {
		if (this.networkUnits == null) {
			this.networkUnits = new LinkedHashMap<>();
		}

		this.networkUnits.put(label, units);
	}

	private void addMachineToNetwork(MachineType type, String label, AMachineModel machine) {
		if (this.machines == null) {
			this.machines = new LinkedHashMap<>();
		}

		Map<String, AMachineModel> machines = this.machines.get(type);
		if (machines == null) {
			machines = new LinkedHashMap<>();
		}

		machines.put(label, machine);

		this.machines.put(type, machines);
	}

	public Set<NetworkInterfaceModel> getNetworkInterfaces(String machine) throws InvalidMachineModelException {
		Set<NetworkInterfaceModel> interfaces = getMachineModel(machine).getNetworkInterfaces();

		if (interfaces == null) {
			interfaces = new LinkedHashSet<>();
		}

		return interfaces;
	}

	/**
	 * @return the whole network. Be aware that you will have to cast the values
	 *         from this method; you are far better to use one of the specialised
	 *         methods
	 */
	public final Map<MachineType, Map<String, AMachineModel>> getMachines() {
		return this.machines;
	}

	/**
	 * @param type
	 * @return A map of all machines of a given type
	 */
	public Map<String, AMachineModel> getMachines(MachineType type) {
		return getMachines().get(type);
	}

	/**
	 * @param type
	 * @return A map of all servers of a given type
	 */
	public Map<String, ServerModel> getServers(MachineType type) {
		final Map<String, ServerModel> servers = new LinkedHashMap<>();

		if (getMachines().get(type) != null) {
			for (final AMachineModel server : getMachines(type).values()) {
				servers.put(server.getLabel(), (ServerModel) server);
			}
		}

		return servers;
	}

	/**
	 * @return A linked map containing all server models for this network. Because
	 *         it is a linked map, it has predictable iteration meaning we can use
	 *         it to e.g. generate IP Addresses
	 */
	public final Map<String, ServerModel> getServers() {
		return getServers(MachineType.SERVER);
	}

	/**
	 * @return A linked map containing all device models *of a particular type* for
	 *         this network. Because it is a linked map, it has predictable
	 *         iteration meaning we can use it to e.g. generate IP Addresses
	 */
	public final Map<String, ADeviceModel> getDevices(MachineType type) {
		final Map<String, ADeviceModel> devices = new LinkedHashMap<>();

		if (getMachines().get(type) != null) {
			for (final AMachineModel device : getMachines(type).values()) {
				devices.put(device.getLabel(), (ADeviceModel) device);
			}
		}

		return devices;
	}

	/**
	 * @return a linked map of all devices (e.g. non-servers) on our network.
	 *         Because it is a linked map, it has predictable iteration meaning we
	 *         can use it to e.g. generate IP Addresses
	 */
	public final Map<String, ADeviceModel> getDevices() {
		return getDevices(MachineType.DEVICE);
	}

	/**
	 * @return A linked map containing all server models for this network. Because
	 *         it is a linked map, it has predictable iteration meaning we can use
	 *         it to e.g. generate IP Addresses
	 */
	public final Map<String, UserDeviceModel> getUserDevices() {
		final Map<String, UserDeviceModel> userDevices = new LinkedHashMap<>();

		for (final ADeviceModel device : getDevices(MachineType.USER).values()) {
			userDevices.put(device.getLabel(), (UserDeviceModel) device);
		}

		return userDevices;
	}

	/**
	 * @return A linked map containing all internal-only device models for this
	 *         network. Because it is a linked map, it has predictable iteration
	 *         meaning we can use it to e.g. generate IP Addresses
	 */
	public final Map<String, InternalOnlyDeviceModel> getInternalOnlyDevices() {
		final Map<String, InternalOnlyDeviceModel> internalOnlyDevices = new LinkedHashMap<>();

		for (final ADeviceModel device : getDevices(MachineType.INTERNAL_ONLY).values()) {
			internalOnlyDevices.put(device.getLabel(), (InternalOnlyDeviceModel) device);
		}

		return internalOnlyDevices;
	}

	/**
	 * @return A linked map containing all external-only device models for this
	 *         network. Because it is a linked map, it has predictable iteration
	 *         meaning we can use it to e.g. generate IP Addresses
	 */
	public final Map<String, ExternalOnlyDeviceModel> getExternalOnlyDevices() {
		final Map<String, ExternalOnlyDeviceModel> externalOnlyDevices = new LinkedHashMap<>();

		for (final ADeviceModel device : getDevices(MachineType.EXTERNAL_ONLY).values()) {
			externalOnlyDevices.put(device.getLabel(), (ExternalOnlyDeviceModel) device);
		}

		return externalOnlyDevices;
	}

	/**
	 * @return A specific machine model.
	 */
	public final AMachineModel getMachineModel(String machine) throws InvalidMachineModelException {
		for (final Map<String, AMachineModel> machines : getMachines().values()) {
			if (machines.containsKey(machine)) {
				return machines.get(machine);
			}
		}

		throw new InvalidMachineModelException(machine + " is not a machine on your network");
	}

	/**
	 * @return A specific server model.
	 */
	public final ServerModel getServerModel(String server) throws InvalidServerModelException {
		if (getServers().containsKey(server)) {
			return getServers().get(server);
		}

		throw new InvalidServerModelException(server + " is not a server on your network");
	}

	/**
	 * @return A specific device model.
	 */
	public final ADeviceModel getDeviceModel(String device) throws InvalidDeviceModelException {
		if (getDevices(MachineType.DEVICE).containsKey(this.label)) {
			return getDevices(MachineType.DEVICE).get(this.label);
		}

		throw new InvalidDeviceModelException(device + " is not a device on your network");
	}

	public final void auditNonBlock(String server, OutputStream out, InputStream in, boolean quiet)
			throws InvalidServerModelException {
		final ManageExec exec = getManageExec(server, "audit", out, quiet);
		if (exec != null) {
			exec.manage();
		}
	}

	public final void auditAll(OutputStream out, InputStream in, boolean quiet) throws InvalidServerModelException {
		for (final String server : getServers().keySet()) {
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
				final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
				final String filename = server + "_" + dateFormat.format(new Date()) + ".sh";
				final PrintWriter wr = new PrintWriter(new FileOutputStream(new File("./", filename), false));
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
}
