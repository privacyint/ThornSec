/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.dns;

import java.util.Collection;

import core.model.machine.AMachineModel;
import core.model.machine.ServerModel;

import core.profile.AStructuredProfile;

/**
 * This is a DNS Server of some type.
 *
 * DNS Servers are quite involved, so you'll need to implement everything!
 */
public abstract class ADNSServerProfile extends AStructuredProfile {

	protected static Integer DEFAULT_LISTEN_PORT = 53;

	public ADNSServerProfile(ServerModel me) {
		super(me);
	}

	/**
	 * add a machine(s) to a given domain
	 */
	public abstract void addRecord(AMachineModel... machine);

	/**
	 * Add the required DNS records for a Collection of Machines
	 * @param machines The machines to build DNS records for
	 */
	public final void addRecord(Collection<AMachineModel> machines) {
		machines.forEach(machine -> addRecord(machine));
	}
}
