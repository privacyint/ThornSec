package core.model;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;

import core.iface.IUnit;
import core.unit.SimpleUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;

public class AptSourcesModel extends AModel {

	Vector<IUnit> sources;
	String repo;
	Vector<IUnit> gpg;
	
	public AptSourcesModel(String label) {
		super(label);
	}

	public void init(NetworkModel model) {
		sources = new Vector<IUnit>();
		gpg = new Vector<IUnit>();
		repo = model.getData().getDebianMirror(this.getLabel());
		
		model.getServerModel(this.getLabel()).getProcessModel().addProcess("dirmngr --daemon --homedir /tmp/apt-key-gpghome.[a-zA-Z0-9]*$");

		model.getServerModel(this.getLabel()).addRouterFirewallRule(this.getLabel(), model, "base_debian", repo, new String[]{"80"});
		model.getServerModel(this.getLabel()).addRouterFirewallRule(this.getLabel(), model, "security_debian", "security.debian.org", new String[]{"80"});
	}

	public Vector<IUnit> getUnits() {
		Vector<IUnit> units = new Vector<IUnit>();

		units.addElement(new FileUnit("sources_list", "proceed", getPersistent(), "/etc/apt/sources.list"));
		units.addElement(new InstalledUnit("dirmngr", "proceed", "dirmngr",
						 "Couldn't install dirmngr.  Anything which requires a GPG key to be downloaded and installed won't work. "
						 + "You can possibly fix this by reconfiguring the service."));
		
		units.addAll(gpg);
		units.addAll(sources);
		
		return units;
	}

	public void addAptSource(String server, NetworkModel model, String name, String precondition, String sourceLine, String keyserver, String fingerprint) {
		sources.addElement(new FileUnit("source_" + name, precondition, sourceLine, "/etc/apt/sources.list.d/" + name + ".list"));
		
		URI hostname;
		try {
			hostname = new URI(keyserver);
			model.getServerModel(server).addRouterFirewallRule(server, model, name, hostname.getHost(), new String[]{"11371"});
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		addGpgKey(name, keyserver, fingerprint);
	}
	
	private void addGpgKey(String name, String keyserver, String fingerprint) {
		gpg.addElement(new SimpleUnit(name + "_gpg", "dirmngr_installed",
				"sudo apt-key adv --recv-keys --keyserver " + keyserver + " " + fingerprint,
				"sudo apt-key list 2>&1 | grep '" + name + "'", "", "fail",
				"Couldn't install " + name + "'s GPG signing cert.  " + name + "'s installation will fail.  You can probably fix this by re-configuring the service."));
	}
	
	private String getPersistent() {
		String list = "";
		list += "deb http://" + repo + "/debian/ stretch main\n";
		list += "deb http://security.debian.org/ stretch/updates main\n";
		list += "deb http://" + repo + "/debian/ stretch-updates main";
		
		return list;
	}
	
}
