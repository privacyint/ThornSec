package core.profile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import core.iface.IChildUnit;
import core.iface.IUnit;
import core.model.network.NetworkModel;
import core.unit.ComplexUnit;

public abstract class ACompoundProfile extends AProfile {

	private String precondition;
	private String config;

	public ACompoundProfile(String name, NetworkModel model, String precondition, String config) {
		super(name, model);
		this.precondition = precondition;
		this.config = config;
	}

	@Override
	public Collection<IUnit> getUnits() {
		final Collection<IUnit> rules = new ArrayList<>();
				getLabel() + "_unchanged=1;\n" + getLabel() + "_compound=1;\n"));
		rules.addAll(this.getChildren());
		rules.add(new ComplexUnit(getLabel(), precondition, config + "\n" + getLabel() + "_unchanged=1;\n",
				getLabel() + "=$" + getLabel() + "_unchanged;\n"));
		return rules;
	}

	public abstract Set<IChildUnit> getChildren();

}
