/*
 * This code is part of the ThornSec project.
 * 
 * To learn more, please head to its GitHub repo: @privacyint
 * 
 * Pull requests encouraged.
 */
package profile.service.machine;

import java.util.HashSet;
import java.util.Set;

import core.data.machine.AMachineData.Encapsulation;
import core.exception.data.InvalidPortException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;

import core.model.network.NetworkModel;

import core.profile.AStructuredProfile;

public class SMBServer extends AStructuredProfile {

	public SMBServer(String label, NetworkModel networkModel) {
		super("media", networkModel);
	}

	public Set<IUnit> getPersistentFirewall()
	throws InvalidServerModelException, InvalidPortException {
		Set<IUnit> units = new HashSet<IUnit>();
		
		networkModel.getServerModel(getLabel()).addListen(Encapsulation.TCP, 137, 138, 139, 445);
		
/*		for (ServerModel router : networkModel.getRouterServers()) {
			
			FirewallModel fm = router.getFirewallModel();
			
			fm.addFilter(getLabel() + "_fwd_ports_allow", me.getForwardChain(),
					"-p tcp"
					+ " -m state --state NEW,ESTABLISHED,RELATED"
					+ " -m tcp -m multiport --dports 139,445"
					+ " -j ACCEPT",
					"Comment");
			fm.addFilter(getLabel() + "_fwd_ports_udp_allow", me.getForwardChain(),
					"-p udp"
					+ " -m udp -m multiport --dports 137,138"
					+ " -j ACCEPT",
					"Ccccomment");
			
			//Allow users to resolve/use internally
			for (DeviceModel device : networkModel.getAllDevices()) {
				if (!device.getType().equals("user") && !device.getType().equals("superuser")) {
					continue;
				}
				
				fm.addFilter("allow_int_resolve_" + getLabel(), device.getForwardChain(),
						" -d " + me.getSubnets() + "/30"
						+ " -p tcp"
						+ " -m state --state NEW"
						+ " -m tcp -m multiport --dports 139,445"
						+ " -j ACCEPT",
						"omment");
				fm.addFilter("allow_int_resolve_udp_" + getLabel(), device.getForwardChain(),
						" -d " + me.getSubnets() + "/30"
						+ " -p udp"
						+ " -m udp -m multiport --dports 137,138"
						+ " -j ACCEPT",
						"Comment");
			}
		}
		*/
		
		return units;
	}
}
