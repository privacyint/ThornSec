package org.privacyinternational.thornsec.profile.type;

import java.util.ArrayList;
import java.util.Collection;
import org.privacyinternational.thornsec.core.exception.AThornSecException;
import org.privacyinternational.thornsec.core.iface.IUnit;
import org.privacyinternational.thornsec.core.model.machine.AMachineModel;
import org.privacyinternational.thornsec.core.model.machine.configuration.networking.NetworkInterfaceModel;
import org.privacyinternational.thornsec.core.profile.AStructuredProfile;

/**
 * This class represents configurations on a Machine on your network
 */
public abstract class AMachine extends AStructuredProfile {
	
	public AMachine(AMachineModel me) {
		super(me);
	}
	
	@Override
	public Collection<IUnit> getPersistentConfig() throws AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		for (NetworkInterfaceModel nic : getMachineModel().getNetworkInterfaces()) {
			nic.getNetworkFile().ifPresent(n -> units.add(n));
			nic.getNetDevFile().ifPresent(n -> units.add(n));
		}

		return units;
	}
}
