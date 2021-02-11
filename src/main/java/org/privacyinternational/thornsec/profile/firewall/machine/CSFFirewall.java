/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.profile.firewall.machine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import org.privacyinternational.thornsec.core.exception.data.InvalidPortException;
import org.privacyinternational.thornsec.core.exception.runtime.ARuntimeException;
import org.privacyinternational.thornsec.core.iface.IUnit;
import org.privacyinternational.thornsec.core.model.machine.ServerModel;
import org.privacyinternational.thornsec.core.unit.SimpleUnit;
import org.privacyinternational.thornsec.core.unit.fs.FileChecksumUnit;
import org.privacyinternational.thornsec.core.unit.fs.FileChecksumUnit.Checksum;
import org.privacyinternational.thornsec.core.unit.fs.FileDownloadUnit;
import org.privacyinternational.thornsec.core.unit.fs.FileUnit;
import org.privacyinternational.thornsec.core.unit.pkg.InstalledUnit;
import inet.ipaddr.HostName;
import org.privacyinternational.thornsec.profile.firewall.AFirewallProfile;

/**
 * This is an installation of CSF Firewall
 * (https://configserver.com/cp/csf.html)
 *
 * This firewall is not designed for routers.
 */
public class CSFFirewall extends AFirewallProfile {

	private static String csfHashDigest;

	public CSFFirewall(ServerModel me) {
		super(me);

		if (getCSFHashDigest() == null) {
			try {
				final URL checksums = new URL("https://www.configserver.com/checksums.txt");
				final BufferedReader line = new BufferedReader(new InputStreamReader(checksums.openStream()));
				String inputLine;
				while ((inputLine = line.readLine()) != null) {
					final String[] netInst = inputLine.split(" ");
					if (netInst[2].equals("csf.tgz")) {
						setCSFHashDigest(netInst[1]);
						break;
					}
				}
				line.close();
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}

	protected String getCSFHashDigest() {
		return CSFFirewall.csfHashDigest;
	}

	protected void setCSFHashDigest(String digest) {
		CSFFirewall.csfHashDigest = digest;
	}

	@Override
	public Collection<IUnit> getInstalled() throws ARuntimeException {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("ca_certificates", "proceed", "ca-certificates"));
		units.add(new InstalledUnit("wget", "proceed", "wget"));

		units.add(new FileDownloadUnit("csf", "proceed", "https://download.configserver.com/csf.tgz", "/root/csf.tgz"));
		units.add(new FileChecksumUnit("csf", "csf_downloaded", Checksum.SHA256, "/root/csf.tgz", getCSFHashDigest()));

		// TODO: build ExtractUnit
		units.add(new SimpleUnit("csf_extracted", "csf_checksum", "sudo tar xzf /root/csf.tar.gz",
				"sudo [ -d /root/csf ] && echo pass || echo fail", "pass", "pass"));

		units.add(new SimpleUnit("csf_installed", "csf_extracted", "/root/csf/install.sh > /dev/null",
				"[ -d /etc/csf ] && echo pass || echo fail", "pass", "pass"));

		units.add(new SimpleUnit("iptables_modules_installed", "csf_installed", "",
				"sudo perl /usr/local/csf/bin/csftest.pl | grep RESULT:", "RESULT: csf should function on this server",
				"pass"));

		units.add(new InstalledUnit("host", "proceed", "host"));
		units.add(new InstalledUnit("ipset", "proceed", "ipset"));
		units.add(new InstalledUnit("unzip", "proceed", "unzip"));
		units.add(new InstalledUnit("msmtp_mta", "proceed", "msmtp_mta"));
		units.add(new InstalledUnit("libwww_perl", "proceed", "libwww-perl"));
		units.add(new InstalledUnit("liblwp-protocol_https_perl", "proceed", "liblwp-protocol-https-perl"));

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentConfig() throws ARuntimeException {
		final Collection<IUnit> units = new ArrayList<>();

		final FileUnit csfConf = new FileUnit("csf_config", "csf_installed", "/etc/csf/csf.conf");
		units.add(csfConf);

		csfConf.appendLine("TESTING = \\\"0\\\"");
		csfConf.appendLine("RESTRICT_SYSLOG = \\\"0\\\"");
		csfConf.appendLine("RESTRICT_SYSLOG_GROUP = \\\"restricted-syslog\\\"");
		csfConf.appendLine("RESTRICT_UI = \\\"1\\\"");
		csfConf.appendLine(
				"AUTO_UPDATES = \\\"" + (getServerModel().getAutoUpdate() ? "1" : "0") + "\\\"");
		csfConf.appendLine("LF_SPI = \\\"1\\\"");
		csfConf.appendLine("ICMP_IN = \\\"1\\\"");
		csfConf.appendLine("ICMP_IN_RATE = \\\"1/s\\\"");
		csfConf.appendLine("ICMP_OUT = \\\"1\\\"");
		csfConf.appendLine("ICMP_OUT_RATE = \\\"0\\\"");
		csfConf.appendLine("ICMP_TIMESTAMP_DROP = \\\"0\\\"");
		csfConf.appendLine("IPV6 = \\\"0\\\"");
		csfConf.appendLine("USE_CONNTRACK = \\\"1\\\"");
		csfConf.appendLine("USE_FTP_HELPER = \\\"0\\\"");
		csfConf.appendLine("SYSLOG_CHECK = \\\"600\\\"");
		csfConf.appendLine("IGNORE_ALLOW = \\\"0\\\"");
		csfConf.appendLine("DNS_STRICT = \\\"1\\\""); // TODO
		csfConf.appendLine("DENY_IP_LIMIT = \\\"200\\\"");
		csfConf.appendLine("DENY_TEMP_IP_LIMIT = \\\"100\\\"");
		csfConf.appendLine("LF_DAEMON = \\\"1\\\"");
		csfConf.appendLine("LF_CSF = \\\"1\\\"");
		csfConf.appendLine("FASTSTART = \\\"1\\\"");
		csfConf.appendLine("LF_IPSET = \\\"1\\\"");
		csfConf.appendLine("WAITLOCK = \\\"1\\\"");
		csfConf.appendLine("WAITLOCK_TIMEOUT = \\\"300\\\"");
		csfConf.appendLine("LF_IPSET_HASHSIZE = \\\"1024\\\"");
		csfConf.appendLine("LF_IPSET_MAXELEM = \\\"65535\\\"");
		csfConf.appendLine("LFD_START = \\\"0\\\"");
		csfConf.appendLine("VERBOSE = \\\"1\\\"");
		csfConf.appendLine("PACKET_FILTER = \\\"1\\\"");
		csfConf.appendLine("LF_LOOKUPS = \\\"0\\\"");
		csfConf.appendLine("STYLE_CUSTOM = \\\"0\\\"");
		csfConf.appendLine("STYLE_MOBILE = \\\"1\\\"");
		csfConf.appendLine("SMTP_BLOCK = \\\"1\\\"");
		csfConf.appendLine("SMTP_ALLOWLOCAL = \\\"1\\\"");
		csfConf.appendLine("SMTP_REDIRECT = \\\"0\\\"");
		csfConf.appendLine("SMTP_ALLOWUSER = \\\"\\\"");
		csfConf.appendLine("SMTP_ALLOWGROUP = \\\"mail,mailman\\\"");
		csfConf.appendLine("SMTPAUTH_RESTRICT = \\\"0\\\"");
		csfConf.appendLine("SYNFLOOD = \\\"0\\\"");
		csfConf.appendLine("SYNFLOOD_RATE = \\\"100/s\\\"");
		csfConf.appendLine("SYNFLOOD_BURST = \\\"150\\\"");
		csfConf.appendLine("CONNLIMIT = \\\"\\\""); // TODO
		csfConf.appendLine("PORTFLOOD = \\\"\\\""); // TODO
		csfConf.appendLine("UDPFLOOD = \\\"0\\\""); // TODO
		csfConf.appendLine("UDPFLOOD_LIMIT = \\\"100/s\\\"");
		csfConf.appendLine("UDPFLOOD_BURST = \\\"500\\\"");
		csfConf.appendLine("SYSLOG = \\\"0\\\"");
		csfConf.appendLine("DROP = \\\"DROP\\\"");
		csfConf.appendLine("DROP_OUT = \\\"REJECT\\\"");
		csfConf.appendLine("DROP_LOGGING = \\\"1\\\"");
		csfConf.appendLine("DROP_IP_LOGGING = \\\"0\\\"");
		csfConf.appendLine("DROP_OUT_LOGGING = \\\"1\\\"");
		csfConf.appendLine("DROP_UID_LOGGING = \\\"1\\\"");
		csfConf.appendLine("DROP_ONLY_RES = \\\"0\\\"");
		csfConf.appendLine("DROP_NOLOG = \\\"23,67,68,111,113,135:139,556,500,513,520\\\"");
		csfConf.appendLine("DROP_PF_LOGGING = \\\"0\\\"");
		csfConf.appendLine("CONNLIMIT_LOGGING = \\\"0\\\""); // TODO
		csfConf.appendLine("UDPFLOOD_LOGGING = \\\"1\\\"");
		csfConf.appendLine("LOGFLOOD_ALERT = \\\"1\\\"");
		csfConf.appendLine("LF_ALERT_TO = \\\"\\\"");
		csfConf.appendLine("LF_ALERT_FROM = \\\"\\\"");
		csfConf.appendLine("LF_ALERT_SMTP = \\\"\\\"");
		csfConf.appendLine("BLOCK_REPORT = \\\"\\\"");
		csfConf.appendLine("UNBLOCK_REPORT = \\\"0\\\"");
		csfConf.appendLine("X_ARF = \\\"0\\\"");
		csfConf.appendLine("X_ARF_TO = \\\"\\\"");
		csfConf.appendLine("X_ARF_ABUSE = \\\"0\\\"");
		csfConf.appendLine("LF_PERMBLOCK = \\\"0\\\""); // TODO
		csfConf.appendLine("LF_NETBLOCK = \\\"0\\\"");
		csfConf.appendLine("SAFECHAINUPDATE = \\\"1\\\"");
		csfConf.appendLine("DYNDNS = \\\"0\\\"");
		csfConf.appendLine("DYNDNS_IGNORE = \\\"0\\\"");
		csfConf.appendLine("LF_GLOBAL = \\\"0\\\"");
		csfConf.appendLine("LF_BOGON_SKIP = \\\"\\\"");
		csfConf.appendLine("URLGET = \\\"2\\\"");
		csfConf.appendLine("URLPROXY = \\\"\\\"");
		csfConf.appendLine("LF_SSH_EMAIL_ALERT = \\\"1\\\"");
		csfConf.appendLine("LF_SU_EMAIL_ALERT = \\\"1\\\"");
		csfConf.appendLine("LF_WEBMIN_EMAIL_ALERT = \\\"1\\\"");
		csfConf.appendLine("LF_CONSOLE_EMAIL_ALERT = \\\"1\\\"");
		csfConf.appendLine("LF_EXPLOIT = \\\"300\\\"");
		csfConf.appendLine("LF_INTERVAL = \\\"3600\\\"");
		csfConf.appendLine("LF_PARSE = \\\"5\\\"");
		csfConf.appendLine("LF_FLUSH = \\\"3600\\\"");
		csfConf.appendLine("LF_REPEATBLOCK = \\\"0\\\"");
		csfConf.appendLine("LF_DIRWATCH = \\\"300\\\"");
		csfConf.appendLine("LF_DIRWATCH_DISABLE = \\\"0\\\"");
		csfConf.appendLine("LF_INTEGRITY = \\\"3600\\\"");
		csfConf.appendLine("LF_DISTATTACK = \\\"0\\\""); // TODO
		csfConf.appendLine("CT_LIMIT = \\\"0\\\""); // TODO
		csfConf.appendLine("CT_SUBNET_LIMIT = \\\"0\\\""); // TODO
		csfConf.appendLine("PT_LIMIT = \\\"60\\\"");
		csfConf.appendLine("PT_INTERVAL  \\\"60\\\"");
		csfConf.appendLine("PT_DELETED = \\\"1\\\""); // TODO
		csfConf.appendLine("PT_USERPROC = \\\"10\\\"");
		csfConf.appendLine("PT_USERMEM = \\\"512\\\"");
		csfConf.appendLine("PT_USERTIME = \\\"1800\\\"");
		csfConf.appendLine("PT_USERKILL = \\\"0\\\"");
		csfConf.appendLine("PT_LOAD = \\\"30\\\"");
		csfConf.appendLine("PT_LOAD_AVG = \\\"5\\\"");
		csfConf.appendLine("PT_LOAD_LEVEL = \\\"6\\\"");
		csfConf.appendLine("PT_LOAD_SKIP = \\\"3600\\\"");
		csfConf.appendLine("PT_FORKBOMB = \\\"250\\\"");
		// TODO More settings?
		return units;
	}

	@Override
	public Collection<IUnit> getLiveConfig() throws ARuntimeException {
		return new ArrayList<>();
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws InvalidPortException {
		getServerModel().addEgress(new HostName("download.configserver.com:443"));
		return new ArrayList<>();
	}

	@Override
	public Collection<IUnit> getLiveFirewall() throws ARuntimeException {
		return new ArrayList<>();
	}

	/*
	 * private Collection<IUnit> machineIngressRules(MachineModel machine) {
	 * Collection<IUnit> units = new ArrayList<>();
	 *
	 * HashMap<String, Set<Integer>> ingress = machine.getIngressSources();
	 *
	 * for (String uri : ingress.keySet()) { InetAddress[] destinations =
	 * hostToInetAddress(uri); Integer cidr = machine.getCIDR(uri);
	 *
	 * String setName= networkModel.getIPSet().getSetName(name);
	 *
	 * networkModel.getIPSet().addToSet(setNalabel, cidr, new
	 * Vector<InetAddress>(Arrays.asList(destinations)));
	 *
	 * String rule = ""; rule += "-p tcp"; rule += (ingress.get(uri).isEmpty() ||
	 * ingress.get(uri).contains(0)) ? "" : " -m multiport --dports " +
	 * collection2String(ingress.get(uri)); rule += (uri.equals("255.255.255.255"))
	 * ? "" : " -m set --match-set " + setName+ " src"; rule += " -j ACCEPT";
	 *
	 * this.firewall.addFilter(
	 * machine.getHostnanetworkModel.getServerModel(getLabel()).) + "_" + setName +
	 * "_ingress", machine.getIngressChain(), rule, "Allow call in to " + uri ); }
	 *
	 * return units; }
	 *
	 * private Collection<IUnit> machineEgressRules(MachineModel machine) {
	 * Collection<IUnit> units = new ArrayList<>();
	 *
	 * HashMap<String, HashMap<Integer, Set<Integer>>> egress =
	 * machine.getRequiredEgress();
	 *
	 * for (String uri : egress.keySet()) { InetAddress[] destinations =
	 * hostToInetAddress(uri);
	 *
	 * String setName= networkModel.getIPSet().getSetName(uri);
	 *
	 * networkModel.getIPSet().addToSet(setNalabel, machine.getCIDR(uri), new
	 * Vector<InetAddress>(Arrays.asList(destinations)));
	 *
	 * String rule = ""; rule += "-p tcp"; rule +=
	 * (egress.get(uri).values().isEmpty() ||
	 * collection2String(egress.get(uri).values()).equals("0")) ? "" :
	 * " -m multiport --dports " + collection2String(egress.get(uri).values()); rule
	 * += (uri.equals("255.255.255.255")) ? "" : " -m set --match-set " + setName+
	 * " dst"; rule += " -j ACCEPT";
	 *
	 * this.firewall.addFilter(
	 * machine.getHostnanetworkModel.getServerModel(getLabel()).) + "_" + setName +
	 * "_egress", machine.getEgressChain(), rule, "Allow call out to " + uri ); }
	 *
	 * return units; }
	 *
	 * private Collection<IUnit> serverForwardRules(ServerModel server) {
	 * Collection<IUnit> units = new ArrayList<>();
	 *
	 * HashMap<String, Set<Integer>> forward = server.getRequiredForward();
	 *
	 * for (String destination : forward.keySet()) { MachineModel destinationMachine
	 * = networkModel.getMachineModel(destination);
	 *
	 * String request = ""; request += "-p tcp"; request += " -m tcp"; request +=
	 * " -m multiport"; request += " --sports " +
	 * collection2String(forward.get(destination)); request += " -s " +
	 * collection2String(destinationMachine.getAddresses()); request += " -d " +
	 * collection2String(server.getAddresses()); request += " -j ACCEPT";
	 *
	 * String reply = ""; reply += "-p tcp"; reply += " -m tcp"; reply +=
	 * " -m multiport"; reply += " --dports " +
	 * collection2String(forward.get(destination)); reply += " -d " +
	 * collection2String(destinationMachine.getAddresses()); reply += " -s " +
	 * collection2String(server.getAddresses()); reply += " -j ACCEPT";
	 *
	 * this.firewall.addFilter(
	 * server.getHostnanetworkModel.getServerModel(getLabel()).) + "_" +
	 * destinationMachine.getHostname() + "_forward", server.getForwardChain(),
	 * request, "Allow traffic from " + destination ); this.firewall.addFilter(
	 * destinationMachine.getHostnanetworkModel.getServerModel(getLabel()).) + "_" +
	 * server.getHostname() + "_forward", destinationMachine.getForwardChain(),
	 * request, "Allow traffic to " + destination ); this.firewall.addFilter(
	 * server.getHostnanetworkModel.getServerModel(getLabel()).) + "_" +
	 * destinationMachine.getHostname() + "_forward", server.getForwardChain(),
	 * reply, "Allow traffic from " + destination ); this.firewall.addFilter(
	 * destinationMachine.getHostnanetworkModel.getServerModel(getLabel()).) + "_" +
	 * server.getHostname() + "_forward", destinationMachine.getForwardChain(),
	 * reply, "Allow traffic to " + destination ); }
	 *
	 * return units; }
	 *
	 * private Collection<IUnit> machineDnatRules(MachineModel machine) {
	 * Collection<IUnit> units = new ArrayList<>();
	 *
	 * HashMap<String, Set<Integer>> dnat = machine.getRequiredDnat();
	 *
	 * //Only create these rules if we actually *have* users. if
	 * (!networkModel.getIPSet().isEmpty("user")) { for (String destinationName:
	 * dnat.keySet()) { MachineModel destinationMachine =
	 * networkModel.getMachineModel(destinationName;
	 *
	 * String rule = ""; rule += "-p tcp"; rule += " -m tcp"; rule +=
	 * " -m multiport"; rule += " --dports " +
	 * collection2String(dnat.get(destinationName); rule += " ! -s " +
	 * collection2String(machine.getAddresses()); rule += " -d " +
	 * collection2String(destinationMachine.getAddresses()); rule += " -j DNAT";
	 * rule += " --to-destination " + collection2String(machine.getAddresses());
	 *
	 * this.firewall.addNatPrerouting(
	 * machine.getHostnanetworkModel.getServerModel(getLabel()).) + "_" +
	 * destinationMachine.getHostname() + "_dnat", rule, "DNAT traffic for " +
	 * destinationName+ " to " + machine.getHostname() ); } }
	 *
	 * //If we've given it an external IP, it's listening, and a request
	 * conetworkModel.getServerModel(getLabel()). in from the outside world, let it
	 * have it! if (networkModel.getData().getExternalIp(machine.getLabel()) != null
	 * && !machine.getRequiredListenTCP().isEmpty()) { String rule = ""; rule +=
	 * "-i " +
	 * collection2String(networkModel.getServerModel(getLabel()).getNetworkData().
	 * getWanIfaces(getLabel())); rule += (this.isStatic) ? " -d " +
	 * networkModel.getData().getExternalIp(machine.getLabel()).getHostAddress() :
	 * ""; rule += " -p tcp"; rule += " -m multiport"; rule += " --dports " +
	 * collection2String(machine.getRequiredListenTCP()); rule += " -j DNAT"; rule
	 * += " --to-destination " + collection2String(machine.getAddresses());
	 *
	 * this.firewall.addNatPrerouting(
	 * machine.getHostnanetworkModel.getServerModel(getLabel()).) +
	 * "_external_ip_dnat", rule, "DNAT external traffic on " +
	 * networkModel.getData().getExternalIp(machine.getLabel()).getHostAddress() +
	 * " to " + machine.getHostnanetworkModel.getServerModel(getLabel()).) +
	 * " if it has an external IP & is listening" ); }
	 *
	 * return units; }
	 *
	 * private Collection<IUnit> machineAllowUserForwardRules(MachineModel machine)
	 * { Collection<IUnit> units = new ArrayList<>();
	 *
	 * Vector<Integer> listen = machine.getRequiredListenTCP(); String machineName=
	 * machine.getLabel();
	 *
	 * //Only create these rules if we actually *have* users. if
	 * (networkModel.getIPSet().isEmpty("user")) { return units; }
	 *
	 * if (machine instanceof ServerModel && listen.size() > 0) { String rule = "";
	 * rule += "-p tcp"; rule += " -m multiport"; rule += " --dports " +
	 * collection2String(listen); rule += " -m set"; rule +=
	 * " --match-set user src"; rule += " -j ACCEPT";
	 *
	 * this.firewall.addFilter( machineName+ "_users_forward",
	 * machine.getForwardChain(), rule, "Allow traffic from users" ); } else if
	 * (machine instanceof DeviceModel &&
	 * networkModel.getInternalOnlyDevices().contains(machine)) { //First, iterate
	 * through everything which should be listening for everyone String listenRule =
	 * ""; listenRule += "-p tcp"; listenRule += (!listen.isEmpty()) ?
	 * " -m multiport --dports " + collection2String(listen) : ""; listenRule +=
	 * " -m set"; listenRule += " --match-set user src"; listenRule += " -j ACCEPT";
	 *
	 * this.firewall.addFilter( machineName+ "_users_forward",
	 * machine.getForwardChain(), listenRule, "Allow traffic from users" );
	 *
	 * //These are managenetworkModel.getServerModel(getLabel()).t ports
	 * Set<Integer> ports = ((DeviceModel)
	 * machine).getManagenetworkModel.getServerModel(getLabel()).tPorts();
	 *
	 * if (ports != null && !ports.isEmpty()) { String
	 * managenetworkModel.getServerModel(getLabel()).tRule = "";
	 * managenetworkModel.getServerModel(getLabel()).tRule += "-p tcp";
	 * managenetworkModel.getServerModel(getLabel()).tRule +=
	 * " -m multiport --dports " + collection2String(ports);
	 * managenetworkModel.getServerModel(getLabel()).tRule += " -m set";
	 * managenetworkModel.getServerModel(getLabel()).tRule += " --match-set " +
	 * machineName + "_admins src";
	 * managenetworkModel.getServerModel(getLabel()).tRule += " -j ACCEPT";
	 *
	 * this.firewall.addFilter( machineName+ "_admins_management_forward",
	 * machine.getForwardChain(),
	 * managenetworkModel.getServerModel(getLabel()).tRule,
	 * "Allow managenetworkModel.getServerModel(getLabel()).t traffic from admins"
	 * ); } }
	 *
	 * return units; }
	 *
	 * private Collection<IUnit> machineIngressEgressForwardRules(MachineModel
	 * machine) { Collection<IUnit> units = new ArrayList<>();
	 *
	 * String wanIfaces =
	 * collection2String(networkModel.getData().getWanIfaces(getLabel()));
	 *
	 * String ingressRule = ""; ingressRule += "-i " + wanIfaces; ingressRule +=
	 * " -j " + machine.getIngressChain();
	 *
	 * String egressRule = ""; egressRule += "-o " + wanIfaces; egressRule += " -j "
	 * + machine.getEgressChain();
	 *
	 * this.firewall.addFilter(
	 * machine.getHostnanetworkModel.getServerModel(getLabel()).) +
	 * "_jump_on_ingress", machine.getForwardChain(), ingressRule,
	 * "Jump to our ingress chain for incoming (external) traffic" );
	 *
	 * this.firewall.addFilter(
	 * machine.getHostnanetworkModel.getServerModel(getLabel()).) +
	 * "_jump_on_egress", machine.getForwardChain(), egressRule,
	 * "Jump to our egress chain for outgoing (external) traffic" );
	 *
	 * return units; }
	 *
	 * private Collection<IUnit> userAllowServerForwardRules(DeviceModel user) {
	 * Collection<IUnit> units = new ArrayList<>();
	 *
	 * if (!networkModel.getAllServers().isEmpty()) { String rule = ""; rule +=
	 * "-m set"; rule += " --match-set servers dst"; rule += " -j ACCEPT";
	 *
	 * this.firewall.addFilter(
	 * user.getHostnanetworkModel.getServerModel(getLabel()).) + "_servers_forward",
	 * user.getForwardChain(), rule, "Allow traffic to servers" ); }
	 *
	 * return units; }
	 *
	 * private Collection<IUnit> userAllowInternalOnlyForwardRules(DeviceModel user)
	 * { Collection<IUnit> units = new ArrayList<>();
	 *
	 * if (!networkModel.getInternalOnlyDevices().isEmpty()) { String rule = "";
	 * rule += "-m set"; rule += " --match-set internalonly dst"; rule +=
	 * " -j ACCEPT";
	 *
	 * this.firewall.addFilter(
	 * user.getHostnanetworkModel.getServerModel(getLabel()).) +
	 * "_internalonly_forward", user.getForwardChain(), rule,
	 * "Allow traffic to internal-only devices" ); }
	 *
	 * return units; }
	 *
	 * private Collection<IUnit> serverAdminRules(MachineModel machine) {
	 * Collection<IUnit> units = new ArrayList<>();
	 *
	 * String machineName= machine.getLabel();
	 *
	 * //We need to check there's anything in the set, first if
	 * (networkModel.getIPSet().isEmpty(machineName+ "_admins")) { if
	 * (((ServerModel)machine).isRouter() && ((ServerModel)machine).isMetal()) {
	 * String rule = ""; rule += "-p tcp"; rule += " --dport " +
	 * networkModel.getData().getSSHPort(machineName; rule += " -j ACCEPT";
	 *
	 * this.firewall.addFilter(
	 * machine.getHostnanetworkModel.getServerModel(getLabel()).) +
	 * "_allow_admin_ssh", machine.getForwardChain(), rule, "Allow SSH from admins"
	 * ); } else { //Hmm. Should probably throw
	 * sonetworkModel.getServerModel(getLabel()).hing here } } else { String rule =
	 * ""; rule += "-p tcp"; rule += " --dport " +
	 * networkModel.getData().getSSHPort(machineName; rule += " -m set"; rule +=
	 * " --match-set " + machineName+ "_admins src"; rule += " -j ACCEPT";
	 *
	 * this.firewall.addFilter(
	 * machine.getHostnanetworkModel.getServerModel(getLabel()).) +
	 * "_allow_admin_ssh", machine.getForwardChain(), rule, "Allow SSH from admins"
	 * ); }
	 *
	 * return units; }
	 *
	 * private Collection<IUnit> networkIptUnits() { Collection<IUnit> units = new
	 * ArrayList<>();
	 *
	 * for (ServerModel server : networkModel.getAllServers()) {
	 * machineIngressRules(server); machineEgressRules(server);
	 * serverForwardRules(server); machineDnatRules(server);
	 * machineAllowUserForwardRules(server); serverAdminRules(server); }
	 *
	 * for (DeviceModel device : networkModel.getUserDevices()) {
	 *
	 * if (device.getSubnets().isEmpty()) { continue; } //Unless they don't have any
	 * interfaces
	 *
	 * //No need for further ingress rules here machineDnatRules(device);
	 * machineAllowUserForwardRules(device); userAllowServerForwardRules(device);
	 * userAllowInternalOnlyForwardRules(device); machineEgressRules(device); }
	 *
	 * for (DeviceModel device : networkModel.getInternalOnlyDevices()) { //No need
	 * for ingress or egress rules here, they only listen on fwd
	 * machineDnatRules(device); //May be behind a load balancer
	 * machineAllowUserForwardRules(device); }
	 *
	 * for (DeviceModel device : networkModel.getExternalOnlyDevices()) { //No need
	 * for forward or ingress rules here machineDnatRules(device); //May be behind a
	 * load balancer machineAllowUserForwardRules(device);
	 * machineEgressRules(device); }
	 *
	 * for (MachineModel machine : networkModel.getAllMachines()) { //Make sure to
	 * push traffic to {in,e}gress chains if (machine.getSubnets().isEmpty()) {
	 * continue; } //Unless they don't have any interfaces
	 * machineIngressEgressForwardRules(machine); }
	 *
	 * if (networkModel.getData().getAutoGuest()) { DeviceModel autoguest = new
	 * DeviceModel("autoguest", networkModel); autoguest.setCIDR(22);
	 * autoguest.setFirstOctet(10);; autoguest.setSecondOctet(250);
	 * autoguest.setThirdOctet(0);
	 *
	 * autoguest.getLanInterfaces().addIface(new InterfaceData( "autoguest", //host
	 * "lan0:9001", //iface null, //mac "static", //inet null, //bridgeports
	 * networkModel.stringToIP("10.250.0.0"), //subnet
	 * networkModel.stringToIP("10.250.0.0"), //address
	 * networkModel.stringToIP("255.255.252.0"), //netmask null, //broadcast
	 * networkModel.stringToIP("10.0.0.1"), //gateway "Auto Guest pool"
	 * //comnetworkModel.getServerModel(getLabel()).t ));
	 *
	 * baseIptConfig(autoguest);
	 *
	 * networkModel.getIPSet().addToSet("autoguest", 22,
	 * networkModel.stringToIP("10.250.0.0"));
	 *
	 * String rule = ""; rule += "-p tcp"; rule +=
	 * " -m set --match-set autoguest src"; rule += " -j ACCEPT";
	 *
	 * this.firewall.addFilter( autoguest.getEgressChain(), "autoguest_egress",
	 * rule, "Allow automatic guest pool to call out to the internet" );
	 *
	 * machineIngressEgressForwardRules(autoguest); }
	 *
	 * return units; }
	 *
	 * private Collection<IUnit> baseIptConfig(MachineModel machine) {
	 * Collection<IUnit> units = new ArrayList<>();
	 *
	 * //Do we want to be logging drops? Boolean debugMode =
	 * Boolean.parseBoolean(networkModel.getData().getProperty(getLabel(), "debug",
	 * false));
	 *
	 * //Create our egress chain for bandwidth (exfil?) tracking //In future, we
	 * could perhaps do sonetworkModel.getServerModel(getLabel()).form of traffic
	 * blocking malarky here? this.firewall.addChain(machine.getEgressChain(),
	 * "filter", machine.getEgressChain()); //Create our ingress chain for download
	 * bandwidth tracking this.firewall.addChain(machine.getIngressChain(),
	 * "filter", machine.getIngressChain()); //Create our forward chain for all
	 * other rules this.firewall.addChain(machine.getForwardChain(), "filter",
	 * machine.getForwardChain());
	 *
	 * //Force traffic to/from a given subnet to jump to our chains
	 * this.firewall.addFilterForward(machine.getHostnanetworkModel.getServerModel(
	 * getLabel()).) + "_ipt_server_src", "-s " +
	 * machine.getSubnets().elenetworkModel.getServerModel(getLabel()).tAt(0).
	 * getHostAddress() + "/" + machine.getCIDR() + " -j "+
	 * machine.getForwardChain(), "Force any internal traffic coming from " +
	 * machine.getHostnanetworkModel.getServerModel(getLabel()).) +
	 * " to its own chain");
	 * this.firewall.addFilterForward(machine.getHostnanetworkModel.getServerModel(
	 * getLabel()).) + "_ipt_server_dst", "-d " +
	 * machine.getSubnets().elenetworkModel.getServerModel(getLabel()).tAt(0).
	 * getHostAddress() + "/" + machine.getCIDR() + " -j " +
	 * machine.getForwardChain(), "Force any internal traffic going to " +
	 * machine.getHostnanetworkModel.getServerModel(getLabel()).) +
	 * " to its own chain");
	 *
	 * //We want to default drop anything not explicitly whitelisted //Make sure
	 * that these are the very first rules as the chain may have been pre-populated
	 * this.firewall.addFilter(machine.getHostnanetworkModel.getServerModel(getLabel
	 * ()).) + "_fwd_default_drop", machine.getForwardChain(), 0, "-j DROP",
	 * "Drop any internal traffic for " +
	 * machine.getHostnanetworkModel.getServerModel(getLabel()).) +
	 * " which has not already hit one of our rules");
	 *
	 * //Don't allow any traffic in from the outside world
	 * this.firewall.addFilter(machine.getHostnanetworkModel.getServerModel(getLabel
	 * ()).) + "_ingress_default_drop", machine.getIngressChain(), 0, "-j DROP",
	 * "Drop any external traffic for " +
	 * machine.getHostnanetworkModel.getServerModel(getLabel()).) +
	 * " which has not already hit one of our rules");
	 *
	 * //Don't allow any traffic out to the outside world
	 * this.firewall.addFilter(machine.getHostnanetworkModel.getServerModel(getLabel
	 * ()).) + "_egress_default_drop", machine.getEgressChain(), 0, "-j DROP",
	 * "Drop any outbound traffic from " +
	 * machine.getHostnanetworkModel.getServerModel(getLabel()).) +
	 * " which has not already hit one of our rules");
	 *
	 * //Have we set debug on? Let's do
	 * sonetworkModel.getServerModel(getLabel()).logging! if (debugMode) {
	 * this.firewall.addFilter(machine.getHostnanetworkModel.getServerModel(getLabel
	 * ()).) + "_fwd_log", machine.getForwardChain(), 1, "-j LOG --log-prefix \"" +
	 * machine.getHostnanetworkModel.getServerModel(getLabel()).) +
	 * "-forward-dropped:\"", "Log any traffic from " +
	 * machine.getHostnanetworkModel.getServerModel(getLabel()).) +
	 * " before dropping it");
	 * this.firewall.addFilter(machine.getHostnanetworkModel.getServerModel(getLabel
	 * ()).) + "_ingress_log", machine.getIngressChain(), 1,
	 * "-j LOG --log-prefix \"" +
	 * machine.getHostnanetworkModel.getServerModel(getLabel()).) +
	 * "-ingress-dropped:\"", "Log any traffic from " +
	 * machine.getHostnanetworkModel.getServerModel(getLabel()).) +
	 * " before dropping it");
	 * this.firewall.addFilter(machine.getHostnanetworkModel.getServerModel(getLabel
	 * ()).) + "_ingress_log", machine.getEgressChain(), 1, "-j LOG --log-prefix \""
	 * + machine.getHostnanetworkModel.getServerModel(getLabel()).) +
	 * "-egress-dropped:\"", "Log any traffic from " +
	 * machine.getHostnanetworkModel.getServerModel(getLabel()).) +
	 * " before dropping it"); }
	 *
	 * //Allow responses to established traffic on all chains
	 * this.firewall.addFilter(machine.getHostnanetworkModel.getServerModel(getLabel
	 * ()).) + "_allow_related_ingress_traffic_tcp", machine.getIngressChain(),
	 * "-p tcp" + " -m state --state ESTABLISHED,RELATED" + " -j ACCEPT", "Allow " +
	 * machine.getHostnanetworkModel.getServerModel(getLabel()).) +
	 * " to receive responses to accepted outbound tcp traffic");
	 * this.firewall.addFilter(machine.getHostnanetworkModel.getServerModel(getLabel
	 * ()).) + "_allow_related_ingress_traffic_udp", machine.getIngressChain(),
	 * "-p udp" + " -m state --state ESTABLISHED,RELATED" + " -j ACCEPT", "Allow " +
	 * machine.getHostnanetworkModel.getServerModel(getLabel()).) +
	 * " to receive responses to accepted outbound udp traffic");
	 * this.firewall.addFilter(machine.getHostnanetworkModel.getServerModel(getLabel
	 * ()).) + "_allow_related_fwd_traffic_tcp", machine.getForwardChain(), "-p tcp"
	 * + " -m state --state ESTABLISHED,RELATED" + " -j ACCEPT", "Allow " +
	 * machine.getHostnanetworkModel.getServerModel(getLabel()).) +
	 * " to receive responses to accepted forward tcp traffic");
	 * this.firewall.addFilter(machine.getHostnanetworkModel.getServerModel(getLabel
	 * ()).) + "_allow_related_fwd_traffic_udp", machine.getForwardChain(), "-p udp"
	 * + " -m state --state ESTABLISHED,RELATED" + " -j ACCEPT", "Allow " +
	 * machine.getHostnanetworkModel.getServerModel(getLabel()).) +
	 * " to receive responses to accepted forward udp traffic");
	 * this.firewall.addFilter(machine.getHostnanetworkModel.getServerModel(getLabel
	 * ()).) + "_allow_related_outbound_traffic_tcp", machine.getEgressChain(),
	 * "-p tcp" + " -m state --state ESTABLISHED,RELATED" + " -j ACCEPT", "Allow " +
	 * machine.getHostnanetworkModel.getServerModel(getLabel()).) +
	 * " to send responses to accepted inbound tcp traffic");
	 * this.firewall.addFilter(machine.getHostnanetworkModel.getServerModel(getLabel
	 * ()).) + "_allow_outbound_traffic_udp", machine.getEgressChain(), "-p udp" +
	 * " -j ACCEPT", "Allow " +
	 * machine.getHostnanetworkModel.getServerModel(getLabel()).) +
	 * " to send udp traffic");
	 *
	 * //Add our forward chain rules (backwards(!)) //Allow our router to talk to us
	 * this.firewall.addFilter(machine.getHostnanetworkModel.getServerModel(getLabel
	 * ()).) + "_allow_router_traffic", machine.getForwardChain(), "-s " +
	 * machine.getSubnets().elenetworkModel.getServerModel(getLabel()).tAt(0).
	 * getHostAddress() + "/30" + " -j ACCEPT", "Allow traffic between " +
	 * machine.getHostnanetworkModel.getServerModel(getLabel()).) +
	 * " and its router");
	 *
	 * return units; }
	 *
	 * private Collection<IUnit> routerScript() { Collection<IUnit> units = new
	 * ArrayList<>();
	 *
	 * String admin = ""; admin += "#!/bin/bash\n"; admin += "\n"; admin +=
	 * "RED='\\\\033[0;31m'\n"; admin += "GREEN='\\\\033[0;32m'\n"; admin +=
	 * "NC='\\\\033[0m'\n"; admin += "\n"; admin += "function checkInternets {\n";
	 * admin += "        clear\n"; admin += "\n"; admin +=
	 * "        echo \\\"Checking your internet connectivity, please wait...\\\"\n";
	 * admin += "        echo \n"; admin +=
	 * "        echo \\\"1/3 (8.8.8.8 - Google DNS)     : \\$(ping -q -w 1 -c 1 8.8.8.8 &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n"
	 * ; admin +=
	 * "        echo \\\"2/3 (208.67.222.222 - OpenDNS) : \\$(ping -q -w 1 -c 1 208.67.222.222 &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n"
	 * ; admin +=
	 * "        echo \\\"3/3 (1.1.1.1 - Cloudflare DNS) : \\$(ping -q -w 1 -c 1 1.1.1.1 &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n"
	 * ; admin += "        echo \n"; admin +=
	 * "        read -n 1 -s -r -p \\\"Press any key to return to the main networkModel.getServerModel(getLabel()).u...\\\"\n"
	 * ; admin += "}\n"; admin += "\n"; admin += "function checkDNS {\n"; admin +=
	 * "        clear\n"; admin += "\n"; admin +=
	 * "        echo \\\"Checking your external DNS server is resolving correctly\\\"\n"
	 * ; admin += "        echo \n"; admin +=
	 * "        echo \\\"Getting the DNS record for Google.com : \\$( dig +short google.com. &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n"
	 * ; admin += "        echo \n"; admin +=
	 * "        read -n 1 -s -r -p \\\"Press any key to return to the main networkModel.getServerModel(getLabel()).u...\\\"\n"
	 * ; admin += "}\n"; admin += "\n"; admin += "function restartUnbound {\n";
	 * admin += "        clear\n"; admin += "\n"; admin +=
	 * "        echo \\\"Restarting the DNS Server - please wait...\\\"\n"; admin +=
	 * "        echo \n"; admin +=
	 * "        echo \\\"Stopping DNS Server : \\$(service unbound stop &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n"
	 * ; admin +=
	 * "        echo \\\"Starting DNS Server : \\$(service unbound start &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n"
	 * ; admin += "        echo \n"; admin +=
	 * "        read -n 1 -s -r -p \\\"Press any key to return to the main networkModel.getServerModel(getLabel()).u...\\\"\n"
	 * ; admin += "}\n"; admin += "\n"; admin += "function restartDHCP {\n"; admin
	 * += "        clear\n"; admin += "\n"; admin +=
	 * "        echo \\\"Restarting the DHCP Server - please wait...\\\"\n"; admin
	 * += "        echo \n"; admin +=
	 * "        echo \\\"Stopping DHCP Server : \\$(service isc-dhcp-server stop &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n"
	 * ; admin +=
	 * "        echo \\\"Starting DHCP Server : \\$(service isc-dhcp-server start &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n"
	 * ; admin += "        echo \n"; admin +=
	 * "        read -n 1 -s -r -p \\\"Press any key to return to the main networkModel.getServerModel(getLabel()).u...\\\"\n"
	 * ; admin += "}\n"; admin += "\n"; if (this.isPPP) { admin +=
	 * "function restartPPPoE {\n"; admin += "        clear\n"; admin += "\n"; admin
	 * += "        echo \\\"Restarting the PPPoE Client - please wait...\\\"\n";
	 * admin += "        echo \n"; admin +=
	 * "        echo \\\"Stopping PPPoE Client : \\$(poff &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n"
	 * ; admin +=
	 * "        echo \\\"Starting PPPoE Client : \\$(pon &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n"
	 * ; admin += "        echo \n"; admin += "        sleep 2\n"; admin +=
	 * "        checkInternets\n"; admin += "}\n"; admin += "\n"; } admin +=
	 * "function reloadIPT {\n"; admin += "        clear\n"; admin += "\n"; admin +=
	 * "        echo \\\"Reloading the firewall - please wait...\\\"\n"; admin +=
	 * "        echo \n"; admin +=
	 * "        echo \\\"Flushing firewall rules  (1/2)  : \\$(iptables -F &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n"
	 * ; admin +=
	 * "        echo \\\"Flushing firewall rules  (2/2)  : \\$(ipset destroy &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n"
	 * ; admin +=
	 * "        echo \\\"Reloading firewall rules (2/2) : \\$(/etc/ipsets/ipsets.up.sh | ipset restore &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n"
	 * ; admin +=
	 * "        echo \\\"Reloading firewall rules (2/2) : \\$(/etc/iptables/iptables.conf.sh | iptables-restore &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n"
	 * ; admin += "        echo \n"; admin +=
	 * "        read -n 1 -s -r -p \\\"Press any key to return to the main networkModel.getServerModel(getLabel()).u...\\\"\n"
	 * ; admin += "}\n"; admin += "\n"; admin += "function tracert {\n"; admin +=
	 * "        clear\n"; admin += "\n"; admin +=
	 * "        echo \\\"Conducting a traceroute between the router and Google.com - please wait...\\\"\n"
	 * ; admin += "        echo \n"; admin += "        traceroute google.com\n";
	 * admin += "        echo \n"; admin +=
	 * "        read -n 1 -s -r -p \\\"Press any key to return to the main networkModel.getServerModel(getLabel()).u...\\\"\n"
	 * ; admin += "}\n"; admin += "\n"; if (this.isPPP) { admin +=
	 * "function configurePPPoE {\n"; admin += "        correct=\\\"false\\\"\n";
	 * admin += "        \n"; admin +=
	 * "        while [ \\\"\\${correct}\\\" = \\\"false\\\" ]; do\n"; admin +=
	 * "            clear\n"; admin += "            \n"; admin +=
	 * "            echo \\\"Enter your ISP's login usernanetworkModel.getServerModel(getLabel()).and press [ENTER]\\\"\n"
	 * ; admin +=
	 * "            read -r usernanetworkModel.getServerModel(getLabel()).n"; admin
	 * +=
	 * "            echo \\\"Enter your ISP's login password and press [ENTER]\\\"\n"
	 * ; admin += "            read -r password\n"; admin += "            \n"; admin
	 * += "            clear\n"; admin += "            \n"; admin +=
	 * "            echo -e \\\"UsernanetworkModel.getServerModel(getLabel()). \\${GREEN}\\${username}\\${NC}\\\"\n"
	 * ; admin +=
	 * "            echo -e \\\"Password: \\${GREEN}\\${password}\\${NC}\\\"\n";
	 * admin += "            \n"; admin +=
	 * "            read -r -p \\\"Are the above credentials correct? [Y/n]\\\" yn\n"
	 * ; admin += "            \n"; admin +=
	 * "            case \\\"\\${yn}\\\" in\n"; admin +=
	 * "                [nN]* ) correct=\\\"false\\\";;\n"; admin +=
	 * "                    * ) correct=\\\"true\\\";\n"; admin +=
	 * "                        printf \\\"%s      *      %s\\\" \\\"\\${usernanetworkModel.getServerModel(getLabel()).\\\" \\\"\\${password}\\\" > /etc/ppp/chap-secrets;;\n"
	 * ; admin += "			esac\n"; admin += "		done\n"; admin += "      \n";
	 * admin +=
	 * "      read -n 1 -s -r -p \\\"Press any key to return to the main networkModel.getServerModel(getLabel()).u...\\\"\n"
	 * ; admin += "\n"; admin += "}\n"; admin += "\n"; } admin +=
	 * "function speedtest {\n"; admin += "        clear\n"; admin += "\n"; admin +=
	 * "        echo \\\"Running a speed test - please wait...\\\"\n"; admin +=
	 * "        echo \n"; admin += "        speedtest-cli\n"; admin +=
	 * "        echo \n"; admin +=
	 * "        read -n 1 -s -r -p \\\"Press any key to return to the main networkModel.getServerModel(getLabel()).u...\\\"\n"
	 * ; admin += "}\n"; admin += "\n"; admin += "if [ \\\"\\${EUID}\\\" -ne 0 ]\n";
	 * admin +=
	 * "    then echo -e \\\"\\${RED}This script requires running as root.  Please sudo and try again.\\${NC}\\\"\n"
	 * ; admin += "    exit\n"; admin += "fi\n"; admin += "\n"; admin +=
	 * "while true; do\n"; admin += "        clear\n"; admin +=
	 * "        echo \\\"Choose an option:\\\"\n"; admin +=
	 * "        echo \\\"1) Check Internet Connectivity\\\"\n"; admin +=
	 * "        echo \\\"2) Check External DNS\\\"\n"; admin +=
	 * "        echo \\\"3) Restart Internal DNS Server\\\"\n"; admin +=
	 * "        echo \\\"4) Restart Internal DHCP Server\\\"\n"; admin +=
	 * "        echo \\\"5) Flush & Reload Firewall\\\"\n"; admin +=
	 * "        echo \\\"6) Traceroute\\\"\n"; if (this.isPPP) { admin +=
	 * "        echo \\\"7) Restart PPPoE (Internet) Connection\\\"\n"; admin +=
	 * "        echo \\\"C) Configure PPPoE credentials\\\"\n"; } admin +=
	 * "        echo \\\"S) Run a line speed test\\\"\n"; admin +=
	 * "        echo \\\"R) Reboot Router\\\"\n"; admin +=
	 * "        echo \\\"Q) Quit\\\"\n"; admin +=
	 * "        read -r -p \\\"Select your option: \\\" opt\n"; admin +=
	 * "        case \\\"\\${opt}\\\" in\n"; admin +=
	 * "                1   ) checkInternets;;\n"; admin +=
	 * "                2   ) checkDNS;;\n"; admin +=
	 * "                3   ) restartUnbound;;\n"; admin +=
	 * "                4   ) restartDHCP;;\n"; admin +=
	 * "                5   ) reloadIPT;;\n"; admin +=
	 * "                6   ) tracert;;\n"; if (this.isPPP) { admin +=
	 * "                7   ) restartPPPoE;;\n"; admin +=
	 * "                c|C ) configurePPPoE;;\n"; } admin +=
	 * "                s|S ) speedtest;;\n"; admin +=
	 * "                r|R ) reboot;;\n"; admin +=
	 * "                q|Q ) exit;;\n"; admin += "        esac\n"; admin += "done";
	 *
	 * units.add(new FileUnit("router_admin", "proceed", admin,
	 * "/root/routerAdmin.sh")); units.add(new FileOwnUnit("router_admin",
	 * "router_admin", "/root/routerAdmin.sh", "root")); units.add(new
	 * FilePermsUnit("router_admin", "router_admin_chowned", "/root/routerAdmin.sh",
	 * "500"));
	 *
	 * return units; }
	 *
	 * // private String cleanString(String string) { // String invalidChars =
	 * "[^a-zA-Z0-9-]"; // String safeChars = "_"; // // return
	 * string.replaceAll(invalidChars, safeChars); // }
	 *
	 * private String getAlias(String toParse) { MessageDigest digest = null;
	 *
	 * try { digest = MessageDigest.getInstance("SHA-512"); digest.reset();
	 * digest.update(toParse.getBytes("utf8")); } catch (NoSuchAlgorithmException e)
	 * { e.printStackTrace(); } catch (UnsupportedEncodingException e) {
	 * e.printStackTrace(); }
	 *
	 * String digested = String.format("%040x", new BigInteger(1, digest.digest()));
	 *
	 * return digested.replaceAll("[^0-9]", "").substring(0, 8); }
	 *
	 */

}
