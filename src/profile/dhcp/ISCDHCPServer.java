/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.dhcp;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import core.data.machine.AMachineData.Encapsulation;
import core.data.machine.AMachineData.MachineType;
import core.exception.AThornSecException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.machine.AMachineModel;
import core.model.machine.configuration.NetworkInterfaceModel;
import core.model.network.NetworkModel;
import core.unit.fs.DirUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.IncompatibleAddressException;
import profile.type.Router;

/**
 * Configure and set up our various different networks, and offer IP addresses
 * across (some of) them.
 */
public class ISCDHCPServer extends ADHCPServerProfile {

	private final Set<String> classes;
	private final Set<String> stanzas;

	private final Map<String, IPAddress> groups;

	public ISCDHCPServer(String label, NetworkModel networkModel) {
		super(label, networkModel);

		this.classes = new LinkedHashSet<>();
		this.stanzas = new LinkedHashSet<>();

		this.groups = new Hashtable<>();
		try {
			putGroup("servers", new IPAddressString(Router.SERVERS_NETWORK).toAddress());
			putGroup("users", new IPAddressString(Router.USERS_NETWORK).toAddress());
			putGroup("admins", new IPAddressString(Router.ADMINS_NETWORK).toAddress());
			putGroup("internalOnlys", new IPAddressString(Router.INTERNALS_NETWORK).toAddress());
			putGroup("externalOnlys", new IPAddressString(Router.EXTERNALS_NETWORK).toAddress());
		} catch (final AddressStringException | IncompatibleAddressException e) {
			// in *theory*, the Router should pass proper subnets, however...
			e.printStackTrace();
		}
	}

	@Override
	public Set<IUnit> getInstalled() {
		final Set<IUnit> units = new HashSet<>();

		units.add(new InstalledUnit("dhcp", "proceed", "isc-dhcp-server"));

		return units;
	}

	private String subnetString(IPAddress subnet, String comment) {
		final String subnetAddress = subnet.getLower().removePrefixLength().toCompressedString();
		final Integer prefixLength = subnet.getNetworkPrefixLength();
		final String networkMask = subnet.getNetwork().getNetworkMask(prefixLength, false).toCompressedString();

		return "subnet " + subnetAddress + " netmask " + networkMask + " { }"
				+ ((comment != null) ? " # " + comment : "");
	}

	@Override
	public Set<IUnit> getPersistentConfig() throws InvalidServerModelException {
		final Set<IUnit> units = new HashSet<>();

		// Create sub-dir
		final DirUnit dhcpdConfD = new DirUnit("dhcpd_conf", "dhcp_installed", "/etc/dhcp/dhcpd.conf.d");
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

		dhcpdConf.appendLine(subnetString(getServersGroup(), "servers"));
		dhcpdConf.appendLine(subnetString(getUsersGroup(), "users"));
		dhcpdConf.appendLine(subnetString(getAdminsGroup(), "admins"));
		dhcpdConf.appendLine(subnetString(getInternalOnlysGroup(), "internalOnlys"));
		dhcpdConf.appendLine(subnetString(getExternalOnlysGroup(), "externalOnlys"));
		dhcpdConf.appendCarriageReturn();

		dhcpdConf.appendLine("include \\\"/etc/dhcp/dhcpd.conf.d/servers.conf\\\"");
		dhcpdConf.appendLine("include \\\"/etc/dhcp/dhcpd.conf.d/users.conf\\\"");
		dhcpdConf.appendLine("include \\\"/etc/dhcp/dhcpd.conf.d/admins.conf\\\"");
		dhcpdConf.appendLine("include \\\"/etc/dhcp/dhcpd.conf.d/internalOnlys.conf\\\"");
		dhcpdConf.appendLine("include \\\"/etc/dhcp/dhcpd.conf.d/externalOnlys.conf\\\"");
		dhcpdConf.appendCarriageReturn();

		for (final String dhcpClass : this.classes) {
			dhcpdConf.appendLine(dhcpClass);
		}

		for (final String stanza : this.stanzas) {
			dhcpdConf.appendLine(stanza);
		}

		dhcpdConf.appendLine("}");

		final FileUnit dhcpdListen = new FileUnit("dhcpd_defiface", "dhcp_installed", "/etc/default/isc-dhcp-server");
		units.add(dhcpdListen);

		dhcpdListen.appendLine("INTERFACES=\\\"servers users admins internalOnlys externalOnlys\\\"");
		getNetworkModel().getServerModel(getLabel()).addProcessString(
				"/usr/sbin/dhcpd -4 -q -cf /etc/dhcp/dhcpd.conf servers users admins internalOnlys externalOnlys$");

		return units;
	}

	private FileUnit buildNetworkConf(String label, Map<String, AMachineModel> machines, IPAddress subnet) {
		final Set<IUnit> units = new HashSet<>();

		final FileUnit conf = new FileUnit("dhcp_" + label + "_conf", "dhcpd_installed",
				"/etc/dhcp/dhcpd.conf.d/" + label + ".conf");
		units.add(conf);

		conf.appendLine("group " + label + " {");
		conf.appendLine(
				"\tserver-name \\\"" + label + "." + getNetworkModel().getData().getDomain(getLabel()) + "\\\"");
		conf.appendLine("\t option routers " + subnet.getLowerNonZeroHost().toCompressedString() + ";");
		conf.appendLine("\t option domain-name-servers " + subnet.getLowerNonZeroHost().toCompressedString() + ";");

		if (machines != null) {
			for (final AMachineModel machine : machines.values()) {
				for (final NetworkInterfaceModel iface : machine.getNetworkInterfaces()) {
					conf.appendCarriageReturn();
					conf.appendLine("\thost " + machine.getLabel() + "-" + iface.getMac().toDashedString() + "{");
					conf.appendLine("\t\thardware ethernet " + iface.getMac().toColonDelimitedString() + ";");
					conf.appendLine("\t\tfixed-address " + iface.getAddress() + ";");
					conf.appendLine("\t\toption host-name " + machine.getLabel() + ";");
					conf.appendLine("\t}");
				}
			}
		}
		conf.appendLine("}");

		return conf;
	}

	@Override
	public Set<IUnit> getLiveConfig() {
		final Set<IUnit> units = new HashSet<>();

		// TODO: work out a way to do this in a loop
		units.add(buildNetworkConf("servers", getNetworkModel().getMachines(MachineType.SERVER), getServersGroup()));
		units.add(buildNetworkConf("users", getNetworkModel().getMachines(MachineType.USER), getUsersGroup()));
		units.add(buildNetworkConf("admins", getNetworkModel().getMachines(MachineType.ADMIN), getAdminsGroup()));
		units.add(buildNetworkConf("internalOnlys", getNetworkModel().getMachines(MachineType.INTERNAL_ONLY),
				getInternalOnlysGroup()));
		units.add(buildNetworkConf("externalOnlys", getNetworkModel().getMachines(MachineType.EXTERNAL_ONLY),
				getExternalOnlysGroup()));

		// TODO: handle guest networking
//		if (this.networkModel.getData().buildAutoGuest()) {
//			serversConf.appendCarriageReturn();
//			serversConf.appendLine("    #This is our pool for guest connections");
//			serversConf.appendLine("    #We put it first, because everyone is a guest until they aren't!");
//			serversConf.appendLine("    subnet 10.250.0.0 netmask 255.255.252.0 {");
//			serversConf.appendLine("        pool {");
//			serversConf.appendLine("            range 10.250.0.15 10.250.3.255;");
//			serversConf.appendLine("            option routers 10.0.0.1;");
//			serversConf.appendLine("            option domain-name-servers 1.1.1.1;");
//			serversConf.appendLine("        }");
//			serversConf.appendLine("    }");
//		}

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

	private IPAddress getGroup(String key) {
		return this.groups.get(key);
	}

	private void putGroup(String key, IPAddress subnet) {
		this.groups.put(key, subnet);
	}

	private IPAddress getServersGroup() {
		return getGroup("servers");
	}

	private IPAddress getUsersGroup() {
		return getGroup("users");
	}

	private IPAddress getAdminsGroup() {
		return getGroup("admins");
	}

	private IPAddress getInternalOnlysGroup() {
		return getGroup("internalOnlys");
	}

	private IPAddress getExternalOnlysGroup() {
		return getGroup("externalOnlys");
	}

	@Deprecated
	public void addStanza(String stanza) {
		this.stanzas.add(stanza);
	}

	@Deprecated
	public void addClass(String stanza) {
		this.classes.add(stanza);
	}
}
