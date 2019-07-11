package core.model.machine;

import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;

import core.exception.AThornSecException;
import core.exception.data.NoValidUsersException;
import core.exception.data.machine.InvalidMachineException;
import core.exception.data.machine.InvalidServerException;
import core.exception.data.machine.InvalidUserException;
import core.exception.runtime.ARuntimeException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.machine.configuration.NetworkInterfaceModel;
import core.model.network.NetworkModel;

import core.profile.AProfile;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.FileAppendUnit;
import core.unit.fs.FilePermsUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;

import profile.machine.configuration.AptSources;
import profile.machine.configuration.BindFS;
import profile.machine.configuration.ConfigFiles;
import profile.machine.configuration.Processes;
import profile.machine.configuration.UserAccounts;

import profile.firewall.AFirewallProfile;
import profile.firewall.router.ShorewallFirewall;
import profile.type.Dedicated;
import profile.type.Metal;
import profile.type.Router;
import profile.type.Service;

import profile.service.machine.SSH;

public class ServerModel extends AMachineModel {
	private Set<AStructuredProfile> types;
	private Set<AProfile> profiles;

	//Server-specific 
	private AFirewallProfile firewall;
	private AptSources       aptSources;
	private Processes        runningProcesses;
	private BindFS           bindMounts;
	private ConfigFiles      configFiles;
	private UserAccounts     users;
	
	ServerModel(String label, NetworkModel networkModel)
	throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, InvalidMachineException, InvalidServerModelException, URISyntaxException {
		super(label, networkModel);
		
		String firewallProfile = networkModel.getData().getFirewallProfile(getLabel());

		//It's going to be *exceedingly* rare that this is set, but it should be customisable tbf
		if (firewallProfile != null) {
			this.firewall = (AFirewallProfile) Class.forName("profile.firewall." + firewallProfile).getDeclaredConstructor(ServerModel.class, NetworkModel.class).newInstance(this, networkModel);
		}
		
		this.types = new HashSet<AStructuredProfile>();
		for (String type : networkModel.getData().getTypes(getLabel())) {
			switch (type.toLowerCase()) {
				case "router":
					if (this.firewall == null) { this.firewall = new ShorewallFirewall(getLabel(), networkModel); }
					this.types.add(new Router(getLabel(), networkModel));
					break;
				case "metal":
//					this.types.add(new Metal(getLabel(), networkModel));
//					if (this.firewall == null) { this.firewall = new CsfFirewall(getLabel(), networkModel); }
					break;
				case "service":
//					this.types.add(new Service(getLabel(), networkModel));
					break;
				case "dedi":
//					this.types.add(new Dedicated(getLabel(), networkModel));
					break;
			}
		}
		
		this.profiles = new HashSet<AProfile>();
		for (String profile : networkModel.getData().getServerProfiles(getLabel())) {
			if (profile.equals("")) { continue; }
			
			AProfile profileClass = (AProfile) Class.forName("profile." + profile).getDeclaredConstructor(ServerModel.class, NetworkModel.class).newInstance(this, networkModel);
			this.profiles.add(profileClass);
		}
		
		this.runningProcesses = new Processes(getLabel(), networkModel);
		this.bindMounts       = new BindFS(getLabel(), networkModel);
		this.aptSources       = new AptSources(getLabel(), networkModel);
		this.users            = new UserAccounts(getLabel(), networkModel);
		this.configFiles      = new ConfigFiles(getLabel(), networkModel);
		
		if (networkModel.getData().getExternalIp(getLabel()) != null) {
			addIngress("*");
		}

		addEgress("cdn.debian.net:80,443");
		addEgress("security-cdn.debian.org:80,443");
		addEgress("prod.debian.map.fastly.net:80,443");
		addEgress("*:25");

	}

	@Override
	public Set<IUnit> getUnits()
	throws AThornSecException {
		Set<IUnit> units = new HashSet<IUnit>();

		units.add(new SimpleUnit("host", "proceed",
				"echo \"ERROR: Configuring with hostname mismatch\";",
				"sudo -S hostname;", getLabel(), "pass"));

		//Should we be autoupdating?
		String aptCommand = "";
		if (networkModel.getData().getAutoUpdate(getLabel())) {
			aptCommand = "sudo apt-get --assume-yes upgrade;";
		}
		else {
			aptCommand = "echo \"There are $(sudo apt-get upgrade -s | grep -P '^\\d+ upgraded'| cut -d' ' -f1) updates available, of which $(sudo apt-get upgrade -s | grep ^Inst | grep Security | wc -l) are security updates\"";
		}
		units.add(new SimpleUnit("update", "proceed",
				aptCommand,
				"sudo apt-get update > /dev/null; sudo apt-get --assume-no upgrade | grep \"[0-9] upgraded, [0-9] newly installed, [0-9] to remove and [0-9] not upgraded.\";",
				"0 upgraded, 0 newly installed, 0 to remove and 0 not upgraded.", "pass",
				"There are $(sudo apt-get upgrade -s | grep -P '^\\d+ upgraded'| cut -d' ' -f1) updates available, of which $(sudo apt-get upgrade -s | grep ^Inst | grep Security | wc -l) are security updates\""));

		//Remove rdnssd (problematic as hell...)
		units.add(new SimpleUnit("rdnssd_uninstalled", "proceed",
				"sudo apt remove rdnssd --purge -y;",
				"dpkg-query --status rdnssd 2>&1 | grep \"Status:\";", "Status: install ok installed", "fail",
				"Couldn't uninstall rdnssd.  This is a package which attempts to be \"clever\" in DNS configuration and just breaks everything instead."));
		
		//SSH ssh = new SSH(this, networkModel);
		//units.addAll(ssh.getUnits());

		this.runningProcesses.addProcess("sshd: " + networkModel.getData().getUser() + " \\[priv\\]$");
		this.runningProcesses.addProcess("sshd: " + networkModel.getData().getUser() + "@pts/0$");
		
		//Useful packages
		units.add(new InstalledUnit("sysstat", "proceed", "sysstat"));
		units.add(new InstalledUnit("lsof", "proceed", "lsof"));
		units.add(new InstalledUnit("net_tools", "proceed", "net-tools"));
		units.add(new InstalledUnit("htop", "proceed", "htop"));
		units.add(new InstalledUnit("mutt", "proceed", "mutt"));
		
		for (AStructuredProfile type : types) {
			units.addAll(type.getUnits());
		}
		
		for (AProfile profile : this.profiles) {
			units.addAll(profile.getUnits());
		}

		units.addAll(serverConfig());
		
		units.addAll(firewall.getUnits());
		units.addAll(bindMounts.getUnits());
		units.addAll(aptSources.getUnits());
		units.addAll(configFiles.getUnits());
		units.addAll(runningProcesses.getUnits());
		units.addAll(users.getUnits());

		units.add(new FileAppendUnit("auto_logout", "proceed",
				"TMOUT=" + ( ( 2*60 ) *60 ) + "\n" + //two hour timeout 
				"readonly TMOUT\n" + 
				"export TMOUT",
				"/etc/profile",
				"Couldn't set the serial timeout. This means users who forget to log out won't be auto logged out after two hours."));

		units.add(new SimpleUnit("apt_autoremove", "proceed",
				"sudo apt-get autoremove --purge --assume-yes",
				"sudo apt-get autoremove --purge --assume-no | grep \"0 to remove\"", "", "fail"));	
		
		return units;
	}
	
	private Set<IUnit> serverConfig() throws InvalidUserException, InvalidServerException, InvalidMachineException {
		Set<IUnit> units = new HashSet<IUnit>();
		
		//Shouldn't /really/ be doing this out here, but these should be the only RAW sockets, in the only circumstances, in a TS network...
		String excludeKnownRaw = "";
		//if (this.isRouter()) {
			excludeKnownRaw += " | grep -v \"dhcpd\"";
			//if (Objects.equals(this.getExtConn(), "ppp")) {
			//	excludeKnownRaw += " | grep -v \"pppd\"";
			//}
		//}
		
		units.add(new SimpleUnit("no_raw_sockets", "lsof_installed",
				"",
				"sudo lsof | grep RAW" + excludeKnownRaw, "", "pass",
				"There are raw sockets running on this machine.  This is almost certainly a sign of compromise."));
		
		//Verify our PAM modules haven't been tampered with
		//https://www.trustedsec.com/2018/04/malware-linux/
		units.add(new SimpleUnit("pam_not_tampered", "proceed",
				"",
				"find /lib/$(uname -m)-linux-gnu/security/ | xargs dpkg -S | cut -d ':' -f 1 | uniq | xargs sudo dpkg -V", "", "pass",
				"There are unexpected/tampered PAM modules on this machine.  This is almost certainly an indicator that this machine has been compromised!"));

		//Check for random SSH keys
		//https://security.stackexchange.com/a/151581
		String excludeKnownSSHKeys = "";

		for (String admin : networkModel.getData().getAdmins(getLabel())) {
			excludeKnownSSHKeys += " | grep -v \"" + networkModel.getData().getUserSSHKey(admin) + "\"";
		}
		
		units.add(new SimpleUnit("no_additional_ssh_keys", "proceed",
				"",
				"for X in $(cut -f6 -d ':' /etc/passwd |sort |uniq); do"
				+ "   for suffix in \"\" \"2\"; do"
				+ "       if sudo [ -s \"${X}/.ssh/authorized_keys$suffix\" ]; then"
				+ "           cat \"${X}/.ssh/authorized_keys$suffix\";"
				+ "       fi;"
				+ "   done;"
				+ "done"
				+ excludeKnownSSHKeys,
				"", "pass",
				"There are unexpected SSH keys on this machine.  This is almost certainly an indicator that this machine has been compromised!"));

		//Check for unexpected executables
		//if (this.isService()) {
		//	units.add(new SimpleUnit("no_unexpected_executables", "proceed",
		//			"",
		//			"find /proc/*/exe -exec readlink {} + | xargs sudo dpkg -S 2>&1 | egrep -v \"/opt/VBoxGuestAdditions-[5-9]{1}\\\\.[0-9]{1,2}\\\\.[0-9]{1,2}/sbin/VBoxService\" | grep 'no path' | grep -v 'deleted'", "", "pass",
		//			"There are unexpected executables running on this machine.  This could be innocent, but is probably a sign of compromise."));
		//}
		//else {
		//	units.addElement(new SimpleUnit("no_unexpected_executables", "proceed",
		//			"",
		//			"find /proc/*/exe -exec readlink {} + | xargs sudo dpkg -S 2>&1 | grep 'no path' | grep -v 'deleted'", "", "pass",
		//			"There are unexpected executables running on this machine.  This could be innocent, but is probably a sign of compromise."));
		//}

		//String emailOnPAM = "";
		//emailOnPAM += "#!/bin/bash\n";
		//emailOnPAM += "\n";
		//emailOnPAM += "host=\\$(hostname)\n";
		//emailOnPAM += "domain=\\\"" + networkModel.getData().getDomain(getLabel()) + "\\\"\n";
		//emailOnPAM += "sender=\\\"" + networkModel.getMachineModel(getLabel()).getEmailAddress() + "\\\"\n";
		//emailOnPAM += "recipients=( ";
		
		//for (String admin : networkModel.getData().getAdmins()) {
		//	emailOnPAM += "\\\"" + admin + "@\\$domain\\\" ";
		//}
		
		//for (String admin : networkModel.getData().getAdmins(getLabel())) {
		//	emailOnPAM += "\\\"" + networkModel.getDeviceModel(admin).getEmailAddress() + "\\\" ";
		//}
		
		//emailOnPAM += ")\n";
		//emailOnPAM += "message=\\$(env)\n";
		//emailOnPAM += "\n";
//		emailOnPAM += "for recipient in \\\"\\${recipients[@]}\\\"\n";
//		emailOnPAM += "do\n";
//		emailOnPAM += "    case \\\"\\${PAM_SERVICE}\\\" in\n";
//		emailOnPAM += "        sshd)\n";
//		emailOnPAM += "            if [ \\${PAM_TYPE} = \\\"open_session\\\" ] ; then\n";
//		emailOnPAM += "                subject=\\\"SSH on \\${host}: \\${PAM_USER} has connected from \\${PAM_RHOST}\\\"\n";
//		emailOnPAM += "            else\n";
//		emailOnPAM += "                subject=\\\"SSH on \\${host}: \\${PAM_USER} has disconnected\\\"\n";
//		emailOnPAM += "            fi\n";
//		emailOnPAM += "            ;;\n";
//		emailOnPAM += "        sudo)\n";
//		emailOnPAM += "            if [ \\${PAM_TYPE} = \\\"open_session\\\" ] ; then\n";
//		emailOnPAM += "                subject=\\\"SSH on \\${host}: \\${PAM_RUSER} has sudo'd to \\${PAM_USER}\\\"\n";
//		emailOnPAM += "            else\n";
//		emailOnPAM += "                subject=\\\"SSH on \\${host}: \\${PAM_USER} has disconnected\\\"\n";
//		emailOnPAM += "            fi\n";
//		emailOnPAM += "            ;;\n";
//		emailOnPAM += "    esac\n";
//		emailOnPAM += "\n";
//		emailOnPAM += "    (echo \\\"\\${message}\\\") | mutt -e \\\"set realname='\\${emailFromRealName}ï¸�' from=\\${sender}\\\" -s \\${subject} -n \\${recipient}\n";
//		//emailOnPAM += "    (echo \\\"Subject: \\$subject\\\"; echo \\\"\\$message\\\") | sendmail -f\\\"\\$sender\\\" \\\"\\$recipient\\\"\n";
//		emailOnPAM += "done";
//		
//		units.addElement(new FileUnit("email_on_pam_script_created", "proceed", emailOnPAM, "/etc/pamEmails.sh",
//				"I couldn't output the file for emailing on SSH login or sudo.  This means you won't receive alerts."));
//		units.addElement(new FilePermsUnit("email_on_pam_script_perms", "email_on_pam_script_created", "/etc/pamEmails.sh", "755"));
		
//		String sshdPAM = "";
//		sshdPAM += "@include common-auth\n";
//		sshdPAM += "account    required     pam_nologin.so\n";
//		sshdPAM += "@include common-account\n";
//		sshdPAM += "session [success=ok ignore=ignore module_unknown=ignore default=bad]        pam_selinux.so close\n";
//		sshdPAM += "session    required     pam_loginuid.so\n";
//		sshdPAM += "session    optional     pam_keyinit.so force revoke\n";
//		sshdPAM += "@include common-session\n";
//		sshdPAM += "session    optional     pam_motd.so  motd=/run/motd.dynamic\n";
//		sshdPAM += "session    optional     pam_motd.so noupdate\n";
//		sshdPAM += "session    optional     pam_mail.so standard noenv\n";
//		sshdPAM += "session    required     pam_limits.so\n";
//		sshdPAM += "session    required     pam_env.so\n";
//		sshdPAM += "session    required     pam_env.so user_readenv=1 envfile=/etc/default/locale\n";
//		sshdPAM += "session [success=ok ignore=ignore module_unknown=ignore default=bad]        pam_selinux.so open\n";
//		sshdPAM += "@include common-password\n";
//		sshdPAM += "session optional pam_exec.so seteuid /etc/pamEmails.sh";
//		
//		units.add(this.getConfigsModel().addConfigFile("pam_sshd_script_created", "email_on_pam_script_created", sshdPAM, "/etc/pam.d/sshd"));
		
		return units;
	}
	
	public AFirewallProfile getFirewallModel() {
		return this.firewall;
	}
	
	protected Processes getProcessModel() {
		return this.runningProcesses;
	}
	
	public BindFS getBindFsModel() {
		return this.bindMounts;
	}
	
	public AptSources getAptSourcesModel() {
		return this.aptSources;
	}
	
	public UserAccounts getUserModel() {
		return this.users;
	}
	
	public ConfigFiles getConfigsModel() {
		return this.configFiles;
	}
	
	public final void addProcessString(String psString) {
		this.getProcessModel().addProcess(psString);
	}

	public final void addSystemUsername(String username) {
		this.users.addUsername(username);		
	}

	public void addConfigFile(String path) {
		this.configFiles.addConfigFilePath(path);
	}
}
