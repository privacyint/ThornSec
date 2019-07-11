package profile;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.JOptionPane;

import core.data.InterfaceData;
import core.iface.IUnit;
import core.model.InterfaceModel;
import core.model.network.NetworkModel;

import core.profile.AStructuredProfile;
import core.unit.fs.DirUnit;

public class Dedicated extends AStructuredProfile {
	
	public Dedicated(String label, NetworkModel networkModel) {
		super(label, networkModel);
		
		ServerModel me = networkModel.getServerModel(getLabel());
		networkModel.getServerModel(getLabel()).addEgressDestination("cdn.debian.net");
		networkModel.getServerModel(getLabel()).addEgressDestination("security-cdn.debian.org");
		networkModel.getServerModel(getLabel()).addEgressDestination("prod.debian.map.fastly.net");
		networkModel.getServerModel(getLabel()).addEgressDestination("download.virtualbox.org");
		
		me.setFirstOctet(10);
		me.setSecondOctet(0);
		me.setThirdOctet(networkModel.getDediServers().indexOf(me) + 1);
	}

	protected Set<IUnit> getPersistentConfig() {
		Set<IUnit> units = new HashSet<IUnit>();
		
		//Create /media/metaldata & /media/data bindfs point
		units.add(new DirUnit("metaldata_bindpoint", "proceed", "/media/metaldata"));
		units.add(new DirUnit("data_dir_exists", "proceed", "/media/data/"));

		return units;
	}

	public Set<IUnit> getPersistentFirewall() {
		Set<IUnit> units = new HashSet<IUnit>();
/*
		Hashtable<String, InterfaceData> lanIfaces = networkModel.getData().getLanIfaces(getLabel());
		if (lanIfaces.isEmpty()) {
			JOptionPane.showMessageDialog(null, "You must declare at least one lan interface for \"" + getLabel() + ".\n\nFormat is:\n\"lan\":[{\"interfacename\":\"macaddress\"}]");
			System.exit(1);
		}
		else {

			
			int i = 0;
			
			for (Map.Entry<String, String> lanIface : lanIfaces.entrySet() ) {
				InterfaceModel im = me.getLanInterfaces();
				
				InetAddress subnet    = networkModel.stringToIP(me.getFirstOctet() + "." + me.getSecondOctet() + "." + me.getThirdOctet() + "." + (i * 4));
				InetAddress router    = networkModel.stringToIP(me.getFirstOctet() + "." + me.getSecondOctet() + "." + me.getThirdOctet() + "." + ((i * 4) + 1));
				InetAddress address   = networkModel.stringToIP(me.getFirstOctet() + "." + me.getSecondOctet() + "." + me.getThirdOctet() + "." + ((i * 4) + 2));
				InetAddress broadcast = networkModel.stringToIP(me.getFirstOctet() + "." + me.getSecondOctet() + "." + me.getThirdOctet() + "." + ((i * 4) + 3));
				InetAddress netmask   = networkModel.getData().getNetmask();
				
				im.addIface(new InterfaceData(getLabel(),
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
*/
		
		
		return units;
	}
}
