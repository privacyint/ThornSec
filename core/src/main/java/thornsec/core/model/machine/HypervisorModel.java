/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;
import core.data.machine.HypervisorData;
import core.data.machine.ServerData;
import core.exception.AThornSecException;
import core.exception.runtime.InvalidMachineModelException;
import core.model.network.NetworkModel;

/**
 * This Class represents a Hypervisor
 */
public class HypervisorModel extends ServerModel {
	public HypervisorModel(ServerData myData, NetworkModel networkModel) throws AThornSecException {
		super(myData, networkModel);
	}
	
	@Override
	public HypervisorData getData() {
		return (HypervisorData) super.getData();
	}
	
	public File getVMBase() {
		return getData().getVmBase().orElse(new File("/srv/ThornSec"));
	}

	public Set<ServiceModel> getServices() throws InvalidMachineModelException {
		Set<ServiceModel> services = new LinkedHashSet<>();
		
		for (ServerData serviceData : getData().getServices()) {
			services.add((ServiceModel)getNetworkModel().getMachineModel(serviceData.getLabel()));
		}
		
		return services;
	}
	
}
