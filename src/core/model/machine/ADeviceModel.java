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
import core.data.machine.AMachineData.MachineType;
import core.exception.AThornSecException;
import core.exception.data.InvalidPortException;
import core.iface.IUnit;
import core.model.network.NetworkModel;
import profile.type.Dedicated;
import profile.type.Device;
import profile.type.ExternalOnly;
import profile.type.Hypervisor;
import profile.type.InternalOnly;
import profile.type.Router;
import profile.type.Server;
import profile.type.Service;
import profile.type.User;

/**
 * This model represents a device on our network.
 *
 * A device is something which is managed by ThornSec, but is not directly
 * configured. For instance, printers, or user machines.
 */
abstract public class ADeviceModel extends AMachineModel {
	private Boolean managed;

	public ADeviceModel(ADeviceData myData, NetworkModel networkModel)
			throws AThornSecException  {
		super(myData, networkModel);

		this.managed = myData.isManaged().orElse(false);

		this.addTypes();
	}
	
	private void addTypes() throws AThornSecException {
		this.addType(MachineType.DEVICE, new Device(this));
		
		for (final MachineType type : ((ADeviceData)getData()).getTypes()) {
			switch (type) {
				case INTERNAL_ONLY:
					addType(type, new InternalOnly((InternalOnlyDeviceModel) this));
					break;
				case EXTERNAL_ONLY:
					addType(type, new ExternalOnly((ExternalOnlyDeviceModel) this));
					break;
				case USER:
					addType(type, new User((UserDeviceModel) this));
					break;
				default:
			}
		}		
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
