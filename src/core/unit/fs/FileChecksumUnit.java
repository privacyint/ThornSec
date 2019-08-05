/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.unit.fs;

import core.unit.SimpleUnit;

public class FileChecksumUnit extends SimpleUnit {
	public enum Checksum {
		SHA512("sha512sum"), SHA256("sha256sum");

		private String checksum;

		Checksum(String checksum) {
			this.checksum = checksum;
		}

		public String getChecksum() {
			return this.checksum;
		}
	}

	/**
	 * Unit test for checking a file's checksum, with custom fail message
	 *
	 * @param name         Name of the unit test (with _checksum appended)
	 * @param precondition Precondition unit test
	 * @param checksum     The type of checksum
	 * @param file         File to check
	 * @param digest       Checksum to test against
	 * @param message      Custom fail message
	 */
	public FileChecksumUnit(String name, String precondition, Checksum checksum, String file, String digest,
			String message) {
		super(name + "_checksum", precondition, "",
				"sudo " + checksum.getChecksum() + " " + file + " | awk '{print $1}'", digest, "pass", message);
	}

	/**
	 * Unit test for checking a file's checksum, with default fail message
	 *
	 * @param name         Name of the unit test (with _checksum appended)
	 * @param precondition Precondition unit test
	 * @param checksum     The type of checksum
	 * @param file         File to check
	 * @param digest       Checksum to test against
	 */
	public FileChecksumUnit(String name, String precondition, Checksum checksum, String file, String digest) {
		this(name, precondition, checksum, file, digest, name
				+ "'s checksum doesn't match.  This could indicate a failed download, MITM attack, or a newer version than our code supports.");
	}
}
