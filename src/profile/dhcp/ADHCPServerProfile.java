/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.dhcp;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import inet.ipaddr.IPAddress;
import core.data.machine.AMachineData.MachineType;
import core.exception.AThornSecException;

import core.iface.IUnit;
import core.model.machine.AMachineModel;
import core.model.machine.ServerModel;

import core.profile.AStructuredProfile;

/**
 * This is a DHCP server of some type.
 *
 * DHCP servers are quite involved, so you'll need to implement everything!
 */
public abstract class ADHCPServerProfile extends AStructuredProfile {

	private final Map<MachineType, Collection<AMachineModel>> subnetsMachines;

	/**
	 * In your constructor, you will need
	 *
	 * @param myData
	 * @param networkModel
	 */
	public ADHCPServerProfile(ServerModel me) {
		super(me);

		this.subnetsMachines = new LinkedHashMap<>();
	}

	/**
	 * @param subnetName
	 * @return false if subnet already exists, true if subnet was added
	 */
	public final void addSubnet(MachineType subnetName, IPAddress subnet) {
		this.subnetsMachines.putIfAbsent(subnetName, new LinkedHashSet<>());
	}

	protected final Map<MachineType, IPAddress> getSubnets() {
		return getNetworkModel().getSubnets();
	}

	protected final IPAddress getSubnet(MachineType subnet) {
		return getSubnets().get(subnet);
	}

	private final void putMachines(MachineType subnetName, Collection<AMachineModel> machines) {
		this.subnetsMachines.put(subnetName, machines);
	}

	/**
	 *
	 * @param type
	 * @return null if doesn't exist
	 */
	protected final Collection<AMachineModel> getMachines(MachineType type) {
		return this.subnetsMachines.get(type);
	}

	/**
	 *
	 * @param subnetName
	 * @param machine
	 * @return false if machine already added, true otherwise
	 */
	public final void addToSubnet(MachineType subnetName, Collection<AMachineModel> machines) {
		final Collection<AMachineModel> currentMachines = getMachines(subnetName);

		currentMachines.addAll(machines);

		putMachines(subnetName, currentMachines);
	}

	// Set all of these as abstract, you'll need to write them, even if they return
	// empty Sets.
	@Override
	public abstract Collection<IUnit> getInstalled() throws AThornSecException;

	@Override
	public abstract Collection<IUnit> getPersistentConfig() throws AThornSecException;

	@Override
	public abstract Collection<IUnit> getLiveConfig() throws AThornSecException;

	@Override
	public abstract Collection<IUnit> getPersistentFirewall() throws AThornSecException;

	@Override
	public abstract Collection<IUnit> getLiveFirewall() throws AThornSecException;

	/**
	 * The entire point of a DHCP server is to distribute IP Addresses.
	 *
	 * You must implement this method, and it must distribute IP addresses across
	 * your various {@code AMachineModel}s.
	 *
	 * @throws AThornSecException
	 */
	// protected abstract void distributeIPs() throws AThornSecException;

	/**
	 * DHCP shouldn't *really* give out MAC addresses, given that it uses them to
	 * distribute its conception of IPs.
	 *
	 * Originally, I used to do all this the other way 'round - i.e. the machines
	 * themselves would decide what they thought their MAC address should be (if it
	 * wasn't set).
	 *
	 * This ended up being highly restrictive on the modelling, and involved turning
	 * the networking into a pile of hacks, held together with hopes and dreams.
	 *
	 * If some machines (and therefore their NICs) are 100% virtual, it doesn't
	 * matter an inch what their MAC addresses are, so long as they a) Have a MAC
	 * address b) The DHCP server knows what that MAC address is we're all good!
	 *
	 * Let's use this method, therefore, to ensure that there are MAC addresses
	 * pushed out across our network where otherwise they'd be null...
	 *
	 * @throws AThornSecException
	 */
	protected abstract void distributeMACs() throws AThornSecException;
}
