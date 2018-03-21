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

public class Backups extends AStructuredProfile {
	
	public Backups() {
		super("backups");
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new InstalledUnit("metal_git", "git"));
		units.addElement(new InstalledUnit("metal_duplicity", "duplicity"));

		units.addElement(new SimpleUnit("metal_qemu_nbd_enabled", "metal_qemu_utils_installed",
				"sudo modprobe nbd",
				"sudo lsmod | grep nbd", "", "fail",
				"The nbd kernel module couldn't be loaded.  Backups won't work."));
		
		model.getServerModel(server).getUserModel().addUsername("nbd");
		
		//This is for our internal backups
		units.addElement(new GitCloneUnit("backup_script", "metal_git_installed", "https://github.com/JohnKaul/rsync-time-backup.git", model.getData().getVmBase(server) + "/backup/rsync-time-backup",
				"The backup script couldn't be retrieved from github.  Backups won't work."));

		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();

		String backupScript = "";
		backupScript += "#!/bin/bash\n";
		backupScript += "echo \\\"=== Starting internal backup at \\`date\\` ===\\\"\n";
		backupScript += "modprobe -r nbd\n";
		backupScript += "modprobe nbd max_part=15\n";
		backupScript += "for dirPath in \\`pwd\\`/*/\n";
		backupScript += "do\n";
		backupScript += "    dirPath=\\${dirPath%*/}\n";
		backupScript += "    vmName=\\${dirPath##*/}\n";
		backupScript += "    if [ \"\\${vmName}\" != \"rsync-time-backup\" ]\n"; //Don't descend in here, it's not a VM!
		backupScript += "    then\n";
		backupScript += "        echo \\\"Backing up \\${vmName}\\\"\n";
		backupScript += "        sudo -u vboxuser_\\${vmName} bash -c \\\"VBoxManage controlvm \\${vmName} acpipowerbutton\\\"\n";
		backupScript += "        wait \\${!}\n";
		backupScript += "        sleep 30s\n";
		backupScript += "        sudo -u vboxuser_\\${vmName} bash -c \\\"VBoxManage controlvm \\${vmName} poweroff\\\"\n";
		backupScript += "        wait \\${!}\n";
        backupScript += "        qemu-nbd -c /dev/nbd0 \\${dirPath}/../../data/\\${vmName}/\\${vmName}_data.vdi\n";
		backupScript += "        wait \\${!}\n";
        backupScript += "        mount /dev/nbd0p1 /mnt\n";
		backupScript += "        wait \\${!}\n";
        backupScript += "        rsync-time-backup/rsync_tmbackup.sh -s /mnt/ -d \\${dirPath}\n";
		backupScript += "        wait \\${!}\n";
        backupScript += "        umount /mnt\n";
		backupScript += "        wait \\${!}\n";
        backupScript += "        qemu-nbd --disconnect /dev/nbd0\n";
		backupScript += "        wait \\${!}\n";
        backupScript += "        sudo -u vboxuser_\\${vmName} bash -c \\\"VBoxManage startvm \\${vmName} --type headless\\\"\n";
		backupScript += "    fi\n";
		//backupScript += "    if [ ! \"\\$(sudo ls -A \\${dirPath}/latest)\" ]; then\n";
		//backupScript += "        echo -e \\\"";
		//backupScript += "            subject:[\\${vmName}." + model.getData().getLabel() + "] WARNING: BACKUP EMPTY\\n";
		//backupScript += "            from:\\${vmName}@" + model.getData().getDomain(server) + "\\n";
		//backupScript += "            recipients:" + model.getData().getAdminEmail() + "\\n";
		//backupScript += "            \\n";
		//backupScript += "            Today's backup is empty.  This is almost certainly unexpected.";
		//backupScript += "        \\\"";
		//backupScript += "        |sendmail \"" + model.getData().getAdminEmail() + "\"\n";		
		//backupScript += "    fi\n";
		backupScript += "done\n";
		backupScript += "echo \\\"=== Finished internal backup at \\`date\\` ===\\\"\n";
		backupScript += "\n";
		backupScript += "if [ -f external_backup.sh ]; then\n";
		backupScript += "    ./external_backup.sh\n";
		backupScript += "fi";

		units.addElement(new FileUnit("metal_backup_script", "proceed", backupScript, model.getData().getVmBase(server) + "/backup/backup.sh"));
		units.addElement(new FileOwnUnit("metal_backup_script", "metal_backup_script", model.getData().getVmBase(server) + "/backup/backup.sh", "root"));
		units.addElement(new FilePermsUnit("metal_backup_script", "metal_backup_script_chowned", model.getData().getVmBase(server) + "/backup/backup.sh", "750"));
		
		String backupCronJob = "";
		backupCronJob += "#!/bin/sh\n";
		backupCronJob += "cd " + model.getData().getVmBase(server) + "/backup\n";
		backupCronJob += "./backup.sh >> backup.log";

		units.addElement(new FileUnit("metal_backup_cron_job", "proceed", backupCronJob, "/etc/cron.daily/vm_backup"));
		units.addElement(new FileOwnUnit("metal_backup_cron_job", "metal_backup_cron_job", "/etc/cron.daily/vm_backup", "root"));
		units.addElement(new FilePermsUnit("metal_backup_cron_job", "metal_backup_cron_job_chowned", "/etc/cron.daily/vm_backup", "755"));
		
		//Backup recovery scripts
		String backupRecoveryScript = "";
		backupRecoveryScript += "#!/bin/bash\n";
		backupRecoveryScript += "echo \\\"=== Restoring latest internal backup at \\`date\\` ===\\\"\n";
		backupRecoveryScript += "for dirPath in \\`pwd\\`/../backup/*/\n";
		backupRecoveryScript += "do\n";
		backupRecoveryScript += "    dirPath=\\${dirPath%*/}\n";
		backupRecoveryScript += "    vmName=\\${dirPath##*/}\n";
		backupRecoveryScript += "    if [ \"\\${vmName}\" != \"rsync-time-backup\" ]\n"; //Don't descend in here, it's not a VM!
		backupRecoveryScript += "    then\n";
		backupRecoveryScript += "        echo \\\"Restoring \\${vmName} from latest backup\\\"\n";
		backupRecoveryScript += "        sudo -u vboxuser_\\${vmName} bash -c \\\"VBoxManage controlvm \\${vmName} poweroff\\\"\n";
		backupRecoveryScript += "        wait \\${!}\n";
		backupRecoveryScript += "        qemu-nbd -c /dev/nbd0 \\${dirPath}/../../data/\\${vmName}/\\${vmName}_data.vdi\n";
		backupRecoveryScript += "        wait \\${!}\n";
		backupRecoveryScript += "        mount /dev/nbd0p1 /mnt\n";
		backupRecoveryScript += "        wait \\${!}\n";
		backupRecoveryScript += "        cp -R \\${dirPath}/latest/* /mnt/\n";
		backupRecoveryScript += "        wait \\${!}\n";
		backupRecoveryScript += "        umount /mnt\n";
		backupRecoveryScript += "        wait \\${!}\n";
		backupRecoveryScript += "        qemu-nbd --disconnect /dev/nbd0\n";
		backupRecoveryScript += "        wait \\${!}\n";
		backupRecoveryScript += "        sudo -u vboxuser_\\${vmName} bash -c \\\"VBoxManage startvm \\${vmName} --type headless\\\"\n";
		backupRecoveryScript += "    fi\n";
		backupRecoveryScript += "done\n";
		backupRecoveryScript += "echo \\\"=== Finished restoring latest backup at \\`date\\` ===\\\"";
		
		units.addElement(new DirUnit("backup_recovery_dir", "proceed", model.getData().getVmBase(server) + "/recoveryscripts"));
		units.addElement(new DirOwnUnit("backup_recovery_dir", "backup_recovery_dir_created", model.getData().getVmBase(server) + "/recoveryscripts", "root"));
		units.addElement(new DirPermsUnit("backup_recovery_dir", "backup_recovery_dir_chowned", model.getData().getVmBase(server) + "/recoveryscripts", "400"));
		
		units.addElement(new FileUnit("backup_recovery_script", "proceed", backupRecoveryScript, model.getData().getVmBase(server) + "/recoveryscripts/recoverFromLatest.sh"));
		units.addElement(new FileOwnUnit("backup_recovery_script", "backup_recovery_script", model.getData().getVmBase(server) + "/recoveryscripts/recoverFromLatest.sh", "root"));
		units.addElement(new FilePermsUnit("backup_recovery_script", "backup_recovery_script_chowned", model.getData().getVmBase(server) + "/recoveryscripts/recoverFromLatest.sh", "755"));
		
		String stopAllScript = "";
		stopAllScript += "#!/bin/bash\n";
		stopAllScript += "echo \\\"=== Stopping all VMs at \\`date\\` ===\\\"\n";
		stopAllScript += "for dirPath in \\`pwd\\`/../backup/*/\n";
		stopAllScript += "do\n";
		stopAllScript += "    dirPath=\\${dirPath%*/}\n";
		stopAllScript += "    vmName=\\${dirPath##*/}\n";
		stopAllScript += "    if [ \"\\${vmName}\" != \"rsync-time-backup\" ]\n"; //Don't descend in here, it's not a VM!
		stopAllScript += "    then\n";
		stopAllScript += "        echo \\\"Stopping \\${vmName}\\\"\n";
		stopAllScript += "        sudo -u vboxuser_\\${vmName} bash -c \\\"VBoxManage controlvm \\${vmName} poweroff\\\"\n";
		stopAllScript += "        wait \\${!}\n";
		stopAllScript += "    fi\n";
		stopAllScript += "done\n";
		stopAllScript += "echo \\\"=== Finished stopping all VMs at \\`date\\` ===\\\"";
		
		units.addElement(new FileUnit("stop_all_script", "proceed", stopAllScript, model.getData().getVmBase(server) + "/recoveryscripts/stopAll.sh"));
		units.addElement(new FileOwnUnit("stop_all_script", "stop_all_script", model.getData().getVmBase(server) + "/recoveryscripts/stopAll.sh", "root"));
		units.addElement(new FilePermsUnit("stop_all_script", "stop_all_script_chowned", model.getData().getVmBase(server) + "/recoveryscripts/stopAll.sh", "755"));

		String startAllScript = "";
		startAllScript += "#!/bin/bash\n";
		startAllScript += "echo \\\"=== Starting all VMs at \\`date\\` ===\\\"\n";
		startAllScript += "for dirPath in \\`pwd\\`/../backup/*/\n";
		startAllScript += "do\n";
		startAllScript += "    dirPath=\\${dirPath%*/}\n";
		startAllScript += "    vmName=\\${dirPath##*/}\n";
		startAllScript += "    if [ \"\\${vmName}\" != \"rsync-time-backup\" ]\n"; //Don't descend in here, it's not a VM!
		startAllScript += "    then\n";
		startAllScript += "        sudo -u vboxuser_\\${vmName} bash -c \\\"VBoxManage startvm \\${vmName} --type headless\\\"\n";
		startAllScript += "        wait \\${!}\n";
		startAllScript += "    fi\n";
		startAllScript += "done\n";
		startAllScript += "echo \\\"=== Finished starting all VMs at \\`date\\` ===\\\"";
		
		units.addElement(new FileUnit("start_all_script", "proceed", startAllScript, model.getData().getVmBase(server) + "/recoveryscripts/startAll.sh"));
		units.addElement(new FileOwnUnit("start_all_script", "start_all_script", model.getData().getVmBase(server) + "/recoveryscripts/startAll.sh", "root"));
		units.addElement(new FilePermsUnit("start_all_script", "start_all_script_chowned", model.getData().getVmBase(server) + "/recoveryscripts/startAll.sh", "755"));

		String mountStorageScript = "";
		mountStorageScript += "#!/bin/bash\n";
		mountStorageScript += "if [ \\$# -eq 0 ]\n";
		mountStorageScript += "then\n";
		mountStorageScript += "	echo \\\"No parameter supplied.\\\nYou need to provide the name of the VM as a parameter\\\"\n";
		mountStorageScript += "	exit 1;\n";
		mountStorageScript += "fi\n";
		mountStorageScript += "\n";
		mountStorageScript += "vmName=\\${1}\n";
		mountStorageScript += "\n";
		mountStorageScript += "echo \\\"=== Mounting storage ===\\\"\n";
		mountStorageScript += "modprobe -r nbd\n";
		mountStorageScript += "modprobe nbd max_part=15\n";
		mountStorageScript += "\n";
		mountStorageScript += "dirPath=\\`pwd\\`\n";
		mountStorageScript += "\n";
		mountStorageScript += "echo \\\"This will mount the storage for \\${vmName} and chroot.\\\"\n";
		mountStorageScript += "echo \\\"When you are finished, type exit to restart the VM\\\"\n";
		mountStorageScript += "\n";
		mountStorageScript += "echo \\\"Shutting down \\${vmName}\\\"\n";
		mountStorageScript += "sudo -u vboxuser_\\${vmName} bash -c \\\"VBoxManage controlvm \\${vmName} acpipowerbutton\\\"\n";
		mountStorageScript += "wait \\${!}\n";
		mountStorageScript += "sleep 30s\n";
		mountStorageScript += "sudo -u vboxuser_\\${vmName} bash -c \\\"VBoxManage controlvm \\${vmName} poweroff\\\"\n";
		mountStorageScript += "wait \\${!}\n";
		mountStorageScript += "qemu-nbd -c /dev/nbd0 \\${dirPath}/../storage/\\${vmName}/\\${vmName}_storage.vdi\n";
		mountStorageScript += "wait \\${!}\n";
		mountStorageScript += "mount /dev/nbd0p1 /mnt\n";
		mountStorageScript += "wait \\${!}\n";
		mountStorageScript += "chroot /mnt\n";
		mountStorageScript += "wait \\${!}\n";
		mountStorageScript += "umount /mnt\n";
		mountStorageScript += "wait \\${!}\n";
		mountStorageScript += "qemu-nbd --disconnect /dev/nbd0\n";
		mountStorageScript += "wait \\${!}\n";
		mountStorageScript += "sudo -u vboxuser_\\${vmName} bash -c \\\"VBoxManage startvm \\${vmName} --type headless\\\"\n";
		mountStorageScript += "echo \\\"=== /fin/ ===\\\"";
				
		units.addElement(new FileUnit("mount_storage_script", "proceed", mountStorageScript, model.getData().getVmBase(server) + "/recoveryscripts/mountStorage.sh"));
		units.addElement(new FileOwnUnit("mount_storage_script", "mount_storage_script", model.getData().getVmBase(server) + "/recoveryscripts/mountStorage.sh", "root"));
		units.addElement(new FilePermsUnit("mount_storage_script", "mount_storage_script_chowned", model.getData().getVmBase(server) + "/recoveryscripts/mountStorage.sh", "755"));
		
		return units;
	}
}
