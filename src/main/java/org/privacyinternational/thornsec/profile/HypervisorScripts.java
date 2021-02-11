/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.profile;

import java.util.ArrayList;
import java.util.Collection;

import org.privacyinternational.thornsec.core.exception.data.machine.InvalidServerException;
import org.privacyinternational.thornsec.core.iface.IUnit;
import org.privacyinternational.thornsec.core.model.machine.ServerModel;
import org.privacyinternational.thornsec.core.model.network.NetworkModel;
import org.privacyinternational.thornsec.core.profile.AStructuredProfile;
import org.privacyinternational.thornsec.core.unit.fs.DirUnit;
import org.privacyinternational.thornsec.core.unit.fs.GitCloneUnit;
import org.privacyinternational.thornsec.core.unit.pkg.InstalledUnit;

public class HypervisorScripts extends AStructuredProfile {

	private String vmBase;

	private String scriptsBase;
	private String recoveryScriptsBase;
	private String backupScriptsBase;
	private String controlScriptsBase;
	private String watchdogScriptsBase;
	private String helperScriptsBase;

	public HypervisorScripts(ServerModel me) {
		super(me);
	}

	@Override
	public Collection<IUnit> getInstalled() {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("metal_git", "proceed", "git"));
		units.add(new InstalledUnit("metal_duplicity", "proceed", "duplicity"));
		units.add(new InstalledUnit("metal_mutt", "proceed", "mutt"));

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentConfig() throws InvalidServerException {
		final Collection<IUnit> units = new ArrayList<>();

		//TODO
		this.vmBase = null;//getServerModel().getHypervisorThornsecBase();

		this.scriptsBase = this.vmBase + "/scripts";
		this.recoveryScriptsBase = this.scriptsBase + "/recovery";
		this.backupScriptsBase = this.scriptsBase + "/backup";
		this.controlScriptsBase = this.scriptsBase + "/vm";
		this.watchdogScriptsBase = this.scriptsBase + "/watchdog";
		this.helperScriptsBase = this.scriptsBase + "/helper";

		units.add(new DirUnit("recovery_scripts_dir", "proceed", this.recoveryScriptsBase));
		units.add(new DirUnit("backup_scripts_dir", "proceed", this.backupScriptsBase));
		units.add(new DirUnit("control_scripts_dir", "proceed", this.controlScriptsBase));
		units.add(new DirUnit("watchdog_scripts_dir", "proceed", this.watchdogScriptsBase));
		units.add(new DirUnit("helper_scripts_dir", "proceed", this.helperScriptsBase));

		// This is for our internal backups
		units.add(new GitCloneUnit("backup_script", "metal_git_installed",
				"https://github.com/JohnKaul/rsync-time-backup.git", this.backupScriptsBase + "/rsync-time-backup",
				"The backup script couldn't be retrieved from github.  Backups won't work."));

		return units;
	}
}
