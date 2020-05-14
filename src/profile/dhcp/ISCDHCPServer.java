/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.dhcp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import core.StringUtils;
import core.data.machine.AMachineData.MachineType;
import core.exception.AThornSecException;
import core.exception.data.machine.InvalidServerException;
import core.exception.data.machine.configuration.InvalidNetworkInterfaceException;
import core.exception.runtime.InvalidMachineModelException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.machine.ADeviceModel;
import core.model.machine.AMachineModel;
import core.model.machine.ServerModel;
import core.model.machine.configuration.networking.NetworkInterfaceModel;
import core.model.network.NetworkModel;
import core.unit.fs.DirUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.EnabledServiceUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.IncompatibleAddressException;

/**
 * Configure and set up our various different networks, and offer IP addresses
 * across (some of) them.
 */
public class ISCDHCPServer extends ADHCPServerProfile {

	public ISCDHCPServer(String label, NetworkModel networkModel) throws AThornSecException {
		super(label, networkModel);

		getNetworkModel().getServerModel(getLabel()).addProcessString("/usr/sbin/dhcpd -4 -q -cf /etc/dhcp/dhcpd.conf");
	}

	private void buildNet(MachineType type)
			throws InvalidServerException {
		// First IP belongs to this net's router, so start from there (as it's assigned)
		IPAddress ip = new IPAddressString(getNetworkModel().getData().getSubnet(type)).getAddress().getLowerNonZeroHost();

		addSubnet(type.toString(), getSubnet(type.toString()));
		addToSubnet(type.toString(), getNetworkModel().getMachines(type).values());

		for (final AMachineModel machine : getNetworkModel().getMachines(type).values()) {

			try {
				if (getNetworkModel().getServerModel(machine.getLabel()).isRouter()) {
					continue;
				}
			} catch (final InvalidServerModelException e) {
				// It's not a server, so can't possibly be a Router
			}

			for (final NetworkInterfaceModel nic : machine.getNetworkInterfaces()) {
				// DHCP servers distribute IP addresses, correct? :)
				if (nic.getAddresses() == null) {
					ip = ip.increment(1);
					nic.addAddress(ip);
				}
			}
		}
	}

	/**
	 * Check whether a given machine has a MAC address set for each of its
	 * interfaces.
	 *
	 * @param machine
	 * @param isRequired true if you want it to throw an Exception
	 * @return true if all interfaces have MAC adddresses, false otherwise.
	 * @throws InvalidNetworkInterfaceException
	 */
	private Boolean checkMACs(AMachineModel machine, Boolean isRequired) throws InvalidNetworkInterfaceException {
		for (final NetworkInterfaceModel nic : machine.getNetworkInterfaces()) {
			if (nic.getMac() == null) {
				if (isRequired) {
					throw new InvalidNetworkInterfaceException("Network interface " + nic.getIface() + " on "
							+ machine.getLabel() + " requires a MAC address to be set.");
				} else {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	protected void distributeMACs() throws AThornSecException {
		final Boolean isRouterHV = getNetworkModel().getServerModel(getLabel()).isHyperVisor();

		// Start by checking all of the devices have a MAC address provided, as these
		// are physical devices!
		for (final ADeviceModel device : getNetworkModel().getDevices().values()) {
			checkMACs(device, !isRouterHV);
		}

		// Iterate through our dedi machines, these are also physical machines
		for (final ServerModel server : getNetworkModel().getServers(MachineType.DEDICATED).values()) {
			checkMACs(server, !isRouterHV);
		}

		// Iterate through our HyperVisor machines, these are also physical machines
		for (final ServerModel server : getNetworkModel().getServers(MachineType.HYPERVISOR).values()) {
			checkMACs(server, !isRouterHV);
		}

		// Finally, iterate through our services, filling in any gaps.
		// TODO: tidy up this loopy mess?
		for (final ServerModel server : getNetworkModel().getServers(MachineType.SERVICE).values()) {
			if (checkMACs(server, false) == false) {
				for (final NetworkInterfaceModel nic : server.getNetworkInterfaces()) {
					if (nic.getMac() == null) {
						nic.setMac(server.generateMAC(nic.getIface()));
					}
				}
			}
		}
	}

	@Override
	public Collection<IUnit> getInstalled() {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("dhcp", "proceed", "isc-dhcp-server"));

		return units;
	}

	private void buildPersistentNets()
			throws InvalidServerException, IncompatibleAddressException, InvalidServerModelException {
		
		for (MachineType type : MachineType.values()) {
			if (getNetworkModel().getMachines(type) != null && !getNetworkModel().getMachines(type).isEmpty()) {
				buildNet(type);
			}
		}
		
		/*
		if (!getNetworkModel().getMachines(MachineType.SERVER).isEmpty()) {
			buildNet(MachineType.SERVER.toString(),
					new IPAddressString(getNetworkModel().getData().getServerSubnet()).getAddress(),
					getNetworkModel().getMachines(MachineType.SERVER).values());
		}

		if (!getNetworkModel().getMachines(MachineType.USER).isEmpty()
				&& !getNetworkModel().getServerModel(getLabel()).isHyperVisor()) {
			buildNet(MachineType.USER.toString(),
					new IPAddressString(getNetworkModel().getData().getUserSubnet()).getAddress(),
					getNetworkModel().getMachines(MachineType.USER).values());
		}

		if (!getNetworkModel().getMachines(MachineType.ADMIN).isEmpty()) {
			buildNet(MachineType.ADMIN.toString(),
					new IPAddressString(getNetworkModel().getData().getAdminSubnet()).getAddress(),
					getNetworkModel().getMachines(MachineType.ADMIN).values());
		}

		if (!getNetworkModel().getMachines(MachineType.INTERNAL_ONLY).isEmpty()) {
			buildNet(MachineType.INTERNAL_ONLY.toString(),
					new IPAddressString(getNetworkModel().getData().getInternalSubnet()).getAddress(),
					getNetworkModel().getMachines(MachineType.INTERNAL_ONLY).values());
		}

		if (!getNetworkModel().getMachines(MachineType.EXTERNAL_ONLY).isEmpty()) {
			buildNet(MachineType.EXTERNAL_ONLY.toString(),
					new IPAddressString(getNetworkModel().getData().getExternalSubnet()).getAddress(),
					getNetworkModel().getMachines(MachineType.EXTERNAL_ONLY).values());
		}
	}

	private FileUnit getDHCPConf() throws InvalidServerModelException {
		final FileUnit dhcpdConf = new FileUnit("dhcpd_conf", "dhcp_installed", "/etc/dhcp/dhcpd.conf");

		dhcpdConf.appendLine("#Options here are set globally across your whole network(s)");
		dhcpdConf.appendLine("#Please see https://www.systutorials.com/docs/linux/man/5-dhcpd.conf/");
		dhcpdConf.appendLine("#for more details");
		dhcpdConf.appendLine("ddns-update-style none;");
		dhcpdConf.appendLine("option domain-name \\\"" + getNetworkModel().getData().getDomain() + "\\\";");
		dhcpdConf.appendLine("option domain-name-servers " + getLabel() + "."
				+ getNetworkModel().getServerModel(getLabel()).getDomain() + ";");
		dhcpdConf.appendLine("default-lease-time 600;");
		dhcpdConf.appendLine("max-lease-time 1800;");
		dhcpdConf.appendLine("get-lease-hostnames true;");
		dhcpdConf.appendLine("authoritative;");
		dhcpdConf.appendLine("log-facility local7;");
		dhcpdConf.appendCarriageReturn();

		for (final String subnet : getSubnets().keySet()) {
			dhcpdConf.appendLine("include \\\"/etc/dhcp/dhcpd.conf.d/" + subnet + ".conf\\\";");
		}

		if (getNetworkModel().getData().buildAutoGuest()) {
			dhcpdConf.appendLine("include \\\"/etc/dhcp/dhcpd.conf.d/Guests.conf\\\";");
		}

		return dhcpdConf;
	}

	private FileUnit getDHCPListenInterfaces() {
		final FileUnit dhcpdListen = new FileUnit("dhcpd_defiface", "dhcp_installed", "/etc/default/isc-dhcp-server");

		dhcpdListen.appendText("INTERFACESv4=\\\"");
		dhcpdListen.appendText(getSubnets().keySet().stream().map(Object::toString).collect(Collectors.joining(" ")));
		dhcpdListen.appendText("\\\"");

		return dhcpdListen;
	}

	@Override
	public Collection<IUnit> getPersistentConfig() throws IncompatibleAddressException, AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		// Create config drop-in dir
		units.add(new DirUnit("dhcpd_confd_dir", "dhcp_installed", "/etc/dhcp/dhcpd.conf.d"));

		buildPersistentNets();
		distributeMACs();

		units.add(getDHCPConf());
		units.add(getDHCPListenInterfaces());

		return units;
	}

	private FileUnit buildSubNet(String net) throws InvalidMachineModelException {
		final FileUnit subnetConfig = new FileUnit(net + "_dhcpd_live_config", "dhcp_installed",
				"/etc/dhcp/dhcpd.conf.d/" + net + ".conf");

		final IPAddress subnet = getSubnet(net);
		final Integer prefix = getSubnet(net).getNetworkPrefixLength();
		final IPAddress netmask = getSubnet(net).getNetwork().getNetworkMask(prefix, false);
		final String gateway = subnet.getLowerNonZeroHost().withoutPrefixLength().toCompressedString();

		// Start by telling our DHCP Server about this subnet.
		subnetConfig.appendLine("subnet " + gateway + " netmask " + netmask + " {}");

		// Now let's create our subnet/groups!
		subnetConfig.appendCarriageReturn();
		subnetConfig.appendLine("group " + net.toLowerCase() + " {");
		subnetConfig.appendLine("\tserver-name \\\"" + net.toLowerCase() + "." + getLabel() + "." + getNetworkModel().getData().getDomain() + "\\\";");
		subnetConfig.appendLine("\toption routers " + gateway + ";");
		subnetConfig.appendLine("\toption domain-name-servers " + gateway + ";");
		subnetConfig.appendCarriageReturn();

		for (final AMachineModel machine : getMachines(net)) {

			// Skip over ourself, we're a router.
			if (machine.equals(getNetworkModel().getMachineModel(getLabel()))) {
				continue;
			}

			for (final NetworkInterfaceModel iface : machine.getNetworkInterfaces()) {
				// We check the requirement elsewhere. Don't try and build non-machine leases
				if (iface.getMac() == null) {
					continue;
				}

				assert (iface.getAddresses().size() == 1);
				final IPAddress ip = (IPAddress) iface.getAddresses().toArray()[0];

				subnetConfig
						.appendLine("\thost " + StringUtils.stringToAlphaNumeric(machine.getLabel().toLowerCase(), "-")
								+ "-" + iface.getMac().toHexString(false) + " {");
				subnetConfig.appendLine("\t\thardware ethernet " + iface.getMac().toColonDelimitedString() + ";");

				subnetConfig.appendLine("\t\tfixed-address " + ip.withoutPrefixLength().toCompressedString() + ";");
				subnetConfig.appendLine("\t}");
				subnetConfig.appendCarriageReturn();

			}
		}
		subnetConfig.appendLine("}");

		return subnetConfig;
	}

	@Override
	public Collection<IUnit> getLiveConfig() throws AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		for (final String subnet : getSubnets().keySet()) {
			units.add(buildSubNet(subnet));
		}

		// @TODO: guest networking
		if (getNetworkModel().getData().buildAutoGuest()) {
			final FileUnit guestConfig = new FileUnit("guest_dhcpd_live_config", "dhcp_installed",
					"/etc/dhcp/dhcpd.conf.d/Guests.conf");
			units.add(guestConfig);

			final IPAddress subnet = new IPAddressString(getNetworkModel().getData().getSubnet(MachineType.GUEST)).getAddress();

			guestConfig.appendLine("group Guests {");
			guestConfig.appendLine("\tsubnet " + subnet.getLower().withoutPrefixLength() + " netmask "
					+ subnet.getNetwork().getNetworkMask(subnet.getPrefixLength(), false) + " {");
			guestConfig.appendLine("\t\tpool {");
			guestConfig.appendLine("\t\t\trange " + subnet.getLower().withoutPrefixLength() + " "
					+ subnet.getUpper().withoutPrefixLength() + ";");
			guestConfig.appendLine("\t\t\toption routers " + subnet.getLowerNonZeroHost().withoutPrefixLength() + ";");
			guestConfig.appendLine(
					"\t\t\toption domain-name-servers " + subnet.getLowerNonZeroHost().withoutPrefixLength() + ";");
			guestConfig.appendLine("\t\t\tdeny known-clients;");
			guestConfig.appendLine("\t\t\tallow unknown-clients;");
			guestConfig.appendLine("\t\t}");
			guestConfig.appendLine("\t}");
			guestConfig.appendLine("}");
		}

		units.add(new EnabledServiceUnit("dhcp", "isc-dhcp-server",
				"I couldn't enable your DHCP server to start at boot"));
		units.add(new RunningUnit("dhcp", "isc-dhcp-server", "dhcpd"));

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws AThornSecException {
		return new ArrayList<>();
	}

	@Override
	public Collection<IUnit> getLiveFirewall() throws AThornSecException {
		// There aren't any :)
		return new ArrayList<>();
	}
}
