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
import core.exception.data.machine.InvalidServerException;
import core.exception.data.machine.configuration.InvalidNetworkInterfaceException;
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
	}

	private void buildNet(String network, IPAddress subnet, Collection<AMachineModel> machines) throws InvalidServerException {
		// First IP belongs to this net's router, so start from there (as it's assigned)
		IPAddress ip = subnet.getLowerNonZeroHost();

		addSubnet(network, subnet);
		addToSubnet(network, machines);

		for (final AMachineModel machine : machines) {

			try {
				if (getNetworkModel().getServerModel(machine.getLabel()).isRouter()) {
					continue;
				}
			} catch (final InvalidServerModelException e) {
				// It's not a server, so can't possibly be a Router
			}

			for (final NetworkInterfaceModel nic : machine.getNetworkInterfaces().values()) {
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
		for (final NetworkInterfaceModel nic : machine.getNetworkInterfaces().values()) {
			if (nic.getMac() == null) {
				if (isRequired) {
					throw new InvalidNetworkInterfaceException("Network interface " + nic.getIface() + " on " + machine.getLabel() + " requires a MAC address to be set.");
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

		// Finally, iterate through our services, filling in any gaps.
		// TODO: tidy up this loopy mess?
		for (final ServerModel server : getNetworkModel().getServers(MachineType.SERVICE).values()) {
			if (checkMACs(server, false) == false) {
				for (final NetworkInterfaceModel nic : server.getNetworkInterfaces().values()) {
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

	@Override
	public Collection<IUnit> getPersistentConfig() throws IncompatibleAddressException, AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		buildNet(MachineType.SERVER.toString(), new IPAddressString(getNetworkModel().getData().getServerSubnet()).getAddress(),
				getNetworkModel().getMachines(MachineType.SERVER).values());
		buildNet(MachineType.USER.toString(), new IPAddressString(getNetworkModel().getData().getUserSubnet()).getAddress(),
				getNetworkModel().getMachines(MachineType.USER).values());
		buildNet(MachineType.ADMIN.toString(), new IPAddressString(getNetworkModel().getData().getAdminSubnet()).getAddress(),
				getNetworkModel().getMachines(MachineType.ADMIN).values());
		buildNet(MachineType.INTERNAL_ONLY.toString(), new IPAddressString(getNetworkModel().getData().getInternalSubnet()).getAddress(),
				getNetworkModel().getMachines(MachineType.INTERNAL_ONLY).values());
		buildNet(MachineType.EXTERNAL_ONLY.toString(), new IPAddressString(getNetworkModel().getData().getExternalSubnet()).getAddress(),
				getNetworkModel().getMachines(MachineType.EXTERNAL_ONLY).values());
		// TODO: Guest network pool
		distributeMACs();

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
		dhcpdConf.appendLine("option domain-name-servers " + getLabel() + "." + getNetworkModel().getServerModel(getLabel()).getDomain() + ";");
		dhcpdConf.appendLine("default-lease-time 600;");
		dhcpdConf.appendLine("max-lease-time 1800;");
		dhcpdConf.appendLine("get-lease-hostnames true;");
		dhcpdConf.appendLine("authoritative;");
		dhcpdConf.appendLine("log-facility local7;");
		// dhcpdConf.appendLine("use-host-decl-names on;");
		dhcpdConf.appendCarriageReturn();

		for (final String subnet : getSubnets().keySet()) {
			dhcpdConf.appendLine("include \\\"/etc/dhcp/dhcpd.conf.d/" + subnet + ".conf\\\";");
		}

		final FileUnit dhcpdListen = new FileUnit("dhcpd_defiface", "dhcp_installed", "/etc/default/isc-dhcp-server");
		units.add(dhcpdListen);

		dhcpdListen.appendText("INTERFACESv4=\\\"");
		for (final String subnet : getSubnets().keySet()) {
			dhcpdListen.appendText(" " + subnet);
		}
		dhcpdListen.appendLine("\\\"");

		getNetworkModel().getServerModel(getLabel()).addProcessString("/usr/sbin/dhcpd -4 -q -cf /etc/dhcp/dhcpd.conf");

		return units;
	}

	@Override
	public Collection<IUnit> getLiveConfig() throws AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		for (final String subnetName : getSubnets().keySet()) {
			final FileUnit subnetConfig = new FileUnit(subnetName + "_dhcpd_live_config", "dhcp_installed", "/etc/dhcp/dhcpd.conf.d/" + subnetName + ".conf");
			units.add(subnetConfig);

			final IPAddress subnet = getSubnet(subnetName);
			final Integer prefix = getSubnet(subnetName).getNetworkPrefixLength();
			final IPAddress netmask = getSubnet(subnetName).getNetwork().getNetworkMask(prefix, false);

			// Start by telling our DHCP Server about this subnet.
			subnetConfig.appendLine("subnet " + subnet.getLower().withoutPrefixLength().toCompressedString() + " netmask " + netmask + " {}");

			// Now let's create our subnet/groups!
			subnetConfig.appendCarriageReturn();
			subnetConfig.appendLine("group " + subnetName.toLowerCase() + " {");
			subnetConfig.appendLine("\tserver-name \\\"" + subnetName.toLowerCase() + "." + getLabel() + "." + getNetworkModel().getData().getDomain() + "\\\";");
			// wait, wut?
			subnetConfig.appendLine("\toption routers " + subnet.getLowerNonZeroHost().withoutPrefixLength() + ";");
			subnetConfig.appendLine("\toption domain-name-servers " + subnet.getLowerNonZeroHost().withoutPrefixLength() + ";");
			subnetConfig.appendCarriageReturn();

			for (final AMachineModel machine : getMachines(subnetName)) {

				// Skip over ourself, we're a router.
				if (machine.equals(getNetworkModel().getMachineModel(getLabel()))) {
					continue;
				}

				for (final NetworkInterfaceModel iface : machine.getNetworkInterfaces().values()) {

					assert (iface.getMac() != null);
					assert (iface.getAddresses().size() == 1);
					final IPAddress ip = (IPAddress) iface.getAddresses().toArray()[0];

					subnetConfig.appendLine("\thost " + StringUtils.stringToAlphaNumeric(machine.getLabel().toLowerCase(), "-") + "-" + iface.getMac().toHexString(false) + " {");
					subnetConfig.appendLine("\t\thardware ethernet " + iface.getMac().toColonDelimitedString() + ";");

					subnetConfig.appendLine("\t\tfixed-address " + ip.withoutPrefixLength().toCompressedString() + ";");
					subnetConfig.appendLine("\t}");
					subnetConfig.appendCarriageReturn();

				}
			}
			subnetConfig.appendLine("}");
		}

		units.add(new EnabledServiceUnit("dhcp", "isc-dhcp-server", "I couldn't enable your DHCP server to start at boot"));
		units.add(new RunningUnit("dhcp", "isc-dhcp-server", "dhcpd"));

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
