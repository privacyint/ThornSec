package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.model.ServerModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.FileChecksumUnit;
import core.unit.fs.FileDownloadUnit;
import core.unit.fs.FilePermsUnit;
import core.unit.fs.GitCloneUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;

public class NodeJS extends AStructuredProfile {
	
	public NodeJS(ServerModel me, NetworkModel networkModel) {
		super("nodejs", me, networkModel);
	}

	protected Vector<IUnit> getInstalled() {
		Vector<IUnit> units = new Vector<IUnit>();

		units.addElement(new InstalledUnit("git", "gzip_installed", "git"));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig() {
		Vector<IUnit> units =  new Vector<IUnit>();

		units.addElement(new FileDownloadUnit("nodejs", "build_essential_installed",
				"https://deb.nodesource.com/setup_9.x",
				"/root/nodejs.sh",
				"nodejs couldn't be downloaded.  Etherpad's installation will fail."));
		
		units.addElement(new FileChecksumUnit("nodejs", "nodejs_downloaded",
				"/root/nodejs.sh",
				"98321bbfa4f4b4108fedc7153666e0a0e5423787f6b9b3285d1b2e71336e114e1ac861e46d2b0ca40790295b98d0bb479bd4e68321b6e03b43eab42b7d09dc35",
				"nodejs's checksum doesn't match.  This could indicate a failed download, MITM attack, or a newer version than our code supports.  Etherpad's installation will fail."));
		
		units.addElement(new FilePermsUnit("nodejs_is_executable", "nodejs_checksum",
				"/root/nodejs.sh",
				"755",
				"nodejs couldn't be set to be executable.  Etherpad's installation will fail."));
		
		units.addElement(new SimpleUnit("nodejs_setup_environment", "nodejs_is_executable_chmoded",
				"sudo -E /root/nodejs.sh",
				"sudo test -f /etc/apt/sources.list.d/nodesource.list && echo 'pass' || echo 'fail'", "pass", "pass",
				"nodejs's setup environment couldn't be configured.  Etherpad's installation will fail."));

		units.addElement(new InstalledUnit("nodejs", "nodejs_setup_environment", "nodejs"));
		
		return units;
	}

	public Vector<IUnit> getNetworking() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		//Let's open this box up to most of the internet.  Thanks, node!
		me.addRequiredEgress("github.com");
		me.addRequiredEgress("deb.nodesource.com");
		me.addRequiredEgress("npmjs.org");
		me.addRequiredEgress("registry.npmjs.org");

		return units;
	}

}
