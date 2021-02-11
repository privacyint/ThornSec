/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.core.model.machine.configuration.disks;

import java.io.File;
import java.util.Optional;
import org.apache.commons.io.FilenameUtils;
import org.privacyinternational.thornsec.core.data.machine.configuration.DiskData;
import org.privacyinternational.thornsec.core.data.machine.configuration.DiskData.Format;
import org.privacyinternational.thornsec.core.data.machine.configuration.DiskData.Medium;
import org.privacyinternational.thornsec.core.exception.AThornSecException;
import org.privacyinternational.thornsec.core.exception.data.machine.configuration.disks.InvalidDiskSizeException;
import org.privacyinternational.thornsec.core.model.AModel;
import org.privacyinternational.thornsec.core.model.network.NetworkModel;

public class ADiskModel extends AModel {
	private Medium medium;
	private Format format;
	private File filename;
	private Integer size;
	private File diffParent;
	private String comment;

	public ADiskModel(String label, Medium medium, Format format, File filename, Integer size, File diffParent, String comment, NetworkModel networkModel) throws InvalidDiskSizeException {
		super(new DiskData(label), networkModel);
		this.setLabel(label);
		setMedium(medium);
		setFormat(format);
		setFilename(filename);
		setSize(size);
		setDiffParent(diffParent);
		setComment(comment);
	}
	
	public ADiskModel(DiskData myData, NetworkModel networkModel) {
		super(myData, networkModel);
	}

	/**
	 * @return the medium
	 */
	public Medium getMedium() {
		return medium;
	}

	/**
	 * @param medium the medium to set
	 */
	public void setMedium(Medium medium) {
		this.medium = medium;
	}

	/**
	 * @return the format
	 */
	public Format getFormat() {
		return format;
	}

	/**
	 * @param format the format to set
	 */
	public void setFormat(Format format) {
		this.format = format;
	}

	/**
	 * @return the filename
	 */
	public String getFilename() {
		if (null == this.filename) {
			return null;
		}
	
		return FilenameUtils.normalize(this.filename.toString(), true);
	}

	public String getFilePath() {
		if (null == this.filename) {
			return null;
		}

		return FilenameUtils.normalize(this.filename.getParent().toString(), true);
	}

	/**
	 * @param filename the filename to set
	 */
	public void setFilename(File filename) {
		this.filename = filename;
	}

	/**
	 * @return the size
	 */
	public Integer getSize() {
		return size;
	}

	/**
	 * @param size the size to set
	 * @throws InvalidDiskSizeException 
	 */
	public void setSize(Integer size) throws InvalidDiskSizeException {
		this.size = size;
	}

	/**
	 * @return the diffParent
	 */
	public Optional<File> getDiffParent() {
		return Optional.ofNullable(diffParent);
	}

	/**
	 * @param diffParent the diffParent to set
	 */
	public void setDiffParent(File diffParent) {
		this.diffParent = diffParent;
	}

	/**
	 * @return the comment
	 */
	public Optional<String> getComment() {
		return Optional.ofNullable(comment);
	}

	/**
	 * @param comment the comment to set
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	@Override
	public void init() throws AThornSecException {
		getData().getComment().ifPresent((comment) -> {
			this.setComment(comment);
		});
		
		getData().getDiffparent().ifPresent((diffParent) -> {
			this.setDiffParent(diffParent);
		});
		
		getData().getFilename().ifPresent((filename) -> {
			this.setFilename(filename);
		});
		
		getData().getFormat().ifPresent((format) -> {
			this.setFormat(format);
		});
		
		getData().getMedium().ifPresent((medium) -> {
			this.setMedium(medium);
		});
		
		getData().getSize().ifPresent((size) -> {
			try {
				this.setSize(size);
			} catch (InvalidDiskSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}
	
	@Override
	public DiskData getData() {
		return (DiskData) getData();
	}
}
