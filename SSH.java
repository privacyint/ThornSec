package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.DirOwnUnit;
import core.unit.fs.DirPermsUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileOwnUnit;
import core.unit.fs.FilePermsUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;

public class SSH extends AStructuredProfile {

	public SSH() {
		super("sshd");
	}

	protected Vector<IUnit> getInstalled(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		units.addElement(new InstalledUnit("sshd", "openssh-server"));
		return units;
	}

	protected Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		String sshdconf = "";
		
		sshdconf += (model.getServerModel(server).isRouter()) ? "ListenAddress " + model.getData().getIP() + "\n" : "";
		sshdconf += "Port " + model.getData().getSSHPort(server) + "\n";
		sshdconf += "Protocol 2\n";
		sshdconf += "HostKey /etc/ssh/ssh_host_rsa_key\n";
		sshdconf += "HostKey /etc/ssh/ssh_host_ed25519_key\n";
		sshdconf += "UsePrivilegeSeparation yes\n";
		sshdconf += "KeyRegenerationInterval 3600\n";
		sshdconf += "ServerKeyBits 1024\n";
		sshdconf += "MACs hmac-sha2-512-etm@openssh.com,hmac-sha2-256-etm@openssh.com,hmac-ripemd160-etm@openssh.com,umac-128-etm@openssh.com,hmac-sha2-512,hmac-sha2-256,hmac-ripemd160,umac-128@openssh.com\n";
		sshdconf += "Ciphers chacha20-poly1305@openssh.com,aes256-gcm@openssh.com,aes128-gcm@openssh.com,aes256-ctr,aes192-ctr,aes128-ctr\n";
		sshdconf += "KexAlgorithms curve25519-sha256@libssh.org,diffie-hellman-group-exchange-sha256\n";
		sshdconf += "SyslogFacility AUTH\n";
		sshdconf += "LogLevel INFO\n";
		sshdconf += "LoginGraceTime 120\n";
		sshdconf += "PermitRootLogin no\n";
		sshdconf += "StrictModes yes\n";
		sshdconf += "RSAAuthentication yes\n";
		sshdconf += "PubkeyAuthentication yes\n";
		sshdconf += "AuthorizedKeysFile %h/.ssh/authorized_keys\n";
		sshdconf += "IgnoreRhosts yes\n";
		sshdconf += "RhostsRSAAuthentication no\n";
		sshdconf += "HostbasedAuthentication no\n";
		sshdconf += "PermitEmptyPasswords no\n";
		sshdconf += "PasswordAuthentication no\n";
		sshdconf += "ChallengeResponseAuthentication no\n";
		sshdconf += "X11Forwarding yes\n";
		sshdconf += "X11DisplayOffset 10\n";
		sshdconf += "PrintMotd no\n"; //This is handled by PAM anyway
		sshdconf += "PrintLastLog yes\n";
		sshdconf += "TCPKeepAlive yes\n";
		sshdconf += "AcceptEnv LANG LC_*\n";
		sshdconf += "Subsystem sftp /usr/lib/openssh/sftp-server\n";
		sshdconf += "UsePAM yes\n";
		sshdconf += "Banner /etc/ssh/sshd_banner\n";
		sshdconf += "MaxSessions 1";
		units.addElement(model.getServerModel(server).getConfigsModel().addConfigFile("sshd", "proceed", sshdconf, "/etc/ssh/sshd_config"));

		//This banner is taken from https://www.dedicatedukhosting.com/hosting/adding-ssh-welcome-and-warning-messages/
		String banner = "";
		banner += "************************NOTICE***********************\n" + 
				"This system is optimised and configured with security and logging as a\n" + 
				"priority. All user activity is logged and streamed offsite. Individuals\n" + 
				"or groups using this system in excess of their authorisation will have\n" + 
				"all access terminated. Illegal access of this system or attempts to\n" + 
				"limit or restrict access to authorised users (such as DoS attacks) will\n" + 
				"be reported to national and international law enforcement bodies. We\n" + 
				"will prosecute to the fullest extent of the law regardless of the funds\n" + 
				"required. Anyone using this system consents to these terms and the laws\n" + 
				"of the United Kingdom and United States respectively.\n" + 
				"************************NOTICE***********************";
		units.addElement(model.getServerModel(server).getConfigsModel().addConfigFile("sshd_banner", "proceed", banner, "/etc/ssh/banner"));
		
		units.addElement(new DirUnit("motd", "proceed", "/etc/update-motd.d/"));
		
		// Elements of this motd banner are taken from https://nickcharlton.net/posts/debian-ubuntu-dynamic-motd.html
		// (c) 2009-2010 Canonical Ltd
		// (c) 2013 Nick Charlton
		// Released under GPL
		String motd = "";
		motd += "#!/bin/bash\n";
		motd += "echo \\\"This machine is a Thornsec configured machine.\\\"\n";
		motd += "echo \\\"_Logging in to configure this machine manually is highly discouraged!_\\\"\n";
		motd += "echo \\\"Please only continue if you know what you're doing!\\\"\n";
		motd += "echo\n";
		motd += "date=\\`date\\`\n";
		motd += "load=\\`cat /proc/loadavg | awk '{print \\$1}'\\`\n";
		motd += "root_usage=\\`df -h / | awk '/\\// {print \\$(NF-1)}'\\`\n";
		motd += "memory_usage=\\`free -m | awk '/Mem/ { printf(\\\"%3.1f%%\\\", \\$3/\\$2*100) }'\\`\n";
		motd += "swap_usage=\\`free -m | awk '/Swap/ { printf(\\\"%3.1f%%\\\", \\$3/\\$2*100) }'\\`\n";
		motd += "users=\\`users | wc -w\\`\n";
		motd += "echo \\\"System information as of: \\${date}\\\"\n";
		motd += "echo\n";
		motd += "printf \\\"System load:\\t%s\\tMemory usage:\\t%s\\n\\\" \\${load} \\${memory_usage}\n";
		motd += "printf \\\"Usage on /:\\t%s\\tSwap usage:\\t%s\\n\\\" \\${root_usage} \\${swap_usage}\n";
		motd += "printf \\\"Local users:\\t%s\\n\\\" \\${users}\n";
		motd += "echo\n";
		motd += "echo \\\"HERE BE DRAGONS.\\\"\n";
		motd += "echo";
		units.addElement(model.getServerModel(server).getConfigsModel().addConfigFile("sshd_motd", "proceed", motd, "/etc/update-motd.d/00-motd"));
		units.addElement(new FilePermsUnit("sshd_motd_perms", "sshd_motd", "/etc/update-motd.d/00-motd", "755"));
		
		units.addElement(new SimpleUnit("sshd_rsa", "sshd_config",
				"echo -e \"y\\n\" | sudo ssh-keygen -f /etc/ssh/ssh_host_rsa_key -N \"\" -t rsa -b 4096",
				"sudo ssh-keygen -lf /etc/ssh/ssh_host_rsa_key | awk '{print $1}'", "4096", "pass",
				"Couldn't generate you a new SSH key.  This isn't too bad, but try re-running the script to get it to work."));

		// Secure sshd as per
		// https://stribika.github.io/2015/01/04/secure-secure-shell.html
		units.addElement(new SimpleUnit("sshd_ed25519", "sshd_config",
				"echo -e \"y\\n\" | sudo ssh-keygen -f /etc/ssh/ssh_host_ed25519_key -N \"\" -t ed25519",
				"sudo ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key | awk '{print $1}'", "256", "pass",
				"Couldn't generate you a strong ed25519 SSH key.  This isn't too bad, but try re-running the script to get it to work."));

		units.addElement(new SimpleUnit("sshd_moduli_exists", "sshd_config",
				"sudo ssh-keygen -G /etc/ssh/moduli.all -b 4096;"
						+ "sudo ssh-keygen -T /etc/ssh/moduli.safe -f /etc/ssh/moduli.all;"
						+ "sudo mv /etc/ssh/moduli.safe /etc/ssh/moduli;" + "sudo rm /etc/ssh/moduli.all",
				"cat /etc/ssh/moduli", "", "fail",
				"Couldn't generate new moduli for your SSH daemon.  This is undesirable, please try re-running the script."));

		units.addElement(new SimpleUnit("sshd_moduli_not_weak", "sshd_moduli_exists",
				"awk '$5 > 2000' /etc/ssh/moduli > /tmp/moduli;" + "sudo mv /tmp/moduli /etc/ssh/moduli;",
				"awk '$5 <= 2000' /etc/ssh/moduli", "", "pass",
				"Couldn't remove weak moduli from your SSH daemon.  This is undesirable, as it weakens your security.  Please re-run the script to try and get this to work."));

		for (String admin : model.getData().getAdmins(server)) {
			String sshDir = "/home/" + admin + "/.ssh";
			String keys   = sshDir + "/authorized_keys";
			
			units.addElement(new SimpleUnit("user_" + admin + "_created", "proceed",
					"sudo useradd -G sudo -p secret -d /home/" + admin + " -m " + admin,
					"id " + admin + " 2>&1", "id: ‘" + admin + "’: no such user", "fail",
					"The nginx user couldn't be added.  This will cause all sorts of errors."));
			
			//Create the .ssh dir for the user, with the correct permissions
			units.addElement(new DirUnit("ssh_dir_" + admin, "sshd_config", sshDir));
			units.addElement(new DirOwnUnit("ssh_dir_" + admin, "ssh_dir_" + admin + "_created", sshDir, admin));
			units.addElement(new DirPermsUnit("ssh_dir_" + admin, "ssh_dir_" + admin + "_chowned", sshDir, "755"));

			//Create the authorized_keys file, with root permissions (we don't want users to be able to add arbitrary keys)
			units.addElement(new FileUnit("ssh_key_" + admin, "ssh_dir_" + admin + "_created", model.getData().getSSHKey(admin), keys));
			units.addElement(new FileOwnUnit("ssh_key_" + admin, "ssh_key_" + admin, keys, "root"));
			units.addElement(new FilePermsUnit("ssh_key_" + admin, "ssh_key_" + admin + "_chowned", keys, "644"));
		}
		
		return units;
	}

	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		if (model.getData().getAllowedSSHSource(server) != null) {
			for (String ip : model.getData().getAllowedSSHSource(server)) {
				units.addElement(model.getServerModel(server).getFirewallModel().addFilterInput("sshd_ipt_in",
						"-p tcp"
						+ " --dport " + model.getData().getSSHPort(server)
						+ " -s " + ip
						+ " -j ACCEPT"));
				units.addElement(model.getServerModel(server).getFirewallModel().addFilterOutput("sshd_ipt_out",
						"-p tcp"
						+ " --sport " + model.getData().getSSHPort(server)
						+ " -d " + ip
						+ " -j ACCEPT"));
			}
		}
		else {
			units.addElement(model.getServerModel(server).getFirewallModel().addFilterInput("sshd_ipt_in",
					"-p tcp --dport " + model.getData().getSSHPort(server) + " -j ACCEPT"));
			units.addElement(model.getServerModel(server).getFirewallModel().addFilterOutput("sshd_ipt_out",
					"-p tcp --sport " + model.getData().getSSHPort(server) + " -j ACCEPT"));
		}
	
		return units;
	}

	protected Vector<IUnit> getLiveConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		units.addElement(new RunningUnit("sshd", "sshd", "sshd"));
		return units;
	}

}
