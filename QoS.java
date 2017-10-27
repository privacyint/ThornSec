package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.FirewallModel;
import core.model.NetworkModel;
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

	public QoS() {
		super("qos");
		
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

	public Vector<IUnit> getPersistentConfig(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addAll(bandwidthThrottlingUnits(server, model));
		units.addAll(bandwidthThrottlingAlertUnits(server, model));
		
		return units;
	}

	protected Vector<IUnit> getPersistentFirewall(String server, NetworkModel model) {
		Vector<IUnit> units = new Vector<IUnit>();

		//Iterate through devicen first
		for (String device : model.getDeviceLabels()) {
			
			//If they're not throttled, don't bother
			if (!model.getData().getDeviceThrottled(device)) { continue; }
			
			String deviceSubnet = model.getDeviceModel(device).getSubnets()[0] + "/24";
			
			switch (model.getDeviceModel(device).getType()) {
				//Email the user only
				case "user":
				case "superuser":
					markingUnits(server, model, device, deviceSubnet, userMark, userMarkAfter);
			        break;
				//This is a peripheral of some sort.  Just let the responsible person know.
				case "intonly":
				case "extonly":
					markingUnits(server, model, device, deviceSubnet, deviceMark, deviceMarkAfter);
					break;
				default:
			}
		}

		for (String srv : model.getServerLabels()) {
			markingUnits(server, model, srv, model.getServerModel(srv).getIP(), serverMark, serverMarkAfter);
		}
		
		return units;
	}
	
	private Vector<IUnit> markingUnits(String server, NetworkModel model, String name, String subnet, int mark, int markAfter) {
		Vector<IUnit> units = new Vector<IUnit>();

        FirewallModel fm = model.getServerModel(server).getFirewallModel();

        String extIface = model.getData().getExtIface(server);

		markAfter = markAfter*1024*1024; //get it in bytes

        //Mark any connection which has uploaded > markAfter bytes
		units.addElement(fm.addMangleForward(name.replaceAll("-",  "_") + "_mark_large_uploads", 
				"-s " + subnet
				+ " -o " + extIface
				+ " -m connbytes --connbytes " + markAfter + ": --connbytes-dir both --connbytes-mode bytes"
				+ " -j MARK --set-mark " + mark));
		//Log any connection which has uploaded > markAfter bytes
        units.addElement(fm.addMangleForward(name.replaceAll("-",  "_") + "_log_large_uploads", 
				"-s " + subnet
				+ " -o " + extIface
				+ " -m connbytes --connbytes " + markAfter + ": --connbytes-dir both --connbytes-mode bytes"
				+ " -m limit --limit 1/minute" //Poor, poor syslog!
				+ " -j LOG --log-prefix \\\"ipt-" + name + "-throttled: \\\""));
		
		return units;
	}
	
	private Vector<IUnit> bandwidthThrottlingAlertUnits(String server, NetworkModel model) {
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
		for (String device : model.getDeviceLabels()) {
			
			//If they're not throttled, don't bother
			if (!model.getData().getDeviceThrottled(device)) { continue; }
			
			String deviceEmail  = device + "@" + model.getData().getDomain(server);
			String adminEmail = model.getData().getAdminEmail();
			String identifier = device + "." + model.getLabel();
			
			String[] ips = model.getDeviceModel(device).getIPs();
			
			switch (model.getDeviceModel(device).getType()) {
				//Email both the user && the responsible person
				case "user":
				case "superuser":
					ommail += buildThrottledEmailAction(ips, identifier, adminEmail, deviceEmail, "mailBodyUser");
					ommail += buildThrottledEmailAction(ips, identifier, deviceEmail, adminEmail, "mailBodyTech");
					break;
				//This is a peripheral of some sort.  Just let the responsible person know.
				case "intonly":
				case "extonly":
					ommail += buildThrottledEmailAction(ips, identifier, deviceEmail, adminEmail, "mailBodyTech");
					break;
				default:
					//It'll default drop.
			}
		}
		
		//Then servers
		for (String srv : model.getServerLabels()) {
			String[] ip = new String[1];
			ip[0] = model.getServerModel(srv).getIP();
			
			ommail += buildThrottledEmailAction(ip, srv + "." + model.getLabel(), srv + "@" + model.getLabel() + model.getData().getDomain(srv), model.getData().getAdminEmail(), "mailBodyTech");
		}
		
		ommail += "}";
		
		units.addElement(new FileUnit("ommail_output", "proceed", ommail, "/etc/rsyslog.d/ommail.conf",
				"I couldn't output the file for firing bandwidth emails.  This means you won't be able to "
				+ "be notified of any potential exfiltration from your network."));
		
		return units;
	}

	private String buildThrottledEmailAction(String[] ips, String identifier, String fromEmail, String toEmail, String bodyTemplate) {
		String action = "";
		action += "    if \\$msg contains \\\"SRC=" + ips[0] + "\\\"";
		
		for (int i = 1; i < ips.length; ++i) {
			action += " or \\$msg contains \\\"SRC=" + ips[i] + "\\\"";
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
		
		return action;
	}
	
	private Vector<IUnit> bandwidthThrottlingUnits(String server, NetworkModel model) {
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
		tcInit += "INTIFACE=" + model.getData().getIface(server) + "\n";
		tcInit += "EXTIFACE=" + model.getData().getExtIface(server) + "\n";
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
	
}