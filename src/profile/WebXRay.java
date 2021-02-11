package profile;

import java.util.Vector;

import javax.swing.JOptionPane;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.model.ServerModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.FileDownloadUnit;

public class WebXRay extends AStructuredProfile {
	
	public WebXRay(ServerModel me, NetworkModel networkModel) {
		super("webxray", me, networkModel);
	}

	public Vector<IUnit> getInstalled() {
		Vector<IUnit> units = new Vector<IUnit>();

		Boolean is64bit = networkModel.getData().getDebianIsoUrl(me.getLabel()).contains("amd64") ? true : false;
		String url = "https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb";
		
		if (!is64bit) {
			JOptionPane.showMessageDialog(null, "WebXRay can only be configured on a 64 bit system. Sorry!");
		}
		
		units.addElement(new FileDownloadUnit("chrome", "proceed", url, "/root/chrome.deb"));
		
		units.addElement(new SimpleUnit("chrome_installed", "chrome_downloaded",
				"export DEBIAN_FRONTEND=noninteractive;"
				+ "sudo -E dpkg -i --assume-yes /root/chrome.deb || sudo apt -f --assume-yes install",
				"google-chrome --version 2>&1", "bash: google-chrome: command not found", "fail"));
		
		//"wget https://chromedriver.storage.googleapis.com/index.html?path=$(https://chromedriver.storage.googleapis.com/LATEST_RELEASE_\\$(google-chrome --version | awk '{ print $3 }'))")
		//;
		
		return units;
	}
	
	public Vector<IUnit> getNetworking() {
		Vector<IUnit> units = new Vector<IUnit>();

		me.addRequiredEgress("255.255.255.255");
		
		return units;
	}
}
