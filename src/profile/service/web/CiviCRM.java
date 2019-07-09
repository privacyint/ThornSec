package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.model.ServerModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.DirUnit;
import core.unit.pkg.InstalledUnit;

public class CiviCRM extends AStructuredProfile {
	
	private Drupal drupal;
	private MariaDB db;
	
	public CiviCRM(ServerModel me, NetworkModel networkModel) {
		super("civicrm", me, networkModel);
		
		this.drupal = new Drupal(me, networkModel);
		this.db     = new MariaDB(me, networkModel);
		
		this.db.setUsername("civicrm");
		this.db.setUserPrivileges("SUPER");
		this.db.setUserPassword("${CIVICRM_PASSWORD}");
		this.db.setDb("civicrm");
	}

	protected Vector<IUnit> getInstalled() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(drupal.getInstalled());
		
		units.add(new InstalledUnit("php_imagemagick", "php_fpm_installed", "php-imagick"));
		units.add(new InstalledUnit("php_mcrypt", "php_fpm_installed", "php-mcrypt"));
		
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

		units.addElement(new DirUnit("drush_home", "drupal_installed", "~/.drush"));

		//This either grabs the preconfigured password out of the settings file, or creates a new, random, URL-Encoded one
		units.addElement(new SimpleUnit("civicrm_mysql_password", "proceed",
				"CIVICRM_PASSWORD=`grep \"define('CIVICRM_DSN', 'mysql://\" /media/data/www/sites/default/civicrm.settings.php 2>/dev/null | awk -F'[:@]' '{print $3}'`; [[ -z $CIVICRM_PASSWORD ]] && CIVICRM_PASSWORD=`openssl rand -hex 32`",
				"echo $CIVICRM_PASSWORD", "", "fail",
				"Couldn't set a password for CiviCRM's database user. The installation will fail."));
		
		units.addAll(this.db.checkUserExists());
		units.addAll(this.db.checkDbExists());
		
		units.addElement(new SimpleUnit("civicrm_installed", "drupal_installed",
				"sudo wget 'https://download.civicrm.org/civicrm-4.7.27-drupal.tar.gz' -O /media/data/www/sites/all/modules/civi.tar.gz"
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
	
	public Vector<IUnit> getNetworking() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		me.addRequiredEgressDestination("download.civicrm.org");
		me.addRequiredEgressDestination("latest.civicrm.org");
		me.addRequiredEgressDestination("civicrm.org");
		me.addRequiredEgressDestination("storage.googleapis.com");
		
		units.addAll(drupal.getNetworking());

		return units;
	}

}