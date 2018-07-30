package core.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import core.data.NetworkData;
import core.exec.ManageExec;
import core.exec.PasswordExec;
import core.iface.IUnit;

public class NetworkModel {

	private String label;

	private NetworkData data;

	private LinkedHashMap<String, ServerModel> servers;

	private Vector<String> routers;

	private Vector<String> metals;

	private Vector<String> services;
	
	private LinkedHashMap<String, DeviceModel> devices;
	
	private LinkedHashMap<String, Vector<IUnit>> units;

	public NetworkModel(String label) {
		this.label = label;
	}

	public String getLabel() {
		return this.label;
	}

	public void init() {
		servers = new LinkedHashMap<>();
		routers = new Vector<String>();
		metals = new Vector<String>();
		services = new Vector<String>();
		devices = new LinkedHashMap<>();
		units = new LinkedHashMap<>();
		
		for (String server : data.getServerLabels()) {
			ServerModel sm = new ServerModel(server);
			sm.setData(this.data);
			servers.put(server, sm);
		}

		//Need to init them out here as they may depend on others
		for(Map.Entry<String, ServerModel> server : servers.entrySet()) {
			server.getValue().init(this);
		}
		
		for (String device : data.getDeviceLabels()) {
			DeviceModel dm = new DeviceModel(device);
			dm.setData(this.data);
			devices.put(device, dm);
			//Can init them in this loop as they don't depend on others
			dm.init(this);
		}
		
		//We need to do it in the following order: devices, services, metals, routers.
		//This is because inherited rules (e.g. IPTables) will only ever go in that direction
		for (String device : data.getDeviceLabels()) {
			//Remove duplication
			if (!units.containsKey(device)) {
				units.put(device, devices.get(device).getUnits());
			}
		}
		for (String service : services) {
			if (!units.containsKey(service)) {
				units.put(service, servers.get(service).getUnits());
			}
		}
		for (String metal : metals) {
			if (!units.containsKey(metal)) {
				units.put(metal, servers.get(metal).getUnits());
			}
		}
		for (String router : routers) {
			if (!units.containsKey(router)) {
				units.put(router, servers.get(router).getUnits());
			}
		}
	}

	public void registerRouter(String label) {
		routers.add(label);
	}
	
	public void registerMetal(String label) {
		metals.add(label);
	}
	
	public void registerService(String label) {
		services.add(label);
	}
	
	public Vector<String> getRouters() {
		return routers;
	}
	
	public Vector<String> getMetals() {
		return metals;
	}
	
	public Vector<String> getServices() {
		return services;
	}
	
	public void registerOnMetal(String label, String metal) {
		this.getServerModel(metal).registerService(label);
	}

	public ServerModel getServerModel(String server) {
		if (servers.containsKey(server)) {
			return servers.get(server);
		}
		else {
			return null;
		}
	}
	
	public DeviceModel getDeviceModel(String device) {
		if (devices.containsKey(device)) {
			return devices.get(device);
		}
		else {
			return null;
		}
	}

	public String[] getServerLabels() {
		//return this.servers.keySet().toArray(new String[servers.size()]);
		return data.getServerLabels();
	}

	public String[] getDeviceLabels() {
		return data.getDeviceLabels();
	}

//	public void auditBlock(String server, OutputStream out, InputStream in, boolean quiet) {
//		ManageExec exec = getManageExec(server, "audit", out, quiet);
//		if (exec != null)
//			exec.runBlock();
//	}

	public void auditNonBlock(String server, OutputStream out, InputStream in, boolean quiet) {
		ManageExec exec = getManageExec(server, "audit", out, quiet);
		if (exec != null)
			exec.runScriptNonBlock();
	}

	public void auditAll(OutputStream out, InputStream in, boolean quiet) {
		for (String server : this.servers.keySet()) {
			ManageExec exec = getManageExec(server, "audit", out, quiet);
			if (exec != null)
				exec.runScriptNonBlock();
		}
	}

//	public void configBlock(String server, OutputStream out, InputStream in) {
//		ManageExec exec = getManageExec(server, "config", out, false);
//		if (exec != null)
//			exec.runBlock();
//	}

//	public void dryrunBlock(String server, OutputStream out, InputStream in) {
//		ManageExec exec = getManageExec(server, "dryrun", out, false);
//		if (exec != null)
//			exec.runBlock();
//	}

	public void configNonBlock(String server, OutputStream out, InputStream in) {
		ManageExec exec = getManageExec(server, "config", out, false);
		if (exec != null)
			exec.runScriptNonBlock();
	}

	public void dryrunNonBlock(String server, OutputStream out, InputStream in) {
		ManageExec exec = getManageExec(server, "dryrun", out, false);
		if (exec != null)
			exec.runScriptNonBlock();
	}

	private ManageExec getManageExec(String server, String action, OutputStream out, boolean quiet) {
		// need to do a series of local checks eg known_hosts or expected
		// fingerprint
		if (servers.containsKey(server)) {
			ServerModel serverModel = servers.get(server);
			PasswordExec pass = new PasswordExec(server, this);

			if (!pass.init()) {
				return null;
			} else {
				String password = pass.getPassword();
				
				System.out.println("PASS: password in keychain for " + serverModel.getLabel());
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

				if (password == null || password.equals("")) {
					System.out.println("FAIL: no password in keychain for " + serverModel.getLabel());
					return null;
				}
				
				ManageExec exec = new ManageExec(server, this, password, audit, out);
				return exec;
			}
		} else {
			System.out.println("No server " + server);
		}
		return null;
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
		String sshKey = getData().getSSHKey(currentUser);

		String preseed = "";
		preseed += "d-i preseed/late_command string";
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
		if (getData().getExtConn(server) != null && getData().getExtConn(server).equals("static")) {
			preseed += "d-i netcfg/disable_dhcp true\n";
			preseed += "d-i netcfg/choose_interface select " + getData().getExtIface(server) + "\n";
			preseed += "d-i netcfg/disable_autoconfig boolean true\n";
			preseed += "d-i netcfg/get_ipaddress string " + getData().getProperty(server, "externaladdress") + "\n";
			preseed += "d-i netcfg/get_netmask string " + getData().getProperty(server, "externalnetmask") + "\n";
			preseed += "d-i netcfg/get_gateway string " + getData().getProperty(server, "externalgateway") + "\n";
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
		preseed += "d-i passwd/user-fullname string " + getData().getFullName(currentUser) + "\n";
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
		preseed += "tasksel tasksel/first multiselect none\n";
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

}
