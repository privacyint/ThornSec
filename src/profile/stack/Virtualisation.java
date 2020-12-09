/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.stack;

import java.util.Collection;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.InvalidMachineModelException;
import core.iface.IUnit;
import core.model.machine.HypervisorModel;
import core.model.machine.ServiceModel;
import core.profile.AStructuredProfile;

/**
 * This class represents a Virtualisation Layer of some kind.
 */
public abstract class Virtualisation extends AStructuredProfile {

	public Virtualisation(HypervisorModel machine) {
		super(machine);
	}

	/**
	 * Build and attach all disks relating to a given service
	 * @param service the service to build the disks for
	 * @return units to build & attach disks to our service
	 */
	protected abstract Collection<IUnit> buildDisks(ServiceModel service);

	/**
	 * Build and attach backup directory to a given service
	 * @param service the service to attach to
	 * @return units to build & attach backup directories
	 */
	protected abstract Collection<IUnit> buildBackups(ServiceModel service);

	/**
	 * Build and attach logs directory to a given service
	 * @param service the service to attach to
	 * @return units to build & attach logs directory
	 */
	protected abstract Collection<IUnit> buildLogs(ServiceModel service);

	/**
	 * Actually build our VM
	 * @param service The ServiceModel to build on our hypervisor
	 * @param bridge NIC to bridge our Service to
	 * @return
	 * @throws InvalidServerException
	 * @throws InvalidMachineModelException
	 */
	public abstract Collection<IUnit> buildServiceVm(ServiceModel service, String bridge);

	@Override
	public HypervisorModel getServerModel() {
		return (HypervisorModel) super.getServerModel();
	}
}
