package profile;

import java.net.InetAddress;
import java.util.Vector;

import core.data.InterfaceData;
import core.iface.IUnit;
import core.model.DeviceModel;
import core.model.NetworkModel;
import core.model.ServerModel;
import core.profile.AStructuredProfile;

public class StrongSwan extends AStructuredProfile {

	public StrongSwan(ServerModel me, NetworkModel networkModel) {
		super("strongswan", me, networkModel);
	}
	
	protected Vector<IUnit> getPersistentConfig() {
		Vector<IUnit> units = new Vector<IUnit>();
        
		;;
		
		return units;
	}

	public Vector<IUnit> getNetworking() {
		Vector<IUnit> units = new Vector<IUnit>();

		for (ServerModel router : networkModel.getRouterServers()) {
			DHCP dhcp = ((ServerModel)router).getRouter().getDHCP();
			DNS  dns  = ((ServerModel)router).getRouter().getDNS();
			
			for (DeviceModel user : networkModel.getUserDevices()) {
				
				String firstThree = user.getFirstOctet() + "." + user.getSecondOctet() + "." + user.getThirdOctet();
				
				int vpnSubnet = ((user.getSubnets().size()) * 4);

				InetAddress netmask = networkModel.getData().getNetmask();
				InetAddress subnet  = networkModel.stringToIP(firstThree + "." + vpnSubnet);
				InetAddress gateway = networkModel.stringToIP(firstThree + "." + (vpnSubnet + 1));
				InetAddress startIp = networkModel.stringToIP(firstThree + "." + (vpnSubnet + 2));
				InetAddress endIp   = startIp;
				
				String domain    = networkModel.getData().getDomain(router.getLabel());
				String subdomain = user.getLabel() + "." + networkModel.getLabel() + ".vpn";

				String roadWarriorClass = "";
				roadWarriorClass += "\n\n";
				roadWarriorClass += "\tclass \\\"" + user.getLabel() + "\\\" {\n";
				roadWarriorClass += "\t\tmatch if ((substring(hardware, 1, 2) = 7a:a7) and (option dhcp-client-identifier = \\\"" + user.getLabel() + "\\\"));\n";
				roadWarriorClass += "\t}";
				
				dhcp.addClass(roadWarriorClass);
				
				String roadWarrior = "";
				roadWarrior += "\n\n";
				roadWarrior += "\tsubnet " + subnet.getHostAddress() + " netmask " + netmask.getHostAddress() + " {\n";
				roadWarrior += "\t\tpool {\n";
				roadWarrior += "\t\t\tallow members of \\\"" + user.getLabel() + "\\\";\n";
				roadWarrior += "\t\t\trange " + startIp.getHostAddress() + " " + endIp.getHostAddress() + ";\n";
				roadWarrior += "\t\t\toption routers " + gateway.getHostAddress() + ";\n";
				roadWarrior += "\t\t}\n";
				roadWarrior += "\t}";
				
				dhcp.addStanza(roadWarrior);
				dns.addDomainRecord(domain, gateway, new String[]{subdomain}, startIp);
	
				user.getInterfaceModel().addIface(new InterfaceData(
						user.getLabel(), //host
						"lan0:2" + user.getThirdOctet() + vpnSubnet, //iface
						null, //mac
						"static", //inet
						null, //bridgeports
						subnet, //subnet
						startIp, //address
						netmask, //netmask
						null, //broadcast
						gateway, //gateway
						"VPN interface" //comment
				));
				
				//router.getInterfaceModel().addIface(new InterfaceData(
				//		user.getLabel(), //host
				//		"lan0:2" + user.getThirdOctet() + vpnSubnet, //iface
				//		null, //mac
				//		"static", //inet
				//		null, //bridgeports
				//		subnet, //subnet
				//		startIp, //address
				//		netmask, //netmask
				//		null, //broadcast
				//		gateway, //gateway
				//		"VPN interface" //comment
				//));
			}
		}

		networkModel.getIPSet().addToSet("user", 32, me.getIP());
		
		for (ServerModel router : networkModel.getRouterServers()) {
			InetAddress ip = networkModel.getServerModel(me.getLabel()).getIP();
			
			((ServerModel)router).getFirewallModel().addNatPrerouting("dnat_" + networkModel.getData().getExternalIp(me.getLabel()),
					"-p udp"
					+ " -m multiport"
					+ " --dports 500,4500"
					+ " -j DNAT"
					+ " --to-destination " + ip.getHostAddress(),
					"Redirect all external UDP traffic on :500 and :4500 (VPN ports) to our VPN server");
			((ServerModel)router).getFirewallModel().addFilter(me.getLabel() + "_allow_vpn_fwd", me.getForwardChain(),
					"-p udp"
					+ " -m multiport"
					+ " --dports 500,4500"
					+ " -j ACCEPT",
					"Allow internal UDP traffic on :500 and :4500 (VPN ports) to our VPN server");
			((ServerModel)router).getFirewallModel().addFilter(me.getLabel() + "_allow_vpn_internally", me.getForwardChain(),
					"-p udp"
					+ " -m multiport"
					+ " --sports 500,4500"
					+ " -j ACCEPT",
					"Allow internal UDP traffic on :500 and :4500 (VPN ports) to our VPN server");
			((ServerModel)router).getFirewallModel().addFilter(me.getLabel() + "_allow_vpn_in", me.getIngressChain(),
					"-p udp"
					+ " -m multiport"
					+ " --dports 500,4500"
					+ " -j ACCEPT",
					"Allow all external UDP traffic on :500 and :4500 (VPN ports)");

			((ServerModel)router).getFirewallModel().addFilter(me.getLabel() + "_allow_egress", me.getEgressChain(),
					"-j ACCEPT",
					"Allow the VPN to talk to the outside world");
			((ServerModel)router).getFirewallModel().addFilter(me.getLabel() + "_allow_ingress", me.getIngressChain(),
					"-m state --state ESTABLISHED,RELATED"
					+ " -j ACCEPT",
					"Allow the VPN to respond to valid traffic");
		}
		
		//for (ServerModel server : networkModel.getAllServers()) {
		//	server.addRequiredForward(me.getLabel());
		//}
		
		return units;
	}
}
