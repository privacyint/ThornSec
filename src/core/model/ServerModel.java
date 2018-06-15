package core.model;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Vector;

import core.data.NetworkData;
import core.iface.IUnit;
import core.profile.AProfile;
import core.unit.SimpleUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;
import profile.DNS;
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
	
	private ConfigFileModel configs;
	
	private UserModel um;
	
	private Vector<String> services;
	
	private Router router;
	
	public ServerModel(String label) {
		super(label);
	}

	public void setData(NetworkData data) {
		this.networkData = data;
	}

	public void init(NetworkModel model) {
		this.networkModel = model;
		this.services = new Vector<String>();
		
		String   me    = this.getLabel();
		String[] types = this.networkData.getTypes(me);
		for (String type : types) {
			switch (type) {
				case "router":
					model.registerRouter(me);
					router = new Router();
					break;
				case "metal":
					model.registerMetal(me);
					break;
				case "service":
					model.registerService(me);
					model.registerOnMetal(me, networkData.getMetal(me));
					break;
				default:
					System.out.println("Unsupported type: " + type);
			}
		}
		
		this.fm = new FirewallModel(me);
		this.fm.init(model);
		this.im = new InterfaceModel(me);
		this.im.init(model);
		this.pm = new ProcessModel(me);
		this.pm.init(model);
		this.bfm = new BindFsModel(me);
		this.bfm.init(model);
		this.aptm = new AptSourcesModel(me);
		this.aptm.init(model);
		this.um = new UserModel(me);
		this.um.init(model);
		this.configs = new ConfigFileModel(me);
		this.configs.init(model);
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
		
		//Verify our PAM modules haven't been tampered with
		//https://www.trustedsec.com/2018/04/malware-linux/
		units.insertElementAt(new SimpleUnit("pam_not_tampered", "proceed",
				"",
				"find /lib/$(uname -m)-linux-gnu/security/ | xargs dpkg -S | cut -d ':' -f 1 | uniq | xargs dpkg -V", "", "pass",
				"There are unexpected/tampered PAM modules on this machine.  This is almost certainly an indicator that this machine has been compromised!"), 7);
		units.insertElementAt(new RunningUnit("syslog", "rsyslog", "rsyslog"), 7);
		
		SSH ssh = new SSH();
		units.addAll(ssh.getUnits(this.getLabel(), networkModel));

		this.pm.addProcess("sshd: " + networkModel.getData().getUser() + " \\[priv\\]$");
		this.pm.addProcess("sshd: " + networkModel.getData().getUser() + "@pts/0$");
		
		//Useful packages
		units.addElement(new InstalledUnit("sysstat", "proceed", "sysstat"));
		units.addElement(new InstalledUnit("lsof", "proceed", "lsof"));
		units.addElement(new InstalledUnit("net_tools", "proceed", "net-tools"));
		units.addElement(new InstalledUnit("htop", "proceed", "htop"));
		
		for (String type : this.getTypes()) {
			switch (type) {
				case "router":
					units.addAll(router.getUnits(this.getLabel(), networkModel));
					break;
				case "metal":
					Metal metal = new Metal();
					units.addAll(metal.getUnits(this.getLabel(), networkModel));
					break;
				case "service":
					Service service = new Service();
					units.addAll(service.getUnits(this.getLabel(), networkModel));
					break;
				default:
					System.out.println("Unsupported type: " + type);
			}

		}
		
		for (String profile : this.getProfiles()) {
			try {
				if (profile.equals("")) { continue; }
				
				AProfile profileClass = (AProfile) Class.forName("profile." + profile).newInstance();
				units.addAll(profileClass.getUnits(this.getLabel(), networkModel));
			} catch (Exception e) {
				System.err.println(profile);
				System.err.println(e.getMessage());
			}
		}
		
		units.addAll(2, fm.getUnits());
		units.addAll(2, bfm.getUnits());
		units.addAll(2, aptm.getUnits());
		units.addAll(2, im.getUnits());
		units.addAll(configs.getUnits());
		units.addAll(pm.getUnits());
		units.addAll(um.getUnits());
		
		units.addElement(new SimpleUnit("no_raw_sockets", "lsof_installed",
				"",
				"sudo lsof | grep RAW", "", "pass",
				"There are raw sockets running on this machine.  This is almost certainly a sign of compromise."));
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
	
	public ConfigFileModel getConfigsModel() {
		return this.configs;
	}
	
	public Router getRouter() {
		return this.router;
	}

	public int getUnitCount() {
		return 0; //this.getUnits().size();
	}

	public String[] getProfiles() {
		return this.networkData.getServerProfiles(this.getLabel());
	}
	
	public String[] getTypes() {
		return this.networkData.getTypes(this.getLabel());
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
		return Arrays.stream(types).anyMatch("router"::equals);
	}

	public boolean isMetal() {
		String[] types = this.networkData.getTypes(this.getLabel());
		return Arrays.stream(types).anyMatch("metal"::equals);
	}

	public boolean isService() {
		String[] types = this.networkData.getTypes(this.getLabel());
		return Arrays.stream(types).anyMatch("service"::equals);
	}
	
	public void addRouterFirewallRule(String server, NetworkModel model, String name, String hostname, String[] ports) {
		try {
			InetAddress ips[] = InetAddress.getAllByName(hostname);
			
			//This Comparator taken from https://thilosdevblog.wordpress.com/2010/09/15/sorting-ip-addresses-in-java/

			/**
			 * LGPL
			 */
			Arrays.sort(ips, new Comparator<InetAddress>() {
			    @Override
			    public int compare(InetAddress adr1, InetAddress adr2) {
			        byte[] ba1 = adr1.getAddress();
			        byte[] ba2 = adr2.getAddress();
			  
			        // general ordering: ipv4 before ipv6
			        if(ba1.length < ba2.length) return -1;
			        if(ba1.length > ba2.length) return 1;
			  
			        // we have 2 ips of the same type, so we have to compare each byte
			        for(int i = 0; i < ba1.length; i++) {
			            int b1 = unsignedByteToInt(ba1[i]);
			            int b2 = unsignedByteToInt(ba2[i]);
			            if(b1 == b2)
			                continue;
			            if(b1 < b2)
			                return -1;
			            else
			                return 1;
			        }
			        return 0;
			    }
			  
			    private int unsignedByteToInt(byte b) {
			        return (int) b & 0xFF;
			    }
			});
			
			String serverIp     = model.getServerModel(server).getIP();
			String cleanName    = server.replaceAll("-",  "_");
			String ingressChain = cleanName + "_ingress";
			String egressChain  = cleanName + "_egress";
			
			for (String router : model.getRouters()) {
				for (InetAddress ip : ips) {
					if (!ip.getHostAddress().contains(":")) { //no IPv6, please
						for (String port : ports) {
							model.getServerModel(router).getFirewallModel().addFilter(server + "_allow_in_" + port, ingressChain,
									"-s " + ip.getHostAddress()
									+ " -d " + serverIp
									+ " -p tcp"
									+ " --sport " + port
									+ " -j ACCEPT");
							model.getServerModel(router).getFirewallModel().addFilter(server + "_allow_out_" + port, egressChain,
									"-d " + ip.getHostAddress()
									+ " -s " + serverIp
									+ " -p tcp"
									+ " --dport " + port
									+ " -j ACCEPT");
						}
					}
				}
			}
		}
		catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public void addRouterPoison(String server, NetworkModel model, String domain, String ip, String[] ports) {
		String serverIp     = model.getServerModel(server).getIP();
		String cleanName    = server.replaceAll("-",  "_");
		String ingressChain = cleanName + "_ingress";
		String egressChain  = cleanName + "_egress";
		
		for (String router : model.getRouters()) {
			DNS dns = model.getServerModel(router).getRouter().getDNS();
			
			dns.addPoison(domain, ip);
			
			for (String port : ports) {
				model.getServerModel(router).getFirewallModel().addFilter(server + "_allow_in_" + port, ingressChain,
						"-s " + ip
						+ " -d " + serverIp
						+ " -p tcp"
						+ " --sport " + port
						+ " -j ACCEPT");
				model.getServerModel(router).getFirewallModel().addFilter(server + "_allow_out_" + port, egressChain,
						"-d " + ip
						+ " -s " + serverIp
						+ " -p tcp"
						+ " --dport " + port
						+ " -j ACCEPT");
			}
		}
	}

}
