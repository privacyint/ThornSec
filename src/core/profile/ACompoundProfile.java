package core.profile;

import java.util.Vector;

import core.iface.IChildUnit;
import core.iface.IUnit;
import core.model.NetworkModel;
import core.model.ServerModel;
import core.unit.ComplexUnit;

public abstract class ACompoundProfile extends AProfile {

	private String precondition;
	private String config;

	public ACompoundProfile(String name, ServerModel me, NetworkModel model, String precondition, String config) {
		super(name, me, model);
		this.precondition = precondition;
		this.config = config;
	}

	public Vector<IUnit> getUnits() {
		Vector<IUnit> rules = new Vector<IUnit>();
		rules.add(new ComplexUnit(getLabel() + "_compound", precondition, "",
				getLabel() + "_unchanged=1;\n" + getLabel() + "_compound=1;\n"));
		rules.addAll(this.getChildren());
		rules.add(new ComplexUnit(getLabel(), precondition, config + "\n" + getLabel() + "_unchanged=1;\n",
				getLabel() + "=$" + getLabel() + "_unchanged;\n"));
		return rules;
	}

	public abstract Vector<IChildUnit> getChildren();

}
