package core.exec.network;

import de.slackspace.openkeepass.KeePassDatabase;

import core.model.machine.ServerModel;
import core.model.network.NetworkModel;

public final class OpenKeePassPassphrase extends APassphrase {

	private KeePassDatabase db;
	
	public OpenKeePassPassphrase(ServerModel server, NetworkModel networkModel) {
		super(server, networkModel);
		
		this.db = null;
	}

	@Override
	public Boolean init() {
		this.db = KeePassDatabase.getInstance(this.getNetworkModel().getKeePassDBPath());
		this.db.openDatabase(this.getNetworkModel().getKeePassDBPassphrase());
		
		return true;
	}

	@Override
	public String getPassphrase() {
		//this.db.;
		return null;
	}

	@Override
	protected String generatePassphrase() {
		// TODO Auto-generated method stub
		return null;
	}

}
