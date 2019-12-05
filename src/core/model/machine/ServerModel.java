/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;

import javax.json.stream.JsonParsingException;
import javax.mail.internet.AddressException;

import com.metapossum.utils.scanner.reflect.ClassesInPackageScanner;

import core.data.machine.AMachineData.MachineType;
import core.exception.AThornSecException;
import core.exception.data.machine.InvalidMachineException;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.InvalidProfileException;
import core.iface.IUnit;
import core.model.machine.configuration.networking.NetworkInterfaceModel;
import core.model.network.NetworkModel;
import core.profile.AProfile;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.FileAppendUnit;
import core.unit.pkg.InstalledUnit;
import profile.firewall.AFirewallProfile;
import profile.firewall.machine.CSFFirewall;
import profile.firewall.router.ShorewallFirewall;
import profile.machine.configuration.AptSources;
import profile.machine.configuration.BindFS;
import profile.machine.configuration.Processes;
import profile.machine.configuration.UserAccounts;
import profile.service.machine.SSH;
import profile.type.Dedicated;
import profile.type.HyperVisor;
import profile.type.Router;
import profile.type.Service;

/**
 * This Class represents a Server. It is either used directly, or is called via
 * one of its children.
 */
public class ServerModel extends AMachineModel {
	private Collection<AStructuredProfile> types;
	private Collection<AProfile> profiles;

	// Server-specific
	private AFirewallProfile firewall;
	private final AptSources aptSources;
	private final Processes runningProcesses;
	private final BindFS bindMounts;
	// private final ConfigFiles configFiles;
	private final UserAccounts users;

	public ServerModel(String label, NetworkModel networkModel) throws AThornSecException, AddressException, JsonParsingException, IOException, URISyntaxException {
		super(label, networkModel);

		final String firewall = getNetworkModel().getData().getFirewallProfile(getLabel());

		// It's going to be *exceedingly* rare that this is set, but it should be
		// customisable tbf
		if (firewall != null) {
			Collection<Class<?>> firewallClasses = null;
			try {
				firewallClasses = new ClassesInPackageScanner().setResourceNameFilter((packageName, fileName) -> fileName.equals(firewall + ".class")).scan("profile.firewall");

				if (firewallClasses.isEmpty()) {
					throw new InvalidProfileException(firewall + " is not a valid firewall profile");
				}

				final String firewallClass = firewallClasses.iterator().next().getPackageName();

				this.firewall = (AFirewallProfile) Class.forName(firewallClass).getDeclaredConstructor(String.class, NetworkModel.class).newInstance(getLabel(), networkModel);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException
					| ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		this.runningProcesses = new Processes(getLabel(), this.networkModel);
		this.bindMounts = new BindFS(getLabel(), this.networkModel);
		this.aptSources = new AptSources(getLabel(), this.networkModel);
		this.users = new UserAccounts(getLabel(), this.networkModel);
	}

	@Override
	public void init() throws AThornSecException {
		// TODO: Probably a cleaner way of doing the below
		this.types = new HashSet<>();
		for (final MachineType type : getNetworkModel().getData().getTypes(getLabel())) {
			switch (type) {
			case ROUTER:
				if (this.firewall == null) {
					this.firewall = new ShorewallFirewall(getLabel(), this.networkModel);
				}
				this.types.add(new Router(getLabel(), this.networkModel));
				break;
			case HYPERVISOR:
				if (this.firewall == null) {
					this.firewall = new CSFFirewall(getLabel(), this.networkModel);
				}
				this.types.add(new HyperVisor(getLabel(), this.networkModel));
				break;
			case SERVICE:
				if (this.firewall == null) {
					this.firewall = new CSFFirewall(getLabel(), this.networkModel);
				}
				this.types.add(new Service(getLabel(), this.networkModel));
				break;
			case DEDICATED:
				this.types.add(new Dedicated(getLabel(), this.networkModel));
				break;
			default:
				break;
			}
		}

		this.profiles = new HashSet<>();
		for (final String profile : getNetworkModel().getData().getProfiles(getLabel())) {
			try {
				addProfile(profile);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException
					| ClassNotFoundException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void addProfile(String... profiles) throws IOException, InvalidProfileException, InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
		if (this.profiles == null) {
			this.profiles = new LinkedHashSet<>();
		}

		for (final String profile : profiles) {
			if (profile.equals("")) {
				continue;
			}

			final Collection<Class<?>> classes = new ClassesInPackageScanner().setResourceNameFilter((packageName, fileName) -> fileName.equals(profile + ".class"))
					.scan("profile");

			if (classes.isEmpty()) {
				throw new InvalidProfileException("I can't find profile " + profile);
			}

			for (final Class<?> profileClass : classes) {
				final AProfile theProfile = (AProfile) Class.forName(profileClass.getName()).getDeclaredConstructor(String.class, NetworkModel.class).newInstance(getLabel(),
						getNetworkModel());
				this.profiles.add(theProfile);
			}
		}
	}

	public Collection<IUnit> getPersistentFirewall() throws InvalidMachineException {
		if (!getNetworkModel().getData().getExternalIPs(getLabel()).isEmpty()) {
			addIngress("*");
		}

		addEgress("cdn.debian.net:80");
		addEgress("security-cdn.debian.org:80");
		addEgress("prod.debian.map.fastly.net:80");
		addEgress("cdn.debian.net:443");
		addEgress("security-cdn.debian.org:443");
		addEgress("prod.debian.map.fastly.net:443");
		addEgress("*:25");

		return null;
	}

	@Override
	public Collection<IUnit> getUnits() throws AThornSecException {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new SimpleUnit("host", "proceed", "echo \"ERROR: Configuring with hostname mismatch\";", "sudo -S hostname;", getLabel(), "pass"));

		units.add(new FileAppendUnit("auto_logout", "proceed", "TMOUT=" + ((2 * 60) * 60) + "\n" + // two hour timeout
				"readonly TMOUT\n" + "export TMOUT", "/etc/profile",
				"Couldn't set the serial timeout. This means users who forget to log out won't be auto logged out after two hours."));

		// Should we be autoupdating?
		String aptCommand = "";
		if (getNetworkModel().getData().getAutoUpdate(getLabel())) {
			aptCommand = "sudo apt-get --assume-yes upgrade;";
		} else {
			aptCommand = "echo \"There are $(sudo apt-get upgrade -s | grep -P '^\\d+ upgraded'| cut -d' ' -f1) updates available, of which $(sudo apt-get upgrade -s | grep ^Inst | grep Security | wc -l) are security updates\"";
		}
		units.add(new SimpleUnit("update", "proceed", aptCommand,
				"sudo apt-get update > /dev/null; sudo apt-get --assume-no upgrade | grep \"[0-9] upgraded, [0-9] newly installed, [0-9] to remove and [0-9] not upgraded.\";",
				"0 upgraded, 0 newly installed, 0 to remove and 0 not upgraded.", "pass",
				"There are $(sudo apt-get upgrade -s | grep -P '^\\d+ upgraded'| cut -d' ' -f1) updates available, of which $(sudo apt-get upgrade -s | grep ^Inst | grep Security | wc -l) are security updates\""));

		final SSH ssh = new SSH(getLabel(), getNetworkModel());
		units.addAll(ssh.getUnits());

		// Useful packages
		units.add(new InstalledUnit("sysstat", "proceed", "sysstat"));
		units.add(new InstalledUnit("lsof", "proceed", "lsof"));
		units.add(new InstalledUnit("net_tools", "proceed", "net-tools"));
		units.add(new InstalledUnit("htop", "proceed", "htop"));

		final Collection<IUnit> typesAndProfileUnits = new ArrayList<>();
		for (final AStructuredProfile type : this.types) {
			typesAndProfileUnits.addAll(type.getUnits());
		}

		for (final AProfile profile : this.profiles) {
			typesAndProfileUnits.addAll(profile.getUnits());
		}

		units.addAll(this.aptSources.getUnits());

		// Before we go any further... now the machine is at least up to date, and has a
		// couple of useful diagnostics packages installed...
		for (final NetworkInterfaceModel nic : getNetworkInterfaces()) {
			if (nic.getNetworkFile() != null) {
				units.add(nic.getNetworkFile());
			}
			if (nic.getNetDevFile() != null) {
				units.add(nic.getNetDevFile());
			}
		}

		units.addAll(typesAndProfileUnits);
		units.addAll(serverConfig());

		if (getFirewall() != null) { // Some machines don't have firewalls for me to configure
			units.addAll(getFirewall().getUnits());
		}
		units.addAll(this.bindMounts.getUnits());
		units.addAll(this.runningProcesses.getUnits());
		units.addAll(this.users.getUnits());

		units.add(new SimpleUnit("apt_autoremove", "proceed", "sudo apt-get autoremove --purge --assume-yes", "sudo apt-get autoremove --purge --assume-no | grep \"0 to remove\"",
				"", "fail"));

		return units;
	}

	private Collection<IUnit> serverConfig() throws InvalidMachineException {
		final Collection<IUnit> units = new ArrayList<>();

		// Shouldn't /really/ be doing this out here, but these should be the only RAW
		// sockets, in the only circumstances, in a TS network...
		String excludeKnownRaw = "";
		// if (this.isRouter()) {
		excludeKnownRaw += " | grep -v \"dhcpd\"";
		// if (Objects.equals(this.getExtConn(), "ppp")) {
		// excludeKnownRaw += " | grep -v \"pppd\"";
		// }
		// }

		units.add(new SimpleUnit("no_raw_sockets", "lsof_installed", "", "sudo lsof | grep RAW" + excludeKnownRaw, "", "pass",
				"There are raw sockets running on this machine.  This is almost certainly a sign of compromise."));

		// Verify our PAM modules haven't been tampered with
		// https://www.trustedsec.com/2018/04/malware-linux/
		units.add(new SimpleUnit("pam_not_tampered", "proceed", "", "find /lib/$(uname -m)-linux-gnu/security/ | xargs dpkg -S | cut -d ':' -f 1 | uniq | xargs sudo dpkg -V", "",
				"pass", "There are unexpected/tampered PAM modules on this machine.  This is almost certainly an indicator that this machine has been compromised!"));

		// Check for random SSH keys
		// https://security.stackexchange.com/a/151581
		String excludeKnownSSHKeys = "";

		for (final String admin : getNetworkModel().getData().getAdmins(getLabel())) {
			excludeKnownSSHKeys += " | grep -v \"" + getNetworkModel().getData().getSSHKey(admin) + "\"";
		}

		units.add(new SimpleUnit("no_additional_ssh_keys", "proceed", "",
				"for X in $(cut -f6 -d ':' /etc/passwd |sort |uniq); do" + "   for suffix in \"\" \"2\"; do" + "       if sudo [ -s \"${X}/.ssh/authorized_keys$suffix\" ]; then"
						+ "           cat \"${X}/.ssh/authorized_keys$suffix\";" + "       fi;" + "   done;" + "done" + excludeKnownSSHKeys,
				"", "pass", "There are unexpected SSH keys on this machine.  This is almost certainly an indicator that this machine has been compromised!"));

		// Check for unexpected executables
		// if (this.isService()) {
		// units.add(new SimpleUnit("no_unexpected_executables", "proceed",
		// "",
		// "find /proc/*/exe -exec readlink {} + | xargs sudo dpkg -S 2>&1 | egrep -v
		// \"/opt/VBoxGuestAdditions-[5-9]{1}\\\\.[0-9]{1,2}\\\\.[0-9]{1,2}/sbin/VBoxService\"
		// | grep 'no path' | grep -v 'deleted'", "", "pass",
		// "There are unexpected executables running on this machine. This could be
		// innocent, but is probably a sign of compromise."));
		// }
		// else {
		// units.addElement(new SimpleUnit("no_unexpected_executables", "proceed",
		// "",
		// "find /proc/*/exe -exec readlink {} + | xargs sudo dpkg -S 2>&1 | grep 'no
		// path' | grep -v 'deleted'", "", "pass",
		// "There are unexpected executables running on this machine. This could be
		// innocent, but is probably a sign of compromise."));
		// }

		// String emailOnPAM = "";
		// emailOnPAM += "#!/bin/bash\n";
		// emailOnPAM += "\n";
		// emailOnPAM += "host=\\$(hostname)\n";
		// emailOnPAM += "domain=\\\"" +
		// getNetworkModel().getData().getDomain(getLabel()) +
		// "\\\"\n";
		// emailOnPAM += "sender=\\\"" +
		// getNetworkModel().getMachineModel(getLabel()).getEmailAddress() + "\\\"\n";
		// emailOnPAM += "recipients=( ";

		// for (String admin : getNetworkModel().getData().getAdmins()) {
		// emailOnPAM += "\\\"" + admin + "@\\$domain\\\" ";
		// }

		// for (String admin : getNetworkModel().getData().getAdmins(getLabel())) {
		// emailOnPAM += "\\\"" +
		// getNetworkModel().getDeviceModel(admin).getEmailAddress() +
		// "\\\" ";
		// }

		// emailOnPAM += ")\n";
		// emailOnPAM += "message=\\$(env)\n";
		// emailOnPAM += "\n";
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

	public Collection<AStructuredProfile> getTypes() {
		return this.types;
	}

	public Boolean isType(MachineType type) throws InvalidServerException {
		return getNetworkModel().getData().getTypes(getLabel()).contains(type);
	}

	public Boolean isRouter() throws InvalidServerException {
		return isType(MachineType.ROUTER);
	}

	public Boolean isHyperVisor() throws InvalidServerException {
		return isType(MachineType.HYPERVISOR);
	}

	public AFirewallProfile getFirewall() {
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

	public final void addProcessString(String psString) {
		getProcessModel().addProcess(psString);
	}

	public final void addSystemUsername(String username) {
		this.users.addUsername(username);
	}
}
