package core.data;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Vector;

import javax.json.JsonObject;
import javax.swing.JOptionPane;

import java.util.Set;

abstract class ADeviceData extends AData {
	//Networking stuff...
	private Integer firstOctet;
	private Integer secondOctet;
	private Integer thirdOctet;
	
	private String[] macs;
	private Set<Integer> ports;
	private String[] cnames;
	
	private String domain;
	private String hostname;
	
	private Boolean isThrottled;
	private Boolean isManaged;
	
	private String fullname;
	private String sshKey;
	private String defaultPassword;
	private String emailAddress;
	
	private HashMap<String, HashMap<Integer, Set<Integer>>> requiredEgress;
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
		this.emailAddress    = null;
		
		this.requiredEgress     = new HashMap<String, HashMap<Integer, Set<Integer>>>();
		this.requiredForward    = new Vector<String>();
		this.requiredIngress    = new Vector<String>();
	}
	
	/*
	 * Setters
	 */
	final protected void addRequiredEgress(String destination, Integer cidr, Set<Integer> ports) {
        //First check we're not trying to ingest garbage
		if (!isValidURI(destination) && !isValidIP(destination)) {
			JOptionPane.showMessageDialog(null, destination + " appears to be an invalid address.");
			System.exit(1);
        }

        if (cidr < 1 || cidr > 32) {
			JOptionPane.showMessageDialog(null, cidr + " is an invalid CIDR. Its value must be 1-32 (inclusive).");
			System.exit(1);
		}
        
        for (Integer port : ports) {
        	if (port < 1 || port > 65535) {
    			JOptionPane.showMessageDialog(null, port + " is an invalid port number. Its value must be 1-65535 (inclusive).");
    			System.exit(1);
        	}
        }
        
        //We still here? Good!
		HashMap<Integer, Set<Integer>> deets = new HashMap<Integer, Set<Integer>>();
		deets.put(cidr, ports);
		
		this.requiredEgress.put(destination, deets);
	}
	
	final protected void addRequiredEgress(String destination, Set<Integer> ports) {
		addRequiredEgress(destination, 32, ports);
	}
	
	final protected void addRequiredForward(String destination) {
		this.requiredForward.addElement(destination);
	}

	final protected void addRequiredIngress(String sourceIP) {
		if (!isValidIP(sourceIP)) {
			JOptionPane.showMessageDialog(null, sourceIP + " appears to be an invalid address.");
			System.exit(1);
        }
		
		this.requiredIngress.addElement(sourceIP);
	}
	
	final protected void setFirstOctet(Integer octet) {
		if (octet < 0 || octet > 255) {
			JOptionPane.showMessageDialog(null, octet + " is an invalid octet. Its value must be 0-255 (inclusive).");
			System.exit(1);
		}
		
		this.firstOctet = octet;
	}

	final protected void setSecondOctet(Integer octet) {
		if (octet < 0 || octet > 255) {
			JOptionPane.showMessageDialog(null, octet + " is an invalid octet. Its value must be 0-255 (inclusive).");
			System.exit(1);
		}

		this.secondOctet = octet;
	}
	
	final protected void setThirdOctet(Integer octet) {
		if (octet < 0 || octet > 255) {
			JOptionPane.showMessageDialog(null, octet + " is an invalid octet. Its value must be 0-255 (inclusive).");
			System.exit(1);
		}

		this.thirdOctet = octet;
	}

	protected void setDomain(String domain) {
		this.domain = domain;
	}
	
	protected void setPorts(String ports) {
		this.ports = super.parseIntList(ports);
	}
	
	protected void setCnames(String[] cnames) {
		this.cnames = cnames;
	}
	
	protected void setHostname(String hostname) {
		this.hostname = hostname;
	}
	
	protected void setEmailAddress(String address) {
		this.emailAddress = address;
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
	
	public Set<Integer> getPorts() {
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
	
	public String getEmailAddress() {
		return this.emailAddress;
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
	
	public HashMap<String, HashMap<Integer, Set<Integer>>> getRequiredEgress() {
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
