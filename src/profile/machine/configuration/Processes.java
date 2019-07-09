package profile.machine.configuration;

import java.util.HashSet;
import java.util.Set;

import core.iface.IUnit;
import core.model.AModel;
import core.model.network.NetworkModel;
import core.unit.SimpleUnit;

public class Processes extends AModel {

	private Set<String> processStrings;

	public Processes(String label, NetworkModel networkModel) {
		super(label, networkModel);

		this.processStrings = new HashSet<String>();
		//These are processes related to TS
		processStrings.add("\\./script.sh; rm -rf script\\.sh; exit;");
		processStrings.add("/bin/bash \\./script.sh$");
		processStrings.add("/sbin/agetty --noclear tty[0-9] linux$");
		
		//These are default Debian/kernel processes
		processStrings.add("/lib/systemd/systemd --system --deserialize 14$");
		processStrings.add("\\[kthreadd\\]$");
		processStrings.add("\\[ksoftirqd/[0-9]{1,2}\\]$");
		processStrings.add("\\[kworker/0:0H\\]$");
		processStrings.add("\\[rcu_sched\\]$");
		processStrings.add("\\[rcu_bh\\]$");
		processStrings.add("\\[migration/[0-9]{1,2}\\]$");
		processStrings.add("\\[watchdog/[0-9]{1,2}\\]$");
		processStrings.add("\\[khelper\\]$");
		processStrings.add("\\[kdevtmpfs\\]$");
		processStrings.add("\\[netns\\]$");
		processStrings.add("\\[khungtaskd\\]$");
		processStrings.add("\\[writeback\\]$");
		processStrings.add("\\[ksmd\\]$");
		processStrings.add("\\[khugepaged\\]$");
		processStrings.add("\\[crypto\\]$");
		processStrings.add("\\[kintegrityd\\]$");
		processStrings.add("\\[bioset\\]$");
		processStrings.add("\\[kblockd\\]$");
		processStrings.add("\\[kswapd0\\]$");
		processStrings.add("\\[vmstat\\]$");
		processStrings.add("\\[fsnotify_mark\\]$");
		processStrings.add("\\[kthrotld\\]$");
		processStrings.add("\\[ipv6_addrconf\\]$");
		processStrings.add("\\[deferwq\\]$");
		processStrings.add("\\[ata_sff\\]$");
		processStrings.add("\\[kpsmoused\\]$");
		processStrings.add("\\[scsi_tmf_[0-9]{1,2}\\]$");
		processStrings.add("\\[scsi_eh_[0-9]{1,2}\\]$");
		processStrings.add("\\[kworker/[u]{0,1}[0-9]{1,2}\\:[0-9]{1,2}[H]{0,1}\\]$");
		processStrings.add("\\[ext4-rsv-conver\\]$");
		processStrings.add("\\[kauditd\\]$");
		processStrings.add("\\[jbd2/sda1-8\\]$");
		processStrings.add("/lib/systemd/systemd-journald$");
		processStrings.add("/lib/systemd/systemd-udevd$");
		processStrings.add("/sbin/dhclient -4 -v -pf /run/dhclient.enp[0-9]s[0-9].pid -lf /var/lib/dhcp/dhclient.enp[0-9]s[0-9].leases -I -df /var/lib/dhcp/dhclient6.enp[0-9]s[0-9].leases enp[0-9]s[0-9]$");
		processStrings.add("/usr/sbin/cron -f$");
		processStrings.add("/usr/sbin/rsyslogd -n$");
		processStrings.add("/usr/sbin/acpid$");
		processStrings.add("/sbin/init$");
		processStrings.add("\\[perf\\]$");
		processStrings.add("\\[jbd2/dm-[0-9]{1}-8\\]$");
		processStrings.add("\\[kdmflush\\]$");
		processStrings.add("/sbin/rdnssd -u rdnssd -H /etc/rdnssd/merge-hook$");
		processStrings.add("/usr/bin/dbus-daemon --system --address=systemd: --nofork --nopidfile --systemd-activation$");
		processStrings.add("\\[khubd\\]$");
		processStrings.add("\\[edac-poller\\]$");
		processStrings.add("\\[hd-audio[0-9]*\\]$");
		processStrings.add("\\[led_workqueue\\]$");
		processStrings.add("/lib/systemd/systemd-logind$");
		processStrings.add("\\[dio/dm-0\\]$");
		processStrings.add("/usr/sbin/sshd -D$");
		processStrings.add("/lib/systemd/systemd --system --deserialize [0-9]{1,2}$");
		processStrings.add("\\[lru-add-drain\\]$");
		processStrings.add("\\[cpuhp/[0-9]+\\]$");
		processStrings.add("\\[oom_reaper\\]$");
		processStrings.add("\\[kcompactd0\\]$");
		processStrings.add("\\[devfreq_wq\\]$");
		processStrings.add("\\[watchdogd\\]$");
		processStrings.add("/lib/systemd/systemd-timesyncd$");
		processStrings.add("/lib/systemd/systemd --user$");
		processStrings.add("\\(sd-pam\\)$");
		processStrings.add("\\[dio/sda[0-9]\\]$");
		processStrings.add("/usr/lib/packagekit/packagekitd$");
		processStrings.add("/usr/lib/policykit-1/polkitd --no-debug$");
		processStrings.add("jbd2/sdb[0-9]-[0-9]$");
		processStrings.add("\\[acpi_thermal_pm\\]$");
		processStrings.add("\\[md\\]$");
		processStrings.add("\\[md[0-9]_raid[0-9]\\]$");
		processStrings.add("\\[raid[0-9]wq\\]$");
		processStrings.add("\\[md[0-9]*_raid[0-9]\\]$");
		processStrings.add("\\[jfsCommit\\]$");
		processStrings.add("\\[jfsIO\\]$");
		processStrings.add("/sbin/lvmetad -f$");
		processStrings.add("\\[usb-storage\\]$");
		processStrings.add("\\[rpciod\\]$");
		processStrings.add("\\[i915/signal:[0-9]\\]$");
		processStrings.add("\\[kcryptd\\]$");
		processStrings.add("\\[kcryptd_io\\]$");
		processStrings.add("\\[irq/36-mei_me\\]$");
		processStrings.add("/sbin/mdadm --monitor --scan$");
		processStrings.add("\\[xfsalloc\\]$");
		processStrings.add("\\[xfs_mru_cache\\]$");
		processStrings.add("\\[jfsSync\\]$");
		processStrings.add("\\[dmcrypt_write\\]$");
		processStrings.add("\\[dio/dm-[0-9]\\]$");
		processStrings.add("\\[xprtiod\\]$");
		processStrings.add("/usr/sbin/blkmapd$");
		processStrings.add("/sbin/rpcbind -f -w$");
		processStrings.add("\\[ttm_swap\\]$");
		processStrings.add("/usr/sbin/irqbalance --foreground$");
	}

	/**
	 * Checks for unexpected processes
	 *
	 */
	public Set<IUnit> getUnits() {
		String grepString = "sudo ps -Awwo pid,user,comm,args | grep -v grep | grep -v 'ps -Awwo pid,user,comm,args$'";
		Set<IUnit> units = new HashSet<IUnit>();
		
		for (String processString : processStrings) {
			grepString += " | egrep -v \"" + processString + "\"";
		}

		grepString += " | tee /dev/stderr | grep -v 'tee /dev/stderr'"; //We want to be able to see what's running if the audit fails!
		
		units.add(new SimpleUnit("no_unexpected_processes", "proceed",
				"",
				grepString, "  PID USER     COMMAND         COMMAND", "pass",
				"There are unexpected processes running on this machine.  This could be a sign that the machine is compromised, or it could be "
				+ "entirely innocent.  Please check the processes carefully to see if there's any cause for concern.  If this machine is a metal, "
				+ "it's not unexpected for there to be processes which haven't been explicitly whitelisted in our base config."));
		
		return units;
	}

	public final void addProcess(String psString) {
		this.processStrings.add(psString);
	}

}
