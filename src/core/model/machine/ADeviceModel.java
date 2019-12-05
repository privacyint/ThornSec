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

import javax.json.stream.JsonParsingException;
import javax.mail.internet.AddressException;

import core.exception.AThornSecException;
import core.exception.data.ADataException;
import core.exception.data.machine.InvalidMachineException;
import core.exception.runtime.InvalidDeviceModelException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.network.NetworkModel;
import profile.type.Device;

/**
 * This model represents a device on our network.
 *
 * A device is something which is managed by ThornSec, but is not directly
 * configured. For instance, users.
 */
abstract public class ADeviceModel extends AMachineModel {
	private Boolean managed;
	@SuppressWarnings("unused")
	private final Device me;

	public ADeviceModel(String label, NetworkModel networkModel)
			throws AddressException, JsonParsingException, ADataException, IOException, InvalidServerModelException, InvalidDeviceModelException {
		super(label, networkModel);

		this.managed = getNetworkModel().getData().isManaged(getLabel());
		this.me = new Device(getLabel(), networkModel);
	}

	final public Boolean isManaged() throws InvalidMachineException {
		return this.managed;
	}

	final protected void setIsManaged(Boolean managed) {
		this.managed = managed;
	}

	@Override
	public Collection<IUnit> getUnits() throws AThornSecException {
		return this.me.getPersistentConfig();
	}

	protected abstract Collection<IUnit> getPersistentFirewall();

//	public Collection<IUnit> getPersistentConfig() throws InvalidDeviceModelException, JsonParsingException, ADataException {
//	}
}
