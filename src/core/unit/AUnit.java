/*
 * This code is part of the ThornSec project.
 * 
 * To learn more, please head to its GitHub repo: @privacyint
 * 
 * Pull requests encouraged.
 */
package core.unit;

import java.util.regex.Pattern;
import core.iface.IUnit;
import core.model.network.NetworkModel;

/**
 * This is a basic unit test.
 */
public abstract class AUnit implements IUnit {

	protected NetworkModel networkModel;

	protected String label;
	protected String precondition;
	protected String config;
	protected String audit;
	protected String message;

	public AUnit(String label, String precondition, String config, String audit, String message) {
		//Do some normalisation of the unit test labels. You'll thank me later, I assure you. 
		this.label        = label.toLowerCase().replaceAll("[^a-z0-9]", "_");
		this.precondition = precondition.toLowerCase().replaceAll("[^a-z0-9]", "_");
		this.config       = config;
		this.audit        = audit;
		this.message      = message;
	}

	public AUnit(String label, String precondition, String config, String audit) {
		this(label, precondition, config, audit, "Default failure message. Oops! I don't know whether this failure is good, bad, or indifferent.");
	}
	
	public final String getLabel() {
		return this.label;
	}

	protected abstract String getAudit();

	protected abstract String getPrecondition();

	protected abstract String getConfig();

	protected abstract String getDryRun();
	
	protected String getMessage() {
		String message = this.message;
		
		message = Pattern.quote(message); //Turn special characters into literal so they don't get parsed out
		message = message.substring(2, message.length()-2).trim(); //Remove '\Q' and '\E' from beginning/end since we're not using this as a regex
		message = message.replace("\"", "\\\""); //Also, make sure quote marks are properly escaped!
		
		return message;
	}
	
	public String genAudit(boolean quiet) {
		String auditString = getLabel() + "=0;\n";
		auditString += this.getAudit();
		auditString += "if [ \"$" + getLabel() + "\" = \"1\" ] ; then\n";
		if (!quiet)
			auditString += "\tprintf \"\\e[0;32m ✓ \\e[0m " + getLabel() + "\n\"\n";
		auditString += "\t" + "((pass++))\n";
		auditString += "else\n";
		if (!quiet)
			auditString += "\tprintf \"\\e[0;31m ❌ \\e[0m " + getLabel() + "\n\"\n";
		auditString += "\t" + "((fail++))\n";
		auditString += "\t" + "fail_string=\"${fail_string}\n" + getLabel();
		auditString += " failed with the message:\n";
		auditString += "\\\"${out}\\\"\n";
		auditString += this.getMessage() + "\n\"\n\n";
		auditString += "fi ;";
		return auditString;
	}

	public String genConfig() {
		String configString = this.getAudit();
		configString += "if [ \"$" + getLabel() + "\" != \"1\" ] ; then\n";
		configString += "\tif [ \"$" + getPrecondition() + "\" = \"1\" ] ; then\n";
		configString += "\t\t" + "printf \"\\e[0;31m ❌ \\e[0m " + getLabel() + "... configuring\n\"\n";
		configString += "\t\t" + getConfig() + "\n";
		configString += "\t\t" + "printf \"...Retesting " + getLabel() + "\n\"\n";
		configString += this.genAudit(false);
		configString += "\telse\n";
		configString += "\t\t" + getLabel() + "=0;\n";
		configString += "\t\t" + "printf \"\\e[0;31m ❌ \\e[0m " + getLabel() + " \\e[0;32mPRECONDITION FAILED\\e[0m " + getPrecondition() + "\n\"\n";
		configString += "\tfi ;\n";
		configString += "else\n";
		configString += "\tprintf \"\\e[0;32m ✓ \\e[0m " + getLabel() + "\n\"\n";
		configString += "\t" + "((pass++))\n";
		configString += "fi ;\n";
		return configString;
	}

	public String genDryRun() {
		String dryrunString = this.getAudit();
		dryrunString += "if [ \"$" + getLabel() + "\" != \"1\" ] ; then\n";
		dryrunString += "\t" + "echo 'fail " + getLabel() + " DRYRUN'\n";
		dryrunString += "\t" + "echo '" + getConfig() + "';\n";
		dryrunString += this.getDryRun();
		dryrunString += "\t" + "((fail++))\n";
		dryrunString += "\t" + "fail_string=\"$fail_string\n" + getLabel() + "\"\n";
		dryrunString += "else\n";
		dryrunString += "\techo pass " + getLabel() + "\n";
		dryrunString += "\t" + "((pass++))\n";
		dryrunString += "fi ;\n";
		return dryrunString;
	}
}
