/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.stack;

import java.util.ArrayList;
import java.util.Collection;

import javax.json.JsonObject;

import core.data.machine.AMachineData.Encapsulation;
import core.exception.data.InvalidIPAddressException;
import core.exception.data.InvalidPortException;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.machine.configuration.networking.WireGuardModel;
import core.model.network.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.pkg.InstalledUnit;

/**
 * This builds a WireGuard server on a given machine (designed to be run on a
 * Router).
 */
public class WireGuard extends AStructuredProfile {

	private final Integer listenPort;
	private final String psk;

	public WireGuard(String label, NetworkModel networkModel) {
		super(label, networkModel);

		final JsonObject wgSettings = getNetworkModel().getData().getProperties(getLabel(), "wireguard");
		this.listenPort = wgSettings.getInt("listen_port", 51820);
		this.psk = wgSettings.getString("psk");
	}

	@Override
	public final Collection<IUnit> getInstalled() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("wireguard", "proceed", "wireguard"));

		return units;
	}

	@Override
	public final Collection<IUnit> getPersistentConfig()
			throws InvalidServerException, InvalidServerModelException, InvalidIPAddressException {
		final Collection<IUnit> units = new ArrayList<>();

		final WireGuardModel nic = new WireGuardModel("VPN", this.psk, this.listenPort);

		getNetworkModel().getUserDevices().keySet().forEach(user -> {
			final String key = getNetworkModel().getWireGuardKey(user);

			if (key != null) {
				nic.addPeer(key);
			}
		});

		nic.addAddress(getNetworkModel().getData().getVPNSubnet());

		getNetworkModel().getServerModel(getLabel()).addNetworkInterface(nic);

		units.add(new SimpleUnit("wireguard_private_key", "wireguard_installed",
				"echo \\$(wg genkey) | sudo tee /etc/wireguard/private.key > /dev/null",
				"sudo cat /etc/wireguard/private.key 2>&1;", "", "pass",
				"I was unable to generate you a private key."));

		return units;
	}

	@Override
	public final Collection<IUnit> getPersistentFirewall() throws InvalidServerModelException, InvalidPortException {
		final Collection<IUnit> units = new ArrayList<>();

		getNetworkModel().getServerModel(getLabel()).addListen(Encapsulation.UDP, this.listenPort);

		return units;
	}
}
