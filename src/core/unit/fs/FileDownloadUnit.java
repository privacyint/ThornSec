/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.unit.fs;

import core.unit.SimpleUnit;

public class FileDownloadUnit extends SimpleUnit {

	/**
	 * Unit test for downloading a file, with custom fail message
	 *
	 * @param name         Name of the unit test (with _downloaded appended)
	 * @param precondition Precondition unit test
	 * @param url          URL of file to download
	 * @param path         Output path
	 * @param message      Custom fail message
	 */
	public FileDownloadUnit(String name, String precondition, String url, String path, String message) {
		super(name + "_downloaded", precondition, "sudo wget '" + url + "' -T 10 -O " + path + " || sudo rm " + path,
				"sudo [ -f " + path + " ] && echo pass || echo fail", "pass", "pass", message);
	}

	/**
	 * Unit test for downloading a file, with default fail message
	 *
	 * @param name         Name of the unit test (with _downloaded appended)
	 * @param precondition Precondition unit test
	 * @param url          URL of file to download
	 * @param path         Output path
	 */
	public FileDownloadUnit(String name, String precondition, String url, String path) {
		this(name, precondition, url, path, "Couldn't download " + url + ".  Sorry about that!");
	}

}
