package core.model;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Vector;

import javax.swing.JOptionPane;

import core.iface.IUnit;

import core.profile.AProfile;
import core.profile.AStructuredProfile;

import core.unit.SimpleUnit;
import core.unit.fs.FileAppendUnit;
import core.unit.fs.FilePermsUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;

import profile.Dedicated;
import profile.Metal;
import profile.Router;
import profile.SSH;
import profile.Service;

public class ServerModel extends MachineModel {
	private Vector<IUnit> initUnits;
	
	private Vector<ServerModel> services;
	
	private Router router;
	private Vector<AStructuredProfile> types;
	private Vector<AProfile> profiles;

	//Server-specific 
	private AptSourcesModel aptSources;
	private FirewallModel   firewall;
	private ProcessModel    runningProcesses;
	private BindFsModel     bindMounts;
	private ConfigFileModel configFiles;
	private UserModel       users;
	
	ServerModel(String label, NetworkModel networkModel) {
		super(label, networkModel);
		
		this.types = new Vector<AStructuredProfile>();
		this.profiles = new Vector<AProfile>();

		this.initUnits = new Vector<IUnit>();
		
		this.services = new Vector<ServerModel>();
		
		this.firewall = new FirewallModel(getLabel(), this, networkModel);
		this.firewall.init();
		
		this.runningProcesses = new ProcessModel(getLabel(), this, networkModel);
		this.runningProcesses.init();
		
		this.bindMounts = new BindFsModel(getLabel(), this, networkModel);
		this.bindMounts.init();
		
		this.aptSources = new AptSourcesModel(getLabel(), this, networkModel);
		this.aptSources.init();
		
		this.users = new UserModel(getLabel(), this, networkModel);
		this.users.init();
		
		this.configFiles = new ConfigFileModel(getLabel(), this, networkModel);
		this.configFiles.init();
	}
	
	public void init() {
		if (this.isRouter()) {
			router = new Router(this, networkModel);
			types.addElement(router);
		}
		if (this.isMetal()) {
			Metal metal = new Metal(this, networkModel);
			types.addElement(metal);
		}
		if (this.isService()) {
			Service service = new Service(this, networkModel);
			networkModel.registerOnMetal(this, getMetal());
			types.addElement(service);
		}
		if (this.isDedi()) {
			Dedicated dedi = new Dedicated(this, networkModel);
			types.addElement(dedi);
		}
		
		for (AStructuredProfile type : this.types) {
			type.init();
		}
		
		for (String profile : this.getProfiles()) {
			try {
				if (profile.equals("")) { continue; }
				
				AProfile profileClass = (AProfile) Class.forName("profile." + profile).getDeclaredConstructor(ServerModel.class, NetworkModel.class).newInstance(this, networkModel);
				this.profiles.addElement(profileClass);
				
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, profile + " has thrown an error.\n\nThe program will terminate");
				System.exit(1);
			}
		}

	}
	
	public void getNetworking() {
		for (AStructuredProfile type : this.types) {
			type.getNetworking();
		}
		for (AProfile profile : this.profiles) {
			profile.getNetworking();
		}
		
		if (networkModel.getData().getExternalIp(getLabel()) != null) {
			addRequiredIngress("255.255.255.255", getRequiredListen().toArray(new Integer[getRequiredListen().size()]));
		}

		addRequiredEgress("cdn.debian.net");
		addRequiredEgress("security-cdn.debian.org");
		addRequiredEgress("prod.debian.map.fastly.net");
		addRequiredEgress("255.255.255.255", 25);
	}

	void registerService(ServerModel service) {
		services.addElement(service);
	}
	
	public ServerModel getMetal() {
		return networkModel.getServerModel(getNetworkData().getMetal(getLabel()));
	}
	
	public Vector<ServerModel> getServices() {
		return services;
	}
	
	public Boolean isRouter() {
		return Arrays.stream(getTypes()).anyMatch("router"::equals);
	}

	public Boolean isMetal() {
		return Arrays.stream(getTypes()).anyMatch("metal"::equals);
	}
	
	public Boolean isService() {
		return Arrays.stream(getTypes()).anyMatch("service"::equals);
	}

	public Boolean isDedi() {
		return Arrays.stream(getTypes()).anyMatch("dedicated"::equals);
	}

	public Vector<IUnit> getUnits() {
		Vector<IUnit> units = new Vector<IUnit>();
		int i = 0;
		
		units.insertElementAt(new SimpleUnit("create_pid_file", "proceed",
				"",
				"touch ~/script.pid; [ -f ~/script.pid ] && echo pass || echo fail", "pass", "pass"), i);
		
		units.insertElementAt(new SimpleUnit("host", "proceed",
				"echo \"ERROR: Configuring with hostname mismatch\";",
				"sudo -S hostname;", getNetworkData().getHostname(getLabel()), "pass"), ++i);

		//Should we be autoupdating?
		String aptCommand = "";
		if (getNetworkData().autoUpdate(getLabel())) {
			aptCommand = "sudo apt-get --assume-yes upgrade;";
		}
		else {
			aptCommand = "echo \"There are `sudo apt-get upgrade -s |grep -P '^\\\\d+ upgraded'|cut -d\\\" \\\" -f1` updates available, of which `sudo apt-get upgrade -s | grep ^Inst | grep Security | wc -l` are security updates\"";
		}
		units.insertElementAt(new SimpleUnit("update", "proceed",
				aptCommand,
				"sudo apt-get update > /dev/null; sudo apt-get --assume-no upgrade | grep \"[0-9] upgraded, [0-9] newly installed, [0-9] to remove and [0-9] not upgraded.\";",
				"0 upgraded, 0 newly installed, 0 to remove and 0 not upgraded.", "pass",
				"There are $(sudo apt-get upgrade -s |grep -P '^\\\\d+ upgraded'|cut -d\" \" -f1) updates available, of which $(sudo apt-get upgrade -s | grep ^Inst | grep Security | wc -l) are security updates\""), ++i);

		//Remove rdnssd (problematic as hell...)
		units.insertElementAt(new SimpleUnit("rdnssd_uninstalled", "proceed",
				"sudo apt remove rdnssd --purge -y;",
				"dpkg-query --status rdnssd 2>&1 | grep \"Status:\";", "Status: install ok installed", "fail",
				"Couldn't uninstall rdnssd.  This is a package which attempts to be \"clever\" in DNS configuration and just breaks everything instead."), ++i);
		
		units.insertElementAt(new RunningUnit("syslog", "rsyslog", "rsyslog"), ++i);
		
		for (String admin : networkModel.getData().getAdmins(getLabel())) {
			String adminDefaultPassword = networkModel.getData().getUserDefaultPassword(admin);
			
			units.insertElementAt(new SimpleUnit("user_" + admin + "_created", "proceed",
					"sudo useradd"
						+ " -m " + admin //Username
						+ " -c \"" + networkModel.getData().getUserFullName(admin) + "\"" //Full name
						+ " -G sudo" //Groups
						+ " -s /bin/bash;"
						+ "echo " + admin + ":" + adminDefaultPassword + " | sudo chpasswd;" //Set their password
						+ "sudo passwd -e " + admin //Expire their password immediately
					,
					"id " + admin + " 2>&1", "id: ‘" + admin + "’: no such user", "fail",
					"The user " + admin + " couldn't be created on this machine."), ++i);
		}
		
		SSH ssh = new SSH(this, networkModel);
		units.addAll(ssh.getUnits());

		this.runningProcesses.addProcess("sshd: " + networkModel.getData().getUser() + " \\[priv\\]$");
		this.runningProcesses.addProcess("sshd: " + networkModel.getData().getUser() + "@pts/0$");
		
		//Useful packages
		units.addElement(new InstalledUnit("sysstat", "proceed", "sysstat"));
		units.addElement(new InstalledUnit("lsof", "proceed", "lsof"));
		units.addElement(new InstalledUnit("net_tools", "proceed", "net-tools"));
		units.addElement(new InstalledUnit("htop", "proceed", "htop"));
		units.addElement(new InstalledUnit("sendmail", "proceed", "sendmail"));
		
		units.addAll(initUnits);
		
		for (AStructuredProfile type : types) {
			units.addAll(type.getUnits());
		}
		
		for (AProfile profile : this.profiles) {
			try {
				units.addAll(profile.getUnits());
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, profile + " has thrown an error.\n\nThe program will terminate.\n\n" + e);
				System.exit(1);
			}
		}

		units.addAll(serverConfig());
		
		units.addAll(2, firewall.getUnits());
		units.addAll(2, bindMounts.getUnits());
		units.addAll(2, aptSources.getUnits());
		units.addAll(2, getNetworkIfaces().getUnits());
		units.addAll(configFiles.getUnits());
		units.addAll(runningProcesses.getUnits());
		units.addAll(users.getUnits());

		units.addElement(new FileAppendUnit("auto_logout", "proceed",
				"TMOUT=" + ( ( 2*60 ) *60 ) + "\n" + //two hour timeout 
				"readonly TMOUT\n" + 
				"export TMOUT",
				"/etc/profile",
				"Couldn't set the serial timeout. This means users who forget to log out won't be auto logged out after two hours."));

		units.addElement(new SimpleUnit("apt_autoremove", "proceed",
				"sudo apt-get autoremove --purge --assume-yes",
				"sudo apt-get autoremove --purge --assume-no | grep \"0 to remove\"", "", "fail"));	
		
		units.addElement(new SimpleUnit("delete_pid_file", "proceed",
				"",
				"rm ~/script.pid; [ -f ~/script.pid ] && echo fail || echo pass", "pass", "pass"));		
		
		//Make sure we have no duplication in our unit tests (this can happen occasionally)
		units = new Vector<IUnit>(new LinkedHashSet<IUnit>(units));
		
		return units;
	}
	
	private Vector<IUnit> serverConfig() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		//Shouldn't /really/ be doing this out here, but these should be the only RAW sockets, in the only circumstances, in a TS network...
		String excludeKnownRaw = "";
		if (this.isRouter()) {
			excludeKnownRaw += " | grep -v \"dhcpd\"";
			if (Objects.equals(this.getExtConn(), "ppp")) {
				excludeKnownRaw += " | grep -v \"pppd\"";
			}
		}
		
		units.addElement(new SimpleUnit("no_raw_sockets", "lsof_installed",
				"",
				"sudo lsof | grep RAW" + excludeKnownRaw, "", "pass",
				"There are raw sockets running on this machine.  This is almost certainly a sign of compromise."));
		
		//Verify our PAM modules haven't been tampered with
		//https://www.trustedsec.com/2018/04/malware-linux/
		units.addElement(new SimpleUnit("pam_not_tampered", "proceed",
				"",
				"find /lib/$(uname -m)-linux-gnu/security/ | xargs dpkg -S | cut -d ':' -f 1 | uniq | xargs sudo dpkg -V", "", "pass",
				"There are unexpected/tampered PAM modules on this machine.  This is almost certainly an indicator that this machine has been compromised!"));

		//Check for random SSH keys
		//https://security.stackexchange.com/a/151581
		String excludeKnownSSHKeys = "";

		for (String admin : networkModel.getData().getAdmins(getLabel())) {
			excludeKnownSSHKeys += " | grep -v \"" + networkModel.getData().getUserSSHKey(admin) + "\"";
		}
		
		units.addElement(new SimpleUnit("no_additional_ssh_keys", "proceed",
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
		if (this.isService()) {
			units.addElement(new SimpleUnit("no_unexpected_executables", "proceed",
					"",
					"find /proc/*/exe -exec readlink {} + | xargs sudo dpkg -S 2>&1 | egrep -v \"/opt/VBoxGuestAdditions-[5-9]{1}\\\\.[0-9]{1,2}\\\\.[0-9]{1,2}/sbin/VBoxService\" | grep 'no path' | grep -v 'deleted'", "", "pass",
					"There are unexpected executables running on this machine.  This could be innocent, but is probably a sign of compromise."));
		}
		else {
			units.addElement(new SimpleUnit("no_unexpected_executables", "proceed",
					"",
					"find /proc/*/exe -exec readlink {} + | xargs sudo dpkg -S 2>&1 | grep 'no path' | grep -v 'deleted'", "", "pass",
					"There are unexpected executables running on this machine.  This could be innocent, but is probably a sign of compromise."));
		}

		String emailOnPAM = "";
		emailOnPAM += "#!/bin/bash\n";
		emailOnPAM += "\n";
		emailOnPAM += "host=\\\"\\`hostname\\`\\\"\n";
		emailOnPAM += "domain=\\\"" + networkModel.getData().getDomain(getLabel()) + "\\\"\n";
		emailOnPAM += "sender=\\\"\\$host@\\$domain\\\"\n";
		emailOnPAM += "recipients=(";
		
		for (String admin : networkModel.getData().getAdmins()) {
			emailOnPAM += "\\\"" + admin + "@\\$domain\\\" ";
		}
		
		emailOnPAM += ")\n";
		emailOnPAM += "message=\\\"\\`env\\`\\\"\n";
		emailOnPAM += "\n";
		emailOnPAM += "for recipient in \\\"\\${recipients[@]}\\\"\n";
		emailOnPAM += "do\n";
		emailOnPAM += "    case \\\"\\$PAM_SERVICE\\\" in\n";
		emailOnPAM += "        sshd)\n";
		emailOnPAM += "            if [ \\$PAM_TYPE = \\\"open_session\\\" ] ; then\n";
		emailOnPAM += "                subject=\\\"SSH on \\$host: \\$PAM_USER has connected from \\$PAM_RHOST\\\"\n";
		emailOnPAM += "            else\n";
		emailOnPAM += "                subject=\\\"SSH on \\$host: \\$PAM_USER has disconnected\\\"\n";
		emailOnPAM += "            fi\n";
		emailOnPAM += "            ;;\n";
		emailOnPAM += "        sudo)\n";
		emailOnPAM += "            if [ \\$PAM_TYPE = \\\"open_session\\\" ] ; then\n";
		emailOnPAM += "                subject=\\\"SSH on \\$host: \\$PAM_RUSER has sudo'd to \\$PAM_USER\\\"\n";
		emailOnPAM += "            else\n";
		emailOnPAM += "                subject=\\\"SSH on \\$host: \\$PAM_USER has disconnected\\\"\n";
		emailOnPAM += "            fi\n";
		emailOnPAM += "            ;;\n";
		emailOnPAM += "    esac\n";
		emailOnPAM += "\n";
		emailOnPAM += "    (echo \\\"Subject: \\$subject\\\"; echo \\\"\\$message\\\") | sendmail -f\\\"\\$sender\\\" \\\"\\$recipient\\\"\n";
		emailOnPAM += "done";
		
		units.addElement(new FileUnit("email_on_pam_script_created", "proceed", emailOnPAM, "/etc/pamEmails.sh",
				"I couldn't output the file for emailing on SSH login or sudo.  This means you won't receive alerts."));
		units.addElement(new FilePermsUnit("email_on_pam_script_perms", "email_on_pam_script_created", "/etc/pamEmails.sh", "755"));
		
		String sshdPAM = "";
		sshdPAM += "@include common-auth\n";
		sshdPAM += "account    required     pam_nologin.so\n";
		sshdPAM += "@include common-account\n";
		sshdPAM += "session [success=ok ignore=ignore module_unknown=ignore default=bad]        pam_selinux.so close\n";
		sshdPAM += "session    required     pam_loginuid.so\n";
		sshdPAM += "session    optional     pam_keyinit.so force revoke\n";
		sshdPAM += "@include common-session\n";
		sshdPAM += "session    optional     pam_motd.so  motd=/run/motd.dynamic\n";
		sshdPAM += "session    optional     pam_motd.so noupdate\n";
		sshdPAM += "session    optional     pam_mail.so standard noenv\n";
		sshdPAM += "session    required     pam_limits.so\n";
		sshdPAM += "session    required     pam_env.so\n";
		sshdPAM += "session    required     pam_env.so user_readenv=1 envfile=/etc/default/locale\n";
		sshdPAM += "session [success=ok ignore=ignore module_unknown=ignore default=bad]        pam_selinux.so open\n";
		sshdPAM += "@include common-password\n";
		sshdPAM += "session optional pam_exec.so seteuid /etc/pamEmails.sh";
		
		units.addElement(this.getConfigsModel().addConfigFile("pam_sshd_script_created", "email_on_pam_script_created", sshdPAM, "/etc/pam.d/sshd"));
		
		return units;
	}
	
	public FirewallModel getFirewallModel() {
		return this.firewall;
	}
	
	public ProcessModel getProcessModel() {
		return this.runningProcesses;
	}
	
	public BindFsModel getBindFsModel() {
		return this.bindMounts;
	}
	
	public AptSourcesModel getAptSourcesModel() {
		return this.aptSources;
	}
	
	public UserModel getUserModel() {
		return this.users;
	}
	
	public ConfigFileModel getConfigsModel() {
		return this.configFiles;
	}
	
	public Router getRouter() {
		return this.router;
	}

	public int getUnitCount() {
		return 0; //this.getUnits().size();
	}

	public String[] getProfiles() {
		return getNetworkData().getServerProfiles(getLabel());
	}
	
	public String[] getTypes() {
		return getNetworkData().getTypes(getLabel());
	}
	
	public String getExtConn() {
		return getNetworkData().getExtConnectionType(getLabel());
	}
}
