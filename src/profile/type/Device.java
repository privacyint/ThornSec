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
	public Collection<IUnit> getPersistentConfig() throws ADataException, InvalidDeviceModelException {
		final Collection<IUnit> units = new ArrayList<>();

		try {
			Map<Direction, Map<String, NetworkInterfaceData>> nics = getNetworkModel().getData().getNetworkInterfaces(getLabel());
	
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
		} catch (JsonParsingException | ADataException | IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return units;
	}

}
