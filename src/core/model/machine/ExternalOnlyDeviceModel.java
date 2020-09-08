/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine;

import java.util.Collection;
import java.util.LinkedHashSet;
import core.data.machine.ExternalDeviceData;
import core.data.machine.configuration.TrafficRule.Encapsulation;
import core.exception.AThornSecException;
import core.exception.data.InvalidPortException;
import core.iface.IUnit;
import core.model.network.NetworkModel;
import inet.ipaddr.HostName;

/**
 * This model represents an "External-Only" device on our network.
 *
 * An external-only device is one which can access the wider internet, but is
 * not allowed to access internal services.
 */
public class ExternalOnlyDeviceModel extends ADeviceModel {
	public ExternalOnlyDeviceModel(ExternalDeviceData myData, NetworkModel networkModel) throws AThornSecException {
		super(myData, networkModel);
	}

	/**
	 * Set up our device to access the Internet 
	 */
	@Override
	public Collection<IUnit> getPersistentFirewall() throws InvalidPortException {
		this.addEgress(Encapsulation.UDP, new HostName("*"));
		this.addEgress(Encapsulation.TCP, new HostName("*"));

		return new LinkedHashSet<>();
	}

	@Override
	public void init() throws AThornSecException {
		// TODO Auto-generated method stub
	}
}
