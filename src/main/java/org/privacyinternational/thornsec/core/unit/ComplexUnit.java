package org.privacyinternational.thornsec.core.unit;

public class ComplexUnit extends AUnit {

	public ComplexUnit(String label, String precondition, String config, String audit) {
		super(label, precondition, config, audit);
	}

	public ComplexUnit(String label, String precondition, String config, String audit, String message) {
		super(label, precondition, config, audit, message);
	}

	protected String getAudit() {
		return this.audit;
	}

	protected String getPrecondition() {
		return precondition;
	}

	protected String getConfig() {
		return config;
	}

	protected String getDryRun() {
		return "";
	}

	protected String getMessage() {
		return message;
	}

}
