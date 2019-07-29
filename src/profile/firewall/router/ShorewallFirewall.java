/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.firewall.router;

import java.util.LinkedHashSet;
import java.util.Set;

import core.exception.runtime.ARuntimeException;
import core.iface.IUnit;
import core.model.network.NetworkModel;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import profile.firewall.AFirewallProfile;

public class ShorewallFirewall extends AFirewallProfile {

	public ShorewallFirewall(String label, NetworkModel networkModel) {
		super(label, networkModel);
	}

	@Override
	public Set<IUnit> getInstalled() throws ARuntimeException {
		final Set<IUnit> units = new LinkedHashSet<>();

		units.add(new InstalledUnit("shorewall", "proceed", "shorewall"));

		return units;
	}

	@Override
	public Set<IUnit> getPersistentConfig() throws ARuntimeException {
		final Set<IUnit> units = new LinkedHashSet<>();

		// Build our default policies
		final FileUnit policies = new FileUnit("shorewall_policies", "shorewall_installed", "/etc/shorewall/policy");
		policies.appendLine("#Default policies to use");
		policies.appendLine("#For specific rules, please look at /etc/shorewall/rules file");
		policies.appendLine("#source destination action");
		policies.appendLine("wan all DROP"); // DROP all ingress traffic
		policies.appendLine("fw all REJECT"); // REJECT all traffic
		policies.appendLine("servers all REJECT"); // REJECT all traffic
		policies.appendLine("users all REJECT"); // REJECT all traffic
		policies.appendLine("admins all REJECT"); // REJECT all traffic
		policies.appendLine("internalOnlys all REJECT"); // REJECT all traffic
		policies.appendLine("externalOnlys all REJECT"); // REJECT all traffic
		units.add(policies);

		return units;
	}

	@Override
	public Set<IUnit> getLiveConfig() throws ARuntimeException {
		final Set<IUnit> units = new LinkedHashSet<>();

		// Build our zones
		final FileUnit zones = new FileUnit("shorewall_zones", "shorewall_installed", "/etc/shorewall/zones");
		zones.appendLine("#This is the file which creates our various zones");
		zones.appendLine("#zone type");
		zones.appendLine("#Please see http://shorewall.net/manpages/shorewall-zones.html for more details");
		zones.appendLine("fw firewall");
		zones.appendLine("wan ipv4");
		zones.appendLine("servers ipv4");
		for (final String serverLabel : getNetworkModel().getServers().keySet()) {
			zones.appendLine("servers:" + serverLabel + " ipv4");
		}
		zones.appendLine("users ipv4");
		for (final String userLabel : getNetworkModel().getUserDevices().keySet()) {
			zones.appendLine("");
		}
		zones.appendLine("admins:users ipv4");
		zones.appendLine("internalOnlys ipv4");
		zones.appendLine("externalOnlys ipv4");
		if (this.networkModel.getData().buildAutoGuest()) {
			zones.appendLine("autoguest ipv4");
		}
		units.add(zones);

		// Dedicate interfaces to zones
		final FileUnit interfaces = new FileUnit("shorewall_interfaces", "shorewall_installed",
				"/etc/shorewall/interfaces");
		interfaces.appendLine("#Dedicate interfaces to zones");
		interfaces.appendLine("#zone interface options");
		interfaces.appendLine("wan " + this.networkModel.getServerModel(getLabel())); // TODO
		interfaces.appendLine("servers servers detect tcpflags,nosmurfs,routefiulter,logmartians");

		for (final String server : this.networkModel.getServers().keySet()) {
			interfaces.appendLine("servers:" + server);
		}

		interfaces.appendLine("users users detect dhcp,tcpflags,nosmurfs,routefiulter,logmartians");
		interfaces.appendLine("admins:users users detect tcpflags,nosmurfs,routefiulter,logmartians");
		interfaces.appendLine("internalOnlys internalOnlys detect dhcp,tcpflags,nosmurfs,routefiulter,logmartians");
		interfaces.appendLine("externalOnlys externalOnlys detect dhcp,tcpflags,nosmurfs,routefiulter,logmartians");
		if (this.networkModel.getData().buildAutoGuest()) {
			interfaces
					.appendLine("autoguest:externalOnlys autoguest detect tcpflags,nosmurfs,routefiulter,logmartians");
		}
		units.add(interfaces);

		return units;
	}

	@Override
	public Set<IUnit> getPersistentFirewall() throws ARuntimeException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<IUnit> getLiveFirewall() throws ARuntimeException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * private Set<IUnit> machineIngressRules(MachineModel machine) { Set<IUnit>
	 * units = new HashSet<IUnit>();
	 *
	 * HashMap<String, Set<Integer>> ingress = machine.getIngressSources();
	 *
	 * for (String uri : ingress.keySet()) { InetAddress[] destinations =
	 * hostToInetAddress(uri); Integer cidr = machine.getCIDR(uri);
	 *
	 * String setName = networkModel.getIPSet().getSetName(name);
	 *
	 * networkModel.getIPSet().addToSet(setName, cidr, new
	 * Vector<InetAddress>(Arrays.asList(destinations)));
	 *
	 * String rule = ""; rule += "-p tcp"; rule += (ingress.get(uri).isEmpty() ||
	 * ingress.get(uri).contains(0)) ? "" : " -m multiport --dports " +
	 * collection2String(ingress.get(uri)); rule += (uri.equals("255.255.255.255"))
	 * ? "" : " -m set --match-set " + setName + " src"; rule += " -j ACCEPT";
	 *
	 * this.firewall.addFilter( machine.getHostname() + "_" + setName + "_ingress",
	 * machine.getIngressChain(), rule, "Allow call in to " + uri ); }
	 *
	 * return units; }
	 *
	 * private Set<IUnit> machineEgressRules(MachineModel machine) { Set<IUnit>
	 * units = new HashSet<IUnit>();
	 *
	 * HashMap<String, HashMap<Integer, Set<Integer>>> egress =
	 * machine.getRequiredEgress();
	 *
	 * for (String uri : egress.keySet()) { InetAddress[] destinations =
	 * hostToInetAddress(uri);
	 *
	 * String setName = networkModel.getIPSet().getSetName(uri);
	 *
	 * networkModel.getIPSet().addToSet(setName, machine.getCIDR(uri), new
	 * Vector<InetAddress>(Arrays.asList(destinations)));
	 *
	 * String rule = ""; rule += "-p tcp"; rule +=
	 * (egress.get(uri).values().isEmpty() ||
	 * collection2String(egress.get(uri).values()).equals("0")) ? "" :
	 * " -m multiport --dports " + collection2String(egress.get(uri).values()); rule
	 * += (uri.equals("255.255.255.255")) ? "" : " -m set --match-set " + setName +
	 * " dst"; rule += " -j ACCEPT";
	 *
	 * this.firewall.addFilter( machine.getHostname() + "_" + setName + "_egress",
	 * machine.getEgressChain(), rule, "Allow call out to " + uri ); }
	 *
	 * return units; }
	 *
	 * private Set<IUnit> serverForwardRules(ServerModel server) { Set<IUnit> units
	 * = new HashSet<IUnit>();
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
	 * this.firewall.addFilter( server.getHostname() + "_" +
	 * destinationMachine.getHostname() + "_forward", server.getForwardChain(),
	 * request, "Allow traffic from " + destination ); this.firewall.addFilter(
	 * destinationMachine.getHostname() + "_" + server.getHostname() + "_forward",
	 * destinationMachine.getForwardChain(), request, "Allow traffic to " +
	 * destination ); this.firewall.addFilter( server.getHostname() + "_" +
	 * destinationMachine.getHostname() + "_forward", server.getForwardChain(),
	 * reply, "Allow traffic from " + destination ); this.firewall.addFilter(
	 * destinationMachine.getHostname() + "_" + server.getHostname() + "_forward",
	 * destinationMachine.getForwardChain(), reply, "Allow traffic to " +
	 * destination ); }
	 *
	 * return units; }
	 *
	 * private Set<IUnit> machineDnatRules(MachineModel machine) { Set<IUnit> units
	 * = new HashSet<IUnit>();
	 *
	 * HashMap<String, Set<Integer>> dnat = machine.getRequiredDnat();
	 *
	 * //Only create these rules if we actually *have* users. if
	 * (!networkModel.getIPSet().isEmpty("user")) { for (String destinationName :
	 * dnat.keySet()) { MachineModel destinationMachine =
	 * networkModel.getMachineModel(destinationName);
	 *
	 * String rule = ""; rule += "-p tcp"; rule += " -m tcp"; rule +=
	 * " -m multiport"; rule += " --dports " +
	 * collection2String(dnat.get(destinationName)); rule += " ! -s " +
	 * collection2String(machine.getAddresses()); rule += " -d " +
	 * collection2String(destinationMachine.getAddresses()); rule += " -j DNAT";
	 * rule += " --to-destination " + collection2String(machine.getAddresses());
	 *
	 * this.firewall.addNatPrerouting( machine.getHostname() + "_" +
	 * destinationMachine.getHostname() + "_dnat", rule, "DNAT traffic for " +
	 * destinationName + " to " + machine.getHostname() ); } }
	 *
	 * //If we've given it an external IP, it's listening, and a request comes in
	 * from the outside world, let it have it! if
	 * (networkModel.getData().getExternalIp(machine.getLabel()) != null &&
	 * !machine.getRequiredListenTCP().isEmpty()) { String rule = ""; rule += "-i "
	 * + collection2String(me.getNetworkData().getWanIfaces(getLabel())); rule +=
	 * (this.isStatic) ? " -d " +
	 * networkModel.getData().getExternalIp(machine.getLabel()).getHostAddress() :
	 * ""; rule += " -p tcp"; rule += " -m multiport"; rule += " --dports " +
	 * collection2String(machine.getRequiredListenTCP()); rule += " -j DNAT"; rule
	 * += " --to-destination " + collection2String(machine.getAddresses());
	 *
	 * this.firewall.addNatPrerouting( machine.getHostname() + "_external_ip_dnat",
	 * rule, "DNAT external traffic on " +
	 * networkModel.getData().getExternalIp(machine.getLabel()).getHostAddress() +
	 * " to " + machine.getHostname() + " if it has an external IP & is listening"
	 * ); }
	 *
	 * return units; }
	 *
	 * private Set<IUnit> machineAllowUserForwardRules(MachineModel machine) {
	 * Set<IUnit> units = new HashSet<IUnit>();
	 *
	 * Vector<Integer> listen = machine.getRequiredListenTCP(); String machineName =
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
	 * this.firewall.addFilter( machineName + "_users_forward",
	 * machine.getForwardChain(), rule, "Allow traffic from users" ); } else if
	 * (machine instanceof DeviceModel &&
	 * networkModel.getInternalOnlyDevices().contains(machine)) { //First, iterate
	 * through everything which should be listening for everyone String listenRule =
	 * ""; listenRule += "-p tcp"; listenRule += (!listen.isEmpty()) ?
	 * " -m multiport --dports " + collection2String(listen) : ""; listenRule +=
	 * " -m set"; listenRule += " --match-set user src"; listenRule += " -j ACCEPT";
	 *
	 * this.firewall.addFilter( machineName + "_users_forward",
	 * machine.getForwardChain(), listenRule, "Allow traffic from users" );
	 *
	 * //These are management ports Set<Integer> ports = ((DeviceModel)
	 * machine).getManagementPorts();
	 *
	 * if (ports != null && !ports.isEmpty()) { String managementRule = "";
	 * managementRule += "-p tcp"; managementRule += " -m multiport --dports " +
	 * collection2String(ports); managementRule += " -m set"; managementRule +=
	 * " --match-set " + machineName + "_admins src"; managementRule +=
	 * " -j ACCEPT";
	 *
	 * this.firewall.addFilter( machineName + "_admins_management_forward",
	 * machine.getForwardChain(), managementRule,
	 * "Allow management traffic from admins" ); } }
	 *
	 * return units; }
	 *
	 * private Set<IUnit> machineIngressEgressForwardRules(MachineModel machine) {
	 * Set<IUnit> units = new HashSet<IUnit>();
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
	 * this.firewall.addFilter( machine.getHostname() + "_jump_on_ingress",
	 * machine.getForwardChain(), ingressRule,
	 * "Jump to our ingress chain for incoming (external) traffic" );
	 *
	 * this.firewall.addFilter( machine.getHostname() + "_jump_on_egress",
	 * machine.getForwardChain(), egressRule,
	 * "Jump to our egress chain for outgoing (external) traffic" );
	 *
	 * return units; }
	 *
	 * private Set<IUnit> userAllowServerForwardRules(DeviceModel user) { Set<IUnit>
	 * units = new HashSet<IUnit>();
	 *
	 * if (!networkModel.getAllServers().isEmpty()) { String rule = ""; rule +=
	 * "-m set"; rule += " --match-set servers dst"; rule += " -j ACCEPT";
	 *
	 * this.firewall.addFilter( user.getHostname() + "_servers_forward",
	 * user.getForwardChain(), rule, "Allow traffic to servers" ); }
	 *
	 * return units; }
	 *
	 * private Set<IUnit> userAllowInternalOnlyForwardRules(DeviceModel user) {
	 * Set<IUnit> units = new HashSet<IUnit>();
	 *
	 * if (!networkModel.getInternalOnlyDevices().isEmpty()) { String rule = "";
	 * rule += "-m set"; rule += " --match-set internalonly dst"; rule +=
	 * " -j ACCEPT";
	 *
	 * this.firewall.addFilter( user.getHostname() + "_internalonly_forward",
	 * user.getForwardChain(), rule, "Allow traffic to internal-only devices" ); }
	 *
	 * return units; }
	 *
	 * private Set<IUnit> serverAdminRules(MachineModel machine) { Set<IUnit> units
	 * = new HashSet<IUnit>();
	 *
	 * String machineName = machine.getLabel();
	 *
	 * //We need to check there's anything in the set, first if
	 * (networkModel.getIPSet().isEmpty(machineName + "_admins")) { if
	 * (((ServerModel)machine).isRouter() && ((ServerModel)machine).isMetal()) {
	 * String rule = ""; rule += "-p tcp"; rule += " --dport " +
	 * networkModel.getData().getSSHPort(machineName); rule += " -j ACCEPT";
	 *
	 * this.firewall.addFilter( machine.getHostname() + "_allow_admin_ssh",
	 * machine.getForwardChain(), rule, "Allow SSH from admins" ); } else { //Hmm.
	 * Should probably throw something here } } else { String rule = ""; rule +=
	 * "-p tcp"; rule += " --dport " +
	 * networkModel.getData().getSSHPort(machineName); rule += " -m set"; rule +=
	 * " --match-set " + machineName + "_admins src"; rule += " -j ACCEPT";
	 *
	 * this.firewall.addFilter( machine.getHostname() + "_allow_admin_ssh",
	 * machine.getForwardChain(), rule, "Allow SSH from admins" ); }
	 *
	 * return units; }
	 *
	 * private Set<IUnit> networkIptUnits() { Set<IUnit> units = new
	 * HashSet<IUnit>();
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
	 * networkModel.stringToIP("10.0.0.1"), //gateway "Auto Guest pool" //comment
	 * ));
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
	 * private Set<IUnit> baseIptConfig(MachineModel machine) { Set<IUnit> units =
	 * new HashSet<IUnit>();
	 *
	 * //Do we want to be logging drops? Boolean debugMode =
	 * Boolean.parseBoolean(networkModel.getData().getProperty(getLabel(), "debug",
	 * false));
	 *
	 * //Create our egress chain for bandwidth (exfil?) tracking //In future, we
	 * could perhaps do some form of traffic blocking malarky here?
	 * this.firewall.addChain(machine.getEgressChain(), "filter",
	 * machine.getEgressChain()); //Create our ingress chain for download bandwidth
	 * tracking this.firewall.addChain(machine.getIngressChain(), "filter",
	 * machine.getIngressChain()); //Create our forward chain for all other rules
	 * this.firewall.addChain(machine.getForwardChain(), "filter",
	 * machine.getForwardChain());
	 *
	 * //Force traffic to/from a given subnet to jump to our chains
	 * this.firewall.addFilterForward(machine.getHostname() + "_ipt_server_src",
	 * "-s " + machine.getSubnets().elementAt(0).getHostAddress() + "/" +
	 * machine.getCIDR() + " -j "+ machine.getForwardChain(),
	 * "Force any internal traffic coming from " + machine.getHostname() +
	 * " to its own chain"); this.firewall.addFilterForward(machine.getHostname() +
	 * "_ipt_server_dst", "-d " + machine.getSubnets().elementAt(0).getHostAddress()
	 * + "/" + machine.getCIDR() + " -j " + machine.getForwardChain(),
	 * "Force any internal traffic going to " + machine.getHostname() +
	 * " to its own chain");
	 *
	 * //We want to default drop anything not explicitly whitelisted //Make sure
	 * that these are the very first rules as the chain may have been pre-populated
	 * this.firewall.addFilter(machine.getHostname() + "_fwd_default_drop",
	 * machine.getForwardChain(), 0, "-j DROP", "Drop any internal traffic for " +
	 * machine.getHostname() + " which has not already hit one of our rules");
	 *
	 * //Don't allow any traffic in from the outside world
	 * this.firewall.addFilter(machine.getHostname() + "_ingress_default_drop",
	 * machine.getIngressChain(), 0, "-j DROP", "Drop any external traffic for " +
	 * machine.getHostname() + " which has not already hit one of our rules");
	 *
	 * //Don't allow any traffic out to the outside world
	 * this.firewall.addFilter(machine.getHostname() + "_egress_default_drop",
	 * machine.getEgressChain(), 0, "-j DROP", "Drop any outbound traffic from " +
	 * machine.getHostname() + " which has not already hit one of our rules");
	 *
	 * //Have we set debug on? Let's do some logging! if (debugMode) {
	 * this.firewall.addFilter(machine.getHostname() + "_fwd_log",
	 * machine.getForwardChain(), 1, "-j LOG --log-prefix \"" +
	 * machine.getHostname() + "-forward-dropped:\"", "Log any traffic from " +
	 * machine.getHostname() + " before dropping it");
	 * this.firewall.addFilter(machine.getHostname() + "_ingress_log",
	 * machine.getIngressChain(), 1, "-j LOG --log-prefix \"" +
	 * machine.getHostname() + "-ingress-dropped:\"", "Log any traffic from " +
	 * machine.getHostname() + " before dropping it");
	 * this.firewall.addFilter(machine.getHostname() + "_ingress_log",
	 * machine.getEgressChain(), 1, "-j LOG --log-prefix \"" + machine.getHostname()
	 * + "-egress-dropped:\"", "Log any traffic from " + machine.getHostname() +
	 * " before dropping it"); }
	 *
	 * //Allow responses to established traffic on all chains
	 * this.firewall.addFilter(machine.getHostname() +
	 * "_allow_related_ingress_traffic_tcp", machine.getIngressChain(), "-p tcp" +
	 * " -m state --state ESTABLISHED,RELATED" + " -j ACCEPT", "Allow " +
	 * machine.getHostname() +
	 * " to receive responses to accepted outbound tcp traffic");
	 * this.firewall.addFilter(machine.getHostname() +
	 * "_allow_related_ingress_traffic_udp", machine.getIngressChain(), "-p udp" +
	 * " -m state --state ESTABLISHED,RELATED" + " -j ACCEPT", "Allow " +
	 * machine.getHostname() +
	 * " to receive responses to accepted outbound udp traffic");
	 * this.firewall.addFilter(machine.getHostname() +
	 * "_allow_related_fwd_traffic_tcp", machine.getForwardChain(), "-p tcp" +
	 * " -m state --state ESTABLISHED,RELATED" + " -j ACCEPT", "Allow " +
	 * machine.getHostname() +
	 * " to receive responses to accepted forward tcp traffic");
	 * this.firewall.addFilter(machine.getHostname() +
	 * "_allow_related_fwd_traffic_udp", machine.getForwardChain(), "-p udp" +
	 * " -m state --state ESTABLISHED,RELATED" + " -j ACCEPT", "Allow " +
	 * machine.getHostname() +
	 * " to receive responses to accepted forward udp traffic");
	 * this.firewall.addFilter(machine.getHostname() +
	 * "_allow_related_outbound_traffic_tcp", machine.getEgressChain(), "-p tcp" +
	 * " -m state --state ESTABLISHED,RELATED" + " -j ACCEPT", "Allow " +
	 * machine.getHostname() +
	 * " to send responses to accepted inbound tcp traffic");
	 * this.firewall.addFilter(machine.getHostname() +
	 * "_allow_outbound_traffic_udp", machine.getEgressChain(), "-p udp" +
	 * " -j ACCEPT", "Allow " + machine.getHostname() + " to send udp traffic");
	 *
	 * //Add our forward chain rules (backwards(!)) //Allow our router to talk to us
	 * this.firewall.addFilter(machine.getHostname() + "_allow_router_traffic",
	 * machine.getForwardChain(), "-s " +
	 * machine.getSubnets().elementAt(0).getHostAddress() + "/30" + " -j ACCEPT",
	 * "Allow traffic between " + machine.getHostname() + " and its router");
	 *
	 * return units; }
	 *
	 * private Set<IUnit> routerScript() { Set<IUnit> units = new HashSet<IUnit>();
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
	 * "        read -n 1 -s -r -p \\\"Press any key to return to the main menu...\\\"\n"
	 * ; admin += "}\n"; admin += "\n"; admin += "function checkDNS {\n"; admin +=
	 * "        clear\n"; admin += "\n"; admin +=
	 * "        echo \\\"Checking your external DNS server is resolving correctly\\\"\n"
	 * ; admin += "        echo \n"; admin +=
	 * "        echo \\\"Getting the DNS record for Google.com : \\$( dig +short google.com. &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n"
	 * ; admin += "        echo \n"; admin +=
	 * "        read -n 1 -s -r -p \\\"Press any key to return to the main menu...\\\"\n"
	 * ; admin += "}\n"; admin += "\n"; admin += "function restartUnbound {\n";
	 * admin += "        clear\n"; admin += "\n"; admin +=
	 * "        echo \\\"Restarting the DNS Server - please wait...\\\"\n"; admin +=
	 * "        echo \n"; admin +=
	 * "        echo \\\"Stopping DNS Server : \\$(service unbound stop &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n"
	 * ; admin +=
	 * "        echo \\\"Starting DNS Server : \\$(service unbound start &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n"
	 * ; admin += "        echo \n"; admin +=
	 * "        read -n 1 -s -r -p \\\"Press any key to return to the main menu...\\\"\n"
	 * ; admin += "}\n"; admin += "\n"; admin += "function restartDHCP {\n"; admin
	 * += "        clear\n"; admin += "\n"; admin +=
	 * "        echo \\\"Restarting the DHCP Server - please wait...\\\"\n"; admin
	 * += "        echo \n"; admin +=
	 * "        echo \\\"Stopping DHCP Server : \\$(service isc-dhcp-server stop &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n"
	 * ; admin +=
	 * "        echo \\\"Starting DHCP Server : \\$(service isc-dhcp-server start &> /dev/null && echo -e \\\"\\${GREEN}\\\"OK!\\\"\\${NC}\\\" || echo -e \\\"\\${RED}\\\"ERROR\\\"\\${NC}\\\")\\\"\n"
	 * ; admin += "        echo \n"; admin +=
	 * "        read -n 1 -s -r -p \\\"Press any key to return to the main menu...\\\"\n"
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
	 * "        read -n 1 -s -r -p \\\"Press any key to return to the main menu...\\\"\n"
	 * ; admin += "}\n"; admin += "\n"; admin += "function tracert {\n"; admin +=
	 * "        clear\n"; admin += "\n"; admin +=
	 * "        echo \\\"Conducting a traceroute between the router and Google.com - please wait...\\\"\n"
	 * ; admin += "        echo \n"; admin += "        traceroute google.com\n";
	 * admin += "        echo \n"; admin +=
	 * "        read -n 1 -s -r -p \\\"Press any key to return to the main menu...\\\"\n"
	 * ; admin += "}\n"; admin += "\n"; if (this.isPPP) { admin +=
	 * "function configurePPPoE {\n"; admin += "        correct=\\\"false\\\"\n";
	 * admin += "        \n"; admin +=
	 * "        while [ \\\"\\${correct}\\\" = \\\"false\\\" ]; do\n"; admin +=
	 * "            clear\n"; admin += "            \n"; admin +=
	 * "            echo \\\"Enter your ISP's login username and press [ENTER]\\\"\n"
	 * ; admin += "            read -r username\n"; admin +=
	 * "            echo \\\"Enter your ISP's login password and press [ENTER]\\\"\n"
	 * ; admin += "            read -r password\n"; admin += "            \n"; admin
	 * += "            clear\n"; admin += "            \n"; admin +=
	 * "            echo -e \\\"Username: \\${GREEN}\\${username}\\${NC}\\\"\n";
	 * admin +=
	 * "            echo -e \\\"Password: \\${GREEN}\\${password}\\${NC}\\\"\n";
	 * admin += "            \n"; admin +=
	 * "            read -r -p \\\"Are the above credentials correct? [Y/n]\\\" yn\n"
	 * ; admin += "            \n"; admin +=
	 * "            case \\\"\\${yn}\\\" in\n"; admin +=
	 * "                [nN]* ) correct=\\\"false\\\";;\n"; admin +=
	 * "                    * ) correct=\\\"true\\\";\n"; admin +=
	 * "                        printf \\\"%s      *      %s\\\" \\\"\\${username}\\\" \\\"\\${password}\\\" > /etc/ppp/chap-secrets;;\n"
	 * ; admin += "			esac\n"; admin += "		done\n"; admin += "      \n";
	 * admin +=
	 * "      read -n 1 -s -r -p \\\"Press any key to return to the main menu...\\\"\n"
	 * ; admin += "\n"; admin += "}\n"; admin += "\n"; } admin +=
	 * "function speedtest {\n"; admin += "        clear\n"; admin += "\n"; admin +=
	 * "        echo \\\"Running a speed test - please wait...\\\"\n"; admin +=
	 * "        echo \n"; admin += "        speedtest-cli\n"; admin +=
	 * "        echo \n"; admin +=
	 * "        read -n 1 -s -r -p \\\"Press any key to return to the main menu...\\\"\n"
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
