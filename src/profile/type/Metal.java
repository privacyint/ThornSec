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
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import core.exception.data.ADataException;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.ARuntimeException;
import core.exception.runtime.InvalidServerModelException;
import core.exec.network.APassphrase;
import core.exec.network.OpenKeePassPassphrase;
import core.iface.IUnit;
import core.model.machine.ServerModel;
import core.model.network.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileChecksumUnit;
import core.unit.fs.FileDownloadUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import profile.stack.Virtualisation;

public class Metal extends AStructuredProfile {

	// TODO: roll scripts back in
	private final Virtualisation hypervisor;
	private final HashSet<ServerModel> services;

	public Metal(String label, NetworkModel networkModel) throws InvalidServerModelException {
		super(label, networkModel);

		this.hypervisor = new Virtualisation(label, networkModel);
		this.services = new HashSet<>();

		this.networkModel.getServerModel(label).setFirstOctet(10);
		this.networkModel.getServerModel(label)
				.setSecondOctet(this.networkModel.getMetalServers().indexOf(networkModel.getServerModel(label)) + 1);
		this.networkModel.getServerModel(label).setThirdOctet(0);
	}

	public HashSet<ServerModel> getServices() {
		return this.services;
	}

	@Override
	protected Set<IUnit> getInstalled() throws InvalidServerException {
		final Set<IUnit> units = new HashSet<>();

		units.addAll(this.hypervisor.getInstalled());

		units.add(new DirUnit("media_dir", "proceed",
				this.networkModel.getData().getHypervisorThornsecBase(getLabel()).toString()));

		units.add(new InstalledUnit("whois", "proceed", "whois"));
		units.add(new InstalledUnit("tmux", "proceed", "tmux"));
		units.add(new InstalledUnit("socat", "proceed", "socat"));

		return units;
	}

	@Override
	protected Set<IUnit> getPersistentConfig() {
		final Set<IUnit> units = new HashSet<>();

		final FileUnit fuseConf = new FileUnit("fuse", "proceed", "/etc/fuse.conf");
		units.add(fuseConf);
		fuseConf.appendLine("#user_allow_other");

		return units;
	}

	@Override
	public Set<IUnit> getPersistentFirewall() throws InvalidServerModelException {
		final Set<IUnit> units = new HashSet<>();

//		final NetworkInterfaceModel im = this.me.getLANInterfaces();
//
//
//		final int i = 0;
//
//		// Add this machine's interfaces
//		for (final Map.Entry<String, String> lanIface : this.networkModel.getData().getLanIfaces(getLabel())
//				.entrySet()) {
//			if (this.me.isRouter()
//					|| this.networkModel.getData().getWanIfaces(getLabel()).containsKey(lanIface.getKey())) { // Try
//				// not
//				// to
//				// duplicate
//				// ifaces
//				// if
//				// we're
//				// a
//				// Router/Metal
//				continue;
//			}
//
//			final InetAddress subnet = this.networkModel.stringToIP(this.me.getFirstOctet() + "."
//					+ this.me.getSecondOctet() + "." + this.me.getThirdOctet() + "." + (i * 4));
//			final InetAddress router = this.networkModel.stringToIP(this.me.getFirstOctet() + "."
//					+ this.me.getSecondOctet() + "." + this.me.getThirdOctet() + "." + ((i * 4) + 1));
//			final InetAddress address = this.networkModel.stringToIP(this.me.getFirstOctet() + "."
//					+ this.me.getSecondOctet() + "." + this.me.getThirdOctet() + "." + ((i * 4) + 2));
//			final InetAddress broadcast = this.networkModel.stringToIP(this.me.getFirstOctet() + "."
//					+ this.me.getSecondOctet() + "." + this.me.getThirdOctet() + "." + ((i * 4) + 3));
//			final InetAddress netmask = this.networkModel.getData().getNetmask();
//
//			im.addIface(new InterfaceData(getLabel(), lanIface.getKey(), lanIface.getValue(), "static", null, subnet,
//					address, netmask, broadcast, router, "comment goes here"));
//		}

		this.networkModel.getServerModel(getLabel()).addEgress("gensho.ftp.acc.umu.se");
		this.networkModel.getServerModel(getLabel()).addEgress("github.com");

		return units;
	}

	@Override
	protected Set<IUnit> getLiveConfig() throws ARuntimeException, ADataException {
		final Set<IUnit> units = new HashSet<>();

		final Vector<String> urls = new Vector<>();
		for (final ServerModel service : this.networkModel.getServices(getLabel())) {
			final String newURL = this.networkModel.getData().getDebianIsoUrl(service.getLabel());
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
					this.networkModel.getData().getHypervisorThornsecBase(getLabel()) + "/" + filename,
					"The Debian net install ISO couldn't be downloaded.  Please check the URI in your config."));
			units.add(new FileChecksumUnit("debian_netinst_iso",
					"debian_netinst_iso_" + cleanedFilename + "_downloaded",
					this.networkModel.getData().getHypervisorThornsecBase(getLabel()) + "/" + filename,
					this.networkModel.getData().getDebianIsoSha512(getLabel()),
					"The sha512 sum of the Debian net install in your config doesn't match what has been downloaded.  This could mean your connection is man-in-the-middle'd, or it could just be that the file has been updated on the server. "
							+ "Please check http://cdimage.debian.org/debian-cd/current/amd64/iso-cd/SHA512SUMS (64 bit) or http://cdimage.debian.org/debian-cd/current/i386/iso-cd/SHA512SUMS (32 bit) for the correct checksum."));
		}

		for (final ServerModel service : this.networkModel.getServices(getLabel())) {
			String password = "";
			final String serviceLabel = service.getLabel();
			Boolean expirePasswords = false;

			final APassphrase pass = new OpenKeePassPassphrase(serviceLabel, this.networkModel);

			if (pass.init()) {
				password = pass.getPassphrase();

				password = Pattern.quote(password); // Turn special characters into literal so they don't get parsed out
				password = password.substring(2, password.length() - 2).trim(); // Remove '\Q' and '\E' from
																				// beginning/end since we're not using
																				// this as a regex
				password = password.replace("\"", "\\\""); // Also, make sure quote marks are properly escaped!
			}

			if (pass.isADefaultPassphrase()) {
				expirePasswords = true;
			}

			units.add(new SimpleUnit(serviceLabel + "_password", "proceed",
					serviceLabel.toUpperCase() + "_PASSWORD=`printf \"" + password + "\" | mkpasswd -s -m md5`",
					"echo $" + serviceLabel.toUpperCase() + "_PASSWORD", "", "fail", "Couldn't set the passphrase for "
							+ serviceLabel + ".  You won't be able to configure this service."));

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

			final String bootDiskDir = this.networkModel.getData().getHypervisorThornsecBase(getLabel())
					+ "/disks/boot/" + serviceLabel + "/";
			final String dataDiskDir = this.networkModel.getData().getHypervisorThornsecBase(getLabel())
					+ "/disks/data/" + serviceLabel + "/";

			units.add(new SimpleUnit(serviceLabel + "_boot_disk_formatted", "proceed", "",
					"sudo bash -c 'export LIBGUESTFS_BACKEND_SETTINGS=force_tcg;" + "virt-filesystems -a " + bootDiskDir
							+ serviceLabel + "_boot.v*'",
					"", "fail",
					"Boot disk is unformatted (therefore has no OS on it), please configure the service and try mounting again."));

			// For now, do this as root. We probably want to move to another user, idk
			units.add(new SimpleUnit(serviceLabel + "_boot_disk_loopback_mounted",
					serviceLabel + "_boot_disk_formatted",
					"sudo bash -c '" + " export LIBGUESTFS_BACKEND_SETTINGS=force_tcg;" + " guestmount -a "
							+ bootDiskDir + serviceLabel + "_boot.v*" + " -i" // Inspect the disk for the relevant
																				// partition
							+ " -o direct_io" // All read operations must be done against live, not cache
							+ " --ro" // _MOUNT THE DISK READ ONLY_
							+ " " + bootDiskDir + "live/" + "'",
					"sudo mount | grep " + bootDiskDir, "", "fail",
					"I was unable to loopback mount the boot disk for " + serviceLabel + " in " + getLabel() + "."));

			units.add(new SimpleUnit(serviceLabel + "_data_disk_formatted", "proceed", "",
					"sudo bash -c 'export LIBGUESTFS_BACKEND_SETTINGS=force_tcg;" + "virt-filesystems -a " + dataDiskDir
							+ serviceLabel + "_data.v*'",
					"", "fail",
					"Data disk is unformatted (therefore hasn't been configured), please configure the service and try mounting again."));

			units.add(
					new SimpleUnit(serviceLabel + "_data_disk_loopback_mounted", serviceLabel + "_data_disk_formatted",
							"sudo bash -c '" + " export LIBGUESTFS_BACKEND_SETTINGS=force_tcg;" + " guestmount -a "
									+ dataDiskDir + serviceLabel + "_data.v*" + " -m /dev/sda1" // Mount the first
																								// partition
									+ " -o direct_io" // All read operations must be done against live, not cache
									+ " --ro" // _MOUNT THE DISK READ ONLY_
									+ " " + dataDiskDir + "live/" + "'",
							"sudo mount | grep " + dataDiskDir, "", "fail",
							"I was unable to loopback mount the data disk for " + serviceLabel + " in " + getLabel()
									+ ".  Backups will not work."));
		}

		return units;
	}
}
