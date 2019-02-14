package core.data;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Vector;
import java.util.Set;

//import core.iface.IUnit;

abstract class ADeviceData extends AData {
	//Networking stuff...
	private Integer firstOctet;
	private Integer secondOctet;
	private Integer thirdOctet;
	
	private String[] macs;
	private String[] ports;
	private String[] cnames;
	
	private String domain;
	private String hostname;
	
	private Boolean isThrottled;
	private Boolean isManaged;
	
	private String fullname;
	private String sshKey;
	private String defaultPassword;
	
	private HashMap<String, Set<Integer>> requiredEgress;
	private Vector<String> requiredForward;
	private Vector<String> requiredIngress;

	protected ADeviceData(String label) {
		super(label);

		this.firstOctet  = null;
		this.secondOctet = null;
		this.thirdOctet  = null;
		
		this.macs   = null;
		this.ports  = null;
		this.cnames = new String[0];
		
		this.domain   = null;
		this.hostname = null;
		
		this.isThrottled = null;
		this.isManaged   = null;
		
		this.fullname        = null;
		this.sshKey          = null;
		this.defaultPassword = null;
		
		this.requiredEgress  = new HashMap<String, Set<Integer>>();
		this.requiredForward = new Vector<String>();
		this.requiredIngress = new Vector<String>();
	}
	
	//public abstract Vector<IUnit> getUnits();

	

	
	/*
	 * Setters
	 */
	protected void addRequiredEgress(String destination, Set<Integer> ports) {
		this.requiredEgress.put(destination, ports);
	}
	
	protected void addRequiredForward(String destination) {
		this.requiredForward.addElement(destination);
	}

	protected void addRequiredIngress(String source) {
		this.requiredIngress.addElement(source);
	}
	protected void setFirstOctet(Integer octet) {
		this.firstOctet = octet;
	}

	protected void setSecondOctet(Integer octet) {
		this.secondOctet = octet;
	}
	
	protected void setThirdOctet(Integer octet) {
		this.thirdOctet = octet;
	}

	protected void setDomain(String domain) {
		this.domain = domain;
	}
	
	protected void setPorts(String[] ports) {
		this.ports = ports;
	}
	
	protected void setCnames(String[] cnames) {
		this.cnames = cnames;
	}
	
	protected void setHostname(String hostname) {
		this.hostname = hostname;
	}
	
	protected void setMacs(String[] macs) {
		this.macs = macs;
	}
	
	protected void setIsThrottled(Boolean isThrottled) {
		this.isThrottled = isThrottled;
	}
	
	protected void setIsManaged(Boolean isManaged) {
		this.isManaged = isManaged;
	}
	
	protected void setFullName(String fullname) {
		this.fullname = fullname;
	}
	
	protected void setSSHKey(String sshKey) {
		this.sshKey = sshKey;
	}
	
	protected void setDefaultPassword(String defaultPassword) {
		this.defaultPassword = defaultPassword;
	}
	
	/*
	 * Getters
	 */
	public HashMap<String, String> getLanIfaces() {
		HashMap<String, String> ifaces = new HashMap<String, String>();
		
		for (String mac : macs) {
			ifaces.put(mac.replaceAll("[^0-9A-F]", ""), mac);
		}
		
		return ifaces;
	}
	
	public Integer getFirstOctet() {
		return this.firstOctet;
	}
	
	public Integer getSecondOctet() {
		return this.secondOctet;
	}
	
	public Integer getThirdOctet() {
		return this.thirdOctet;
	}
	
	public Boolean getIsThrottled() {
		return this.isThrottled;
	}

	public Boolean getIsManaged() {
		return this.isManaged;
	}
	
	public String[] getPorts() {
		return this.ports;
	}
	
	public String[] getMacs() {
		return this.macs;
	}
	
	public String[] getCnames() {
		return this.cnames;
	}
	
	public String getSSHKey() {
		return this.sshKey;
	}
	
	public String getFullName() {
		return this.fullname;
	}
	
	public String getDefaultPassword() {
		return this.defaultPassword;
	}
	
	public String getFirstThreeOctets() {
		return getFirstOctet() + "." + getSecondOctet() + "." + getThirdOctet() + ".";
	}
	
	public InetAddress getNetmask() {
		return stringToIP("255.255.255.252");
	}
	
	public HashMap<String, Set<Integer>> getRequiredEgress() {
		return this.requiredEgress;
	}
	
	public Vector<String> getRequiredForward() {
		return this.requiredForward;
	}
	
	public Vector<String> getRequiredIngress() {
		return this.requiredIngress;
	}
	
	public String getDomain() {
		return this.domain;
	}
	
	public String getHostname() {
		if (this.hostname == null) {
			return getLabel().replace("_", "-");
		}
		
		return this.hostname;
	}
}
