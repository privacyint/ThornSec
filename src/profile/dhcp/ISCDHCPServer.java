/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.dhcp;

import java.util.HashSet;
import java.util.Set;

import core.data.machine.AMachineData.Encapsulation;
import core.data.machine.AMachineData.MachineType;
import core.exception.AThornSecException;
import core.exception.data.InvalidIPAddressException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.machine.AMachineModel;
import core.model.machine.ServerModel;
import core.model.machine.configuration.NetworkInterfaceModel;
import core.model.network.NetworkModel;
import core.unit.fs.DirUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddress.IPVersion;
import inet.ipaddr.IPAddressString;

/**
 * Configure and set up our various different networks, and offer IP addresses
 * across (some of) them.
 */
public class ISCDHCPServer extends ADHCPServerProfile {

	public ISCDHCPServer(String label, NetworkModel networkModel) throws AThornSecException {
		super(label, networkModel);
	}

	@Override
	protected Set<IUnit> distributeIPs() throws AThornSecException {
		// Start by iterating through our hypervisors, as we use them as a base for our
		// IP addressing
		int secondOctet = 0;
		for (final String hvLabel : getNetworkModel().getServers(MachineType.HYPERVISOR).keySet()) {
			final ServerModel hv = getNetworkModel().getServerModel(hvLabel);
			int thirdOctet = 0;

			hv.setSecondOctet(++secondOctet);
			hv.setThirdOctet(thirdOctet++);

			int fourthOctet = 0;
			for (final NetworkInterfaceModel nic : hv.getNetworkInterfaces()) {
				if (nic.getAddress() == null) {
					try {
						nic.setAddress(new IPAddressString(hv.getFirstOctet() + "." + hv.getSecondOctet() + "."
								+ hv.getThirdOctet() + "." + (++fourthOctet)).toAddress(IPVersion.IPV4));
					} catch (final AddressStringException e) {
						throw new InvalidIPAddressException(e.getMessage() + " is an invalid IP address");
					}
				}
			}

			// While we're here - HyperVisors have services - let's iterate through those
			// too!
			fourthOctet = 0;
			for (final String serviceLabel : getNetworkModel().getServicesOnHyperVisor(hvLabel)) {
				final ServerModel service = getNetworkModel().getServerModel(serviceLabel);

				service.setSecondOctet(secondOctet);
				service.setThirdOctet(thirdOctet++);

				for (final NetworkInterfaceModel nic : service.getNetworkInterfaces()) {
					if (nic.getAddress() == null) {
						try {
							nic.setAddress(new IPAddressString(service.getFirstOctet() + "." + service.getSecondOctet()
									+ "." + service.getThirdOctet() + "." + (++fourthOctet)).toAddress(IPVersion.IPV4));
						} catch (final AddressStringException e) {
							throw new InvalidIPAddressException(e.getMessage() + " is an invalid IP address");
						}
					}
				}
			}

			// Now let's loop through the devices
		}

		return null;
	}

	@Override
	public Set<IUnit> getInstalled() {
		final Set<IUnit> units = new HashSet<>();

		units.add(new InstalledUnit("dhcp", "proceed", "isc-dhcp-server"));

		return units;
	}

	@Override
	public Set<IUnit> getPersistentConfig() throws InvalidServerModelException {
		final Set<IUnit> units = new HashSet<>();

		// Create sub-dir
		final DirUnit dhcpdConfD = new DirUnit("dhcpd_confd_dir", "dhcp_installed", "/etc/dhcp/dhcpd.conf.d");
		units.add(dhcpdConfD);
		final FileUnit dhcpdConf = new FileUnit("dhcpd_conf", "dhcp_installed", "/etc/dhcp/dhcpd.conf");
		units.add(dhcpdConf);

		dhcpdConf.appendLine("#Options here are set globally across your whole network(s)");
		dhcpdConf
				.appendLine("#Please see https://www.systutorials.com/docs/linux/man/5-dhcpd.conf/\n#for more details");
		dhcpdConf.appendLine("ddns-update-style none;");
		dhcpdConf.appendLine("option domain-name \\\"" + getNetworkModel().getData().getDomain() + "\\\";");
		dhcpdConf.appendLine("option domain-name-servers " + getLabel() + " "
				+ getNetworkModel().getServerModel(getLabel()).getDomain() + ";");
		dhcpdConf.appendLine("default-lease-time 600;");
		dhcpdConf.appendLine("max-lease-time 1800;");
		dhcpdConf.appendLine("authoritative;");
		dhcpdConf.appendLine("log-facility local7;");
		// dhcpdConf.appendLine("use-host-decl-names on;");
		dhcpdConf.appendCarriageReturn();

		for (final String subnet : getSubnets().keySet()) {
			dhcpdConf.appendLine("include \\\"/etc/dhcp/dhcpd.conf.d/" + subnet + ".conf\\\"");
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
	public Set<IUnit> getLiveConfig() throws AThornSecException {
		final Set<IUnit> units = new HashSet<>();

		distributeIPs();

		for (final String subnetName : getSubnets().keySet()) {
			final FileUnit subnetConfig = new FileUnit(subnetName + "_dhcpd_live_config", "dhcp_installed",
					"/etc/dhcp/dhcpd.conf.d/" + subnetName + ".conf");
			units.add(subnetConfig);

			final IPAddress subnet = getGateway(subnetName).withoutPrefixLength();
			final Integer prefix = getGateway(subnetName).getNetworkPrefixLength();
			final IPAddress netmask = subnet.getNetwork().getNetworkMask(prefix, false);

			// Start by telling our DHCP Server about this subnet.
			subnetConfig.appendLine(
					"subnet " + subnet.withoutPrefixLength().toCompressedString() + " netmask " + netmask + " {}");

			// Now let's create our subnet/groups!
			subnetConfig.appendCarriageReturn();
			subnetConfig.appendLine("group " + subnetName + " {");
			subnetConfig.appendLine("\tserver-name \\\"" + subnetName + "." + getLabel() + "."
					+ getNetworkModel().getData().getDomain() + "\\\";");
			subnetConfig.appendLine("\toption routers " + subnet.toCompressedString() + ";");
			subnetConfig.appendLine("\toption domain-name-servers " + subnet.toCompressedString() + ";");

			for (final AMachineModel machine : getMachines(subnetName)) {

				// Skip over ourself, we're a router.
				if (machine.equals(getNetworkModel().getMachineModel(getLabel()))) {
					continue;
				}

				for (final NetworkInterfaceModel iface : machine.getNetworkInterfaces()) {

					// If I don't know its MAC address, I shouldn't attempt to route it.
					if (iface.getMac() == null) {
						continue;
					}

					subnetConfig.appendLine(
							"\thost " + machine.getLabel() + "-" + iface.getMac().toNormalizedString() + " {");
					subnetConfig.appendLine("\t\thardware ethernet " + iface.getMac().toColonDelimitedString() + ";");
					subnetConfig.appendLine("\t\tfixed-address " + iface.getAddress().toCompressedString() + ";");
					subnetConfig.appendLine("\t}");

				}
			}
			subnetConfig.appendLine("}");
		}

		units.add(new RunningUnit("dhcp_running", "isc-dhcp-server", "dhcpd"));

		return units;
	}

	@Override
	public Set<IUnit> getPersistentFirewall() throws AThornSecException {
		// DNS needs to talk on :67 UDP
		getNetworkModel().getServerModel(getLabel()).addListen(Encapsulation.UDP, 67);

		return new HashSet<>();
	}

	@Override
	public Set<IUnit> getLiveFirewall() throws AThornSecException {
		// There aren't any :)
		return new HashSet<>();
	}
}
