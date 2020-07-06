/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.dns;

import java.util.Collection;

import core.exception.AThornSecException;

import core.iface.IUnit;

import core.model.machine.AMachineModel;
import core.model.machine.ServerModel;

import core.profile.AStructuredProfile;

/**
 * This is a DNS Server of some type.
 *
 * DNS Servers are quite involved, so you'll need to implement everything!
 */
public abstract class ADNSServerProfile extends AStructuredProfile {

	public ADNSServerProfile(ServerModel me) {
		super(me);
	}

	@Override
	public abstract Collection<IUnit> getInstalled() throws AThornSecException;

	@Override
	public abstract Collection<IUnit> getPersistentConfig() throws AThornSecException;

	@Override
	public abstract Collection<IUnit> getLiveConfig() throws AThornSecException;

	@Override
	public abstract Collection<IUnit> getPersistentFirewall() throws AThornSecException;

	@Override
	public abstract Collection<IUnit> getLiveFirewall() throws AThornSecException;

	/**
	 * add a machine(s) to a given domain
	 */
	public abstract void addRecord(AMachineModel... machine);
}
