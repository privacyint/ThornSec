package org.privacyinternational.thornsec.profile.guest;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import org.privacyinternational.thornsec.core.exception.AThornSecException;
import org.privacyinternational.thornsec.core.exception.data.InvalidPortException;
import org.privacyinternational.thornsec.core.exception.data.machine.InvalidServerException;
import org.privacyinternational.thornsec.core.exception.runtime.InvalidMachineModelException;
import org.privacyinternational.thornsec.core.iface.IUnit;
import org.privacyinternational.thornsec.core.model.machine.ServerModel;
import inet.ipaddr.HostName;
import org.privacyinternational.thornsec.profile.machine.configuration.AptSources;

public class Alpine extends AOS {
	private final AptSources aptSources;
	private static String RELEASES_FILE = "https://uk.alpinelinux.org/alpine/latest-stable/releases/x86_64/latest-releases.yaml";

	public Alpine(ServerModel me) throws AThornSecException {
		super(me);

		this.aptSources = new AptSources(me);
	}

	@Override
	public Collection<IUnit> buildIso() throws InvalidServerException, InvalidMachineModelException {
		String url = getServerModel().getIsoUrl().orElseGet(() -> getIsoURLFromLatest());
		String checksum = getServerModel().getIsoSHA512().orElseGet(() -> getIsoSHA512FromLatest());

		return super.getISODownloadUnits(url, checksum);
	}

	private String getLatestIsoDetails() {
		try {
			InputStream alpineLatest = new URL(RELEASES_FILE).openStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//Yaml.createYamlInput(alpineLatest).readYamlStream().anyMatch((release) -> release.
		return null;
	}

	@Override
	protected String getIsoURLFromLatest() {
		getLatestIsoDetails();
		String url = "";

		return url;
	}

	@Override
	protected String getIsoSHA512FromLatest() {
		getLatestIsoDetails();
		String checksum = "";
		return checksum;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws InvalidPortException {
		getServerModel().addEgress(new HostName("alpine.global.ssl.fastly.net:443"));
		return new ArrayList<>();
	}

	@Override
	public String getPackageMirror() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPackageDirectory() {
		// TODO Auto-generated method stub
		return null;
	}
}
