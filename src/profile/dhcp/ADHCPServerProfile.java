/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.dhcp;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import core.exception.AThornSecException;
import core.iface.IUnit;
import core.model.machine.AMachineModel;
import core.model.network.NetworkModel;
import core.profile.AStructuredProfile;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IncompatibleAddressException;

/**
 * This is a DHCP server of some type.
 *
 * DHCP servers are quite involved, so you'll need to implement everything!
 */
public abstract class ADHCPServerProfile extends AStructuredProfile {

	private final Map<String, IPAddress> subnetsGateways;
	private final Map<String, Set<AMachineModel>> subnetsMachines;

	/**
	 * In your constructor, you will need
	 *
	 * @param label
	 * @param networkModel
	 */
	public ADHCPServerProfile(String label, NetworkModel networkModel) {
		super(label, networkModel);

		this.subnetsGateways = new LinkedHashMap<>();
		this.subnetsMachines = new LinkedHashMap<>();
	}

	/**
	 * @param subnetName
	 * @return false if subnet already exists, true if subnet was added
	 */
	public final void addSubnet(String subnetName, IPAddress gateway) {
		this.subnetsMachines.putIfAbsent(subnetName, new LinkedHashSet<>());
		this.subnetsGateways.putIfAbsent(subnetName, gateway);
	}

	protected final Map<String, IPAddress> getSubnets() {
		return this.subnetsGateways;
	}

	private final void putMachines(String subnetName, Set<AMachineModel> machines) {
		this.subnetsMachines.put(subnetName, machines);
	}

	/**
	 *
	 * @param subnetName
	 * @return null if doesn't exist
	 */
	protected final Set<AMachineModel> getMachines(String subnetName) {
		return this.subnetsMachines.get(subnetName);
	}

	protected final IPAddress getGateway(String subnetName) {
		return this.subnetsGateways.get(subnetName);
	}

	/**
	 *
	 * @param subnetName
	 * @param machine
	 * @return false if machine already added, true otherwise
	 */
	public final void addToSubnet(String subnetName, AMachineModel... machines) {
		final Set<AMachineModel> currentMachines = getMachines(subnetName);

		for (final AMachineModel machine : machines) {
			currentMachines.add(machine);
		}

		putMachines(subnetName, currentMachines);
	}

	// Set all of these as abstract, you'll need to write them, even if they return
	// empty Sets.
	@Override
	public abstract Set<IUnit> getInstalled() throws AThornSecException;

	@Override
	public abstract Set<IUnit> getPersistentConfig() throws AThornSecException;

	@Override
	public abstract Set<IUnit> getLiveConfig() throws AThornSecException;

	@Override
	public abstract Set<IUnit> getPersistentFirewall() throws AThornSecException;

	@Override
	public abstract Set<IUnit> getLiveFirewall() throws AThornSecException;
}
