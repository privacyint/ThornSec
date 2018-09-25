package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.FirewallModel;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.pkg.InstalledUnit;

public class Email extends AStructuredProfile {
	
	private Nginx   webserver;
	private PHP     php;
	private MariaDB db;
	
	public Email() {
		super("email");
		
		webserver = new Nginx();
		php = new PHP();
		db = new MariaDB();
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(webserver.getInstalled(server, model));
		units.addAll(php.getInstalled(server, model));
		units.addAll(db.getInstalled(server, model));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units =  new Vector<IUnit>();
		
		units.addAll(webserver.getPersistentConfig(server, model));
		units.addAll(php.getPersistentConfig(server, model));
		units.addAll(db.getPersistentConfig(server, model));
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(webserver.getLiveConfig(server, model));
		units.addAll(php.getLiveConfig(server, model));
		units.addAll(db.getLiveConfig(server, model));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();

		units.addAll(webserver.getPersistentFirewall(server, model));
		
		model.getServerModel(server).getFirewallModel().addFilterForward(server,
					"-p tcp"
					+ " -m state --state NEW,ESTABLISHED"
					+ " -m tcp -m multiport --dports 993,465"
					+ " -j ACCEPT");
		model.getServerModel(server).getFirewallModel().addFilterForward(server,
					"-p tcp"
					+ " -m state --state ESTABLISHED"
					+ " -m tcp -m multiport --sports 993,465"
					+ " -j ACCEPT");
		
		for (String router : model.getRouters()) {
			
			FirewallModel fm = model.getServerModel(router).getFirewallModel();
			
			String intIface = model.getData().getIface(router);
			
			for (String device : model.getDeviceLabels()) {
				if (!model.getDeviceModel(device).getType().equals("user") && !model.getDeviceModel(device).getType().equals("superuser")) {
					continue;
				}
				
				if (!model.getData().getVpnOnly()) { 
					String emailRule = "";
					emailRule += "-i " + intIface + ":1+";
					emailRule += " -d " + model.getServerModel(server).getSubnet() + "/30";
					emailRule += " -p tcp";
					emailRule += " -m state --state NEW";
					emailRule += " -m tcp -m multiport --dports 993,465";
					emailRule += " -j ACCEPT";
					fm.addFilter("allow_int_only_" + server, device + "_fwd", emailRule);
				}
				
				String emailRule = "";
				emailRule += "-i " + intIface + ":2+";
				emailRule += " -d " + model.getServerModel(server).getSubnet() + "/30";
				emailRule += " -p tcp";
				emailRule += " -m state --state NEW";
				emailRule += " -m tcp -m multiport --dports 993,465";
				emailRule += " -j ACCEPT";
				fm.addFilter("allow_int_only_" + server, device + "_fwd", emailRule);
				
			}
		}
		
		return units;
	}
}