/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine;

import java.util.Collection;

import core.data.machine.ADeviceData;
import core.exception.AThornSecException;
import core.exception.data.InvalidPortException;
import core.iface.IUnit;
import core.model.network.NetworkModel;

/**
 * This model represents a device on our network.
 *
 * A device is something which is managed by ThornSec, but is not directly
 * configured. For instance, users.
 */
abstract public class ADeviceModel extends AMachineModel {
	private Boolean managed;

	public ADeviceModel(ADeviceData myData, NetworkModel networkModel)
			throws AThornSecException  {
		super(myData, networkModel);

		this.managed = myData.isManaged().orElse(false);
	}

	final public Boolean isManaged() {
		return this.managed;
	}

	final protected void setIsManaged(Boolean managed) {
		this.managed = managed;
	}

	final public Boolean hasRealNICs() {
		return getNetworkInterfaces()
				.stream()
				.filter((nic) -> nic.getMac().isPresent())
				.count() > 0;
	}

	protected abstract Collection<IUnit> getPersistentFirewall() throws InvalidPortException;

}
