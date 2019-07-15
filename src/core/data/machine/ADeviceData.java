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
 * Abstract class for something representing "Device Data" on our network.
 * This is the parent class for all devices on our network. These are things like
 * users, or printers, or similar. 
 */
public abstract class ADeviceData extends AMachineData {
	private Boolean managed;

	protected ADeviceData(String label) {
		super(label);

		this.managed = null;
	}
	
	@Override
	protected void read(JsonObject data)
	throws ADataException, JsonParsingException, IOException, URISyntaxException {
		super.read(data);

		this.managed = getBooleanProperty("managed");
	}

	public final Boolean isManaged() {
		return this.managed;
	}
}
