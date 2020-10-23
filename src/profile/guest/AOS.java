/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.guest;

import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import javax.swing.JOptionPane;
import core.StringUtils;
import core.exception.AThornSecException;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.InvalidMachineModelException;
import core.iface.IUnit;
import core.model.machine.ServerModel;
import core.profile.AStructuredProfile;

public abstract class AOS extends AStructuredProfile {
	
	public AOS(ServerModel me) throws InvalidMachineModelException {
		super(me);
	}

	public abstract Collection<IUnit> buildIso() throws AThornSecException;

	protected abstract String getIsoURLFromLatest();

	protected abstract String getIsoSHA512FromLatest(); 

	public Collection<IUnit> getISODownloadUnits(String url, String checksum) throws InvalidServerException, InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		String filename = null;
		String cleanedFilename = null;

		try {
			filename = Paths.get(new URI(url).getPath()).getFileName().toString();
			cleanedFilename = StringUtils.stringToAlphaNumeric(filename, "_");
		}
		catch (final Exception e) {
			JOptionPane.showMessageDialog(null, "It doesn't appear that " + url + " is a valid link to an ISO.\n\nPlease fix this in your JSON");
			System.exit(1);
		}

		//units.add(new FileDownloadUnit(cleanedFilename, "metal_genisoimage_installed", url,
		//		getServerModel().getVMBase().getAbsolutePath() + "/" + filename,
		//		"The Debian net install ISO couldn't be downloaded.  Please check the URI in your config."));
		//units.add(new FileChecksumUnit(cleanedFilename, cleanedFilename + "_downloaded", Checksum.SHA512,
		//		getServerModel().getHypervisorModel().getVMBase().getAbsolutePath() + "/" + filename, checksum,
		//		"The sha512 sum of the Debian net install in your config doesn't match what has been downloaded."
		//		+ " This could mean your connection is man-in-the-middle'd, that the download was corrupted,"
		//		+ " or it could just be that the file has been updated on the server."
		//		+ " Please check http://cdimage.debian.org/debian-cd/current/amd64/iso-cd/SHA512SUMS (64 bit)"
		//		+ " or http://cdimage.debian.org/debian-cd/current/i386/iso-cd/SHA512SUMS (32 bit) for the correct checksum."));

		return units;
	}

	//Force our OS to return some firewall rules.
	@Override
	public abstract Collection<IUnit> getPersistentFirewall() throws AThornSecException;
}
