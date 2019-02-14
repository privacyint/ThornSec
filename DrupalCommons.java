package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.model.ServerModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;

public class DrupalCommons extends AStructuredProfile {
	
	Drupal drupal;
	
	public DrupalCommons(ServerModel me, NetworkModel networkModel) {
		super("drupalcommons", me, networkModel);
		
		this.drupal = new Drupal(me, networkModel);
	}

	protected Vector<IUnit> getInstalled() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(drupal.getInstalled());
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig() {
		Vector<IUnit> units =  new Vector<IUnit>();
		
		units.addAll(drupal.getPersistentConfig());
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(drupal.getLiveConfig());

		units.addElement(new SimpleUnit("commons_installed", "drupal_installed",
				"sudo /media/data/drush/drush -y -r /media/data/www dl commons"
				+ " && sudo /media/data/drush/drush si -y -r /media/data/www commons",
				"sudo /media/data/drush/drush -r /media/data/www pm-info commons_site_homepage 2>&1 | grep 'Status' | awk '{print $3}'", "enabled", "pass"));

		return units;
	}
	
	public Vector<IUnit> getNetworking() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(drupal.getNetworking());

		return units;
	}

}
