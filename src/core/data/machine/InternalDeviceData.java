/*
 * This code is part of the ThornSec project.
 * 
 * To learn more, please head to its GitHub repo: @privacyint
 * 
 * Pull requests encouraged.
 */
package core.data.machine;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.json.JsonObject;
import javax.json.stream.JsonParsingException;

import core.exception.data.ADataException;

/**
 * Represents an internal-only device on our network.
 * 
 * This is a device which is allowed to *respond* to our Users, but
 * shouldn't be able to talk to the wider Internet.
 * 
 * Why is this important? Ask NASA.
 * https://www.zdnet.com/article/nasa-hacked-because-of-unauthorized-raspberry-pi-connected-to-its-network/
 */
public class InternalDeviceData extends ADeviceData {

	public InternalDeviceData(String label) {
		super(label);
		
		this.putType(MachineType.INTERNAL_ONLY);
	}

	@Override
	public void read(JsonObject data)
	throws ADataException, JsonParsingException, IOException, URISyntaxException {
		super.read(data);
	}
}
