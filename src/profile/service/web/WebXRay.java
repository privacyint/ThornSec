package profile.service.web;

import java.util.HashSet;
import java.util.Set;

import javax.swing.JOptionPane;

import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.network.NetworkModel;

import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.FileDownloadUnit;

public class WebXRay extends AStructuredProfile {
	
	public WebXRay(String label, NetworkModel networkModel) {
		super(label, networkModel);
	}

	public Set<IUnit> getInstalled() 
	throws InvalidServerException {
		Set<IUnit> units = new HashSet<IUnit>();

		Boolean is64bit = networkModel.getData().getDebianIsoUrl(getLabel()).contains("amd64") ? true : false;
		String url = "https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb";
		
		if (!is64bit) {
			JOptionPane.showMessageDialog(null, "WebXRay can only be configured on a 64 bit system. Sorry!");
		}
		
		units.add(new FileDownloadUnit("chrome", "proceed", url, "/root/chrome.deb"));
		
		units.add(new SimpleUnit("chrome_installed", "chrome_downloaded",
				"export DEBIAN_FRONTEND=noninteractive;"
				+ "sudo -E dpkg -i --assume-yes /root/chrome.deb || sudo apt -f --assume-yes install",
				"google-chrome --version 2>&1", "bash: google-chrome: command not found", "fail"));
		
		//"wget https://chromedriver.storage.googleapis.com/index.html?path=$(https://chromedriver.storage.googleapis.com/LATEST_RELEASE_\\$(google-chrome --version | awk '{ print $3 }'))")
		//;
		
		return units;
	}
	
	public Set<IUnit> getPersistentFirewall()
	throws InvalidServerModelException {
		Set<IUnit> units = new HashSet<IUnit>();

		networkModel.getServerModel(getLabel()).addEgress("*");
		
		return units;
	}
}
