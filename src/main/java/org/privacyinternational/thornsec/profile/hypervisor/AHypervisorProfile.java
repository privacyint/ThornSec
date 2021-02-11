/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.profile.hypervisor;

import java.util.Collection;
import org.privacyinternational.thornsec.core.exception.runtime.InvalidMachineModelException;
import org.privacyinternational.thornsec.core.iface.IUnit;
import org.privacyinternational.thornsec.core.model.machine.HypervisorModel;
import org.privacyinternational.thornsec.core.model.machine.ServiceModel;
import org.privacyinternational.thornsec.core.profile.AStructuredProfile;

public abstract class AHypervisorProfile extends AStructuredProfile {

	public AHypervisorProfile(HypervisorModel me) {
		super(me);
	}

	protected abstract void buildDisks();

	protected abstract void buildBackups();

	protected abstract void buildVMs() throws InvalidMachineModelException;

	/**
	 * Return this machine as a HypervisorModel 
	 */
	@Override
	public HypervisorModel getServerModel() {
		return (HypervisorModel) getMachineModel();
	}

	public abstract Collection<IUnit> buildVM(ServiceModel service);
}
