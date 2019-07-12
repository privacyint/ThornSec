package core.profile;

import java.util.HashSet;
import java.util.Set;

import core.iface.IChildUnit;
import core.iface.IUnit;
import core.model.NetworkModel;
import core.unit.ComplexUnit;

public abstract class ACompoundProfile extends AProfile {

	private String precondition;
	private String config;

	public ACompoundProfile(String label, NetworkModel model, String precondition, String config) {
		super(label, model);
		this.precondition = precondition;
		this.config = config;
	}

	public Set<IUnit> getUnits() {
		Set<IUnit> rules = new HashSet<IUnit>();
		rules.add(new ComplexUnit(getLabel() + "_compound", precondition, "",
				getLabel() + "_unchanged=1;\n" + getLabel() + "_compound=1;\n"));
		rules.addAll(this.getChildren());
		rules.add(new ComplexUnit(getLabel(), precondition, config + "\n" + getLabel() + "_unchanged=1;\n",
				getLabel() + "=$" + getLabel() + "_unchanged;\n"));
		return rules;
	}

	public abstract Set<IChildUnit> getChildren();
}
