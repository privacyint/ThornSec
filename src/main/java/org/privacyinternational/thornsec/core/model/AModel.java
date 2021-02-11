/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.core.model;

import org.privacyinternational.thornsec.core.data.AData;
import org.privacyinternational.thornsec.core.exception.AThornSecException;
import org.privacyinternational.thornsec.core.model.network.NetworkModel;

/**
 * This class represents a Model of some type.
 *
 * A Model is what we build from Data objects.
 */
public abstract class AModel {

	private String label;
	private NetworkModel networkModel;
	private AData myData;

	/**
	 * Create a new AModel. This AModel represents a logical <i>thing</i>, such
	 * as a Device or a Server, on our Network. Its child classes should be
	 * ever-more specialised types of <i>things</i>.
	 * <p>
	 * AModel's job is to take AData and turn it into a working representation
	 * of that <i>thing</i> as an Object we can interact with.
	 * <p>
	 * As many of AData's methods only Optionally return, your AModel should
	 * handle this gracefully.
	 * 
	 * @param myData The AData object representing persistent configurations
	 * 		such as a JSON file
	 * @param networkModel The Network to which this Model belongs
	 */
	protected AModel(AData myData, NetworkModel networkModel) {
		this.setLabel(myData.getLabel());
		this.networkModel = networkModel;
		this.myData = myData;
	}

	/**
	 * Get this model's label, as set from its Data
	 * 
	 * @return the model's label, corresponding with its Data
	 */
	public final String getLabel() {
//		//assertNotNull(this.label);

		return this.label;
	}

	/**
	 * Get the NetworkModel in which this AModel resides.
	 * 
	 * @return this AModel's Network
	 */
	public final NetworkModel getNetworkModel() {
		////assertNotNull(this.networkModel);

		return this.networkModel;
	}

	/**
	 * Get this Model's AData. This may be useful in Profiles where you want to
	 * get at properties without having to refactor the whole Object
	 * 
	 * @return the Model's AData, representing its persistent configuration
	 */
	public AData getData() {
		////assertNotNull(this.myData);

		return this.myData;
	}

	/**
	 * Initialise this model. This is where logic which relies on, for example,
	 * other models existing on our Network should live.
	 * 
	 * @throws AThornSecException if something goes wrong.
	 */
	public abstract void init() throws AThornSecException;

	protected void setLabel(String label) {
		this.label = label;
	}
}
