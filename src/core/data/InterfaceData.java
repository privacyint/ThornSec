package core.data;

import java.net.InetAddress;

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
	
	public String getIface() {
		return this.iface;
	}
	
	public InetAddress getSubnet() {
		return this.subnet;
	}
	
	public InetAddress getAddress() {
		return this.address;
	}
	
	public InetAddress getNetmask() {
		return this.netmask;
	}

	public InetAddress getBroadcast() {
		return this.broadcast;
	}

	public InetAddress getGateway() {
		return this.gateway;
	}

	public String getMac() {
		return this.mac;
	}
	
	public String[] getBridgePorts() {
		return this.bridgePorts;
	}
	
	public String getInet() {
		return this.inet;
	}
	
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
	
	public String getRouterStanza() {
		String ifaceConf = "";
		ifaceConf += "#" + host + " interface\n";
		ifaceConf += "#" + comment + "\n";
		ifaceConf += "iface " + iface + " inet " + inet;
		ifaceConf += (bridgePorts != null) ?  "\n" + "bridge_ports " + String.join(" ", bridgePorts) : "";
		ifaceConf += (gateway != null) ? "\n" + "address " + gateway.getHostAddress() : "";
		ifaceConf += (netmask != null) ? "\n" + "netmask " + netmask.getHostAddress() : "";
		
		return ifaceConf;
	}
	
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
	
	private String cleanString(String string) {
		String invalidChars = "[^a-zA-Z0-9_]";
		String safeChars    = "_";
		
		return string.replaceAll(invalidChars, safeChars);
	}
}