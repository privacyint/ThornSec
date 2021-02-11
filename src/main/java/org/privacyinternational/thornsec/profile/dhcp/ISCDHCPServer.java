/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.profile.dhcp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import org.privacyinternational.thornsec.core.data.machine.AMachineData.MachineType;
import org.privacyinternational.thornsec.core.data.machine.configuration.TrafficRule.Encapsulation;
import org.privacyinternational.thornsec.core.exception.AThornSecException;
import org.privacyinternational.thornsec.core.exception.data.InvalidIPAddressException;
import org.privacyinternational.thornsec.core.exception.data.InvalidPortException;
import org.privacyinternational.thornsec.core.exception.data.machine.configuration.InvalidNetworkInterfaceException;
import org.privacyinternational.thornsec.core.iface.IUnit;
import org.privacyinternational.thornsec.core.model.machine.AMachineModel;
import org.privacyinternational.thornsec.core.model.machine.ServerModel;
import org.privacyinternational.thornsec.core.model.machine.configuration.networking.NetworkInterfaceModel;
import org.privacyinternational.thornsec.core.unit.fs.DirUnit;
import org.privacyinternational.thornsec.core.unit.fs.FileUnit;
import org.privacyinternational.thornsec.core.unit.pkg.EnabledServiceUnit;
import org.privacyinternational.thornsec.core.unit.pkg.InstalledUnit;
import org.privacyinternational.thornsec.core.unit.pkg.RunningUnit;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IncompatibleAddressException;

/**
 * Configure and set up our various different networks, and offer IP addresses
 * across (some of) them.
 * https://linux.die.net/man/8/dhcpd
 */
public class ISCDHCPServer extends ADHCPServerProfile {

	/**
	 * Initialise a new ISC DHCP Server	on a given Server
	 * @param me The ServerModel which is to be offering IP addresses (generally
	 * 			 assumed to be our Router, but that's not *necessarily* true
	 * @throws AThornSecException
	 */
	public ISCDHCPServer(ServerModel me) throws AThornSecException {
		super(me);

		me.addProcessString("/usr/sbin/dhcpd -4 -q -cf /etc/dhcp/dhcpd.conf");
	}

	/**
	 * Builds a given subnet
	 * @param type The Machine type you're building the subnet for
	 * @throws InvalidIPAddressException if an invalid IP address is assigned
	 */
	private void buildNet(MachineType type) throws InvalidIPAddressException {
		// First IP belongs to this net's router, so start from there (as it's assigned)
		IPAddress ip = getNetworkModel().getSubnet(type).getLowerNonZeroHost();

		addSubnet(type, getSubnet(type));
		addToSubnet(type, getNetworkModel().getMachines(type));

		for (final AMachineModel machine : getNetworkModel().getMachines(type)) {

			if (machine.isType(MachineType.ROUTER) ||
				machine.getNetworkInterfaces() == null) {
				continue;
			}

			for (final NetworkInterfaceModel nic : machine.getNetworkInterfaces()) {
				// DHCP servers distribute IP addresses, correct? :)
				if (nic.getAddresses().isEmpty()) {
					do {
						ip = ip.increment(1);
					}
					while (isAssigned(ip));
						
					nic.addAddress(ip);
				}
			}
		}
	}

	/**
	 * Checks whether a given IP address is already assigned somewhere on our
	 * network.
	 * 
	 * @param ip the IP address to check
	 * @return true if assigned, false otherwise 
	 */
	private Boolean isAssigned(IPAddress ip) {
		return getNetworkModel().getMachines()
						 .values()
						 .stream()
						 .filter(machine -> machine.getIPs().contains(ip))
						 .count() > 0;
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
			if (nic.getMac().isEmpty()) {
				if (isRequired) {
					throw new InvalidNetworkInterfaceException("Network interface " + nic.getIface() + " on "
							+ machine.getLabel() + " requires a MAC address to be set.");
				}
				else {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	protected void distributeMACs() throws AThornSecException {
		final Boolean isRouterHV = getMachineModel().isType(MachineType.HYPERVISOR);

		// Start by checking all of the devices have a MAC address provided, as these
		// are physical devices!
		for (final AMachineModel device : getNetworkModel().getMachines(MachineType.DEVICE)) {
			checkMACs(device, !isRouterHV);
		}

		// Iterate through our dedi machines, these are also physical machines
		for (final AMachineModel server : getNetworkModel().getMachines(MachineType.DEDICATED)) {
			checkMACs(server, !isRouterHV);
		}

		// Iterate through our HyperVisor machines, these are also physical machines
		for (final AMachineModel server : getNetworkModel().getMachines(MachineType.HYPERVISOR)) {
			checkMACs(server, !isRouterHV);
		}

		// Finally, iterate through our services, filling in any gaps.
		// TODO: tidy up this loopy mess?
		for (final AMachineModel server : getNetworkModel().getMachines(MachineType.SERVICE)) {
			if (checkMACs(server, false) == false) {
				server.getNetworkInterfaces().forEach(nic -> {
					if (nic.getMac().isEmpty()) {
						nic.setMac(server.generateMAC(nic.getIface()));
					}
				});
			}
		}
	}

	@Override
	public Collection<IUnit> getInstalled() {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("dhcp", "proceed", "isc-dhcp-server"));

		return units;
	}

	/**
	 * Build our subnets where they have machines in them
	 * @throws InvalidIPAddressException if attempting to assign an invalid IP
	 */
	private void buildPersistentNets() throws InvalidIPAddressException {
		for (MachineType type : getNetworkModel().getSubnets().keySet()) {
			if (getNetworkModel().getMachines(type).isEmpty()) {
				continue;
			}

			buildNet(type);
		}
	}

	/**
	 * Build our /etc/dhcp/dhcpd.conf file, including all subnet files
	 * 
	 * @return FileUnit for DHCPd.conf
	 */
	private FileUnit getDHCPConf() {
		final FileUnit dhcpdConf = new FileUnit("dhcpd_conf", "dhcp_installed", "/etc/dhcp/dhcpd.conf");

		dhcpdConf.appendLine("#Options here are set globally across your whole network(s)");
		dhcpdConf.appendLine("#Please see https://www.systutorials.com/docs/linux/man/5-dhcpd.conf/");
		dhcpdConf.appendLine("#for more details");
		dhcpdConf.appendLine("ddns-update-style none;");
		dhcpdConf.appendLine("option domain-name \\\"" + getNetworkModel().getDomain() + "\\\";");
		dhcpdConf.appendLine("option domain-name-servers " + getMachineModel().getLabel() + "." + getMachineModel().getDomain() + ";");
		dhcpdConf.appendLine("default-lease-time 600;");
		dhcpdConf.appendLine("max-lease-time 1800;");
		dhcpdConf.appendLine("get-lease-hostnames true;");
		dhcpdConf.appendLine("authoritative;");
		dhcpdConf.appendLine("log-facility local7;");
		dhcpdConf.appendCarriageReturn();

		for (final MachineType subnet : getNetworkModel().getSubnets().keySet()) {
			if (getNetworkModel().getMachines(subnet).isEmpty()) {
				continue;
			}

			dhcpdConf.appendLine("include \\\"/etc/dhcp/dhcpd.conf.d/" + subnet.toString() + ".conf\\\";");
		}

		if (getNetworkModel().buildAutoGuest()) {
			dhcpdConf.appendLine("include \\\"/etc/dhcp/dhcpd.conf.d/Guests.conf\\\";");
		}

		return dhcpdConf;
	}

	private FileUnit getDHCPListenInterfaces() {
		final FileUnit dhcpdListen = new FileUnit("dhcpd_defiface", "dhcp_installed", "/etc/default/isc-dhcp-server");

		dhcpdListen.appendText("INTERFACESv4=\\\"");
		dhcpdListen.appendText(getNetworkModel().getSubnets().keySet().stream()
				.filter((type) -> !getNetworkModel().getMachines(type).isEmpty())
				.map(Object::toString)
				.collect(Collectors.joining(" ")));
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

	/**
	 * Build the configuration for a given subnet
	 * @param type
	 * @return
	 * @throws InvalidIPAddressException
	 */
	private FileUnit buildSubNet(MachineType type) throws InvalidIPAddressException {
		final FileUnit subnetConfig = new FileUnit(type + "_dhcpd_live_config", "dhcp_installed",
				"/etc/dhcp/dhcpd.conf.d/" + type + ".conf");

		final IPAddress subnet = getNetworkModel().getSubnet(type);

		final Integer prefix = subnet.getNetworkPrefixLength();
		final IPAddress netmask = subnet.getNetwork().getNetworkMask(prefix, false);
		final String gateway = subnet.getLowerNonZeroHost().withoutPrefixLength().toCompressedString();
		final String broadcast = subnet.getLower().withoutPrefixLength().toCompressedString();

		// Start by telling our DHCP Server about this subnet.
		subnetConfig.appendLine("subnet " + broadcast + " netmask " + netmask + " {}");

		// Now let's create our subnet/groups!
		subnetConfig.appendCarriageReturn();
		subnetConfig.appendLine("group " + type.toString().toLowerCase() + " {");
		subnetConfig.appendLine("\tserver-name \\\"" + type.toString().toLowerCase() + "." + getMachineModel().getHostName() + "." + getNetworkModel().getDomain() + "\\\";");
		subnetConfig.appendLine("\toption routers " + gateway + ";");
		subnetConfig.appendLine("\toption domain-name-servers " + gateway + ";");
		subnetConfig.appendCarriageReturn();

		for (final AMachineModel machine : getNetworkModel().getMachines(type)) {
			// Skip over ourself, we're a router.
			if (machine.equals(getMachineModel())) {
				continue;
			}

			for (final NetworkInterfaceModel iface : machine.getNetworkInterfaces()) {
				// We check the requirement elsewhere. Don't try and build non-machine leases
				if (iface.getMac().isEmpty()) {
					continue;
				}

				if (iface.getAddresses().isPresent()) {
					final IPAddress ip = (IPAddress) iface.getAddresses().get().toArray()[0];

					subnetConfig
							.appendLine("\thost " + machine.getHostName() + "-" + iface.getMac().get().toHexString(false) + " {");
					subnetConfig.appendLine("\t\thardware ethernet " + iface.getMac().get().toColonDelimitedString() + ";");
	
					subnetConfig.appendLine("\t\tfixed-address " + ip.withoutPrefixLength().toCompressedString() + ";");
					subnetConfig.appendLine("\t}");
					subnetConfig.appendCarriageReturn();
				}
			}
		}
		subnetConfig.appendLine("}");

		return subnetConfig;
	}

	@Override
	public Collection<IUnit> getLiveConfig() throws InvalidIPAddressException {
		final Collection<IUnit> units = new ArrayList<>();

		for (final MachineType subnet : getNetworkModel().getSubnets().keySet()) {
			if (getNetworkModel().getMachines(subnet).isEmpty()) {
				continue;
			}

			units.add(buildSubNet(subnet));
		}

		// @TODO: guest networking
		if (getNetworkModel().buildAutoGuest()) {
			final FileUnit guestConfig = new FileUnit("guest_dhcpd_live_config", "dhcp_installed",
					"/etc/dhcp/dhcpd.conf.d/Guests.conf");
			units.add(guestConfig);

			IPAddress subnet = getNetworkModel().getSubnet(MachineType.GUEST);

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
	public Collection<IUnit> getPersistentFirewall() throws InvalidPortException {
		getMachineModel().addLANOnlyListen(Encapsulation.UDP, 67);

		return new ArrayList<>();
	}

	@Override
	public Collection<IUnit> getLiveFirewall() {
		// There aren't any :)
		return new ArrayList<>();
	}
}
