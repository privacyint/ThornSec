/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.hypervisor;

import java.util.Collection;
import core.exception.runtime.InvalidMachineModelException;
import core.iface.IUnit;
import core.model.machine.HypervisorModel;
import core.model.machine.ServiceModel;
import core.profile.AStructuredProfile;

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
