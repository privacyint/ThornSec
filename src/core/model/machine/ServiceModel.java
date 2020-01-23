/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import javax.json.stream.JsonParsingException;
import javax.mail.internet.AddressException;
import core.data.machine.configuration.DiskData;
import core.data.machine.configuration.DiskData.Format;
import core.data.machine.configuration.DiskData.Medium;
import core.exception.AThornSecException;
import core.iface.IUnit;
import core.model.machine.configuration.DiskModel;
import core.model.network.NetworkModel;

/**
 * This model represents a Service on our network.
 *
 * A service is a machine which is run on a HyperVisor
 */
public class ServiceModel extends ServerModel {
	
	private Map<String, DiskModel> disks;
	
	public ServiceModel(String label, NetworkModel networkModel)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException, ClassNotFoundException, URISyntaxException, AddressException,
			IOException, JsonParsingException, AThornSecException {
		super(label, networkModel);
		
		Collection<DiskData> disks = getNetworkModel().getData().getDisks(label);
		
		disks.forEach(diskData -> {
			DiskModel disk = new DiskModel(diskData);
			addDisk(disk.getLabel(), disk);
		});
		
		if (getDisk("boot") == null) {
			DiskModel bootDisk = new DiskModel("boot", Medium.DISK, Format.VMDK, null, 666, null, "autogenerated boot disk");
			addDisk(bootDisk.getLabel(), bootDisk);
		}
		if (getDisk("data") == null) {
			DiskModel dataDisk = new DiskModel("data", Medium.DISK, Format.VMDK, null, 666, null, "autogenerated data disk");
			addDisk(dataDisk.getLabel(), dataDisk);
		}
	}

	@Override
	public Collection<IUnit> getUnits() throws AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(super.getUnits());

		return units;
	}

	public void addDisk(String label, DiskModel disk) {
		this.disks.put(label, disk);
	}

	public Map<String, DiskModel> getDisks() {
		return this.disks;
	}
	
	public DiskModel getDisk(String label) {
		return this.getDisks().get(label);
	}
}
