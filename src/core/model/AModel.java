package core.model;

public abstract class AModel {

	protected String label;
	protected MachineModel me;
	protected NetworkModel networkModel;

	AModel(String label, MachineModel me, NetworkModel networkModel) {
		this.label        = label;
		this.me           = me;
		this.networkModel = networkModel;
	}
	
	public AModel(String label, NetworkModel networkModel) {
		this(label, null, networkModel);
	}

	public String getLabel() {
		return label;
	}

	public void init() { /* stub */	}
	
}
