/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.type;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import core.data.machine.AMachineData.MachineType;
import core.exception.AThornSecException;
import core.exception.data.InvalidIPAddressException;
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
	public final static String SERVERS_NETWORK = "10.0.0.1/8";
	public final static String USERS_NETWORK = "172.16.0.1/16";
	public final static String ADMINS_NETWORK = "172.20.0.1/16";
	public final static String INTERNALS_NETWORK = "172.24.0.1/16";
	public final static String EXTERNALS_NETWORK = "172.28.0.1/16";
	public final static String AUTOGUEST_NETWORK = "172.31.0.1/16";

	private final ADNSServerProfile dnsServer;
	private final ADHCPServerProfile dhcpServer;

	private final Map<MachineType, IPAddress> macVLANs;

	public Router(String label, NetworkModel networkModel) throws InvalidIPAddressException {
		super(label, networkModel);

		this.macVLANs = new LinkedHashMap<>();

		addMACVLAN(MachineType.SERVER, SERVERS_NETWORK);
		addMACVLAN(MachineType.USER, USERS_NETWORK);
		addMACVLAN(MachineType.ADMIN, ADMINS_NETWORK);
		addMACVLAN(MachineType.INTERNAL_ONLY, INTERNALS_NETWORK);
		addMACVLAN(MachineType.EXTERNAL_ONLY, EXTERNALS_NETWORK);
		if (networkModel.getData().buildAutoGuest()) {
			addMACVLAN(MachineType.GUEST, AUTOGUEST_NETWORK);
		}

		this.dnsServer = new UnboundDNSServer(label, networkModel);
		this.dhcpServer = new ISCDHCPServer(label, networkModel);
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
	 */
	public final void addMACVLAN(MachineType type, String vlanNetwork) throws InvalidIPAddressException {
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
	protected Set<IUnit> getPersistentConfig() throws AThornSecException {
		final Set<IUnit> units = new HashSet<>();

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

		// Switch systemd-networkd on...
		units.add(new EnabledServiceUnit("systemd_networkd", "proceed", "systemd-networkd",
				"I was unable to enable the networking service. This is bad!"));

		// Create our VLANs for our various networks.
		for (final MachineType vlan : getMACVLANs().keySet()) {
			units.addAll(NetworkInterfaceModel.buildMACVLAN(vlan, getMACVLANs().get(vlan)));
		}

		units.addAll(getDHCPServer().getPersistentConfig());
		units.addAll(getDNSServer().getPersistentConfig());

		return units;
	}

	@Override
	protected Set<IUnit> getInstalled() throws AThornSecException {
		final Set<IUnit> units = new HashSet<>();

		// Add useful tools for Routers here
		units.add(new InstalledUnit("traceroute", "proceed", "traceroute"));
		units.add(new InstalledUnit("speedtest_cli", "proceed", "speedtest-cli"));

		units.addAll(getDNSServer().getInstalled());
		units.addAll(getDHCPServer().getInstalled());

		return units;
	}

	@Override
	protected Set<IUnit> getLiveConfig() throws AThornSecException {
		final Set<IUnit> units = new HashSet<>();

		units.addAll(getDHCPServer().getLiveConfig());
		units.addAll(getDNSServer().getLiveConfig());

		return units;
	}

	@Override
	public Set<IUnit> getPersistentFirewall() throws AThornSecException {
		final Set<IUnit> units = new HashSet<>();

		units.addAll(getDHCPServer().getPersistentFirewall());
		units.addAll(getDNSServer().getPersistentFirewall());

		return units;
	}

	@Override
	public Set<IUnit> getLiveFirewall() throws AThornSecException {
		final Set<IUnit> units = new HashSet<>();

		units.addAll(getDHCPServer().getLiveFirewall());
		units.addAll(getDNSServer().getLiveFirewall());

		return units;
	}
}
