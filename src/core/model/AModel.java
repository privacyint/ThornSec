/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model;

import core.exception.AThornSecException;
import core.model.network.NetworkModel;

/**
 * This class represents a Model of some type.
 *
 * A Model is what we build from Data objects.
 */
public abstract class AModel {

	protected String label;
	protected NetworkModel networkModel;

	protected AModel(String label, NetworkModel networkModel) {
		this.label = label;
		this.networkModel = networkModel;
	}

	public final String getLabel() {
		return this.label;
	}

	public final NetworkModel getNetworkModel() {
		return this.networkModel;
	}

	public void init() throws AThornSecException {
		/* stub */
	}

}
