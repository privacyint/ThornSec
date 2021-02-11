/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.core.model.machine;

import java.util.Collection;
import java.util.LinkedHashSet;
import org.privacyinternational.thornsec.core.data.machine.UserDeviceData;
import org.privacyinternational.thornsec.core.data.machine.configuration.TrafficRule.Encapsulation;
import org.privacyinternational.thornsec.core.exception.AThornSecException;
import org.privacyinternational.thornsec.core.exception.data.InvalidPortException;
import org.privacyinternational.thornsec.core.iface.IUnit;
import org.privacyinternational.thornsec.core.model.network.NetworkModel;
import inet.ipaddr.HostName;

/**
 * This model represents a User device on our network.
 *
 * User devices can see "everything" both internal and external - be careful
 * with these devices!
 */
public class UserDeviceModel extends ADeviceModel {
	public UserDeviceModel(UserDeviceData myData, NetworkModel networkModel) throws AThornSecException {
		super(myData, networkModel);
	}

	@Override
	public void init() throws AThornSecException {
		// TODO Auto-generated method stub
	}

	@Override
	protected Collection<IUnit> getPersistentFirewall() throws InvalidPortException {
		this.addEgress(Encapsulation.UDP, new HostName("*"));
		this.addEgress(Encapsulation.TCP, new HostName("*"));

		return new LinkedHashSet<>();
	}
}
