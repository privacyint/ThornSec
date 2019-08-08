/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
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

		DISK("disk"), DVD("dvd");

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
		VDI("vdi"), VMDK("vmdk"), VHD("vhd");

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
	private File diffParent;
	private String comment;

	public DiskData(String label) throws InvalidDiskSizeException {
		super(label);

		this.medium = null;
		this.format = null;
		this.filename = null;
		this.size = null;
		this.diffParent = null;
		this.comment = null;
	}

	@Override
	public void read(JsonObject data) throws ADataException, JsonParsingException, IOException {
		if (data.containsKey("medium")) {
			setMedium(Medium.valueOf(data.getString("medium")));
		}
		if (data.containsKey("format")) {
			setFormat(Format.valueOf(data.getString("format")));
		}
		if (data.containsKey("filename")) {
			setFilename(new File(data.getString("filename")));
		}
		if (data.containsKey("diffparent")) {
			setdiffParent(new File(data.getString("diffparent")));
		}
		if (data.containsKey("comment")) {
			setComment(data.getString("comment"));
		}
		if (data.containsKey("size")) {
			setSize(data.getInt("size"));
		}
	}

	private void setSize(int size) throws InvalidDiskSizeException {
		if (size < 512) {
			throw new InvalidDiskSizeException(size);
		}

		this.size = size;
	}

	private void setComment(String comment) {
		this.comment = comment;
	}

	private void setFilename(File filename) {
		this.filename = filename;
	}

	private void setdiffParent(File diffParent) {
		this.diffParent = diffParent;
	}

	private void setFormat(Format format) {
		this.format = format;
	}

	private void setMedium(Medium medium) {
		this.medium = medium;
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
		return this.diffParent;
	}

	public String getComment() {
		return this.comment;
	}

}
