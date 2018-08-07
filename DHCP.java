package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;

public class DHCP extends AStructuredProfile {

	private Vector<String> classes;
	private Vector<String> stanzas;
	private Vector<String> listeningIfaces;
	
	private String invalidChars;
	
	public DHCP() {
		super("dhcp");

		classes         = new Vector<String>();
		stanzas         = new Vector<String>();
		listeningIfaces = new Vector<String>();
		
		invalidChars = "[^\\-a-zA-Z0-9]";
	}
	
	public void addStanza(String stanza) {
		stanzas.add(stanza);
	}
	
	public void addClass(String stanza) {
		classes.add(stanza);
	}

	public void addListeningIface(String iface) {
		listeningIfaces.add(iface);
	}

	public Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		//If our router is also a metal, then we only want to bind to bridges
		if (model.getServerModel(server).isMetal()) {
			units.addElement(new InstalledUnit("bridge_utils", "proceed", "bridge-utils"));

			//DHCP listening interfaces
			for (String service : model.getServerModel(server).getServices()) {
				String subnet = model.getData().getSubnet(service);
				addListeningIface("br" + subnet);
			}
		}
		//Otherwise, just bind to the internally-facing interface
		else {
			addListeningIface(model.getData().getIface(server));
		}

		String defiface = "INTERFACES=\\\"";
		String procString = "/usr/sbin/dhcpd -4 -q -cf /etc/dhcp/dhcpd.conf ";

		for (String iface : listeningIfaces) {
			defiface += iface + " ";
			procString += iface + " ";
		}

		defiface = defiface.trim() + "\\\"";
		procString = procString.trim() + "$";
		
		units.addElement(model.getServerModel(server).getConfigsModel().addConfigFile("dhcp_defiface", "dhcp_installed", defiface, "/etc/default/isc-dhcp-server"));

		model.getServerModel(server).getProcessModel().addProcess(procString);
		
		String dhcpconf  = "";
		
		for (String srv : model.getServerLabels()) {
			if (!model.getServerModel(srv).isRouter()) {
				dhcpconf = "\n\n";
				dhcpconf += "\tsubnet " + model.getServerModel(srv).getSubnet() + " netmask " + model.getData().getNetmask() + " {\n";
				dhcpconf += "\t\thost " + srv .replaceAll(invalidChars, "_")+ " {\n";
				dhcpconf += "\t\t\thardware ethernet " + model.getServerModel(srv).getMac() + ";\n";
				dhcpconf += "\t\t\tfixed-address " + model.getServerModel(srv).getIP() + ";\n";
				dhcpconf += "\t\t\toption routers " + model.getServerModel(srv).getGateway() + ";\n";
				dhcpconf += "\t\t}\n";
				dhcpconf += "\t}";
				
				stanzas.add(dhcpconf);
			}
		}
		
		for (String device : model.getDeviceLabels()) {
			String[] subnets  = model.getDeviceModel(device).getSubnets();
			String[] ips      = model.getDeviceModel(device).getIPs();
			String[] macs     = model.getDeviceModel(device).getMacs();
			String[] gateways = model.getDeviceModel(device).getGateways();

			String netmask = model.getDeviceModel(device).getNetmask();
			
			for (int i = 0; i < subnets.length; ++i) {
				dhcpconf = "\n\n";
				dhcpconf += "\tsubnet " + subnets[i] + " netmask " + netmask + " {\n";
				dhcpconf += "\t\thost " + device.replaceAll(invalidChars, "_") + "_" + i + " {\n";
				dhcpconf += "\t\t\thardware ethernet " + macs[i] + ";\n";
				dhcpconf += "\t\t\tfixed-address " + ips[i] + ";\n";
				dhcpconf += "\t\t\toption routers " + gateways[i] + ";\n";
				dhcpconf += "\t\t}\n";
				dhcpconf += "\t}";
				
				stanzas.add(dhcpconf);
			}
		}
		
		return units;
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new InstalledUnit("dhcp", "isc-dhcp-server"));

		return units;
	}
	
	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(model.getServerModel(server).getFirewallModel().addNatPostrouting("router_nat", "-o " + model.getData().getExtIface(server) + " -j MASQUERADE"));

		units.addElement(model.getServerModel(server).getFirewallModel().addFilterInput("dhcp_ipt_in",
				"-i " + model.getData().getIface(server) + " -p udp --dport 67 -j ACCEPT"));
		units.addElement(model.getServerModel(server).getFirewallModel().addFilterOutput("dhcp_ipt_out",
				"-o " + model.getData().getIface(server) + " -p udp --sport 67 -j ACCEPT"));

		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new RunningUnit("dhcp", "isc-dhcp-server", "isc-dhcp-server"));
		
		String dhcpconf = "ddns-update-style none;\n";
		dhcpconf += "option domain-name \\\"" + model.getData().getDomain(server) + "\\\";\n";
		dhcpconf += "option domain-name-servers " + model.getServerModel(server).getGateway() + ";\n";
		dhcpconf += "default-lease-time 600;\n";
		dhcpconf += "max-lease-time 1800;\n";
		dhcpconf += "authoritative;\n";
		dhcpconf += "log-facility local7;";
		
		for (int i = 0; i < classes.size(); ++i) {
			dhcpconf += classes.elementAt(i);
		}

		dhcpconf += "\n\n";
		// add iptfwd for shared net
		dhcpconf += "shared-network sharednet {\n";
		dhcpconf += "\n";
		dhcpconf += "\tsubnet " + model.getServerModel(server).getSubnet() + " netmask " + model.getData().getNetmask() + " {\n";
		dhcpconf += "\t}";

		for (String stanza : stanzas) {
			dhcpconf += stanza;
		}
		
		dhcpconf += "\n}";

		units.addElement(model.getServerModel(server).getConfigsModel().addConfigFile("dhcp", "dhcp_installed", dhcpconf, "/etc/dhcp/dhcpd.conf"));

		return units;
	}	
}
