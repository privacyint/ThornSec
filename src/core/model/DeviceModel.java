package core.model;

import java.net.InetAddress;

import java.util.Map;
import java.util.Set;
import java.util.Vector;

import core.data.InterfaceData;
import core.iface.IUnit;

public class DeviceModel extends MachineModel {

	public DeviceModel(String label, NetworkModel networkModel) {
		super(label, networkModel);
	}
	
	public Vector<IUnit> getNetworking() {
		InterfaceModel im = getInterfaceModel();

		super.setFirstOctet(10);
		super.setSecondOctet(50);
		super.setThirdOctet(networkModel.getAllDevices().indexOf(this) + 1);
		
		int i = 0;
		
		//Add this machine's interfaces
		for (Map.Entry<String, String> lanIface : networkModel.getData().getLanIfaces(getLabel()).entrySet() ) {	
			InetAddress subnet    = networkModel.stringToIP(getFirstOctet() + "." + getSecondOctet() + "." + getThirdOctet() + "." + (i * 4));
			InetAddress router    = networkModel.stringToIP(getFirstOctet() + "." + getSecondOctet() + "." + getThirdOctet() + "." + ((i * 4) + 1));
			InetAddress address   = networkModel.stringToIP(getFirstOctet() + "." + getSecondOctet() + "." + getThirdOctet() + "." + ((i * 4) + 2));
			InetAddress broadcast = networkModel.stringToIP(getFirstOctet() + "." + getSecondOctet() + "." + getThirdOctet() + "." + ((i * 4) + 3));
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
		
		return new Vector<IUnit>();
	}
	
	public String getType() {
		return getNetworkData().getDeviceType(getLabel());
	}
	
	public Boolean isThrottled() {
		return getNetworkData().getDeviceIsThrottled(getLabel());
	}

	public Boolean isManaged() {
		return getNetworkData().getDeviceIsManaged(getLabel());
	}
	
	public Set<Integer> getManagementPorts() {
		return getNetworkData().getDevicePorts(getLabel());
	}

	@Override
	protected Vector<IUnit> getUnits() {
		// TODO Auto-generated method stub
		return null;
	}
}
