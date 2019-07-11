/*
 * This code is part of the ThornSec project.
 * 
 * To learn more, please head to its GitHub repo: @privacyint
 * 
 * Pull requests encouraged.
 */
package core.exec.network;

import core.model.machine.ServerModel;
import core.model.network.NetworkModel;
import core.exception.data.ADataException;
import core.exception.runtime.ARuntimeException;

/**
 * This class represents a Password of some kind.
 */
public abstract class APassphrase {

	private ServerModel  server;
	private NetworkModel networkModel;
	private Boolean      isADefaultPassphrase;

	public APassphrase(ServerModel server, NetworkModel networkModel) {
		this.server               = server;
		this.networkModel         = networkModel;
		this.isADefaultPassphrase = null;
	}

	public abstract Boolean init() ;
	
	public abstract String getPassphrase() throws ARuntimeException, ADataException;

	protected abstract String generatePassphrase();
		
	/**
	 * If you're going to be generating passwords, you'd better
	 * know something about the server you're generating for.
	 * 
	 * Use it as a seed, or to set a default salt or something.
	 */
	protected final ServerModel getServer() {
		return this.server;
	}
	
	protected final NetworkModel getNetworkModel() {
		return this.networkModel;
	}
	
	public final Boolean isADefaultPassphrase() {
		return this.isADefaultPassphrase;
	}

	protected final void setIsADefaultPassphrase(Boolean isADefaultPassphrase) {
		this.isADefaultPassphrase = isADefaultPassphrase;
	}
}
