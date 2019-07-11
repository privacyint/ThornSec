/*
 * This code is part of the ThornSec project.
 * 
 * To learn more, please head to its GitHub repo: @privacyint
 * 
 * Pull requests encouraged.
 */
package core.unit;

import java.util.regex.Pattern;

public class SimpleUnit extends ComplexUnit {

	protected String test;
	protected String result;
	protected String message;

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
		this.message = "This is a placeholder.  I don't know whether this failure is good, bad, or indifferent.  I'm sorry!";
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
		super(name, precondition, config, audit);
		this.test = test;
		this.result = result;
		this.message = message;
	}

	protected String getAudit() {
		String auditString = "out=$(" + super.getAudit() + ");\n";
		auditString += "test=\"" + getTest() + "\";\n";
		
		if (getResult().equals("fail"))
			auditString += "if [ \"${out}\" = \"${test}\" ] ; then\n";
		else
			auditString += "if [ \"${out}\" != \"${test}\" ] ; then\n";
		auditString += "\t" + getLabel() + "=0;\n";
		auditString += "else\n";
		auditString += "\t" + getLabel() + "=1;\n";
		auditString += "fi ;\n";
		return auditString;
	}

	protected String getTest() {
		return this.test;
	}

	protected String getResult() {
		return this.result;
	}
	
	protected String getMessage() {
		String message = this.message;
		
		message = Pattern.quote(message); //Turn special characters into literal so they don't get parsed out
		message = message.substring(2, message.length()-2).trim(); //Remove '\Q' and '\E' from beginning/end since we're not using this as a regex
		message = message.replace("\"", "\\\""); //Also, make sure quote marks are properly escaped!
		
		return message;
	}

}
