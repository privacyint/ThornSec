package core.data;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import javax.swing.JOptionPane;

import java.util.Set;

/**
 * Abstract class for something representing "Device Data" on our network.
 * 
 * This is the parent class for all machines on our network
 */
abstract class ADeviceData extends AData {
	
	//Network Interface stuff...
	private Integer firstOctet;
	private Integer secondOctet;
	private Integer thirdOctet;
	private String[] macs;

	//DNS stuff
	private String[] cnames;
	private String domain;
	private String hostname;
	
	//Information about this device
	private String fullname;
	private String sshKey;
	private String defaultPassword;
	private String emailAddress;

	//Firewall stuff...
	private Boolean isThrottled;
	private Boolean isManaged;
	
	private Set<Integer> listenPorts;
	private HashMap<String, HashMap<Integer, Set<Integer>>> requiredEgress;
	private Vector<String> requiredForward;
	private Vector<String> requiredIngress;

	/**
	 * Instantiates a new a device data.
	 *
	 * @param label the device label
	 */
	protected ADeviceData(String label) {
		super(label);

		this.firstOctet  = null;
		this.secondOctet = null;
		this.thirdOctet  = null;
		this.macs        = null;

		this.cnames   = new String[0];
		this.domain   = null;
		this.hostname = null;
		
		this.fullname        = null;
		this.sshKey          = null;
		this.defaultPassword = null;
		this.emailAddress    = null;
		
		this.isThrottled = null;
		this.isManaged   = null;

		this.listenPorts     = null;
		this.requiredEgress  = new HashMap<String, HashMap<Integer, Set<Integer>>>();
		this.requiredForward = new Vector<String>();
		this.requiredIngress = new Vector<String>();
	}
	
	/**
	 * Adds a required egress rule.
	 *
	 * @param destination the destination (IP or FQDN)
	 * @param cidr the cidr
	 * @param ports the destination ports
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
	
	/**
	 * Adds a required egress rule, using a default CIDR of 32 (a single IP)
	 *
	 * @param destination the destination (IP or FQDN)
	 * @param ports the destination ports
	 */
	final protected void addRequiredEgress(String destination, Set<Integer> ports) {
		addRequiredEgress(destination, 32, ports);
	}
	
	/**
	 * Adds a required egress rule to a single port, using a CIDR of 32 (a single IP) 
	 * 
	 * @param destination the destination (IP or FQDN)
	 * @param port a single destination port
	 */
	final public void addRequiredEgress(String destination, Integer port) {
		HashSet<Integer> portSet = new HashSet<Integer>();
		portSet.add(port);
		
		addRequiredEgress(destination, 32, portSet);
	}

	/**
	 * Adds a required forward (to a device inside our network)
	 *
	 * @param destination the destination
	 */
	final protected void addRequiredForward(String destination) {
		this.requiredForward.addElement(destination);
	}

	/**
	 * Adds a required ingress.
	 *
	 * @param sourceIP the source IP
	 */
	final protected void addRequiredIngress(String sourceIP) {
		if (!isValidIP(sourceIP)) {
			JOptionPane.showMessageDialog(null, sourceIP + " appears to be an invalid address.");
			System.exit(1);
        }
		
		this.requiredIngress.addElement(sourceIP);
	}
	
	/**
	 * Sets the IP address first octet.
	 *
	 * @param octet the new first octet
	 */
	final protected void setFirstOctet(Integer octet) {
		if (octet < 0 || octet > 255) {
			JOptionPane.showMessageDialog(null, octet + " is an invalid octet. Its value must be 0-255 (inclusive).");
			System.exit(1);
		}
		
		this.firstOctet = octet;
	}

	/**
	 * Sets the IP address second octet.
	 *
	 * @param octet the new second octet
	 */
	final protected void setSecondOctet(Integer octet) {
		if (octet < 0 || octet > 255) {
			JOptionPane.showMessageDialog(null, octet + " is an invalid octet. Its value must be 0-255 (inclusive).");
			System.exit(1);
		}

		this.secondOctet = octet;
	}
	
	/**
	 * Sets the IP address third octet.
	 *
	 * @param octet the new third octet
	 */
	final protected void setThirdOctet(Integer octet) {
		if (octet < 0 || octet > 255) {
			JOptionPane.showMessageDialog(null, octet + " is an invalid octet. Its value must be 0-255 (inclusive).");
			System.exit(1);
		}

		this.thirdOctet = octet;
	}

	/**
	 * Sets the device's domain.
	 *
	 * @param domain the new domain
	 */
	protected void setDomain(String domain) {
		this.domain = domain;
	}
	
	/**
	 * Sets the ports.
	 *
	 * @param ports the new ports
	 */
	protected void setListenPorts(String ports) {
		for (Integer port : super.parseIntList(ports)) {
			addListenPort(port);
		}
	}
	
	/**
	 * Add a listening port to our device.
	 * 
	 * @param port Port to listen on
	 */
	protected void addListenPort(Integer port) {
		if (this.listenPorts == null) {
			this.listenPorts = new HashSet<Integer>();
		}
		
		this.listenPorts.add(port);
	}
	
	/**
	 * Sets the cnames.
	 *
	 * @param cnames the new cnames
	 */
	protected void setCnames(String[] cnames) {
		this.cnames = cnames;
	}
	
	/**
	 * Sets the hostname.
	 *
	 * @param hostname the new hostname
	 */
	protected void setHostname(String hostname) {
		this.hostname = hostname;
	}
	
	/**
	 * Sets the email address.
	 *
	 * @param address the new email address
	 */
	protected void setEmailAddress(String address) {
		this.emailAddress = address;
	}
	
	/**
	 * Sets the macs.
	 *
	 * @param macs the new macs
	 */
	protected void setMacs(String[] macs) {
		this.macs = macs;
	}
	
	/**
	 * Sets whether this device should have its egress throttled.
	 *
	 * @param isThrottled the new checks if is throttled
	 */
	protected void setIsThrottled(Boolean isThrottled) {
		this.isThrottled = isThrottled;
	}
	
	/**
	 * Sets whether this device is managed (e.g. WiFi AP with web front end).
	 *
	 * @param isManaged the new checks if is managed
	 */
	protected void setIsManaged(Boolean isManaged) {
		this.isManaged = isManaged;
	}
	
	/**
	 * Sets the full name - usually only used for user accounts.
	 *
	 * @param fullname the new full name
	 */
	protected void setFullName(String fullname) {
		this.fullname = fullname;
	}
	
	/**
	 * Sets the public SSH key for this device.
	 *
	 * @param sshKey the new SSH key
	 */
	protected void setSSHKey(String sshKey) {
		this.sshKey = sshKey;
	}
	
	/**
	 * Sets the default password.
	 *
	 * @param defaultPassword the new default password
	 */
	protected void setDefaultPassword(String defaultPassword) {
		this.defaultPassword = defaultPassword;
	}
	
	/**
	 * Gets the lan ifaces.
	 *
	 * @return the lan ifaces
	 */
	public HashMap<String, String> getLanIfaces() {
		HashMap<String, String> ifaces = new HashMap<String, String>();
		
		for (String mac : macs) {
			ifaces.put(mac.replaceAll("[^0-9A-F]", ""), mac);
		}
		
		return ifaces;
	}
	
	/**
	 * Gets the first octet.
	 *
	 * @return the first octet
	 */
	public Integer getFirstOctet() {
		return this.firstOctet;
	}
	
	/**
	 * Gets the second octet.
	 *
	 * @return the second octet
	 */
	public Integer getSecondOctet() {
		return this.secondOctet;
	}
	
	/**
	 * Gets the third octet.
	 *
	 * @return the third octet
	 */
	public Integer getThirdOctet() {
		return this.thirdOctet;
	}
	
	/**
	 * Should this device have its egress throttled?
	 *
	 * @return whether this device should be throttled
	 */
	public Boolean getIsThrottled() {
		return this.isThrottled;
	}

	/**
	 * Is this device managed?
	 *
	 * @return whether this device should be managed
	 */
	public Boolean getIsManaged() {
		return this.isManaged;
	}
	
	/**
	 * Gets the ports this device requires listening on.
	 *
	 * @return the ports
	 */
	public Set<Integer> getListenPorts() {
		return this.listenPorts;
	}
	
	/**
	 * Gets the macs.
	 *
	 * @return the macs
	 */
	public String[] getMacs() {
		return this.macs;
	}
	
	/**
	 * Gets the cnames.
	 *
	 * @return the cnames
	 */
	public String[] getCnames() {
		return this.cnames;
	}
	
	/**
	 * Gets the SSH key.
	 *
	 * @return the SSH key
	 */
	public String getSSHKey() {
		return this.sshKey;
	}
	
	/**
	 * Gets the full name.
	 *
	 * @return the full name
	 */
	public String getFullName() {
		return this.fullname;
	}
	
	/**
	 * Gets the email address.
	 *
	 * @return the email address
	 */
	public String getEmailAddress() {
		return this.emailAddress;
	}
	
	/**
	 * Gets the default password.
	 *
	 * @return the default password
	 */
	public String getDefaultPassword() {
		return this.defaultPassword;
	}
	
	/**
	 * Gets the first three octets.
	 *
	 * @return the first three octets ("X.X.X.")
	 */
	public String getFirstThreeOctets() {
		return getFirstOctet() + "." + getSecondOctet() + "." + getThirdOctet() + ".";
	}
	
	/**
	 * Gets the netmask. This is hardcoded to /30 for now
	 *
	 * @return the netmask of 255.255.255.252
	 */
	public InetAddress getNetmask() {
		return stringToIP("255.255.255.252");
	}
	
	/**
	 * Gets all required egress.
	 *
	 * @return the required egress
	 */
	public HashMap<String, HashMap<Integer, Set<Integer>>> getRequiredEgress() {
		return this.requiredEgress;
	}
	
	/**
	 * Gets all required forwards.
	 *
	 * @return the required forward
	 */
	public Vector<String> getRequiredForward() {
		return this.requiredForward;
	}
	
	/**
	 * Gets any required ingress.
	 *
	 * @return the required ingress
	 */
	public Vector<String> getRequiredIngress() {
		return this.requiredIngress;
	}
	
	/**
	 * Gets the domain.
	 *
	 * @return the domain
	 */
	public String getDomain() {
		return this.domain;
	}
	
	/**
	 * Gets the hostname.
	 *
	 * @return the hostname
	 */
	public String getHostname() {
		if (this.hostname == null) {
			return getLabel().replace("_", "-");
		}
		
		return this.hostname;
	}
}
