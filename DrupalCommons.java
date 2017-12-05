package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;

public class DrupalCommons extends AStructuredProfile {
	
	Drupal drupal;
	
	public DrupalCommons() {
		super("drupalcommons");
		
		this.drupal = new Drupal();
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(drupal.getInstalled(server, model));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units =  new Vector<IUnit>();
		
		units.addAll(drupal.getPersistentConfig(server, model));
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(drupal.getLiveConfig(server, model));

		units.addElement(new SimpleUnit("commons_installed", "drupal_installed",
				"sudo /media/data/drush/drush -y -r /media/data/www dl commons"
				+ " && sudo /media/data/drush/drush si -y -r /media/data/www commons",
				"sudo /media/data/drush/drush -r /media/data/www pm-info commons 2>&1 | grep 'Status' | awk '{print $3}'", "enabled", "pass"));

		return units;
	}
	
	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String cleanName   = server.replaceAll("-",  "_");
		String egressChain = cleanName + "_egress";
		
		//Allow email capability
		for (String router : model.getRouters()) {
			model.getServerModel(router).getFirewallModel().addFilter(server + "_allow_email", egressChain,
				"-p tcp"
				+ " --dport 25"
				+ " -j ACCEPT");
		}
		
		units.addAll(drupal.getPersistentFirewall(server, model));

		return units;
	}

}