/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine.configuration.networking;

import java.util.Map;
import java.util.Optional;
import core.data.machine.configuration.NetworkInterfaceData;
import core.data.machine.configuration.NetworkInterfaceData.Inet;
import core.model.network.NetworkModel;
import core.unit.fs.FileUnit;

/**
 * This model creates a WireGuard interface through systemd-networkd.
 * 
 * For more information, see https://www.wireguard.com/
 */
public class WireGuardModel extends NetworkInterfaceModel {

	private Map<String, String> peerKeys;
	private String psk;
	private Integer listenPort;

	public WireGuardModel(NetworkInterfaceData myData, NetworkModel networkModel) {
		super(myData, networkModel);
		
		super.setInet(Inet.WIREGUARD);
		
		this.peerKeys = null;
		this.psk = null;
		this.listenPort = null;
	}

	public WireGuardModel() {
		this(null, null);
	}

	public void setListenPort(Integer listenPort) {
		this.listenPort = listenPort;
	}
	
	public void setPSK(String psk) {
		this.psk = psk;
	}
	
	@Override
	public Optional<FileUnit> getNetDevFile() {
		FileUnit netdev = super.getNetDevFile().get();

		netdev.appendCarriageReturn();
		netdev.appendCarriageReturn();
		netdev.appendLine("[WireGuard]");
		netdev.appendLine("PrivateKey=$(cat /etc/wireguard/private.key)");
		netdev.appendLine("ListenPort=" + this.listenPort);

		this.peerKeys.forEach((peer, pubKey) -> {
			netdev.appendCarriageReturn();
			netdev.appendLine("[WireGuardPeer]");
			netdev.appendLine("PublicKey=" + pubKey);
			netdev.appendLine("PresharedKey=" + this.psk);
			netdev.appendLine("AllowedIPs=0.0.0.0/0");
			netdev.appendLine("Description=" + peer);
		});

		return Optional.of(netdev);
	}

	public void addWireGuardPeer(String peer, String pubKey) {
		this.peerKeys.put(peer, pubKey);
	}
}
