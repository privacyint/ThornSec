/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.hypervisor;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import core.exception.AThornSecException;
import core.exception.data.NoValidUsersException;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.InvalidMachineModelException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.machine.configuration.DiskModel;
import core.model.machine.HypervisorModel;
import core.model.machine.ServerModel;
import core.model.machine.ServiceModel;
import core.data.machine.AMachineData.MachineType;
import core.data.machine.configuration.DiskData.Medium;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import profile.type.Hypervisor;

public class Virtualbox extends AHypervisorProfile {

	public Virtualbox(HypervisorModel me) {
		super(me);
	}

	@Override
	protected void buildDisks() {
		// TODO Auto-generated method stub
	}

	@Override
	protected void buildBackups() {
	}

	@Override
	protected void buildVMs() throws InvalidMachineModelException {
		for (ServiceModel service : getServerModel().getServices()) {
			buildVM(service);
		}
	}
	
	private void buildVM(ServiceModel service) {
		final String baseDir = getServerModel().getVMBase().getAbsolutePath();

		final String backupDir = baseDir + "/backups/" + service.getLabel();
		final String ttySocketDir = baseDir + "/sockets/" + service.getLabel();

		final String user = "vboxuser_" + service.getLabel();
		final String group = "vboxusers";

		final Collection<IUnit> units = new ArrayList<>();

		// Create VirtualBox user
		units.add(new SimpleUnit("metal_virtualbox_" + service.getLabel() + "_user", "metal_virtualbox_installed",
				"sudo adduser " + user + " --system --shell=/bin/false --disabled-login --ingroup " + group,
				"id -u " + user + " 2>&1 | grep 'no such user'", "", "pass", "Couldn't create the user for " + service.getLabel()
						+ " on its HyperVisor.  This is fatal, " + service.getLabel() + " will not be installed."));

		// Create VM itself
		units.add(new SimpleUnit(service.getLabel() + "_exists", "metal_virtualbox_" + service.getLabel() + "_user",
				"sudo -u " + user + " VBoxManage createvm --name " + service.getLabel() + " --ostype \"" + group + "\""
						+ " --register;" + "sudo -u " + user + " VBoxManage modifyvm " + service.getLabel() + " --description "
						+ "\"" + service.getLabel() + "." + getNetworkModel().getData().getDomain() + "\n"
						+ "ThornSec guest machine\n" + "Built with profile(s): "
						+ String.join(", ", service.getProfiles().keySet()) + "\n"
						+ "Built at $(date)" + "\"",
				"sudo -u " + user + " VBoxManage list vms | grep " + service.getLabel(), "", "fail", "Couldn't create " + service.getLabel()
						+ " on its HyperVisor.  This is fatal, " + service.getLabel() + " will not exist on your network."));

		units.add(new DirUnit("socket_dir_" + service.getLabel(), "proceed", ttySocketDir, user, group, 0750, ""));

		// Architecture setup
		units.add(modifyVm(service.getLabel(), user, "paravirtprovider", "kvm")); // Default, make it explicit
		units.add(modifyVm(service.getLabel(), user, "chipset", "ich9"));
		units.add(modifyVm(service.getLabel(), user, "ioapic", "on", "IO APIC couldn't be enabled for " + service.getLabel()
				+ ".  This is required for 64-bit installations, and for more than 1 virtual CPU in a service."));
		units.add(modifyVm(service.getLabel(), user, "hwvirtex", "on"));
		units.add(modifyVm(service.getLabel(), user, "pae", "on"));
		units.add(modifyVm(service.getLabel(), user, "cpus", service.getCPUs()));
		units.add(modifyVm(service.getLabel(), user, "cpuexecutioncap", service.getCPUExecutionCap()));

		// RAM setup
		units.add(modifyVm(service.getLabel(), user, "memory", service.getRAM()));
		units.add(modifyVm(service.getLabel(), user, "vram", "16"));
		units.add(modifyVm(service.getLabel(), user, "nestedpaging", "on"));
		units.add(modifyVm(service.getLabel(), user, "largepages", "on"));

		// Audio setup (switch it off)
		units.add(modifyVm(service.getLabel(), user, "audio", "none"));

		// Use high precision event timers instead of legacy
		units.add(modifyVm(service.getLabel(), user, "hpet", "on"));

		// Shared folders setup
		units.add(new SimpleUnit(service.getLabel() + "_backup_sf_attached", service.getLabel() + "_exists",
				"sudo -u " + user + " VBoxManage sharedfolder add " + service.getLabel() + " --name backup --hostpath "
						+ backupDir,
				"sudo -u " + user + " VBoxManage showvminfo " + service.getLabel()
						+ " --machinereadable | grep SharedFolderPathMachineMapping2",
				"SharedFolderPathMachineMapping2=\\\"" + backupDir + "\\\"", "pass"));

		// Clock setup to try and stop drift between host and guest
		// https://www.virtualbox.org/manual/ch09.html#changetimesync
		units.add(guestPropertySet(service.getLabel(), user, "timesync-interval", "10000", "Couldn't sync the clock between "
				+ service.getLabel() + " and its metal.  You'll probably see some clock drift in " + service.getLabel() + " as a result."));
		units.add(guestPropertySet(service.getLabel(), user, "timesync-min-adjust", "100", "Couldn't sync the clock between "
				+ service.getLabel() + " and its metal.  You'll probably see some clock drift in " + service.getLabel() + " as a result."));
		units.add(guestPropertySet(service.getLabel(), user, "timesync-set-on-restore", "1", "Couldn't sync the clock between "
				+ service.getLabel() + " and its metal.  You'll probably see some clock drift in " + service.getLabel() + " as a result."));
		units.add(guestPropertySet(service.getLabel(), user, "timesync-set-threshold", "1000", "Couldn't sync the clock between "
				+ service.getLabel() + " and its metal.  You'll probably see some clock drift in " + service.getLabel() + " as a result."));

		// tty0 socket
		units.add(new SimpleUnit(service.getLabel() + "_tty0_com_port", service.getLabel() + "_exists",
				"sudo -u " + user + " VBoxManage modifyvm " + service.getLabel() + " --uart1 0x3F8 4",
				"sudo -u " + user + " VBoxManage showvminfo " + service.getLabel() + " --machinereadable | grep ^uart1=",
				"uart1=\\\"0x03f8,4\\\"", "pass"));

		units.add(new SimpleUnit(service.getLabel() + "_tty0_socket", service.getLabel() + "_tty0_com_port",
				"sudo -u " + user + " VBoxManage modifyvm " + service.getLabel() + " --uartmode1 server " + ttySocketDir
						+ "/vboxttyS0",
				"sudo -u " + user + " VBoxManage showvminfo " + service.getLabel() + " --machinereadable | grep ^uartmode1=",
				"uartmode1=\\\"server," + ttySocketDir + "/vboxttyS0\\\"", "pass"));

		units.add(new SimpleUnit(service.getLabel() + "_tty1_com_port", service.getLabel() + "_exists",
				"sudo -u " + user + " VBoxManage modifyvm " + service.getLabel() + " --uart2 0x2F8 3",
				"sudo -u " + user + " VBoxManage showvminfo " + service.getLabel() + " --machinereadable | grep ^uart2=",
				"uart2=\\\"0x02f8,3\\\"", "pass"));

		units.add(new SimpleUnit(service.getLabel() + "_tty1_socket", service.getLabel() + "_tty1_com_port",
				"sudo -u " + user + " VBoxManage modifyvm " + service.getLabel() + " --uartmode2 server " + ttySocketDir
						+ "/vboxttyS1",
				"sudo -u " + user + " VBoxManage showvminfo " + service.getLabel() + " --machinereadable | grep ^uartmode2=",
				"uartmode2=\\\"server," + ttySocketDir + "/vboxttyS1\\\"", "pass"));

		getServerModel().addProcessString("/usr/lib/virtualbox/VBoxHeadless --comment " + service.getLabel() + " --startvm `if id '" + user
						+ "' >/dev/null 2>&1; then sudo -u " + user + " bash -c 'VBoxManage list runningvms | grep "
						+ service.getLabel() + "' | awk '{ print $2 }' | tr -d '{}'; else echo ''; fi` --vrde config *$");
		getServerModel().addProcessString("awk \\{");
		getServerModel().addProcessString("tr -d \\{\\}$");
		getServerModel().getUserModel().addUsername(user);

	}
	
	@Override
	public Collection<IUnit> getInstalled() throws AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("metal_virtualbox", "a2f683c52980aecf_pgp", "virtualbox-6.1"));
		units.add(new InstalledUnit("metal_genisoimage", "proceed", "genisoimage"));
		units.add(new InstalledUnit("metal_rsync", "proceed", "rsync"));
		units.add(new InstalledUnit("metal_guestfs_utils", "proceed", "libguestfs-tools"));
		units.add(new InstalledUnit("metal_wget", "proceed", "wget"));

		return units;
	}

	@Override
	public final Collection<IUnit> getPersistentConfig() throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		getServerModel().addProcessString("/usr/lib/virtualbox/VBoxXPCOMIPCD$");
		getServerModel().addProcessString("/usr/lib/virtualbox/VBoxSVC --auto-shutdown$");
		getServerModel().addProcessString("\\[iprt-VBoxWQueue\\]$");
		getServerModel().addProcessString("\\[iprt-VBoxTscThr\\]$");
		getServerModel().addProcessString("\\[kvm-irqfd-clean\\]$");

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

//		getServerModel().getAptSourcesModel().addAptSource("virtualbox", "deb http://download.virtualbox.org/virtualbox/debian buster contrib", "keyserver.ubuntu.com", "a2f683c52980aecf");
//		//getMachineModel().addEgress("download.virtualbox.org:80");

		return units;
	}

	protected Collection<IUnit> buildDisks(String user, String group, String service, Map<String, DiskModel> disks) {
		final Collection<IUnit> units = new ArrayList<>();

		// Disk controller setup
		units.add(new SimpleUnit(service + "_hdds_sas_controller", service + "_exists",
				"sudo -u " + user + " VBoxManage storagectl " + service + " --name \"HDDs\"" + " --add sas"
						+ " --controller LSILogicSAS" + " --portcount "
						+ disks.values().stream().filter(disk -> disk.getMedium() == Medium.DISK).count()
						+ " --hostiocache off",
				"sudo -u " + user + " VBoxManage showvminfo " + service
						+ " --machinereadable | grep ^storagecontrollername0=",
				"storagecontrollername0=\\\"HDDs\\\"", "pass",
				"The hard drive SAS controller for " + service
						+ " (where its disks are attached) Couldn't be created/attached to " + service
						+ ".  This is fatal, " + service + " will not be installed."));

		units.add(new SimpleUnit(service + "_dvds_ide_controller", service + "_exists", "sudo -u " + user
				+ " VBoxManage storagectl " + service + " --name \"DVDs\"" + " --add ide" + " --controller PIIX4"
				// + " --portcount " + disks.values().stream().filter(disk -> disk.getMedium()
				// == Medium.DVD).count()
				+ " --hostiocache off",
				"sudo -u " + user + " VBoxManage showvminfo " + service
						+ " --machinereadable | grep ^storagecontrollername1=",
				"storagecontrollername1=\\\"DVDs\\\"", "pass",
				"The DVD SAS controller for " + service
						+ " (where its disks are attached) Couldn't be created/attached to " + service
						+ ".  This is fatal, " + service + " will not be installed."));

		int deviceCounter = 0;
		for (final DiskModel disk : disks.values().stream().filter(disk -> disk.getMedium() == Medium.DISK)
				.toArray(DiskModel[]::new)) {
			units.add(new DirUnit(disk.getLabel() + "_disk_dir_" + service, "proceed", disk.getFilePath(), user, group, 0750, ""));

			units.add(new DirUnit(disk.getLabel() + "_disk_loopback_dir_" + service, "proceed",
					disk.getFilePath() + "/live/", "root", "root", 0700, ""));

			String diskCreation = "";
			diskCreation += "sudo -u " + user + " VBoxManage createmedium --filename " + disk.getFilename();
			diskCreation += " --size " + disk.getSize();
			diskCreation += " --format " + disk.getFormat();
			diskCreation += (disk.getDiffParent().isPresent()) ? " --diffparent " + disk.getDiffParent() : "";

			units.add(new SimpleUnit(service + "_" + disk.getLabel() + "_disk",
					disk.getLabel() + "_disk_dir_" + service + "_chmoded", diskCreation,
					"sudo [ -f " + disk.getFilename() + " ] && echo pass;", "pass", "pass",
					"Couldn't create the disk " + disk.getLabel() + " for " + service + "."));
//			units.add(new FileOwnUnit(service + "_" + disk.getLabel() + "_disk",
//					service + "_" + disk.getLabel() + "_disk", disk.getFilename(), user, group));

			String diskAttach = "";
			diskAttach += "sudo -u " + user + " VBoxManage storageattach " + service;
			diskAttach += " --storagectl \"HDDs\"";
			diskAttach += " --port " + deviceCounter;
			diskAttach += " --device 0";
			diskAttach += " --type hdd";
			diskAttach += " --medium " + disk.getFilename();
			// diskAttach += (disk.getLabel().contentEquals("boot")) ? " --bootable on" : "
			// --bootable -off";
			// diskAttach += " --comment \\\"" + disk.getComment() + "\\\";";

			units.add(new SimpleUnit(service + "_" + disk.getLabel() + "_disk_attached",
					service + "_hdds_sas_controller", diskAttach,
					"sudo -u " + user + " VBoxManage showvminfo " + service + " --machinereadable | grep \"HDDs-"
							+ deviceCounter + "-0\"",
					"\\\"HDDs-" + deviceCounter + "-0\\\"=\\\"" + disk.getFilename() + "\\\"", "pass",
					"Couldn't attach disk " + disk.getLabel() + "for " + service + "."));

			deviceCounter++;
		}

		deviceCounter = 0;
		for (final DiskModel disk : disks.values().stream().filter(disk -> disk.getMedium() == Medium.DVD)
				.toArray(DiskModel[]::new)) {
			String diskAttach = "";
			diskAttach += "sudo -u " + user + " VBoxManage storageattach " + service;
			diskAttach += " --storagectl \"DVDs\"";
			diskAttach += " --port " + deviceCounter;
			diskAttach += " --device 0";
			diskAttach += " --type dvddrive";
			diskAttach += " --medium " + disk.getFilename();
			// diskAttach += (disk.getLabel().contentEquals("boot")) ? " --bootable on" : "
			// --bootable -off";
			// diskAttach += " --comment \\\"" + disk.getComment() + "\\\";";

			units.add(new SimpleUnit(service + "_" + disk.getLabel() + "_disk_attached",
					service + "_dvds_ide_controller", diskAttach,
					"sudo -u " + user + " VBoxManage showvminfo " + service + " --machinereadable | grep \"DVDs-0-"
							+ deviceCounter + "\"",
					"\\\"DVDs-" + deviceCounter + "-0\\\"=\\\"" + disk.getFilename() + "\\\"", "pass",
					"Couldn't attach disk " + disk.getLabel() + " for " + service + "."));

			deviceCounter++;
		}

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
	
	protected Collection<IUnit> buildBackups(String service, String logDir, String user, String group) {
		Collection<IUnit> units = new ArrayList<>();
		
		units.add(new DirUnit("log_dir_" + service, "proceed", logDir, user, group, 0750, ""));

		units.add(new SimpleUnit(service + "_log_sf_attached", service + "_exists",
				"sudo -u " + user + " VBoxManage sharedfolder add " + service + " --name log --hostpath " + logDir + ";"
						+ "sudo -u " + user + " VBoxManage setextradata " + service
						+ " VBoxInternal1/SharedFoldersEnableSymlinksCreate/log 1",
				"sudo -u " + user + " VBoxManage showvminfo " + service
						+ " --machinereadable | grep SharedFolderPathMachineMapping1",
				"SharedFolderPathMachineMapping1=\\\"" + logDir + "\\\"", "pass",
				"Couldn't attach the logs folder to " + service + ".  This means logs will only exist in the VM."));
		
		return units;
	}
	
	protected Collection<IUnit> buildLogs(String service, String backupDir, String user, String group) {
		Collection<IUnit> units = new ArrayList<>();
		
		units.add(new DirUnit("backup_dir_" + service, "proceed", backupDir, user, group, 0750, ""));

		// Mark the backup destination directory as a valid destination
		units.add(new FileUnit(service + "_mark_backup_dir", "backup_dir_" + service + "_chmoded",
				backupDir + "/backup.marker", "In memoriam Luke and Guy.  Miss you two!"));
		
		return units;
	}

	protected SimpleUnit modifyVm(String service, String user, String setting, String value, String errorMsg,
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

	protected SimpleUnit modifyVm(String service, String user, String setting, String value, String errorMsg) {
		return modifyVm(service, user, setting, value, errorMsg, service + "_exists");
	}

	protected SimpleUnit modifyVm(String service, String user, String setting, String value) {
		return modifyVm(service, user, setting, value, "Couldn't change " + setting + " to " + value);
	}

	protected SimpleUnit modifyVm(String service, String user, String setting, Integer value) {
		return modifyVm(service, user, setting, value + "", "Couldn't change " + setting + " to " + value);
	}

	protected SimpleUnit guestPropertySet(String service, String user, String property, String value, String errorMsg,
			String prerequisite) {
		return new SimpleUnit(service + "_" + property.replaceAll("-", "_") + "_" + value, prerequisite,
				"sudo -u " + user + " VBoxManage guestproperty set " + service
						+ " \"/VirtualBox/GuestAdd/VBoxService/--" + property + "\" " + value,
				"sudo -u " + user + " VBoxManage guestproperty enumerate " + service
						+ " | grep \"Name: /VirtualBox/GuestAdd/VBoxService/--" + property + ", value: " + value + "\"",
				"", "fail", errorMsg);
	}

	protected SimpleUnit guestPropertySet(String service, String user, String property, String value, String errorMsg) {
		return guestPropertySet(service, user, property, value, errorMsg, service + "_exists");
	}

	public Collection<? extends IUnit> buildIso(String service) throws InvalidServerException, InvalidServerModelException, NoValidUsersException, MalformedURLException, URISyntaxException, InvalidMachineModelException {
		// TODO Auto-generated method stub
		return null;
	}

}
