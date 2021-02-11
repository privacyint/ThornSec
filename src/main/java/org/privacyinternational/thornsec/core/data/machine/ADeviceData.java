/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.core.data.machine;

import java.util.Optional;
import javax.json.JsonArray;
import javax.json.JsonObject;
import org.privacyinternational.thornsec.core.data.machine.configuration.NetworkInterfaceData;
import org.privacyinternational.thornsec.core.exception.data.ADataException;
import org.privacyinternational.thornsec.core.exception.data.machine.configuration.InvalidNetworkInterfaceException;
import inet.ipaddr.MACAddressString;

/**
 * Abstract class for something representing "Device Data" on our network. This
 * is the parent class for all devices on our network. These are things like
 * users, or printers, or similar.
 */
public abstract class ADeviceData extends AMachineData {
	private Boolean managed;

	protected ADeviceData(String label) {
		super(label);

		this.managed = null;

		this.putType(MachineType.DEVICE);
	}

	@Override
	public ADeviceData read(JsonObject data) throws ADataException {
		super.read(data);

		readIsManaged(data);
		readNICs(data);

		return this;
	}

	private final void readIsManaged(JsonObject data) {
		if (!data.containsKey("managed")) {
			return;
		}

		this.managed = data.getBoolean("managed");
	}

	private final void readNICs(JsonObject data) throws InvalidNetworkInterfaceException {
		if (!data.containsKey("macs")) {
			return;
		}

		final JsonArray macs = data.getJsonArray("macs");
		for (int i = 0; i < macs.size(); ++i) {
			final NetworkInterfaceData iface = new NetworkInterfaceData(getLabel());
			iface.setIface(getLabel() + i);
			iface.setMAC(new MACAddressString(macs.getString(i)).getAddress());
			putNetworkInterface(iface);
		}
	}

	public final Optional<Boolean> isManaged() {
		return Optional.ofNullable(this.managed);
	}
}
