package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.DeviceModel;
import core.model.FirewallModel;
import core.model.NetworkModel;
import core.model.ServerModel;
import core.profile.AStructuredProfile;

public class SMBServer extends AStructuredProfile {

	public SMBServer(ServerModel me, NetworkModel networkModel) {
		super("media", me, networkModel);
	}

	public Vector<IUnit> getNetworking() {
		Vector<IUnit> units = new Vector<IUnit>();

		for (ServerModel router : networkModel.getRouterServers()) {
			
			FirewallModel fm = router.getFirewallModel();
			
			fm.addFilter(me.getLabel() + "_fwd_ports_allow", me.getForwardChain(),
					"-p tcp"
					+ " -m state --state NEW,ESTABLISHED,RELATED"
					+ " -m tcp -m multiport --dports 139,445"
					+ " -j ACCEPT",
					"Comment");
			fm.addFilter(me.getLabel() + "_fwd_ports_udp_allow", me.getForwardChain(),
					"-p udp"
					+ " -m udp -m multiport --dports 137,138"
					+ " -j ACCEPT",
					"Ccccomment");
			
			//Allow users to resolve/use internally
			for (DeviceModel device : networkModel.getAllDevices()) {
				if (!device.getType().equals("user") && !device.getType().equals("superuser")) {
					continue;
				}
				
				fm.addFilter("allow_int_resolve_" + me.getLabel(), device.getForwardChain(),
						" -d " + me.getSubnets() + "/30"
						+ " -p tcp"
						+ " -m state --state NEW"
						+ " -m tcp -m multiport --dports 139,445"
						+ " -j ACCEPT",
						"omment");
				fm.addFilter("allow_int_resolve_udp_" + me.getLabel(), device.getForwardChain(),
						" -d " + me.getSubnets() + "/30"
						+ " -p udp"
						+ " -m udp -m multiport --dports 137,138"
						+ " -j ACCEPT",
						"Comment");
			}
		}
		
		return units;
	}
}
