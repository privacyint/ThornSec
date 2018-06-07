package core.model;

import java.util.Vector;

import core.iface.IUnit;
import core.unit.SimpleUnit;

public class UserModel extends AModel {

	private Vector<String> usernames;

	public UserModel(String label) {
		super(label);
	}

	public void init(NetworkModel model) {
		this.usernames = new Vector<String>();

		for (String admin : model.getData().getAdmins(getLabel())) {
			usernames.addElement(admin);
		}
		
		usernames.addElement("root");
		usernames.addElement("daemon");
		usernames.addElement("bin");
		usernames.addElement("sys");
		usernames.addElement("sync");
		usernames.addElement("games");
		usernames.addElement("man");
		usernames.addElement("lp");
		usernames.addElement("mail");
		usernames.addElement("news");
		usernames.addElement("uucp");
		usernames.addElement("proxy");
		usernames.addElement("www-data");
		usernames.addElement("backup");
		usernames.addElement("list");
		usernames.addElement("irc");
		usernames.addElement("gnats");
		usernames.addElement("nobody");
		usernames.addElement("systemd-timesync");
		usernames.addElement("systemd-network");
		usernames.addElement("systemd-resolve");
		usernames.addElement("systemd-bus-proxy");
		usernames.addElement("_apt");
		usernames.addElement("messagebus");
		usernames.addElement("sshd");
		usernames.addElement("statd");
//		usernames.addElement("");
//		usernames.addElement("");
//		usernames.addElement("");
//		usernames.addElement("");
	}

	public Vector<IUnit> getUnits() {
		String grepString = "awk -F':' '{ print $1 }' /etc/passwd";
		Vector<IUnit> units = new Vector<IUnit>();
				
		for (int i = 0; i < usernames.size(); ++i) {
			grepString += " | egrep -v \"^" + usernames.elementAt(i) + "$\"";
		}

		grepString += " | tee /dev/stderr | grep -v 'tee /dev/stderr'"; //We want to be able to see what users are there if the audit fails!
				
		units.addElement(new SimpleUnit("no_unexpected_users", "proceed",
				"",
				grepString, "", "pass",
				"There are unexpected user accounts on this machine.  This could be a sign that the machine is compromised, or it could be "
				+ "entirely innocent.  Please check the usernames carefully to see if there's any cause for concern."));
		
		return units;
	}

	public void addUsername(String username) {
		this.usernames.addElement(username);
	}

}
