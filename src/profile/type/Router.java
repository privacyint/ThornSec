/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.type;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.json.stream.JsonParsingException;

import core.data.machine.AMachineData.MachineType;
import core.data.machine.configuration.NetworkInterfaceData;
import core.data.machine.configuration.NetworkInterfaceData.Direction;
import core.exception.AThornSecException;
import core.exception.data.ADataException;
import core.exception.data.InvalidIPAddressException;
import core.exception.runtime.InvalidMachineModelException;
import core.iface.IUnit;
import core.model.machine.configuration.NetworkInterfaceModel;
import core.model.network.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.fs.FilePermsUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.EnabledServiceUnit;
import core.unit.pkg.InstalledUnit;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.IncompatibleAddressException;
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

	private final Map<MachineType, IPAddress> macVLANs;

	public Router(String label, NetworkModel networkModel) throws AThornSecException {
		super(label, networkModel);

		// Start by building the VLANs we'll be hanging all of our networking off
		this.macVLANs = new LinkedHashMap<>();
		addMACVLAN(MachineType.SERVER, getNetworkModel().getData().getServerSubnet());
		addMACVLAN(MachineType.USER, getNetworkModel().getData().getUserSubnet());
		addMACVLAN(MachineType.ADMIN, getNetworkModel().getData().getAdminSubnet());
		addMACVLAN(MachineType.INTERNAL_ONLY, getNetworkModel().getData().getInternalSubnet());
		addMACVLAN(MachineType.EXTERNAL_ONLY, getNetworkModel().getData().getExternalSubnet());
		if (networkModel.getData().buildAutoGuest()) {
			addMACVLAN(MachineType.GUEST, getNetworkModel().getData().getGuestSubnet());
		}

		// Now create our DHCP Server.
		this.dhcpServer = new ISCDHCPServer(label, networkModel);
		this.dnsServer = new UnboundDNSServer(label, networkModel);
	}

	/**
	 * Creates a MACVLAN on our Router.
	 *
	 * A MACVLAN is a VLAN represented as a separate "physical" (virtual) LAN by
	 * using a different MAC address from the underlying hardware.
	 *
	 * The MACVLAN acts as a virtual switch, and as such should follow roughly the
	 * same idea as zones in firewalls.
	 *
	 * @param type
	 * @param vlanNetwork
	 * @throws InvalidIPAddressException
	 * @throws InvalidMachineModelException
	 */
	public final void addMACVLAN(MachineType type, String vlanNetwork)
			throws InvalidIPAddressException, InvalidMachineModelException {
		IPAddress ip = null;

		try {
			ip = new IPAddressString(vlanNetwork).toAddress();

			if (ip == null) {
				throw new AddressStringException(vlanNetwork);
			}
		} catch (AddressStringException | IncompatibleAddressException e) {
			throw new InvalidIPAddressException(vlanNetwork);
		}

		this.macVLANs.put(type, ip);
		try {
			final Collection<NetworkInterfaceData> nicsData = getNetworkModel().getData()
					.getNetworkInterfaces(getLabel()).get(Direction.LAN);
			for (final NetworkInterfaceData nicData : nicsData) {
				final String nicName = nicData.getIface();

				for (final NetworkInterfaceModel nicModel : getNetworkModel().getNetworkInterfaces(getLabel())) {
					if (nicModel.getIface().equals(nicName)) {
						nicModel.setIsIPForwarding(true);
						nicModel.addMACVLAN(type.toString());
					}
				}

			}
		} catch (JsonParsingException | ADataException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			final Collection<NetworkInterfaceData> nicsData = getNetworkModel().getData()
					.getNetworkInterfaces(getLabel()).get(Direction.WAN);
			for (final NetworkInterfaceData nicData : nicsData) {
				final String nicName = nicData.getIface();

				for (final NetworkInterfaceModel nicModel : getNetworkModel().getNetworkInterfaces(getLabel())) {
					if (nicModel.getIface().equals(nicName)) {
						nicModel.setIsIPMasquerading(true);
					}
				}

			}
		} catch (JsonParsingException | ADataException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Map<MachineType, IPAddress> getMACVLANs() {
		return this.macVLANs;
	}

	public ADHCPServerProfile getDHCPServer() {
		return this.dhcpServer;
	}

	public ADNSServerProfile getDNSServer() {
		return this.dnsServer;
	}

	@Override
	protected Collection<IUnit> getPersistentConfig() throws AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

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
		//Stop our machine from spamming internal ARP requests on our external interface
		//See https://git.kernel.org/pub/scm/linux/kernel/git/davem/net.git/tree/Documentation/networking/ip-sysctl.txt#n1247
		sysctl.appendLine("net.ipv4.conf.all.arp_filter=1");
		sysctl.appendLine("net.ipv4.conf.default.arp_filter=1");
		//Set Reverse Path filtering to "strict"
		// See https://git.kernel.org/pub/scm/linux/kernel/git/davem/net.git/tree/Documentation/networking/ip-sysctl.txt#n1226
		sysctl.appendLine("net.ipv4.conf.all.rp_filter=1");
		sysctl.appendLine("net.ipv4.conf.default.rp_filter=1");

		// Switch systemd-networkd on...
		units.add(new EnabledServiceUnit("systemd_networkd", "proceed", "systemd-networkd",
				"I was unable to enable the networking service. This is bad!"));

		// Create VLANs for our various networks, and tell our DHCP Server about them
		for (final MachineType vlan : getMACVLANs().keySet()) {
			units.addAll(NetworkInterfaceModel.buildMACVLAN(vlan, getMACVLANs().get(vlan)));
			getDHCPServer().addSubnet(vlan.toString(), getMACVLANs().get(vlan));
		}

		units.addAll(getDHCPServer().getPersistentConfig());
		units.addAll(getDNSServer().getPersistentConfig());

		return units;
	}

	@Override
	protected Collection<IUnit> getInstalled() throws AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		// Add useful tools for Routers here
		units.add(new InstalledUnit("traceroute", "proceed", "traceroute"));
		units.add(new InstalledUnit("speedtest_cli", "proceed", "speedtest-cli"));

		units.addAll(getDNSServer().getInstalled());
		units.addAll(getDHCPServer().getInstalled());

		return units;
	}

	@Override
	protected Collection<IUnit> getLiveConfig() throws AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		// Collection<NetworkInterfaceData> lanIfaces = null;
		// try {
		// lanIfaces =
		// getNetworkModel().getData().getNetworkInterfaces(getLabel()).get(Direction.LAN);
		// } catch (JsonParsingException | IOException e) {
		// TODO Auto-generated catch block
		// e.printStackTrace();
		// }

//		if ((lanIfaces == null) || (lanIfaces.size() > 1)) {
//			System.out.println(
//					"I've found more than one LAN interface. We don't handle this (yet). Binding to the first.");
//		}

//		final NetworkInterfaceData nic = lanIfaces.iterator().next();

//		final DirUnit dropIn = new DirUnit("macvlan_dir", "proceed",
//				"/etc/systemd/network/" + nic.getIface() + ".network.d");
//		units.add(dropIn);

//		final FileUnit macVLANs = new FileUnit("macvlan_assign", "macvlan_dir_created",
//				"/etc/systemd/network/" + nic + "/vlans.conf");
//		units.add(macVLANs);
//		macVLANs.appendLine("[Network]");

//		for (final MachineType vlan : getMACVLANs().keySet()) {

//			macVLANs.appendLine("MACVLAN=" + vlan);

//			for (final AMachineModel machine : getNetworkModel().getMachines(vlan).values()) {
//				getDHCPServer().addToSubnet(vlan.toString(), machine);
//			}
//		}

		units.addAll(getDHCPServer().getLiveConfig());
		units.addAll(getDNSServer().getLiveConfig());

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(getDHCPServer().getPersistentFirewall());
		units.addAll(getDNSServer().getPersistentFirewall());

		return units;
	}

	@Override
	public Collection<IUnit> getLiveFirewall() throws AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(getDHCPServer().getLiveFirewall());
		units.addAll(getDNSServer().getLiveFirewall());

		return units;
	}
}
