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
import java.util.HashMap;
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
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.machine.ServerModel;
import core.model.machine.configuration.networking.BondInterfaceModel;
import core.model.machine.configuration.networking.BondModel;
import core.model.machine.configuration.networking.DummyModel;
import core.model.machine.configuration.networking.ISystemdNetworkd;
import core.model.machine.configuration.networking.MACVLANModel;
import core.model.machine.configuration.networking.MACVLANTrunkModel;
import core.model.machine.configuration.networking.NetworkInterfaceModel;
import core.model.network.NetworkModel;
import core.unit.SimpleUnit;
import core.unit.fs.FilePermsUnit;
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
public class Router extends AMachineProfile {
	private final ADNSServerProfile dnsServer;
	private final ADHCPServerProfile dhcpServer;
	private Map<NetworkInterfaceModel, MachineType> vlans;
	
	private final ServerModel me;
	private final AFirewallProfile firewall;

	public Router(String label, NetworkModel networkModel) throws AThornSecException, JsonParsingException {
		super(label, networkModel);

		this.vlans = null;
		this.me = getNetworkModel().getServerModel(getLabel());
		this.firewall = me.getFirewall();
		
		try {
			addLANIfaces();
			addWANIfaces();
			this.firewall.addVLANs(buildVLANs());
		} catch (JsonParsingException | ADataException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		this.dhcpServer = new ISCDHCPServer(label, networkModel);
		this.dnsServer = new UnboundDNSServer(label, networkModel);
	}

	/**
	 * Builds our various VLANs, but only if they're required.
	 * @return a collection of the VLANs built in this method
	 * @throws InvalidIPAddressException if an IP address is invalid
	 * @throws InvalidServerModelException if a given ServerModel doesn't exist
	 */
	private final Map<NetworkInterfaceModel, MachineType> buildVLANs() throws InvalidIPAddressException, InvalidServerModelException {
		final MACVLANTrunkModel trunk = new MACVLANTrunkModel("Trunk");
		trunk.setIface("LAN");

		me.addNetworkInterface(trunk);

		if (!getNetworkModel().getMachines(MachineType.SERVER).isEmpty()) {
			NetworkInterfaceModel serversNic = new MACVLANModel(MachineType.SERVER.toString(), trunk,
					getNetworkModel().getData().getServerSubnet(), getNetworkModel().getData().getServerSubnet());
			
			me.addNetworkInterface(serversNic);
			this.addVLAN(serversNic, MachineType.SERVER);
		}

		if (!me.isHyperVisor()) {
			if (!getNetworkModel().getMachines(MachineType.ADMIN).isEmpty()) {
				NetworkInterfaceModel adminsNic = new MACVLANModel(MachineType.ADMIN.toString(), trunk,
						getNetworkModel().getData().getAdminSubnet(), getNetworkModel().getData().getAdminSubnet());
				
				me.addNetworkInterface(adminsNic);
				this.addVLAN(adminsNic, MachineType.ADMIN);
			}

			if ((!getNetworkModel().getMachines(MachineType.USER).isEmpty())) {
				if (getNetworkModel().getUserDevices().values()
						.stream()
						.filter((dev) -> dev.getNetworkInterfaces() != null) //Only if there are any interfaces, though
						.count() > 0) {
				
					NetworkInterfaceModel usersNic = new MACVLANModel(MachineType.USER.toString(), trunk,
							getNetworkModel().getData().getUserSubnet(), getNetworkModel().getData().getUserSubnet());
					
					me.addNetworkInterface(usersNic);
					this.addVLAN(usersNic, MachineType.USER);
				}
			}

			if (!getNetworkModel().getMachines(MachineType.EXTERNAL_ONLY).isEmpty()) {
				NetworkInterfaceModel extOnlyNic = new MACVLANModel(MachineType.EXTERNAL_ONLY.toString(), trunk,
						getNetworkModel().getData().getExternalSubnet(),
						getNetworkModel().getData().getExternalSubnet());
				
				me.addNetworkInterface(extOnlyNic);
				this.addVLAN(extOnlyNic, MachineType.EXTERNAL_ONLY);
			}

			if (!getNetworkModel().getMachines(MachineType.INTERNAL_ONLY).isEmpty()) {
				NetworkInterfaceModel intOnlyNic = new MACVLANModel(MachineType.INTERNAL_ONLY.toString(), trunk,
						getNetworkModel().getData().getInternalSubnet(),
						getNetworkModel().getData().getInternalSubnet());
				
				me.addNetworkInterface(intOnlyNic);
				this.addVLAN(intOnlyNic, MachineType.INTERNAL_ONLY);
			}

			// if we want a guest network, build one of them, too
			if (getNetworkModel().getData().buildAutoGuest()) {
				NetworkInterfaceModel guestNic = new MACVLANModel(MachineType.GUEST.toString(), trunk,
						getNetworkModel().getData().getGuestSubnet(), getNetworkModel().getData().getGuestSubnet());
				
				me.addNetworkInterface(guestNic);
				this.addVLAN(guestNic, MachineType.GUEST);
			}
		}
		
		return this.getVLANs();
	}
	
	/**
	 * get the VLANs built on this router
	 * @return a Map of NetworkInterfaceModels and their appropriate MachineType, or null if none set
	 */
	public final Map<NetworkInterfaceModel, MachineType> getVLANs() {
		return this.vlans;
	}
	
	/**
	 * Register VLANs on this machine
	 * @param nic the MACVLAN(s) to register
	 */
	private final void addVLAN(NetworkInterfaceModel nic, MachineType type) {
		if (this.vlans == null) {
			this.vlans = new LinkedHashMap<>();
		}
		
		this.vlans.put(nic, type);
	}

	private final void addWANIfaces() throws JsonParsingException, ADataException, IOException {
		final Map<String, NetworkInterfaceData> wanIfaces = getNetworkModel().getData().getNetworkInterfaces(getLabel())
				.get(Direction.WAN);

		// Declare external network interfaces
		wanIfaces.forEach((iface, nic) -> {
			try {
				super.buildIface(nic, true);
			} catch (final InvalidMachineModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}

	private final void addLANIfaces() throws JsonParsingException, ADataException, InvalidServerModelException {
		Map<String, NetworkInterfaceData> lanIfaces = new HashMap<>();

		// Start by building our trunk. This trunk will bond any LAN-facing
		// NICs, or will be a dummy if there aren't any. We'll hang our VLANs off it.
		NetworkInterfaceModel lanTrunk = null;

		try {
			lanIfaces = getNetworkModel().getData().getNetworkInterfaces(getLabel()).get(Direction.LAN);
		} catch (final IOException e) {
			// @TODO: This
			e.printStackTrace();
		}

		// Bond each LAN interface
		if (lanIfaces != null) {
			lanTrunk = new BondModel("bond");
			for (final NetworkInterfaceData ifaceData : lanIfaces.values()) {
				final NetworkInterfaceModel link = new BondInterfaceModel(ifaceData.getIface(), lanTrunk);
				getNetworkModel().getServerModel(getLabel()).addNetworkInterface(link);
			}
		} else {
			lanTrunk = new DummyModel("dummy");
		}

		lanTrunk.setIface("LAN");
	}

	public final ISystemdNetworkd buildBond(String bondName) {
		return null;
	}

	public ADHCPServerProfile getDHCPServer() {
		return this.dhcpServer;
	}

	public ADNSServerProfile getDNSServer() {
		return this.dnsServer;
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

		final FileUnit resolvConf = new FileUnit("leave_my_resolv_conf_alone", "proceed",
				"/etc/dhcp/dhclient-enter-hooks.d/leave_my_resolv_conf_alone");
		units.add(resolvConf);

		resolvConf.appendLine("make_resolv_conf() { :; }");

		units.add(new FilePermsUnit("leave_my_resolv_conf_alone", "leave_my_resolv_conf_alone",
				"/etc/dhcp/dhclient-enter-hooks.d/leave_my_resolv_conf_alone", "755",
				"I couldn't stop various systemd services deciding to override your DNS settings."
						+ " This will cause you intermittent, difficult to diagnose problems as it randomly"
						+ " sets your DNS to wherever it decides. Great for laptops/desktops, atrocious for servers..."));

		// The trunk device needs to be set to Promiscuous mode, or else the Kernel
		// can't see anything running over it.
		// As per
		// https://wiki.archlinux.org/index.php/Network_configuration#Promiscuous_mode
		final FileUnit promiscuousService = new FileUnit("promiscuous_service", "proceed", "/etc/systemd/system/promiscuous@.service",
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
		// Stop our machine from spamming internal ARP requests on our external
		// interface
		// See
		// https://git.kernel.org/pub/scm/linux/kernel/git/davem/net.git/tree/Documentation/networking/ip-sysctl.txt#n1247
		sysctl.appendLine("net.ipv4.conf.all.arp_filter=1");
		sysctl.appendLine("net.ipv4.conf.default.arp_filter=1");
		// Set Reverse Path filtering to "strict"
		// See
		// https://git.kernel.org/pub/scm/linux/kernel/git/davem/net.git/tree/Documentation/networking/ip-sysctl.txt#n1226
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
