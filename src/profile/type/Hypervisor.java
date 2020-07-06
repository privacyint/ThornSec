/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.type;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import javax.json.stream.JsonParsingException;
import javax.swing.JOptionPane;
import core.StringUtils;
import core.data.machine.AMachineData.MachineType;
import core.data.machine.HypervisorData;
import core.data.machine.ServerData;
import core.data.machine.configuration.NetworkInterfaceData;
import core.data.machine.configuration.DiskData.Medium;
import core.data.machine.configuration.NetworkInterfaceData.Direction;
import core.exception.AThornSecException;
import core.exception.data.ADataException;
import core.exception.data.NoValidUsersException;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.ARuntimeException;
import core.exception.runtime.InvalidMachineModelException;
import core.exception.runtime.InvalidServerModelException;
import core.exec.network.APassphrase;
import core.exec.network.OpenKeePassPassphrase;
import core.iface.IUnit;
import core.model.machine.AMachineModel;
import core.model.machine.HypervisorModel;
import core.model.machine.ServerModel;
import core.model.machine.ServiceModel;
import core.model.machine.configuration.DiskModel;
import core.model.machine.configuration.networking.DHCPClientInterfaceModel;
import core.model.machine.configuration.networking.NetworkInterfaceModel;
import core.model.machine.configuration.networking.StaticInterfaceModel;
import core.model.network.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileChecksumUnit;
import core.unit.fs.FileChecksumUnit.Checksum;
import core.unit.fs.FileDownloadUnit;
import core.unit.pkg.InstalledUnit;
import inet.ipaddr.IPAddress;
import profile.HypervisorScripts;
import profile.hypervisor.AHypervisorProfile;
import profile.hypervisor.Virtualbox;

/**
 * This is the representation of your HyperVisor itself.
 * 
 * These are things which should be done on a HyperVisor machine, regardless of
 * what hypervisor layer it's actually running
 */
public class Hypervisor extends AStructuredProfile {

	private final AHypervisorProfile virtualbox;
	private final HypervisorScripts scripts;
	
	private Set<ServiceModel> services;

	/**
	 * Create a new HyperVisor box, with initialised NICs, and initialise the
	 * virtualisation layer itself, including the building of Service machines
	 * 
	 * @param myData
	 * @param networkModel
	 * @throws JsonParsingException
	 * @throws ADataException
	 * @throws InvalidMachineModelException 
	 */
	public Hypervisor(HypervisorModel me) throws JsonParsingException, ADataException, InvalidMachineModelException {
		super(me);

		if (!me.isType(MachineType.ROUTER)) {
/*			try {
				if (getMachineModel().getNetworkInterfaces().get(Direction.WAN) != null) {
					for (final NetworkInterfaceData wanNic : getMachineModel().getNetworkInterfaces()
							.get(Direction.WAN).values()) {
						NetworkInterfaceModel link = null;
	
						switch (wanNic.getInet()) {
						case STATIC:
							link = new StaticInterfaceModel(wanNic.getIface());
							break;
						case DHCP:
							link = new DHCPClientInterfaceModel(wanNic.getIface());
							break;
						default:
						}
						link.addAddress(wanNic.getAddresses().toArray(IPAddress[]::new));
						link.setGateway(wanNic.getGateway());
						link.setBroadcast(wanNic.getBroadcast());
						link.setMac(wanNic.getMAC());
						link.setIsIPMasquerading(true);
						me.addNetworkInterface(link);
					}
				}
				for (final NetworkInterfaceData lanNic : networkModel.getData().getNetworkInterfaces(getLabel())
						.get(Direction.LAN).values()) {
					NetworkInterfaceModel link = null;
	
					switch (lanNic.getInet()) {
					case STATIC:
						link = new StaticInterfaceModel(lanNic.getIface());
						break;
					case DHCP:
						link = new DHCPClientInterfaceModel(lanNic.getIface());
						break;
					default:
					}
					if (lanNic.getAddresses() != null) {
						link.addAddress(lanNic.getAddresses().toArray(IPAddress[]::new));
					}
					link.setGateway(lanNic.getGateway());
					link.setBroadcast(lanNic.getBroadcast());
					link.setMac(lanNic.getMAC());
					me.addNetworkInterface(link);
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
			*/
		}
		
		this.virtualbox = new Virtualbox(me);
		this.scripts = new HypervisorScripts(me);
		
		addServices();
	}

	private void addServices() throws InvalidMachineModelException {
		this.services = getServerModel().getServices();
	}
		
	public Set<ServiceModel> getServices() {
		return this.services;
	}

	@Override
	public Collection<IUnit> getInstalled() throws AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.virtualbox.getInstalled());
		units.addAll(this.scripts.getInstalled());

		units.add(new DirUnit("thornsec_base_dir", "proceed", getServerModel().getVMBase().getAbsolutePath()));

		units.add(new InstalledUnit("whois", "proceed", "whois"));
		units.add(new InstalledUnit("tmux", "proceed", "tmux"));
		units.add(new InstalledUnit("socat", "proceed", "socat"));

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentConfig() throws AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.virtualbox.getPersistentConfig());
		units.addAll(this.scripts.getPersistentConfig());

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.virtualbox.getPersistentFirewall());
		units.addAll(this.scripts.getPersistentFirewall());

		//getMachineModel().addEgress("gensho.ftp.acc.umu.se:80");
		//getMachineModel().addEgress("github.com:443");

		return units;
	}

	private Collection<IUnit> getISODownloadUnits() throws InvalidServerException {
		final Collection<IUnit> units = new ArrayList<>();

		String filename = null;
		String cleanedFilename = null;

		try {
			filename = Paths.get(new URI(url).getPath()).getFileName().toString();
			cleanedFilename = StringUtils.stringToAlphaNumeric(filename, "_");
		}
		catch (final Exception e) {
			JOptionPane.showMessageDialog(null, "It doesn't appear that " + url + " is a valid link to a Debian ISO.\n\nPlease fix this in your JSON");
			System.exit(1);
		}

		units.add(new FileDownloadUnit(cleanedFilename, "metal_genisoimage_installed", url,
				getServerModel().getVMBase().getAbsolutePath() + "/" + filename,
				"The Debian net install ISO couldn't be downloaded.  Please check the URI in your config."));
		units.add(new FileChecksumUnit(cleanedFilename, cleanedFilename + "_downloaded", Checksum.SHA512,
				getServerModel().getVMBase().getAbsolutePath() + "/" + filename, checksum,
				"The sha512 sum of the Debian net install in your config doesn't match what has been downloaded."
				+ " This could mean your connection is man-in-the-middle'd, that the download was corrupted,"
				+ " or it could just be that the file has been updated on the server."
				+ " Please check http://cdimage.debian.org/debian-cd/current/amd64/iso-cd/SHA512SUMS (64 bit)"
				+ " or http://cdimage.debian.org/debian-cd/current/i386/iso-cd/SHA512SUMS (32 bit) for the correct checksum."));
		
		return units;
	}
	
	private String getNetworkBridge() throws InvalidServerException, InvalidMachineModelException {
		if (getMachineModel().isType(MachineType.ROUTER)) {
			return MachineType.SERVER.toString();
		}

		Collection<NetworkInterfaceData> lanNics = null;
		try {
			lanNics = getNetworkModel().getData().getNetworkInterfaces(getLabel()).get(Direction.LAN).values();
		} catch (JsonParsingException | ADataException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return lanNics.iterator().next().getIface();
	}
	
	@Override
	public Collection<IUnit> getLiveConfig() throws AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		for (final ServiceModel service : getServices()) {
			units.addAll(service.getOS().getISODownloadUnits();
			units.addAll(getUserPasswordUnits());

			//try {
				//TODO
				//units.addAll(this.hypervisor.buildIso(service));
			//} catch (InvalidServerException | InvalidServerModelException | NoValidUsersException | MalformedURLException | URISyntaxException e) {
				// TODO Auto-generated catch block
			//	e.printStackTrace();
			//}
			units.addAll(virtualbox.buildServiceVm(service, getNetworkBridge()));
			units.addAll(getDisksFormattedUnits(service));
			units.addAll(getDiskLoopbackUnits(service));
		}

		units.addAll(this.hypervisor.getLiveConfig());
		units.addAll(this.scripts.getLiveConfig());
		
		return units;
	}

	private Collection<IUnit> getDisksFormattedUnits(String service) throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();
		
		((ServiceModel)getNetworkModel().getMachineModel(service)).getDisks().forEach((label, disk) -> {
			// For now, do all this as root. We probably want to move to another user, idk
			units.add(new SimpleUnit(service + "_" + label + "_disk_formatted", "proceed",
					"", //No config to do here
					"sudo bash -c 'export LIBGUESTFS_BACKEND_SETTINGS=force_tcg;" + "virt-filesystems -a " + disk.getFilename() + "'", "", "fail",
					service + "'s " + label + " disk is unformatted, please configure the service and try mounting again."));
		});
		
		return units;
	}
	
	private Collection<IUnit> getDiskLoopbackUnits(String service) throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();
		
		for (DiskModel disk : ((ServiceModel)getNetworkModel().getMachineModel(service)).getDisks().values().stream().filter(disk -> disk.getMedium() == Medium.DISK).toArray(DiskModel[]::new)) {
			// For now, do all this as root. We probably want to move to another user, idk
			
			units.add(new SimpleUnit(service + "_" + disk.getLabel() + "_disk_loopback_mounted", service + "_" + disk.getLabel() + "_disk_formatted",
					"sudo bash -c '" + " export LIBGUESTFS_BACKEND_SETTINGS=force_tcg;" + " guestmount -a " + disk.getFilename() + " -i" // Inspect the disk for the relevant partition
							+ " -o direct_io" // All read operations must be done against live, not cache
							+ " --ro" // _MOUNT THE DISK READ ONLY_
							+ " " + disk.getFilePath() + "/live/" + "'",
					"sudo mount | grep " + disk.getFilePath(), "", "fail",
					"I was unable to loopback mount the " + disk.getLabel() + " disk for " + service + " in " + getMachineModel().getLabel() + "."));
		}
		
		return units;
	}
	
	@Override
	public HypervisorModel getServerModel() {
		return (HypervisorModel) getServerModel();
	}
}
