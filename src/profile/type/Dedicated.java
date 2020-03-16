/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.type;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.json.stream.JsonParsingException;

import core.data.machine.configuration.NetworkInterfaceData;
import core.data.machine.configuration.NetworkInterfaceData.Direction;
import core.exception.data.ADataException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;

import core.model.network.NetworkModel;


/**
 * This is a dedicated server on your network. This is something ThornSec needs
 * to know about, but shouldn't attempt to configure
 */
public class Dedicated extends AMachineProfile {

	public Dedicated(String label, NetworkModel networkModel) throws InvalidServerModelException, JsonParsingException, ADataException, IOException {
		super(label, networkModel);

		final Map<Direction, Map<String, NetworkInterfaceData>> nics = networkModel.getData().getNetworkInterfaces(getLabel());

		if (nics != null) {
			nics.keySet().forEach(dir -> {
				nics.get(dir).forEach((iface, nic) -> {
					try {
						buildIface(nic, dir.equals(Direction.WAN));
					} catch (InvalidServerModelException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});
			});
		}
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		getNetworkModel().getServerModel(getLabel()).addEgress("cdn.debian.net:80");
		getNetworkModel().getServerModel(getLabel()).addEgress("security-cdn.debian.org:80");
		getNetworkModel().getServerModel(getLabel()).addEgress("prod.debian.map.fastly.net:80");
		getNetworkModel().getServerModel(getLabel()).addEgress("download.virtualbox.org:80");
		getNetworkModel().getServerModel(getLabel()).addEgress("cdn.debian.net:443");
		getNetworkModel().getServerModel(getLabel()).addEgress("security-cdn.debian.org:443");
		getNetworkModel().getServerModel(getLabel()).addEgress("prod.debian.map.fastly.net:443");
		getNetworkModel().getServerModel(getLabel()).addEgress("download.virtualbox.org:443");

		return units;
	}
}
