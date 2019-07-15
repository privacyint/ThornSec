/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine.configuration;

import java.util.Set;

import core.StringUtils;
import core.data.machine.configuration.NetworkInterfaceData.Inet;
import core.model.AModel;
import core.model.network.NetworkModel;
import inet.ipaddr.IPAddress;
import inet.ipaddr.mac.MACAddress;

/**
 * This model represents a NIC.
 *
 */
public class NetworkInterfaceModel extends AModel {

	private String comment;
	private String iface;
	private String preUp;
	private String postDown;
	private Inet inet;
	private MACAddress mac;

	private Set<String> bridgePorts;

	private IPAddress subnet;
	private IPAddress address;
	private IPAddress netmask;
	private IPAddress broadcast;
	private IPAddress gateway;

	public NetworkInterfaceModel(String label, NetworkModel networkModel) {
		super(label, networkModel);

		this.iface = null;
		this.preUp = null;
		this.postDown = null;
		this.subnet = null;
		this.inet = null;
		this.bridgePorts = null;
		this.address = null;
		this.netmask = null;
		this.broadcast = null;
		this.gateway = null;
		this.mac = null;
		this.comment = null;
	}

	public final String getComment() {
		return this.comment;
	}

	public final void setComment(String comment) {
		this.comment = comment;
	}

	public final String getIface() {
		return this.iface;
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
		return this.inet;
	}

	public final void setInet(Inet inet) {
		this.inet = inet;
	}

	public final MACAddress getMac() {
		return this.mac;
	}

	public final void setMac(MACAddress mac) {
		this.mac = mac;
	}

	public final Set<String> getBridgePorts() {
		return this.bridgePorts;
	}

	public final void setBridgePorts(Set<String> bridgePorts) {
		this.bridgePorts = bridgePorts;
	}

	public final IPAddress getSubnet() {
		return this.subnet;
	}

	public final void setSubnet(IPAddress subnet) {
		this.subnet = subnet;
	}

	public final IPAddress getAddress() {
		return this.address;
	}

	public final void setAddress(IPAddress address) {
		this.address = address;
	}

	public final IPAddress getNetmask() {
		return this.netmask;
	}

	public final void setNetmask(IPAddress netmask) {
		this.netmask = netmask;
	}

	public final IPAddress getBroadcast() {
		return this.broadcast;
	}

	public final void setBroadcast(IPAddress broadcast) {
		this.broadcast = broadcast;
	}

	public final IPAddress getGateway() {
		return this.gateway;
	}

	public final void setGateway(IPAddress gateway) {
		this.gateway = gateway;
	}

	private String getStanzaHeader() {
		String ifaceConf = "";
		ifaceConf += "#" + getLabel() + " interface\n";
		ifaceConf += "#" + getComment() + "\n";

		return ifaceConf;
	}

	public String getServerStanza() {
		String ifaceConf = "";
		ifaceConf += getStanzaHeader();
		ifaceConf += "iface " + getIface() + " inet " + getInet();
		ifaceConf += (getBridgePorts() != null) ? "\n" + "bridge_ports " + String.join(" ", getBridgePorts()) : "";
		ifaceConf += (getAddress() != null) ? "\n" + "address " + getAddress() : "";
		ifaceConf += (getNetmask() != null) ? "\n" + "netmask " + getNetmask() : "";
		ifaceConf += (getBroadcast() != null) ? "\n" + "broadcast " + getBroadcast() : "";
		ifaceConf += (getGateway() != null) ? "\n" + "gateway " + getGateway() : "";

		return ifaceConf;
	}

	public String getRouterStanza() {
		String ifaceConf = "";
		ifaceConf += getStanzaHeader();
		ifaceConf += "iface " + getIface() + " inet " + getInet();
		ifaceConf += (getBridgePorts() != null) ? "\n" + "bridge_ports " + String.join(" ", getBridgePorts()) : "";
		ifaceConf += (getGateway() != null) ? "\n" + "address " + getGateway() : "";
		ifaceConf += (getNetmask() != null) ? "\n" + "netmask " + getNetmask() : "";

		return ifaceConf;
	}

	public String getDhcpStanza() {
		if (this.mac != null) {
			String ifaceConfDhcp = "";
			ifaceConfDhcp = "\n\n";
			ifaceConfDhcp += "\t#" + getLabel() + " interface\n";
			ifaceConfDhcp += "\t#" + getComment() + "\n";
			ifaceConfDhcp += "\tsubnet " + getSubnet() + " netmask " + getNetmask() + " {\n";
			ifaceConfDhcp += "\t\thost " + StringUtils.stringToAlphaNumeric(getLabel().toString(), "_") + "-"
					+ StringUtils.stringToAlphaNumeric(getMac().toNormalizedString(), "") + " {\n";
			ifaceConfDhcp += "\t\t\thardware ethernet " + getMac() + ";\n";
			ifaceConfDhcp += "\t\t\tfixed-address " + getAddress() + ";\n";
			ifaceConfDhcp += "\t\t\toption routers " + getGateway() + ";\n";
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
