/*
 * This code is part of the ThornSec project.
 * 
 * To learn more, please head to its GitHub repo: @privacyint
 * 
 * Pull requests encouraged.
 */
package core.model.machine.configuration;

import java.util.Set;

import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;
import inet.ipaddr.mac.MACAddress;

import core.StringUtils;

import core.model.AModel;
import core.model.network.NetworkModel;

import core.data.machine.configuration.NetworkInterfaceData.Inet;

/**
 * This model represents a NIC.
 *
 */
public class NetworkInterfaceModel extends AModel {

	private String     comment;
	private HostName   host;
	private String     iface;
	private String     preUp;
	private String     postDown;
	private Inet       inet;
	private MACAddress mac;

	private Set<String> bridgePorts;
	
	private IPAddress subnet;
	private IPAddress address;
	private IPAddress netmask;
	private IPAddress broadcast;
	private IPAddress gateway;
	
	public NetworkInterfaceModel(String label, NetworkModel networkModel) {
		super(label, networkModel);
		
		this.host        = null;
		this.iface       = null;
		this.preUp       = null;
		this.postDown    = null;
		this.subnet      = null;
		this.inet        = null;
		this.bridgePorts = null;
		this.address     = null;
		this.netmask     = null;
		this.broadcast   = null;
		this.gateway     = null;
		this.mac         = null;
		this.comment     = null;
	}
	
	public final String getComment() {
		return comment;
	}

	public final void setComment(String comment) {
		this.comment = comment;
	}

	public final HostName getHost() {
		return host;
	}

	public final void setHost(HostName host) {
		this.host = host;
	}

	public final String getIface() {
		return iface;
	}

	public final void setIface(String iface) {
		this.iface = iface;
	}

	public final String getPreUp() {
		return this.preUp;
	}
	
	public final void setPreUp(String preUp) {
		this.preUp = preUp;
	}
	
	public final String getPostDown() {
		return this.postDown;
	}
	
	public final void setPostDown(String postDown) {
		this.postDown = postDown;
	}
	
	public final Inet getInet() {
		return inet;
	}

	public final void setInet(Inet inet) {
		this.inet = inet;
	}

	public final MACAddress getMac() {
		return mac;
	}

	public final void setMac(MACAddress mac) {
		this.mac = mac;
	}

	public final Set<String> getBridgePorts() {
		return bridgePorts;
	}

	public final void setBridgePorts(Set<String> bridgePorts) {
		this.bridgePorts = bridgePorts;
	}

	public final IPAddress getSubnet() {
		return subnet;
	}

	public final void setSubnet(IPAddress subnet) {
		this.subnet = subnet;
	}

	public final IPAddress getAddress() {
		return address;
	}

	public final void setAddress(IPAddress address) {
		this.address = address;
	}

	public final IPAddress getNetmask() {
		return netmask;
	}

	public final void setNetmask(IPAddress netmask) {
		this.netmask = netmask;
	}

	public final IPAddress getBroadcast() {
		return broadcast;
	}

	public final void setBroadcast(IPAddress broadcast) {
		this.broadcast = broadcast;
	}

	public final IPAddress getGateway() {
		return gateway;
	}

	public final void setGateway(IPAddress gateway) {
		this.gateway = gateway;
	}

	private String getStanzaHeader() {
		String ifaceConf = "";
		ifaceConf += "#" + this.getHost() + " interface\n";
		ifaceConf += "#" + this.getComment() + "\n";
		ifaceConf += "iface " + this.getIface() + " inet " + this.getInet();
		
		return ifaceConf;
	}
	
	public String getServerStanza() {
		String ifaceConf = "";
		ifaceConf += this.getStanzaHeader();
		ifaceConf += (this.getBridgePorts() != null) ? "\n" + "bridge_ports " + String.join(" ", this.getBridgePorts()) : "";
		ifaceConf += (this.getAddress()     != null) ? "\n" + "address " + this.getAddress() : "";
		ifaceConf += (this.getNetmask()     != null) ? "\n" + "netmask " + this.getNetmask() : "";
		ifaceConf += (this.getBroadcast()   != null) ? "\n" + "broadcast " + this.getBroadcast() : "";
		ifaceConf += (this.getGateway()     != null) ? "\n" + "gateway " + this.getGateway() : "";
		
		return ifaceConf;
	}
	
	public String getRouterStanza() {
		String ifaceConf = "";
		ifaceConf += this.getStanzaHeader();
		ifaceConf += (this.getBridgePorts() != null) ? "\n" + "bridge_ports " + String.join(" ", this.getBridgePorts()) : "";
		ifaceConf += (this.getGateway()     != null) ? "\n" + "address " + this.getGateway() : "";
		ifaceConf += (this.getNetmask()     != null) ? "\n" + "netmask " + this.getNetmask() : "";

		return ifaceConf;
	}
	
	public String getDhcpStanza() {
		if (mac != null) {
			String ifaceConfDhcp = "";
			ifaceConfDhcp = "\n\n";
			ifaceConfDhcp += "\t#" + this.getHost() + " interface\n";
			ifaceConfDhcp += "\t#" + this.getComment() + "\n";
			ifaceConfDhcp += "\tsubnet " + this.getSubnet() + " netmask " + this.getNetmask() + " {\n";
			ifaceConfDhcp += "\t\thost " + StringUtils.stringToAlphaNumeric(this.getHost().toString(), "_") + "-" + StringUtils.stringToAlphaNumeric(this.getMac().toNormalizedString(), "") + " {\n";
			ifaceConfDhcp += "\t\t\thardware ethernet " + this.getMac() + ";\n";
			ifaceConfDhcp += "\t\t\tfixed-address " + this.getAddress() + ";\n";
			ifaceConfDhcp += "\t\t\toption routers " + this.getGateway() + ";\n";
			ifaceConfDhcp += "\t\t}\n";
			ifaceConfDhcp += "\t}";
			
			return ifaceConfDhcp;
		}
		
		return null;
	}
	
//	private Hashtable<String, InterfaceData> interfaces;
//	private Set<String> customStanzas; 
//
//	InterfaceModel(String label, NetworkModel networkModel) {
//		super(label, networkModel);
//		
//		this.interfaces    = null;
//		this.customStanzas = null;
//	}
//
//	/**
//	 * Gets the configuration/audit units.
//	 *
//	 * @return the units
//	 */
//	public Vector<IUnit> getUnits() {
//		Vector<IUnit> units = new Vector<IUnit>();
//		
//		//We need to handle network resets a bit more carefully (a little less #YOLO)
//		
//		
//		//If we're a router, we want to be very careful in our tearing down...
//		if (networkModel.getRouterServers().contains(me)) {
//			units.addElement(new SimpleUnit("net_conf_persist", "proceed",
//					"echo \"" + getPersistent() + "\" | sudo tee /etc/network/interfaces > /dev/null\n"
//					+ "sudo ip address flush lan0 & \n"
//					+ "ip addr show lan0 | grep -q '10.0.0.1' || (sudo ifdown lan0 &>/dev/null ; sudo ifup lan0 &>/dev/null ) &  \n"
//					+ "sudo service networking restart & \n",
//					"cat /etc/network/interfaces;", getPersistent(), "pass",
//					"Couldn't create our required network interfaces.  This will cause all sorts of issues."));
//		}
//		else {
//			units.addElement(new SimpleUnit("net_conf_persist", "proceed",
//					"echo \"" + getPersistent() + "\" | sudo tee /etc/network/interfaces > /dev/null\n"
//					+ "sudo service networking restart & \n",
//					"cat /etc/network/interfaces;", getPersistent(), "pass",
//					"Couldn't create our required network interfaces.  This will cause all sorts of issues."));			
//		}
//		
//		return units;
//	}
//
//    public void addIface(InterfaceData iface) {
//		if (this.interfaces == null) { this.interfaces = new Hashtable<String, InterfaceModel>(); }
//		
//	}
//    
//	public SimpleUnit addPPPIface(String name, String iface) {
//		String net = "";
//		net +=	"iface " + iface + " inet manual\n";
//		net += "\n";
//		net += "auto provider\n";
//		net += "iface provider inet ppp\n";
//		net += "provider provider";
//		customStanzas.add(net);
//		names.add(iface);
//		return new SimpleUnit(name, "proceed", "echo \\\"handled by model\\\";",
//				"grep \"iface provider inet ppp\" /etc/network/interfaces;",
//				"iface provider inet ppp", "pass");
//	}
//
//	private String getPersistent() {
//		String net = "source /etc/network/interfaces.d/*\n";
//		net += "\n";
//		net += "auto lo\n";
//		net += "iface lo inet loopback\n";
//		net += "pre-up /etc/ipsets/ipsets.up.sh | ipset -! restore\n";
//		net += "pre-up /etc/iptables/iptables.conf.sh | iptables-restore\n";
//		net += "\n";
//		net += "auto";
//		for (String name : names) {
//			net += " " + name;
//		}
//		
//		for (String stanza : customStanzas) {
//			net += "\n\n";
//			net += stanza;
//		}
//
//		for (InterfaceData iface : ifaces) {
//			net += "\n\n";
//			
//			//If we're a router, use the router declaration
//			if (networkModel.getRouterServers().contains(me) && iface.getIface().contains("lan")) {
//				net += iface.getRouterStanza();
//			}
//			//Otherwise, we're on the machine itself
//			else {
//				net += iface.getServerStanza();
//			}
//		}
//
//		return net.trim();
//	}

}
