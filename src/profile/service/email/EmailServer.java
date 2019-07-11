/*
 * This code is part of the ThornSec project.
 * 
 * To learn more, please head to its GitHub repo: @privacyint
 * 
 * Pull requests encouraged.
 */
package profile.service.email;

import java.util.HashSet;
import java.util.Set;

import core.iface.IUnit;
import core.model.network.NetworkModel;
import core.data.machine.AMachineData.Encapsulation;

import core.profile.AStructuredProfile;

import profile.stack.MariaDB;
import profile.stack.Nginx;
import profile.stack.PHP;

import core.exception.data.InvalidPortException;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.InvalidServerModelException;


/**
 * This profile is, in effect, a stub for now.
 * 
 * It handles firewall stuff, but other than that, you're on your own.
 */
public class EmailServer extends AStructuredProfile {
	
	private Nginx   webserver;
	private PHP     php;
	private MariaDB db;
	
	public EmailServer(String label, NetworkModel networkModel) {
		super(label, networkModel);
		
		webserver = new Nginx(getLabel(), networkModel);
 		php       = new PHP(getLabel(), networkModel);
		db        = new MariaDB(getLabel(), networkModel);
	}

	protected Set<IUnit> getInstalled()
	throws InvalidServerModelException {
		Set<IUnit> units = new HashSet<IUnit>();
		
		units.addAll(webserver.getInstalled());
		units.addAll(php.getInstalled());
		units.addAll(db.getInstalled());
		
		return units;
	}
	
	protected Set<IUnit> getPersistentConfig()
	throws InvalidServerException, InvalidServerModelException {
		Set<IUnit> units =  new HashSet<IUnit>();
		
		units.addAll(webserver.getPersistentConfig());
		units.addAll(php.getPersistentConfig());
		units.addAll(db.getPersistentConfig());
		
		return units;
	}

	protected Set<IUnit> getLiveConfig()
	throws InvalidServerModelException {
		Set<IUnit> units = new HashSet<IUnit>();
		
		units.addAll(webserver.getLiveConfig());
		units.addAll(php.getLiveConfig());
		units.addAll(db.getLiveConfig());
		
		return units;
	}
	
	public Set<IUnit> getPersistentFirewall()
	throws InvalidServerModelException, InvalidPortException {
		Set<IUnit> units = new HashSet<IUnit>();

		units.addAll(webserver.getPersistentFirewall());

		networkModel.getServerModel(getLabel()).addListen(Encapsulation.TCP, 25, 465, 993);
		networkModel.getServerModel(getLabel()).addEgress("spamassassin.apache.org");
		networkModel.getServerModel(getLabel()).addEgress("www.sa-update.pccc.com");
		
		return units;
	}
}
