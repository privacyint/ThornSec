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

		addFirewallRule(this.getLabel(), model, "base_debian", repo);
		addFirewallRule(this.getLabel(), model, "security_debian", "security.debian.org");
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
			hostname = new URI(sourceLine.split(" ")[1]);
			addFirewallRule(server, model, name, hostname.getHost());
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
	
	private void addFirewallRule(String server, NetworkModel model, String name, String hostname) {
		try {
			InetAddress mirrorIps[] = InetAddress.getAllByName(hostname);
			
			//This Comparator taken from https://thilosdevblog.wordpress.com/2010/09/15/sorting-ip-addresses-in-java/

			/**
			 * LGPL
			 */
			Arrays.sort(mirrorIps, new Comparator<InetAddress>() {
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
			
			String ip           = model.getServerModel(server).getIP();
			String cleanName    = server.replaceAll("-",  "_");
			String ingressChain = cleanName + "_ingress";
			String egressChain  = cleanName + "_egress";
			
			Vector<String> routers = model.getRouters();
			Iterator<String> itr = routers.iterator();
			
			while (itr.hasNext()) {
				String router = itr.next();
				
				for (int i = 0; i < mirrorIps.length; ++i) {
					if (!mirrorIps[i].getHostAddress().contains(":")) { //no IPv6, please
						model.getServerModel(router).getFirewallModel().addFilter(server + "_allow_apt_mirror_in_" + i, ingressChain,
								"-s " + mirrorIps[i].getHostAddress()
								+ " -d " + ip
								+ " -p tcp"
								+ " --sport 80"
								+ " -j ACCEPT");
						model.getServerModel(router).getFirewallModel().addFilter(server + "_allow_apt_mirror_out_" + i, egressChain,
								"-d " + mirrorIps[i].getHostAddress()
								+ " -s " + ip
								+ " -p tcp"
								+ " --dport 80"
								+ " -j ACCEPT");
					}
				}
			}
	
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

}
