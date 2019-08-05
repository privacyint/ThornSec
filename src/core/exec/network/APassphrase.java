/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.exec.network;

import core.exception.data.ADataException;
import core.exception.runtime.ARuntimeException;
import core.model.network.NetworkModel;

/**
 * This class represents a Password of some kind.
 */
public abstract class APassphrase {

	private final String label;
	protected final NetworkModel networkModel;
	private Boolean isADefaultPassphrase;

	public APassphrase(String label, NetworkModel networkModel) {
		this.label = label;
		this.networkModel = networkModel;
		this.isADefaultPassphrase = null;
	}

	public abstract Boolean init();

	public abstract String getPassphrase() throws ARuntimeException, ADataException;

	protected abstract String generatePassphrase();

	public final NetworkModel getNetworkModel() {
		return this.networkModel;
	}

	public final String getLabel() {
		return this.label;
	}

	public final Boolean isADefaultPassphrase() {
		return this.isADefaultPassphrase;
	}

	protected final void setIsADefaultPassphrase(Boolean isADefaultPassphrase) {
		this.isADefaultPassphrase = isADefaultPassphrase;
	}
}
