/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.data.machine;


import java.util.LinkedHashMap;
import java.util.Map;

import javax.json.JsonObject;

import core.data.machine.configuration.DiskData;
import core.exception.data.ADataException;
import core.exception.data.InvalidPropertyException;
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

	private Integer cpuExecutionCap;

	public ServiceData(String label) {
		super(label);

		this.putType(MachineType.SERVICE);

		this.hypervisor = null;

		this.debianISOURL = null;
		this.debianISOSHA512 = null;

		this.cpuExecutionCap = null;
		
		this.disks = null;
	}

	@Override
	public void read(JsonObject data) throws ADataException {
		super.read(data);

		if (data.containsKey("disks")) {
			final JsonObject disks = data.getJsonObject("disks");

			for (final String disk : disks.keySet()) {
				final DiskData diskData = new DiskData(disk);
				diskData.read(disks.getJsonObject(disk));
				
				this.addDisk(diskData);
			}
		}

		this.debianISOURL = data.getString("debian_iso_url", null);
		this.debianISOSHA512 = data.getString("debian_iso_sha512", null);

		if (data.containsKey("cpu_execution_cap")) {
			this.setCPUExecutionCap(data.getInt("cpu_execution_cap"));
		}
		
	}

	private void addDisk(DiskData diskData) {
		if (getDisks() == null) {
			this.disks = new LinkedHashMap<>();
		}
		
		this.disks.put(diskData.getLabel(), diskData);
	}

	public void setCPUExecutionCap(Integer capPct) throws InvalidPropertyException {
		if (capPct < 1 || capPct > 100) {
			throw new InvalidPropertyException("CPU Execution Cap must be an integer between 1-100");
		}
		
		this.cpuExecutionCap = capPct;
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

	/**
	 * @return The CPU execution cap as an Integer {1-100}
	 */
	public Integer getCPUExecutionCap() {
		return this.cpuExecutionCap;
	}

}
