/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.data.machine.configuration;

import java.io.File;
import java.util.Optional;
import javax.json.JsonObject;

import core.StringUtils;
import core.data.AData;
import core.exception.data.InvalidPropertyException;
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

		@Override
		public String toString() {
			return this.format;
		}
	}
	
	public enum DiskType {
		
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
	public void read(JsonObject data) throws InvalidDiskSizeException {
		if (data.containsKey("medium")) {
			setMedium(Medium.valueOf(data.getString("medium").toUpperCase()));
		}
		if (data.containsKey("format")) {
			setFormat(Format.valueOf(data.getString("format").toUpperCase()));
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
			Integer sizeInMB;
			try {
				sizeInMB = StringUtils.stringToMegaBytes(data.getString("size"));
			} catch (InvalidPropertyException e) {
				throw new InvalidDiskSizeException(data.getString("size"));
			}

			setSize(sizeInMB);
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

	public Optional<Medium> getMedium() {
		return Optional.ofNullable(this.medium);
	}

	public Optional<Format> getFormat() {
		return Optional.ofNullable(this.format);
	}

	public Optional<File> getFilename() {
		return Optional.ofNullable(this.filename);
	}

	public Optional<Integer> getSize() {
		return Optional.ofNullable(this.size);
	}

	public Optional<File> getDiffparent() {
		return Optional.ofNullable(this.diffParent);
	}

	public Optional<String> getComment() {
		return Optional.ofNullable(this.comment);
	}

}
