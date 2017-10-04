package core.model;

import java.util.LinkedHashSet;
import java.util.Vector;

import core.data.NetworkData;
import core.iface.IUnit;
import core.profile.AProfile;
import core.unit.SimpleUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;
import profile.Metal;
import profile.Router;
import profile.SSH;
import profile.Service;

public class ServerModel extends AModel {

	private NetworkData networkData;

	private NetworkModel networkModel;

	private AptSourcesModel aptm;
	
	private FirewallModel fm;

	private InterfaceModel im;
	
	private ProcessModel pm;
	
	private BindFsModel bfm;
	
	private UserModel um;
	
	private Vector<String> services;
	
	public ServerModel(String label) {
		super(label);
	}

	public void setData(NetworkData data) {
		this.networkData = data;
	}

	public void init(NetworkModel model) {
		this.networkModel = model;
		this.services = new Vector<>();
		String[] types = this.networkData.getTypes(this.getLabel());
		for (int i = 0; i < types.length; i++) {
			if (types[i].equals("router")) {
				model.registerRouter(this.getLabel());
			} else if (types[i].equals("metal")) {
				model.registerMetal(this.getLabel());
			} else if (types[i].equals("service")) {
				model.registerService(this.getLabel());
				model.registerOnMetal(this.getLabel(), networkData.getMetal(this.getLabel()));
			} else {
				System.out.println("Unsupported type: " + types[i]);
			}
		}
		this.fm = new FirewallModel(this.getLabel());
		this.fm.init(model);
		this.im = new InterfaceModel(this.getLabel());
		this.im.init(model);
		this.pm = new ProcessModel(this.getLabel());
		this.pm.init(model);
		this.bfm = new BindFsModel(this.getLabel());
		this.bfm.init(model);
		this.aptm = new AptSourcesModel(this.getLabel());
		this.aptm.init(model);
		this.um = new UserModel(this.getLabel());
		this.um.init(model);
	}
	
	public void registerService(String label) {
		this.services.addElement(label);
	}
	
	public Vector<String> getServices() {
		return this.services;
	}

	public Vector<IUnit> getUnits() {		
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.insertElementAt(new SimpleUnit("create_pid_file", "proceed",
				"",
				"touch ~/script.pid; [ -f ~/script.pid ] && echo pass || echo fail", "pass", "pass"), 0);
		
		units.insertElementAt(new SimpleUnit("host", "proceed", "echo \"ERROR: Configuring with hostname mismatch\";",
				"sudo -S hostname;", networkData.getHostname(this.getLabel()), "pass"), 1);
		String configcmd = "";
		if (networkData.getUpdate(this.getLabel()).equals("true")) {
			configcmd = "sudo apt-get --assume-yes upgrade;";
		} else {
			configcmd = "echo \"There are `sudo apt-get upgrade -s |grep -P '^\\\\d+ upgraded'|cut -d\\\" \\\" -f1` updates available, of which `sudo apt-get upgrade -s | grep ^Inst | grep Security | wc -l` are security updates\"";
		}
		units.insertElementAt(new SimpleUnit("update", "proceed", configcmd,
				"sudo apt-get update > /dev/null; sudo apt-get --assume-no upgrade | grep \"[0-9] upgraded, [0-9] newly installed, [0-9] to remove and [0-9] not upgraded.\";",
				"0 upgraded, 0 newly installed, 0 to remove and 0 not upgraded.", "pass",
				"There are `sudo apt-get upgrade -s |grep -P '^\\\\d+ upgraded'|cut -d\" \" -f1` updates available, of which `sudo apt-get upgrade -s | grep ^Inst | grep Security | wc -l` are security updates\""), 2);

		//haveged is not perfect, but according to
		//https://security.stackexchange.com/questions/34523/is-it-appropriate-to-use-haveged-as-a-source-of-entropy-on-virtual-machines
		//is OK for producing entropy in VMs.
		//It is also recommended by novell for VM entropy http://www.novell.com/support/kb/doc.php?id=7011351
		units.insertElementAt(new InstalledUnit("entropy_generator", "proceed", "haveged"), 3);
		units.insertElementAt(new RunningUnit("entropy_generator", "haveged", "haveged"), 4);
		units.insertElementAt(new SimpleUnit("enough_entropy_available", "entropy_generator_installed",
				"while [ `cat /proc/sys/kernel/random/entropy_avail` -le 600 ]; do sleep 2; done;",
				"(($(cat /proc/sys/kernel/random/entropy_avail) > 600)) && echo pass || echo fail", "pass", "pass"), 5);
		
		this.pm.addProcess("/usr/sbin/haveged --Foreground --verbose=1 -w 1024$");
		
		//Remove rdnssd (problematic as hell...)
		units.insertElementAt(new SimpleUnit("rdnssd_uninstalled", "proceed",
				"sudo apt remove rdnssd --purge -y;",
				"dpkg-query --status rdnssd | grep \"Status:\";", "Status: install ok installed", "fail",
				"Couldn't uninstall rdnssd.  This is a package which attempts to be \"clever\" in DNS configuration and just breaks everything instead."), 6);
		
		SSH ssh = new SSH();
		units.addAll(ssh.getUnits(this.getLabel(), networkModel));

		this.pm.addProcess("sshd: " + networkModel.getData().getUser(this.getLabel()) + " \\[priv\\]$");
		this.pm.addProcess("sshd: " + networkModel.getData().getUser(this.getLabel()) + "@pts/0$");
		
		String[] types = this.networkData.getTypes(this.getLabel());
		for (int i = 0; i < types.length; i++) {
			if (types[i].equals("router")) {
				Router router = new Router();
				units.addAll(router.getUnits(this.getLabel(), networkModel));
			} else if (types[i].equals("metal")) {
				Metal metal = new Metal();
				units.addAll(metal.getUnits(this.getLabel(), networkModel));
			} else if (types[i].equals("service")) {
				Service service = new Service();
				units.addAll(service.getUnits(this.getLabel(), networkModel));
			} else {
				System.out.println("Unsupported type: " + types[i]);
			}

		}
		
		String profiles[] = this.networkData.getServerProfiles(this.getLabel());
		if (profiles != null) {
			for (int i = 0; i < profiles.length; i++) {
				try {
					AProfile profile = (AProfile) Class.forName("profile." + profiles[i]).newInstance();
					units.addAll(profile.getUnits(this.getLabel(), networkModel));
				} catch (Exception e) {
					System.err.println(profiles[i]);
				}
			}
		}
		
		units.addAll(2, fm.getUnits());
		units.addAll(2, bfm.getUnits());
		units.addAll(2, aptm.getUnits());
		units.addAll(2, im.getUnits());
		units.addAll(pm.getUnits());
		units.addAll(um.getUnits());
		
		units.addElement(new SimpleUnit("delete_pid_file", "proceed",
				"",
				"rm ~/script.pid; [ -f ~/script.pid ] && echo fail || echo pass", "pass", "pass"));		
		
		//Make sure we have no duplication in our unit tests (this can happen occasionally)
		units = new Vector<IUnit>(new LinkedHashSet<IUnit>(units));
		
		return units;
	}

	public FirewallModel getFirewallModel() {
		return this.fm;
	}
	
	public InterfaceModel getInterfaceModel() {
		return this.im;
	}
	
	public ProcessModel getProcessModel() {
		return this.pm;
	}
	
	public BindFsModel getBindFsModel() {
		return this.bfm;
	}
	
	public AptSourcesModel getAptSourcesModel() {
		return this.aptm;
	}
	
	public UserModel getUserModel() {
		return this.um;
	}

	public int getUnitCount() {
		return 0; //this.getUnits().size();
	}

	public String[] getProfiles() {
		String[] profiles = this.networkData.getServerProfiles(this.getLabel());
		
		if (profiles == null) {
			return new String[] {};
		}
		
		return profiles;
	}
	
	public String[] getTypes() {
		String[] types = this.networkData.getTypes(this.getLabel());
		
		if (types == null) {
			return new String[] {};
		}
		
		return types;
	}

	private String ipFromClass() {
		String subnet = this.networkData.getSubnet(this.getLabel());
		if (this.networkData.getIPClass().equals("c")) {
			return "192.168." + subnet;
		} else if (this.networkData.getIPClass().equals("b")) {
			return "172.16." + subnet;
		} else if (this.networkData.getIPClass().equals("a")) {
			return "10.0." + subnet;
		} else {
			return "0.0.0";
		}
	}
	
	public String getBroadcast() {
		return ipFromClass() + ".3";
	}
	
	public String getIP() {
		return ipFromClass() + ".2";
	}
	
	public String getGateway() {
		return ipFromClass() + ".1";

	}

	public String getSubnet() {
		return ipFromClass() + ".0";
	}
	
	public String getMac() {
		return this.networkData.getMac(this.getLabel());
	}
	
	public String getExtConn() {
		return this.networkData.getExtConn(this.getLabel());
	}
	
	public boolean isRouter() {
		String[] types = this.networkData.getTypes(this.getLabel());
		for (int i = 0; i < types.length; i++) {
			if (types[i].equals("router"))
				return true;
		}
		return false;
	}

	public boolean isMetal() {
		String[] types = this.networkData.getTypes(this.getLabel());
		for (int i = 0; i < types.length; i++) {
			if (types[i].equals("metal"))
				return true;
		}
		return false;
	}

	public boolean isService() {
		String[] types = this.networkData.getTypes(this.getLabel());
		for (int i = 0; i < types.length; i++) {
			if (types[i].equals("service"))
				return true;
		}
		return false;
	}

}
