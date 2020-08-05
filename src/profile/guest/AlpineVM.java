package profile.guest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import com.amihaiemil.eoyaml.YamlNode;
import core.exception.AThornSecException;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.InvalidMachineModelException;
import core.iface.IUnit;
import core.model.machine.ServiceModel;
import profile.machine.configuration.AptSources;

public class AlpineVM extends AGuestProfile {

	private final AptSources aptSources;
	private static String RELEASES_FILE = "https://uk.alpinelinux.org/alpine/latest-stable/releases/x86_64/latest-releases.yaml";
	
	public AlpineVM(ServiceModel me) throws AThornSecException {
		super(me);
		
		this.aptSources = new AptSources(me);
	}

	@Override
	public Collection<IUnit> buildIso() throws InvalidServerException, InvalidMachineModelException {
		String url = getServerModel().getIsoUrl().orElseGet(() -> getIsoURLFromLatest());
		String checksum = getServerModel().getIsoSHA512().orElseGet(() -> getIsoSHA512FromLatest());

		return super.getISODownloadUnits(url, checksum);
	}

	private YamlNode getLatestIsoDetails() {
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

	
}
