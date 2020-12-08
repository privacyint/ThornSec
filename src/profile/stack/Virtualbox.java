/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.stack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;
import core.exception.data.InvalidPortException;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.InvalidMachineModelException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.machine.HypervisorModel;
import core.model.machine.ServiceModel;
import core.model.machine.configuration.disks.ADiskModel;
import core.model.machine.configuration.disks.DVDModel;
import core.model.machine.configuration.disks.HardDiskModel;
import core.model.network.NetworkModel;
import core.data.machine.configuration.DiskData.Medium;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.DirOwnUnit;
import core.unit.fs.DirPermsUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileOwnUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;

public class Virtualbox extends Virtualisation {

	final static String USER_PREFIX = "vboxuser_";
	final static String USER_GROUP = "vboxusers";
	
	public Virtualbox(HypervisorModel machine) {
		super(machine);
	}

	@Override
	public Collection<IUnit> getInstalled() {
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

		getServerModel().getServices().forEach((service) -> {
			// Create system user for this VM
			units.add(
				new SimpleUnit("metal_virtualbox_" + service.getLabel() + "_user",
					"metal_virtualbox_installed",
					"sudo adduser " + USER_PREFIX + service.getLabel()
						+ " --system" //System account, no aging info
						+ " --shell=/bin/false" //Disables console
						+ " --disabled-login"
						+ " --ingroup " + USER_GROUP,
					"id -u " + USER_PREFIX + service.getLabel() + " 2>&1 | grep 'no such user'", 
					"",
					"pass",
					"Couldn't create the user for " + service.getLabel()
						+ " on its HyperVisor."
						+ " This is fatal, " + service.getLabel() + " will not be installed."));
		});

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		getNetworkModel().getServerModel(getLabel()).getAptSourcesModel().addAptSource("virtualbox", "deb http://download.virtualbox.org/virtualbox/debian buster contrib", "keyserver.ubuntu.com", "a2f683c52980aecf");
		getNetworkModel().getServerModel(getLabel()).addEgress("download.virtualbox.org:80");

		return units;
	}

	@Override
	protected Collection<IUnit> buildDisks(ServiceModel service) {
		final Collection<IUnit> units = new ArrayList<>();

		String user = USER_PREFIX + service.getLabel();

		Set<HardDiskModel> hdds = service.getDisks()
										 .values()
										 .stream()
										 .filter(disk -> disk instanceof HardDiskModel)
										 .map(HardDiskModel.class::cast)
										 .collect(Collectors.toSet());

		Set<DVDModel> dvds = service.getDisks()
									.values()
									.stream()
									.filter(disk -> disk instanceof DVDModel)
									.map(DVDModel.class::cast)
									.collect(Collectors.toSet());

		// Disk controller setup
		units.add(new SimpleUnit(service.getLabel() + "_hdds_sas_controller", service + "_exists",
						"sudo -u " + user
							+ " VBoxManage storagectl " + service.getLabel()
								+ " --name \"HDDs\""
								+ " --add sas"
								+ " --controller LSILogicSAS"
								+ " --portcount " + hdds.size()
								+ " --hostiocache off",
				"sudo -u " + user
						+ " VBoxManage showvminfo " + service.getLabel()
							+ " --machinereadable | grep ^storagecontrollername0=",
				"storagecontrollername0=\\\"HDDs\\\"", "pass",
				"The hard drive SAS controller for " + service.getLabel()
					+ " (where its disks are attached) Couldn't be"
					+ " created/attached to " + service.getLabel() + "."
					+ " This is fatal, " + service.getLabel() + " will not be installed."));

		units.add(new SimpleUnit(service + "_dvds_ide_controller", service.getLabel() + "_exists",
						"sudo -u " + user
							+ " VBoxManage storagectl " + service
								+ " --name \"DVDs\""
								+ " --add ide"
								+ " --controller PIIX4"
								+ " --hostiocache off",
				"sudo -u " + user + " VBoxManage showvminfo " + service.getLabel()
						+ " --machinereadable | grep ^storagecontrollername1=",
				"storagecontrollername1=\\\"DVDs\\\"", "pass",
				"The DVD IDE controller for " + service.getLabel() + " (where its"
						+ " installation/booting medium is attached) couldn't be"
						+ " created/attached to " + service.getLabel() + "."
						+ " This is fatal, " + service + " will not be bootable."));

		int deviceCounter = 0;
		hdds.forEach((disk) -> {
			//Make sure its directories exists, with the correct ownership
			units.add(new DirUnit(disk.getLabel() + "_disk_dir_" + service.getLabel(),
								  "proceed",
								  disk.getFilePath(),
								  user,
								  USER_GROUP,
							  	600,
							  	""
					 )
			);
			units.add(new DirUnit(disk.getLabel() + "_disk_loopback_dir_" + service.getLabel(),
								  "proceed",
								  disk.getFilePath() + "/live/"
					)
			);

			//Build the disk
			units.add(new SimpleUnit(disk.getLabel() + "_disk_" + service.getLabel() + "_created",
							service.getLabel() + "_exists",
							"sudo -u " + user
								+ " VBoxManage createmedium"
									+ " --filename " + FilenameUtils.normalize(disk.getFilename(), true)
									+ " --size " + disk.getSize()
									+ " --format " + disk.getFormat()
									+ ((disk.getDiffParent().isPresent())
										? " --diffparent " + disk.getDiffParent().get()
										: ""
									  ),
							"sudo [ -f " + FilenameUtils.normalize(disk.getFilename(), true) + " ] && echo pass;",
							"pass",
							"pass",
							"Couldn't create the disk " + disk.getLabel() + " for " + service + "."
					 )
			);

			//Attach the disk
			units.add(new SimpleUnit(disk.getLabel() + "_disk_" + service.getLabel() + "_attached",
						service.getLabel() + "_hdds_sas_controller",
						"sudo -u " + user
							+ " VBoxManage storageattach"
								+ " --storagectl \"HDDs\""
								+ " --port" + deviceCounter
								+ " --device 0"
								+ " --type hdd"
								+ " --medium " + FilenameUtils.normalize(disk.getFilename(), true),
						"sudo -u " + user + " VBoxManage showvminfo " + service.getLabel() + " --machinereadable | grep \"HDDs-" + deviceCounter + "-0\"",
						"\\\"HDDs-" + deviceCounter + "-0\\\"=\\\"" + FilenameUtils.normalize(disk.getFilename(), true) + "\\\"",
						"pass",
						"Couldn't attach disk " + disk.getLabel() + "for " + service + "."
					)
			);

			deviceCounter++;
		});

		deviceCounter = 0;
		dvds.forEach((disk) -> {
			//Attach the disk
			units.add(new SimpleUnit(disk.getLabel() + "_disk_" + service.getLabel() + "_attached",
						service.getLabel() + "_dvds_ide_controller",
						"sudo -u " + user
							+ " VBoxManage storageattach"
								+ " --storagectl \"DVDs\""
								+ " --port" + deviceCounter
								+ " --device 0"
								+ " --type dvddrive"
								+ " --medium " + FilenameUtils.normalize(disk.getFilename(), true),
						"sudo -u " + user + " VBoxManage showvminfo " + service.getLabel() + " --machinereadable | grep \"DVDs-0-" + deviceCounter + "\"",
						"\\\"DVDs-0-" + deviceCounter + "\\\"=\\\"" + FilenameUtils.normalize(disk.getFilename(), true) + "\\\"",
						"pass",
						"Couldn't attach disk " + disk.getLabel() + "for " + service + "."
					)
			);

			deviceCounter++;
		});

		return units;
	}

	@Override
	protected Collection<IUnit> buildBackups(ServiceModel service) {
		Collection<IUnit> units = new ArrayList<>();
		
		units.add(new DirUnit("log_dir_" + service, "proceed", logDir));
		units.add(new DirOwnUnit("log_dir_" + service, "log_dir_" + service + "_created", logDir, user, group));
		units.add(new DirPermsUnit("log_dir_" + service, "log_dir_" + service + "_chowned", logDir, "750"));

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
	
	@Override
	protected Collection<IUnit> buildLogs(ServiceModel service) {
		Collection<IUnit> units = new ArrayList<>();
		
		units.add(new DirUnit("backup_dir_" + service, "proceed", backupDir));
		units.add(new DirOwnUnit("backup_dir_" + service, "backup_dir_" + service + "_created", backupDir, user,
				group));
		units.add(new DirPermsUnit("backup_dir_" + service, "backup_dir_" + service + "_chowned", backupDir,
				"750"));
		// Mark the backup destination directory as a valid destination
		units.add(new FileUnit(service + "_mark_backup_dir", "backup_dir_" + service + "_chmoded",
				backupDir + "/backup.marker", "In memoriam Luke and Guy.  Miss you two!"));
		
		return units;
	}

	@Override
	public Collection<IUnit> buildServiceVm(ServiceModel service, String bridge)
			throws InvalidServerException, InvalidMachineModelException {
		final String baseDir = getNetworkModel().getData().getHypervisorThornsecBase(getLabel());

		final String logDir = baseDir + "/logs/" + service;
		final String backupDir = baseDir + "/backups/" + service;
		final String ttySocketDir = baseDir + "/sockets/" + service;

		// final String installIso = baseDir + "/isos/" + service + "/" + service +
		// ".iso";
		final String user = "vboxuser_" + service;
		final String group = "vboxusers";

		final Collection<IUnit> units = new ArrayList<>();

		// Create VM itself
		units.add(new SimpleUnit(service + "_exists", "metal_virtualbox_" + service + "_user",
				"sudo -u " + user + " VBoxManage createvm --name " + service + " --ostype \"" + group + "\""
						+ " --register;" + "sudo -u " + user + " VBoxManage modifyvm " + service + " --description "
						+ "\"" + service + "." + getNetworkModel().getData().getDomain() + "\n"
						+ "ThornSec guest machine\n" + "Built with profile(s): "
						+ String.join(", ", getNetworkModel().getData().getProfiles(service)) + "\n"
						+ "Built at $(date)" + "\"",
				"sudo -u " + user + " VBoxManage list vms | grep " + service, "", "fail", "Couldn't create " + service
						+ " on its HyperVisor.  This is fatal, " + service + " will not exist on your network."));

		// Set up VM's storage
		units.addAll(buildDisks(user, group, service, getNetworkModel().getServiceModel(service).getDisks()));
		//Make sure Logs are attached
		units.addAll(buildLogs(service, logDir, user, group));
		//And the backups
		units.addAll(buildBackups(service, backupDir, user, group));

		units.add(new DirUnit("socket_dir_" + service, "proceed", ttySocketDir));
		units.add(new DirOwnUnit("socket_dir_" + service, "socket_dir_" + service + "_created", ttySocketDir, user,
				group));
		units.add(new DirPermsUnit("socket_dir_" + service, "socket_dir_" + service + "_chowned", ttySocketDir, "750"));

		// Architecture setup
		units.add(modifyVm(service, user, "paravirtprovider", "kvm")); // Default, make it explicit
		units.add(modifyVm(service, user, "chipset", "ich9"));
		units.add(modifyVm(service, user, "ioapic", "on", "IO APIC couldn't be enabled for " + service
				+ ".  This is required for 64-bit installations, and for more than 1 virtual CPU in a service."));
		units.add(modifyVm(service, user, "hwvirtex", "on"));
		units.add(modifyVm(service, user, "pae", "on"));
		units.add(modifyVm(service, user, "cpus", getNetworkModel().getData().getCPUs(service)));
		units.add(modifyVm(service, user, "cpuexecutioncap", getNetworkModel().getData().getCPUExecutionCap(service)));

		// RAM setup
		units.add(modifyVm(service, user, "memory", getNetworkModel().getData().getRAM(service)));
		units.add(modifyVm(service, user, "vram", "16"));
		units.add(modifyVm(service, user, "nestedpaging", "on"));
		units.add(modifyVm(service, user, "largepages", "on"));

		// Audio setup (switch it off)
		units.add(modifyVm(service, user, "audio", "none"));

		// Use high precision event timers instead of legacy
		units.add(modifyVm(service, user, "hpet", "on"));

		// Shared folders setup
		units.add(new SimpleUnit(service + "_backup_sf_attached", service + "_exists",
				"sudo -u " + user + " VBoxManage sharedfolder add " + service + " --name backup --hostpath "
						+ backupDir,
				"sudo -u " + user + " VBoxManage showvminfo " + service
						+ " --machinereadable | grep SharedFolderPathMachineMapping2",
				"SharedFolderPathMachineMapping2=\\\"" + backupDir + "\\\"", "pass"));

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

		getNetworkModel().getServerModel(getLabel())
				.addProcessString("/usr/lib/virtualbox/VBoxHeadless --comment " + service + " --startvm `if id '" + user
						+ "' >/dev/null 2>&1; then sudo -u " + user + " bash -c 'VBoxManage list runningvms | grep "
						+ service + "' | awk '{ print $2 }' | tr -d '{}'; else echo ''; fi` --vrde config *$");
		getNetworkModel().getServerModel(getLabel()).addProcessString("awk \\{");
		getNetworkModel().getServerModel(getLabel()).addProcessString("tr -d \\{\\}$");
		getNetworkModel().getServerModel(getLabel()).getUserModel().addUsername(user);

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
}
