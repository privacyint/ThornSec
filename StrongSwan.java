package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;

public class StrongSwan extends AStructuredProfile {

	public StrongSwan() {
		super("strongswan");
	}

	protected Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
        
		getDhcpConfig(model);
		
		return units;
	}

	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();

		for (String router : model.getRouters()) {
			String ip           = model.getServerModel(server).getIP();
			String hostname     = model.getData().getHostname(server);
			String fwdChain     = hostname + "_fwd";
			String ingressChain = hostname + "_ingress";
			String egressChain  = hostname + "_egress";
			
			model.getServerModel(router).getFirewallModel().addNatPrerouting("dnat_" + model.getData().getExternalIp(server),
					"-p udp"
					+ " -m multiport"
					+ " --dports 500,4500"
					+ " -j DNAT"
					+ " --to-destination " + ip);
			model.getServerModel(router).getFirewallModel().addFilter(server + "_allow_vpn_fwd", fwdChain,
					"-p udp"
					+ " -m multiport"
					+ " --dports 500,4500"
					+ " -j ACCEPT");
			model.getServerModel(router).getFirewallModel().addFilter(server + "_allow_vpn_internally", fwdChain,
					"-p udp"
					+ " -m multiport"
					+ " --sports 500,4500"
					+ " -j ACCEPT");
			model.getServerModel(router).getFirewallModel().addFilter(server + "_allow_vpn_in", ingressChain,
					"-p udp"
					+ " --dports 500,4500"
					+ " -j ACCEPT");

			model.getServerModel(router).getFirewallModel().addFilter(server + "_allow_egress", egressChain,
					"-j ACCEPT");
			model.getServerModel(router).getFirewallModel().addFilter(server + "_allow_ingress", ingressChain,
					"-m state --state ESTABLISHED,RELATED"
					+ " -j ACCEPT");
		}
		
		return units;
	}
	
	private Vector<IUnit> getDhcpConfig(NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();

		Vector<String> users = new Vector<String>();
		
		for (String device : model.getDeviceLabels()) {
			switch (model.getDeviceModel(device).getType()) {
				case "superuser":
				case "user":
					users.add(device);
					break;
			}
		}
		
		for (String router : model.getRouters()) {
			DHCP dhcp = model.getServerModel(router).getRouter().getDHCP();
			DNS  dns  = model.getServerModel(router).getRouter().getDNS();
			
			for (String user : users) {
				String firstThree = model.getDeviceModel(user).getSubnets()[0];
				firstThree = firstThree.substring(0, firstThree.length()-2);
				
				int vpnSubnet = ((model.getDeviceModel(user).getSubnets().length) * 4);

				String netmask = "255.255.255.252";//model.getData().getNetmask();
				
				String subnet    = firstThree + "." + vpnSubnet;
				String gateway   = firstThree + "." + (vpnSubnet + 1);
				String startIp   = firstThree + "." + (vpnSubnet + 2);
				String endIp     = firstThree + "." + (vpnSubnet + 2);
				String domain    = model.getData().getDomain(router);
				String subdomain = user + "." + model.getLabel() + ".vpn";

				String roadWarriorClass = "";
				roadWarriorClass += "\n\n";
				roadWarriorClass += "class \\\"" + user + "\\\" {\n";
				roadWarriorClass += "\tmatch if ((substring(hardware, 1, 2) = 7a:a7) and (option dhcp-client-identifier = \\\"" + user + "\\\"));\n";
				roadWarriorClass += "}";
				
				dhcp.addClass(roadWarriorClass);
				
				String roadWarrior = "";
				roadWarrior += "\n\n";
				roadWarrior += "\tsubnet " + subnet + " netmask " + netmask + " {\n";
				roadWarrior += "\t\tpool {\n";
				roadWarrior += "\t\t\tallow members of \\\"" + user + "\\\";\n";
				roadWarrior += "\t\t\trange " + startIp + " " + endIp + ";\n";
				roadWarrior += "\t\t\toption routers " + gateway + ";\n";
				roadWarrior += "\t\t}\n";
				roadWarrior += "\t}";
				
				dhcp.addStanza(roadWarrior);
				dns.addDomainRecord(domain, gateway, new String[]{subdomain}, startIp);
				
				units.addElement(model.getServerModel(router).getInterfaceModel().addIface(user.replaceAll("-", "_") + "_vpn_router_iface",
						"static",
						model.getData().getIface(router) + ":2" + firstThree.split("\\.")[2] + vpnSubnet,
						null,
						gateway,
						model.getData().getNetmask(),
						null,
						null));
			}
		}
		
 		return units;
	}
}