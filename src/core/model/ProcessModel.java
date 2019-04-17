package core.model;

import java.util.Vector;

import core.iface.IUnit;
import core.unit.SimpleUnit;

public class ProcessModel extends AModel {

	private Vector<String> processStrings;

	ProcessModel(String label, ServerModel me, NetworkModel networkModel) {
		super(label, me, networkModel);

		this.processStrings = new Vector<String>();
		//These are processes related to TS
		processStrings.addElement("\\./script.sh; rm -rf script\\.sh; exit;");
		processStrings.addElement("/bin/bash \\./script.sh$");
		processStrings.addElement("/sbin/agetty --noclear tty[0-9] linux$");
		
		//These are default Debian/kernel processes
		processStrings.addElement("/lib/systemd/systemd --system --deserialize 14$");
		processStrings.addElement("\\[kthreadd\\]$");
		processStrings.addElement("\\[ksoftirqd/[0-9]{1,2}\\]$");
		processStrings.addElement("\\[kworker/0:0H\\]$");
		processStrings.addElement("\\[rcu_sched\\]$");
		processStrings.addElement("\\[rcu_bh\\]$");
		processStrings.addElement("\\[migration/[0-9]{1,2}\\]$");
		processStrings.addElement("\\[watchdog/[0-9]{1,2}\\]$");
		processStrings.addElement("\\[khelper\\]$");
		processStrings.addElement("\\[kdevtmpfs\\]$");
		processStrings.addElement("\\[netns\\]$");
		processStrings.addElement("\\[khungtaskd\\]$");
		processStrings.addElement("\\[writeback\\]$");
		processStrings.addElement("\\[ksmd\\]$");
		processStrings.addElement("\\[khugepaged\\]$");
		processStrings.addElement("\\[crypto\\]$");
		processStrings.addElement("\\[kintegrityd\\]$");
		processStrings.addElement("\\[bioset\\]$");
		processStrings.addElement("\\[kblockd\\]$");
		processStrings.addElement("\\[kswapd0\\]$");
		processStrings.addElement("\\[vmstat\\]$");
		processStrings.addElement("\\[fsnotify_mark\\]$");
		processStrings.addElement("\\[kthrotld\\]$");
		processStrings.addElement("\\[ipv6_addrconf\\]$");
		processStrings.addElement("\\[deferwq\\]$");
		processStrings.addElement("\\[ata_sff\\]$");
		processStrings.addElement("\\[kpsmoused\\]$");
		processStrings.addElement("\\[scsi_tmf_[0-9]{1,2}\\]$");
		processStrings.addElement("\\[scsi_eh_[0-9]{1,2}\\]$");
		processStrings.addElement("\\[kworker/[u]{0,1}[0-9]{1,2}\\:[0-9]{1,2}[H]{0,1}\\]$");
		processStrings.addElement("\\[ext4-rsv-conver\\]$");
		processStrings.addElement("\\[kauditd\\]$");
		processStrings.addElement("\\[jbd2/sda1-8\\]$");
		processStrings.addElement("/lib/systemd/systemd-journald$");
		processStrings.addElement("/lib/systemd/systemd-udevd$");
		processStrings.addElement("/sbin/dhclient -4 -v -pf /run/dhclient.enp[0-9]s[0-9].pid -lf /var/lib/dhcp/dhclient.enp[0-9]s[0-9].leases -I -df /var/lib/dhcp/dhclient6.enp[0-9]s[0-9].leases enp[0-9]s[0-9]$");
		processStrings.addElement("/usr/sbin/cron -f$");
		processStrings.addElement("/usr/sbin/rsyslogd -n$");
		processStrings.addElement("/usr/sbin/acpid$");
		processStrings.addElement("/sbin/init$");
		processStrings.addElement("\\[perf\\]$");
		processStrings.addElement("\\[jbd2/dm-[0-9]{1}-8\\]$");
		processStrings.addElement("\\[kdmflush\\]$");
		processStrings.addElement("/sbin/rdnssd -u rdnssd -H /etc/rdnssd/merge-hook$");
		processStrings.addElement("/usr/bin/dbus-daemon --system --address=systemd: --nofork --nopidfile --systemd-activation$");
		processStrings.addElement("\\[khubd\\]$");
		processStrings.addElement("\\[edac-poller\\]$");
		processStrings.addElement("\\[hd-audio[0-9]*\\]$");
		processStrings.addElement("\\[led_workqueue\\]$");
		processStrings.addElement("/lib/systemd/systemd-logind$");
		processStrings.addElement("\\[dio/dm-0\\]$");
		processStrings.addElement("/usr/sbin/sshd -D$");
		processStrings.addElement("/lib/systemd/systemd --system --deserialize [0-9]{1,2}$");
		processStrings.addElement("\\[lru-add-drain\\]$");
		processStrings.addElement("\\[cpuhp/[0-9]+\\]$");
		processStrings.addElement("\\[oom_reaper\\]$");
		processStrings.addElement("\\[kcompactd0\\]$");
		processStrings.addElement("\\[devfreq_wq\\]$");
		processStrings.addElement("\\[watchdogd\\]$");
		processStrings.addElement("/lib/systemd/systemd-timesyncd$");
		processStrings.addElement("/lib/systemd/systemd --user$");
		processStrings.addElement("\\(sd-pam\\)$");
		processStrings.addElement("\\[dio/sda[0-9]\\]$");
		processStrings.addElement("/usr/lib/packagekit/packagekitd$");
		processStrings.addElement("/usr/lib/policykit-1/polkitd --no-debug$");
		processStrings.addElement("jbd2/sdb[0-9]-[0-9]$");
		processStrings.addElement("\\[acpi_thermal_pm\\]$");
		processStrings.addElement("\\[md\\]$");
		processStrings.addElement("\\[md[0-9]_raid[0-9]\\]$");
		processStrings.addElement("\\[raid[0-9]wq\\]$");
		processStrings.addElement("\\[md[0-9]*_raid[0-9]\\]$");
		processStrings.addElement("\\[jfsCommit\\]$");
		processStrings.addElement("\\[jfsIO\\]$");
		processStrings.addElement("/sbin/lvmetad -f$");
		processStrings.addElement("\\[usb-storage\\]$");
		processStrings.addElement("\\[rpciod\\]$");
		processStrings.addElement("\\[i915/signal:[0-9]\\]$");
		processStrings.addElement("\\[kcryptd\\]$");
		processStrings.addElement("\\[kcryptd_io\\]$");
		processStrings.addElement("\\[irq/36-mei_me\\]$");
		processStrings.addElement("/sbin/mdadm --monitor --scan$");
		processStrings.addElement("\\[xfsalloc\\]$");
		processStrings.addElement("\\[xfs_mru_cache\\]$");
		processStrings.addElement("\\[jfsSync\\]$");
		processStrings.addElement("\\[dmcrypt_write\\]$");
		processStrings.addElement("\\[dio/dm-[0-9]\\]$");
		processStrings.addElement("\\[xprtiod\\]$");
		processStrings.addElement("/usr/sbin/blkmapd$");
		processStrings.addElement("/sbin/rpcbind -f -w$");
		processStrings.addElement("\\[ttm_swap\\]$");
		processStrings.addElement("/usr/sbin/irqbalance --foreground$");

		//processStrings.addElement("");
	}

	/**
	 * Checks for unexpected processes
	 *
	 */
	public Vector<IUnit> getUnits() {
		String grepString = "sudo ps -Awwo pid,user,comm,args | grep -v grep | grep -v 'ps -Awwo pid,user,comm,args$'";
		Vector<IUnit> units = new Vector<IUnit>();
				
		for (int i = 0; i < processStrings.size(); ++i) {
			grepString += " | egrep -v \"" + processStrings.elementAt(i) + "\"";
		}

		grepString += " | tee /dev/stderr | grep -v 'tee /dev/stderr'"; //We want to be able to see what's running if the audit fails!
		
		//grepString += " " + awkString;
		
		units.addElement(new SimpleUnit("no_unexpected_processes", "proceed",
				"",
				grepString, "  PID USER     COMMAND         COMMAND", "pass",
				"There are unexpected processes running on this machine.  This could be a sign that the machine is compromised, or it could be "
				+ "entirely innocent.  Please check the processes carefully to see if there's any cause for concern.  If this machine is a metal, "
				+ "it's not unexpected for there to be processes which haven't been explicitly whitelisted in our base config."));
		
		return units;
	}


	public void addProcess(String psString) {
		this.processStrings.addElement(psString);
	}

}
