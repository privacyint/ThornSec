package profile;

import java.net.InetAddress;
import java.util.Map;
import java.util.Vector;

import core.StringUtils;
import core.data.InterfaceData;
import core.iface.IUnit;
import core.model.DeviceModel;
import core.model.FirewallModel;
import core.model.NetworkModel;
import core.model.ServerModel;
import core.profile.AStructuredProfile;
import core.unit.fs.FilePermsUnit;
import core.unit.fs.FileUnit;

public class QoS extends AStructuredProfile {

	private int userMarkAfter;
	private int deviceMarkAfter;
	private int serverMarkAfter;
	private int userMark;
	private int deviceMark;
	private int serverMark;

	private int userUploadRate;
	private int extOnlyUploadRate;
	
	private String tcUnits;

	public QoS(ServerModel me, NetworkModel networkModel) {
		super("qos", me, networkModel);
		
		userMarkAfter = 20;
		userMark      = 4;
		
		deviceMarkAfter = 20;
		deviceMark      = 5;
		
		serverMarkAfter = 20;
		serverMark      = 6;

		userUploadRate = 150;
		extOnlyUploadRate = 600;
		
		tcUnits = "kbps"; //Kilobytes per second
	}

	public Vector<IUnit> getPersistentConfig() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(bandwidthThrottlingUnits());
		units.addAll(bandwidthThrottlingAlertUnits());
		
		return units;
	}

	public Vector<IUnit> getNetworking() {
		Vector<IUnit> units = new Vector<IUnit>();

		//Iterate through devicen first
		for (DeviceModel device : networkModel.getAllDevices()) {
			
			//If they're not throttled, don't bother
			if (!networkModel.getData().getDeviceIsThrottled(device.getLabel())) { continue; }
			//Or if they don't have an IP (i.e. not a network "internal" user)
			if (device.getSubnets().isEmpty()) { continue; }
			
			String deviceSubnet = device.getSubnets().elementAt(0).getHostAddress() + "/24";
			
			switch (device.getType()) {
				//Email the user only
				case "User":
					markingUnits(device.getLabel(), deviceSubnet, userMark, userMarkAfter);
			        break;
				//This is a peripheral of some sort.  Just let the responsible person know.
				case "Internal":
				case "External":
					markingUnits(device.getLabel(), deviceSubnet, deviceMark, deviceMarkAfter);
					break;
				default:
			}
		}

		for (ServerModel srv : networkModel.getAllServers()) {
			
			if (srv.isRouter()) { continue; }
			
			Vector<InterfaceData> ifaces = srv.getInterfaces();
			
			if (ifaces != null && !ifaces.isEmpty()) {
				markingUnits(srv.getLabel(), ifaces.firstElement().getSubnet().getHostAddress() + "/30", serverMark, serverMarkAfter);
			}
		}
		
		return units;
	}
	
	private Vector<IUnit> markingUnits(String name, String subnet, int mark, int markAfter) {
		Vector<IUnit> units = new Vector<IUnit>();

        FirewallModel fm = ((ServerModel)me).getFirewallModel();

		markAfter = markAfter*1024*1024; //get it in bytes

		for (Map.Entry<String, String> wanIface : networkModel.getData().getWanIfaces(me.getLabel()).entrySet() ) {
	        //Mark any connection which has uploaded > markAfter bytes
			fm.addMangleForward(StringUtils.stringToAlphaNumeric(name,  "_") + "_mark_large_uploads", 
					"-s " + subnet
					+ " -o " + wanIface.getKey()
					+ " -m connbytes --connbytes " + markAfter + ": --connbytes-dir original --connbytes-mode bytes"
					+ " -j MARK --set-mark " + mark,
					"Mark (tag) packets which are related to large uploads, so they can be treated differently");
			//Log any connection which has uploaded > markAfter bytes
	        fm.addMangleForward(StringUtils.stringToAlphaNumeric(name,  "_") + "_log_large_uploads", 
					"-s " + subnet
					+ " -o " + wanIface.getKey()
					+ " -m connbytes --connbytes " + markAfter + ": --connbytes-dir original --connbytes-mode bytes"
					+ " -m limit --limit 1/minute" //Poor, poor syslog!
					+ " -j LOG --log-prefix \\\"ipt-" + name + "-throttled: \\\"",
					"Log any large uploads, limited to once a minute");
		}
		
		return units;
	}
	
	private Vector<IUnit> bandwidthThrottlingAlertUnits() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String ommail = "";
		String emailBody = "";
		
		emailBody += "As you know, one of the key advantages afforded by operating our network through Thornsec is that it monitors for uploading traffic. ";
		emailBody += "The reason for this is that we want to try and check for any data exfiltration from our network.";
		emailBody += "\\r\\n";
		emailBody += "\\r\\n";
		emailBody += "Our router has noticed that you have uploaded more than 20mb in a single connection. ";
		emailBody += "While this may be entirely normal for you, our router is set to identify this type of activity as a potential exfiltration. ";
		emailBody += "That is, it's possible your computer has been compromised and data is being uploaded to an external server. (Just ask Hacking Team about that!)";
		emailBody += "\\r\\n";
		emailBody += "\\r\\n";
		emailBody += "Over the next little while we are going to start speed-limiting uploads which are over 20mb, and will be alerting both you and the Tech Team every time this happens. ";
		emailBody += "\\r\\n";
		emailBody += "\\r\\n";
		emailBody += "It is important to note a couple of things: as a privacy organisation, we aren't using this to snoop on what you're up to! ";
		emailBody += "However, we do have a lot of sensitive data inside our network, and we will be putting some precautions in place for large uploads. ";
		emailBody += "\\r\\n";
		emailBody += "\\r\\n";
		emailBody += "We want this to be an inclusive feedback process so that we can try and minimise false positives before starting to roll out these precautions ";
		emailBody += "- if you've received this email and aren't doing anything you perceive to be out of the ordinary, please let us know.";
		emailBody += "\\r\\n";
		emailBody += "\\r\\n";
		emailBody += "You will only receive one of these email notices per hour.";
		emailBody += "\\r\\n";
		emailBody += "\\r\\n";
		emailBody += "Thanks!";
		emailBody += "\\r\\n";
		emailBody += "Tech Team";
		
		ommail += "module(load=\\\"ommail\\\")\n";
		ommail += "\n";
		ommail += "template(name=\\\"mailBodyUser\\\" type=\\\"string\\\" string=\\\"" + emailBody + "\\\")\n";
		ommail += "template(name=\\\"mailBodyTech\\\" type=\\\"string\\\" string=\\\"%msg%\\\")\n";
		ommail += "\n";
		ommail += "if \\$msg contains \\\"throttled\\\" then {\n";

		//Iterate through devicen first
		for (DeviceModel device : networkModel.getAllDevices()) {
			
			//If they're not throttled, don't bother
			if (!networkModel.getData().getDeviceIsThrottled(device.getLabel())) { continue; }
			
			String deviceEmail  = device.getLabel() + "@" + networkModel.getData().getDomain(me.getLabel());
			String adminEmail   = networkModel.getData().getAdminEmail();
			String identifier   = device.getLabel() + "." + networkModel.getLabel();
			
			Vector<InetAddress> ips = device.getAddresses();
			
			switch (device.getType()) {
				//Email both the user && the responsible person
				case "User":
					ommail += buildThrottledEmailAction(ips, identifier, adminEmail, deviceEmail, "mailBodyUser");
					ommail += buildThrottledEmailAction(ips, identifier, deviceEmail, adminEmail, "mailBodyTech");
					break;
				//This is a peripheral of some sort.  Just let the responsible person know.
				case "Internal":
				case "External":
					ommail += buildThrottledEmailAction(ips, identifier, deviceEmail, adminEmail, "mailBodyTech");
					break;
				default:
			}
		}
		
		//Then servers
		for (ServerModel srv : networkModel.getAllServers()) {
			Vector<InetAddress> ip = new Vector<InetAddress>();
			
			if (!ip.isEmpty() && ip != null) {
				ip.add(srv.getIP());
				
				ommail += buildThrottledEmailAction(ip, srv + "." + networkModel.getLabel(), srv + "@" + networkModel.getLabel() + networkModel.getData().getDomain(srv.getLabel()), networkModel.getData().getAdminEmail(), "mailBodyTech");
			}
		}
		
		ommail += "}";
		
		units.addElement(new FileUnit("ommail_output", "proceed", ommail, "/etc/rsyslog.d/ommail.conf",
				"I couldn't output the file for firing bandwidth emails.  This means you won't be able to "
				+ "be notified of any potential exfiltration from your network."));
		
		return units;
	}

	private String buildThrottledEmailAction(Vector<InetAddress> ips, String identifier, String fromEmail, String toEmail, String bodyTemplate) {
		String action = "";

		if (!ips.isEmpty() && ips != null) {
			action += "    if \\$msg contains \\\"SRC=" + ips.elementAt(0).getHostAddress() + "\\\"";
			
			for (InetAddress ip : ips) {
				action += " or \\$msg contains \\\"SRC=" + ip.getHostAddress() + "\\\"";
			}
			
			action += " then {\n";
			action += "        action(type=\\\"ommail\\\" server=\\\"localhost\\\" port=\\\"25\\\"\n";
			action += "               mailfrom=\\\"" + fromEmail + "\\\"\n";
			action += "               mailto=[\\\"" + toEmail + "\\\"]\n";
			action += "               subject.text=\\\"[" + identifier + "] Upload bandwidth notification\\\"\n";
			action += "               action.execonlyonceeveryinterval=\\\"3600\\\"\n";
	        action += "               template=\\\"" + bodyTemplate + "\\\"\n";
	        action += "               body.enable=\\\"on\\\"\n";
			action += "        )\n";
			action += "    }\n";
		}
		
		return action;
	}
	
	private Vector<IUnit> bandwidthThrottlingUnits() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		String tcInit = "";
		tcInit += "#!/bin/bash\n";
		tcInit += "TC=/sbin/tc\n";
		tcInit += "\n";
		tcInit += "#DNLD=150Kbit # DOWNLOAD Limit\n";
		tcInit += "#DWEIGHT=15Kbit # DOWNLOAD Weight Factor ~ 1/10 of DOWNLOAD Limit\n";
		tcInit += "\n";
		tcInit += "USR_UPLD=" + userUploadRate + tcUnits + " # UPLOAD Limit for users\n";
		tcInit += "USR_UWEIGHT=" + (userUploadRate/10) + tcUnits + " # UPLOAD Weight Factor for users\n";
		tcInit += "EXT_UPLD=" + extOnlyUploadRate + tcUnits + " # UPLOAD Limit for external-only devicen\n";
		tcInit += "EXT_UWEIGHT=" + (extOnlyUploadRate/10) + tcUnits + " # UPLOAD Weight Factor for external-only devicen\n";
		tcInit += "\n";
		tcInit += "INTIFACE=lan0\n";
		tcInit += "EXTIFACE=" + networkModel.getData().getWanIfaces(me.getLabel()).keySet().toArray()[0] + "\n";
		tcInit += "\n";
		tcInit += "tc_start() {\n";

		//Ingress throttling
		tcInit += "#    \\$TC qdisc add dev \\$INTIFACE root handle 11: cbq bandwidth 1000Mbit avpkt 1000 mpu 64\n";
		tcInit += "#    \\$TC class add dev \\$INTIFACE parent 11:0 classid 11:1 cbq rate \\$DNLD weight \\$DWEIGHT allot 1514 prio 1 avpkt 1000 bounded\n";
		tcInit += "#    \\$TC filter add dev \\$INTIFACE parent 11:0 protocol ip handle 4 fw flowid 11:1\n";
		tcInit += "\n";

		//Egress throttling
		tcInit += "#    \\$TC qdisc add dev \\$EXTIFACE root handle 10: cbq bandwidth 1000Mbit avpkt 1000 mpu 64\n";
		tcInit += "#    \\$TC class add dev \\$EXTIFACE parent 10:0 classid 10:1 cbq rate \\$USR_UPLD weight \\$USR_UWEIGHT allot 1514 prio 1 avpkt 1000 bounded\n";
		tcInit += "#    \\$TC filter add dev \\$EXTIFACE parent 10:0 protocol ip handle 4 fw flowid 10:1\n";
		tcInit += "#    \\$TC class add dev \\$EXTIFACE parent 10:0 classid 10:2 cbq rate \\$EXT_UPLD weight \\$EXT_UWEIGHT allot 1514 prio 1 avpkt 1000 bounded\n";
		tcInit += "#    \\$TC filter add dev \\$EXTIFACE parent 10:0 protocol ip handle 3 fw flowid 10:2\n";
		tcInit += "}\n";
		tcInit += "\n";
		tcInit += "tc_stop() {\n";
		tcInit += "#    \\$TC qdisc del dev \\$INTIFACE root\n";
		tcInit += "#    \\$TC qdisc del dev \\$EXTIFACE root\n";
		tcInit += "}\n";
		tcInit += "\n";
		tcInit += "tc_restart() {\n";
		tcInit += "    tc_stop\n";
		tcInit += "    sleep 1\n";
		tcInit += "    tc_start\n";
		tcInit += "}\n";
		tcInit += "\n";
		tcInit += "tc_show() {\n";
		tcInit += "#    echo \\\"\\\"\n";
		tcInit += "#    echo \\\"\\$INTIFACE:\\\"\n";
		tcInit += "#    \\$TC qdisc show dev \\$INTIFACE\n";
		tcInit += "#    \\$TC class show dev \\$INTIFACE\n";
		tcInit += "#    \\$TC filter show dev \\$INTIFACE\n";
		tcInit += "#    echo \\\"\\\"\n";
		tcInit += "\n";
		tcInit += "    echo \\\"\\$EXTIFACE:\\\"\n";
		tcInit += "    \\$TC qdisc show dev \\$EXTIFACE\n";
		tcInit += "    \\$TC class show dev \\$EXTIFACE\n";
		tcInit += "    \\$TC filter show dev \\$EXTIFACE\n";
		tcInit += "    echo \\\"\\\"\n";
		tcInit += "}\n";
		tcInit += "\n";
		tcInit += "case \\\"\\$1\\\" in\n";
		tcInit += "  start)\n";
		tcInit += "    echo -n \\\"Starting bandwidth shaping: \\\"\n";
		tcInit += "    tc_start\n";
		tcInit += "    echo \\\"done\\\"\n";
		tcInit += "    ;;\n";
		tcInit += "  stop)\n";
		tcInit += "    echo -n \\\"Stopping bandwidth shaping: \\\"\n";
		tcInit += "    tc_stop\n";
		tcInit += "    echo \\\"done\\\"\n";
		tcInit += "    ;;\n";
		tcInit += "  restart)\n";
		tcInit += "    echo -n \\\"Restarting bandwidth shaping: \\\"\n";
		tcInit += "    tc_restart\n";
		tcInit += "    echo \\\"done\\\"\n";
		tcInit += "    ;;\n";
		tcInit += "  *)\n";
		tcInit += "    echo \\\"Usage: /etc/init.d/tc.sh {start|stop|restart|show}\\\"\n";
		tcInit += "    ;;\n";
		tcInit += "esac\n";
		tcInit += "exit 0";
		
		units.addElement(new FileUnit("tc_init_script_created", "proceed", tcInit, "/etc/init.d/tc.sh",
				"I couldn't output the file for starting bandwidth shaping.  This means you won't be able to "
				+ "control any potential exfiltration from your network."));
		units.addElement(new FilePermsUnit("tc_init_script_perms", "tc_init_script_created", "/etc/init.d/tc.sh", "755"));

		return units;
	}
	
	public Vector<IUnit> getInstalled() { 
		return new Vector<IUnit>();
	}
	
	public Vector<IUnit> getLiveConfig() { 
		return new Vector<IUnit>();
	}

}
