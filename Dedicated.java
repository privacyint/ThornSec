package profile;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.JOptionPane;

import core.data.InterfaceData;
import core.iface.IUnit;
import core.model.InterfaceModel;
import core.model.NetworkModel;
import core.model.ServerModel;
import core.profile.AStructuredProfile;
import core.unit.fs.DirUnit;

public class Dedicated extends AStructuredProfile {
	
	public Dedicated(ServerModel me, NetworkModel networkModel) {
		super("dedicated", me, networkModel);
	}

	protected Vector<IUnit> getPersistentConfig() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		//Create /media/metaldata
		units.addElement(new DirUnit("metaldata_bindpoint", "proceed", "/media/metaldata"));
		//Create /media/data bindfs point
		units.addElement(new DirUnit("data_dir_exists", "proceed", "/media/data/"));

		return units;
	}

	public Vector<IUnit> getNetworking() {
		Vector<IUnit> units = new Vector<IUnit>();

		HashMap<String, String> lanIfaces = networkModel.getData().getLanIfaces(me.getLabel());
		if (lanIfaces.isEmpty()) {
			JOptionPane.showMessageDialog(null, "You must declare at least one lan interface for \"" + me.getLabel() + ".\n\nFormat is:\n\"lan\":[{\"interfacename\":\"macaddress\"}]");
			System.exit(1);
		}
		else {
			me.setFirstOctet(10);
			me.setSecondOctet(0);
			me.setThirdOctet(networkModel.getDediServers().indexOf(me) + 1);
			
			int i = 0;
			
			for (Map.Entry<String, String> lanIface : lanIfaces.entrySet() ) {
				InterfaceModel im = me.getInterfaceModel();
				
				InetAddress subnet    = networkModel.stringToIP(me.getFirstOctet() + "." + me.getSecondOctet() + "." + me.getThirdOctet() + "." + (i * 4));
				InetAddress router    = networkModel.stringToIP(me.getFirstOctet() + "." + me.getSecondOctet() + "." + me.getThirdOctet() + "." + ((i * 4) + 1));
				InetAddress address   = networkModel.stringToIP(me.getFirstOctet() + "." + me.getSecondOctet() + "." + me.getThirdOctet() + ((i * 4) + 2));
				InetAddress broadcast = networkModel.stringToIP(me.getFirstOctet() + "." + me.getSecondOctet() + "." + me.getThirdOctet() + ((i * 4) + 3));
				InetAddress netmask   = networkModel.getData().getNetmask();
				
				im.addIface(new InterfaceData(me.getLabel(),
											lanIface.getKey(),
											lanIface.getValue(),
											"static",
											null,
											subnet,
											address,
											netmask,
											broadcast,
											router,
											"comment goes here")
				);
				
				++i;
			}
		}

		me.addRequiredEgress("cdn.debian.net");
		me.addRequiredEgress("security-cdn.debian.org");
		me.addRequiredEgress("prod.debian.map.fastly.net");
		me.addRequiredEgress("download.virtualbox.org");
		
		return units;
	}
}
