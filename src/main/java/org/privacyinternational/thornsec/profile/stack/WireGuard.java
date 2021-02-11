/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.profile.stack;

import java.util.ArrayList;
import java.util.Collection;

import javax.json.JsonObject;

import org.privacyinternational.thornsec.core.data.machine.AMachineData.MachineType;
import org.privacyinternational.thornsec.core.data.machine.configuration.TrafficRule.Encapsulation;
import org.privacyinternational.thornsec.core.exception.data.InvalidIPAddressException;
import org.privacyinternational.thornsec.core.exception.data.InvalidPortException;
import org.privacyinternational.thornsec.core.exception.data.machine.InvalidServerException;
import org.privacyinternational.thornsec.core.exception.data.machine.configuration.InvalidNetworkInterfaceException;
import org.privacyinternational.thornsec.core.exception.runtime.InvalidMachineModelException;
import org.privacyinternational.thornsec.core.exception.runtime.InvalidServerModelException;
import org.privacyinternational.thornsec.core.iface.IUnit;
import org.privacyinternational.thornsec.core.model.machine.ServerModel;
import org.privacyinternational.thornsec.core.model.machine.configuration.networking.WireGuardModel;
import org.privacyinternational.thornsec.core.model.network.UserModel;
import org.privacyinternational.thornsec.core.profile.AStructuredProfile;
import org.privacyinternational.thornsec.core.unit.SimpleUnit;
import org.privacyinternational.thornsec.core.unit.pkg.InstalledUnit;

/**
 * This builds a WireGuard server on a given machine (intended to be run on a
 * Router).
 */
public class WireGuard extends AStructuredProfile {

	private final Integer listenPort;

	public WireGuard(ServerModel me) {
		super(me);

		final JsonObject wgSettings = getServerModel().getData().getData().getJsonObject("wireguard");
		this.listenPort = wgSettings.getInt("listen_port", 51820);
	}

	@Override
	public final Collection<IUnit> getInstalled() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("wireguard", "proceed", "wireguard"));

		return units;
	}

	@Override
	public final Collection<IUnit> getPersistentConfig()
			throws InvalidServerException, InvalidIPAddressException, InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		WireGuardModel nic = null;
		try {
			nic = new WireGuardModel(getNetworkModel());
		} catch (InvalidNetworkInterfaceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (UserModel user : getNetworkModel().getUsers().values()) {
			nic.addWireGuardPeer(user);
		}

		nic.setListenPort(this.listenPort);
		nic.addAddress(getNetworkModel().getSubnet(MachineType.VPN));

		getMachineModel().addNetworkInterface(nic);

		units.add(new SimpleUnit("wireguard_private_key", "wireguard_installed",
				"echo $(wg genkey) | sudo tee /etc/wireguard/private.key > /dev/null",
				"sudo cat /etc/wireguard/private.key 2>&1;", "", "fail",
				"I was unable to generate you a private key."));

		return units;
	}

	@Override
	public final Collection<IUnit> getPersistentFirewall() throws InvalidPortException {
		final Collection<IUnit> units = new ArrayList<>();

		getServerModel().addListen(Encapsulation.UDP, this.listenPort);

		return units;
	}
}
