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

import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.network.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.FileChecksumUnit;
import core.unit.fs.FileChecksumUnit.Checksum;
import core.unit.fs.FileDownloadUnit;
import core.unit.fs.FilePermsUnit;
import core.unit.pkg.InstalledUnit;

/**
 * This profile installs and configures NodeJS
 */
public class NodeJS extends AStructuredProfile {

	public NodeJS(String label, NetworkModel networkModel) {
		super(label, networkModel);
	}

	@Override
	public Collection<IUnit> getInstalled() {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("build_essential", "proceed", "build-essential"));
		units.add(new InstalledUnit("gzip", "proceed", "gzip"));
		units.add(new InstalledUnit("git", "gzip_installed", "git"));

		units.add(new FileDownloadUnit("nodejs", "build_essential_installed", "https://deb.nodesource.com/setup_9.x",
				"/root/nodejs.sh", "nodejs couldn't be downloaded.  Etherpad's installation will fail."));

		units.add(new FileChecksumUnit("nodejs", "nodejs_downloaded", Checksum.SHA512, "/root/nodejs.sh",
				"98321bbfa4f4b4108fedc7153666e0a0e5423787f6b9b3285d1b2e71336e114e1ac861e46d2b0ca40790295b98d0bb479bd4e68321b6e03b43eab42b7d09dc35",
				"nodejs's checksum doesn't match.  This could indicate a failed download, MITM attack, or a newer version than our code supports.  Etherpad's installation will fail."));

		units.add(new FilePermsUnit("nodejs_is_executable", "nodejs_checksum", "/root/nodejs.sh", "755",
				"nodejs couldn't be set to be executable.  Etherpad's installation will fail."));

		units.add(new SimpleUnit("nodejs_setup_environment", "nodejs_is_executable_chmoded", "sudo -E /root/nodejs.sh",
				"sudo test -f /etc/apt/sources.list.d/nodesource.list && echo 'pass' || echo 'fail'", "pass", "pass",
				"nodejs's setup environment couldn't be configured.  Etherpad's installation will fail."));

		units.add(new InstalledUnit("nodejs", "nodejs_setup_environment", "nodejs"));

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentConfig() {
		final Collection<IUnit> units = new ArrayList<>();

		// TODO: iunno. Is there something which needs to go here?

		return units;
	}

	@Override
	public Collection<IUnit> getLiveConfig() {
		final Collection<IUnit> units = new ArrayList<>();

		// TODO: check it's up to date, etc

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		// Let's open this box up to most of the internet. Thanks, node!
		getNetworkModel().getServerModel(getLabel()).addEgress("www.github.com");
		getNetworkModel().getServerModel(getLabel()).addEgress("deb.nodesource.com");
		getNetworkModel().getServerModel(getLabel()).addEgress("www.npmjs.org");
		getNetworkModel().getServerModel(getLabel()).addEgress("registry.npmjs.org");

		return units;
	}
}
