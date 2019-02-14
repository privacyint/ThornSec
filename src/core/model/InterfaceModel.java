package core.model;

import java.util.LinkedHashSet;
import java.util.Vector;

import core.data.InterfaceData;
import core.iface.IUnit;
import core.unit.SimpleUnit;

public class InterfaceModel extends AModel {

	private Vector<InterfaceData> ifaces;
	
	private LinkedHashSet<String> names;
	
	private Vector<String> customStanzas; 

	InterfaceModel(String label, MachineModel me, NetworkModel networkModel) {
		super(label, me, networkModel);
		
		this.ifaces = new Vector<InterfaceData>();
		this.names = new LinkedHashSet<String>();
		this.customStanzas = new Vector<String>();
	}

	public void init() {
		;;
	}

	public Vector<IUnit> getUnits() {
		Vector<IUnit> units = new Vector<IUnit>();
		units.addElement(new SimpleUnit("net_conf_persist", "proceed",
				"echo \"" + getPersistent() + "\" | sudo tee /etc/network/interfaces; sudo service networking restart;",
				"cat /etc/network/interfaces;", getPersistent(), "pass",
				"Couldn't create our required network interfaces.  This will cause all sorts of issues."));
		return units;
	}

    public void addIface(InterfaceData iface) {
		ifaces.add(iface);
		names.add(iface.getIface());
	}
    
    /**
     * @return A Vector of InterfaceData
     */
    public Vector<InterfaceData> getIfaces() {
    	return ifaces;
    }
    
	public SimpleUnit addPPPIface(String name, String iface) {
		String net = "";
		net +=	"iface " + iface + " inet manual\n";
		net += "\n";
		net += "auto provider\n";
		net += "iface provider inet ppp\n";
		net += "provider provider";
		customStanzas.add(net);
		names.add(iface);
		return new SimpleUnit(name, "proceed", "echo \\\"handled by model\\\";",
				"grep \"iface provider inet ppp\" /etc/network/interfaces;",
				"iface provider inet ppp", "pass");
	}

	private String getPersistent() {
		String net = "source /etc/network/interfaces.d/*\n";
		net += "\n";
		net += "auto lo\n";
		net += "iface lo inet loopback\n";
		net += "pre-up /etc/ipsets/ipsets.up.sh | ipset restore\n";
		net += "pre-up /etc/iptables/iptables.conf.sh | iptables-restore\n";
		net += "\n";
		net += "auto";
		for (String name : names) {
			net += " " + name;
		}
		
		for (String stanza : customStanzas) {
			net += "\n\n";
			net += stanza;
		}

		for (InterfaceData iface : ifaces) {
			net += "\n\n";
			net += iface.getServerStanza();
		}

		return net.trim();
	}

}
