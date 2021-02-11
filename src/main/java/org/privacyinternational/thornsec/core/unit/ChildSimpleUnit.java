package org.privacyinternational.thornsec.core.unit;

public class ChildSimpleUnit extends SimpleUnit {

	protected String parent;

	public ChildSimpleUnit(String parent, String name, String precondition, String config, String audit, String test,
			String result) {
		super(name, precondition, config + parent + "_unchanged=0;\n", audit, test, result);
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