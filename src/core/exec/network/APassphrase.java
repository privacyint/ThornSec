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
import core.model.machine.ServerModel;
import core.profile.AProfile;

/**
 * This class represents a Password of some kind.
 */
public abstract class APassphrase extends AProfile {

	private Boolean isADefaultPassphrase;

	public APassphrase(ServerModel me) {
		super(me);
		this.isADefaultPassphrase = null;
	}

	public abstract Boolean init();

	public abstract String getPassphrase() throws ARuntimeException, ADataException;

	protected abstract String generatePassphrase();

	public final Boolean isADefaultPassphrase() {
		return this.isADefaultPassphrase;
	}

	protected final void setIsADefaultPassphrase(Boolean isADefaultPassphrase) {
		this.isADefaultPassphrase = isADefaultPassphrase;
	}
}
