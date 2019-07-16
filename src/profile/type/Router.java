/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.type;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import core.data.machine.configuration.NetworkInterfaceData.Inet;
import core.exception.AThornSecException;
import core.iface.IUnit;
import core.model.machine.configuration.NetworkInterfaceModel;
import core.model.network.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.fs.FilePermsUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import inet.ipaddr.IPAddressString;
import profile.dhcp.ADHCPServerProfile;
import profile.dhcp.ISCDHCPServer;
import profile.dns.ADNSServerProfile;
import profile.dns.UnboundDNSServer;

/**
 * This is a Router.
 *
 * This is where much of the security is enforced across the network. I've tried
 * to split it out as much as possible!
 *
 * If you want to make changes in here, you'll have a lot of reading to do :)!
 */
public class Router extends AStructuredProfile {
	private final ADNSServerProfile dnsServer;
	private final ADHCPServerProfile dhcpServer;

	private final HashMap<String, InetAddress[]> resolved;

	public Router(String label, NetworkModel networkModel) {
		super(label, networkModel);

		this.dnsServer = new UnboundDNSServer(label, networkModel);
		this.dhcpServer = new ISCDHCPServer(label, networkModel);

		this.resolved = new HashMap<>();
	}

	public ADHCPServerProfile getDHCPServer() {
		return this.dhcpServer;
	}

	public ADNSServerProfile getDNSServer() {
		return this.dnsServer;
	}

	@Override
	protected Set<IUnit> getPersistentConfig() throws AThornSecException {
		final Set<IUnit> units = new HashSet<>();

		units.addAll(this.dhcpServer.getPersistentConfig());
		units.addAll(this.dnsServer.getPersistentConfig());

		final FileUnit resolvConf = new FileUnit("leave_my_resolv_conf_alone", "proceed",
				"/etc/dhcp/dhclient-enter-hooks.d/leave_my_resolv_conf_alone");
		units.add(resolvConf);

		resolvConf.appendLine("make_resolv_conf() { :; }");

		units.add(new FilePermsUnit("leave_my_resolv_conf_alone", "leave_my_resolv_conf_alone",
				"/etc/dhcp/dhclient-enter-hooks.d/leave_my_resolv_conf_alone", "755",
				"I couldn't stop various systemd services deciding to override your DNS settings."
						+ " This will cause you intermittent, difficult to diagnose problems as it randomly"
						+ " sets your DNS to wherever it decides. Great for laptops/desktops, atrocious for servers..."));

		final FileUnit sysctl = new FileUnit("sysctl_conf", "proceed", "/etc/sysctl.conf");
		units.add(sysctl);

		sysctl.appendLine("net.ipv4.ip_forward=1");
		sysctl.appendLine("net.ipv6.conf.all.disable_ipv6=1");
		sysctl.appendLine("net.ipv6.conf.default.disable_ipv6=1");
		sysctl.appendLine("net.ipv6.conf.lo.disable_ipv6=1");

		// this.networkModel.getServerModel(getLabel()).getConfigsModel().addConfigFilePath("/etc/sysctl.conf");

		if (this.networkModel.getData().buildAutoGuest()) {
			final NetworkInterfaceModel iface = new NetworkInterfaceModel("autoguest", this.networkModel);
			this.networkModel.getServerModel(getLabel()).addLANInterface(iface);

			iface.setIface("lan0:9001");
			iface.setInet(Inet.STATIC);
			iface.setSubnet(new IPAddressString("10.250.0.0").getAddress());
			iface.setNetmask(new IPAddressString("255.255.252.0").getAddress());
			iface.setGateway(new IPAddressString("10.250.0.1").getAddress());
			iface.setComment("Auto Guest pool, bridged to our lan");
		}

		return units;
	}

	@Override
	protected Set<IUnit> getInstalled() throws AThornSecException {
		final Set<IUnit> units = new HashSet<>();

		units.addAll(this.dnsServer.getInstalled());
		units.addAll(this.dhcpServer.getInstalled());

		// Add useful tools for Routers here
		units.add(new InstalledUnit("traceroute", "proceed", "traceroute"));
		units.add(new InstalledUnit("speedtest_cli", "proceed", "speedtest-cli"));

		return units;
	}

	@Override
	protected Set<IUnit> getLiveConfig() throws AThornSecException {
		final Set<IUnit> units = new HashSet<>();

		units.addAll(this.dhcpServer.getLiveConfig());
		units.addAll(this.dnsServer.getLiveConfig());

		return units;
	}

	@Override
	public Set<IUnit> getPersistentFirewall() {
		final Set<IUnit> units = new HashSet<>();
		// TODO: THIS!!!
//		final NetworkInterfaceModel interfaces = me.getLANInterfaces();
//
//		firewall = ((ServerModel) me).getFirewallModel();
//		domain = this.networkModel.getData().getDomain(getLabel());
//
//		final JsonArray extInterfaces = this.networkModel.getData().getPropertyObjectArray(getLabel(), "wan");
//
//		if (extInterfaces.size() == 0) {
//			JOptionPane.showMessageDialog(null,
//					"You must specify at least one WAN interface for your router.\n\nValid options are 'ppp', 'static', and 'dhcp'");
//			System.exit(1);
//		}
//
//		for (int i = 0; i < extInterfaces.size(); ++i) {
//			final JsonObject row = extInterfaces.getJsonObject(i);
//
//			String wanIface = row.getString("iface");
//
//			// If we've already declared this iface, give it an alias
//			if (wanIfaces.contains(wanIface)) {
//				wanIface += ":" + i;
//			}
//
//			// These are fine to be null if not given
//			final InetAddress staticAddress = this.networkModel.stringToIP(row.getString("address", null));
//			final InetAddress netmask = this.networkModel.stringToIP(row.getString("netmask", null));
//			final InetAddress gateway = this.networkModel.stringToIP(row.getString("gateway", null));
//			final InetAddress broadcast = this.networkModel.stringToIP(row.getString("broadcast", null));
//
//			switch (row.getString("inettype", null)) {
//			case "dhcp":
//				interfaces.addIface(new InterfaceData(getLabel(), // host
//						wanIface, // iface
//						null, // mac
//						"dhcp", // inet
//						null, // bridgeports
//						null, // subnet
//						null, // address
//						null, // netmask
//						null, // broadcast
//						null, // gateway
//						"DHCP WAN physical network interface" // comment
//				));
//
//				String dhclient = "option rfc3442-classless-static-routes code 121 = array of unsigned integer 8;\n";
//				dhclient += "send host-name = gethostname();\n";
//				dhclient += "supersede domain-search \\\"" + this.domain + "\\\";\n";
//				dhclient += "supersede domain-name-servers 10.0.0.1;\n";
//				dhclient += "request subnet-mask, broadcast-address, time-offset, routers,\n";
//				dhclient += "	domain-name, domain-name-servers, domain-search, host-name,\n";
//				dhclient += "	dhcp6.name-servers, dhcp6.domain-search,\n";
//				dhclient += "	netbios-name-servers, netbios-scope, interface-mtu,\n";
//				dhclient += "	rfc3442-classless-static-routes, ntp-servers;";
//				units.add(new FileUnit("router_ext_dhcp_persist", "proceed", dhclient, "/etc/dhcp/dhclient.conf"));
//
//				firewall.addFilterInput("router_ext_dhcp_in", "-i " + wanIface + " -d 255.255.255.255" + " -p udp"
//						+ " --dport 68" + " --sport 67" + " -j ACCEPT", "Make sure the Router can send DHCP requests");
//				firewall.addFilterOutput("router_ext_dhcp_ipt_out",
//						"-o " + wanIface + " -p udp" + " --dport 67" + " --sport 68" + " -j ACCEPT",
//						"Make sure the Router can receive DHCP responses");
//				break;
//			case "ppp":
//				units.add(new InstalledUnit("ext_ppp", "ppp"));
//				units.add(me.getLANInterfaces().addPPPIface("router_ext_ppp_iface", wanIface));
//				this.networkModel.getServerModel(getLabel()).addProcessString("/usr/sbin/pppd call provider$");
//
//				((ServerModel) me).getConfigsModel().addConfigFilePath("/etc/ppp/peers/dsl-provider$");
//				((ServerModel) me).getConfigsModel().addConfigFilePath("/etc/ppp/options$");
//
//				units.add(((ServerModel) me).getConfigsModel().addConfigFile("resolv_conf", "proceed",
//						"nameserver 127.0.0.1", "/etc/ppp/resolv.conf"));
//
//				firewall.addMangleForward("clamp_mss_to_pmtu",
//						"-p tcp --tcp-flags SYN,RST SYN -m tcpmss --mss 1400:1536 -j TCPMSS --clamp-mss-to-pmtu",
//						"Clamp the MSS to PMTU. This makes sure the packets over PPPoE are the correct size (and take into account the PPPoE overhead)");
//
//				this.isPPP = true;
//
//				break;
//			case "static":
//				interfaces.addIface(new InterfaceData(getLabel(), // host
//						wanIface, // iface
//						null, // mac
//						"static", // inet
//						null, // bridgeports
//						null, // subnet
//						staticAddress, // address
//						netmask, // netmask
//						broadcast, // broadcast
//						gateway, // gateway
//						"Static WAN physical network interface" // comment
//				));
//
//				this.isStatic = true;
//
//				break;
//			default:
//				JOptionPane.showMessageDialog(null,
//						"Valid options for your router's WAN inettype are 'ppp', 'static', and 'dhcp'");
//				System.exit(1);
//				break;
//			}
//
//			wanIfaces.add(wanIface);
//	}

//		final String lanBridge = "lan0";
//
//		// We don't actually care about LAN ifaces if we're a metal/router
//		if (this.networkModel.getData().isMetal(getLabel())) {
//			// We very deliberately hang everything off a bridge, to stop internal traffic
//			// over the WAN iface
//			units.add(new InstalledUnit("bridge_utils", "proceed", "bridge-utils"));
//
//			final InetAddress subnet = this.networkModel.stringToIP("10.0.0.0");
//			final InetAddress address = this.networkModel.stringToIP("10.0.0.1");
//
//			interfaces.addIface(new InterfaceData(getLabel(), // host
//					this.lanBridge, // iface
//					null, // mac
//					"static", // inet
//					new String[] { "none" }, // bridgeports
//					subnet, // subnet
//					null, // address
//					this.netmask, // netmask
//					null, // broadcast
//					address, // "gateway" - because this is a router iface, it only looks at gateways
//					"VM interface on a bridge to nowhere" // comment
//			));
//		} else {
//			// First, add this machine's own interfaces
//			for (final String lanIface : this.routerLanIfaces) {
//				interfaces.addIface(new InterfaceData(getLabel(), // host
//						lanIface, // iface
//						null, // mac
//						"manual", // inet
//						null, // bridgeports
//						null, // subnet
//						null, // address
//						null, // netmask
//						null, // broadcast
//						null, // gateway
//						"LAN physical network interface" // comment
//				));
//			}
//
//			// Now, bridge 'em
//			final InetAddress subnet = this.networkModel.stringToIP("10.0.0.0");
//			final InetAddress address = this.networkModel.stringToIP("10.0.0.1");
//			interfaces.addIface(new InterfaceData("lan", // host
//					this.lanBridge, // iface
//					null, // mac
//					"static", // inet
//					this.routerLanIfaces.toArray(new String[this.routerLanIfaces.size()]), // bridgeports
//					subnet, // subnet
//					address, // address
//					this.netmask, // netmask
//					null, // broadcast
//					address, // gateway
//					"bridge all physical interfaces" // comment
//			));
//		}
//
//		// Now add for our servers
//		// for (ServerModel srv : networkModel.getAllServers()) {
//		for (final MachineModel machine : this.networkModel.getAllMachines()) {
//			if (this.machine.equals(me)) {
//				continue;
//			} // Skip if we're talking about ourself
//
//			Integer classifier = null;
//
//			if (this.machine instanceof ServerModel) {
//				classifier = 0;
//			} else if (this.machine instanceof DeviceModel) {
//				classifier = 1;
//			}
//
//			for (final InterfaceData machineLanIface : this.machine.getLANInterfaces().getIfaces()) {
//				// Parse our MAC address into an integer to stop collisions when adding/removing
//				// interfaces
//				// String alias = null; //getAlias(machineLanIface.getMac());
//
//				// if (machineLanIface.getMac() == null) {
//				// alias = getAlias(machineLanIface.getIface());
//				// }
//				// else {
//				// alias = getAlias(machineLanIface.getMac());
//				// }
//				String ifaceName = null;
//				String ifaceComment = null;
//
//				if (((ServerModel) me).isMetal()) {
//					ifaceName = "vm" + this.machine.getThirdOctet();
//					ifaceComment = "Router/Metal interface. This is a fake interface just for the VM";
//				} else {
//					ifaceName = this.lanBridge + ":" + classifier + this.machine.getThirdOctet();
//					ifaceComment = "Router interface. Let's bridge to lan";
//				}
//
//				interfaces.addIface(new InterfaceData(this.machine.getLabel(), // host
//						ifaceName, // iface
//						machineLanIface.getMac(), // mac
//						"static", // inet
//						machineLanIface.getBridgePorts(), // bridgeports
//						machineLanIface.getSubnet(), // subnet
//						machineLanIface.getAddress(), // address
//						this.netmask, // netmask
//						machineLanIface.getBroadcast(), // broadcast
//						machineLanIface.getGateway(), // gateway
//						ifaceComment // comment
//				));
//
//				final String[] cnames = this.networkModel.getData().getCnames(this.machine.getLabel());
//				final String[] subdomains = new String[cnames.length + 1];
//				System.arraycopy(new String[] { this.machine.getHostname() }, 0, subdomains, 0, 1);
//				System.arraycopy(cnames, 0, subdomains, 1, cnames.length);
//
//				this.dnsServer.addDomainRecord(this.networkModel.getData().getDomain(this.machine.getLabel()),
//						machineLanIface.getGateway(), subdomains, machineLanIface.getAddress());
//			}
//		}

		return units;
	}

}
