package profile.type;

import core.model.machine.AMachineModel;
import core.profile.AStructuredProfile;

/**
 * This class represents configurations on a Machine on your network
 */
public abstract class AMachine extends AStructuredProfile {
	
	public AMachine(AMachineModel me) {
		super(me);
	}
}
