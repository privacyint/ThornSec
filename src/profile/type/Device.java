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

import javax.json.stream.JsonParsingException;

import core.data.machine.configuration.NetworkInterfaceData;
import core.data.machine.configuration.NetworkInterfaceData.Direction;
import core.exception.data.ADataException;
import core.exception.runtime.InvalidDeviceModelException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.machine.ADeviceModel;
import core.model.machine.configuration.networking.DHCPClientInterfaceModel;
import core.model.machine.configuration.networking.NetworkInterfaceModel;
import core.model.machine.configuration.networking.StaticInterfaceModel;
import core.model.network.NetworkModel;
import core.profile.AStructuredProfile;

/**
 * This is a device on our network
 */
public class Device extends AMachineProfile {

	public Device(String label, NetworkModel networkModel) throws InvalidServerModelException, JsonParsingException, ADataException, InvalidDeviceModelException {
		super(label, networkModel);
	}

	@Override
	public Collection<IUnit> getPersistentConfig() throws JsonParsingException, ADataException, InvalidDeviceModelException {
		final Collection<IUnit> units = new ArrayList<>();

		final ADeviceModel me = getNetworkModel().getDeviceModel(getLabel());

		try {
			if (getNetworkModel().getData().getNetworkInterfaces(getLabel()).get(Direction.WAN) != null) {
				for (final NetworkInterfaceData wanNic : getNetworkModel().getData().getNetworkInterfaces(getLabel()).get(Direction.WAN)) {
					NetworkInterfaceModel link = null;

					switch (wanNic.getInet()) {
					case STATIC:
						link = new StaticInterfaceModel(wanNic.getIface());
						break;
					case DHCP:
						link = new DHCPClientInterfaceModel(wanNic.getIface());
						break;
					default:
					}
					link.addAddress(wanNic.getAddress());
					link.setGateway(wanNic.getGateway());
					link.setBroadcast(wanNic.getBroadcast());
					link.setMac(wanNic.getMAC());
					link.setIsIPMasquerading(true);
					me.addNetworkInterface(link);
				}
			}
			for (final NetworkInterfaceData lanNic : getNetworkModel().getData().getNetworkInterfaces(getLabel()).get(Direction.LAN)) {
				NetworkInterfaceModel link = null;

				switch (lanNic.getInet()) {
				case STATIC:
					link = new StaticInterfaceModel(lanNic.getIface());
					break;
				case DHCP:
					link = new DHCPClientInterfaceModel(lanNic.getIface());
					break;
				default:
				}
				link.addAddress(lanNic.getAddress());
				link.setGateway(lanNic.getGateway());
				link.setBroadcast(lanNic.getBroadcast());
				link.setMac(lanNic.getMAC());
				me.addNetworkInterface(link);
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return units;
	}

}
