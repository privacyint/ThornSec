/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.core.model.machine.configuration.disks;

import java.io.File;
import org.privacyinternational.thornsec.core.data.machine.configuration.DiskData;
import org.privacyinternational.thornsec.core.exception.data.machine.configuration.disks.DiskModelException;
import org.privacyinternational.thornsec.core.exception.data.machine.configuration.disks.InvalidDiskFilenameException;
import org.privacyinternational.thornsec.core.model.network.NetworkModel;

public class DVDModel extends ADiskModel {
	public DVDModel(DiskData myData, NetworkModel networkModel) throws DiskModelException {
		super(myData, networkModel);

		setFilename(myData.getFilename().orElseGet(() -> null));
	}

	public DVDModel(String label, File filename) throws InvalidDiskFilenameException {
		super(null, null);
		
		setLabel(label);
		setFilename(filename);
	}
}
