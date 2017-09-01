package core.iface;

import java.util.Vector;

import core.model.NetworkModel;

public interface IProfile {

	public String getLabel();

	public Vector<IUnit> getUnits(String server, NetworkModel model);

}