/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.core.exec.network;

import org.privacyinternational.thornsec.core.exception.data.ADataException;
import org.privacyinternational.thornsec.core.exception.runtime.ARuntimeException;
import org.privacyinternational.thornsec.core.model.machine.ServerModel;
import org.privacyinternational.thornsec.core.profile.AProfile;

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
