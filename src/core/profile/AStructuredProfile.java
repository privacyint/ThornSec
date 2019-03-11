package core.profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.MachineModel;
import core.model.NetworkModel;

public abstract class AStructuredProfile extends AProfile {

	protected AStructuredProfile(String name, MachineModel me, NetworkModel networkModel) {
		super(name, me, networkModel);
	}

	public Vector<IUnit> getUnits() {
		Vector<IUnit> children = new Vector<IUnit>();
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
