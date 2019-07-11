package profile.service.web;

import java.util.Vector;

import core.iface.IUnit;
import core.model.network.NetworkModel;

import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;

public class DrupalCommons extends AStructuredProfile {
	
	Drupal7 drupal;
	
	public DrupalCommons(String label, NetworkModel networkModel) {
		super("drupalcommons", networkModel);
		
		this.drupal = new Drupal7(getLabel(), networkModel);
	}

	protected Set<IUnit> getInstalled() {
		Set<IUnit> units = new HashSet<IUnit>();
		
		units.addAll(drupal.getInstalled());
		
		return units;
	}
	
	protected Set<IUnit> getPersistentConfig() {
		Set<IUnit> units =  new HashSet<IUnit>();
		
		units.addAll(drupal.getPersistentConfig());
		
		return units;
	}

	protected Set<IUnit> getLiveConfig() {
		Set<IUnit> units = new HashSet<IUnit>();
		
		units.addAll(drupal.getLiveConfig());

		units.add(new SimpleUnit("commons_installed", "drupal_installed",
				"sudo /media/data/drush/drush -y -r /media/data/www dl commons"
				+ " && sudo /media/data/drush/drush si -y -r /media/data/www commons",
				"sudo /media/data/drush/drush -r /media/data/www pm-info commons_site_homepage 2>&1 | grep 'Status' | awk '{print $3}'", "enabled", "pass"));

		return units;
	}
	
	public Set<IUnit> getPersistentFirewall() {
		Set<IUnit> units = new HashSet<IUnit>();
		
		units.addAll(drupal.getPersistentFirewall());

		return units;
	}

}
