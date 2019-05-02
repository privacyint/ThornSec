package profile;

import java.util.Map;
import java.util.Vector;

import core.data.InterfaceData;
import core.iface.IUnit;
import core.model.FirewallModel;
import core.model.NetworkModel;
import core.model.ServerModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.CustomFileUnit;
import core.unit.pkg.InstalledUnit;

public class DHCP extends AStructuredProfile {

	private Vector<String> classes;
	private Vector<String> stanzas;
	private Vector<String> listeningIfaces;
	
	public DHCP(ServerModel me, NetworkModel networkModel) {
		super("dhcp", me, networkModel);

		this.classes         = new Vector<String>();
		this.stanzas         = new Vector<String>();
		this.listeningIfaces = new Vector<String>();
	}
	
	public void addStanza(String stanza) {
		this.stanzas.add(stanza);
	}
	
	public void addClass(String stanza) {
		this.classes.add(stanza);
	}

	public void addListeningIface(String iface) {
		this.listeningIfaces.add(iface);
	}

	public Vector<IUnit> getPersistentConfig() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String ifaceAutoString = "lan0";

		//Build listening interfaces
		for (InterfaceData iface : me.getInterfaces()) {
			//Skip over non-LAN interfaces, or we'll potentially offer DHCP to the whole internet!
			if (!iface.getIface().contains("lan") || iface.getMac() == null) { continue; }
			
			//ifaceAutoString += iface.getIface() + " ";
			this.stanzas.add(iface.getDhcpStanza());
		}
		
		units.addElement(((ServerModel)me).getConfigsModel().addConfigFile("dhcp_defiface", "dhcp_installed", "INTERFACES=\\\"" + ifaceAutoString + "\\\"", "/etc/default/isc-dhcp-server"));
		((ServerModel)me).getProcessModel().addProcess("/usr/sbin/dhcpd -4 -q -cf /etc/dhcp/dhcpd.conf " + ifaceAutoString + "$");

		return units;
	}

	protected Vector<IUnit> getInstalled() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new InstalledUnit("dhcp", "isc-dhcp-server"));

		return units;
	}
	
	public Vector<IUnit> getNetworking() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		FirewallModel fm = ((ServerModel)me).getFirewallModel();
		
		for (Map.Entry<String, String> lanIface : networkModel.getData().getLanIfaces(me.getLabel()).entrySet() ) {
			fm.addFilterInput("dhcp_ipt_in", "-i " + lanIface.getKey() + " -p udp --dport 67 -j ACCEPT", "Allow server to get an address by DHCP (This is only used up until the first configuration)");
			fm.addFilterOutput("dhcp_ipt_out", "-o " + lanIface.getKey() + " -p udp --sport 67 -j ACCEPT", "Allow server to get an address by DHCP (This is only used up until the first configuration)");
		}

		return units;
	}

	protected Vector<IUnit> getLiveConfig() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new CustomFileUnit("dhcp_custom_conf", "dhcp_installed", "/etc/dhcp/dhcpd.custom.conf"));

		String dhcpconf = "";
		dhcpconf += "include \\\"/etc/dhcp/dhcpd.custom.conf\\\";\n";
		dhcpconf += "\n";
		dhcpconf += "ddns-update-style none;\n";
		dhcpconf += "option domain-name \\\"" + networkModel.getData().getDomain(me.getLabel()) + "\\\";\n";
		dhcpconf += "option domain-name-servers 10.0.0.1;\n";
		dhcpconf += "default-lease-time 600;\n";
		dhcpconf += "max-lease-time 1800;\n";
		dhcpconf += "authoritative;\n";
		dhcpconf += "log-facility local7;\n";
		dhcpconf += "\n";
		dhcpconf += "shared-network " + networkModel.getLabel() + " {\n";
		dhcpconf += "\n";
		dhcpconf += "\tsubnet 10.0.0.0 netmask 255.0.0.0 {\n"; 
		dhcpconf += "\t}\n";

		if (networkModel.getData().getAutoGuest()) {
			dhcpconf += "\n";
			dhcpconf += "\t#This is our pool for guest connections\n";
			dhcpconf += "\t#We put it first, because everyone is a guest until they aren't!\n";
			dhcpconf += "\tsubnet 10.250.0.0 netmask 255.255.252.0 {\n";
			dhcpconf += "\t\tpool {\n";
			dhcpconf += "\t\t\trange 10.250.0.15 10.250.3.255;\n";
			dhcpconf += "\t\t\toption routers 10.0.0.1;\n";
			dhcpconf += "\t\t\toption domain-name-servers 1.1.1.1;\n";
			dhcpconf += "\t\t}\n";
			dhcpconf += "\t}";
		}

		for (int i = 0; i < this.classes.size(); ++i) {
			dhcpconf += this.classes.elementAt(i);
		}

		dhcpconf += "\n";

		for (String stanza : this.stanzas) {
			dhcpconf += stanza;
		}
		
		dhcpconf += "\n}";

		units.addElement(((ServerModel)me).getConfigsModel().addConfigFile("dhcp", "dhcp_installed", dhcpconf, "/etc/dhcp/dhcpd.conf"));

		units.addElement(new SimpleUnit("dhcp_running", "dhcp_installed",
				"",
				"sudo service isc-dhcp-server restart", "", "pass"));

		return units;
	}	
}
