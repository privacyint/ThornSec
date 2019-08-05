/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.machine.configuration;

import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Set;

import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.network.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import inet.ipaddr.HostName;

public class AptSources extends AStructuredProfile {

	private final HostName debianRepo;
	private final String debianDir;

	private final Hashtable<String, Set<String>> sources;
	private final Hashtable<String, Set<String>> pgpKeys;

	public AptSources(String label, NetworkModel networkModel) throws URISyntaxException, InvalidServerModelException {
		super(label, networkModel);

		this.debianRepo = getNetworkModel().getData().getDebianMirror(getLabel());
		this.debianDir = getNetworkModel().getData().getDebianDirectory(getLabel());

		this.sources = new Hashtable<>();
		this.pgpKeys = new Hashtable<>();
	}

	@Override
	public final Set<IUnit> getInstalled() throws InvalidServerModelException {
		final Set<IUnit> units = new HashSet<>();

		units.add(new InstalledUnit("dirmngr", "proceed", "dirmngr",
				"Couldn't install dirmngr.  Anything which requires a PGP key to be downloaded and installed won't work. "
						+ "You can possibly fix this by running a configuration again."));

		getNetworkModel().getServerModel(getLabel())
				.addProcessString("dirmngr --daemon --homedir /tmp/apt-key-gpghome.[a-zA-Z0-9]*$");

		return units;
	}

	@Override
	public final Set<IUnit> getPersistentFirewall() throws InvalidServerModelException {
		getNetworkModel().getServerModel(getLabel()).addEgress(this.debianRepo);
		getNetworkModel().getServerModel(getLabel()).addEgress(new HostName("security.debian.org"));

		return new LinkedHashSet<>();
	}

	@Override
	public final Set<IUnit> getPersistentConfig() {
		final Set<IUnit> units = new HashSet<>();

		// Give apt 3 seconds before timing out
		final FileUnit aptTimeout = new FileUnit("decrease_apt_timeout", "proceed", "/etc/apt/apt.conf.d/99timeout",
				"Couldn't decrease the apt timeout. If your network connection is poor, the machine may appear to hang during configuration");

		units.add(aptTimeout);

		aptTimeout.appendLine("Acquire::http::Timeout \\\"3\\\";");
		aptTimeout.appendLine("Acquire::ftp::Timeout \\\"3\\\";");

		final FileUnit aptSources = new FileUnit("apt_debian_sources", "proceed", "/etc/apt/sources.list");
		units.add(aptSources);

		aptSources.appendLine("deb http://" + this.debianRepo + this.debianDir + " stretch main");
		aptSources.appendLine("deb http://security.debian.org/ stretch/updates main");
		aptSources.appendLine("deb http://" + this.debianRepo + this.debianDir + " stretch-updates main");

		return units;
	}

	@Override
	public final Set<IUnit> getLiveConfig() {
		final Set<IUnit> units = new HashSet<>();

		// First import all of the keys
		for (final String keyserver : this.pgpKeys.keySet()) {
			for (final String fingerprint : this.pgpKeys.get(keyserver)) {
				units.add(new SimpleUnit(fingerprint + "_pgp", "dirmngr_installed",
						"sudo apt-key adv --recv-keys --keyserver " + keyserver + " " + fingerprint,
						"sudo apt-key list 2>&1 | grep '" + fingerprint + "'", "", "fail",
						"Couldn't install the PGP signing cert " + fingerprint
								+ ". You can probably fix this by re-configuring the service."));
			}
		}

		// Then configure the sources
		for (final String source : this.sources.keySet()) {
			final String sourceLines = String.join("\n", this.sources.get(source));

			units.add(new FileUnit(source + "_apt_source", "proceed", sourceLines,
					"/etc/apt/sources.list.d/" + source + ".list"));
		}

		return units;
	}

	public final void addAptSource(String name, String sourceLine, String keyserver, String fingerprint)
			throws InvalidServerModelException {
		getNetworkModel().getServerModel(getLabel()).addEgress(new HostName(keyserver + "11371"));

		this.addAptSource(name, sourceLine);
		addPGPKey(keyserver, fingerprint);
	}

	private void addAptSource(String name, String sourceLine) {
		Set<String> sources = this.sources.get(name);
		if (sources == null) {
			sources = new HashSet<>();
		}
		sources.add(sourceLine);

		this.sources.put(name, sources);
	}

	private void addPGPKey(String keyserver, String fingerprint) {
		Set<String> fingerprints = this.pgpKeys.get(keyserver);
		if (fingerprints == null) {
			fingerprints = new HashSet<>();
		}
		fingerprints.add(fingerprint);

		this.pgpKeys.put(keyserver, fingerprints);
	}
}
