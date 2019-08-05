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
import java.util.HashSet;
import java.util.Set;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.stream.JsonParsingException;

import core.data.machine.configuration.DiskData;
import core.exception.data.ADataException;
import profile.type.HyperVisor;

/**
 * This Class represents the data of a Service For our purposes, this is
 * something which runs on a {@link HyperVisor} - i.e. a Virtual Machine
 */
public class ServiceData extends ServerData {

	private String hypervisor;

	private String debianISOURL;
	private String debianISOSHA512;

	private Set<DiskData> disks;

	private Integer backupFrequency;

	public ServiceData(String label) {
		super(label);

		this.hypervisor = null;

		this.debianISOURL = null;
		this.debianISOSHA512 = null;

		this.disks = null;

		this.backupFrequency = null;
	}

	@Override
	public void read(JsonObject data) throws ADataException, JsonParsingException, IOException, URISyntaxException {
		super.read(data);

		if (data.containsKey("disks")) {
			final JsonArray disks = data.getJsonArray("disks");

			this.disks = new HashSet<>();

			for (int i = 0; i < disks.size(); ++i) {
				final DiskData disk = new DiskData(getLabel());
				disk.read(disks.getJsonObject(i));
				this.disks.add(disk);
			}
		}

		this.hypervisor = data.getString("hypervisor", null);

		this.debianISOURL = data.getString("debianisourl", null);
		this.debianISOSHA512 = data.getString("debianisosha512", null);

		if (data.containsKey("backupfrequency")) {
			this.backupFrequency = data.getInt("backupfrequency");
		}
	}

	public final Set<DiskData> getDisks() {
		return this.disks;
	}

	/**
	 * @return the backup frequency, in hours
	 */
	public final Integer getBackupFrequency() {
		return this.backupFrequency;
	}

	/**
	 * @return the {@code label} of this service's HyperVisor
	 */
	public final String getHypervisor() {
		return this.hypervisor;
	}

	/**
	 * @return the URL to use for building this service
	 */
	public final String getDebianIsoUrl() {
		return this.debianISOURL;
	}

	/**
	 * @return the expected SHA512SUM of the Debian ISO
	 */
	public final String getDebianIsoSha512() {
		return this.debianISOSHA512;
	}
}
