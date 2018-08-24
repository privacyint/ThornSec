package profile;

import java.util.Vector;
import java.util.regex.Pattern;

import core.exec.PasswordExec;
import core.iface.IUnit;
import core.model.FirewallModel;
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
	private HypervisorScripts backups;
	
	public Metal() {
		super("metal");
		
		hypervisor = new Virtualisation();
		backups = new HypervisorScripts();
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
		
		//Routers build their own external ifaces.
		//Next step will be to allow more than one iface on a non-Router HyperVisor, so we can do load balancing
		if (!model.getServerModel(server).isRouter()) {
			units.addElement(model.getServerModel(server).getInterfaceModel().addIface(server.replace("-", "_") + "_primary_iface",
					   "static",
					   model.getData().getIface(server),
					   null,
					   model.getServerModel(server).getIP(),
					   model.getData().getNetmask(),
					   null,
					   model.getServerModel(server).getGateway()));
		}
		else {
			units.addElement(im.addIface(server.replace("-", "_") + "_br" + model.getData().getSubnet(server),
					   "static",
					   "br" + model.getData().getSubnet(server),
					   "none",
					   model.getServerModel(server).getGateway(),
					   model.getData().getNetmask(),
					   null,
					   null));
		
			for (String service : model.getServerModel(server).getServices()) {
				units.addElement(im.addIface(server.replace("-", "_") + "_br" + model.getData().getSubnet(service),
									   "static",
									   "br" + model.getData().getSubnet(service),
									   "none",
									   model.getServerModel(service).getGateway(),
									   model.getData().getNetmask(),
									   null,
									   null));
			}
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
			
			String bridge = "";
			//If we're also a router, bind to a bridge to keep the traffic off the external iface
			if (model.getServerModel(server).isRouter()) {
				bridge = "br" + model.getData().getSubnet(service);
			}
			//Otherwise, just bind to the physical iface
			else {
				bridge = model.getData().getIface(server);
			}
			
			units.addAll(hypervisor.buildIso(server, service, model, hypervisor.preseed(server, service, model, expirePasswords)));
			units.addAll(hypervisor.buildVm(server, service, model, bridge));
		}
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		for (String router : model.getRouters()) {
			
			FirewallModel routerFm = model.getServerModel(router).getFirewallModel();
		
			routerFm.addFilter(server + "_egress_25_allow", server + "_egress",
					"-p tcp"
					+ " --dport 25"
					+ " -j ACCEPT");
		}
				
		model.getServerModel(server).addRouterEgressFirewallRule(server, model, "allow_debian_cd_image", "gensho.ftp.acc.umu.se", new String[]{"443"});
		model.getServerModel(server).addRouterEgressFirewallRule(server, model, "allow_github", "github.com", new String[]{"443"});
		
		model.getServerModel(server).addRouterPoison(server, model, "cdn.debian.net", "130.89.148.14", new String[] {"80"});
		model.getServerModel(server).addRouterPoison(server, model, "security-cdn.debian.org", "151.101.0.204", new String[] {"80"});
		model.getServerModel(server).addRouterPoison(server, model, "prod.debian.map.fastly.net", "151.101.36.204", new String[] {"80"});
		model.getServerModel(server).addRouterPoison(server, model, "download.virtualbox.org", "2.19.60.219", new String[]{"80"});
		
		return units;
	}
}
