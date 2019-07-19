/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import javax.json.stream.JsonParsingException;
import javax.mail.internet.AddressException;

import core.exception.AThornSecException;
import core.exception.data.ADataException;
import core.exception.data.machine.InvalidMachineException;
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

	public ADeviceModel(String label, NetworkModel networkModel)
			throws AddressException, JsonParsingException, ADataException, IOException {
		super(label, networkModel);

		this.managed = networkModel.getData().isManaged(getLabel());
	}

	final public Boolean isManaged() throws InvalidMachineException {
		return this.managed;
	}

	final protected void setIsManaged(Boolean managed) {
		this.managed = managed;
	}

	@Override
	protected Set<IUnit> getUnits() throws AThornSecException {
		return null;
	}

	protected abstract Collection<? extends IUnit> getPersistentFirewall();
}
