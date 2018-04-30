package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.FirewallModel;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.pkg.InstalledUnit;

public class EmailServer extends AStructuredProfile {
	
	public EmailServer() {
		super("emailserver");
	}

	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();

		for (String router : model.getRouters()) {
			
			FirewallModel fm = model.getServerModel(router).getFirewallModel();
			
			//DNAT email to our server
			fm.addNatPrerouting("dnat_email_ports",
					"-i " + model.getData().getExtIface(router)
					+ " -p tcp"
					+ " -m multiport"
					+ " --dports 25,465,993"
					+ " -j DNAT --to-destination " + model.getServerModel(server).getIP());				
		
			//Ingress
			fm.addFilter(server + "_ingress_email_allow", server + "_ingress",
					"-p tcp"
					+ " -m state --state NEW,ESTABLISHED,RELATED"
					+ " -m tcp -m multiport --dports 25,465,993"
					+ " -j ACCEPT");
			//Fwd
			fm.addFilter(server + "_fwd_email_allow", server + "_fwd",
					"-p tcp"
					+ " -m state --state NEW,ESTABLISHED,RELATED"
					+ " -m tcp -m multiport --dports 25,465,993"
					+ " -j ACCEPT");
			
			//Egress
			fm.addFilter(server + "_egress_email_allow", server + "_egress",
					"-p tcp"
					+ " -m state --state ESTABLISHED,RELATED"
					+ " -m tcp -m multiport --sports 25,465,993"
					+ " -j ACCEPT");
			
			//Allow users to resolve/use internally
			for (String device : model.getDeviceLabels()) {
				if (!model.getDeviceModel(device).getType().equals("user") && !model.getDeviceModel(device).getType().equals("superuser")) {
					continue;
				}
				
				String emailRule = "";
				emailRule += " -d " + model.getServerModel(server).getSubnet() + "/30";
				emailRule += " -p tcp";
				emailRule += " -m state --state NEW";
				emailRule += " -m tcp -m multiport --dports 25,465,993";
				emailRule += " -j ACCEPT";
				fm.addFilter("allow_int_resolve_" + server, device + "_fwd", emailRule);
			}
		}
		
		return units;
	}
}