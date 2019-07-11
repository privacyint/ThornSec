package core.model;

import core.model.network.NetworkModel;

import core.exception.runtime.ARuntimeException;

public abstract class AModel {

	protected String label;
	protected NetworkModel networkModel;

	protected AModel(String label, NetworkModel networkModel) {
		this.label        = label;
		this.networkModel = networkModel;
	}
	
	public final String getLabel() {
		return this.label;
	}

	public void init() throws ARuntimeException { /* stub */ }
	
}
