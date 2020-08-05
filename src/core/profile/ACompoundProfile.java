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
import java.util.Set;

import core.iface.IChildUnit;
import core.iface.IUnit;

import core.model.machine.ServerModel;

import core.unit.ComplexUnit;

public abstract class ACompoundProfile extends AProfile {

	private final String precondition;
	private final String config;

	public ACompoundProfile(ServerModel me, String precondition, String config) {
		super(me);
		
		this.precondition = precondition;
		this.config = config;
	}

	@Override
	public Collection<IUnit> getUnits() {
		final Collection<IUnit> rules = new ArrayList<>();
		rules.add(new ComplexUnit(getLabel() + "_compound", this.precondition, "",
				getLabel() + "_unchanged=1;\n" + getLabel() + "_compound=1;\n"));
		rules.addAll(getChildren());
		rules.add(new ComplexUnit(getLabel(), this.precondition, this.config + "\n" + getLabel() + "_unchanged=1;\n",
				getLabel() + "=$" + getLabel() + "_unchanged;\n"));
		return rules;
	}

	public abstract Set<IChildUnit> getChildren();

}
