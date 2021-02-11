/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.core.iface;

import java.util.Collection;

import org.privacyinternational.thornsec.core.exception.AThornSecException;

/**
 * All Profiles must implement at least a method of getting their units
 */
public interface IProfile {

	Collection<IUnit> getUnits() throws AThornSecException;

}
