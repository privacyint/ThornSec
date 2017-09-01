package core.unit.fs;

import core.unit.SimpleUnit;

public class FileDownloadUnit extends SimpleUnit {

	/**
	 * Unit test for downloading a file, with default fail message
	 * @param name         Name of the unit test (with _downloaded appended)
	 * @param precondition Precondition unit test
	 * @param url          URL of file to download
	 * @param path         Output path
	 */
	public FileDownloadUnit(String name, String precondition, String url, String path) {
		super(name + "_downloaded", precondition,
				"sudo wget " + url + " -O " + path,
				"sudo [ -f " + path + " ] && echo 'pass' || echo 'fail'", "pass", "pass",
				"Couldn't download " + url + ".  Sorry about that!");
	}

	/**
	 * Unit test for downloading a file, with custom fail message
	 * @param name         Name of the unit test (with _downloaded appended)
	 * @param precondition Precondition unit test
	 * @param url          URL of file to download
	 * @param path         Output path
	 * @param message      Custom fail message
	 */
	public FileDownloadUnit(String name, String precondition, String url, String path, String message) {
		super(name + "_downloaded", precondition,
				"sudo wget " + url + " -O " + path,
				"sudo [ -f " + path + " ] && echo 'pass' || echo 'fail'", "pass", "pass",
				message);
	}

}
