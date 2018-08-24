package core.unit.fs;

import core.unit.SimpleUnit;

public class FileChecksumUnit extends SimpleUnit {

	/**
	 * Unit test for checking a file's SHA512 checksum, with custom fail message
	 * @param name         Name of the unit test (with _checksum appended)
	 * @param precondition Precondition unit test
	 * @param file         File to check
	 * @param sha512       Checksum to test against
	 * @param message      Custom fail message
	 */
	public FileChecksumUnit(String name, String precondition, String file, String sha512, String message) {
		super(name + "_checksum", precondition,
				"",
				"sudo sha512sum " + file + " | awk '{print $1}'", sha512, "pass",
				message);
	}
	
	/**
	 * Unit test for checking a file's SHA512 checksum, with default fail message
	 * @param name         Name of the unit test (with _checksum appended)
	 * @param precondition Precondition unit test
	 * @param file         File to check
	 * @param sha512       Checksum to test against
	 */
	public FileChecksumUnit(String name, String precondition, String file, String sha512) {
		this(name, precondition, file, sha512, name + "'s checksum doesn't match.  This could indicate a failed download, MITM attack, or a newer version than our code supports.");
	}

}
