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
import core.model.machine.ServerModel;
import core.model.machine.configuration.networking.DHCPClientInterfaceModel;
import core.model.machine.configuration.networking.NetworkInterfaceModel;
import core.model.machine.configuration.networking.StaticInterfaceModel;
import core.model.network.NetworkModel;
import core.profile.AStructuredProfile;

/**
 * This is a dedicated server on your network. This is something ThornSec needs
 * to know about, but shouldn't attempt to configure
 */
public class Dedicated extends AStructuredProfile {

	public Dedicated(String label, NetworkModel networkModel) throws InvalidServerModelException, JsonParsingException, ADataException {
		super(label, networkModel);

		final ServerModel me = getNetworkModel().getServerModel(getLabel());

		try {
			final Map<Direction, Collection<NetworkInterfaceData>> nics = networkModel.getData()
					.getNetworkInterfaces(getLabel());

			if (nics != null) {
				if (nics.containsKey(Direction.WAN)) {
					nics.get(Direction.WAN).forEach(nic -> {
						NetworkInterfaceModel link = null;

						switch (nic.getInet()) {
						case STATIC:
							link = new StaticInterfaceModel(nic.getIface());
							break;
						case DHCP:
							link = new DHCPClientInterfaceModel(nic.getIface());
							// @TODO: DHCPClient is a raw socket. Fix that test.
							break;
						default:
						}

						link.addAddress(nic.getAddress());
						link.setGateway(nic.getGateway());
						link.setBroadcast(nic.getBroadcast());
						link.setMac(nic.getMAC());
						link.setIsIPMasquerading(true);
						me.addNetworkInterface(link);
					});
				}
				if (nics.containsKey(Direction.LAN)) {
					nics.get(Direction.LAN).forEach(nic -> {
						NetworkInterfaceModel link = null;

						switch (nic.getInet()) {
						case STATIC:
							link = new StaticInterfaceModel(nic.getIface());
							break;
						case DHCP:
							link = new DHCPClientInterfaceModel(nic.getIface());
							break;
						default:
						}

						link.addAddress(nic.getAddress());
						link.setGateway(nic.getGateway());
						link.setBroadcast(nic.getBroadcast());
						link.setMac(nic.getMAC());
						me.addNetworkInterface(link);
					});
				}
			}
		} catch (final IOException e) {
			e.printStackTrace();
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
