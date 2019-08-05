/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.profile;

import java.util.HashSet;
import java.util.Set;

import core.exception.AThornSecException;
import core.iface.IUnit;
import core.model.network.NetworkModel;

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

	protected AStructuredProfile(String name, NetworkModel networkModel) {
		super(name, networkModel);
	}

	/**
	 * Returns a Set of all units related to this profile.
	 */
	@Override
	public Set<IUnit> getUnits() throws AThornSecException {
		final Set<IUnit> children = new HashSet<>();

		children.addAll(getInstalled());
		children.addAll(getPersistentConfig());
		children.addAll(getLiveConfig());
		children.addAll(getPersistentFirewall());
		children.addAll(getLiveFirewall());

		return children;
	}

	/**
	 * This is units relating to installing software for this profile
	 */
	protected Set<IUnit> getInstalled() throws AThornSecException {
		return new HashSet<>();
	}

	/**
	 * Expecting the configuration to change regularly? Put it in
	 * {@link #getLiveConfig()}!
	 *
	 * @throws AThornSecException
	 */
	protected Set<IUnit> getPersistentConfig() throws AThornSecException {
		return new HashSet<>();
	}

	/**
	 * This is Units for configurations you expect to change regularly, e.g. Devices
	 * or Servers or similar.
	 */
	protected Set<IUnit> getLiveConfig() throws AThornSecException {
		return new HashSet<>();
	}

	/**
	 * You almost certainly mean {@link #getLiveFirewall}.
	 *
	 * Please put Units related to the persistent configuration of the firewall
	 * (e.g. adding a Device) here.
	 */
	protected Set<IUnit> getPersistentFirewall() throws AThornSecException {
		return new HashSet<>();
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
	protected Set<IUnit> getLiveFirewall() throws AThornSecException {
		return new HashSet<>();
	}

	/**
	 * This is where you put any initialisation Units you require. Please don't
	 * actually use this method, it will be going soon
	 */
	@Deprecated
	public Set<IUnit> init() {
		return new HashSet<>();
	}
}
