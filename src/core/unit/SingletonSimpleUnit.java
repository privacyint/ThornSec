package core.unit;

public class SingletonSimpleUnit extends SimpleUnit {

	public SingletonSimpleUnit(String name, String precondition, String config, String audit, String test,
			String result) {
		super(name, precondition, config, audit, test, result);
	}

	public String genConfig() {
		String configString = this.getAudit();
		configString += "if [ \"$" + getLabel() + "\" != \"1\" ] ; then\n";
		configString += "\techo fail " + getLabel() + " CONFIG SINGLETON\n";
		configString += "\t" + "((fail++))\n";
		configString += "\t" + "fail_string=\"$fail_string\n" + getLabel() + "\"\n";
		configString += "else\n";
		configString += "\techo pass " + getLabel() + "\n";
		configString += "\t" + "((pass++))\n";
		configString += "fi ;\n";
		return configString;
	}

	public String genDryRun() {
		String dryrunString = this.getAudit();
		dryrunString += "if [ \"$" + getLabel() + "\" != \"1\" ] ; then\n";
		dryrunString += "\t" + "echo 'fail " + getLabel() + " DRYRUN SINGLETON'\n";
		dryrunString += "\t" + "((fail++))\n";
		dryrunString += "\t" + "fail_string=\"$fail_string\n" + getLabel() + "\"\n";
		dryrunString += "else\n";
		dryrunString += "\techo pass " + getLabel() + "\n";
		dryrunString += "\t" + "((pass++))\n";
		dryrunString += "fi ;\n";
		return dryrunString;
	}

}
