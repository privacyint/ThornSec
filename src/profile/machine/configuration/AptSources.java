package profile.machine.configuration;

import java.net.URISyntaxException;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import inet.ipaddr.HostName;

import core.iface.IUnit;

import core.model.network.NetworkModel;

import core.profile.AStructuredProfile;

import core.unit.SimpleUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;

import core.exception.runtime.InvalidServerModelException;

public class AptSources extends AStructuredProfile {

	private HostName debianRepo;
	private String   debianDir;

	private Hashtable<String, Set<String>> sources;
	private Hashtable<String, Set<String>> pgpKeys;
	
	public AptSources(String label, NetworkModel networkModel)
	throws URISyntaxException, InvalidServerModelException {
		super(label, networkModel);

		this.debianRepo = networkModel.getData().getDebianMirror(getLabel());
		this.debianDir  = networkModel.getData().getDebianDirectory(getLabel());
		
		this.sources = new Hashtable<String, Set<String>>();
		this.pgpKeys     = new Hashtable<String, Set<String>>();

		networkModel.getServerModel(getLabel()).addEgress(this.debianRepo);
		networkModel.getServerModel(getLabel()).addEgress(new HostName("security.debian.org"));
		networkModel.getServerModel(getLabel()).addProcessString("dirmngr --daemon --homedir /tmp/apt-key-gpghome.[a-zA-Z0-9]*$");
	}

	@Override
	public final Set<IUnit> getInstalled() {
		Set<IUnit> units = new HashSet<IUnit>();
		
		units.add(new InstalledUnit("dirmngr", "proceed", "dirmngr",
				 "Couldn't install dirmngr.  Anything which requires a PGP key to be downloaded and installed won't work. "
				 + "You can possibly fix this by running a configuration again."));
		
		return units;
	}
	
	@Override
	public final Set<IUnit> getPersistentConfig() {
		Set<IUnit> units = new HashSet<IUnit>();

		//Give apt 3 seconds before timing out
		FileUnit aptTimeout = new FileUnit("decrease_apt_timeout", "proceed", "/etc/apt/apt.conf.d/99timeout",
				"Couldn't decrease the apt timeout. If your network connection is poor, the machine may appear to hang during configuration");

		units.add(aptTimeout);
		
		aptTimeout.appendLine("Acquire::http::Timeout \\\"3\\\";"); 
		aptTimeout.appendLine("Acquire::ftp::Timeout \\\"3\\\";");
		
		FileUnit aptSources = new FileUnit("apt_debian_sources", "proceed", "/etc/apt/sources.list");
		units.add(aptSources);
		
		aptSources.appendLine("deb http://" + debianRepo + debianDir + " stretch main");
		aptSources.appendLine("deb http://security.debian.org/ stretch/updates main");
		aptSources.appendLine("deb http://" + debianRepo + debianDir + " stretch-updates main");

		return units;
	}
	
	@Override
	public final Set<IUnit> getLiveConfig() {
		Set<IUnit> units = new HashSet<IUnit>();

		//First import all of the keys 
		for (String keyserver : this.pgpKeys.keySet()) {
			for (String fingerprint : this.pgpKeys.get(keyserver)) {
				units.add(new SimpleUnit(fingerprint + "_pgp", "dirmngr_installed",
						"sudo apt-key adv --recv-keys --keyserver " + keyserver + " " + fingerprint,
						"sudo apt-key list 2>&1 | grep '" + fingerprint + "'", "", "fail",
						"Couldn't install the PGP signing cert " + fingerprint + ". You can probably fix this by re-configuring the service."));
			}
		}
		
		//Then configure the sources
		for (String source : this.sources.keySet()) {
			String sourceLines = String.join("\n", this.sources.get(source));
			
			units.add(new FileUnit(source + "_apt_source", "proceed", sourceLines, "/etc/apt/sources.list.d/" + source + ".list"));			
		}
		
		return units;
	}

	public final void addAptSource(String name, String sourceLine, String keyserver, String fingerprint)
	throws InvalidServerModelException {
		networkModel.getServerModel(getLabel()).addEgress(new HostName(keyserver + "11371"));

		this.addAptSource(name, sourceLine);
		this.addPGPKey(keyserver, fingerprint);
	}
	
	private void addAptSource(String name, String sourceLine) {
		Set<String> sources = this.sources.get(name);
		if (sources == null) { sources = new HashSet<String>(); }
		sources.add(sourceLine);
		
		this.sources.put(name, sources);
	}
	
	private void addPGPKey(String keyserver, String fingerprint) {
		Set<String> fingerprints = this.pgpKeys.get(keyserver);
		if (fingerprints == null) { fingerprints = new HashSet<String>(); }
		fingerprints.add(fingerprint);
		
		this.pgpKeys.put(keyserver, fingerprints);
	}
}
