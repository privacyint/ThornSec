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
		backupScript += "#!/bin/sh\n";
		backupScript += "echo \\\"=== Starting internal backup at \\`date\\` ===\\\"\n";
		backupScript += "for dirPath in \\`pwd\\`/*/\n";
		backupScript += "do\n";
		backupScript += "    dirPath=\\${dirPath%*/}\n";
		backupScript += "    vmName=\\${dirPath##*/}\n";
		backupScript += "    if [ \"\\${vmName}\" != \"rsync-time-backup\" ]\n"; //Don't descend in here, it's not a VM!
		backupScript += "    then\n";
		backupScript += "        echo \\\"Backing up \\${vmName}\\\"\n";
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
		backupScript += "    if [ ! \"\\$(ls -A \\${dirPath}/latest)\" ]; then\n";
		backupScript += "        echo -e \\\"";
		backupScript += "            subject:[\\${vmName}." + model.getData().getLabel() + "] WARNING: BACKUP EMPTY\\n";
		backupScript += "            from:\\${vmName}@" + model.getData().getDomain(server) + "\\n";
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
		backupRecoveryScript += "#!/bin/sh\n";
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
		
		return units;
	}
}