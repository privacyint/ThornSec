/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine.configuration.networking;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import core.data.machine.configuration.NetworkInterfaceData;
import core.data.machine.configuration.NetworkInterfaceData.Inet;
import core.model.network.NetworkModel;
import core.unit.fs.FileUnit;

/**
 * This model represents a MACVLAN Trunk - i.e. the "physical" interface upon
 * which to stack the VLANs.
 */
public class MACVLANTrunkModel extends NetworkInterfaceModel {
	private Set<MACVLANModel> vlans;

	public MACVLANTrunkModel(NetworkInterfaceData myData, NetworkModel networkModel) {
		super(myData, networkModel);
		
		super.setInet(Inet.MACVLAN);
		super.setWeighting(10);
		super.setARP(false);
		super.setIsIPForwarding(true);
		super.setIsIPMasquerading(false);
		super.setReqdForOnline(true);

		vlans = new LinkedHashSet<>();
	}
	
	public MACVLANTrunkModel() {
		this(new NetworkInterfaceData("MACVLANTrunk"), null);
	}

	@Override
	public Optional<FileUnit> getNetworkFile() {
		
		final FileUnit network = super.getNetworkFile().get();

		getVLANs().forEach(vlan -> {
			network.appendLine("MACVLAN=" + vlan.getIface());
		});

		return Optional.of(network);
	}

	@Override
	public Optional<FileUnit> getNetDevFile() {
		return Optional.empty(); // Don't need a NetDev for a MACVLAN Trunk
	}

	public final void addVLAN(MACVLANModel vlan) {
		//assertNotNull(vlan);

		this.vlans.add(vlan);
	}

	public final Collection<MACVLANModel> getVLANs() {
		return this.vlans;
	}
}
