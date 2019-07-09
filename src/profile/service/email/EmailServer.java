package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.model.ServerModel;
import core.profile.AStructuredProfile;

public class EmailServer extends AStructuredProfile {
	
	public EmailServer(ServerModel me, NetworkModel networkModel) {
		super("emailserver", me, networkModel);
	}

	public Vector<IUnit> getNetworking() {
		Vector<IUnit> units = new Vector<IUnit>();

		me.addRequiredListen(new Integer[] {25, 465, 993});

		me.addRequiredEgressDestination("spamassassin.apache.org");
		me.addRequiredEgressDestination("www.sa-update.pccc.com");
		
		return units;
	}
}
