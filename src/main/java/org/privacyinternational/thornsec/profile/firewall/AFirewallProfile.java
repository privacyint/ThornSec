/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.profile.firewall;

import java.util.Collection;
import org.privacyinternational.thornsec.core.exception.AThornSecException;
import org.privacyinternational.thornsec.core.exception.runtime.ARuntimeException;
import org.privacyinternational.thornsec.core.iface.IUnit;
import org.privacyinternational.thornsec.core.model.machine.ServerModel;
import org.privacyinternational.thornsec.core.profile.AStructuredProfile;

/**
 * This is a firewall of some type.
 *
 * Firewalls are quite involved, so you'll need to implement everything!
 */
public abstract class AFirewallProfile extends AStructuredProfile {

	public AFirewallProfile(ServerModel me) {
		super(me);
	}

	@Override
	public abstract Collection<IUnit> getInstalled() throws ARuntimeException;

	@Override
	public abstract Collection<IUnit> getPersistentConfig() throws ARuntimeException;

	@Override
	public abstract Collection<IUnit> getLiveConfig() throws ARuntimeException;

	@Override
	public abstract Collection<IUnit> getPersistentFirewall() throws AThornSecException;

	@Override
	public abstract Collection<IUnit> getLiveFirewall() throws ARuntimeException;

}
