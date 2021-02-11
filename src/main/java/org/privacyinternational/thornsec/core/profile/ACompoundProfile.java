/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.core.profile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.privacyinternational.thornsec.core.iface.IChildUnit;
import org.privacyinternational.thornsec.core.iface.IUnit;

import org.privacyinternational.thornsec.core.model.machine.ServerModel;

import org.privacyinternational.thornsec.core.unit.ComplexUnit;

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
		rules.add(new ComplexUnit(getMachineModel().getLabel() + "_compound",
						this.precondition,
						"",
						getMachineModel().getLabel() + "_unchanged=1;\n" + getMachineModel().getLabel() + "_compound=1;\n")
		);
		
		rules.addAll(getChildren());
		
		rules.add(new ComplexUnit(getMachineModel().getLabel(),
						this.precondition,
						this.config + "\n" + getMachineModel().getLabel() + "_unchanged=1;\n",
						getMachineModel().getLabel() + "=$" + getMachineModel().getLabel() + "_unchanged;\n")
		);
		
		return rules;
	}

	public abstract Set<IChildUnit> getChildren();

}
