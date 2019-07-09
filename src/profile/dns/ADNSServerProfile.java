/*
 * This code is part of the ThornSec project.
 * 
 * To learn more, please head to its GitHub repo: @privacyint
 * 
 * Pull requests encouraged.
 */
package profile.dns;

import java.util.Set;

import core.iface.IUnit;

import core.model.network.NetworkModel;

import core.profile.AStructuredProfile;
import inet.ipaddr.HostName;
import core.exception.AThornSecException;

/**
 * This is a DNS Server of some type.
 * 
 * DNS Servers are quite involved, so you'll need to implement everything!
 */
public abstract class ADNSServerProfile extends AStructuredProfile {

	public ADNSServerProfile(String label, NetworkModel networkModel) {
		super(label, networkModel);
	}

	@Override
	public abstract Set<IUnit> getInstalled() throws AThornSecException;

	@Override
	public  abstract Set<IUnit> getPersistentConfig() throws AThornSecException;

	@Override
	public abstract Set<IUnit> getLiveConfig() throws AThornSecException;

	@Override
	public abstract Set<IUnit> getPersistentFirewall() throws AThornSecException;

	@Override
	public abstract Set<IUnit> getLiveFirewall() throws AThornSecException;
	
	/**
	 * add a DNS record to a given domain
	 */
	public  abstract void addRecord(String domain, String host, HostName... records);
}
