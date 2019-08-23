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

import core.data.machine.AMachineData.Encapsulation;
import core.data.machine.AMachineData.MachineType;
import core.exception.AThornSecException;
import core.exception.data.InvalidIPAddressException;
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
import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.MACAddressString;
import inet.ipaddr.mac.MACAddress;
import profile.type.Router;

/**
 * Configure and set up our various different networks, and offer IP addresses
 * across (some of) them.
 */
public class ISCDHCPServer extends ADHCPServerProfile {

	public ISCDHCPServer(String label, NetworkModel networkModel) throws AThornSecException {
		super(label, networkModel);
	}

	@Override
	protected void distributeIPs() throws AThornSecException {
		// TODO: Refactor this method, as it's a bit loopy

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
				// DHCP servers distribute IP addresses, correct? :)
				if (nic.getAddress() == null) {
					try {
						nic.setAddress(new IPAddressString(hv.getFirstOctet() + "." + hv.getSecondOctet() + "."
								+ hv.getThirdOctet() + "." + (++fourthOctet)).toAddress());
					} catch (final AddressStringException e) {
						throw new InvalidIPAddressException(e.getMessage() + " is an invalid IP address");
					}
				}

				if (nic.getMac() == null) {
					throw new InvalidNetworkInterfaceException("Network interface " + nic.getIface() + " on "
							+ getLabel() + " requires a MAC address to be set.");
				}
			}

			// While we're here - HyperVisors have services - let's iterate through those
			// too!
			for (final String serviceLabel : getNetworkModel().getServicesOnHyperVisor(hvLabel)) {
				final ServerModel service = getNetworkModel().getServerModel(serviceLabel);

				service.setSecondOctet(secondOctet);
				service.setThirdOctet(thirdOctet++);
				fourthOctet = 0;

				for (final NetworkInterfaceModel nic : service.getNetworkInterfaces()) {
					// DHCP continues to give out addresses
					if (nic.getAddress() == null) {
						try {
							nic.setAddress(new IPAddressString(service.getFirstOctet() + "." + service.getSecondOctet()
									+ "." + service.getThirdOctet() + "." + (++fourthOctet)).toAddress());
						} catch (final AddressStringException e) {
							throw new InvalidIPAddressException(e.getMessage() + " is an invalid IP address");
						}
					}
				}
			}

			// Dedicated servers
			for (final ServerModel dedi : getNetworkModel().getServers(MachineType.DEDICATED).values()) {
				dedi.setSecondOctet(++secondOctet);
				dedi.setThirdOctet(0);
				fourthOctet = 0;

				for (final NetworkInterfaceModel nic : dedi.getNetworkInterfaces()) {
					// DHCP continues to give out addresses
					if (nic.getAddress() == null) {
						try {
							nic.setAddress(new IPAddressString(dedi.getFirstOctet() + "." + dedi.getSecondOctet() + "."
									+ dedi.getThirdOctet() + "." + (++fourthOctet)).toAddress());
						} catch (final AddressStringException e) {
							throw new InvalidIPAddressException(e.getMessage() + " is an invalid IP address");
						}
					}
				}
			}

			/*
			 * public final static String USERS_NETWORK = "172.16.0.1/16"; public final
			 * static String ADMINS_NETWORK = "172.20.0.1/16"; public final static String
			 * INTERNALS_NETWORK = "172.24.0.1/16"; public final static String
			 * EXTERNALS_NETWORK = "172.28.0.1/16"; public final static String
			 * AUTOGUEST_NETWORK = "172.31.0.1/16";
			 */
			// Now let's loop through the devices
			IPAddress ip = new IPAddressString(Router.USERS_NETWORK).getAddress();
			for (final ADeviceModel device : getNetworkModel().getDevices(MachineType.USER).values()) {
				for (final NetworkInterfaceModel nic : device.getNetworkInterfaces()) {
					if (nic.getAddress() == null) {
						ip = ip.increment(1);

						nic.setAddress(ip);
					}
				}
			}

			ip = new IPAddressString(Router.ADMINS_NETWORK).getAddress();
			for (final ADeviceModel device : getNetworkModel().getDevices(MachineType.ADMIN).values()) {
				for (final NetworkInterfaceModel nic : device.getNetworkInterfaces()) {
					if (nic.getAddress() == null) {
						ip = ip.increment(1);

						nic.setAddress(ip);
					}
				}
			}

			ip = new IPAddressString(Router.INTERNALS_NETWORK).getAddress();
			for (final ADeviceModel device : getNetworkModel().getDevices(MachineType.INTERNAL_ONLY).values()) {
				for (final NetworkInterfaceModel nic : device.getNetworkInterfaces()) {
					if (nic.getAddress() == null) {
						ip = ip.increment(1);

						nic.setAddress(ip);
					}
				}
			}

			ip = new IPAddressString(Router.EXTERNALS_NETWORK).getAddress();
			for (final ADeviceModel device : getNetworkModel().getDevices(MachineType.EXTERNAL_ONLY).values()) {
				for (final NetworkInterfaceModel nic : device.getNetworkInterfaces()) {
					if (nic.getAddress() == null) {
						ip = ip.increment(1);

						nic.setAddress(ip);
					}
				}
			}

			// TODO: Guest network pool
		}
	}

	@Override
	protected void distributeMACs() throws AThornSecException {
		for (final ServerModel dedi : getNetworkModel().getServers(MachineType.DEDICATED).values()) {
			for (final NetworkInterfaceModel nic : dedi.getNetworkInterfaces()) {
				if (nic.getMac() == null) {
					throw new InvalidNetworkInterfaceException("Network interface " + nic.getIface() + " on "
							+ getLabel() + " requires a MAC address to be set.");
				}
			}
		}

		// Start by iterating through our hypervisors, as we use them as a base for our
		// Services' MAC addressing
		for (final ServerModel hv : getNetworkModel().getServers(MachineType.HYPERVISOR).values()) {

			// If we're also a router, it doesn't matter, because it doesn't matter what
			// Routers' MAC addresses are.
			if (hv.isRouter()) {
				continue;
			}

			// This is a physical machine, we need to know what its physical MAC addresses
			// are...
			for (final NetworkInterfaceModel nic : hv.getNetworkInterfaces()) {
				if (nic.getMac() == null) {
					throw new InvalidNetworkInterfaceException("Network interface " + nic.getIface() + " on "
							+ getLabel() + " requires a MAC address to be set.");
				}
			}

			// Quickly iterate its services - these ones may be null...
			for (final String serviceLabel : getNetworkModel().getServicesOnHyperVisor(hv.getLabel())) {
				final ServerModel service = getNetworkModel().getServerModel(serviceLabel);

				for (final NetworkInterfaceModel nic : service.getNetworkInterfaces()) {
					if (nic.getMac() == null) {
						final String macString = "080027".concat(nic.getAddress().toHexString(false).substring(2));
						final MACAddress mac = new MACAddressString(macString).getAddress();
						nic.setMac(mac);
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
	public Collection<IUnit> getPersistentConfig() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

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
	public Collection<IUnit> getLiveConfig() throws AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		distributeIPs();
		distributeMACs();

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
			subnetConfig.appendCarriageReturn();

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
							"\thost " + machine.getLabel() + "-" + iface.getMac().toHexString(false) + " {");
					subnetConfig.appendLine("\t\thardware ethernet " + iface.getMac().toColonDelimitedString() + ";");
					subnetConfig
							.appendLine("\t\tfixed-address " + iface.getAddress().toCompressedWildcardString() + ";");
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
