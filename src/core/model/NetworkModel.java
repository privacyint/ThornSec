package core.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.JOptionPane;

import core.data.NetworkData;
import core.exec.ManageExec;
import core.exec.PasswordExec;
import core.iface.IUnit;
import profile.IPSet;

public class NetworkModel {

	private String label;

	private NetworkData data;
	
	private IPSet ipsets;

	private LinkedHashMap<String, ServerModel> servers;
	private LinkedHashMap<String, DeviceModel> devices;

	private Vector<ServerModel> routers;
	private Vector<ServerModel> dedis;
	private Vector<ServerModel> metals;
	private Vector<ServerModel> services;
	
	private Vector<DeviceModel> userDevices;
	private Vector<DeviceModel> intOnlyDevices;
	private Vector<DeviceModel> extOnlyDevices;
	private Vector<DeviceModel> peripherals;

	private LinkedHashMap<String, Vector<IUnit>> units;

	NetworkModel(String label) {
		this.label = label;

		//First, the two different types
		servers = new LinkedHashMap<>();
		devices = new LinkedHashMap<>();
		
		//Now the subtypes
		routers  = new Vector<ServerModel>();
		metals   = new Vector<ServerModel>();
		dedis    = new Vector<ServerModel>();
		services = new Vector<ServerModel>();
		
		peripherals = new Vector<DeviceModel>();
		
		userDevices    = new Vector<DeviceModel>();
		intOnlyDevices = new Vector<DeviceModel>();
		extOnlyDevices = new Vector<DeviceModel>();

		ipsets = new IPSet(this);
		
		units = new LinkedHashMap<>();
	}

	public String getLabel() {
		return this.label;
	}
	
	public IPSet getIPSet() {
		return this.ipsets;
	}

	void init() {
		//Create (and classify) all devices
		for (String device : data.getAllDeviceLabels()) {
			DeviceModel deviceModel = new DeviceModel(device, this);
			deviceModel.setData(this.data);
			devices.put(device, deviceModel);
			
			deviceModel.init();

			switch (deviceModel.getType()) {
				case "User":
					userDevices.add(deviceModel);
					break;
				case "Internal":
					peripherals.add(deviceModel);
					intOnlyDevices.add(deviceModel);
					break;
				case "External":
					peripherals.add(deviceModel);
					extOnlyDevices.add(deviceModel);
					break;
				default:
					//In theory, we should never get here. Theory is a fine thing.
					System.out.println("Encountered an unsupported device type for " + device);
			}
		}

		//Then, create (and classify) all servers
		for (String server : data.getAllServerLabels()) {
			ServerModel serverModel = new ServerModel(server, this);
			serverModel.setData(this.data);
			servers.put(server, serverModel);
			serverModel.init();
			
			if (serverModel.isDedi()) {
				dedis.add(serverModel);
			}
			if (serverModel.isMetal()) {
				metals.add(serverModel);
			}
			if (serverModel.isRouter()) {
				routers.add(serverModel);
			}
			if (serverModel.isService()) {
				services.add(serverModel);
			}
		}

		//Now everything is classified and init()ed, get their networking requirements
		//We want to do this in a certain order - backwards through the network
		for(DeviceModel device : devices.values()) {
			device.getNetworking();
		}

		//Allow external-only && users to call out to the web
		for (DeviceModel device : userDevices) {
			device.addRequiredEgress("255.255.255.255", 0);
		}
		
		for (DeviceModel device : extOnlyDevices) {
			device.addRequiredEgress("255.255.255.255", 0);
		}

		for(ServerModel service : services) {
			service.getNetworking();
		}

		for(ServerModel metal : metals) {
			if (metal.isRouter()) { //If it's an external server...
				continue; //Skip it, otherwise we'll be duplicating its ifaces!
			}
			metal.getNetworking();
		}

		for(ServerModel dedi : dedis) {
			dedi.getNetworking();
		}
		
		//Now populate our ipsets before building our Router
		this.ipsets.init();

		for(ServerModel router : routers) {
			router.getNetworking();
		}

		//Finally, get all of the config units
		for (MachineModel machine : getAllMachines()) {
			units.put(machine.getLabel(), machine.getUnits());
		}

	}

	public Vector<MachineModel> getAllMachines() {
		Vector<MachineModel> machines = new Vector<MachineModel>();
		machines.addAll(servers.values());
		machines.addAll(devices.values());
		
		return machines;
	}
	
	public Vector<ServerModel> getRouterServers() {
		return routers;
	}
	
	public Vector<ServerModel> getDediServers() {
		return dedis;
	}

	public Vector<ServerModel> getMetalServers() {
		return metals;
	}

	public Vector<ServerModel> getServiceServers() {
		return services;
	}
	
	public Vector<ServerModel> getAllServers() {
		return new Vector<ServerModel>(servers.values());
	}

	public Vector<DeviceModel> getAllPeripheralDevices() {
		return peripherals;
	}
	
	public Vector<DeviceModel> getUserDevices() {
		return userDevices;
	}

	public Vector<DeviceModel> getInternalOnlyDevices() {
		return intOnlyDevices;
	}

	public Vector<DeviceModel> getExternalOnlyDevices() {
		return extOnlyDevices;
	}
	
	public Vector<DeviceModel> getAllDevices() {
		return new Vector<DeviceModel>(devices.values());
	}
	
	void registerOnMetal(ServerModel service, ServerModel metal) {
		metal.registerService(service);
	}

	public MachineModel getMachineModel(String machine) {
		if (servers.containsKey(machine)) {
			return servers.get(machine);
		}
		else if (devices.containsKey(machine)) {
			return devices.get(machine);
		}
		else {
			JOptionPane.showMessageDialog(null, machine + " does not exist in your network, yet you are trying to configure for it.\n\nThis is most likely due to a WebProxy pointing at an undeclared machine.\n\nPlease correct this, and run again");
			System.exit(1);
		}

		return null;
	}

	public ServerModel getServerModel(String server) {
		if (servers.containsKey(server)) {
			return servers.get(server);
		}
		else {
			JOptionPane.showMessageDialog(null, server + " does not exist in your network, yet you are trying to configure for it.\n\nThis is most likely due to a WebProxy pointing at an undeclared machine.\n\nPlease correct this, and run again");
			System.exit(1);
			return null;
		}
	}
	
	public DeviceModel getDeviceModel(String device) {
		if (devices.containsKey(device)) {
			return devices.get(device);
		}
		else {
			JOptionPane.showMessageDialog(null, device + " does not exist in your network, yet you are trying to configure for it.\n\nThis is most likely due to a requested admin user which has not been added to your users block.\n\nPlease correct this, and run again");
			System.exit(1);
			return null;
		}
	}

	public void auditNonBlock(String server, OutputStream out, InputStream in, boolean quiet) {
		ManageExec exec = getManageExec(server, "audit", out, quiet);
		if (exec != null)
			exec.manage();
	}

	public void auditAll(OutputStream out, InputStream in, boolean quiet) {
		for (String server : this.servers.keySet()) {
			ManageExec exec = getManageExec(server, "audit", out, quiet);
			if (exec != null)
				exec.manage();
		}
	}

	public void configNonBlock(String server, OutputStream out, InputStream in) {
		ManageExec exec = getManageExec(server, "config", out, false);
		if (exec != null)
			exec.manage();
	}

	public void dryrunNonBlock(String server, OutputStream out, InputStream in) {
		ManageExec exec = getManageExec(server, "dryrun", out, false);
		if (exec != null)
			exec.manage();
	}

	private ManageExec getManageExec(String server, String action, OutputStream out, boolean quiet) {
		// need to do a series of local checks eg known_hosts or expected
		// fingerprint
		ServerModel serverModel = servers.get(server);
		PasswordExec pass = new PasswordExec(server, this);

		String password = pass.getPassword();
		
		String audit = getScript(serverModel, action, quiet);
		
		if (action.equals("dryrun")) {
			try {
				Date now = new Date();
				PrintWriter wr = new PrintWriter(new FileOutputStream("./" + server + "_" + now.toString() + ".sh"));
				wr.write(audit);
				wr.flush();
				wr.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		if (pass.isDefaultPassword()) {
			System.out.println("FAIL: no password in keychain for " + serverModel.getLabel());
			System.out.println("Using the default password instead (this almost certainly won't work!)");
			return null;
		}
		
		ManageExec exec = new ManageExec(this.getData().getUser(), password, serverModel.getIP(), this.getData().getSSHPort(server), audit, out);
		return exec;
	}

	private String getScript(ServerModel serverModel, String action, boolean quiet) {
		System.out.println(
				"=======================" + this.label + ":" + serverModel.getLabel() + "==========================");
		String line = this.getHeader(serverModel.getLabel(), action) + "\n";
		Vector<IUnit> serverRules = units.get(serverModel.getLabel());
		for (int i = 0; i < serverRules.size(); i++) {
			IUnit unit = (IUnit) serverRules.elementAt(i);
			line += "#============ " + serverRules.elementAt(i).getLabel() + " =============\n";
			line += getText(action, unit, quiet) + "\n";
		}
		line += this.getFooter(serverModel.getLabel(), action);
		return line;
	}

	private String getText(String action, IUnit unit, boolean quiet) {
		String line = "";
		if (action.equals("audit")) {
			line = unit.genAudit(quiet);
		} else if (action.equals("config")) {
			line = unit.genConfig();
		} else if (action.equals("dryrun")) {
			line = unit.genConfig();
			//line = unit.genDryRun();
		}
		return line;
	}

	private String getHeader(String server, String action) {
		String line = "#!/bin/bash\n";
		line += "\n";
		line += "hostname=$(hostname);\n";
		line += "proceed=1;\n";
		line += "\n";
		line += "echo \"Started " + action + " $hostname with config label: " + server + "\"\n";
		line += "pass=0; fail=0; fail_string=;";
		return line;
	}

	private String getFooter(String server, String action) {
		String line = "echo \"pass=$pass fail=$fail failed:$fail_string\"\n\n";
		line += "\n";
		line += "echo \"Finished " + action + " $hostname with config label: " + server + "\"";
		return line;
	}

	public void setData(NetworkData data) {
		this.data = data;
	}

	public NetworkData getData() {
		return this.data;
	}
	
	public void genIsoServer(String server, String dir) {
		String currentUser = getData().getUser();
		String sshDir = "/home/" + currentUser + "/.ssh";
		String sshKey = getData().getUserSSHKey(currentUser);

		String preseed = "";
		preseed += "d-i preseed/late_command staring";
		preseed += "	in-target mkdir " + sshDir + ";";
		preseed += "    in-target touch " + sshDir + "/authorized_keys;";
		preseed += "	echo \\\"echo \\\'" + sshKey + "\\\' >> " + sshDir + "/authorized_keys; \\\" | chroot /target /bin/bash;";
		
		preseed += "	in-target chmod 700 " + sshDir + ";";
		preseed += "	in-target chmod 400 " + sshDir + "/authorized_keys;";
		preseed += "	in-target chown -R " + currentUser + ":" + currentUser + " " + sshDir + ";";
		//Force the user to change their passphrase on first login, lock the root account
		preseed += "	in-target passwd -e " + currentUser + ";";
		preseed += "	in-target passwd -l root;\n";
		
		preseed += "d-i debian-installer/locale string en_GB.UTF-8\n";
		preseed += "d-i keyboard-configuration/xkb-keymap select uk\n";
		preseed += "d-i netcfg/target_network_config select ifupdown\n";
		if (getData().getExtConnectionType(server) != null && getData().getExtConnectionType(server).equals("static")) {
			preseed += "d-i netcfg/disable_dhcp true\n";
			preseed += "d-i netcfg/choose_interface select " + getData().getWanIfaces(server) + "\n";
			preseed += "d-i netcfg/disable_autoconfig boolean true\n";
			preseed += "d-i netcfg/get_ipaddress string " + getData().getProperty(server, "externaladdress", true) + "\n";
			preseed += "d-i netcfg/get_netmask string " + getData().getProperty(server, "externalnetmask", true) + "\n";
			preseed += "d-i netcfg/get_gateway string " + getData().getProperty(server, "externalgateway", true) + "\n";
			preseed += "d-i netcfg/get_nameservers string " + getData().getDNS()[0] + "\n"; //Use the first DNS server
			preseed += "d-i netcfg/confirm_static boolean true\n";
		}
		else {
			preseed += "d-i netcfg/choose_interface select auto\n";
		}
		preseed += "d-i netcfg/get_hostname string " + server + "\n";
		preseed += "d-i netcfg/get_domain string " + getData().getDomain(server) + "\n";
		preseed += "d-i netcfg/hostname string " + server + "\n";
		//preseed += "d-i hw-detect/load_firmware boolean true\n"; //Always try to load non-free firmware
		preseed += "d-i mirror/country string GB\n";
		preseed += "d-i mirror/http/mirror string " + getData().getDebianMirror(server) + "\n";
		preseed += "d-i mirror/http/directory string /debian\n";
		preseed += "d-i mirror/http/proxy string\n";
		preseed += "d-i passwd/root-password password secret\n";
		preseed += "d-i passwd/root-password-again password secret\n";
		preseed += "d-i passwd/user-fullname string " + getData().getUserFullName(currentUser) + "\n";
		preseed += "d-i passwd/username string " + currentUser + "\n";
		preseed += "d-i passwd/user-password password secret\n";
		preseed += "d-i passwd/user-password-again password secret\n";
		preseed += "d-i passwd/user-default-groups string sudo\n";
		preseed += "d-i clock-setup/utc boolean true\n";
		preseed += "d-i time/zone string Europe/London\n";
		preseed += "d-i clock-setup/ntp boolean true\n";
		preseed += "d-i partman-auto/disk string /dev/sda\n";
		preseed += "d-i partman-auto/method string regular\n";
		preseed += "d-i partman-auto/purge_lvm_from_device boolean true\n";
		preseed += "d-i partman-lvm/device_remove_lvm boolean true\n";
		preseed += "d-i partman-md/device_remove_md boolean true\n";
		preseed += "d-i partman-lvm/confirm boolean true\n";
		preseed += "d-i partman-auto/choose_recipe select atomic\n";
		preseed += "d-i partman-partitioning/confirm_write_new_label boolean true\n";
		preseed += "d-i partman/choose_partition select finish\n";
		preseed += "d-i partman/confirm boolean true\n";
		preseed += "d-i partman/confirm_nooverwrite boolean true\n";
		preseed += "tasksel tasksel/first mualtiselect none\n";
		preseed += "d-i pkgsel/include string sudo openssh-server dkms gcc bzip2\n";
		preseed += "d-i preseed/late_command string sed -i '/^deb cdrom:/s/^/#/' /target/etc/apt/sources.list\n";
		preseed += "d-i apt-setup/use_mirror boolean false\n";
		preseed += "d-i apt-setup/cdrom/set-first boolean false\n";
		preseed += "d-i apt-setup/cdrom/set-next boolean false\n";
		preseed += "d-i apt-setup/cdrom/set-failed boolean false\n";
		preseed += "popularity-contest popularity-contest/participate boolean false\n";
		preseed += "d-i grub-installer/only_debian boolean true\n";
		preseed += "d-i grub-installer/with_other_os boolean true\n";
		preseed += "d-i grub-installer/bootdev string default\n";
		preseed += "d-i finish-install/reboot_in_progress note";

		String script = "#!/bin/bash\n";
		script += "cd " + dir + "\n";
		script += "umount -t cd9660 loopdir &>/dev/null\n";
		script += "sudo rm -rf cd\n";
		script += "sudo rm -rf loopdir\n";
		script += "while [[ ! -f \"/tmp/debian-netinst.iso\" ]] || [[ $(shasum -a512 /tmp/debian-netinst.iso | awk '{print $1}') != '" + getData().getDebianIsoSha512(server) + "' ]]\n";
		script += "do\n";
		script += "    echo -e '\033[0;36m'\n";
		script += "    echo 'Please wait while I download the net-install ISO.  This may take some time.'\n";
		script += "    echo -e '\033[0m'\n";
		script += "    curl -L -o /tmp/debian-netinst.iso " + getData().getDebianIsoUrl(server) + "\n";
		script += "done\n";
		script += "a=$(hdiutil attach -nobrowse -nomount /tmp/debian-netinst.iso | head -1 | awk {'print($1)'})\n";
		script += "mkdir loopdir\n";
		script += "mount -t cd9660 $a loopdir &>/dev/null\n";
		script += "mkdir cd\n";
		script += "rsync -a -H --exclude=TRANS.TBL loopdir/ cd &>/dev/null\n";
		script += "cd cd\n";
		script += "echo '" + preseed + "' | sudo tee preseed.cfg > /dev/null\n";
		script += "sed 's_timeout 0_timeout 10_' ../loopdir/isolinux/isolinux.cfg | sudo tee isolinux/isolinux.cfg > /dev/null\n";
		script += "sed 's_append_append file=/cdrom/preseed.cfg auto=true_' ../loopdir/isolinux/txt.cfg | sudo tee isolinux/txt.cfg > /dev/null\n";
		script += "md5 -r ./ | sudo tee -a md5sum.txt > /dev/null\n";
		script += "cd ..\n";
		script += "sudo dd if=/tmp/debian-netinst.iso bs=512 count=1 of=/tmp/isohdpfx.bin &>/dev/null\n";
		script += "chmod +x /tmp/xorriso\n";
		script += "sudo /tmp/xorriso -as mkisofs -o " + dir + "/" + server + ".iso -r -J -R -no-emul-boot -iso-level 4 "
				+ "-isohybrid-mbr /tmp/isohdpfx.bin -boot-load-size 4 -boot-info-table "
				+ "-b isolinux/isolinux.bin -c isolinux/boot.cat ./cd\n";
		script += "sudo rm -rf initrd\n";
		script += "umount -t cd9660 loopdir\n";
		script += "hdiutil detach $a\n";
		script += "sudo rm -rf loopdir\n";
		script += "sudo rm -rf cd\n";
		script += "echo -e '\033[0;36m'\n";
		script += "echo 'ISO generation complete!'\n";
		script += "echo -e '\033[0m'\n";
		script += "read -r -p 'Would you like to dd it to a USB stick now? [Y/n] ' -n 1 response\n";
		script += "if [[  $response == 'n' || $response == 'N' ]];\n";
		script += "then\n";
		script += "    echo ''\n";
		script += "    exit 1\n";
		script += "else\n";
		script += "    read -r -p 'USB device name (e.g. disk2, sdc): ' usb\n";
		script += "    echo -e '\033[0;36m'\n";
		script += "    echo \"Writing to USB device /dev/$usb.  This will take some time.\"\n";
		script += "    echo -e '\033[0m'\n";
		script += "    diskutil unmountDisk /dev/$usb &>/dev/null\n";
		script += "    isosum=$(shasum -a512 " + dir + "/" + server + ".iso | awk '{ print $1}')\n";
		script += "    sudo dd if=" + dir + "/" + server + ".iso of=/dev/$usb bs=10m &>/dev/null\n";
		script += "    usbsum=$(sudo dd if=/dev/disk2 | head -c `wc -c " + dir + "/" + server + ".iso` | shasum -a512 | awk '{ print $1 }')\n";
		script += "    diskutil eject /dev/$usb &>/dev/null\n";
		script += "    if [[ \"$usbsum\" == \"$isosum\" ]];\n";
		script += "    then\n";
		script += "        echo -e '\033[0;36m'\n";
		script += "        echo 'Checksums match!'\n";
		script += "        echo 'Done!  Now unplug the USB stick, insert into the target machine, and boot from it!'\n";
		script += "        echo -e '\033[0m'\n";
		script += "    else\n";
		script += "        echo -e '\033[0;30m'\n";
		script += "        echo 'Something went wrong! :('\n";
		script += "        echo -e '\033[0m'\n";
		script += "    fi\n";
		script += "fi";
		
		try {
			PrintWriter wr = new PrintWriter(new FileOutputStream(dir + "/geniso-" + server + ".command"));
			Files.copy(new File("./misc/xorriso").toPath(), new File("/tmp/xorriso").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			wr.write(script);
			wr.flush();
			wr.close();
			File file = new File(dir + "/geniso-" + server + ".command");
			file.setExecutable(true);
			Runtime.getRuntime().exec("open --new /Applications/Utilities/Terminal.app " + dir + "/geniso-" + server + ".command");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Inet4Address stringToIP(String toParse) {
		Object ip = null;
		
		//If we don't check this, null == 127.0.0.1, which throws everything :)
		if (toParse == null) { return null; }
		
		try {
			ip = Inet4Address.getByName(toParse);
		}
		catch (UnknownHostException e) {
			JOptionPane.showMessageDialog(null, toParse + " appears to be an invalid address, or you're currently offline. Please check your network connection and try again.");
			System.exit(1);
		}
		
		return (Inet4Address) ip;
	}
	
	public InetAddress[] stringToAllIPs(String toParse) {
		Object[] parsed = null;
		ArrayList<InetAddress> addresses = new ArrayList<InetAddress>(); 
		
		//If we don't check this, null == 127.0.0.1, which throws everything :)
		if (toParse == null) { return null; }
		
		try {
			parsed = Inet4Address.getAllByName(toParse);
		}
		catch (UnknownHostException e) {
			JOptionPane.showMessageDialog(null, toParse + " appears to be an invalid address, or you're currently offline. Please check your network connection and try again.");
			System.exit(1);
		}
		
		for (InetAddress ip : (InetAddress[]) parsed) {
			if (ip instanceof Inet4Address) {
				addresses.add(ip);
			}
		}

		return addresses.toArray(new InetAddress[addresses.size()]);
	}
}
