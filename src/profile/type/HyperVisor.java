/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.type;

import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import core.exception.data.ADataException;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.ARuntimeException;
import core.exception.runtime.InvalidServerModelException;
import core.exec.network.APassphrase;
import core.exec.network.OpenKeePassPassphrase;
import core.iface.IUnit;
import core.model.machine.ServiceModel;
import core.model.network.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileChecksumUnit;
import core.unit.fs.FileChecksumUnit.Checksum;
import core.unit.fs.FileDownloadUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import profile.stack.Virtualisation;

public class HyperVisor extends AStructuredProfile {

	// TODO: roll scripts back in
	private final Virtualisation hypervisor;
	private final Map<String, ServiceModel> services;

	public HyperVisor(String label, NetworkModel networkModel) throws InvalidServerModelException {
		super(label, networkModel);

		this.hypervisor = new Virtualisation(label, networkModel);
		this.services = new LinkedHashMap<>();
	}

	public void addService(String label, ServiceModel service) {
		this.services.put(label, service);
	}

	public Map<String, ServiceModel> getServices() {
		return this.services;
	}

	@Override
	protected Collection<IUnit> getInstalled() throws InvalidServerException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.hypervisor.getInstalled());

		units.add(new DirUnit("media_dir", "proceed",
				getNetworkModel().getData().getHypervisorThornsecBase(getLabel()).toString()));

		units.add(new InstalledUnit("whois", "proceed", "whois"));
		units.add(new InstalledUnit("tmux", "proceed", "tmux"));
		units.add(new InstalledUnit("socat", "proceed", "socat"));

		return units;
	}

	@Override
	protected Collection<IUnit> getPersistentConfig() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		final FileUnit fuseConf = new FileUnit("fuse", "proceed", "/etc/fuse.conf");
		units.add(fuseConf);
		fuseConf.appendLine("#user_allow_other");

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		getNetworkModel().getServerModel(getLabel()).addEgress("gensho.ftp.acc.umu.se");
		getNetworkModel().getServerModel(getLabel()).addEgress("github.com");

		return units;
	}

	@Override
	protected Collection<IUnit> getLiveConfig() throws ARuntimeException, ADataException {
		final Collection<IUnit> units = new ArrayList<>();

		final Set<String> urls = new LinkedHashSet<>();

		for (final String service : getServices().keySet()) {
			final String newURL = getNetworkModel().getData().getDebianIsoUrl(service);
			if (urls.contains(newURL)) {
				continue;
			} else {
				urls.add(newURL);
			}
		}

		for (final String url : urls) {
			String filename = null;
			String cleanedFilename = null;

			try {
				filename = Paths.get(new URI(url).getPath()).getFileName().toString();
				cleanedFilename = filename.replaceAll("[^A-Za-z0-9]", "_");
			} catch (final Exception e) {
				JOptionPane.showMessageDialog(null, "It doesn't appear that " + url
						+ " is a valid link to a Debian ISO.\n\nPlease fix this in your JSON");
				System.exit(1);
			}

			units.add(new FileDownloadUnit("debian_netinst_iso_" + cleanedFilename, "metal_genisoimage_installed", url,
					getNetworkModel().getData().getHypervisorThornsecBase(getLabel()) + "/" + filename,
					"The Debian net install ISO couldn't be downloaded.  Please check the URI in your config."));
			units.add(new FileChecksumUnit("debian_netinst_iso",
					"debian_netinst_iso_" + cleanedFilename + "_downloaded", Checksum.SHA512,
					getNetworkModel().getData().getHypervisorThornsecBase(getLabel()) + "/" + filename,
					getNetworkModel().getData().getDebianIsoSha512(getLabel()),
					"The sha512 sum of the Debian net install in your config doesn't match what has been downloaded.  This could mean your connection is man-in-the-middle'd, or it could just be that the file has been updated on the server. "
							+ "Please check http://cdimage.debian.org/debian-cd/current/amd64/iso-cd/SHA512SUMS (64 bit) or http://cdimage.debian.org/debian-cd/current/i386/iso-cd/SHA512SUMS (32 bit) for the correct checksum."));
		}

		for (final String service : getServices().keySet()) {
			String password = "";
			final APassphrase pass = new OpenKeePassPassphrase(service, getNetworkModel());

			if (pass.init()) {
				password = pass.getPassphrase();

				password = Pattern.quote(password); // Turn special characters into literal so they don't get parsed out
				password = password.substring(2, password.length() - 2).trim(); // Remove '\Q' and '\E' from
																				// beginning/end since we're not using
																				// this as a regex
				password = password.replace("\"", "\\\""); // Also, make sure quote marks are properly escaped!
			}

			if (pass.isADefaultPassphrase()) {
			}

			units.add(new SimpleUnit(service + "_password", "proceed",
					service.toUpperCase() + "_PASSWORD=`printf \"" + password + "\" | mkpasswd -s -m md5`",
					"echo $" + service.toUpperCase() + "_PASSWORD", "", "fail",
					"Couldn't set the passphrase for " + service + ".  You won't be able to configure this service."));

			// TODO: Networking stuff in here
//			String bridge = this.networkModel.getData().getMetalIface(serviceLabel);
//
//			if ((bridge == null) || bridge.equals("")) {
//				if (this.networkModel.getData().isRouter(getLabel())) {
//					bridge = "vm" + service.getThirdOctet();
//				} else {
//					bridge = this.me.getInterfaces().get(0).getIface();
//				}
//			}

			// TODO: iso/vms
			// units.addAll(this.hypervisor.buildIso(service.getLabel(),
			// this.hypervisor.preseed(service.getLabel(), expirePasswords)));
			// units.addAll(this.hypervisor.buildServiceVm(service.getLabel(), bridge));

			final String bootDiskDir = getNetworkModel().getData().getHypervisorThornsecBase(getLabel())
					+ "/disks/boot/" + service + "/";
			final String dataDiskDir = getNetworkModel().getData().getHypervisorThornsecBase(getLabel())
					+ "/disks/data/" + service + "/";

			units.add(new SimpleUnit(service + "_boot_disk_formatted", "proceed", "",
					"sudo bash -c 'export LIBGUESTFS_BACKEND_SETTINGS=force_tcg;" + "virt-filesystems -a " + bootDiskDir
							+ service + "_boot.v*'",
					"", "fail",
					"Boot disk is unformatted (therefore has no OS on it), please configure the service and try mounting again."));

			// For now, do this as root. We probably want to move to another user, idk
			units.add(new SimpleUnit(service + "_boot_disk_loopback_mounted", service + "_boot_disk_formatted",
					"sudo bash -c '" + " export LIBGUESTFS_BACKEND_SETTINGS=force_tcg;" + " guestmount -a "
							+ bootDiskDir + service + "_boot.v*" + " -i" // Inspect the disk for the relevant
																			// partition
							+ " -o direct_io" // All read operations must be done against live, not cache
							+ " --ro" // _MOUNT THE DISK READ ONLY_
							+ " " + bootDiskDir + "live/" + "'",
					"sudo mount | grep " + bootDiskDir, "", "fail",
					"I was unable to loopback mount the boot disk for " + service + " in " + getLabel() + "."));

			units.add(new SimpleUnit(service + "_data_disk_formatted", "proceed", "",
					"sudo bash -c 'export LIBGUESTFS_BACKEND_SETTINGS=force_tcg;" + "virt-filesystems -a " + dataDiskDir
							+ service + "_data.v*'",
					"", "fail",
					"Data disk is unformatted (therefore hasn't been configured), please configure the service and try mounting again."));

			units.add(new SimpleUnit(service + "_data_disk_loopback_mounted", service + "_data_disk_formatted",
					"sudo bash -c '" + " export LIBGUESTFS_BACKEND_SETTINGS=force_tcg;" + " guestmount -a "
							+ dataDiskDir + service + "_data.v*" + " -m /dev/sda1" // Mount the first
																					// partition
							+ " -o direct_io" // All read operations must be done against live, not cache
							+ " --ro" // _MOUNT THE DISK READ ONLY_
							+ " " + dataDiskDir + "live/" + "'",
					"sudo mount | grep " + dataDiskDir, "", "fail", "I was unable to loopback mount the data disk for "
							+ service + " in " + getLabel() + ".  Backups will not work."));
		}

		return units;
	}
}
