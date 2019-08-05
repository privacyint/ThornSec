/*
 * This code is part of the ThornSec project.
 * 
 * To learn more, please head to its GitHub repo: @privacyint
 * 
 * Pull requests encouraged.
 */
package profile.dhcp;

import java.util.Set;

import core.iface.IUnit;

import core.model.network.NetworkModel;

import core.profile.AStructuredProfile;
import core.exception.AThornSecException;

/**
 * This is a DHCP server of some type.
 * 
 * DHCP servers are quite involved, so you'll need to implement everything!
 */
public abstract class ADHCPServerProfile extends AStructuredProfile {

	public ADHCPServerProfile(String label, NetworkModel networkModel) {
		super(label, networkModel);
	}

	@Override
	public abstract Set<IUnit> getInstalled() throws AThornSecException;

	@Override
	public abstract Set<IUnit> getPersistentConfig() throws AThornSecException;

	@Override
	public abstract Set<IUnit> getLiveConfig() throws AThornSecException;

	@Override
	public abstract Set<IUnit> getPersistentFirewall() throws AThornSecException;

	@Override
	public abstract Set<IUnit> getLiveFirewall() throws AThornSecException;
}
