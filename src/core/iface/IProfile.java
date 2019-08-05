/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.iface;

import java.util.Set;

import core.exception.AThornSecException;

/**
 * All Profiles must implement at least a method of getting their label, and
 * units.
 */
public interface IProfile {

	String getLabel();

	Set<IUnit> getUnits() throws AThornSecException;

}
