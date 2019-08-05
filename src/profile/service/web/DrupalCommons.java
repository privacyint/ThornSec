/*
 *
 */
package profile.service.web;

import java.util.HashSet;
import java.util.Set;

import core.exception.data.InvalidPortException;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.network.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;

/**
 * This profile install and configures Drupal Commons.
 *
 * Drupal Commons is no longer maintained, this profile is kept for legacy
 * reasons only.
 */
public class DrupalCommons extends AStructuredProfile {

	private final Drupal7 drupal7;

	@Deprecated
	public DrupalCommons(String label, NetworkModel networkModel) {
		super(label, networkModel);

		this.drupal7 = new Drupal7(getLabel(), networkModel);
	}

	@Override
	protected Set<IUnit> getInstalled() throws InvalidServerModelException {
		final Set<IUnit> units = new HashSet<>();

		units.addAll(this.drupal7.getInstalled());

		return units;
	}

	@Override
	protected Set<IUnit> getPersistentConfig() throws InvalidServerException, InvalidServerModelException {
		final Set<IUnit> units = new HashSet<>();

		units.addAll(this.drupal7.getPersistentConfig());

		return units;
	}

	@Override
	protected Set<IUnit> getLiveConfig() throws InvalidServerModelException {
		final Set<IUnit> units = new HashSet<>();

		units.addAll(this.drupal7.getLiveConfig());

		units.add(new SimpleUnit("commons_installed", "drupal_installed",
				"sudo /media/data/drush/drush -y -r /media/data/www dl commons"
						+ " && sudo /media/data/drush/drush si -y -r /media/data/www commons",
				"sudo /media/data/drush/drush -r /media/data/www pm-info commons_site_homepage 2>&1 | grep 'Status' | awk '{print $3}'",
				"enabled", "pass"));

		return units;
	}

	@Override
	public Set<IUnit> getPersistentFirewall() throws InvalidServerModelException, InvalidPortException {
		final Set<IUnit> units = new HashSet<>();

		units.addAll(this.drupal7.getPersistentFirewall());

		return units;
	}

}
