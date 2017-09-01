package core.iface;

public interface IUnit  {
	
	public String getLabel();

	public String genAudit(boolean quiet);

	public String genConfig();

	public String genDryRun();

}