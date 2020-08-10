/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine.configuration.networking;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Optional;
import core.data.machine.configuration.NetworkInterfaceData;
import core.data.machine.configuration.NetworkInterfaceData.Inet;
import core.model.network.NetworkModel;
import core.unit.fs.FileUnit;

/**
 * This model represents a Bonded physical interface - i.e. the "physical"
 * interface to add to a trunk.
 */
public class BondInterfaceModel extends NetworkInterfaceModel {
	private BondModel bond;

	public BondInterfaceModel(NetworkInterfaceData myData, NetworkModel networkModel) {
		super(myData, networkModel);
		
		super.setInet(Inet.MANUAL);
		super.setReqdForOnline(true);
		super.setWeighting(0);
		super.setARP(false);
		
		this.bond = null;
	}

	@Override
	public Optional<FileUnit> getNetworkFile() {
		final FileUnit network = super.getNetworkFile().get();
		
		network.appendCarriageReturn();
		network.appendLine("Bond=" + getBond().getIface());

		return Optional.of(network);
	}

	private BondModel getBond() {
		return this.bond;
	}
	
	public void setBond(BondModel bond) {
		//assertNotNull(bond);
		
		this.bond = bond;
	}

	@Override
	public Optional<FileUnit> getNetDevFile() {
		return Optional.empty(); // Don't need a NetDev for a Bonded Link
	}
}
