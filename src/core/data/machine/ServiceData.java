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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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

	private Map<String, DiskData> disks;

	private Integer backupFrequency;

	public ServiceData(String label) {
		super(label);

		this.hypervisor = null;

		this.debianISOURL = null;
		this.debianISOSHA512 = null;

		this.disks = null;
	}

	@Override
	public void read(JsonObject data) throws ADataException, JsonParsingException, IOException, URISyntaxException {
		super.read(data);

		if (data.containsKey("disks")) {
			this.disks = new LinkedHashMap<>();
			final JsonObject disks = data.getJsonObject("disks");

			for (final String diskLabel : disks.keySet()) {
				final DiskData diskData = new DiskData(diskLabel);
				diskData.read(disks.getJsonObject(diskLabel));

				this.disks.put(diskLabel, diskData);
			}
		}

		this.debianISOURL = data.getString("debian_iso_url", null);
		this.debianISOSHA512 = data.getString("debian_iso_sha512", null);

		// Force it to recognise us as a service...
		if ((getTypes() == null) || !getTypes().contains(MachineType.SERVICE)) {
			final Set<MachineType> types = new LinkedHashSet<>();
			types.add(MachineType.SERVICE);
			setTypes(types);
		}
	}

	/**
	 * Get a given DiskData from its label
	 * 
	 * @param label the DiskData label
	 * @return the DiskData
	 */
	public final DiskData getDisk(String label) {
		try {
			return getDisks().getOrDefault(label, null);
		}
		catch (NullPointerException e) {
			return null;
		}
	}
	
	/**
	 * Get all of the disks associated with this Service
	 * 
	 * @return a Map<label, DiskData> of all disks
	 */
	public final Map<String, DiskData> getDisks() {
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
	
	public final void setHypervisor(String hypervisor) {
		this.hypervisor = hypervisor;
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

	/**
	 * @return The boot disk size, or null 
	 */
	public Integer getBootDiskSize() {
		try {
			return getDisk("boot").getSize();
		}
		catch (NullPointerException e) {
			return null;
		}
	}

	/**
	 * @return The data disk size, or null 
	 */
	public Integer getDataDiskSize() {
		try {
			return getDisk("data").getSize();
		}
		catch (NullPointerException e) {
			return null;
		}
	}

}
