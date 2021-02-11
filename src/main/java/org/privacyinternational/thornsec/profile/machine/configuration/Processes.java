/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.profile.machine.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.privacyinternational.thornsec.core.iface.IUnit;
import org.privacyinternational.thornsec.core.model.AModel;
import org.privacyinternational.thornsec.core.model.machine.ServerModel;
import org.privacyinternational.thornsec.core.model.network.NetworkModel;
import org.privacyinternational.thornsec.core.profile.AProfile;
import org.privacyinternational.thornsec.core.unit.SimpleUnit;

public class Processes extends AProfile {

	private final Collection<String> processStrings;

	public Processes(ServerModel server) {
		super(server);

		this.processStrings = new HashSet<String>();
		// These are processes related to TS
		this.processStrings.add("\\./script.sh; rm -rf script\\.sh; exit;");
		this.processStrings.add("/bin/bash \\./script.sh$");
		this.processStrings.add("/sbin/agetty --noclear tty[0-9] linux$");

		// These are default Debian/kernel processes
		this.processStrings.add("/lib/systemd/systemd --system --deserialize 14$");
		this.processStrings.add("\\[kthreadd\\]$");
		this.processStrings.add("\\[ksoftirqd/[0-9]{1,2}\\]$");
		this.processStrings.add("\\[kworker/0:0H\\]$");
		this.processStrings.add("\\[rcu_sched\\]$");
		this.processStrings.add("\\[rcu_bh\\]$");
		this.processStrings.add("\\[migration/[0-9]{1,2}\\]$");
		this.processStrings.add("\\[watchdog/[0-9]{1,2}\\]$");
		this.processStrings.add("\\[khelper\\]$");
		this.processStrings.add("\\[kdevtmpfs\\]$");
		this.processStrings.add("\\[netns\\]$");
		this.processStrings.add("\\[khungtaskd\\]$");
		this.processStrings.add("\\[writeback\\]$");
		this.processStrings.add("\\[ksmd\\]$");
		this.processStrings.add("\\[khugepaged\\]$");
		this.processStrings.add("\\[crypto\\]$");
		this.processStrings.add("\\[kintegrityd\\]$");
		this.processStrings.add("\\[bioset\\]$");
		this.processStrings.add("\\[kblockd\\]$");
		this.processStrings.add("\\[kswapd0\\]$");
		this.processStrings.add("\\[vmstat\\]$");
		this.processStrings.add("\\[fsnotify_mark\\]$");
		this.processStrings.add("\\[kthrotld\\]$");
		this.processStrings.add("\\[ipv6_addrconf\\]$");
		this.processStrings.add("\\[deferwq\\]$");
		this.processStrings.add("\\[ata_sff\\]$");
		this.processStrings.add("\\[kpsmoused\\]$");
		this.processStrings.add("\\[scsi_tmf_[0-9]{1,2}\\]$");
		this.processStrings.add("\\[scsi_eh_[0-9]{1,2}\\]$");
		this.processStrings.add("\\[kworker/[u]{0,1}[0-9]{1,2}\\:[0-9]{1,2}[H]{0,1}\\]$");
		this.processStrings.add("\\[ext4-rsv-conver\\]$");
		this.processStrings.add("\\[kauditd\\]$");
		this.processStrings.add("\\[jbd2/sda1-8\\]$");
		this.processStrings.add("/lib/systemd/systemd-journald$");
		this.processStrings.add("/lib/systemd/systemd-udevd$");
		this.processStrings.add(
				"/sbin/dhclient -4 -v -pf /run/dhclient.enp[0-9]s[0-9].pid -lf /var/lib/dhcp/dhclient.enp[0-9]s[0-9].leases -I -df /var/lib/dhcp/dhclient6.enp[0-9]s[0-9].leases enp[0-9]s[0-9]$");
		this.processStrings.add("/usr/sbin/cron -f$");
		this.processStrings.add("/usr/sbin/rsyslogd -n$");
		this.processStrings.add("/usr/sbin/acpid$");
		this.processStrings.add("/sbin/init$");
		this.processStrings.add("\\[perf\\]$");
		this.processStrings.add("\\[jbd2/dm-[0-9]{1}-8\\]$");
		this.processStrings.add("\\[kdmflush\\]$");
		this.processStrings.add("/sbin/rdnssd -u rdnssd -H /etc/rdnssd/merge-hook$");
		this.processStrings
				.add("/usr/bin/dbus-daemon --system --address=systemd: --nofork --nopidfile --systemd-activation$");
		this.processStrings.add("\\[khubd\\]$");
		this.processStrings.add("\\[edac-poller\\]$");
		this.processStrings.add("\\[hd-audio[0-9]*\\]$");
		this.processStrings.add("\\[led_workqueue\\]$");
		this.processStrings.add("/lib/systemd/systemd-logind$");
		this.processStrings.add("\\[dio/dm-0\\]$");
		this.processStrings.add("/usr/sbin/sshd -D$");
		this.processStrings.add("/lib/systemd/systemd --system --deserialize [0-9]{1,2}$");
		this.processStrings.add("\\[lru-add-drain\\]$");
		this.processStrings.add("\\[cpuhp/[0-9]+\\]$");
		this.processStrings.add("\\[oom_reaper\\]$");
		this.processStrings.add("\\[kcompactd0\\]$");
		this.processStrings.add("\\[devfreq_wq\\]$");
		this.processStrings.add("\\[watchdogd\\]$");
		this.processStrings.add("/lib/systemd/systemd-timesyncd$");
		this.processStrings.add("/lib/systemd/systemd --user$");
		this.processStrings.add("\\(sd-pam\\)$");
		this.processStrings.add("\\[dio/sda[0-9]\\]$");
		this.processStrings.add("/usr/lib/packagekit/packagekitd$");
		this.processStrings.add("/usr/lib/policykit-1/polkitd --no-debug$");
		this.processStrings.add("jbd2/sdb[0-9]-[0-9]$");
		this.processStrings.add("\\[acpi_thermal_pm\\]$");
		this.processStrings.add("\\[md\\]$");
		this.processStrings.add("\\[md[0-9]_raid[0-9]\\]$");
		this.processStrings.add("\\[raid[0-9]wq\\]$");
		this.processStrings.add("\\[md[0-9]*_raid[0-9]\\]$");
		this.processStrings.add("\\[jfsCommit\\]$");
		this.processStrings.add("\\[jfsIO\\]$");
		this.processStrings.add("/sbin/lvmetad -f$");
		this.processStrings.add("\\[usb-storage\\]$");
		this.processStrings.add("\\[rpciod\\]$");
		this.processStrings.add("\\[i915/signal:[0-9]\\]$");
		this.processStrings.add("\\[kcryptd\\]$");
		this.processStrings.add("\\[kcryptd_io\\]$");
		this.processStrings.add("\\[irq/36-mei_me\\]$");
		this.processStrings.add("/sbin/mdadm --monitor --scan$");
		this.processStrings.add("\\[xfsalloc\\]$");
		this.processStrings.add("\\[xfs_mru_cache\\]$");
		this.processStrings.add("\\[jfsSync\\]$");
		this.processStrings.add("\\[dmcrypt_write\\]$");
		this.processStrings.add("\\[dio/dm-[0-9]\\]$");
		this.processStrings.add("\\[xprtiod\\]$");
		this.processStrings.add("/usr/sbin/blkmapd$");
		this.processStrings.add("/sbin/rpcbind -f -w$");
		this.processStrings.add("\\[ttm_swap\\]$");
		this.processStrings.add("/usr/sbin/irqbalance --foreground$");
	}

	/**
	 * Checks for unexpected processes
	 *
	 */
	public Collection<IUnit> getUnits() {
		String grepString = "sudo ps -Awwo pid,user,comm,args | grep -v grep | grep -v 'ps -Awwo pid,user,comm,args$'";
		final Collection<IUnit> units = new ArrayList<>();

		for (final String processString : this.processStrings) {
			grepString += " | grep -Ev \"" + processString + "\"";
		}

		grepString += " | tee /dev/stderr | grep -v 'tee /dev/stderr'"; // We want to be able to see what's running if
																		// the audit fails!

		units.add(new SimpleUnit("no_unexpected_processes", "proceed", "", grepString,
				"  PID USER     COMMAND         COMMAND", "pass",
				"There are unexpected processes running on this machine.  This could be a sign that the machine is compromised, or it could be "
						+ "entirely innocent.  Please check the processes carefully to see if there's any cause for concern.  If this machine is a metal, "
						+ "it's not unexpected for there to be processes which haven't been explicitly whitelisted in our base config."));

		return units;
	}

	public final void addProcess(String psString) {
		this.processStrings.add(psString);
	}

}
