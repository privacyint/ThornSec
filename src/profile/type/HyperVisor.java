/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.type;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.json.stream.JsonParsingException;
import javax.swing.JOptionPane;
import core.StringUtils;
import core.data.machine.AMachineData.MachineType;
import core.data.machine.HypervisorData;
import core.data.machine.UserDeviceData;
import core.data.machine.configuration.NetworkInterfaceData;
import core.data.machine.configuration.DiskData.Medium;
import core.data.machine.configuration.NetworkInterfaceData.Direction;
import core.exception.AThornSecException;
import core.exception.data.ADataException;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.ARuntimeException;
import core.exception.runtime.InvalidMachineModelException;
import core.exception.runtime.InvalidServerModelException;
import core.exec.network.APassphrase;
import core.exec.network.OpenKeePassPassphrase;
import core.iface.IUnit;
import core.model.machine.ServerModel;
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
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import inet.ipaddr.IPAddress;
import profile.HypervisorScripts;
import profile.stack.Virtualisation;

public class HyperVisor extends AStructuredProfile {

	private final Virtualisation hypervisor;
	private final HypervisorScripts scripts;
	
	private Map<String, ServerModel> services;

	public HyperVisor(String label, NetworkModel networkModel) throws InvalidServerModelException, JsonParsingException, ADataException {
		super(label, networkModel);

		final ServerModel me = getNetworkModel().getServerModel(getLabel());
	
		if (!me.isRouter()) {
			try {
				if (networkModel.getData().getNetworkInterfaces(getLabel()).get(Direction.WAN) != null) {
					for (final NetworkInterfaceData wanNic : networkModel.getData().getNetworkInterfaces(getLabel())
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
		}
		
		this.hypervisor = new Virtualisation(label, networkModel);
		this.scripts = new HypervisorScripts(label, networkModel);
		this.services = null;
	}

	public void addService(String label, ServerModel service) {
		if (this.services == null) {
			this.services = new LinkedHashMap<>();
		}
		
		this.services.put(label, service);
	}

	public Map<String, ServerModel> getServices() {
		return this.services;
	}

	@Override
	protected Collection<IUnit> getInstalled() throws InvalidServerException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.hypervisor.getInstalled());
		units.addAll(this.scripts.getInstalled());

		units.add(new DirUnit("media_dir", "proceed", getNetworkModel().getData().getHypervisorThornsecBase(getLabel()).toString()));

		units.add(new InstalledUnit("whois", "proceed", "whois"));
		units.add(new InstalledUnit("tmux", "proceed", "tmux"));
		units.add(new InstalledUnit("socat", "proceed", "socat"));

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentConfig() throws InvalidServerModelException, InvalidServerException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.hypervisor.getPersistentConfig());
		units.addAll(this.scripts.getPersistentConfig());

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.hypervisor.getPersistentFirewall());
		units.addAll(this.scripts.getPersistentFirewall());

		getNetworkModel().getServerModel(getLabel()).addEgress("gensho.ftp.acc.umu.se:80");
		getNetworkModel().getServerModel(getLabel()).addEgress("github.com:443");

		return units;
	}

	private Collection<IUnit> getISODownloadUnits(String url, String checksum) throws InvalidServerException {
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
				getNetworkModel().getData().getHypervisorThornsecBase(getLabel()) + "/" + filename,
				"The Debian net install ISO couldn't be downloaded.  Please check the URI in your config."));
		units.add(new FileChecksumUnit(cleanedFilename, cleanedFilename + "_downloaded", Checksum.SHA512,
				getNetworkModel().getData().getHypervisorThornsecBase(getLabel()) + "/" + filename, checksum,
				"The sha512 sum of the Debian net install in your config doesn't match what has been downloaded."
				+ " This could mean your connection is man-in-the-middle'd, that the download was corrupted,"
				+ " or it could just be that the file has been updated on the server."
				+ " Please check http://cdimage.debian.org/debian-cd/current/amd64/iso-cd/SHA512SUMS (64 bit)"
				+ " or http://cdimage.debian.org/debian-cd/current/i386/iso-cd/SHA512SUMS (32 bit) for the correct checksum."));
		
		return units;
	}
	
	private Collection<IUnit> getUserPasswordUnits(String service) throws ARuntimeException, ADataException {
		final Collection<IUnit> units = new ArrayList<>();

		String password = "";
		final APassphrase pass = new OpenKeePassPassphrase(service, getNetworkModel());

		if (pass.init()) {
			password = pass.getPassphrase();
			password = Pattern.quote(password); // Turn special characters into literal so they don't get parsed out
			password = password.substring(2, password.length() - 2).trim(); // Remove '\Q' and '\E' from beginning/end since we're not using this as a regex
			password = password.replace("\"", "\\\""); // Also, make sure quote marks are properly escaped!
		}
		else {
			password = ((UserDeviceData)getNetworkModel().getData().getUserDevices().get(getNetworkModel().getData().getUser())).getDefaultPassphrase();
		}
		
		if (password == null || password.isEmpty()) {
			password = service;
		}

		//if (pass.isADefaultPassphrase()) {
		//}

		units.add(new SimpleUnit(service + "_password", "proceed",
				service.toUpperCase() + "_PASSWORD=$(printf \"" + password + "\" | mkpasswd -s -m md5)",
				"echo $" + service.toUpperCase() + "_PASSWORD", "", "fail",
				"Couldn't set the passphrase for " + service + "."
				+ " You won't be able to configure this service."));
		
		return units;
	}
	
	private String getNetworkBridge() throws InvalidServerException, InvalidServerModelException {
		if (getNetworkModel().getServerModel(getLabel()).isRouter()) {
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

		((HypervisorData)getNetworkModel().getData().getServers().get(getLabel())).getServices().forEach(server -> {
			try {
				addService(server.getLabel(), getNetworkModel().getServerModel(server.getLabel()));
			} catch (InvalidServerModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		
		for (final String service : getServices().keySet()) {
			units.addAll(getISODownloadUnits(getNetworkModel().getData().getDebianIsoUrl(service), getNetworkModel().getData().getDebianIsoSha512(service)));
			units.addAll(getUserPasswordUnits(service));

			try {
				units.addAll(this.hypervisor.buildIso(service));
			} catch (InvalidServerException | MalformedURLException | URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			units.addAll(this.hypervisor.buildServiceVm(service, getNetworkBridge()));
			units.addAll(getDisksFormattedUnits(service));
			units.addAll(getDiskLoopbackUnits(service));
		}

		units.addAll(this.hypervisor.getLiveConfig());
		units.addAll(this.scripts.getLiveConfig());
		
		return units;
	}

	private Collection<IUnit> getDisksFormattedUnits(String service) throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();
		
		getNetworkModel().getServiceModel(service).getDisks().forEach((label, disk) -> {
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
		
		for (DiskModel disk : getNetworkModel().getServiceModel(service).getDisks().values().stream().filter(disk -> disk.getMedium() == Medium.DISK).toArray(DiskModel[]::new)) {
			// For now, do all this as root. We probably want to move to another user, idk
			
			units.add(new SimpleUnit(service + "_" + disk.getLabel() + "_disk_loopback_mounted", service + "_" + disk.getLabel() + "_disk_formatted",
					"sudo bash -c '" + " export LIBGUESTFS_BACKEND_SETTINGS=force_tcg;" + " guestmount -a " + disk.getFilename() + " -i" // Inspect the disk for the relevant partition
							+ " -o direct_io" // All read operations must be done against live, not cache
							+ " --ro" // _MOUNT THE DISK READ ONLY_
							+ " " + disk.getFilePath() + "/live/" + "'",
					"sudo mount | grep " + disk.getFilePath(), "", "fail",
					"I was unable to loopback mount the " + disk.getLabel() + " disk for " + service + " in " + getLabel() + "."));
		}
		
		return units;
	}
}
