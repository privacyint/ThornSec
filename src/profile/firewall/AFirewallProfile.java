/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.firewall;

import java.util.Collection;
import java.util.Map;
import core.data.machine.AMachineData.MachineType;
import core.exception.AThornSecException;
import core.exception.runtime.ARuntimeException;
import core.iface.IUnit;
import core.model.machine.ServerModel;
import core.profile.AStructuredProfile;

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

	public final void addVLANs(Map<NetworkInterfaceModel, MachineType> vlans) {
		this.vlans = vlans;
	}
}
