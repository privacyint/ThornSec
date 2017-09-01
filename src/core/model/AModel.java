package core.model;

public abstract class AModel {

	private String label;

	public AModel(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
	
	public abstract void init(NetworkModel model);

}
