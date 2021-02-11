/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.core.model.machine;

import java.util.Collection;

import org.privacyinternational.thornsec.core.data.machine.ADeviceData;
import org.privacyinternational.thornsec.core.data.machine.AMachineData.MachineType;
import org.privacyinternational.thornsec.core.exception.AThornSecException;
import org.privacyinternational.thornsec.core.exception.data.InvalidPortException;
import org.privacyinternational.thornsec.core.iface.IUnit;
import org.privacyinternational.thornsec.core.model.network.NetworkModel;
import org.privacyinternational.thornsec.profile.type.Device;
import org.privacyinternational.thornsec.profile.type.ExternalOnly;
import org.privacyinternational.thornsec.profile.type.InternalOnly;
import org.privacyinternational.thornsec.profile.type.User;

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
