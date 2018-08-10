package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.DirOwnUnit;
import core.unit.fs.DirPermsUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileOwnUnit;
import core.unit.fs.FilePermsUnit;
import core.unit.fs.FileUnit;
import core.unit.fs.GitCloneUnit;
import core.unit.pkg.InstalledUnit;

public class HypervisorScripts extends AStructuredProfile {
	
	private String vmBase;
	private String dataDirBase;
	private String backupDirBase;
	private String storageDirBase;
	private String isoDirBase;
	private String logDirBase;

	private String scriptsBase;
	private String recoveryScriptsBase;
	private String backupScriptsBase;
	private String controlScriptsBase;
	private String watchdogScriptsBase;
	
	public HypervisorScripts() {
		super("hypervisorscripts");
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();

		vmBase      = model.getData().getVmBase(server);
		scriptsBase = vmBase + "/scripts";

		dataDirBase    = vmBase + "/data";
		backupDirBase  = vmBase + "/backup";
		storageDirBase = vmBase + "/storage";
		isoDirBase     = vmBase + "/iso";
		logDirBase     = vmBase + "/log";

		recoveryScriptsBase = scriptsBase + "/recovery";
		backupScriptsBase   = scriptsBase + "/backup";
		controlScriptsBase  = scriptsBase + "/vm";
		watchdogScriptsBase = scriptsBase + "/watchdog";
		
		units.addElement(new InstalledUnit("metal_git", "git"));
		units.addElement(new InstalledUnit("metal_duplicity", "duplicity"));

		units.addElement(new SimpleUnit("metal_qemu_nbd_enabled", "metal_qemu_utils_installed",
				"sudo modprobe nbd",
				"sudo lsmod | grep nbd", "", "fail",
				"The nbd kernel module couldn't be loaded.  Backups won't work."));
		
		model.getServerModel(server).getUserModel().addUsername("nbd");
		
		//This is for our internal backups
		units.addElement(new GitCloneUnit("backup_script", "metal_git_installed", "https://github.com/JohnKaul/rsync-time-backup.git", backupScriptsBase + "/rsync-time-backup",
				"The backup script couldn't be retrieved from github.  Backups won't work."));

		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new DirUnit("recovery_scripts_dir", "proceed", recoveryScriptsBase));
		units.addElement(new DirOwnUnit("recovery_scripts_dir", "recovery_scripts_dir_created", recoveryScriptsBase, "root"));
		units.addElement(new DirPermsUnit("recovery_scripts_dir", "recovery_scripts_dir_chowned", recoveryScriptsBase, "400"));
		
		units.addElement(new DirUnit("backup_scripts_dir", "proceed", backupScriptsBase));
		units.addElement(new DirOwnUnit("backup_scripts_dir", "backup_scripts_dir_created", backupScriptsBase, "root"));
		units.addElement(new DirPermsUnit("backup_scripts_dir", "backup_scripts_dir_chowned", backupScriptsBase, "400"));

		units.addElement(new DirUnit("control_scripts_dir", "proceed", controlScriptsBase));
		units.addElement(new DirOwnUnit("control_scripts_dir", "control_scripts_dir_created", controlScriptsBase, "root"));
		units.addElement(new DirPermsUnit("control_scripts_dir", "control_scripts_dir_chowned", controlScriptsBase, "400"));

		units.addElement(new DirUnit("watchdog_scripts_dir", "proceed", watchdogScriptsBase));
		units.addElement(new DirOwnUnit("watchdog_scripts_dir", "watchdog_scripts_dir_created", watchdogScriptsBase, "root"));
		units.addElement(new DirPermsUnit("watchdog_scripts_dir", "watchdog_scripts_dir_chowned", watchdogScriptsBase, "400"));
		
		units.addAll(recoveryScripts(server, model));
		units.addAll(backupScripts(server, model));
		units.addAll(vmControlScripts(server, model));
		units.addAll(watchdogScripts(server, model));
		
		units.addAll(adminScripts(server, model));
		
		return units;
	}

	private Vector<IUnit> watchdogScripts(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String isUpScript = "";
		isUpScript += "#!/bin/bash\n";
		isUpScript += "statusPath=" + watchdogScriptsBase + "/.status\n";
		isUpScript += "logPath=" + model.getData().getVmBase(server) + "/log/*/\n";
		isUpScript += "emailTo=" + model.getData().getAdminEmail() + "\n";
		isUpScript += "emailFrom=" + server + "." + model.getLabel() + "@" + model.getData().getDomain(server) + "\n";
		isUpScript += "emailFromRealName=\\\"ThornSec Server Daemon™ on " + server + "\\\"\n";
		isUpScript += "\n";
		isUpScript += "blacklist=(\\\"lost+found\\\")\n";
		isUpScript += "\n";
		isUpScript += "mkdir -p \\${statusPath}\n";
		isUpScript += "\n";
		isUpScript += "for dirPath in \\${logPath}\n";
		isUpScript += "do\n";
		isUpScript += "        dirPath=\\${dirPath%*/}\n";
		isUpScript += "        vmName=\\${dirPath##*/}\n";
		isUpScript += "        echo 0 > \\${statusPath}/\\${vmName}\n";
		isUpScript += "done\n";
		isUpScript += "\n";
		isUpScript += "sleep 120\n";
		isUpScript += "\n";
		isUpScript += "while true\n";
		isUpScript += "do\n";
		isUpScript += "        for dirPath in \\${logPath}\n";
		isUpScript += "        do\n";
		isUpScript += "                dirPath=\\${dirPath%*/}\n";
		isUpScript += "                vmName=\\${dirPath##*/}\n";
		isUpScript += "\n";
		isUpScript += "                counterPath=\\${statusPath}/\\${vmName}\n";
		isUpScript += "\n";
		isUpScript += "                if [[ -z \\$(printf '%s\\n' \\\"\\${blacklist[@]}\\\" | grep -w \\${vmName}) ]]\n";
		isUpScript += "                then\n";
		isUpScript += "                        counter=\\$(<\\${counterPath})\n";
		isUpScript += "\n";
		isUpScript += "                        if [ \\${dirPath}/syslog -ot \\${counterPath} ]\n";
		isUpScript += "                        then\n";
		isUpScript += "                                (( counter++ ))\n";
		isUpScript += "\n";
		isUpScript += "                                if (( \\${counter} >= 2 ))\n";
		isUpScript += "                                then\n";
		isUpScript += "                                        (\n";
		isUpScript += "                                        echo \\\"Forcibly restarting \\${vmName}\\\";\n";
		isUpScript += "                                        sudo -u vboxuser_\\${vmName} VBoxManage controlvm \\${vmName} poweroff;\n";
		isUpScript += "                                        wait \\${!};\n";
		isUpScript += "                                        sudo -u vboxuser_\\${vmName} VBoxManage startvm \\${vmName} --type headless;\n";
		isUpScript += "                                        wait \\${!}\n";
		isUpScript += "                                        ) | mutt -e \\\"set realname='\\${emailFromRealName}️' from=\\${emailFrom}\\\" -s \\\"Restarted \\${vmName} on " + server + "\\\" -n \\${emailTo}\n";
		isUpScript += "\n";
		isUpScript += "                                        counter=0\n";
		isUpScript += "                                fi\n";
		isUpScript += "\n";
		isUpScript += "                                echo \\$counter > \\${counterPath}\n";
		isUpScript += "                        fi\n";
		isUpScript += "                fi\n";
		isUpScript += "        done\n";
		isUpScript += "\n";
		isUpScript += "        sleep 1200\n";
		isUpScript += "done";

		units.addElement(new FileUnit("uptime_watchdog_script", "proceed", isUpScript, watchdogScriptsBase + "/isUp.sh"));
		units.addElement(new FileOwnUnit("uptime_watchdog_script", "uptime_watchdog_script", watchdogScriptsBase + "/isUp.sh", "root"));
		units.addElement(new FilePermsUnit("uptime_watchdog_script", "uptime_watchdog_script_chowned", watchdogScriptsBase + "/isUp.sh", "750"));

		String service = "";
		service += "[Unit]\n";
		service += "Description=VM Watchdog service\n";
		service += "After=network.target\n";
		service += "\n";
		service += "[Service]\n";
		service += "ExecStart=" + watchdogScriptsBase + "/isUp.sh\n";
		service += "KillMode=process\n";
		service += "Restart=always\n";
		service += "RestartPreventExitStatus=255\n";
		service += "\n";
		service += "[Install]\n";
		service += "WantedBy=multi-user.target";

		units.addElement(new FileUnit("uptime_watchdog_service", "proceed", service, "/lib/systemd/system/watchdog.service"));
		units.addElement(new FileOwnUnit("uptime_watchdog_service", "uptime_watchdog_service", "/lib/systemd/system/watchdog.service", "root"));
		units.addElement(new FilePermsUnit("uptime_watchdog_service", "uptime_watchdog_service_chowned", "/lib/systemd/system/watchdog.service", "644"));

		units.addElement(new SimpleUnit("uptime_watchdog_service_enabled", "uptime_watchdog_service",
				"sudo systemctl enable watchdog",
				"sudo systemctl is-enabled watchdog", "enabled", "pass",
				"Couldn't set the VM watchdog to auto-start on boot.  You will need to manually start the service (\"sudo service watchdog start\") on reboot."));
		
		return units;
	}
	
	private Vector<IUnit> backupScripts(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String backupScript = "";
		backupScript += "#!/bin/bash\n";
		backupScript += "echo \\\"=== Starting internal backup at \\$(date) ===\\\"\n";
		backupScript += "modprobe -r nbd\n";
		backupScript += "modprobe nbd max_part=15\n";
		backupScript += "\n";
		backupScript += "backupBase=" + backupDirBase + "\n";
		backupScript += "\n";
		backupScript += "for dirPath in " + dataDirBase + "/*/\n";
		backupScript += "do\n";
		backupScript += "    dirPath=\\\"\\${dirPath%*/}\\\"\n";
		backupScript += "    vmName=\\\"\\${dirPath##*/}\\\"\n";
		backupScript += "\n";
		backupScript += "    echo \\\"Backing up \\${vmName}\\\"\n";
		backupScript += "    sudo -u \\\"vboxuser_\\${vmName}\\\" \\\"VBoxManage controlvm \\${vmName} acpipowerbutton\\\"\n";
		backupScript += "    sleep 30\n";
		backupScript += "    sudo -u \\\"vboxuser_\\${vmName}\\\" \\\"VBoxManage controlvm \\${vmName} poweroff\\\"\n";
        backupScript += "    qemu-nbd -c /dev/nbd0 \\\"\\${dirPath}/\\${vmName}_data.vdi\\\"\n";
		backupScript += "    sleep 30\n";
        backupScript += "    mount /dev/nbd0p1 /mnt\n";
		backupScript += "    sleep 30\n";
        backupScript += "    ./rsync-time-backup/rsync_tmbackup.sh -s /mnt/ -d \\\"\\${backupBase}/\\${vmName}\\\"\n";
		backupScript += "    sleep 30\n";
        backupScript += "    umount /mnt\n";
		backupScript += "    sleep 30\n";
        backupScript += "    qemu-nbd --disconnect /dev/nbd0\n";
		backupScript += "    sleep 30\n";
        backupScript += "    sudo -u \\\"vboxuser_\\${vmName}\\\" \\\"VBoxManage startvm \\${vmName} --type headless\\\"\n";
		backupScript += "\n";
		backupScript += "    if [ ! \\\"\\$(ls -A \\${backupBase}/\\${vmName}/latest)\\\" ]; then\n";
		backupScript += "        echo \\\"";
		backupScript += "subject:[\\${vmName}." + model.getData().getLabel() + "] WARNING: BACKUP EMPTY\n";
		backupScript += "from:\\${vmName}." + model.getLabel() + "@" + model.getData().getDomain(server) + "\n";
		backupScript += "recipients:" + model.getData().getAdminEmail() + "\n";
		backupScript += "\n";
		backupScript += "Today's backup is empty.  This is almost certainly unexpected.";
		backupScript += "\\\"|sendmail \"" + model.getData().getAdminEmail() + "\"\n";		
		backupScript += "    fi\n";
		backupScript += "done\n";
		backupScript += "echo \\\"=== Finished internal backup at \\$(date) ===\\\"\n";
		backupScript += "\n";
		backupScript += "if [ -f external_backup.sh ]; then\n";
		backupScript += "    ./external_backup.sh\n";
		backupScript += "fi";

		units.addElement(new FileUnit("metal_backup_script", "proceed", backupScript, backupScriptsBase + "/backup.sh"));
		units.addElement(new FileOwnUnit("metal_backup_script", "metal_backup_script", backupScriptsBase + "/backup.sh", "root"));
		units.addElement(new FilePermsUnit("metal_backup_script", "metal_backup_script_chowned", backupScriptsBase + "/backup.sh", "750"));
		
		String backupCronJob = "";
		backupCronJob += "#!/bin/sh\n";
		backupCronJob += "cd " + backupScriptsBase + "\n";
		backupCronJob += "./backup.sh >> backup.log";

		units.addElement(new FileUnit("metal_backup_cron_job", "proceed", backupCronJob, "/etc/cron.daily/vm_backup"));
		units.addElement(new FileOwnUnit("metal_backup_cron_job", "metal_backup_cron_job", "/etc/cron.daily/vm_backup", "root"));
		units.addElement(new FilePermsUnit("metal_backup_cron_job", "metal_backup_cron_job_chowned", "/etc/cron.daily/vm_backup", "750"));
		
		return units;
	}
	
	private Vector<IUnit> vmControlScripts(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String stopScript = "";
		stopScript += "#!/bin/bash\n";
		stopScript += "if [ \\$# -eq 0 ]\n";
		stopScript += "then\n";
		stopScript += "    echo \\\"No parameter supplied. You need to provide the name of the VM as a parameter, or 'all' to stop all VMs\\\"\n";
		stopScript += "    exit 1;\n";
		stopScript += "fi\n";
		stopScript += "\n";
		stopScript += "vm=\\\"\\${1}\\\"\n";
		stopScript += "\n";
		stopScript += "function stopVm {\n";
		stopScript += "    vm=\\\"\\${1}\\\"\n";
		stopScript += "\n";
		stopScript += "    echo \\\"Stopping \\${vm}\\\"\n";
		stopScript += "    sudo -u vboxuser_\\\"\\${vm}\\\" VBoxManage controlvm \\\"\\${vm}\\\" poweroff\n";
		stopScript += "    wait \\${!}\n";
		stopScript += "}\n";
		stopScript += "\n";
		stopScript += "function stopAll {\n";
		stopScript += "    echo \\\"=== Stopping all VMs at \\$(date) ===\\\"\n";
		stopScript += "    for dirPath in " + dataDirBase + "/*/\n";
		stopScript += "    do\n";
		stopScript += "        dirPath=\\\"\\${dirPath%*/}\\\"\n";
		stopScript += "        vm=\\\"\\${dirPath##*/}\\\"\n";
		stopScript += "        stopVm \\\"\\${vm}\\\"\n";
		stopScript += "    done\n";
		stopScript += "echo \\\"=== Finished stopping all VMs at \\$(date) ===\\\"\n";
		stopScript += "}\n";
		stopScript += "\n";
		stopScript += "case \\\"\\${vm}\\\" in\n";
		stopScript += "    all ) stopAll;;\n";
		stopScript += "    *   ) stopVm \\\"\\${vm}\\\";;\n";
		stopScript += "esac";
		
		units.addElement(new FileUnit("stop_script", "proceed", stopScript, controlScriptsBase + "/stopVm.sh"));
		units.addElement(new FileOwnUnit("stop_script", "stop_script", controlScriptsBase + "/stopVm.sh", "root"));
		units.addElement(new FilePermsUnit("stop_script", "stop_script_chowned", controlScriptsBase + "/stopVm.sh", "750"));

		String startScript = "";
		startScript += "#!/bin/bash\n";
		startScript += "if [ \\$# -eq 0 ]\n";
		startScript += "then\n";
		startScript += "    echo \\\"No parameter supplied. You need to provide the name of the VM as a parameter, or 'all' to start all VMs\\\"\n";
		startScript += "    exit 1;\n";
		startScript += "fi\n";
		startScript += "\n";
		startScript += "vm=\\\"\\${1}\\\"\n";
		startScript += "\n";
		startScript += "function startVm {\n";
		startScript += "    vm=\\\"\\${1}\\\"\n";
		startScript += "\n";
		startScript += "    sudo -u vboxuser_\\\"\\${vm}\\\" VBoxManage startvm \\\"\\${vm}\\\" --type headless\n";
		startScript += "    wait \\${!}\n";
		startScript += "\n";
		startScript += "}\n";
		startScript += "\n";
		startScript += "function startAll {\n";
		startScript += "    echo \\\"=== Starting all VMs at \\$(date) ===\\\"\n";
		startScript += "    for dirPath in " + dataDirBase + "/*/\n";
		startScript += "    do\n";
		startScript += "        dirPath=\\\"\\${dirPath%*/}\\\"\n";
		startScript += "        vm=\\\"\\${dirPath##*/}\\\"\n";
		startScript += "        startVm \\\"\\${vm}\\\"\n";
		startScript += "    done\n";
		startScript += "echo \\\"=== Finished starting all VMs at \\$(date) ===\\\"\n";
		startScript += "}\n";
		startScript += "\n";
		startScript += "case \\\"\\${vm}\\\" in\n";
		startScript += "    all ) startAll;;\n";
		startScript += "    *   ) startVm \\\"\\${vm}\\\";;\n";
		startScript += "esac";
		
		units.addElement(new FileUnit("start_script", "proceed", startScript, controlScriptsBase + "/startVm.sh"));
		units.addElement(new FileOwnUnit("start_script", "start_script", controlScriptsBase + "/startVm.sh", "root"));
		units.addElement(new FilePermsUnit("start_script", "start_script_chowned", controlScriptsBase + "/startVm.sh", "750"));

		String deleteVmScript = "";
		deleteVmScript += "#!/bin/bash\n";
		deleteVmScript += "if [ \\$# -eq 0 ]\n";
		deleteVmScript += "then\n";
		deleteVmScript += "        echo \\\"No parameter supplied. You need to provide the name of the VM as a parameter\\\"\n";
		deleteVmScript += "        exit 1;\n";
		deleteVmScript += "fi\n";
		deleteVmScript += "\n";
		deleteVmScript += "vmName=\\${1}\n";
		deleteVmScript += "\n";
		deleteVmScript += "echo \\\"Shutting down \\${vmName}\\\"\n";
		deleteVmScript += "sudo -u vboxuser_\\\"\\${vmName}\\\" VBoxManage controlvm \\\"\\${vmName}\\\" poweroff\n";
		deleteVmScript += "wait\n";
		deleteVmScript += "\n";
		deleteVmScript += "echo \\\"Deleting \\${vmName}'s files\\\"\n";
		deleteVmScript += "rm -R \\\"" + storageDirBase + "/storage/\\${vmName}\\\" 2>/dev/null\n";
		deleteVmScript += "rm -R \\\"" + isoDirBase + "/iso/\\${vmName}\\\" 2>/dev/null\n";
		deleteVmScript += "rm -R \\\"" + logDirBase + "/log/\\${vmName}\\\" 2>/dev/null\n";
		deleteVmScript += "\n";
		deleteVmScript += "echo \\\"Deleting \\${vmName}'s user\\\"\n";
		deleteVmScript += "userdel -r -f vboxuser_\\\"\\${vmName}\\\" 2>/dev/null\n";
		deleteVmScript += "echo \\\"=== /fin/ ===\\\"";

		units.addElement(new FileUnit("delete_vm_script", "proceed", deleteVmScript, controlScriptsBase + "/deleteVm.sh"));
		units.addElement(new FileOwnUnit("delete_vm_script", "delete_vm_script", controlScriptsBase + "/deleteVm.sh", "root"));
		units.addElement(new FilePermsUnit("delete_vm_script", "delete_vm_script_chowned", controlScriptsBase + "/deleteVm.sh", "750"));
		
		return units;
	}
	
	private Vector<IUnit> recoveryScripts(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();

		//Backup recovery scripts
		String backupRecoveryScript = "";
		backupRecoveryScript += "#!/bin/bash\n";
		backupRecoveryScript += "if [ \\$# -eq 0 ]\n";
		backupRecoveryScript += "then\n";
		backupRecoveryScript += "        echo \\\"No parameter supplied. You need to provide the name of the VM as a parameter\\\"\n";
		backupRecoveryScript += "        exit 1;\n";
		backupRecoveryScript += "fi\n";
		backupRecoveryScript += "\n";
		backupRecoveryScript += "vm=\\\"${1}\\\"\n";
		backupRecoveryScript += "\n";
		backupRecoveryScript += "modprobe -r nbd\n";
		backupRecoveryScript += "modprobe nbd max_part=15\n";
		backupRecoveryScript += "\n";
		backupRecoveryScript += "echo \\\"=== Restoring latest internal backup of ${vm} at \\$(date) ===\\\"\n";
		backupRecoveryScript += "sudo -u vboxuser_\\\"\\${vm}\\\" VBoxManage controlvm \\\"\\${vm}\\\" poweroff\n";
		backupRecoveryScript += "sleep 5\n";
		backupRecoveryScript += "qemu-nbd -c /dev/nbd0 \\\"" + dataDirBase + "/\\${vm}/\\${vm}_data.vdi\\\"\n";
		backupRecoveryScript += "sleep 5\n";
		backupRecoveryScript += "mount /dev/nbd0p1 /mnt\n";
		backupRecoveryScript += "sleep 5\n";
		backupRecoveryScript += "cp -R \\\"" + backupDirBase + "/\\${vm}/latest/*\\\" /mnt/\n";
		backupRecoveryScript += "sleep 5\n";
		backupRecoveryScript += "umount /mnt\n";
		backupRecoveryScript += "sleep 5\n";
		backupRecoveryScript += "qemu-nbd --disconnect /dev/nbd0\n";
		backupRecoveryScript += "sleep 5\n";
		backupRecoveryScript += "sudo -u vboxuser_\\\"\\${vm}\\\" VBoxManage startvm \\\"\\${vm}\\\" --type headless\n";
		backupRecoveryScript += "echo \\\"=== Finished restoring latest internal backup of ${vm} at \\$(date) ===\\\"";
		
		units.addElement(new FileUnit("backup_recovery_script", "proceed", backupRecoveryScript, recoveryScriptsBase + "/recoverFromLatest.sh"));
		units.addElement(new FileOwnUnit("backup_recovery_script", "backup_recovery_script", recoveryScriptsBase + "/recoverFromLatest.sh", "root"));
		units.addElement(new FilePermsUnit("backup_recovery_script", "backup_recovery_script_chowned", recoveryScriptsBase + "/recoverFromLatest.sh", "750"));

		String mountStorageScript = "";
		mountStorageScript += "#!/bin/bash\n";
		mountStorageScript += "if [ \\$# -eq 0 ]\n";
		mountStorageScript += "then\n";
		mountStorageScript += "	   echo \\\"No parameter supplied.\\\nYou need to provide the name of the VM as a parameter\\\"\n";
		mountStorageScript += "	   exit 1;\n";
		mountStorageScript += "fi\n";
		mountStorageScript += "\n";
		mountStorageScript += "vmName=\\${1}\n";
		mountStorageScript += "\n";
		mountStorageScript += "echo \\\"=== Mounting storage ===\\\"\n";
		mountStorageScript += "modprobe -r nbd\n";
		mountStorageScript += "modprobe nbd max_part=15\n";
		mountStorageScript += "\n";
		mountStorageScript += "storageDirPath=" + storageDirBase + "\n";
		mountStorageScript += "\n";
		mountStorageScript += "echo \\\"This will mount the storage for \\${vmName} and chroot.\\\"\n";
		mountStorageScript += "echo \\\"When you are finished, type exit to restart the VM\\\"\n";
		mountStorageScript += "\n";
		mountStorageScript += "echo \\\"Shutting down \\${vmName}\\\"\n";
		mountStorageScript += "sudo -u vboxuser_\\\"\\${vmName}\\\" VBoxManage controlvm \\\"\\${vmName}\\\" acpipowerbutton\n";
		mountStorageScript += "wait \\${!}\n";
		mountStorageScript += "sleep 30s\n";
		mountStorageScript += "sudo -u vboxuser_\\\"\\${vmName}\\\" VBoxManage controlvm \\\"\\${vmName}\\\" poweroff\n";
		mountStorageScript += "wait \\${!}\n";
		mountStorageScript += "qemu-nbd -c /dev/nbd0 \\\"\\${storageDirPath}\\\"/\\${vmName}/\\\"\\${vmName}\\\"_storage.vdi\n";
		mountStorageScript += "wait \\${!}\n";
		mountStorageScript += "mount /dev/nbd0p1 /mnt\n";
		mountStorageScript += "wait \\${!}\n";
		mountStorageScript += "chroot /mnt\n";
		mountStorageScript += "wait \\${!}\n";
		mountStorageScript += "umount /mnt\n";
		mountStorageScript += "wait \\${!}\n";
		mountStorageScript += "qemu-nbd --disconnect /dev/nbd0\n";
		mountStorageScript += "wait \\${!}\n";
		mountStorageScript += "sudo -u vboxuser_\\\"\\${vmName}\\\" VBoxManage startvm \\\"\\${vmName}\\\" --type headless\n";
		mountStorageScript += "echo \\\"=== /fin/ ===\\\"";
				
		units.addElement(new FileUnit("mount_storage_script", "proceed", mountStorageScript, recoveryScriptsBase + "/mountStorage.sh"));
		units.addElement(new FileOwnUnit("mount_storage_script", "mount_storage_script", recoveryScriptsBase + "/mountStorage.sh", "root"));
		units.addElement(new FilePermsUnit("mount_storage_script", "mount_storage_script_chowned", recoveryScriptsBase + "/mountStorage.sh", "750"));

		String prevToVbox = "";
		prevToVbox += "#!/bin/bash\n";
		prevToVbox += "echo \\\"=== Restoring all vbox-prev files ===\\\"\n";
		prevToVbox += "for dirPath in " + dataDirBase + "/*/\n";
		prevToVbox += "do\n";
		prevToVbox += "    dirPath=\\${dirPath%*/}\n";
		prevToVbox += "    vm=${dirPath##*/}\n";
		prevToVbox += "    echo \\\"Fixing ${vm}\\\"\n";
		prevToVbox += "        mv \\\"/home/vboxuser_${vm}/VirtualBox VMs/${vm}/${vm}.vbox-prev\\\" \\\"/home/vboxuser_${vm}/VirtualBox VMs/${vm}/${vm}.vbox\\\"\n"; 
		prevToVbox += "done\n";
		prevToVbox += "echo \\\"=== Finished ===\\\"";

		units.addElement(new FileUnit("prev_to_vbox_script", "proceed", mountStorageScript, recoveryScriptsBase + "/prevToVbox.sh"));
		units.addElement(new FileOwnUnit("prev_to_vbox_script", "prev_to_vbox_script", recoveryScriptsBase + "/prevToVbox.sh", "root"));
		units.addElement(new FilePermsUnit("prev_to_vbox_script", "prev_to_vbox_script_chowned", recoveryScriptsBase + "/prevToVbox.sh", "750"));

		return units;
	}
	
	private Vector<IUnit> adminScripts(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String adminScript = "";
		adminScript += "#!/bin/bash\n";
		adminScript += "\n";
		adminScript += "VMS_BASE=" + dataDirBase + "\n";
		adminScript += "SCRIPTS_BASE=" + scriptsBase + "\n";
		adminScript += "CONTROL_SCRIPTS=" + controlScriptsBase + "\n";
		adminScript += "RECOVERY_SCRIPTS=" + recoveryScriptsBase + "\n";
		adminScript += "BACKUP_SCRIPTS=" + backupScriptsBase + "\n";
		adminScript += "\n";
		adminScript += "vms=\\$(find \\${VMS_BASE} -maxdepth 1 -type d ! -name '.*' -printf '%f ')\n";
		adminScript += "PS3=\\\"Number: \\\"\n";
		adminScript += "\n";
		adminScript += "function stopVM {\n";
		adminScript += "    clear\n";
		adminScript += "\n";
		adminScript += "    echo \\\"Choose a VM to stop:\\\"\n";
		adminScript += "    echo \\\"\\\"\n";
		adminScript += "\n";
		adminScript += "    select vm in \\\"\\${vms}\\\" \\\"all\\\" \\\"Back to main menu\\\" \\\"Quit to command line\\\";\n";
		adminScript += "    do\n";
		adminScript += "        case \\\"\\${vm}\\\" in\n";
		adminScript += "            \\\"Back to main menu\\\")\n";
		adminScript += "                break\n";
		adminScript += "                ;;\n";
		adminScript += "            \\\"Quit to command line\\\")\n";
		adminScript += "                exit\n";
		adminScript += "                ;;\n";
		adminScript += "            *)\n";
		adminScript += "                \\\"\\${CONTROL_SCRIPTS}\\\"/stopVm.sh \\\"\\${vm}\\\"\n";
		adminScript += "                ;;\n";
		adminScript += "        esac\n";
		adminScript += "    done\n";
		adminScript += "}\n";
		adminScript += "\n";
		adminScript += "function startVM {\n";
		adminScript += "    clear\n";
		adminScript += "\n";
		adminScript += "    echo \\\"Choose a VM to start:\\\"\n";
		adminScript += "    echo \\\"\\\"\n";
		adminScript += "\n";
		adminScript += "    select vm in \\\"\\${vms}\\\" \\\"all\\\" \\\"Back to main menu\\\" \\\"Quit to command line\\\";\n";
		adminScript += "    do\n";
		adminScript += "        case \\\"\\${vm}\\\" in\n";
		adminScript += "            \\\"Back to main menu\\\")\n";
		adminScript += "                break\n";
		adminScript += "                ;;\n";
		adminScript += "            \\\"Quit to command line\\\")\n";
		adminScript += "                exit\n";
		adminScript += "                ;;\n";
		adminScript += "            *)\n";
		adminScript += "                \\\"\\${CONTROL_SCRIPTS}\\\"/startVm.sh \\\"\\${vm}\\\"\n";
		adminScript += "                ;;\n";
		adminScript += "        esac\n";
		adminScript += "    done\n";
		adminScript += "}\n";
		adminScript += "\n";
		adminScript += "function deleteVM {\n";
		adminScript += "    clear\n";
		adminScript += "\n";
		adminScript += "    echo -e \\\"\\\\033[0;31m\\\"\n";
		adminScript += "    echo \\\"************** WARNING *************\\\"\n";
		adminScript += "    echo \\\"* THIS WILL BLINDLY DELETE YOUR VM *\\\"\n";
		adminScript += "    echo \\\"*  ~THIS ACTION CANNOT BE UNDONE~  *\\\"\n";
		adminScript += "    echo \\\"*      _YOU HAVE BEEN WARNED!_     *\\\"\n";
		adminScript += "    echo \\\"************** WARNING *************\\\"\n";
		adminScript += "    echo -e \\\"\\\\033[0m\\\"\n";
		adminScript += "\n";
		adminScript += "    echo \\\"Choose a VM to delete:\\\"\n";
		adminScript += "    echo \n";
		adminScript += "\n";
		adminScript += "    select vm in \\\"\\${vms}\\\" \\\"Back to main menu\\\" \\\"Quit to command line\\\";\n";
		adminScript += "    do\n";
		adminScript += "        case \\\"\\${vm}\\\" in\n";
		adminScript += "            \\\"Back to main menu\\\")\n";
		adminScript += "                break\n";
		adminScript += "                ;;\n";
		adminScript += "            \\\"Quit to command line\\\")\n";
		adminScript += "                exit\n";
		adminScript += "                ;;\n";
		adminScript += "            *)\n";
		adminScript += "                \\\"\\${CONTROL_SCRIPTS}\\\"/deleteVm.sh \\\"\\${vm}\\\"\n";
		adminScript += "                ;;\n";
		adminScript += "        esac\n";
		adminScript += "    done\n";
		adminScript += "}\n";
		adminScript += "\n";
		adminScript += "function internalBackups {\n";
		adminScript += "    echo -e \\\"\\\\033[0;31m\\\"\n";
		adminScript += "    echo \\\"************** WARNING *************\\\"\n";
		adminScript += "    echo \\\"*  THIS WILL STOP EACH VM IN TURN  *\\\"\n";
		adminScript += "    echo \\\"*     IN ORDER TO BACK THEM UP     *\\\"\n";
		adminScript += "    echo \\\"*  THIS WILL ALSO TRIGGER EXTERNAL *\\\"\n";
		adminScript += "    echo \\\"*    BACKUPS, IF YOU HAVE THEM     *\\\"\n";
		adminScript += "    echo \\\"*    THIS WILL TAKE SOME TIME!     *\\\"\n";
		adminScript += "    echo \\\"************** WARNING *************\\\"\n";
		adminScript += "    echo -e \\\"\\\\033[0m\\\"\n";
		adminScript += "    read -r -p \\\"Please type 'fg' to continue in the foreground, 'bg' to continue in the background, or 'c' to cancel: \\\" go\n";
		adminScript += "\n";
		adminScript += "    case \\\"\\${go}\\\" in\n";
		adminScript += "        fg ) \\\"\\${BACKUP_SCRIPTS}\\\"/backup.sh;;\n";
		adminScript += "        bg ) \\\"\\${BACKUP_SCRIPTS}\\\"/backup.sh &;;\n";
		adminScript += "        c  ) exit;;\n";
		adminScript += "    esac\n";
		adminScript += "}\n";
		adminScript += "\n";
		adminScript += "function rebuildVbox {\n";
		adminScript += "    clear\n";
		adminScript += "    \\\"\\${RECOVERY_SCRIPTS}\\\"/prevToVbox.sh\n";
		adminScript += "    sleep 5\n";
		adminScript += "}\n";
		adminScript += "\n";
		adminScript += "function restoreVmBackup {\n";
		adminScript += "    clear \n";
		adminScript += "\n";
		adminScript += "    echo \\\"Choose a VM to restore from the latest backup:\\\"\n";
		adminScript += "    echo \n";
		adminScript += "\n";
		adminScript += "    select vm in \\\"\\${vms}\\\" \\\"Back to main menu\\\" \\\"Quit to command line\\\";\n";
		adminScript += "    do\n";
		adminScript += "        case \\\"\\$vm\\\" in\n";
		adminScript += "            \\\"Back to main menu\\\")\n";
		adminScript += "                break\n";
		adminScript += "                ;;\n";
		adminScript += "            \\\"Quit to command line\\\")\n";
		adminScript += "                exit\n";
		adminScript += "                ;;\n";
		adminScript += "            *)\n";
		adminScript += "                \\\"\\${RECOVERY_SCRIPTS}\\\"/recoverFromLatest.sh \\\"\\${vm}\\\"\n";
		adminScript += "                ;;\n";
		adminScript += "        esac\n";
		adminScript += "    done\n";
		adminScript += "}\n";
		adminScript += "\n";
		adminScript += "function changePassword {\n";
		adminScript += "    read -r -p \\\"Please enter the username whose password you'd like to change: \\\" user\n";
		adminScript += "    passwd \\\"\\${user}\\\"\n";
		adminScript += "    sleep 5\n";
		adminScript += "}\n";
		adminScript += "\n";
		adminScript += "if [ \\\"\\${EUID}\\\" -ne 0 ]\n";
		adminScript += "    then echo \\\"Please run as root\\\"\n";
		adminScript += "    exit\n";
		adminScript += "fi\n";
		adminScript += "\n";
		adminScript += "while true; do\n";
		adminScript += "    clear\n";
		adminScript += "    echo \\\"Choose an option:\\\"\n";
		adminScript += "    echo \\\"1) Stop a VM\\\"\n";
		adminScript += "    echo \\\"2) Start a VM\\\"\n";
		adminScript += "    echo \\\"3) Delete a VM\\\"\n";
		adminScript += "    echo \\\"4) Start internal & external backups (manually)\\\"\n";
		adminScript += "    echo \\\"5) Rebuild VM configuration from previous\\\"\n";
		adminScript += "    echo \\\"6) Restore VM data to most recent backup\\\"\n";
		adminScript += "    echo \\\"7) Change a user's password\\\"\n";
		adminScript += "    echo \\\"Q) Quit\\\"\n";
		adminScript += "    read -r -p \\\"Select your option: \\\" opt\n";
		adminScript += "    case \\\"\\${opt}\\\" in\n";
		adminScript += "        1   ) stopVM;;\n";
		adminScript += "        2   ) startVM;;\n";
		adminScript += "        3   ) deleteVM;;\n";
		adminScript += "        4   ) internalBackups;;\n";
		adminScript += "        5   ) rebuildVbox;;\n";
		adminScript += "        6   ) restoreVmBackup;;\n";
		adminScript += "        7   ) changePassword;;\n";
		adminScript += "        q|Q ) exit;;\n";
		adminScript += "    esac\n";
		adminScript += "done";
		
		units.addElement(new FileUnit("hypervisor_admin_script", "proceed", adminScript, "/root/hvAdmin.sh"));
		units.addElement(new FileOwnUnit("hypervisor_admin_script", "hypervisor_admin_script", "/root/hvAdmin.sh", "root"));
		units.addElement(new FilePermsUnit("hypervisor_admin_script", "hypervisor_admin_script_chowned", "/root/hvAdmin.sh", "750"));
		
		return units;
	}
}
