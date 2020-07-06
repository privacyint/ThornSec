/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.exec.network;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Collection;
import core.exception.AThornSecException;
import core.iface.IUnit;
import core.model.machine.ServerModel;
import de.slackspace.openkeepass.KeePassDatabase;

public final class OpenKeePassPassphrase extends APassphrase {

	private KeePassDatabase db;

	public OpenKeePassPassphrase(ServerModel me) {
		super(me);

		this.db = null;
	}

	@Override
	public Boolean init() {
		try {
			final File keypassDB = new File(getNetworkModel().getKeePassDBPath(getMachineModel().getLabel()));
			if (keypassDB.isFile()) {
				this.db = KeePassDatabase.getInstance(keypassDB);
				this.db.openDatabase("IAmAString");

				return true;
			}
		} catch (final URISyntaxException | IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public String getPassphrase() {
		// this.db.;
		return null;
	}

	@Override
	protected String generatePassphrase() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<IUnit> getUnits() throws AThornSecException {
		// TODO Auto-generated method stub
		return null;
	}
}
