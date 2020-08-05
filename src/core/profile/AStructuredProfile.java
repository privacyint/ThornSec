/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.profile;

import java.util.ArrayList;
import java.util.Collection;
import core.exception.AThornSecException;
import core.iface.IUnit;
import core.model.machine.AMachineModel;
import core.model.machine.ServerModel;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.IncompatibleAddressException;

/**
 * This represents a fully-fledged profile, designed to hold the end-to-end
 * configuration steps for something.
 *
 * Profiles are intended to "outsource" tasks to specialised Units or Profiles -
 * Profiles, really, are aimed at holding _configuration_ rather than anything
 * else.
 *
 * If you find yourself doing something more than once, think about whether you
 * should really be building a Unit for it.
 */
public abstract class AStructuredProfile extends AProfile {

	protected AStructuredProfile(AMachineModel me) {
		super(me);
	}

	/**
	 * Returns a Set of all units related to this profile.
	 */
	@Override
	public Collection<IUnit> getUnits() throws AThornSecException {
		final Collection<IUnit> children = new ArrayList<>();

		children.addAll(getPersistentFirewall());
		children.addAll(getLiveFirewall());
		children.addAll(getInstalled());
		children.addAll(getPersistentConfig());
		children.addAll(getLiveConfig());

		return children;
	}

	/**
	 * This is units relating to installing software for this profile
	 */
	protected Collection<IUnit> getInstalled() throws AThornSecException {
		return new ArrayList<>();
	}

	/**
	 * Expecting the configuration to change regularly? Put it in
	 * {@link #getLiveConfig()}!
	 *
	 * @throws AThornSecException
	 */
	public Collection<IUnit> getPersistentConfig() throws AThornSecException {
		return new ArrayList<>();
	}

	/**
	 * This is Units for configurations you expect to change regularly, e.g. Devices
	 * or Servers or similar.
	 *
	 * @throws IncompatibleAddressException
	 * @throws AddressStringException
	 */
	public Collection<IUnit> getLiveConfig() throws AThornSecException {
		return new ArrayList<>();
	}

	/**
	 * You almost certainly mean {@link #getLiveFirewall}.
	 *
	 * Please put Units related to the persistent configuration of the firewall
	 * (e.g. adding a Device) here.
	 */
	public Collection<IUnit> getPersistentFirewall() throws AThornSecException {
		return new ArrayList<>();
	}

	/**
	 * This is probably the firewall method you're looking for.
	 *
	 * This is for operations which only require a reload of the rules such as
	 * ingress, egress, opening ports, or etc.
	 *
	 * If you want to do configuration of the firewall (e.g. add a Device) please
	 * see {@link #getPersistentFirewall()}
	 */
	protected Collection<IUnit> getLiveFirewall() throws AThornSecException {
		return new ArrayList<>();
	}
	
	/**
	 * Get the ServerModel on which this profile resides
	 * @return
	 */
	public ServerModel getServerModel() {
		return (ServerModel)getMachineModel();
	}
}
