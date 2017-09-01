package core.profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;

public abstract class AStructuredProfile extends AProfile {

	protected AStructuredProfile(String name) {
		super(name);
	}

	public Vector<IUnit> getUnits(String server, NetworkModel model) {
		Vector<IUnit> children = new Vector<IUnit>();
		children.addAll(this.getInstalled(server, model));
		children.addAll(this.getPersistentConfig(server, model));
		children.addAll(this.getPersistentFirewall(server, model));
		children.addAll(this.getLiveConfig(server, model));
		children.addAll(this.getLiveFirewall(server, model));
		return children;
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		return new Vector<IUnit>();
	}

	protected Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		return new Vector<IUnit>();
	}

	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		return new Vector<IUnit>();
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		return new Vector<IUnit>();
	}

	protected Vector<IUnit> getLiveFirewall(String server, NetworkModel model) {
		return new Vector<IUnit>();
	}

}
