/*
 * This code is part of the ThornSec project.
 * 
 * To learn more, please head to its GitHub repo: @privacyint
 * 
 * Pull requests encouraged.
 */
package core.unit;

public class SimpleUnit extends ComplexUnit {

	protected String test;
	protected String result;

	/**
	 * This is a unit test, with a default fail message.
	 * You should try not to invoke this directly, instead using specific unit tests
	 * @param name         Unit test name
	 * @param precondition Precondition unit test
	 * @param config       Configuration commandss
	 * @param audit        Audit string
	 * @param test         Test string
	 * @param result       "pass/fail"
	 */
	public SimpleUnit(String name, String precondition, String config, String audit, String test, String result) {
		super(name, precondition, config, audit);
		this.test = test;
		this.result = result;
	}
	
	/**
	 * This is a unit test, with a custom fail message.
	 * You should try not to invoke this directly, instead using specific unit tests
	 * @param name         Unit test name
	 * @param precondition Precondition unit test
	 * @param config       Configuration commands
	 * @param audit        Audit string
	 * @param test         Test string
	 * @param result       "pass/fail"
	 * @param message      Custom fail message
	 */
	public SimpleUnit(String name, String precondition, String config, String audit, String test, String result, String message) {
		super(name, precondition, config, audit, message);
		this.test = test;
		this.result = result;
	}

	@Override
	protected final String getAudit() {
		String operator = (getResult().equals("fail")) ? "!=" : "=" ;

		String auditString = "";
		auditString += getLabel() + "_expected=\"" + getTest() + "\";\n";		
		auditString += "\n";
		auditString += getLabel() + "_audit() {\n";
		auditString += "\t" +getLabel() + "_actual=$(" + super.getAudit() + ");\n";
		auditString += "\n";
		auditString += "\tif [ \"${" +getLabel() + "_expected}\" "+operator+" \"${"+getLabel() + "_actual}\" ] ; then\n";
		auditString += "\t\treturn 0\n";
		auditString += "\telse\n";
		auditString += "\t\treturn 1\n";
		auditString += "\tfi ;\n";
		auditString += "}\n";

		return auditString;
	}

	protected final String getTest() {
		return this.test;
	}

	protected final String getResult() {
		return this.result;
	}
}
