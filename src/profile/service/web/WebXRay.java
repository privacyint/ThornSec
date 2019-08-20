/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.service.web;

import java.util.ArrayList;
import java.util.Collection;

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

	@Override
	public Collection<IUnit> getInstalled() throws InvalidServerException {
		final Collection<IUnit> units = new ArrayList<>();

		final Boolean is64bit = getNetworkModel().getData().getDebianIsoUrl(getLabel()).contains("amd64");
		final String url = "https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb";

		if (!is64bit) {
			JOptionPane.showMessageDialog(null, "WebXRay can only be configured on a 64 bit system. Sorry!");
		}

		units.add(new FileDownloadUnit("chrome", "proceed", url, "/root/chrome.deb"));

		units.add(new SimpleUnit("chrome_installed", "chrome_downloaded",
				"export DEBIAN_FRONTEND=noninteractive;"
						+ "sudo -E dpkg -i --assume-yes /root/chrome.deb || sudo apt -f --assume-yes install",
				"google-chrome --version 2>&1", "bash: google-chrome: command not found", "fail"));

		// "wget
		// https://chromedriver.storage.googleapis.com/index.html?path=$(https://chromedriver.storage.googleapis.com/LATEST_RELEASE_\\$(google-chrome
		// --version | awk '{ print $3 }'))")
		// ;

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws InvalidServerModelException {
		final Collection<IUnit> units = new ArrayList<>();

		getNetworkModel().getServerModel(getLabel()).addEgress("*");

		return units;
	}
}
