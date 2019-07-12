package core.profile;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import core.iface.IUnit;
import core.model.MachineModel;
import core.model.NetworkModel;

public abstract class AStructuredProfile extends AProfile {

	protected AStructuredProfile(String label, NetworkModel networkModel) {
		super(label, networkModel);
	}

	public Set<IUnit> getUnits() {
		Set<IUnit> children = new HashSet<IUnit>();
		children.addAll(this.getInstalled());
		children.addAll(this.getPersistentConfig());
		children.addAll(this.getLiveConfig());
		children.addAll(this.getLiveFirewall());
		return children;
	}

	protected Vector<IUnit> getInstalled() {
		return new Vector<IUnit>();
	}

	protected Vector<IUnit> getPersistentConfig() {
		return new Vector<IUnit>();
	}

	protected Vector<IUnit> getLiveConfig() {
		return new Vector<IUnit>();
	}

	protected Vector<IUnit> getLiveFirewall() {
		return new Vector<IUnit>();
	}

	public Vector<IUnit> init() {
		return new Vector<IUnit>();
	}

	public Vector<IUnit> getNetworking() {
		return new Vector<IUnit>();
	}

}
