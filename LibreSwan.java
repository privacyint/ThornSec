package profile;

import java.util.Iterator;
import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;
import java.util.Iterator;

public class LibreSwan extends AStructuredProfile {

	public LibreSwan() {
		super("libreswan");
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		return units;
	}

	protected Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
         
		return units;
	}

	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		if (model.getData().getExternalIp(server) != null) {
			Vector<String> routers = model.getRouters();
			Iterator<String> itr = routers.iterator();
			
			while (itr.hasNext()) {
				String router = itr.next();
				
				String ip           = model.getServerModel(server).getIP();
				String cleanName    = server.replaceAll("-",  "_");
				String fwdChain     = cleanName + "_fwd";
				String ingressChain = cleanName + "_fwd";
				String egressChain  = cleanName + "_fwd";
				
				model.getServerModel(router).getFirewallModel().addNatPrerouting("dnat_" + model.getData().getExternalIp(server) + "_1701",
						"-p udp --dport 1701 -j DNAT --to-destination " + model.getServerModel(server).getIP() + ":1701");
				model.getServerModel(router).getFirewallModel().addFilter(server + "_allow_1701", fwdChain,
						"-p udp"
						+ " --dport 1701"
						+ " -j ACCEPT");

				model.getServerModel(router).getFirewallModel().addNatPrerouting("dnat_" + model.getData().getExternalIp(server) + "_4500",
						"-p udp --dport 4500 -j DNAT --to-destination " + model.getServerModel(server).getIP() + ":4500");
				model.getServerModel(router).getFirewallModel().addFilter(server + "_allow_4500", fwdChain,
						"-p udp"
						+ " --dport 4500"
						+ " -j ACCEPT");
			
				model.getServerModel(router).getFirewallModel().addNatPrerouting("dnat_" + model.getData().getExternalIp(server) + "_500",
						"-p udp --dport 500 -j DNAT --to-destination " + model.getServerModel(server).getIP() + ":500");
				model.getServerModel(router).getFirewallModel().addFilter(server + "_allow_500", fwdChain,
						"-p udp"
						+ " --dport 500"
						+ " -j ACCEPT");

				model.getServerModel(router).getFirewallModel().addFilter(server + "_allow_egress", egressChain,
						"-j ACCEPT");
				model.getServerModel(router).getFirewallModel().addFilter(server + "_allow_ingress", ingressChain,
						"-m state --state ESTABLISHED,RELATED"
						+ " -j ACCEPT");
				
			}
		}
		
		return units;
	}


}
