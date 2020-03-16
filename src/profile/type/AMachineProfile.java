package profile.type;

import java.io.IOException;
import java.util.Map;

import javax.json.stream.JsonParsingException;

import core.data.machine.configuration.NetworkInterfaceData;
import core.data.machine.configuration.NetworkInterfaceData.Direction;
import core.exception.data.ADataException;
import core.exception.runtime.InvalidServerModelException;
import core.model.machine.configuration.networking.DHCPClientInterfaceModel;
import core.model.machine.configuration.networking.NetworkInterfaceModel;
import core.model.machine.configuration.networking.StaticInterfaceModel;
import core.model.network.NetworkModel;
import core.profile.AStructuredProfile;
import inet.ipaddr.IPAddress;

public abstract class AMachineProfile extends AStructuredProfile {
	
	protected AMachineProfile(String name, NetworkModel networkModel) {
		super(name, networkModel);
	}

	protected void buildIface(NetworkInterfaceData nic, Boolean masquerading) throws InvalidServerModelException {
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

		for (IPAddress address : nic.getAddresses()) {
			link.addAddress(address);
		}
		
		link.setGateway(nic.getGateway());
		link.setBroadcast(nic.getBroadcast());
		link.setMac(nic.getMAC());
		link.setIsIPMasquerading(masquerading);
		
		getNetworkModel().getServerModel(getLabel()).addNetworkInterface(link);
	}

	protected void buildNICs() {
		try {
			final Map<Direction, Map<String, NetworkInterfaceData>> nics = getNetworkModel().getData()
					.getNetworkInterfaces(getLabel());

			if (nics != null) {
				nics.keySet().forEach(dir -> {
					nics.get(dir).forEach((iface, nic) -> {
						try {
							buildIface(nic, dir.equals(Direction.WAN));
						} catch (final InvalidServerModelException e) {
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
	}
}
