package core.data;

import java.net.InetAddress;

import core.StringUtils;

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
	
	public String getComment() {
		return this.comment;
	}
	
	/**
	 * Gets the address.
	 *
	 * @return the address
	 */
	public InetAddress getAddress() {
		return this.address;
	}
	
	public String getHost() {
		return this.host;
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
	
	private String getHeader() {
		String ifaceConf = "";
		ifaceConf += "#" + this.getHost() + " interface\n";
		ifaceConf += "#" + this.getComment() + "\n";
		ifaceConf += "iface " + this.getIface() + " inet " + this.getInet();
		
		return ifaceConf;
	}
	
	/**
	 * Gets the metal stanza for this interface.
	 *
	 * @return the metal's stanza
	 */
	public String getServerStanza() {
		String ifaceConf = "";
		ifaceConf += this.getHeader();
		ifaceConf += (this.getBridgePorts() != null) ? "\n" + "bridge_ports " + String.join(" ", this.getBridgePorts()) : "";
		ifaceConf += (this.getAddress()     != null) ? "\n" + "address " + this.getAddress().getHostAddress() : "";
		ifaceConf += (this.getNetmask()     != null) ? "\n" + "netmask " + this.getNetmask().getHostAddress() : "";
		ifaceConf += (this.getBroadcast()   != null) ? "\n" + "broadcast " + this.getBroadcast().getHostAddress() : "";
		ifaceConf += (this.getGateway()     != null) ? "\n" + "gateway " + this.getGateway().getHostAddress() : "";
		
		return ifaceConf;
	}
	
	/**
	 * Gets the router's stanza for this interface.
	 *
	 * @return the router stanza
	 */
	public String getRouterStanza() {
		String ifaceConf = "";
		ifaceConf += this.getHeader();
		ifaceConf += (this.getBridgePorts() != null) ? "\n" + "bridge_ports " + String.join(" ", this.getBridgePorts()) : "";
		ifaceConf += (this.getGateway()     != null) ? "\n" + "address " + this.getGateway().getHostAddress() : "";
		ifaceConf += (this.getNetmask()     != null) ? "\n" + "netmask " + this.getNetmask().getHostAddress() : "";

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
			ifaceConfDhcp += "\t#" + this.getHost() + " interface\n";
			ifaceConfDhcp += "\t#" + this.getComment() + "\n";
			ifaceConfDhcp += "\tsubnet " + this.getSubnet().getHostAddress() + " netmask " + this.getNetmask().getHostAddress() + " {\n";
			ifaceConfDhcp += "\t\thost " + StringUtils.stringToAlphaNumeric(this.getHost(), "_") + "-" + StringUtils.stringToAlphaNumeric(this.getMac(), "") + " {\n";
			ifaceConfDhcp += "\t\t\thardware ethernet " + this.getMac() + ";\n";
			ifaceConfDhcp += "\t\t\tfixed-address " + this.getAddress().getHostAddress() + ";\n";
			ifaceConfDhcp += "\t\t\toption routers " + this.getGateway().getHostAddress() + ";\n";
			ifaceConfDhcp += "\t\t}\n";
			ifaceConfDhcp += "\t}";
			
			return ifaceConfDhcp;
		}
		
		return null;
	}
}
