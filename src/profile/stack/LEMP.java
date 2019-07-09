/*
 * This code is part of the ThornSec project.
 * 
 * To learn more, please head to its GitHub repo: @privacyint
 * 
 * Pull requests encouraged.
 */
package profile.stack;

import java.util.HashSet;
import java.util.Set;

import core.exception.data.InvalidPortException;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.network.NetworkModel;

import core.profile.AStructuredProfile;

import profile.stack.MariaDB;
import profile.stack.Nginx;
import profile.stack.PHP;

/**
 * This is a premade "LEMP" server
 * i.e. MariaDB & PHP-FPM & Nginx.
 */
public class LEMP extends AStructuredProfile {
	
	private Nginx webserver;
	private PHP php;
	private MariaDB db;
	
	public LEMP(String label, NetworkModel networkModel) {
		super(label, networkModel);
		
		this.webserver = new Nginx(getLabel(), networkModel);
		this.php       = new PHP(getLabel(), networkModel);
		this.db        = new MariaDB(getLabel(), networkModel);
	}

	@Override
	public final Set<IUnit> getInstalled()
	throws InvalidServerModelException {
		Set<IUnit> units = new HashSet<IUnit>();
		
		units.addAll(webserver.getInstalled());
		units.addAll(php.getInstalled());
		units.addAll(db.getInstalled());
		
		return units;
	}
	
	@Override
	public final Set<IUnit> getPersistentConfig()
	throws InvalidServerException, InvalidServerModelException {
		Set<IUnit> units =  new HashSet<IUnit>();
		
		units.addAll(webserver.getPersistentConfig());
		units.addAll(db.getPersistentConfig());
		units.addAll(php.getPersistentConfig());
		
		return units;
	}
	
	@Override
	public final Set<IUnit> getLiveConfig()
	throws InvalidServerModelException {
		Set<IUnit> units =  new HashSet<IUnit>();
		
		units.addAll(webserver.getLiveConfig());
		units.addAll(db.getLiveConfig());
		units.addAll(php.getLiveConfig());
		
		return units;
	}
	
	@Override
	public final Set<IUnit> getPersistentFirewall()
	throws InvalidServerModelException, InvalidPortException {
		Set<IUnit> units = new HashSet<IUnit>();
		
		units.addAll(webserver.getPersistentFirewall());
		units.addAll(db.getPersistentFirewall());
		units.addAll(php.getPersistentFirewall());
		
		return units;
	}

	@Override
	public final Set<IUnit> getLiveFirewall()
	throws InvalidServerModelException, InvalidPortException {
		Set<IUnit> units = new HashSet<IUnit>();
		
		units.addAll(webserver.getLiveFirewall());
		units.addAll(db.getLiveFirewall());
		units.addAll(php.getLiveFirewall());
		
		return units;
	}

	public final Nginx getWebserver() {
		return this.webserver;
	}
	
	public final MariaDB getDB() {
		return this.db;
	}
	
	public final PHP getPHP() {
		return this.php;
	}
}
	