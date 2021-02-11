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
import core.iface.IUnit;
import core.model.machine.ServerModel;

import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.DirMountedUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileAppendUnit;

public class media extends AStructuredProfile {

	public media(ServerModel me) {
		super(me);
	}

	@Override
	public Collection<IUnit> getLiveFirewall() {
		final Collection<IUnit> units = new ArrayList<>();

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentConfig() {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new SimpleUnit("candc_user", "proceed", "sudo useradd -m candc -c 'C&C Music Factory'",
				"id candc 2>&1", "id: ‘candc’: no such user", "fail", "The candc user couldn't be added."));

		units.add(new DirUnit("tc_dir_exists", "is_virtualbox_guest", "/media/data/transcodeDir"));

		// Mount /media/data/transcodeDir
		units.add(new FileAppendUnit("tc_fstab", "is_virtualbox_guest",
				"transcodeDir    /media/data/transcodeDir      vboxsf defaults,_netdev,uid=$(id candc -u),gid=$(id candc -g) 0 0",
				"/etc/fstab", "Couldn't create the mount for transcoding at /media/data/transcodeDir"));
		units.add(new DirUnit("tc_bindpoint", "is_virtualbox_guest", "/media/data/transcodeDir"));
		units.add(new DirMountedUnit("tc", "tc_fstab_appended", "transcodeDir",
				"Couldn't mount the transcoding directory."));

		return units;
	}

}
