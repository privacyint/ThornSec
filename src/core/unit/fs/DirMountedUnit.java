package core.unit.fs;

import core.unit.SimpleUnit;

public class DirMountedUnit extends SimpleUnit {

	/**
	 * Unit test for checking whether a directory has been mounted, with a default fail message
	 * 
	 * @param name         Name of the unit test (with _mounted appended)
	 * @param precondition Precondition unit test
	 * @param dir          Directory to check/mount
	 */
	public DirMountedUnit(String name, String precondition, String dir) {
		super(name + "_mounted", precondition, "sudo mount " + dir, "mount | grep '" + dir + "';", "", "fail", "Terribly sorry, I was unable to mount " + dir);
	}
	
	/**
	 * Unit test for checking whether a directory has been mounted, with a custom fail message
	 * 
	 * @param name         Name of the unit test (with _mounted appended)
	 * @param precondition Precondition unit teste
	 * @param dir          Directory to check/mount
	 * @param message      Fail message
	 */
	public DirMountedUnit(String name, String precondition, String dir, String message) {
		super(name + "_mounted", precondition, "sudo mount " + dir, "mount | grep '" + dir + "';", "", "fail", message);
	}
	
}
