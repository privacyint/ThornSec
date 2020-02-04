/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile;

import java.util.ArrayList;
import java.util.Collection;

import core.exception.data.machine.InvalidServerException;
import core.iface.IUnit;
import core.model.network.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.fs.DirOwnUnit;
import core.unit.fs.DirPermsUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.GitCloneUnit;
import core.unit.pkg.InstalledUnit;

public class HypervisorScripts extends AStructuredProfile {

	private String vmBase;

	private String scriptsBase;
	private String recoveryScriptsBase;
	private String backupScriptsBase;
	private String controlScriptsBase;
	private String watchdogScriptsBase;
	private String helperScriptsBase;

	public HypervisorScripts(String label, NetworkModel networkModel) {
		super(label, networkModel);
	}

	@Override
	protected Collection<IUnit> getInstalled() {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("metal_git", "proceed", "git"));
		units.add(new InstalledUnit("metal_duplicity", "proceed", "duplicity"));
		units.add(new InstalledUnit("metal_mutt", "proceed", "mutt"));

		return units;
	}

	@Override
	protected Collection<IUnit> getPersistentConfig() throws InvalidServerException {
		final Collection<IUnit> units = new ArrayList<>();

		this.vmBase = getNetworkModel().getData().getHypervisorThornsecBase(getLabel());

		this.scriptsBase = this.vmBase + "/scripts";
		this.recoveryScriptsBase = this.scriptsBase + "/recovery";
		this.backupScriptsBase = this.scriptsBase + "/backup";
		this.controlScriptsBase = this.scriptsBase + "/vm";
		this.watchdogScriptsBase = this.scriptsBase + "/watchdog";
		this.helperScriptsBase = this.scriptsBase + "/helper";

		units.add(new DirUnit("recovery_scripts_dir", "proceed", this.recoveryScriptsBase));
		units.add(new DirOwnUnit("recovery_scripts_dir", "recovery_scripts_dir_created", this.recoveryScriptsBase,
				"root"));
		units.add(new DirPermsUnit("recovery_scripts_dir", "recovery_scripts_dir_chowned", this.recoveryScriptsBase,
				"400"));

		units.add(new DirUnit("backup_scripts_dir", "proceed", this.backupScriptsBase));
		units.add(new DirOwnUnit("backup_scripts_dir", "backup_scripts_dir_created", this.backupScriptsBase, "root"));
		units.add(new DirPermsUnit("backup_scripts_dir", "backup_scripts_dir_chowned", this.backupScriptsBase, "400"));

		units.add(new DirUnit("control_scripts_dir", "proceed", this.controlScriptsBase));
		units.add(
				new DirOwnUnit("control_scripts_dir", "control_scripts_dir_created", this.controlScriptsBase, "root"));
		units.add(
				new DirPermsUnit("control_scripts_dir", "control_scripts_dir_chowned", this.controlScriptsBase, "400"));

		units.add(new DirUnit("watchdog_scripts_dir", "proceed", this.watchdogScriptsBase));
		units.add(new DirOwnUnit("watchdog_scripts_dir", "watchdog_scripts_dir_created", this.watchdogScriptsBase,
				"root"));
		units.add(new DirPermsUnit("watchdog_scripts_dir", "watchdog_scripts_dir_chowned", this.watchdogScriptsBase,
				"400"));

		units.add(new DirUnit("helper_scripts_dir", "proceed", this.helperScriptsBase));
		units.add(new DirOwnUnit("helper_scripts_dir", "helper_scripts_dir_created", this.helperScriptsBase, "root"));
		units.add(new DirPermsUnit("helper_scripts_dir", "helper_scripts_dir_chowned", this.helperScriptsBase, "400"));

		// This is for our internal backups
		units.add(new GitCloneUnit("backup_script", "metal_git_installed",
				"https://github.com/JohnKaul/rsync-time-backup.git", this.backupScriptsBase + "/rsync-time-backup",
				"The backup script couldn't be retrieved from github.  Backups won't work."));

		return units;
	}
}
