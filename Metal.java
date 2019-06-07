package profile;

import java.net.InetAddress;
import java.net.URI;

import java.nio.file.Paths;

import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import core.data.InterfaceData;
import core.exec.PasswordExec;
import core.iface.IUnit;
import core.model.InterfaceModel;
import core.model.NetworkModel;
import core.model.ServerModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileChecksumUnit;
import core.unit.fs.FileDownloadUnit;
import core.unit.pkg.InstalledUnit;

public class Metal extends AStructuredProfile {
	
	private Virtualisation hypervisor;
	private HypervisorScripts backups;
	
	private Vector<ServerModel> services;
	
	private ServerModel me;
	
	public Metal(ServerModel me, NetworkModel networkModel) {
		super("metal", me, networkModel);
		
		this.me = me;
		
		this.hypervisor = new Virtualisation(me, networkModel);
		this.backups    = new HypervisorScripts(me, networkModel);
		this.services   = new Vector<ServerModel>();
	}
	
	public Vector<ServerModel> getServices() {
		return services;
	}

	protected Vector<IUnit> getInstalled() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(hypervisor.getInstalled());
		units.addAll(backups.getInstalled());
		
		units.addElement(new DirUnit("media_dir", "proceed", networkModel.getData().getHypervisorThornsecBase(me.getLabel())));

		units.addElement(new InstalledUnit("whois", "proceed", "whois"));
		units.addElement(new InstalledUnit("tmux", "proceed", "tmux"));
		units.addElement(new InstalledUnit("socat", "proceed", "socat"));
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig() {
		Vector<IUnit> units = new Vector<IUnit>();

		String fuse = "";
		fuse += "#user_allow_other";
		units.addElement(((ServerModel)me).getConfigsModel().addConfigFile("fuse", "proceed", fuse, "/etc/fuse.conf"));

		units.addAll(backups.getPersistentConfig());
	
		return units;
	}
	
	public Vector<IUnit> getNetworking() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		InterfaceModel im = me.getInterfaceModel();

		me.setFirstOctet(10);
		me.setSecondOctet(networkModel.getMetalServers().indexOf(me) + 1);
		me.setThirdOctet(0);
		
		int i = 0;
		
		//Add this machine's interfaces
		for (Map.Entry<String, String> lanIface : networkModel.getData().getLanIfaces(me.getLabel()).entrySet() ) {
			if (me.isRouter() || networkModel.getData().getWanIfaces(me.getLabel()).containsKey(lanIface.getKey())) { //Try not to duplicate ifaces if we're a Router/Metal
				continue;
			}
			
			InetAddress subnet    = networkModel.stringToIP(me.getFirstOctet() + "." + me.getSecondOctet() + "." + me.getThirdOctet() + "." + (i * 4));
			InetAddress router    = networkModel.stringToIP(me.getFirstOctet() + "." + me.getSecondOctet() + "." + me.getThirdOctet() + "." + ((i * 4) + 1));
			InetAddress address   = networkModel.stringToIP(me.getFirstOctet() + "." + me.getSecondOctet() + "." + me.getThirdOctet() + "." + ((i * 4) + 2));
			InetAddress broadcast = networkModel.stringToIP(me.getFirstOctet() + "." + me.getSecondOctet() + "." + me.getThirdOctet() + "." + ((i * 4) + 3));
			InetAddress netmask   = networkModel.getData().getNetmask();
			
			im.addIface(new InterfaceData(me.getLabel(),
					lanIface.getKey(),
					lanIface.getValue(),
					"static",
					null,
					subnet,
					address,
					netmask,
					broadcast,
					router,
					"comment goes here")
			);
		}

		me.addRequiredEgress("gensho.ftp.acc.umu.se");
		me.addRequiredEgress("github.com");
		
		return units;
	}

	protected Vector<IUnit> getLiveConfig() {
		Vector<IUnit> units = new Vector<IUnit>();

		Vector<String> urls = new Vector<String>();
		for (ServerModel service : me.getServices()) {
			String newURL = networkModel.getData().getDebianIsoUrl(service.getLabel());
			if (urls.contains(newURL)) {
				continue;
			}
			else {
				urls.add(newURL);
			}
		}
		
		for (String url : urls) {
			String filename = null;
			String cleanedFilename = null;
			
			try {
				filename = Paths.get(new URI(url).getPath()).getFileName().toString();
				cleanedFilename = filename.replaceAll("[^A-Za-z0-9]", "_");
			}
			catch (Exception e) {
				JOptionPane.showMessageDialog(null, "It doesn't appear that " + url + " is a valid link to a Debian ISO.\n\nPlease fix this in your JSON");
				System.exit(1);
			}
			
			units.addElement(new FileDownloadUnit("debian_netinst_iso_" + cleanedFilename, "metal_genisoimage_installed",
									url,
									networkModel.getData().getHypervisorThornsecBase(me.getLabel()) + "/" + filename,
									"The Debian net install ISO couldn't be downloaded.  Please check the URI in your config."));
			units.addElement(new FileChecksumUnit("debian_netinst_iso", "debian_netinst_iso_" + cleanedFilename + "_downloaded",
									networkModel.getData().getHypervisorThornsecBase(me.getLabel()) + "/" + filename,
									networkModel.getData().getDebianIsoSha512(me.getLabel()),
									"The sha512 sum of the Debian net install in your config doesn't match what has been downloaded.  This could mean your connection is man-in-the-middle'd, or it could just be that the file has been updated on the server. "
									+ "Please check http://cdimage.debian.org/debian-cd/current/amd64/iso-cd/SHA512SUMS (64 bit) or http://cdimage.debian.org/debian-cd/current/i386/iso-cd/SHA512SUMS (32 bit) for the correct checksum."));
		}
		
		for (ServerModel service : me.getServices()) {
			String password = "";
			String serviceLabel = service.getLabel();
			Boolean expirePasswords = false;
			
			PasswordExec pass = new PasswordExec(serviceLabel, networkModel);
			
			if (pass.init()) {
				password = pass.getPassword();

				password = Pattern.quote(password); //Turn special characters into literal so they don't get parsed out
				password = password.substring(2, password.length()-2).trim(); //Remove '\Q' and '\E' from beginning/end since we're not using this as a regex
				password = password.replace("\"", "\\\""); //Also, make sure quote marks are properly escaped!
			}
			
			if (pass.isDefaultPassword()) {
				expirePasswords = true;
			}
			
			units.addElement(new SimpleUnit(serviceLabel + "_password", "proceed",
					serviceLabel.toUpperCase() + "_PASSWORD=`printf \"" + password + "\" | mkpasswd -s -m md5`",
					"echo $" + serviceLabel.toUpperCase() + "_PASSWORD", "", "fail",
					"Couldn't set the passphrase for " + serviceLabel + ".  You won't be able to configure this service."));
			
			String bridge = networkModel.getData().getMetalIface(serviceLabel);
			
			if (bridge == null || bridge.equals("")) {
				bridge = me.getInterfaceModel().getIfaces().elementAt(0).getIface();
			}
			
			units.addAll(hypervisor.buildIso(service.getLabel(), hypervisor.preseed(service.getLabel(), expirePasswords)));
			units.addAll(hypervisor.buildServiceVm(service.getLabel(), bridge));
			
			String bootDiskDir = networkModel.getData().getHypervisorThornsecBase(me.getLabel()) + "/disks/boot/" + serviceLabel + "/";
			String dataDiskDir = networkModel.getData().getHypervisorThornsecBase(me.getLabel()) + "/disks/data/" + serviceLabel + "/";
			
			units.addElement(new SimpleUnit(serviceLabel + "_boot_disk_formatted", "proceed",
					"",
					"sudo bash -c 'export LIBGUESTFS_BACKEND_SETTINGS=force_tcg;"
					+ "virt-filesystems -a " + bootDiskDir + serviceLabel + "_boot.v*'", "", "fail",
					"Boot disk is unformatted (therefore has no OS on it), please configure the service and try mounting again."));
			
			//For now, do this as root.  We probably want to move to another user, idk
			units.addElement(new SimpleUnit(serviceLabel + "_boot_disk_loopback_mounted", serviceLabel + "_boot_disk_formatted",
					"sudo bash -c '"
						+ " export LIBGUESTFS_BACKEND_SETTINGS=force_tcg;"
						+ " guestmount -a " + bootDiskDir + serviceLabel + "_boot.v*"
						+ " -i" //Inspect the disk for the relevant partition
						+ " -o direct_io" //All read operations must be done against live, not cache
						+ " --ro" //_MOUNT THE DISK READ ONLY_
						+ " " + bootDiskDir + "live/"
					+"'",
					"sudo mount | grep " + bootDiskDir, "", "fail",
					"I was unable to loopback mount the boot disk for " + serviceLabel + " in " + getLabel() + "."));
			
			units.addElement(new SimpleUnit(serviceLabel + "_data_disk_formatted", "proceed",
					"",
					"sudo bash -c 'export LIBGUESTFS_BACKEND_SETTINGS=force_tcg;"
					+ "virt-filesystems -a " + dataDiskDir + serviceLabel + "_data.v*'", "", "fail",
					"Data disk is unformatted (therefore hasn't been configured), please configure the service and try mounting again."));

			units.addElement(new SimpleUnit(serviceLabel + "_data_disk_loopback_mounted", serviceLabel + "_data_disk_formatted",
					"sudo bash -c '"
						+ " export LIBGUESTFS_BACKEND_SETTINGS=force_tcg;"
						+ " guestmount -a " + dataDiskDir + serviceLabel + "_data.v*"
						+ " -m /dev/sda1" //Mount the first partition
						+ " -o direct_io" //All read operations must be done against live, not cache
						+ " --ro" //_MOUNT THE DISK READ ONLY_
						+ " " + dataDiskDir + "live/"
					+"'",
					"sudo mount | grep " + dataDiskDir, "", "fail",
					"I was unable to loopback mount the data disk for " + serviceLabel + " in " + getLabel() + ".  Backups will not work."));
		}
		
		return units;
	}
}
