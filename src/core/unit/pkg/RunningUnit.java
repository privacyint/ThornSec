package core.unit.pkg;

import core.unit.SimpleUnit;

public class RunningUnit extends SimpleUnit {

	public RunningUnit(String name, String pkg, String grep) {
		super(name + "_running", name + "_installed", "sudo service " + pkg + " restart;",
				//"ps aux | grep -v grep | grep " + grep + ";", "", "fail"); This doesn't work properly under all circumstances
				"sudo systemctl status " + pkg + " | grep -v grep | grep Active: | awk '{print $2 $3}'", "active(running)", "pass",
				"I can't get " + pkg + " running.  This could be due to a misconfiguration, or a dependency on something yet to be configured.  Try restarting the service if things aren't working as expected.");
	}

}
