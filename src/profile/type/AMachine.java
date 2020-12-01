package profile.type;

import java.util.ArrayList;
import java.util.Collection;
import core.exception.AThornSecException;
import core.iface.IUnit;
import core.model.machine.AMachineModel;
import core.model.machine.configuration.networking.NetworkInterfaceModel;
import core.profile.AStructuredProfile;

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
