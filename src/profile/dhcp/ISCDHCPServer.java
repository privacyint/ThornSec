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

import core.StringUtils;
import core.data.machine.AMachineData.Encapsulation;
import core.data.machine.AMachineData.MachineType;
import core.exception.AThornSecException;
import core.exception.data.machine.configuration.InvalidNetworkInterfaceException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.machine.ADeviceModel;
import core.model.machine.AMachineModel;
import core.model.machine.ServerModel;
import core.model.machine.configuration.NetworkInterfaceModel;
import core.model.network.NetworkModel;
import core.unit.fs.DirUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import profile.type.Router;

/**
 * Configure and set up our various different networks, and offer IP addresses
 * across (some of) them.
 */
public class ISCDHCPServer extends ADHCPServerProfile {

	public ISCDHCPServer(String label, NetworkModel networkModel) throws AThornSecException {
		super(label, networkModel);
	}

	private void buildNet(IPAddress subnet, Collection<AMachineModel> machines) {
		// First IP belongs to this net's router, so skip over
		IPAddress ip = subnet.getLowerNonZeroHost().increment(1);

		for (final AMachineModel machine : machines) {
			for (final NetworkInterfaceModel nic : machine.getNetworkInterfaces()) {
				// DHCP servers distribute IP addresses, correct? :)
				if (nic.getAddress() == null) {
					ip = ip.increment(1);
					nic.setAddress(ip);
				}
			}
		}
	}

	@Override
	protected void distributeIPs() throws AThornSecException {

		buildNet(new IPAddressString(Router.SERVERS_NETWORK).getAddress(),
				getNetworkModel().getMachines(MachineType.SERVER).values());
		buildNet(new IPAddressString(Router.USERS_NETWORK).getAddress(),
				getNetworkModel().getMachines(MachineType.USER).values());
		buildNet(new IPAddressString(Router.ADMINS_NETWORK).getAddress(),
				getNetworkModel().getMachines(MachineType.ADMIN).values());
		buildNet(new IPAddressString(Router.INTERNALS_NETWORK).getAddress(),
				getNetworkModel().getMachines(MachineType.INTERNAL_ONLY).values());
		buildNet(new IPAddressString(Router.EXTERNALS_NETWORK).getAddress(),
				getNetworkModel().getMachines(MachineType.EXTERNAL_ONLY).values());
		// TODO: Guest network pool
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
							+ getLabel() + " requires a MAC address to be set.");
				} else {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	protected void distributeMACs() throws AThornSecException {

		// Start by checking all of the devices have a MAC address provided, as these
		// are physical devices!
		for (final ADeviceModel device : getNetworkModel().getDevices().values()) {
			checkMACs(device, true);
		}

		// Iterate through our dedi machines, these are also physical machines
		for (final ServerModel server : getNetworkModel().getServers(MachineType.DEDICATED).values()) {
			checkMACs(server, true);
		}

		// Iterate through our HyperVisor machines, these are also physical machines
		for (final ServerModel server : getNetworkModel().getServers(MachineType.HYPERVISOR).values()) {
			if (server.isRouter()) {
				continue; // We don't care
			} else {
				checkMACs(server, true);
			}
		}

		// Finally, iterate through our services.
		for (final ServerModel server : getNetworkModel().getServers(MachineType.SERVICE).values()) {
			if (checkMACs(server, false) == false) {
				server.generateMAC();
			}
		}
	}

	@Override
	public Collection<IUnit> getInstalled() {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("dhcp", "proceed", "isc-dhcp-server"));

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentConfig() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		// Create sub-dir
		final DirUnit dhcpdConfD = new DirUnit("dhcpd_confd_dir", "dhcp_installed", "/etc/dhcp/dhcpd.conf.d");
		units.add(dhcpdConfD);
		final FileUnit dhcpdConf = new FileUnit("dhcpd_conf", "dhcp_installed", "/etc/dhcp/dhcpd.conf");
		units.add(dhcpdConf);

		dhcpdConf.appendLine("#Options here are set globally across your whole network(s)");
		dhcpdConf.appendLine("#Please see https://www.systutorials.com/docs/linux/man/5-dhcpd.conf/");
		dhcpdConf.appendLine("#for more details");
		dhcpdConf.appendLine("ddns-update-style none;");
		dhcpdConf.appendLine("option domain-name \\\"" + getNetworkModel().getData().getDomain() + "\\\";");
		dhcpdConf.appendLine("option domain-name-servers " + getLabel() + "."
				+ getNetworkModel().getServerModel(getLabel()).getDomain() + ";");
		dhcpdConf.appendLine("default-lease-time 600;");
		dhcpdConf.appendLine("max-lease-time 1800;");
		dhcpdConf.appendLine("authoritative;");
		dhcpdConf.appendLine("log-facility local7;");
		// dhcpdConf.appendLine("use-host-decl-names on;");
		dhcpdConf.appendCarriageReturn();

		for (final String subnet : getSubnets().keySet()) {
			dhcpdConf.appendLine("include \\\"/etc/dhcp/dhcpd.conf.d/" + subnet + ".conf\\\";");
		}

		final FileUnit dhcpdListen = new FileUnit("dhcpd_defiface", "dhcp_installed", "/etc/default/isc-dhcp-server");
		units.add(dhcpdListen);

		dhcpdListen.appendLine("INTERFACES=\\\"", false);
		for (final String subnet : getSubnets().keySet()) {
			dhcpdListen.appendLine(" " + subnet, false);
		}
		dhcpdListen.appendLine("\\\"");

		getNetworkModel().getServerModel(getLabel()).addProcessString("/usr/sbin/dhcpd -4 -q -cf /etc/dhcp/dhcpd.conf");

		return units;
	}

	@Override
	public Collection<IUnit> getLiveConfig() throws AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		distributeIPs();
		distributeMACs();

		for (final String subnetName : getSubnets().keySet()) {
			final FileUnit subnetConfig = new FileUnit(subnetName + "_dhcpd_live_config", "dhcp_installed",
					"/etc/dhcp/dhcpd.conf.d/" + subnetName + ".conf");
			units.add(subnetConfig);

			final IPAddress subnet = getGateway(subnetName);
			final IPAddress gateway = subnet.getLowerNonZeroHost().withoutPrefixLength();
			final Integer prefix = subnet.getNetworkPrefixLength();
			final IPAddress netmask = subnet.getNetwork().getNetworkMask(prefix, false);

			// Start by telling our DHCP Server about this subnet.
			subnetConfig.appendLine("subnet " + subnet.getLower().withoutPrefixLength().toCompressedString()
					+ " netmask " + netmask + " {}");

			// Now let's create our subnet/groups!
			subnetConfig.appendCarriageReturn();
			subnetConfig.appendLine("group " + subnetName + " {");
			subnetConfig.appendLine("\tserver-name \\\"" + subnetName + "." + getLabel() + "."
					+ getNetworkModel().getData().getDomain() + "\\\";");
			subnetConfig.appendLine("\toption routers " + gateway.toCompressedString() + ";");
			subnetConfig.appendLine("\toption domain-name-servers " + gateway.toCompressedString() + ";");
			subnetConfig.appendCarriageReturn();

			for (final AMachineModel machine : getMachines(subnetName)) {

				// Skip over ourself, we're a router.
				if (machine.equals(getNetworkModel().getMachineModel(getLabel()))) {
					continue;
				}

				for (final NetworkInterfaceModel iface : machine.getNetworkInterfaces()) {

					assert (iface.getMac() != null);

					subnetConfig.appendLine("\thost " + StringUtils.stringToAlphaNumeric(machine.getLabel()) + "-"
							+ iface.getMac().toHexString(false) + " {");
					subnetConfig.appendLine("\t\thardware ethernet " + iface.getMac().toColonDelimitedString() + ";");
					subnetConfig.appendLine("\t\tfixed-address " + iface.getAddress().toCompressedString() + ";");
					subnetConfig.appendLine("\t}");
					subnetConfig.appendCarriageReturn();

				}
			}
			subnetConfig.appendLine("}");
		}

		units.add(new RunningUnit("dhcp_running", "isc-dhcp-server", "dhcpd"));

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws AThornSecException {
		// DNS needs to talk on :67 UDP
		getNetworkModel().getServerModel(getLabel()).addListen(Encapsulation.UDP, 67);

		return new ArrayList<>();
	}

	@Override
	public Collection<IUnit> getLiveFirewall() throws AThornSecException {
		// There aren't any :)
		return new ArrayList<>();
	}
}
