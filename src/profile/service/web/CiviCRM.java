/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.service.web;

import java.util.ArrayList;
import java.util.Collection;

import core.exception.data.InvalidPortException;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.network.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.DirUnit;
import core.unit.pkg.InstalledUnit;
import profile.stack.MariaDB;

/**
 * This profile installs and configures CiviCRM (https://civicrm.org/) on a
 * Drupal (7) base.
 */
@SuppressWarnings("deprecation") // Civi requires Drupal 7, which is deprecated.
public class CiviCRM extends AStructuredProfile {

	private final Drupal7 drupal;
	private final MariaDB db;

	public CiviCRM(String label, NetworkModel networkModel) {
		super(label, networkModel);

		this.drupal = new Drupal7(getLabel(), networkModel);
		this.db = new MariaDB(getLabel(), networkModel);

		this.db.setUsername("civicrm");
		this.db.setUserPrivileges("SUPER");
		this.db.setUserPassword("${CIVICRM_PASSWORD}");
		this.db.setDb("civicrm");
	}

	@Override
	protected Collection<IUnit> getInstalled() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.db.getInstalled());
		units.addAll(this.drupal.getInstalled());

		units.add(new InstalledUnit("php_imagemagick", "php_fpm_installed", "php-imagick"));
		units.add(new InstalledUnit("php_mcrypt", "php_fpm_installed", "php-mcrypt"));

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentConfig() throws InvalidServerException, InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.db.getPersistentConfig());
		units.addAll(this.drupal.getPersistentConfig());

		return units;
	}

	@Override
	public Collection<IUnit> getLiveConfig() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.db.getLiveConfig());
		units.addAll(this.drupal.getLiveConfig());

		units.add(new DirUnit("drush_home", "drupal_installed", "~/.drush"));

		// This either grabs the preconfigured password out of the settings file, or
		// creates a new, random, URL-Encoded one
		units.add(new SimpleUnit("civicrm_mysql_password", "proceed",
				"CIVICRM_PASSWORD=`grep \"define('CIVICRM_DSN', 'mysql://\" /media/data/www/sites/default/civicrm.settings.php 2>/dev/null | awk -F'[:@]' '{print $3}'`; [[ -z $CIVICRM_PASSWORD ]] && CIVICRM_PASSWORD=`openssl rand -hex 32`",
				"echo $CIVICRM_PASSWORD", "", "fail",
				"Couldn't set a password for CiviCRM's database user. The installation will fail."));

		units.addAll(this.db.checkUserExists());
		units.addAll(this.db.checkDbExists());

		units.add(new SimpleUnit("civicrm_installed", "drupal_installed",
				"sudo wget 'https://download.civicrm.org/civicrm-4.7.27-drupal.tar.gz' -O /media/data/www/sites/all/modules/civi.tar.gz"
						+ " && sudo tar -zxf /media/data/www/sites/all/modules/civi.tar.gz -C ~/.drush/ civicrm/drupal/drush"
						+ " && sudo -E /media/data/drush/drush -r /media/data/www cache-clear drush"
						+ " && sudo -E /media/data/drush/drush -y -r /media/data/www civicrm-install --dbname=civicrm --dbuser=civicrm --dbpass=${CIVICRM_PASSWORD} --dbhost=localhost:3306 --tarfile=/media/data/www/sites/all/modules/civi.tar.gz --destination=sites/all/modules"
						+ " && sudo rm -R ~/.drush/civicrm"
						+ " && sudo -E /media/data/drush/drush -r /media/data/www pm-enable civicrm"
						+ " && sudo rm /media/data/www/sites/all/modules/civi.tar"
						+ " && sudo /media/data/drush/drush -r /media/data/www cache-clear drush",
				"sudo /media/data/drush/drush -r /media/data/www pm-info civicrm 2>&1 | grep 'Status' | awk '{print $3}'",
				"enabled", "pass",
				"Couldn't fully install CiviCRM. Depending on the error message above, this could be OK, or could mean Civi is left in a broken state."));

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws InvalidServerModelException, InvalidPortException {
		final Collection<IUnit> units = new ArrayList<>();

		getNetworkModel().getServerModel(getLabel()).addEgress("download.civicrm.org");
		getNetworkModel().getServerModel(getLabel()).addEgress("latest.civicrm.org");
		getNetworkModel().getServerModel(getLabel()).addEgress("www.civicrm.org");
		getNetworkModel().getServerModel(getLabel()).addEgress("storage.googleapis.com");

		units.addAll(this.db.getPersistentFirewall());
		units.addAll(this.drupal.getPersistentFirewall());

		return units;
	}

}