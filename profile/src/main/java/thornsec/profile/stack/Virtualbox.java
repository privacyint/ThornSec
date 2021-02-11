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
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;
import core.exception.data.InvalidPortException;
import core.exception.runtime.InvalidMachineModelException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.machine.HypervisorModel;
import core.model.machine.ServiceModel;
import core.model.machine.configuration.disks.DVDModel;
import core.model.machine.configuration.disks.HardDiskModel;
import core.unit.SimpleUnit;
import core.unit.fs.DirUnit;
import core.unit.pkg.InstalledUnit;
import inet.ipaddr.HostName;

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
		try {
			getServerModel().addEgress(new HostName("download.virtualbox.org:80"));
		} catch (InvalidPortException e) {
			// Shouldn't be able to get here!
			e.printStackTrace();
		}

		return new ArrayList<>();
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
		for (HardDiskModel disk : hdds) {
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
		}

		deviceCounter = 0;
		for (DVDModel disk : dvds) {
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
		}

		return units;
	}

	@Override
	protected Collection<IUnit> buildBackups(ServiceModel service) {
		Collection<IUnit> units = new ArrayList<>();

		String path = FilenameUtils.normalize(getServerModel().getVMBase().getPath() + "/backups", true);

		units.add(new DirUnit(service.getLabel() + "_backup_dir",
							  "proceed",
							  path,
							  "root",
							  "root",
							  700,
							  "Couldn't create " + service.getLabel() + "'s backup directory"
				)
		);

		units.add(new SimpleUnit(service.getLabel() + "_backup_sf_attached", service + "_exists",
				"sudo -u " + getServiceUser(service)
					+ " VBoxManage sharedfolder add " + service.getLabel()
						+ " --name backups"
						+ " --hostpath " + path,
				"sudo -u " + getServiceUser(service)+ " VBoxManage showvminfo " + service
						+ " --machinereadable | grep SharedFolderPathMachineMapping2",
				"SharedFolderPathMachineMapping1=\\\"" + path + "\\\"", "pass",
				"Couldn't attach the backups folder to " + service + "."
					+ " This means backups will not be accessible from the VM."));

		return units;
	}
	
	@Override
	protected Collection<IUnit> buildLogs(ServiceModel service) {
		Collection<IUnit> units = new ArrayList<>();

		String path = FilenameUtils.normalize(getServerModel().getVMBase().getPath() + "/logs", true);

		units.add(new DirUnit(service.getLabel() + "_log_dir",
							  "proceed",
							  path,
							  getServiceUser(service),
							  USER_GROUP,
							  750,
							  "Couldn't create " + service.getLabel() + "'s log directory"
				 )
		);

		units.add(new SimpleUnit(service.getLabel() + "_log_sf_attached", service + "_exists",
				"sudo -u " + getServiceUser(service)
					+ " VBoxManage sharedfolder add " + service.getLabel()
						+ " --name logs"
						+ " --hostpath " + path + ";"
				+ "sudo -u " + getServiceUser(service)
					+ " VBoxManage setextradata " + service
						+ " VBoxInternal1/SharedFoldersEnableSymlinksCreate/logs 1",
				"sudo -u " + getServiceUser(service)
					+ " VBoxManage showvminfo " + service + " --machinereadable"
				+ " | grep SharedFolderPathMachineMapping1",
				"SharedFolderPathMachineMapping1=\\\"" + path + "\\\"", "pass",
				"Couldn't attach the logs folder to " + service + "."
					+ " This means logs will only exist in the VM."));

		return units;
	}

	@Override
	public Collection<IUnit> buildServiceVm(ServiceModel service, String bridge) {
		final String baseDir = FilenameUtils.normalize(getServerModel().getVMBase().toString(), true);

		final String ttySocketDir = baseDir + "/sockets/" + service.getLabel();

		final String user = getServiceUser(service);

		final String osType = "Linux";

		final Collection<IUnit> units = new ArrayList<>();

		// Create VM itself
		units.add(new SimpleUnit(service + "_exists", "metal_virtualbox_" + service + "_user",
				"sudo -u " + user + " VBoxManage createvm"
							+ " --name " + service
							+ " --ostype \"" + osType + "\""
							+ " --register;"
						+ "sudo -u " + user + " VBoxManage modifyvm " + service
							+ " --description \"" + service.getLabel() + "." + service.getDomain() + "\n"
								+ "ThornSec guest machine\n"
								+ "Built with profile(s): "
								+ String.join(", ", service.getProfiles().keySet()) + "\n"
								+ "Built at $(date)" + "\"",
				"sudo -u " + user + " VBoxManage list vms | grep " + service, "", "fail",
				"Couldn't create " + service + " on its HyperVisor."
						+ " This is fatal, " + service + " will not exist on your network.")
		);

		// Set up VM's storage
		units.addAll(buildDisks(service));
		//Make sure Logs are attached
		units.addAll(buildLogs(service));
		//And the backups
		units.addAll(buildBackups(service));

		units.add(new DirUnit("socket_dir_" + service, "proceed", ttySocketDir));

		// Architecture setup
		units.add(modifyVm(service, "paravirtprovider", "kvm")); // Default, make it explicit
		units.add(modifyVm(service, "chipset", "ich9"));
		units.add(modifyVm(service, "ioapic", "on",
				"IO APIC couldn't be enabled for " + service + "."
				+ " This is required for 64-bit installations, and for more than"
				+ " 1 virtual CPU in a service."));
		units.add(modifyVm(service, "hwvirtex", "on"));
		units.add(modifyVm(service, "pae", "on"));
		units.add(modifyVm(service, "cpus", service.getCPUs()));
		units.add(modifyVm(service, "cpuexecutioncap", service.getCPUExecutionCap()));

		// RAM setup
		units.add(modifyVm(service, "memory", service.getRAM()));
		units.add(modifyVm(service, "vram", 16));
		units.add(modifyVm(service, "nestedpaging", "on"));
		units.add(modifyVm(service, "largepages", "on"));

		// Audio setup (switch it off)
		units.add(modifyVm(service, user, "audio", "none"));

		// Use high precision event timers instead of legacy
		units.add(modifyVm(service, user, "hpet", "on"));

		// Clock setup to try and stop drift between host and guest
		// https://www.virtualbox.org/manual/ch09.html#changetimesync
		units.add(guestPropertySet(service, "timesync-interval", 10000,
				"Couldn't sync the clock between " + service.getLabel()
					+ " and its metal. You'll probably see some clock drift in "
					+ service.getLabel() + " as a result."));
		units.add(guestPropertySet(service, "timesync-min-adjust", 100,
				"Couldn't sync the clock between " + service.getLabel()
					+ " and its metal. You'll probably see some clock drift in "
					+ service.getLabel() + " as a result."));
		units.add(guestPropertySet(service, "timesync-set-on-restore", 1,
				"Couldn't sync the clock between " + service.getLabel()
					+ " and its metal. You'll probably see some clock drift in "
					+ service.getLabel() + " as a result."));
		units.add(guestPropertySet(service, "timesync-set-threshold", 1000,
				"Couldn't sync the clock between " + service.getLabel()
					+ " and its metal. You'll probably see some clock drift in "
					+ service.getLabel() + " as a result."));

		// tty0 socket
		units.add(new SimpleUnit(service.getLabel() + "_tty0_com_port", service.getLabel() + "_exists",
				"sudo -u " + user
					+ " VBoxManage modifyvm " + service.getLabel()
						+ " --uart1 0x3F8 4",
				"sudo -u " + user
					+ " VBoxManage showvminfo " + service.getLabel() + " --machinereadable"
					+ " | grep ^uart1=",
				"uart1=\\\"0x03f8,4\\\"", "pass"));

		units.add(new SimpleUnit(service.getLabel() + "_tty0_socket", service.getLabel() + "_tty0_com_port",
				"sudo -u " + user
					+ " VBoxManage modifyvm " + service.getLabel()
						+ " --uartmode1 server " + ttySocketDir + "/vboxttyS0",
				"sudo -u " + user
					+ " VBoxManage showvminfo " + service.getLabel()
						+ " --machinereadable"
					+ " | grep ^uartmode1=",
				"uartmode1=\\\"server," + ttySocketDir + "/vboxttyS0\\\"", "pass"));

		units.add(new SimpleUnit(service.getLabel() + "_tty1_com_port", service.getLabel() + "_exists",
				"sudo -u " + user
					+ " VBoxManage modifyvm " + service.getLabel()
						+ " --uart2 0x2F8 3",
				"sudo -u " + user
					+ " VBoxManage showvminfo " + service.getLabel() + " --machinereadable"
					+ " | grep ^uart2=",
				"uart2=\\\"0x02f8,3\\\"", "pass"));

		units.add(new SimpleUnit(service.getLabel() + "_tty1_socket", service.getLabel() + "_tty1_com_port",
				"sudo -u " + user
					+ " VBoxManage modifyvm " + service.getLabel()
					+ " --uartmode2 server " + ttySocketDir	+ "/vboxttyS1",
				"sudo -u " + user
					+ " VBoxManage showvminfo " + service.getLabel() + " --machinereadable"
					+ " | grep ^uartmode2=",
				"uartmode2=\\\"server," + ttySocketDir + "/vboxttyS1\\\"", "pass"));

		//TODO: add running vbox process string
		getServerModel().getUserModel().addUsername(user);

		return units;
	}

	/**
	 * Modifies the "hardware" of the VM in some way.
	 * 
	 * https://www.virtualbox.org/manual/ch08.html#vboxmanage-modifyvm
	 * @param service The service being modified
	 * @param setting What we're modifying
	 * @param value What to set it to
	 * @param errorMsg Error to return if we were unable to set
	 * @param prerequisite What needs to be in place before this runs
	 * @return
	 */
	protected SimpleUnit modifyVm(ServiceModel service, String setting, String value, String errorMsg, String prerequisite) {

		String check = "";

		// Integers aren't quoted...
		if (value.matches("-?(0|[1-9]\\d*)")) {
			check = setting + "=" + value;
		}
		else {
			check = setting + "=\\\"" + value + "\\\"";
		}

		return new SimpleUnit(service + "_" + setting + "_" + value, prerequisite,
				"sudo -u " + getServiceUser(service)
					+ " VBoxManage modifyvm " + service.getLabel()
						+ " --" + setting + " " + value,
				"sudo -u " + getServiceUser(service)
					+ " VBoxManage showvminfo " + service + " --machinereadable"
					+ " | grep ^" + setting + "=",
				check, "pass", errorMsg);
	}

	protected SimpleUnit modifyVm(ServiceModel service, String setting, String value, String errorMsg) {
		return modifyVm(service, setting, value, errorMsg, service + "_exists");
	}

	protected SimpleUnit modifyVm(ServiceModel service, String setting, String value) {
		return modifyVm(service, setting, value, "Couldn't change " + setting + " to " + value);
	}

	protected SimpleUnit modifyVm(ServiceModel service, String setting, Integer value) {
		return modifyVm(service, setting, value + "", "Couldn't change " + setting + " to " + value);
	}

	/**
	 * Sets a property on the Guest VM.
	 * 
	 * This can either be arbitrary strings for low-volume communication between
	 * host and guest (https://www.virtualbox.org/manual/ch08.html#vboxmanage-guestproperty) 
	 * or for guest additions-related settings (https://www.virtualbox.org/manual/ch04.html#guestadd-guestprops)
	 * @param service The service to set the property on
	 * @param property The property to set
	 * @param value What to set it to
	 * @param errorMsg Error to return if we were unable to set
	 * @param prerequisite What needs to be in place before this runs
	 * @return
	 */
	protected SimpleUnit guestPropertySet(ServiceModel service, String property, String value, String errorMsg,
			String prerequisite) {

		return new SimpleUnit(service.getLabel() + "_" + property.replaceAll("-", "_") + "_" + value, prerequisite,
				"sudo -u " + getServiceUser(service)
					+ " VBoxManage guestproperty set " + service.getLabel() + " \"/VirtualBox/GuestAdd/VBoxService/--" + property + "\" " + value,
				"sudo -u " + getServiceUser(service)
					+ " VBoxManage guestproperty enumerate " + service.getLabel()
					+ " | grep \"Name: /VirtualBox/GuestAdd/VBoxService/--" + property + ", value: " + value + "\"",
				"", "fail", errorMsg);
	}

	protected SimpleUnit guestPropertySet(ServiceModel service, String property, Integer value, String errorMsg) {
		return guestPropertySet(service, property, value.toString(), errorMsg, service.getLabel() + "_exists");
	}

	protected String getServiceUser(ServiceModel service) {
		return USER_PREFIX + service.getLabel();		
	}
}
