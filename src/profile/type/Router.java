/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.type;

import java.util.HashSet;
import java.util.Set;

import core.exception.AThornSecException;
import core.iface.IUnit;
import core.model.machine.configuration.NetworkInterfaceModel;
import core.model.network.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.fs.FilePermsUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import inet.ipaddr.AddressStringException;
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

	public final static String SERVERS_NETWORK = "10.0.0.0/255.0.0.0";
	public final static String USERS_NETWORK = "172.16.0.0/255.255.0.0";
	public final static String ADMINS_NETWORK = "172.20.0.0/255.255.0.0";
	public final static String INTERNALS_NETWORK = "172.24.0.0/255.255.0.0";
	public final static String EXTERNALS_NETWORK = "192.168.0.0/255.255.0.0";

	public Router(String label, NetworkModel networkModel) {
		super(label, networkModel);

		this.dnsServer = new UnboundDNSServer(label, networkModel);
		this.dhcpServer = new ISCDHCPServer(label, networkModel);

		// You've gotta give real addresses or stuff breaks!
		try {
			assert (new IPAddressString(Router.SERVERS_NETWORK).toAddress() != null);
			assert (new IPAddressString(Router.USERS_NETWORK).toAddress() != null);
			assert (new IPAddressString(Router.ADMINS_NETWORK).toAddress() != null);
			assert (new IPAddressString(Router.INTERNALS_NETWORK).toAddress() != null);
			assert (new IPAddressString(Router.EXTERNALS_NETWORK).toAddress() != null);
		} catch (AddressStringException | IncompatibleAddressException e) {
			e.printStackTrace();
		}
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

		// Create our VLANs for our various networks.
		units.addAll(NetworkInterfaceModel.buildVLAN("servers", SERVERS_NETWORK));
		units.addAll(NetworkInterfaceModel.buildVLAN("admins", ADMINS_NETWORK));
		units.addAll(NetworkInterfaceModel.buildVLAN("users", USERS_NETWORK));
		units.addAll(NetworkInterfaceModel.buildVLAN("externalOnlys", EXTERNALS_NETWORK));
		units.addAll(NetworkInterfaceModel.buildVLAN("internalOnlys", INTERNALS_NETWORK));

		// TODO: Revisit
		if (getNetworkModel().getData().buildAutoGuest()) {
			units.addAll(NetworkInterfaceModel.buildVLAN("autoguest", "10.0.0.1"));
		}

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
		units.addAll(getDNSServer().getPersistentConfig());

		return units;
	}
}
