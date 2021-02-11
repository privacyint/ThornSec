/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine.configuration.disks;

import java.io.File;
import core.data.machine.configuration.DiskData;
import core.exception.data.machine.configuration.disks.DiskModelException;
import core.exception.data.machine.configuration.disks.InvalidDiskFilenameException;
import core.model.network.NetworkModel;

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
