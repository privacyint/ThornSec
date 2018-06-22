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
	
	private String recoveryScriptsBase;
	private String backupScriptsBase;
	private String controlScriptsBase;
	private String watchdogScriptsBase; 
	
	public HypervisorScripts() {
		super("hypervisorscripts");
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();

		String scriptsBase = model.getData().getVmBase(server) + "/scripts";
		
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
		isUpScript += "                                        echo \\\"Forcibly restarting \\${vmName}\\n\\\";\n";
		isUpScript += "                                        sudo -u vboxuser_\\${vmName} VBoxManage controlvm \\${vmName} poweroff;\n";
		isUpScript += "                                        wait \\${!};\n";
		isUpScript += "                                        sudo -u vboxuser_\\${vmName} VBoxManage startvm \\${vmName} --type headless;\n";
		isUpScript += "                                        wait \\${!}\n";
		isUpScript += "                                        ) | mutt -e \\\"set realname='\\${emailFromRealName}️' from=\\${emailFrom}\\\" -s \\\"Restarted ${vmName}\\\" -n \\${emailTo}\n";
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
		backupScript += "echo \\\"=== Starting internal backup at \\`date\\` ===\\\"\n";
		backupScript += "modprobe -r nbd\n";
		backupScript += "modprobe nbd max_part=15\n";
		backupScript += "\n";
		backupScript += "backupBase=" + model.getData().getVmBase(server) + "/backup\n";
		backupScript += "\n";
		backupScript += "for dirPath in " + model.getData().getVmBase(server) + "/data/*/\n";
		backupScript += "do\n";
		backupScript += "    dirPath=\\${dirPath%*/}\n";
		backupScript += "    vmName=\\${dirPath##*/}\n";
		backupScript += "\n";
		backupScript += "    echo \\\"Backing up \\${vmName}\\\"\n";
		backupScript += "    sudo -u vboxuser_\\${vmName} VBoxManage controlvm \\${vmName} acpipowerbutton\n";
		backupScript += "    sleep 30\n";
		backupScript += "    sudo -u vboxuser_\\${vmName} VBoxManage controlvm \\${vmName} poweroff\n";
        backupScript += "    qemu-nbd -c /dev/nbd0 \\${dirPath}/\\${vmName}_data.vdi\n";
		backupScript += "    sleep 30\n";
        backupScript += "    mount /dev/nbd0p1 /mnt\n";
		backupScript += "    sleep 30\n";
        backupScript += "    ./rsync-time-backup/rsync_tmbackup.sh -s /mnt/ -d \\${backupBase}/\\${vmName}\n";
		backupScript += "    sleep 30\n";
        backupScript += "    umount /mnt\n";
		backupScript += "    sleep 30\n";
        backupScript += "    qemu-nbd --disconnect /dev/nbd0\n";
		backupScript += "    sleep 30\n";
        backupScript += "    sudo -u vboxuser_\\${vmName} VBoxManage startvm \\${vmName} --type headless\n";
		backupScript += "\n";
		backupScript += "    if [ ! \\\"\\$(ls -A \\${backupBase}/\\${vmName}/latest)\\\" ]; then\n";
		backupScript += "        echo -e \\\"";
		backupScript += "            subject:[\\${vmName}." + model.getData().getLabel() + "] WARNING: BACKUP EMPTY\\n";
		backupScript += "            from:\\${vmName}." + model.getLabel() + "@" + model.getData().getDomain(server) + "\\n";
		backupScript += "            recipients:" + model.getData().getAdminEmail() + "\\n";
		backupScript += "            \\n";
		backupScript += "            Today's backup is empty.  This is almost certainly unexpected.";
		backupScript += "        \\\"";
		backupScript += "        |sendmail \"" + model.getData().getAdminEmail() + "\"\n";		
		backupScript += "    fi\n";
		backupScript += "done\n";
		backupScript += "echo \\\"=== Finished internal backup at \\`date\\` ===\\\"\n";
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
		
		String stopAllScript = "";
		stopAllScript += "#!/bin/bash\n";
		stopAllScript += "echo \\\"=== Stopping all VMs at \\`date\\` ===\\\"\n";
		stopAllScript += "for dirPath in " + model.getData().getVmBase(server) + "/data/*/\n";
		stopAllScript += "do\n";
		stopAllScript += "    dirPath=\\${dirPath%*/}\n";
		stopAllScript += "    vmName=\\${dirPath##*/}\n";
		stopAllScript += "    echo \\\"Stopping \\${vmName}\\\"\n";
		stopAllScript += "    sudo -u vboxuser_\\${vmName} VBoxManage controlvm \\${vmName} poweroff\n";
		stopAllScript += "    wait \\${!}\n";
		stopAllScript += "done\n";
		stopAllScript += "echo \\\"=== Finished stopping all VMs at \\`date\\` ===\\\"";
		
		units.addElement(new FileUnit("stop_all_script", "proceed", stopAllScript, controlScriptsBase + "/stopAll.sh"));
		units.addElement(new FileOwnUnit("stop_all_script", "stop_all_script", controlScriptsBase + "/stopAll.sh", "root"));
		units.addElement(new FilePermsUnit("stop_all_script", "stop_all_script_chowned", controlScriptsBase + "/stopAll.sh", "750"));

		String startAllScript = "";
		startAllScript += "#!/bin/bash\n";
		startAllScript += "echo \\\"=== Starting all VMs at \\`date\\` ===\\\"\n";
		startAllScript += "for dirPath in " + model.getData().getVmBase(server) + "/data/*/\n";
		startAllScript += "do\n";
		startAllScript += "    dirPath=\\${dirPath%*/}\n";
		startAllScript += "    vmName=\\${dirPath##*/}\n";
		startAllScript += "    sudo -u vboxuser_\\${vmName} VBoxManage startvm \\${vmName} --type headless\n";
		startAllScript += "    wait \\${!}\n";
		startAllScript += "done\n";
		startAllScript += "echo \\\"=== Finished starting all VMs at \\`date\\` ===\\\"";
		
		units.addElement(new FileUnit("start_all_script", "proceed", startAllScript, controlScriptsBase + "/startAll.sh"));
		units.addElement(new FileOwnUnit("start_all_script", "start_all_script", controlScriptsBase + "/startAll.sh", "root"));
		units.addElement(new FilePermsUnit("start_all_script", "start_all_script_chowned", controlScriptsBase + "/startAll.sh", "750"));

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
		deleteVmScript += "sudo -u vboxuser_\\${vmName} VBoxManage controlvm \\${vmName} poweroff\n";
		deleteVmScript += "wait\n";
		deleteVmScript += "\n";
		deleteVmScript += "echo \\\"Deleting \\${vmName}'s files\\\"\n";
		deleteVmScript += "rm -R " + model.getData().getVmBase(server) + "/storage/\\${vmName} 2>/dev/null\n";
		deleteVmScript += "rm -R " + model.getData().getVmBase(server) + "/iso/\\${vmName} 2>/dev/null\n";
		deleteVmScript += "rm -R " + model.getData().getVmBase(server) + "/data/\\${vmName} 2>/dev/null\n";
		deleteVmScript += "rm -R " + model.getData().getVmBase(server) + "/log/\\${vmName} 2>/dev/null\n";
		deleteVmScript += "\n";
		deleteVmScript += "echo \\\"Deleting \\${vmName}'s user\\\"\n";
		deleteVmScript += "userdel -r -f vboxuser_\\${vmName} 2>/dev/null\n";
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
		backupRecoveryScript += "echo \\\"=== Restoring latest internal backup at \\`date\\` ===\\\"\n";
		backupRecoveryScript += "for dirPath in " + model.getData().getVmBase(server) + "/data/*/\n";
		backupRecoveryScript += "do\n";
		backupRecoveryScript += "    dirPath=\\${dirPath%*/}\n";
		backupRecoveryScript += "    vmName=\\${dirPath##*/}\n";
		backupRecoveryScript += "    echo \\\"Restoring \\${vmName} from latest backup\\\"\n";
		backupRecoveryScript += "    sudo -u vboxuser_\\${vmName} VBoxManage controlvm \\${vmName} poweroff\n";
		backupRecoveryScript += "    wait \\${!}\n";
		backupRecoveryScript += "    qemu-nbd -c /dev/nbd0 \\${dirPath}/\\${vmName}/\\${vmName}_data.vdi\n";
		backupRecoveryScript += "    wait \\${!}\n";
		backupRecoveryScript += "    mount /dev/nbd0p1 /mnt\n";
		backupRecoveryScript += "    wait \\${!}\n";
		backupRecoveryScript += "    cp -R " + model.getData().getVmBase(server) + "/backup/\\${vmName}/latest/* /mnt/\n";
		backupRecoveryScript += "    wait \\${!}\n";
		backupRecoveryScript += "    umount /mnt\n";
		backupRecoveryScript += "    wait \\${!}\n";
		backupRecoveryScript += "    qemu-nbd --disconnect /dev/nbd0\n";
		backupRecoveryScript += "    wait \\${!}\n";
		backupRecoveryScript += "    sudo -u vboxuser_\\${vmName} VBoxManage startvm \\${vmName} --type headless\n";
		backupRecoveryScript += "done\n";
		backupRecoveryScript += "echo \\\"=== Finished restoring latest backup at \\`date\\` ===\\\"";
		
		units.addElement(new FileUnit("backup_recovery_script", "proceed", backupRecoveryScript, recoveryScriptsBase + "/recoverAllFromLatest.sh"));
		units.addElement(new FileOwnUnit("backup_recovery_script", "backup_recovery_script", recoveryScriptsBase + "/recoverAllFromLatest.sh", "root"));
		units.addElement(new FilePermsUnit("backup_recovery_script", "backup_recovery_script_chowned", recoveryScriptsBase + "/recoverAllFromLatest.sh", "750"));

		
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
		mountStorageScript += "dirPath=" + model.getData().getVmBase(server) + "\n";
		mountStorageScript += "\n";
		mountStorageScript += "echo \\\"This will mount the storage for \\${vmName} and chroot.\\\"\n";
		mountStorageScript += "echo \\\"When you are finished, type exit to restart the VM\\\"\n";
		mountStorageScript += "\n";
		mountStorageScript += "echo \\\"Shutting down \\${vmName}\\\"\n";
		mountStorageScript += "sudo -u vboxuser_\\${vmName} VBoxManage controlvm \\${vmName} acpipowerbutton\n";
		mountStorageScript += "wait \\${!}\n";
		mountStorageScript += "sleep 30s\n";
		mountStorageScript += "sudo -u vboxuser_\\${vmName} VBoxManage controlvm \\${vmName} poweroff\n";
		mountStorageScript += "wait \\${!}\n";
		mountStorageScript += "qemu-nbd -c /dev/nbd0 \\${dirPath}/storage/\\${vmName}/\\${vmName}_storage.vdi\n";
		mountStorageScript += "wait \\${!}\n";
		mountStorageScript += "mount /dev/nbd0p1 /mnt\n";
		mountStorageScript += "wait \\${!}\n";
		mountStorageScript += "chroot /mnt\n";
		mountStorageScript += "wait \\${!}\n";
		mountStorageScript += "umount /mnt\n";
		mountStorageScript += "wait \\${!}\n";
		mountStorageScript += "qemu-nbd --disconnect /dev/nbd0\n";
		mountStorageScript += "wait \\${!}\n";
		mountStorageScript += "sudo -u vboxuser_\\${vmName} VBoxManage startvm \\${vmName} --type headless\n";
		mountStorageScript += "echo \\\"=== /fin/ ===\\\"";
				
		units.addElement(new FileUnit("mount_storage_script", "proceed", mountStorageScript, recoveryScriptsBase + "/mountStorage.sh"));
		units.addElement(new FileOwnUnit("mount_storage_script", "mount_storage_script", recoveryScriptsBase + "/mountStorage.sh", "root"));
		units.addElement(new FilePermsUnit("mount_storage_script", "mount_storage_script_chowned", recoveryScriptsBase + "/mountStorage.sh", "750"));

		return units;
	}
}
