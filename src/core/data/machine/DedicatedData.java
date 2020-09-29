/*
 * This code is part of the ThornSec project.
 * 
 * To learn more, please head to its GitHub repo: @privacyint
 * 
 * Pull requests encouraged.
 */
package core.data.machine;

/**
 * Represents a Dedicated Server on our network.
 * 
 * This is a server which provides some form of functionality on your network,
 * but isn't under ThornSec's control.
 * 
 * Because ThornSec is representative of the network, it at least needs to know
 * about these servers so it can do routing and firewalling. 
 */
public class DedicatedData extends ServerData {

	public DedicatedData(String label) {
		super(label);

		this.putType(MachineType.DEDICATED);
	}
}
