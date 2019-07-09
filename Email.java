package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.model.ServerModel;
import core.profile.AStructuredProfile;

public class Email extends AStructuredProfile {
	
	private Nginx   webserver;
	private PHP     php;
	private MariaDB db;
	
	public Email(ServerModel me, NetworkModel networkModel) {
		super("email", me, networkModel);
		
		webserver = new Nginx(me, networkModel);
 		php       = new PHP(me, networkModel);
		db        = new MariaDB(me, networkModel);
	}

	protected Vector<IUnit> getInstalled() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(webserver.getInstalled());
		units.addAll(php.getInstalled());
		units.addAll(db.getInstalled());
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig() {
		Vector<IUnit> units =  new Vector<IUnit>();
		
		units.addAll(webserver.getPersistentConfig());
		units.addAll(php.getPersistentConfig());
		units.addAll(db.getPersistentConfig());
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(webserver.getLiveConfig());
		units.addAll(php.getLiveConfig());
		units.addAll(db.getLiveConfig());
		
		return units;
	}
	
	public Vector<IUnit> getNetworking() {
		Vector<IUnit> units = new Vector<IUnit>();

		units.addAll(webserver.getNetworking());

		me.addRequiredListen(new Integer[] {25, 465, 993});
		
		return units;
	}
}
