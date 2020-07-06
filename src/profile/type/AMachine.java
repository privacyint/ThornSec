package profile.type;

import inet.ipaddr.IPAddress;

import core.exception.data.InvalidIPAddressException;
import core.exception.runtime.InvalidMachineModelException;
import core.data.machine.configuration.NetworkInterfaceData;
import core.model.machine.AMachineModel;
import core.model.machine.configuration.networking.DHCPClientInterfaceModel;
import core.model.machine.configuration.networking.NetworkInterfaceModel;
import core.model.machine.configuration.networking.StaticInterfaceModel;
import core.profile.AStructuredProfile;

public abstract class AMachine extends AStructuredProfile {
	
	public AMachine(AMachineModel me) {
		super(me);
	}

	protected void buildIface(NetworkInterfaceData nic, Boolean masquerading) throws InvalidIPAddressException, InvalidMachineModelException {
		NetworkInterfaceModel link = null;

		switch (nic.getInet()) {
		case STATIC:
			link = new StaticInterfaceModel(nic, getNetworkModel());
			break;
		case DHCP:
			link = new DHCPClientInterfaceModel(nic, getNetworkModel());
			// @TODO: DHCPClient is a raw socket. Fix that test.
			break;
		default:
			
		}

		if (nic.getAddresses().isPresent()) {
			for (IPAddress address : nic.getAddresses().get()) {
				link.addAddress(address);
			}
		}
		if (nic.getGateway().isPresent()) {
			link.setGateway(nic.getGateway().get());
			
		}
		if (nic.getBroadcast().isPresent()) {
			link.setBroadcast(nic.getBroadcast().get());
		}
		if (nic.getMAC().isPresent()) {
			link.setMac(nic.getMAC().get());
		}

		link.setIsIPMasquerading(masquerading);
		
		getMachineModel().addNetworkInterface(link);
	}

	protected void buildNICs() {
		/*
		try {
			final Map<Direction, Map<String, NetworkInterfaceData>> nics = getNetworkModel().getData()
					.getNetworkInterfaces(getLabel());

			if (nics != null) {
				nics.keySet().forEach(dir -> {
					nics.get(dir).forEach((iface, nic) -> {
						try {
							buildIface(nic, dir.equals(Direction.WAN));
						} catch (final InvalidMachineModelException e) {
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
		*/
	}
}
