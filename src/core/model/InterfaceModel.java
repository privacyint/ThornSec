package core.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Vector;

import core.iface.IUnit;
import core.unit.SimpleUnit;

public class InterfaceModel extends AModel {

	private Vector<String> stanzas;
	private Vector<String> ifaces;

	public InterfaceModel(String label) {
		super(label);
	}

	public void init(NetworkModel model) {
		this.stanzas = new Vector<>();
		this.ifaces  = new Vector<>();
	}

	public Vector<IUnit> getUnits() {
		Vector<IUnit> units = new Vector<IUnit>();
		units.addElement(new SimpleUnit("net_conf_persist", "proceed",
				"echo \"" + getPersistent() + "\" | sudo tee /etc/network/interfaces; sudo service networking restart;",
				"cat /etc/network/interfaces;", getPersistent(), "pass",
				"Couldn't create our required network interfaces.  This will cause all sorts of issues."));
		return units;
	}

    public SimpleUnit addIface(String name, String type, String iface, String bridgePorts, String address, String netmask, String broadcast, String gateway) {
		String net = "";
		net += "iface " + iface + " inet " + type;
		net += (bridgePorts != null) ?  "\n" + "bridge_ports " + bridgePorts : "";
		net += (address != null) ? "\n" + "address " + address : "";
		net += (netmask != null) ? "\n" + "netmask " + netmask : "";
		net += (broadcast != null) ? "\n" + "broadcast " + broadcast : "";
		net += (gateway != null) ? "\n" + "gateway " + gateway : "";
		stanzas.add(net);
		ifaces.add(iface);
		return new SimpleUnit(name, "proceed", "echo \\\"handled by model\\\";",
				"grep \"" + address.replace(".", "\\.") + "\" /etc/network/interfaces;", "", "fail");
	}

	public SimpleUnit addPPPIface(String name, String iface) {
		String net = "";
		net +=	"iface " + iface + " inet manual\n";
		net += "\n";
		net += "auto provider\n";
		net += "iface provider inet ppp\n";
		net += "provider provider";
		stanzas.add(net);
		ifaces.add(iface);
		return new SimpleUnit(name, "proceed", "echo \\\"handled by model\\\";",
				"grep \"iface provider inet ppp\" /etc/network/interfaces;",
				"iface provider inet ppp", "pass");
	}

	private String getPersistent() {
		String net = "source /etc/network/interfaces.d/*\n";
		net += "\n";
		net += "auto lo\n";
		net += "iface lo inet loopback\n";
		net += "pre-up iptables-restore < /etc/iptables/iptables.conf";
		
		ifaces = new Vector<String>(new LinkedHashSet<String>(ifaces)); //Remove any duplicate stanzas
		Collections.sort(ifaces); // Sort them into what's (hopefully) a sensible order

		net += "\n\n";
		net += "auto";
		for (String iface : ifaces) {
			net += " " + iface;
		}
		
		stanzas = new Vector<String>(new LinkedHashSet<String>(stanzas)); //Remove any duplicate stanzas
		for (String stanza : stanzas) {
			net += "\n\n";
			net += stanza;
		}
		return net.trim();
	}

}
