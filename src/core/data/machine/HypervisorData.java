/*
 * This code is part of the ThornSec project.
 * 
 * To learn more, please head to its GitHub repo: @privacyint
 * 
 * Pull requests encouraged.
 */
package core.data.machine;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.json.JsonObject;
import javax.json.stream.JsonParsingException;

import core.exception.data.ADataException;

/**
 * This class represents a Hypervisor.
 * 
 * For our purposes, a Hypervisor is a machine which is running a type-2
 * https://en.wikipedia.org/wiki/Hypervisor
 */
public class HypervisorData extends ServerData {
	private File vmBase;
	
	public HypervisorData(String label) {
		super(label);
	
		this.vmBase = null;
	}

	public void read(JsonObject data)
	throws ADataException, JsonParsingException, IOException, URISyntaxException {
		super.read(data);
		
		this.vmBase = new File(getStringProperty("vmbase"));
	}

	public final File getVmBase() {
		return this.vmBase;
	}
}
