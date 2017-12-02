package profile;

import java.util.Vector;
import java.util.regex.Pattern;

import core.exec.PasswordExec;
import core.iface.IUnit;
import core.model.InterfaceModel;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileChecksumUnit;
import core.unit.fs.FileDownloadUnit;
import core.unit.pkg.InstalledUnit;

public class Metal extends AStructuredProfile {
	
	private Virtualisation hypervisor;
	private Backups backups;
	
	public Metal() {
		super("metal");
		
		hypervisor = new Virtualisation();
		backups = new Backups();
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(hypervisor.getInstalled(server, model));
		units.addAll(backups.getInstalled(server, model));
		
		units.addElement(new DirUnit("media_dir", "proceed", model.getData().getVmBase(server)));

		units.addElement(new InstalledUnit("whois", "proceed", "whois"));
		
		units.addElement(new FileDownloadUnit("debian_netinst_iso", "metal_genisoimage_installed", model.getData().getDebianIsoUrl(server), model.getData().getVmBase(server) + "/debian-netinst.iso",
											  "The Debian net install ISO couldn't be downloaded.  Please check the URI in your config."));
		units.addElement(new FileChecksumUnit("debian_netinst_iso", "debian_netinst_iso_downloaded", model.getData().getVmBase(server) + "/debian-netinst.iso", model.getData().getDebianIsoSha512(server),
											  "The sha512 sum of the Debian net install in your config doesn't match what has been downloaded.  This could mean your connection is man-in-the-middle'd, or it could just be that the file has been updated on the server. "
											  + "Please check http://cdimage.debian.org/debian-cd/current/amd64/iso-cd/SHA512SUMS (64 bit) or http://cdimage.debian.org/debian-cd/current/i386/iso-cd/SHA512SUMS (32 bit) for the correct checksum."));
		
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();

		InterfaceModel im = model.getServerModel(server).getInterfaceModel();
		
		String serverCleanName = model.getData().getHostname(server);
		String iface           = model.getData().getIface(server);
		String netmask         = model.getData().getNetmask();
		
		//Add our actual physical iface
		units.addElement(im.addIface(serverCleanName + "_primary_iface",
								   "static",
								   iface,
								   null,
								   model.getServerModel(server).getGateway(),
								   netmask,
								   null,
								   null));
		
		//Add aliases for services
		for (String service : model.getServerModel(server).getServices()) {
			String subnet           = model.getData().getSubnet(service);
			String serviceCleanName = service.replaceAll("-", "_");
			String gateway          = model.getServerModel(service).getGateway();
			
			units.addElement(im.addIface(serverCleanName + "_" + serviceCleanName + "_iface",
								   "static",
								   iface + ":" + subnet,
								   null,
								   gateway,
								   netmask,
								   null,
								   null));
		}

		units.addAll(backups.getPersistentConfig(server, model));
	
		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		for (String service : model.getServerModel(server).getServices()) {
			String password = "";
			Boolean expirePasswords = false;
			
			PasswordExec pass   = new PasswordExec(service, model);
			if (pass.init()) {
				password = pass.getPassword();
			}
			
			if (password.equals("")) {
				password = "secret";
				expirePasswords = true;
			}
			else {
				password = Pattern.quote(password); //Turn special characters into literal so they don't get parsed out
				password = password.substring(2, password.length()-3).trim(); //Remove '\Q' and '\E' from beginning/end since we're not using this as a regex
				password = password.replace("\"", "\\\""); //Also, make sure quote marks are properly escaped!
			}
			
			units.addElement(new SimpleUnit(service + "_password", "proceed",
					service.toUpperCase() + "_PASSWORD=`printf \"" + password + "\" | mkpasswd -s -m md5`",
					"echo $" + service.toUpperCase() + "_PASSWORD", "", "fail",
					"Couldn't set the passphrase for " + service + ".  You won't be able to configure this service."));
			
			units.addAll(hypervisor.buildIso(server, service, model, hypervisor.preseed(server, service, model, expirePasswords)));
			units.addAll(hypervisor.buildVm(server, service, model, model.getData().getIface(server) + ":" + model.getData().getSubnet(service)));
		}
		
		return units;
	}
	

}