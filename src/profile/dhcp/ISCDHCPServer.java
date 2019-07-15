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
import core.exception.AThornSecException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.network.NetworkModel;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;

public class ISCDHCPServer extends ADHCPServerProfile {

	private final Set<String> classes;
	private final Set<String> stanzas;

	public ISCDHCPServer(String label, NetworkModel networkModel) {
		super(label, networkModel);

		this.classes = null;
		this.stanzas = null;
	}

	public void addStanza(String stanza) {
		this.stanzas.add(stanza);
	}

	public void addClass(String stanza) {
		this.classes.add(stanza);
	}

	@Override
	public Set<IUnit> getPersistentConfig() throws InvalidServerModelException {
		final Set<IUnit> units = new HashSet<>();

		final FileUnit dhcpdConf = new FileUnit("dhcp_conf", "dhcp_installed", "/etc/dhcp/dhcpd.conf");
		units.add(dhcpdConf);

		dhcpdConf.appendLine("include \\\"/etc/dhcp/dhcpd.conf.d/users.conf\\\"");
		dhcpdConf.appendCarriageReturn();
		dhcpdConf.appendLine("ddns-update-style none;");
		dhcpdConf.appendLine("option domain-name \\\"" + this.networkModel.getData().getDomain() + "\\\";");
		dhcpdConf.appendLine("option domain-name-servers 10.0.0.1;");
		dhcpdConf.appendLine("default-lease-time 600;");
		dhcpdConf.appendLine("max-lease-time 1800;");
		dhcpdConf.appendLine("authoritative;");
		dhcpdConf.appendLine("log-facility local7;");
		dhcpdConf.appendCarriageReturn();
		dhcpdConf.appendLine("shared-network " + this.networkModel.getLabel() + " {");
		dhcpdConf.appendCarriageReturn();
		dhcpdConf.appendLine("    subnet 10.0.0.0 netmask 255.0.0.0 {");
		dhcpdConf.appendLine("    }");

		if (this.networkModel.getData().buildAutoGuest()) {
			dhcpdConf.appendCarriageReturn();
			dhcpdConf.appendLine("    #This is our pool for guest connections");
			dhcpdConf.appendLine("    #We put it first, because everyone is a guest until they aren't!");
			dhcpdConf.appendLine("    subnet 10.250.0.0 netmask 255.255.252.0 {");
			dhcpdConf.appendLine("        pool {");
			dhcpdConf.appendLine("            range 10.250.0.15 10.250.3.255;");
			dhcpdConf.appendLine("            option routers 10.0.0.1;");
			dhcpdConf.appendLine("            option domain-name-servers 1.1.1.1;");
			dhcpdConf.appendLine("            #deny members of \"VPN\";");
			dhcpdConf.appendLine("        }");
			dhcpdConf.appendLine("    }");
		}

		for (final String dhcpClass : this.classes) {
			dhcpdConf.appendLine(dhcpClass);
		}

		dhcpdConf.appendCarriageReturn();

		for (final String stanza : this.stanzas) {
			dhcpdConf.appendLine(stanza);
		}

		dhcpdConf.appendLine("}");

		final FileUnit dhcpdListen = new FileUnit("dhcpd_defiface", "dhcp_installed", "/etc/default/isc-dhcp-server");
		units.add(dhcpdListen);

		dhcpdListen.appendLine("INTERFACES=\\\"lan0\\\"");
		this.networkModel.getServerModel(getLabel())
				.addProcessString("/usr/sbin/dhcpd -4 -q -cf /etc/dhcp/dhcpd.conf lan0$");

		return units;
	}

	@Override
	public Set<IUnit> getInstalled() {
		final Set<IUnit> units = new HashSet<>();

		units.add(new InstalledUnit("dhcp", "proceed", "isc-dhcp-server"));

		return units;
	}

	@Override
	public Set<IUnit> getLiveConfig() {
		final Set<IUnit> units = new HashSet<>();

		// TODO: Add users
		units.add(new RunningUnit("dhcp_running", "isc-dhcp-server", "dhcpd"));

		return units;
	}

	@Override
	public Set<IUnit> getPersistentFirewall() throws AThornSecException {
		// DNS needs to talk on :67 UDP
		this.networkModel.getServerModel(getLabel()).addListen(Encapsulation.UDP, 67);

		return new HashSet<>();
	}

	@Override
	public Set<IUnit> getLiveFirewall() throws AThornSecException {
		// There aren't any :)
		return new HashSet<>();
	}
}
