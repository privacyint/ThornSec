package core.data;

import java.net.InetAddress;

/**
 * The Class InterfaceData.
 * 
 * Represents a network interface
 */
public class InterfaceData {
	
	private String comment;
	private String host;
	private String iface;
	private String inet;
	private String mac;
	private String[] bridgePorts;
	private InetAddress subnet;
	private InetAddress address;
	private InetAddress netmask;
	private InetAddress broadcast;
	private InetAddress gateway;
	
	/**
	 * Instantiates a new interface.
	 *
	 * @param host Name of the host
	 * @param iface Name of the iface
	 * @param mac MAC address
	 * @param inet Internet connection type - manual/static/dhcp
	 * @param bridgePorts Interfaces to bridge this interface with
	 * @param subnet Subnet for this interface
	 * @param address IP address
	 * @param netmask Netmask
	 * @param broadcast Broadcast
	 * @param gateway Gateway
	 * @param comment Comment to add to the DHCP stanza
	 */
	public InterfaceData(String host, String iface, String mac, String inet, String[] bridgePorts, InetAddress subnet, InetAddress address, InetAddress netmask, InetAddress broadcast, InetAddress gateway, String comment) {
		this.host        = host;
		this.iface       = iface;
		this.subnet      = subnet;
		this.inet        = inet;
		this.bridgePorts = bridgePorts;
		this.address     = address;
		this.netmask     = netmask;
		this.broadcast   = broadcast;
		this.gateway     = gateway;
		this.mac         = mac;
		this.comment     = comment;
	}
	
	/**
	 * Gets the iface.
	 *
	 * @return the iface
	 */
	public String getIface() {
		return this.iface;
	}
	
	/**
	 * Gets the subnet.
	 *
	 * @return the subnet
	 */
	public InetAddress getSubnet() {
		return this.subnet;
	}
	
	/**
	 * Gets the address.
	 *
	 * @return the address
	 */
	public InetAddress getAddress() {
		return this.address;
	}
	
	/**
	 * Gets the netmask.
	 *
	 * @return the netmask
	 */
	public InetAddress getNetmask() {
		return this.netmask;
	}

	/**
	 * Gets the broadcast.
	 *
	 * @return the broadcast
	 */
	public InetAddress getBroadcast() {
		return this.broadcast;
	}

	/**
	 * Gets the gateway.
	 *
	 * @return the gateway
	 */
	public InetAddress getGateway() {
		return this.gateway;
	}

	/**
	 * Gets the mac.
	 *
	 * @return the mac
	 */
	public String getMac() {
		return this.mac;
	}
	
	/**
	 * Gets the bridge ports.
	 *
	 * @return the bridge ports
	 */
	public String[] getBridgePorts() {
		return this.bridgePorts;
	}
	
	/**
	 * Gets the inet.
	 *
	 * @return the inet
	 */
	public String getInet() {
		return this.inet;
	}
	
	/**
	 * Gets the metal stanza for this interface.
	 *
	 * @return the metal's stanza
	 */
	public String getMetalStanza() {
		String ifaceConf = "";
		ifaceConf += "#" + host + " interface\n";
		ifaceConf += "#" + comment + "\n";
		ifaceConf += "iface " + iface + " inet " + inet;
		ifaceConf += (bridgePorts != null) ?  "\n" + "bridge_ports " + bridgePorts : "";
		ifaceConf += (address != null) ? "\n" + "address " + address.getHostAddress() : "";
		ifaceConf += (netmask != null) ? "\n" + "netmask " + netmask.getHostAddress() : "";
		ifaceConf += (broadcast != null) ? "\n" + "broadcast " + broadcast.getHostAddress() : "";
		ifaceConf += (gateway != null) ? "\n" + "gateway " + gateway.getHostAddress() : "";
		
		return ifaceConf;
	}
	
	/**
	 * Gets the server's stanza for this interface.
	 *
	 * @return the server stanza
	 */
	public String getServerStanza() {
		String ifaceConf = "";
		ifaceConf += "#" + host + " interface\n";
		ifaceConf += "#" + comment + "\n";
		ifaceConf += "iface " + iface + " inet " + inet;
		ifaceConf += (bridgePorts != null) ?  "\n" + "bridge_ports " + String.join(" ", bridgePorts) : "";
		ifaceConf += (address != null) ? "\n" + "address " + address.getHostAddress() : "";
		ifaceConf += (netmask != null) ? "\n" + "netmask " + netmask.getHostAddress() : "";
		ifaceConf += (broadcast != null) ? "\n" + "broadcast " + broadcast.getHostAddress() : "";
		ifaceConf += (gateway != null) ? "\n" + "gateway " + gateway.getHostAddress() : "";
		
		return ifaceConf;
	}
	
	/**
	 * Gets the router's stanza for this interface.
	 *
	 * @return the router stanza
	 */
	public String getRouterStanza() {
		String ifaceConf = "";
		ifaceConf += "#" + host + " interface\n";
		ifaceConf += "#" + comment + "\n";
		ifaceConf += "iface " + iface + " inet " + inet;
		ifaceConf += (address != null) ? "\n" + "address " + address.getHostAddress() : "";
		ifaceConf += (netmask != null) ? "\n" + "netmask " + netmask.getHostAddress() : "";
		ifaceConf += (broadcast != null) ? "\n" + "broadcast " + broadcast.getHostAddress() : "";
		ifaceConf += (gateway != null) ? "\n" + "gateway " + gateway.getHostAddress() : "";		
		return ifaceConf;
	}
	
	/**
	 * Gets the dhcpd stanza in isc-dhcp-server format
	 *
	 * @return the dhcp stanza
	 */
	public String getDhcpStanza() {
		if (mac != null) {
			String ifaceConfDhcp = "";
			ifaceConfDhcp = "\n\n";
			ifaceConfDhcp += "\t#" + host + " interface\n";
			ifaceConfDhcp += "\t#" + comment + "\n";
			ifaceConfDhcp += "\tsubnet " + subnet.getHostAddress() + " netmask " + netmask.getHostAddress() + " {\n";
			ifaceConfDhcp += "\t\thost " + cleanString(host) + "-" + mac.replaceAll(":", "") + " {\n";
			ifaceConfDhcp += "\t\t\thardware ethernet " + mac + ";\n";
			ifaceConfDhcp += "\t\t\tfixed-address " + address.getHostAddress() + ";\n";
			ifaceConfDhcp += "\t\t\toption routers " + gateway.getHostAddress() + ";\n";
			ifaceConfDhcp += "\t\t}\n";
			ifaceConfDhcp += "\t}";
			
			return ifaceConfDhcp;
		}
		
		return "";
	}
	
	/**
	 * Cleans a string of all non-alphanumeric characters.
	 *
	 * @param string the dirty string
	 * @return the clean string, with all impermissable characters replaced with _
	 */
	private String cleanString(String string) {
		String invalidChars = "[^a-zA-Z0-9_]";
		String safeChars    = "_";
		
		return string.replaceAll(invalidChars, safeChars);
	}
}
