package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.model.ServerModel;
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

	private String disksDirBase;
	private String dataDiskDirBase;
	private String bootDiskDirBase;

	private String backupDirBase;
	private String isoDirBase;
	private String logDirBase;

	private String scriptsBase;
	private String recoveryScriptsBase;
	private String backupScriptsBase;
	private String controlScriptsBase;
	private String watchdogScriptsBase;
	private String helperScriptsBase;
	
	public HypervisorScripts(ServerModel me, NetworkModel networkModel) {
		super("hypervisorscripts", me, networkModel);
	}

	protected Vector<IUnit> getInstalled() {
		Vector<IUnit> units = new Vector<IUnit>();

		units.addElement(new InstalledUnit("metal_git", "git"));
		units.addElement(new InstalledUnit("metal_duplicity", "duplicity"));
		units.addElement(new InstalledUnit("metal_mutt", "mutt"));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig() {
		Vector<IUnit> units = new Vector<IUnit>();

		this.vmBase = super.networkModel.getData().getHypervisorThornsecBase(me.getLabel());
		
		this.disksDirBase    = this.vmBase + "/disks";
		this.dataDiskDirBase = this.disksDirBase + "/data";
		this.bootDiskDirBase = this.disksDirBase + "/boot";

		this.backupDirBase = this.vmBase + "/backups";
		this.isoDirBase    = this.vmBase + "/isos";
		this.logDirBase    = this.vmBase + "/logs";

		this.scriptsBase         = this.vmBase + "/scripts";
		this.recoveryScriptsBase = this.scriptsBase + "/recovery";
		this.backupScriptsBase   = this.scriptsBase + "/backup";
		this.controlScriptsBase  = this.scriptsBase + "/vm";
		this.watchdogScriptsBase = this.scriptsBase + "/watchdog";
		this.helperScriptsBase   = this.scriptsBase + "/helper";
		
		units.addElement(new DirUnit("recovery_scripts_dir", "proceed", this.recoveryScriptsBase));
		units.addElement(new DirOwnUnit("recovery_scripts_dir", "recovery_scripts_dir_created", this.recoveryScriptsBase, "root"));
		units.addElement(new DirPermsUnit("recovery_scripts_dir", "recovery_scripts_dir_chowned", this.recoveryScriptsBase, "400"));
		
		units.addElement(new DirUnit("backup_scripts_dir", "proceed", this.backupScriptsBase));
		units.addElement(new DirOwnUnit("backup_scripts_dir", "backup_scripts_dir_created", this.backupScriptsBase, "root"));
		units.addElement(new DirPermsUnit("backup_scripts_dir", "backup_scripts_dir_chowned", this.backupScriptsBase, "400"));

		units.addElement(new DirUnit("control_scripts_dir", "proceed", this.controlScriptsBase));
		units.addElement(new DirOwnUnit("control_scripts_dir", "control_scripts_dir_created", this.controlScriptsBase, "root"));
		units.addElement(new DirPermsUnit("control_scripts_dir", "control_scripts_dir_chowned", this.controlScriptsBase, "400"));

		units.addElement(new DirUnit("watchdog_scripts_dir", "proceed", this.watchdogScriptsBase));
		units.addElement(new DirOwnUnit("watchdog_scripts_dir", "watchdog_scripts_dir_created", this.watchdogScriptsBase, "root"));
		units.addElement(new DirPermsUnit("watchdog_scripts_dir", "watchdog_scripts_dir_chowned", this.watchdogScriptsBase, "400"));

		units.addElement(new DirUnit("helper_scripts_dir", "proceed", this.helperScriptsBase));
		units.addElement(new DirOwnUnit("helper_scripts_dir", "helper_scripts_dir_created", this.helperScriptsBase, "root"));
		units.addElement(new DirPermsUnit("helper_scripts_dir", "helper_scripts_dir_chowned", this.helperScriptsBase, "400"));

		//This is for our internal backups
		units.addElement(new GitCloneUnit("backup_script", "metal_git_installed",
				"https://github.com/JohnKaul/rsync-time-backup.git",
				this.backupScriptsBase + "/rsync-time-backup",
				"The backup script couldn't be retrieved from github.  Backups won't work."));
		
		units.addAll(helperScripts());
		units.addAll(recoveryScripts());
		units.addAll(backupScripts());
		units.addAll(vmControlScripts());
		units.addAll(watchdogScripts());
		
		units.addAll(adminScripts());
		
		return units;
	}

	private Vector<IUnit> helperScripts() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String mountDataScript = "";
		mountDataScript += "#!/bin/bash\n";
		mountDataScript += "if [ \\$# -eq 0 ]; then\n";
		mountDataScript += "    echo \\\"No parameter supplied. You need to provide the name of the VM as a parameter\\\"\n";
		mountDataScript += "    exit 1;\n";
		mountDataScript += "fi\n";
		mountDataScript += "\n";
		mountDataScript += "vm=\\${1}\n";
		mountDataScript += "src=\\$(sudo -u vboxuser_\\\"\\${vm}\\\" VBoxManage showvminfo \\\"\\${vm}\\\" --machinereadable | grep \\\"SAS-1-0\\\" | awk -F\\\" '{print \\$4}')\n";
		mountDataScript += "dst=/srv/VMs/disks/data/\\${vm}/live/\n";
		mountDataScript += "\n";
		mountDataScript += "echo \\\"Mounting \\${vm}'s data disk (\\${src})\\\"\n";
		mountDataScript += "\n";
		mountDataScript += "export LIBGUESTFS_BACKEND_SETTINGS=force_tcg;\n";
		mountDataScript += "\n";
		mountDataScript += "formatted=\\$(virt-filesystems -a \\${src})\n";
		mountDataScript += "\n";
		mountDataScript += "if [ \\\"\\${formatted}\\\" = \\\"\\\" ]; then\n";
		mountDataScript += "    echo \\\"Disk is not formatted. I shall not attempt to mount it.\\\"\n";
		mountDataScript += "    exit 1\n";
		mountDataScript += "else\n";
		mountDataScript += "    guestmount -a \\${src} -m /dev/sda1 -o direct_io --ro \\${dst}\n";
		mountDataScript += "    exit \\$?\n";
		mountDataScript += "fi";
		
		units.addElement(new FileUnit("mount_data_script", "proceed", mountDataScript, this.helperScriptsBase + "/mountData.sh"));
		units.addElement(new FileOwnUnit("mount_data_script", "mount_data_script", this.helperScriptsBase + "/mountData.sh", "root"));
		units.addElement(new FilePermsUnit("mount_data_script", "mount_data_script_chowned", this.helperScriptsBase + "/mountData.sh", "750"));
		
		String umountDataScript = "";
		umountDataScript += "#!/bin/bash\n";
		umountDataScript += "if [ \\$# -eq 0 ]; then\n";
		umountDataScript += "    echo \\\"No parameter supplied. You need to provide the name of the VM as a parameter\\\"\n";
		umountDataScript += "    exit 1;\n";
		umountDataScript += "fi\n";
		umountDataScript += "\n";
		umountDataScript += "vm=\\${1}\n";
		umountDataScript += "echo \\\"Unmounting \\${vm}'s data disk\\\"\n"; 
		umountDataScript += "\n"; 
		umountDataScript += "umount \\\"" + this.dataDiskDirBase + "/\\${vm}/live/\\\"\n";
		umountDataScript += "exit \\$?";
		
		units.addElement(new FileUnit("umount_data_script", "proceed", umountDataScript, this.helperScriptsBase + "/umountData.sh"));
		units.addElement(new FileOwnUnit("umount_data_script", "umount_data_script", this.helperScriptsBase + "/umountData.sh", "root"));
		units.addElement(new FilePermsUnit("umount_data_script", "umount_data_script_chowned", this.helperScriptsBase + "/umountData.sh", "750"));
		
		return units;
	}
	private Vector<IUnit> watchdogScripts() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String isUpScript = "";
		isUpScript += "#!/bin/bash\n";
		isUpScript += "statusPath=" + watchdogScriptsBase + "/.status\n";
		isUpScript += "logPath=" + logDirBase + "/*/\n";
		isUpScript += "emailTo=" + networkModel.getData().getAdminEmail() + "\n";
		isUpScript += "emailFrom=" + me.getLabel() + "." + networkModel.getLabel() + "@" + networkModel.getData().getDomain(me.getLabel()) + "\n";
		isUpScript += "emailFromRealName=\\\"ThornSec Server Daemon™ on " + me.getLabel() + "\\\"\n";
		isUpScript += "\n";
		isUpScript += "blacklist=(\\\"lost+found\\\")\n";
		isUpScript += "\n";
		isUpScript += "mkdir -p \\${statusPath}\n";
		isUpScript += "\n";
		isUpScript += "for dirPath in \\${logPath}\n";
		isUpScript += "do\n";
		isUpScript += "        dirPath=\\${dirPath%*/}\n";
		isUpScript += "        vm=\\${dirPath##*/}\n";
		isUpScript += "        echo 0 > \\${statusPath}/\\${vm}\n";
		isUpScript += "done\n";
		isUpScript += "\n";
		isUpScript += "sleep 120\n";
		isUpScript += "\n";
		isUpScript += "while true\n";
		isUpScript += "do\n";
		isUpScript += "        for dirPath in \\${logPath}\n";
		isUpScript += "        do\n";
		isUpScript += "                dirPath=\\${dirPath%*/}\n";
		isUpScript += "                vm=\\${dirPath##*/}\n";
		isUpScript += "\n";
		isUpScript += "                counterPath=\\${statusPath}/\\${vm}\n";
		isUpScript += "\n";
		isUpScript += "                if [[ -z \\$(printf '%s\\n' \\\"\\${blacklist[@]}\\\" | grep -w \\${vm}) ]]\n";
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
		isUpScript += "                                        echo \\\"Forcibly restarting \\${vm}\\\";\n";
		isUpScript += "                                        " + controlScriptsBase + "/stopVm.sh \\\"\\${vm}\\\"\n";
		isUpScript += "                                        wait \\${!};\n";
		isUpScript += "                                        " + controlScriptsBase + "/startVm.sh \\\"\\${vm}\\\"\n";
		isUpScript += "                                        wait \\${!}\n";
		isUpScript += "                                        ) | mutt -e \\\"set realname='\\${emailFromRealName}️' from=\\${emailFrom}\\\" -s \\\"Restarted \\${vm} on " + me.getLabel() + "\\\" -n \\${emailTo}\n";
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
		service += "Restart=no\n";
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
	
	private Vector<IUnit> backupScripts() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String backupScript = "";
		backupScript += "#!/bin/bash\n";
		backupScript += "echo \\\"=== Starting internal backup at \\$(date) ===\\\"\n";
		backupScript += "emailTo=" + networkModel.getData().getAdminEmail() + "\n";
		backupScript += "emailFrom=" + me.getEmailAddress() + "\n";
		backupScript += "emailFromRealName=\\\"ThornSec Backup Daemon™ on " + me.getLabel() + "\\\"\n";
		backupScript += "backupBase=" + backupDirBase + "\n";
		backupScript += "\n";
		backupScript += "warnings=\\\"\\\"\n";
		backupScript += "\n";
		backupScript += "for dirPath in " + dataDiskDirBase + "/*/\n";
		backupScript += "do\n";
		backupScript += "    dirPath=\\\"\\${dirPath%*/}\\\"\n";
		backupScript += "    vm=\\\"\\${dirPath##*/}\\\"\n";
		backupScript += "\n";
		backupScript += "    echo \\\"Backing up \\${vm}\\\"\n";
		backupScript += "    " + this.helperScriptsBase + "/umountData.sh \\\"\\${vm}\\\" 2>/dev/null\n";
        backupScript += "    " + this.helperScriptsBase + "/mountData.sh \\\"\\${vm}\\\" && " + backupScriptsBase + "/rsync-time-backup/rsync_tmbackup.sh -s " + dataDiskDirBase + "/\\\"\\${vm}\\\"/live/ -d \\\"\\${backupBase}/\\${vm}\\\"\n";
		backupScript += "    " + this.helperScriptsBase + "/umountData.sh \\\"\\${vm}\\\" 2>/dev/null\n";
		backupScript += "\n";
		backupScript += "    if [ ! \\\"\\$(ls -A \\${backupBase}/\\${vm}/latest)\\\" ]; then\n";
		backupScript += "         warnings+=\\\"WARNING: \\${vm}'s latest backup is empty.\\n\\\"\n";
		backupScript += "    fi\n";
		backupScript += "done\n";
		backupScript += "echo \\\"=== Finished internal backup at \\$(date) ===\\\"\n";
		backupScript += "\n";
		backupScript += "if [ -f external_backup.sh ]; then\n";
		backupScript += "    ./external_backup.sh\n";
		backupScript += "fi";
		backupScript += "\n";
		backupScript += "(echo -e \\\"\\n\\${warnings}\\nBackup précis:\\n\\\"; egrep -w '===|Backing|sent|total' " + backupScriptsBase + "/backup.latest) |\n"; 
		backupScript += "mutt \\${emailTo} -e \\\"set realname='\\${emailFromRealName}️' from=\\${emailFrom}\\\" -s \\\"[INFO] [" + me.getLabel() + "] Backup Complete\\\" -a \\\"" + backupScriptsBase + "/backup.latest\\\"\n";
		backupScript += "\n";
		backupScript += "if [ ! -z \\\"\\${warnings} \\\" ]; then\n";
		backupScript += "    (echo -e \\\"\\n\\${warnings}\\\") |\n"; 
		backupScript += "    mutt ";
		for (String admin : getNetworkModel().getData().getAdmins(me.getLabel())) {
			backupScript += getNetworkModel().getDeviceModel(admin).getEmailAddress() + " ";
		}
		backupScript += "-e \\\"set realname='\\${emailFromRealName}️' from=\\${emailFrom}\\\" -s \\\"[WARN] [" + me.getLabel() + "] Backup Failed!\\\" -a \\\"" + backupScriptsBase + "/backup.latest\\\"\n";
		backupScript += "fi";

		units.addElement(new FileUnit("metal_backup_script", "proceed", backupScript, backupScriptsBase + "/backup.sh"));
		units.addElement(new FileOwnUnit("metal_backup_script", "metal_backup_script", backupScriptsBase + "/backup.sh", "root"));
		units.addElement(new FilePermsUnit("metal_backup_script", "metal_backup_script_chowned", backupScriptsBase + "/backup.sh", "750"));
		
		String backupCronJob = "";
		backupCronJob += "#!/bin/sh\n";
		backupCronJob += "cd " + backupScriptsBase + "\n";
		backupCronJob += "./backup.sh | tee ./backup.latest >> ./backup.log";

		units.addElement(new FileUnit("metal_backup_cron_job", "proceed", backupCronJob, "/etc/cron.daily/vm_backup"));
		units.addElement(new FileOwnUnit("metal_backup_cron_job", "metal_backup_cron_job", "/etc/cron.daily/vm_backup", "root"));
		units.addElement(new FilePermsUnit("metal_backup_cron_job", "metal_backup_cron_job_chowned", "/etc/cron.daily/vm_backup", "750"));
		
		return units;
	}
	
	private Vector<IUnit> vmControlScripts() {
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
		stopScript += "    sudo -u vboxuser_\\\"\\${vm}\\\" VBoxManage controlvm \\\"\\${vm}\\\" acpipowerbutton\n";
		stopScript += "    sleep 30\n";
		stopScript += "    sudo -u vboxuser_\\\"\\${vm}\\\" VBoxManage controlvm \\\"\\${vm}\\\" poweroff\n";
		stopScript += "    wait \\${!}\n";
		stopScript += "}\n";
		stopScript += "\n";
		stopScript += "function stopAll {\n";
		stopScript += "    echo \\\"=== Stopping all VMs at \\$(date) ===\\\"\n";
		stopScript += "    for dirPath in " + dataDiskDirBase + "/*/\n";
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
		startScript += "    for dirPath in " + dataDiskDirBase + "/*/\n";
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
		deleteVmScript += "vm=\\${1}\n";
		deleteVmScript += "\n";
		deleteVmScript += controlScriptsBase + "/stopVm.sh \\\"\\${vm}\\\"\n";
		deleteVmScript += "wait\n";
		deleteVmScript += "\n";
		deleteVmScript += "echo \\\"Unregistering the \\${vm} VM\\\"\n";
		deleteVmScript += "sudo -u vboxuser_\\\"\\${vm}\\\" VBoxManage unregistervm \\\"\\${vm}\\\"\n";
		deleteVmScript += "wait\n";
		deleteVmScript += "\n";
		deleteVmScript += "echo \\\"Deleting \\${vm}'s files\\\"\n";
		deleteVmScript += "rm -R \\\"" + bootDiskDirBase + "/\\${vm}\\\" 2>/dev/null\n";
		deleteVmScript += "rm -R \\\"" + isoDirBase + "/\\${vm}\\\" 2>/dev/null\n";
		deleteVmScript += "rm -R \\\"" + logDirBase + "/\\${vm}\\\" 2>/dev/null\n";
		deleteVmScript += "rm -R \\\"/home/vboxuser_\\${vm}/VirtualBox VMs\\\" 2>/dev/null\n";
		deleteVmScript += "\n";
		deleteVmScript += "delete=\\\"unknown\\\"\n";
		deleteVmScript += "\n";
		deleteVmScript += "while [ \\\"\\${delete}\\\" = \\\"unknown\\\" ]; do\n";
		deleteVmScript += "    read -r -p \\\"Would you like to delete the data disk? Please type \\\\\"YES\\\\\" (in all caps) to delete, or anything else to leave the data disk in place:\\\" yn\n";
		deleteVmScript += "\n";
		deleteVmScript += "    case \\\"\\${yn}\\\" in\n";
		deleteVmScript += "        YES ) delete=\\\"true\\\";;\n";
		deleteVmScript += "        yes ) delete=\\\"unknown\\\";;\n";
		deleteVmScript += "          * ) delete=\\\"false\\\";;\n";
		deleteVmScript += "    esac\n";
		deleteVmScript += "done\n";
		deleteVmScript += "if [ \\${delete} = \\\"true\\\" ]; then\n";
		deleteVmScript += "    rm -R \\\"" + dataDiskDirBase + "/\\${vm}\\\" 2>/dev/null\n";
		deleteVmScript += "fi\n";
		deleteVmScript += "#echo \\\"Deleting \\${vm}'s user\\\"\n";
		deleteVmScript += "#userdel -r -f vboxuser_\\\"\\${vm}\\\" 2>/dev/null\n";
		deleteVmScript += "echo \\\"=== /fin/ ===\\\"";

		units.addElement(new FileUnit("delete_vm_script", "proceed", deleteVmScript, controlScriptsBase + "/deleteVm.sh"));
		units.addElement(new FileOwnUnit("delete_vm_script", "delete_vm_script", controlScriptsBase + "/deleteVm.sh", "root"));
		units.addElement(new FilePermsUnit("delete_vm_script", "delete_vm_script_chowned", controlScriptsBase + "/deleteVm.sh", "750"));
		
		return units;
	}
	
	private Vector<IUnit> recoveryScripts() {
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
		backupRecoveryScript += "vm=\\\"\\${1}\\\"\n";
		backupRecoveryScript += "\n";
		backupRecoveryScript += "echo \\\"=== Restoring latest internal backup of ${vm} at \\$(date) ===\\\"\n";
		backupRecoveryScript += controlScriptsBase + "/stopVm.sh \\\"\\${vm}\\\"\n";
		backupRecoveryScript += "rm -rf /mnt/*\n";
		backupRecoveryScript += "cp -R \\\"" + backupDirBase + "/\\${vm}/latest/*\\\" /mnt/\n";
		backupRecoveryScript += this.helperScriptsBase + "/unmountVdi.sh\n";
		backupRecoveryScript += controlScriptsBase + "/startVm.sh \\\"\\${vm}\\\"\n";
		backupRecoveryScript += "echo \\\"=== Finished restoring latest internal backup of \\${vm} at \\$(date) ===\\\"";
		
		units.addElement(new FileUnit("backup_recovery_script", "proceed", backupRecoveryScript, recoveryScriptsBase + "/recoverFromLatest.sh"));
		units.addElement(new FileOwnUnit("backup_recovery_script", "backup_recovery_script", recoveryScriptsBase + "/recoverFromLatest.sh", "root"));
		units.addElement(new FilePermsUnit("backup_recovery_script", "backup_recovery_script_chowned", recoveryScriptsBase + "/recoverFromLatest.sh", "750"));

		String prevToVbox = "";
		prevToVbox += "#!/bin/bash\n";
		prevToVbox += "echo \\\"=== Restoring all vbox-prev files ===\\\"\n";
		prevToVbox += "for dirPath in " + dataDiskDirBase + "/*/\n";
		prevToVbox += "do\n";
		prevToVbox += "    dirPath=\\${dirPath%*/}\n";
		prevToVbox += "    vm=${dirPath##*/}\n";
		prevToVbox += "    echo \\\"Fixing ${vm}\\\"\n";
		prevToVbox += "        mv \\\"/home/vboxuser_${vm}/VirtualBox VMs/${vm}/${vm}.vbox-prev\\\" \\\"/home/vboxuser_${vm}/VirtualBox VMs/${vm}/${vm}.vbox\\\"\n"; 
		prevToVbox += "done\n";
		prevToVbox += "echo \\\"=== Finished ===\\\"";

		units.addElement(new FileUnit("prev_to_vbox_script", "proceed", prevToVbox, recoveryScriptsBase + "/prevToVbox.sh"));
		units.addElement(new FileOwnUnit("prev_to_vbox_script", "prev_to_vbox_script", recoveryScriptsBase + "/prevToVbox.sh", "root"));
		units.addElement(new FilePermsUnit("prev_to_vbox_script", "prev_to_vbox_script_chowned", recoveryScriptsBase + "/prevToVbox.sh", "750"));

		return units;
	}
	
	private Vector<IUnit> adminScripts() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String adminScript = "";
		adminScript += "#!/bin/bash\n";
		adminScript += "\n";
		adminScript += "VMS_BASE=" + this.bootDiskDirBase + "\n";
		adminScript += "SCRIPTS_BASE=" + this.scriptsBase + "\n";
		adminScript += "HELPER_SCRIPTS=" + this.helperScriptsBase + "\n";
		adminScript += "CONTROL_SCRIPTS=" + this.controlScriptsBase + "\n";
		adminScript += "RECOVERY_SCRIPTS=" + this.recoveryScriptsBase + "\n";
		adminScript += "BACKUP_SCRIPTS=" + this.backupScriptsBase + "\n";
		adminScript += "\n";
		adminScript += "vms=\\$(find \\${VMS_BASE}/* -maxdepth 0 -type d ! -name '.*' -printf '%f ')\n";
		adminScript += "PS3=\\\"Number: \\\"\n";
		adminScript += "\n";
		adminScript += "function stopVM {\n";
		adminScript += "    clear\n";
		adminScript += "\n";
		adminScript += "    echo \\\"Choose a VM to stop:\\\"\n";
		adminScript += "    echo \\\"\\\"\n";
		adminScript += "\n";
		adminScript += "    select vm in \\${vms} \\\"all\\\" \\\"Back to main menu\\\" \\\"Quit to command line\\\";\n";
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
		adminScript += "    echo \n";
		adminScript += "\n";
		adminScript += "    select vm in \\${vms} \\\"all\\\" \\\"Back to main menu\\\" \\\"Quit to command line\\\";\n";
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
		adminScript += "    select vm in \\${vms} \\\"Back to main menu\\\" \\\"Quit to command line\\\";\n";
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
		adminScript += "    echo \\\"*  THIS WILL TRIGGER YOUR BACKUPS  *\\\"\n";
		adminScript += "    echo \\\"*     THIS MAY TAKE SOME TIME!     *\\\"\n";
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
		adminScript += "    select vm in \\${vms} \\\"Back to main menu\\\" \\\"Quit to command line\\\";\n";
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
		
		units.addElement(new FileUnit("hypervisor_admin_script", "proceed", adminScript, "/root/hvAdmin"));
		units.addElement(new FileOwnUnit("hypervisor_admin_script", "hypervisor_admin_script", "/root/hvAdmin", "root"));
		units.addElement(new FilePermsUnit("hypervisor_admin_script", "hypervisor_admin_script_chowned", "/root/hvAdmin", "750"));
		
		return units;
	}
}
