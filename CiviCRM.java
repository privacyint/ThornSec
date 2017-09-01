package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.DirUnit;
import core.unit.pkg.InstalledUnit;

public class CiviCRM extends AStructuredProfile {
	
	Drupal drupal;
	MariaDB db;
	
	public CiviCRM() {
		super("civicrm");
		
		this.drupal = new Drupal();
		this.db = new MariaDB();
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

		units.addElement(new DirUnit("drush_home", "drupal_installed", "~/.drush"));

		//This either grabs the preconfigured password out of the settings file, or creates a new, random, URL-Encoded one
		units.addElement(new SimpleUnit("civicrm_mysql_password", "proceed",
				"CIVICRM_PASSWORD=`grep \"define('CIVICRM_DSN', 'mysql://\" /media/data/www/sites/default/civicrm.settings.php 2>/dev/null | awk -F'[:@]' '{print $3}'`; [[ -z $CIVICRM_PASSWORD ]] && CIVICRM_PASSWORD=`openssl rand -hex 32`",
				"echo $CIVICRM_PASSWORD", "", "fail",
				"Couldn't set a password for CiviCRM's database user. The installation will fail."));
		
		units.addAll(db.createDb("civicrm", "civicrm", "SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, INDEX, ALTER, CREATE TEMPORARY TABLES, LOCK TABLES, TRIGGER, CREATE ROUTINE, ALTER ROUTINE, REFERENCES", "CIVICRM_PASSWORD"));
		
		units.addElement(new SimpleUnit("civicrm_installed", "drupal_installed",
				"sudo wget 'https://download.civicrm.org/civicrm-4.7.23-drupal.tar.gz' -O /media/data/www/sites/all/modules/civi.tar.gz"
				+ " && sudo tar -zxf /media/data/www/sites/all/modules/civi.tar.gz -C ~/.drush/ civicrm/drupal/drush"
				+ " && sudo -E /media/data/drush/drush -r /media/data/www cache-clear drush"
				+ " && sudo -E /media/data/drush/drush -y -r /media/data/www civicrm-install --dbname=civicrm --dbuser=civicrm --dbpass=${CIVICRM_PASSWORD} --dbhost=localhost:3306 --tarfile=/media/data/www/sites/all/modules/civi.tar.gz --destination=sites/all/modules"
				+ " && sudo rm -R ~/.drush/civicrm"
				+ " && sudo -E /media/data/drush/drush -r /media/data/www pm-enable civicrm"
				+ " && sudo rm /media/data/www/sites/all/modules/civi.tar"
				+ " && sudo /media/data/drush/drush -r /media/data/www cache-clear drush",
				"sudo /media/data/drush/drush -r /media/data/www pm-info civicrm 2>&1 | grep 'Status' | awk '{print $3}'", "enabled", "pass",
				"Couldn't fully install CiviCRM. Depending on the error message above, this could be OK, or could mean Civi is left in a broken state."));

		return units;
	}
	
	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(drupal.getPersistentFirewall(server, model));

		return units;
	}

}