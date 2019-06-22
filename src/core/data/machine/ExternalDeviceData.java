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
 * Represents an external-only device on our network.
 * 
 * This is a device which is allowed wider connection to the
 * Internet, but shouldn't be allowed to see any of our internal infra
 */
public class ExternalDeviceData extends ADeviceData {

	public ExternalDeviceData(String label) {
		super(label);
	}

	@Override
	public void read(JsonObject data)
	throws ADataException, JsonParsingException, IOException, URISyntaxException {
		super.read(data);
	}
}
