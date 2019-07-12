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

/**
 * This Class represents the data of a Service
 * For our purposes, this is something which runs on a hypervisor - i.e. a Virtual Machine
 */
public class ServiceData extends ServerData {

	private String hypervisor;

	private String debianISOURL;
	private String debianISOSHA512;
	
	private Integer ram;
	private Integer cpus;
	
	private Set<DiskData> disks;
	
	private Integer backupFrequency;

	public ServiceData(String label) {
		super(label);
		
		this.hypervisor = null;

		this.debianISOURL    = null;
		this.debianISOSHA512 = null;
		
		this.ram  = null;
		this.cpus = null;

		this.disks = null;
		
		this.backupFrequency = null;
	}

	@Override
	public void read(JsonObject data)
	throws ADataException, JsonParsingException, IOException, URISyntaxException {
		super.read(data);
		
		JsonArray disks = getPropertyObjectArray("disks");
		if (disks != null) {
			this.disks = new HashSet<DiskData>();

			for (int i = 0; i < disks.size(); ++i) {
				DiskData disk = new DiskData();
				disk.read(disks.getJsonObject(i));
				this.disks.add(disk);
			}

		}
		
		this.hypervisor = getStringProperty("metal");

		this.debianISOURL    = getStringProperty("debianisourl", null);
		this.debianISOSHA512 = getStringProperty("debianisosha512", null);
		
		this.ram  = getIntegerProperty("ram");
		this.cpus = getIntegerProperty("cpus");
		
		this.backupFrequency = getIntegerProperty("backups");
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
	
	public final String getHypervisor() {
		return this.hypervisor;
	}
	
	/**
	 * @return the ram in megabytes
	 */
	public final Integer getRAM() {
		return this.ram;
	}

	public final Integer getCPUs() {
		return this.cpus;
	}

	public final String getDebianIsoUrl() {
		return this.debianISOURL;
	}

	public final String getDebianIsoSha512() {
		return this.debianISOSHA512;
	}
}
