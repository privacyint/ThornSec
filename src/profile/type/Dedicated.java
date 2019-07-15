/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.type;

import java.util.HashSet;
import java.util.Set;

import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.network.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.fs.DirUnit;

public class Dedicated extends AStructuredProfile {

	public Dedicated(String label, NetworkModel networkModel) throws InvalidServerModelException {
		super(label, networkModel);

		networkModel.getServerModel(label).setFirstOctet(10);
		networkModel.getServerModel(label).setSecondOctet(0);
//TODO: fixme
		// networkModel.getServerModel(label)
		// .setThirdOctet(networkModel.getDediServers().indexOf(networkModel.getServerModel(label))
		// + 1);
	}

	@Override
	protected Set<IUnit> getPersistentConfig() {
		final Set<IUnit> units = new HashSet<>();

		// Create /media/metaldata & /media/data bindfs point
		units.add(new DirUnit("metaldata_bindpoint", "proceed", "/media/metaldata"));
		units.add(new DirUnit("data_dir_exists", "proceed", "/media/data/"));

		return units;
	}

	@Override
	public Set<IUnit> getPersistentFirewall() throws InvalidServerModelException {
		final Set<IUnit> units = new HashSet<>();

		this.networkModel.getServerModel(getLabel()).addEgress("cdn.debian.net");
		this.networkModel.getServerModel(getLabel()).addEgress("security-cdn.debian.org");
		this.networkModel.getServerModel(getLabel()).addEgress("prod.debian.map.fastly.net");
		this.networkModel.getServerModel(getLabel()).addEgress("download.virtualbox.org");

		/*
		 * Hashtable<String, InterfaceData> lanIfaces =
		 * networkModel.getData().getLanIfaces(getLabel()); if (lanIfaces.isEmpty()) {
		 * JOptionPane.showMessageDialog(null,
		 * "You must declare at least one lan interface for \"" + getLabel() +
		 * ".\n\nFormat is:\n\"lan\":[{\"interfacename\":\"macaddress\"}]");
		 * System.exit(1); } else {
		 *
		 *
		 * int i = 0;
		 *
		 * for (Map.Entry<String, String> lanIface : lanIfaces.entrySet() ) {
		 * InterfaceModel im = me.getLanInterfaces();
		 *
		 * InetAddress subnet = networkModel.stringToIP(me.getFirstOctet() + "." +
		 * me.getSecondOctet() + "." + me.getThirdOctet() + "." + (i * 4)); InetAddress
		 * router = networkModel.stringToIP(me.getFirstOctet() + "." +
		 * me.getSecondOctet() + "." + me.getThirdOctet() + "." + ((i * 4) + 1));
		 * InetAddress address = networkModel.stringToIP(me.getFirstOctet() + "." +
		 * me.getSecondOctet() + "." + me.getThirdOctet() + "." + ((i * 4) + 2));
		 * InetAddress broadcast = networkModel.stringToIP(me.getFirstOctet() + "." +
		 * me.getSecondOctet() + "." + me.getThirdOctet() + "." + ((i * 4) + 3));
		 * InetAddress netmask = networkModel.getData().getNetmask();
		 *
		 * im.addIface(new InterfaceData(getLabel(), lanIface.getKey(),
		 * lanIface.getValue(), "static", null, subnet, address, netmask, broadcast,
		 * router, "comment goes here") );
		 *
		 * ++i; } }
		 */

		return units;
	}
}
