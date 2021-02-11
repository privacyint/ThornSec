package core.unit.fs;

import core.unit.SimpleUnit;

public class CrontabUnit extends SimpleUnit {

	/**
	 * Unit test for setting up a crontab, with custom fail message.
	 * Follows the crontab style for intervals
	 *
	 * @param name         Name of the unit test (with _crontab appended)
	 * @param precondition Precondition unit test
	 * @param quiet        Redirect errors on the cron job to /dev/null
	 * @param user         The user to run as
	 * @param command      The command
	 * @param dayOfWeek    The day of the week (0-6/*)
	 * @param month        The month (1-12/*)
	 * @param dayOfMonth   The day of the month (1-31/*)
	 * @param hour         The hour (0-23/*)
	 * @param min          The min (0-59/*)
	 * @param failMessage  The fail message
	 */
	public CrontabUnit(String name, String precondition, Boolean quiet, String user, String command, String dayOfWeek, String month, String dayOfMonth, String hour, String min, String failMessage) {
		super(name + "_crontab", precondition,
				"( sudo crontab -u " + user + " -l | grep -v -F \"" + command + "\" ;"
					+ "echo \\\"" + getCrontabLine(min, hour, dayOfMonth, month, dayOfWeek, command, quiet) + "\\\" )"
				+ " | sudo crontab -u " + user + " - ;",
				"sudo crontab -u " + user + " -l | grep -F \"" + command + "\"", getCrontabLine(min, hour, dayOfMonth, month, dayOfWeek, command, quiet), "pass",
				failMessage);
	}
	
	/**
	 * Unit test for setting up a crontab, with default fail message
	 * @param name         Name of the unit test (with _crontab appended)
	 * @param precondition Precondition unit test
	 * @param quiet        Redirect errors on the cron job to /dev/null
	 * @param user         The user to run as
	 * @param command      The command
	 * @param dayOfWeek    The day of the week (0-6/*)
	 * @param month        The month (1-12/*)
	 * @param dayOfMonth   The day of the month (1-31/*)
	 * @param hour         The hour (0-23/*)
	 * @param min          The min (0-59/*)
	 */
	public CrontabUnit(String name, String precondition, Boolean quiet, String user, String command, String dayOfWeek, String month, String dayOfMonth, String hour, String min) {
		this(name, precondition, quiet, user, command, dayOfWeek, month, dayOfMonth, hour, min, "could not add " + command + " to " + user +"'s crontab.");
	}

	private static String getCrontabLine(String min, String hour, String dayOfMonth, String month, String dayOfWeek, String command, Boolean quiet) {
		return min + " " + hour + " " + dayOfMonth + " " + month + " " + dayOfWeek + " " + command + (quiet ? " 2>/dev/null" : "");
	}
}
