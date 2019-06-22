/*
 * This code is part of the ThornSec project.
 * 
 * To learn more, please head to its GitHub rep: @privacyint
 * 
 * Pull requests encouraged.
 */
package core.data.machine.configuration;

import java.io.File;
import java.io.IOException;

import javax.json.JsonObject;
import javax.json.stream.JsonParsingException;

import core.data.AData;

import core.exception.data.ADataException;
import core.exception.data.machine.InvalidDiskSizeException;

/**
 * Represents some form of Disk attached to a Service
 */
public class DiskData extends AData {

	/**
	 * You're either a Disk or you're a DVD.
	 */
	public enum Medium {
		
		DISK("disk"),
		DVD("dvd");
		
		private String medium;
		
		Medium(String medium) {
			this.medium = medium;
		}
		
		public String getMedium() {
			return this.medium;
		}
	}

	/**
	 * As far as we're concerned, there are only three file formats.
	 */
	public enum Format {
		VDI("vdi"),
		VMDK("vmdk"),
		VHD("vhd");
		
		private String format;
		
		Format(String format) {
			this.format = format;
		}
		
		public String getFormat() {
			return this.format;
		}
	}
	
	private Medium medium;
	private Format format;
	private File filename;
	private Integer size;
	private File diffparent;
	private String comment;
	
	/**
	 * Creates a new DiskData full of null values.
	 * 
	 * Please call read() if instantiating this way!
	 */
	public DiskData()
	throws InvalidDiskSizeException {
		this(null, null, null, null, null, null);
	}
	
	/**
	 * @param medium disk|dvd
	 * @param format vdi|vmdk|vhd
	 * @param filename
	 * @param size disk size in megabytes
	 * @param diffparent
	 * @param comment
	 * @throws InvalidDiskSizeException
	 */
	public DiskData(Medium medium, Format format, File filename, Integer size, File diffparent, String comment)
	throws InvalidDiskSizeException {
		super("disk");
		
		this.medium     = medium;
		this.format     = format;
		this.filename   = filename;
		
		if (size <= 0) {
			throw new InvalidDiskSizeException();
		}
		else {
			this.size = size;
		}
		
		this.diffparent = diffparent;
		this.comment    = comment;
	}
	
	@Override
	public void read(JsonObject data)
	throws ADataException, JsonParsingException, IOException {
		this.medium     = Medium.valueOf(data.getString("medium", null));
		this.format     = Format.valueOf(data.getString("format", null));
		this.filename   = new File(data.getString("filename", null));
		this.diffparent = new File(data.getString("filename", null));
		this.comment    = data.getString("comment", null);
	}
	
	public Medium getMedium() {
		return this.medium;
	}
	
	public Format getFormat() {
		return this.format;
	}

	public File getFilename() {
		return this.filename;
	}

	public Integer getSize() {
		return this.size;
	}

	public File getDiffparent() {
		return this.diffparent;
	}

	public String getComment() {
		return this.comment;
	}
	
}
