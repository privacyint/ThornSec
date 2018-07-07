package core.model;

import java.util.Vector;

import core.iface.IUnit;
import core.unit.SimpleUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;

public class AptSourcesModel extends AModel {

	private Vector<IUnit> sources;
	private String repo;
	private String dir;
	private Vector<IUnit> gpg;
	
	public AptSourcesModel(String label) {
		super(label);
	}

	public void init(NetworkModel model) {
		sources = new Vector<IUnit>();
		gpg = new Vector<IUnit>();
		repo = model.getData().getDebianMirror(this.getLabel());
		dir = model.getData().getDebianDirectory(this.getLabel());
		
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
		
		//Give it 3 seconds before timing out
		String timeoutConf = "";
		timeoutConf += "Acquire::http::Timeout \"3\";\n"; 
		timeoutConf += "Acquire::ftp::Timeout \"3\";";
		
		units.addElement(new FileUnit("decrease_apt_timeout", "proceed", timeoutConf, "/etc/apt/apt.conf.d/99timeout"));
		
		units.addAll(gpg);
		units.addAll(sources);
		
		return units;
	}

	public void addAptSource(String server, NetworkModel model, String name, String precondition, String sourceLine, String keyserver, String fingerprint) {
		sources.addElement(new FileUnit("source_" + name, precondition, sourceLine, "/etc/apt/sources.list.d/" + name + ".list"));
		
		model.getServerModel(server).addRouterFirewallRule(server, model, name, keyserver, new String[]{"11371"});
		
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
		list += "deb http://" + repo + dir + " stretch main\n";
		list += "deb http://security.debian.org/ stretch/updates main\n";
		list += "deb http://" + repo + dir + " stretch-updates main";
		
		return list;
	}
	
}
