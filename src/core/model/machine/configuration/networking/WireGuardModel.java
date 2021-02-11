/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine.configuration.networking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import core.data.machine.configuration.NetworkInterfaceData;
import core.data.machine.configuration.NetworkInterfaceData.Inet;
import core.exception.data.machine.configuration.InvalidNetworkInterfaceException;
import core.model.network.NetworkModel;
import core.model.network.UserModel;
import core.unit.fs.FileUnit;

/**
 * This model creates a WireGuard interface through systemd-networkd.
 * 
 * For more information, see https://www.wireguard.com/
 */
public class WireGuardModel extends NetworkInterfaceModel {

	private Collection<UserModel> peers;
	private Integer listenPort;

	public WireGuardModel(NetworkInterfaceData myData, NetworkModel networkModel) throws InvalidNetworkInterfaceException {
		super(myData, networkModel);

		super.setInet(Inet.WIREGUARD);

		this.peers = null;
		this.listenPort = null;
	}

	public WireGuardModel(NetworkInterfaceModel nic) throws InvalidNetworkInterfaceException {
		super(nic);

		super.setInet(Inet.WIREGUARD);

		this.peers = null;
		this.listenPort = null;
	}

	public WireGuardModel(NetworkModel networkModel) throws InvalidNetworkInterfaceException {
		this(new NetworkInterfaceData("wg"), networkModel);
	}

	public void setListenPort(Integer listenPort) {
		this.listenPort = listenPort;
	}

	@Override
	public Optional<FileUnit> getNetDevFile() {
		FileUnit netdev = super.getNetDevFile().get();

		netdev.appendCarriageReturn();
		netdev.appendLine("[WireGuard]");
		netdev.appendLine("PrivateKey=$(cat /etc/wireguard/private.key)");
		netdev.appendLine("ListenPort=" + this.listenPort);

		this.peers.forEach((peer) -> {
			netdev.appendCarriageReturn();
			netdev.appendLine("[WireGuardPeer]");
			netdev.appendLine("PublicKey=" + peer.getWireGuardKey().orElseGet(() -> ""));
			netdev.appendLine("PresharedKey=" + peer.getWireguardPSK().orElseGet(() -> ""));
			netdev.appendLine("AllowedIPs=" + String.join(", ", peer.getWireGuardIPs().orElseGet(() -> new ArrayList<>())));
			netdev.appendLine("Description=" + peer.getUsername());
		});

		return Optional.of(netdev);
	}

	public void addWireGuardPeer(UserModel user) {
		if (this.peers == null) {
			this.peers = new ArrayList<>();
		}

		this.peers.add(user);
	}
}
