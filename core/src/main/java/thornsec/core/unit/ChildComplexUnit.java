package core.unit;

public class ChildComplexUnit extends ComplexUnit {

	protected String parent;

	public ChildComplexUnit(String parent, String name, String precondition, String config, String audit) {
		super(name, precondition, config + parent + "_unchanged=0;\n", audit);
		this.parent = parent;
	}

	protected String getDryRun() {
		return "\t" + getParent() + "_unchanged=0;\n";
	}

	protected String getConfig() {
		return config + getParent() + "_unchanged=0;\n";
	}

	public String getParent() {
		return parent;
	}
}