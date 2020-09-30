/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.data.machine;


import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.json.JsonObject;
import core.data.machine.configuration.DiskData;
import core.exception.data.ADataException;
import core.exception.data.InvalidPropertyException;
import core.exception.data.machine.InvalidDiskSizeException;
import profile.type.Hypervisor;

/**
 * This Class represents the data of a Service For our purposes, this is
 * something which runs on a {@link Hypervisor} - i.e. a Virtual Machine
 */
public class ServiceData extends ServerData {
	// These are the only types of machine I'll recognise until I'm told otherwise...
	public enum GuestOS {
		DEBIAN_64("amd64"),
		DEBIAN_32("i386"),
		ALPINE_64("http://dl-cdn.alpinelinux.org/alpine/"),
		ALPINE_32("x86");

		public static Set<GuestOS> alpine = EnumSet.of(ALPINE_32, ALPINE_64);
		public static Set<GuestOS> debian = EnumSet.of(DEBIAN_32, DEBIAN_64);

		private String guestOS;

		GuestOS(String guestOS) {
			this.guestOS = guestOS;
		}

		@Override
		public String toString() {
			return this.guestOS;
		}
	}
	
	private HypervisorData hypervisor;

	private String iso;
	private String isoSHA512;

	private Map<String, DiskData> disks;

	private Integer backupFrequency;
	private Integer cpuExecutionCap;

	private GuestOS guestOS;
	
	public ServiceData(String label) {
		super(label);

		putType(MachineType.SERVICE);

		this.hypervisor = null;
		this.guestOS = null;

		this.iso = null;
		this.isoSHA512 = null;

		this.backupFrequency = null;
		
		this.cpuExecutionCap = null;
		
		this.disks = null;
	}

	@Override
	public void read(JsonObject data) throws ADataException {
		super.read(data);

		readOS();
		readDisks();
		readBackupFrequency();
		readCPUExecutionCap();
		readISO();
	}

	private void readISO() {
		this.iso = getData().getString("iso_url", null);
		this.isoSHA512 = getData().getString("iso_sha512", null);
	}

	private void readOS() {
		if (!getData().containsKey("os")) {
			return;
		}

		setOS(getData().getString("os"));		
	}

	private void setOS(String os) {
		this.guestOS = GuestOS.valueOf(os);
	}

	private void readBackupFrequency() throws InvalidPropertyException {
		if (!getData().containsKey("backup_frequency")) {
			return;
		}
		
		setBackupFrequency(getData().getInt("backup_frequency"));
	}

	/**
	 * Set the frequency with which to back up this machine, in whole hours
	 * 
	 * @param frequency in hours
	 * @throws InvalidPropertyException if <1
	 */
	private void setBackupFrequency(Integer frequency) throws InvalidPropertyException {
		if (frequency < 1) {
			throw new InvalidPropertyException("You must set backups a minimum of 1 hour apart");
		}
		
		this.backupFrequency = frequency;
	}

	/**
	 * Read in the CPU execution cap absolute percentage for this service
	 * 
	 * @throws InvalidPropertyException if the percentage is invalid
	 */
	private void readCPUExecutionCap() throws InvalidPropertyException {
		if (!getData().containsKey("cpu_execution_cap")) {
			return;
		}
		
		setCPUExecutionCap(getData().getInt("cpu_execution_cap"));		
	}

	/**
	 * Read in any disk information
	 * 
	 * @throws InvalidDiskSizeException if a disk size is invalid; this could
	 * 		potentially mean it's too small, or is NaN. 
	 */
	private void readDisks() throws InvalidDiskSizeException {
		if (!getData().containsKey("disks")) {
			return;
		}
		final JsonObject disks = getData().getJsonObject("disks");

		for (final String disk : disks.keySet()) {
			final DiskData diskData = new DiskData(disk);
			diskData.read(disks.getJsonObject(disk));
			
			this.addDisk(diskData);
		}
	}

	/**
	 * Add a disk derived from our JSON to this Service
	 * 
	 * @param diskData the disk to add
	 */
	private void addDisk(DiskData diskData) {
		if (this.disks == null) {
			this.disks = new LinkedHashMap<>();
		}
		
		this.disks.put(diskData.getLabel(), diskData);
	}

	/**
	 * Indicate we want to cap available CPU usage for this machine at a given percentage
	 * 
	 * @param capPct the cap, as an absolute percentage of CPU usage
	 * @throws InvalidPropertyException if the percentage is <1% or >100%
	 */
	protected void setCPUExecutionCap(Integer capPct) throws InvalidPropertyException {
		if (capPct < 1 || capPct > 100) {
			throw new InvalidPropertyException("CPU Execution Cap must be an integer between 1-100");
		}
		
		this.cpuExecutionCap = capPct;
	}

	/**
	 * Retrieve a given DiskData by its label
	 * 
	 * @param label the DiskData label
	 * @return the DiskData, if it's attached to this machine
	 */
	public final Optional<DiskData> getDiskData(String label) {
		return Optional.ofNullable(getDisks().get().get(label));
	}

	/**
	 * Get all of the disks associated with this Service
	 * 
	 * @return a Map<label, DiskData> of all disks
	 */
	public final Optional<Map<String, DiskData>> getDisks() {
		return Optional.ofNullable(this.disks);
	}

	/**
	 * @return the backup frequency, in hours
	 */
	public final Optional<Integer> getBackupFrequency() {
		return Optional.ofNullable(this.backupFrequency);
	}

	/**
	 * @return the {@code label} of this service's HyperVisor
	 */
	public final HypervisorData getHypervisor() {
		//assertNotNull(this.hypervisor);

		return this.hypervisor;
	}

	/**
	 * Set the Hypervisor for this machine - warning, this is unchecked. You're
	 * expected to make sure this machine exists elsewhere.
	 * 
	 * @param hv The label of the hypervisor machine
	 */
	public final void setHypervisor(HypervisorData hv) {
		//assertNotNull(hv);

		this.hypervisor = hv;
	}
	
	/**
	 * @return the URL to use for building this service
	 */
	public final String getIsoUrl() {
		return this.iso;
	}

	/**
	 * @return the expected SHA512SUM of the Debian ISO
	 */
	public final String getIsoSha512() {
		return this.isoSHA512;
	}

	/**
	 * Get a disk's size from its data, if it's set in the JSON, otherwise return
	 * a default value
	 * 
	 * @param diskLabel the disk's label
	 * @param defaultSize the default size to return if the size is not set
	 * @return Either the disk's size as set in the JSON or the defaultSize
	 */
	public Optional<Integer> getDiskSize(String diskLabel) {
		Optional<DiskData> disk = getDiskData(diskLabel);

		if (disk.isPresent()) {
			return disk.get().getSize();
		}

		return null;
	}
		/**
	 * @return The CPU execution cap as an absolute percentage {1-100}
	 */
	public Optional<Integer> getCPUExecutionCap() {
		return Optional.ofNullable(this.cpuExecutionCap);
	}

	public Optional<GuestOS> getOS() {
		return Optional.ofNullable(this.guestOS);
	}

}
