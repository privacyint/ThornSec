/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.json.stream.JsonParsingException;
import core.data.machine.AMachineData.MachineType;
import core.data.machine.configuration.NetworkInterfaceData.Direction;
import core.exception.AThornSecException;
import core.exception.data.InvalidIPAddressException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.machine.ServerModel;
import core.model.machine.configuration.networking.BondInterfaceModel;
import core.model.machine.configuration.networking.BondModel;
import core.model.machine.configuration.networking.DummyModel;
import core.model.machine.configuration.networking.MACVLANModel;
import core.model.machine.configuration.networking.MACVLANTrunkModel;
import core.model.machine.configuration.networking.NetworkInterfaceModel;
import core.unit.SimpleUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.EnabledServiceUnit;
import core.unit.pkg.InstalledUnit;
import profile.dhcp.ADHCPServerProfile;
import profile.dhcp.ISCDHCPServer;
import profile.dns.ADNSServerProfile;
import profile.dns.UnboundDNSServer;
import profile.firewall.AFirewallProfile;

/**
 * This is a Router.
 *
 * This is where much of the security is enforced across the network. I've tried
 * to split it out as much as possible!
 *
 * If you want to make changes in here, you'll have a lot of reading to do :)!
 */
public class Router extends AMachine {
	private final ADNSServerProfile dnsServer;
	private final ADHCPServerProfile dhcpServer;
	private final AFirewallProfile firewall;

	public Router(ServerModel me) throws AThornSecException, JsonParsingException {
		super(me);

		bondLANIfaces();
		buildVLANs();

		this.firewall = me.getFirewall();
		this.dhcpServer = new ISCDHCPServer(me);
		this.dnsServer = new UnboundDNSServer(me);
	}

	private void bondLANIfaces() {
		NetworkInterfaceModel lanTrunk = null;

		Set<NetworkInterfaceModel> nics = getMachineModel().getNetworkInterfaces()
			.stream()
			.filter(nic -> Direction.LAN.equals(nic.getDirection()))
			.collect(Collectors.toSet());

		if (nics.isEmpty()) {
			lanTrunk = new DummyModel();
		}
		else {
			lanTrunk = new BondModel();
			nics.forEach((lanNic) -> {
				getMachineModel().addNetworkInterface(new BondInterfaceModel(lanNic.getData(), getNetworkModel()));
			});
		}

		lanTrunk.setIface("LAN");
		getMachineModel().addNetworkInterface(lanTrunk);
	}

	/**
	 * Builds our various VLANs, but only if they're required.
	 * @return a collection of the VLANs built in this method
	 * @throws InvalidIPAddressException if an IP address is invalid
	 * @throws InvalidServerModelException if a given ServerModel doesn't exist
	 */
	private final MACVLANTrunkModel buildVLANs() throws InvalidIPAddressException, InvalidServerModelException {
		Set<MachineType> vlans = new LinkedHashSet<>();
		vlans.add(MachineType.SERVER);
		vlans.add(MachineType.INTERNAL_ONLY);
		vlans.add(MachineType.EXTERNAL_ONLY);
		vlans.add(MachineType.USER);
		vlans.add(MachineType.ADMIN);
		if (getNetworkModel().buildAutoGuest()) {
			vlans.add(MachineType.GUEST);
		}

		final MACVLANTrunkModel trunk = new MACVLANTrunkModel();
		trunk.setIface("LAN");
		getMachineModel().addNetworkInterface(trunk);

		for (MachineType type : vlans) {
			if (getNetworkModel().getMachines(type).isEmpty()) {
				continue;
			}

			MACVLANModel vlan = new MACVLANModel();
			vlan.setIface(type.toString());
			vlan.setSubnet(getNetworkModel().getSubnet(type));
			vlan.addAddress(getNetworkModel().getSubnet(type).getLowerNonZeroHost());
			vlan.setType(type);
			trunk.addVLAN(vlan);
			getMachineModel().addNetworkInterface(vlan);
		}

		return trunk;
	}

	public ADHCPServerProfile getDHCPServer() {
		return this.dhcpServer;
	}

	public ADNSServerProfile getDNSServer() {
		return this.dnsServer;
	}

	@Override
	public Collection<IUnit> getInstalled() throws AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		// Add useful tools for Routers here
		units.add(new InstalledUnit("traceroute", "proceed", "traceroute"));
		units.add(new InstalledUnit("speedtest_cli", "proceed", "speedtest-cli"));

		units.addAll(getDHCPServer().getInstalled());
		units.addAll(getDNSServer().getInstalled());

		return units;
	}

	@Override
	public Collection<IUnit> getLiveConfig() throws AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(getDHCPServer().getLiveConfig());
		units.addAll(getDNSServer().getLiveConfig());

		return units;
	}

	@Override
	public Collection<IUnit> getLiveFirewall() throws AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(getDHCPServer().getLiveFirewall());
		units.addAll(getDNSServer().getLiveFirewall());

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentConfig() throws AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(super.getPersistentConfig());

		final FileUnit resolvConf = new FileUnit("leave_my_resolv_conf_alone", "proceed",
				"/etc/dhcp/dhclient-enter-hooks.d/leave_my_resolv_conf_alone", 
				"I couldn't stop various systemd services deciding to override your DNS settings."
						+ " This will cause you intermittent, difficult to diagnose problems as it randomly"
						+ " sets your DNS to wherever it decides. Great for laptops/desktops, atrocious for servers...");
		units.add(resolvConf);

		resolvConf.appendLine("make_resolv_conf() { :; }");

		// The trunk device needs to be set to Promiscuous mode, or else the Kernel can't see anything running over it.
		// As per https://wiki.archlinux.org/index.php/Network_configuration#Promiscuous_mode
		final FileUnit promiscuousService = new FileUnit("promiscuous_service", "proceed",
				"/etc/systemd/system/promiscuous@.service",
				"I failed at creating a SystemD service to set the trunk (LAN) Network Interface card to promiscuous mode. Your Router will not be able to route any internal traffic.");
		units.add(promiscuousService);

		promiscuousService.appendLine("[Unit]");
		promiscuousService.appendLine("Description=Set %i interface in promiscuous mode");
		promiscuousService.appendLine("After=network.target");
		promiscuousService.appendCarriageReturn();
		promiscuousService.appendLine("[Service]");
		promiscuousService.appendLine("Type=oneshot");
		promiscuousService.appendLine("ExecStart=/usr/bin/ip link set dev %i promisc on");
		promiscuousService.appendLine("RemainAfterExit=yes");
		promiscuousService.appendCarriageReturn();
		promiscuousService.appendLine("[Install]");
		promiscuousService.appendLine("WantedBy=multi-user.target");

		units.add(new SimpleUnit("promiscuous_mode_enabled_lan", "promiscuous_service", "sudo systemctl enable promiscuous@LAN.service",
				"sudo systemctl is-enabled promiscuous@LAN.service", "enabled", "pass"));

		final FileUnit sysctl = new FileUnit("sysctl_conf", "proceed", "/etc/sysctl.conf");
		units.add(sysctl);

		sysctl.appendLine("net.ipv4.ip_forward=1");
		sysctl.appendLine("net.ipv6.conf.all.disable_ipv6=1");
		sysctl.appendLine("net.ipv6.conf.default.disable_ipv6=1");
		sysctl.appendLine("net.ipv6.conf.lo.disable_ipv6=1");
		// Stop our machine from spamming internal ARP requests on our external interface
		// See https://git.kernel.org/pub/scm/linux/kernel/git/davem/net.git/tree/Documentation/networking/ip-sysctl.txt#n1247
		sysctl.appendLine("net.ipv4.conf.all.arp_filter=1");
		sysctl.appendLine("net.ipv4.conf.default.arp_filter=1");
		// Set Reverse Path filtering to "strict"
		// See https://git.kernel.org/pub/scm/linux/kernel/git/davem/net.git/tree/Documentation/networking/ip-sysctl.txt#n1226
		sysctl.appendLine("net.ipv4.conf.all.rp_filter=1");
		sysctl.appendLine("net.ipv4.conf.default.rp_filter=1");

		// Switch systemd-networkd on...
		units.add(new EnabledServiceUnit("systemd_networkd", "proceed", "systemd-networkd", "I was unable to enable the networking service. This is bad!"));

		units.addAll(getDHCPServer().getPersistentConfig());
		units.addAll(getDNSServer().getPersistentConfig());

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(getDHCPServer().getPersistentFirewall());
		units.addAll(getDNSServer().getPersistentFirewall());

		return units;
	}
}
