package profile.firewall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import core.StringUtils;
import core.iface.IUnit;
import core.model.network.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.CustomFileUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FilePermsUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;

//https://linux.die.net/man/8/iptables
public class IPTablesFirewall extends AStructuredProfile {

	private Hashtable<String, Hashtable<String, Set<String>>> tables;
	
	public IPTablesFirewall(String label, NetworkModel networkModel) {
		super(label, networkModel);

		tables = new Hashtable<String, Hashtable<String, Set<String>>>();

		tables.put("mangle", new Hashtable<String, Set<String>>());
		tables.put("nat",    new Hashtable<String, Set<String>>());
		tables.put("filter", new Hashtable<String, Set<String>>());

		Hashtable<String, Set<String>> mangle = tables.get("mangle");
		Hashtable<String, Set<String>> nat    = tables.get("nat");
		Hashtable<String, Set<String>> filter = tables.get("filter");

		mangle.put("PREROUTING",  new HashSet<String>());
		mangle.put("INPUT",       new HashSet<String>());
		mangle.put("FORWARD",     new HashSet<String>());
		mangle.put("OUTPUT",      new HashSet<String>());
		mangle.put("POSTROUTING", new HashSet<String>());
		
		nat.put("PREROUTING",  new HashSet<String>());
		nat.put("INPUT",       new HashSet<String>());
		nat.put("OUTPUT",      new HashSet<String>());
		nat.put("POSTROUTING", new HashSet<String>());

		filter.put("INPUT",   new HashSet<String>());
		filter.put("FORWARD", new HashSet<String>());
		filter.put("OUTPUT",  new HashSet<String>());

		this.addFilterInput("iptables_in_tcp",  "-p tcp -j ACCEPT",  "Allow inbound TCP");
		this.addFilterInput("iptables_in_udp",  "-p udp -j ACCEPT",  "Allow inbound UDP");
		this.addFilterInput("iptables_in_icmp", "-p icmp -j ACCEPT", "Allow inbound ping");

		this.addFilterForward("iptables_fwd_tcp",  "-p tcp -j ACCEPT",  "Allow internal comms on TCP");
		this.addFilterForward("iptables_fwd_udp",  "-p udp -j ACCEPT",  "Allow internal comms on UDP");
		this.addFilterForward("iptables_fwd_icmp", "-p icmp -j ACCEPT", "Allow internal machines to ping each other");

		this.addFilterOutput("iptables_out_tcp",  "-p tcp -j ACCEPT",  "Allow outbound TCP");
		this.addFilterOutput("iptables_out_udp",  "-p udp -j ACCEPT",  "Allow outbound UDP");
		this.addFilterOutput("iptables_out_icmp", "-p icmp -j ACCEPT", "Allow outbound ping");
	}
	
	/**
	 * Gets the firewall configuration units.
	 * 
	 * @return Configuration units
	 */
	public Set<IUnit> getUnits() {
		Set<IUnit> units = new HashSet<IUnit>();
		
		//Need to do IPSet stuff up here, as iptables now relies on it
		//units.addAll(networkModel.getIPSet().getUnits());
		units.add(new SimpleUnit("iptables_running_conf_backup", "iptables_dir_created",
				"echo 'iptables-save > /etc/iptables/iptables.conf.bak' | sudo bash",
				"find /etc/iptables/iptables.conf.bak -cmin -1 2>&1", "/etc/iptables/iptables.conf.bak", "pass",
				"Couldn't take a backup of the currently running iptables rules.  I won't apply the new ones as a precaution."));
		
		units.add(new InstalledUnit("iptables", "proceed", "iptables", "I was unable to install your firewall. This is bad."));

		units.add(new DirUnit("iptables_dir", "proceed", "/etc/iptables"));
		
		String iptablesConfSh = "";
		iptablesConfSh += "#!/bin/bash\n";
		iptablesConfSh += "cd /etc/iptables/\n";
		iptablesConfSh += "{\n";
		iptablesConfSh += "    cat iptables.mangle.policies.conf | awk /./;\n";
		iptablesConfSh += "    cat iptables.mangle.forward.conf | awk /./;\n";
		iptablesConfSh += "    cat iptables.mangle.custom.conf | awk /./;\n";
		iptablesConfSh += "    cat iptables.nat.prerouting.policies.conf | awk /./;\n";
		iptablesConfSh += "    cat iptables.nat.prerouting.custom.conf | awk /./;\n";
		iptablesConfSh += "    cat iptables.nat.prerouting.rules.conf | awk /./;\n";
		iptablesConfSh += "    cat iptables.nat.postrouting.policies.conf | awk /./;\n";
		iptablesConfSh += "    cat iptables.nat.postrouting.custom.conf | awk /./;\n";
		iptablesConfSh += "    cat iptables.nat.postrouting.rules.conf | awk /./;\n";
		iptablesConfSh += "    cat iptables.filter.policies.conf | awk /./;\n";
		iptablesConfSh += "    cat iptables.filter.custom.conf | awk /./;\n";
		iptablesConfSh += "    cat iptables.filter.rules.conf | awk /./;\n";
		iptablesConfSh += "}\n";
		iptablesConfSh += "echo 'COMMIT'";
		
		units.add(new FileUnit("iptables_conf_shell_script", "iptables_dir_created", iptablesConfSh, "/etc/iptables/iptables.conf.sh"));
		units.add(new FilePermsUnit("iptables_conf_shell_script", "iptables_conf_shell_script", "/etc/iptables/iptables.conf.sh", "750"));
		
		String manglePolicies = "";
		manglePolicies += "*mangle\n";
		manglePolicies += ":PREROUTING ACCEPT [0:0]\n";
		manglePolicies += ":INPUT ACCEPT [0:0]\n";
		manglePolicies += ":FORWARD ACCEPT [0:0]\n";
		manglePolicies += ":OUTPUT ACCEPT [0:0]\n";
		manglePolicies += ":POSTROUTING ACCEPT [0:0]";
		units.add(new FileUnit("iptables_mangle_policies", "iptables_dir_created", manglePolicies, "/etc/iptables/iptables.mangle.policies.conf"));
		units.add(new CustomFileUnit("iptables_mangle_custom", "iptables_dir_created", "/etc/iptables/iptables.mangle.custom.conf"));
		units.add(new FileUnit("iptables_mangle_forward", "iptables_dir_created", getMangleForward(), "/etc/iptables/iptables.mangle.forward.conf"));

		String natPreRoutingPolicy = "";
		natPreRoutingPolicy += "COMMIT\n";
		natPreRoutingPolicy += "*nat\n";
		natPreRoutingPolicy += ":PREROUTING ACCEPT [0:0]";
		units.add(new FileUnit("iptables_nat_prerouting_policy", "iptables_dir_created", natPreRoutingPolicy, "/etc/iptables/iptables.nat.prerouting.policies.conf"));
		units.add(new CustomFileUnit("iptables_nat_prerouting_rules_custom", "iptables_dir_created",	"/etc/iptables/iptables.nat.prerouting.custom.conf"));
		units.add(new FileUnit("iptables_nat_prerouting_rules", "iptables_dir_created", getNatPrerouting(), "/etc/iptables/iptables.nat.prerouting.rules.conf"));

		String natPostRoutingPolicies = "";
		natPostRoutingPolicies += ":INPUT ACCEPT [0:0]\n";
		natPostRoutingPolicies += ":OUTPUT ACCEPT [0:0]\n";
		natPostRoutingPolicies += ":POSTROUTING ACCEPT [0:0]";
		units.add(new FileUnit("iptables_nat_postrouting_policy", "iptables_dir_created", natPostRoutingPolicies, "/etc/iptables/iptables.nat.postrouting.policies.conf"));
		units.add(new CustomFileUnit("iptables_nat_postrouting_rules_custom", "iptables_dir_created","/etc/iptables/iptables.nat.postrouting.custom.conf"));
		units.add(new FileUnit("iptables_nat_postrouting_rules", "iptables_dir_created", getNatPostrouting(), "/etc/iptables/iptables.nat.postrouting.rules.conf"));
		
		String filterPolicies = "";
		filterPolicies += "COMMIT\n";
		filterPolicies += "*filter\n";
		filterPolicies += ":INPUT ACCEPT [0:0]\n";
		filterPolicies += ":FORWARD ACCEPT [0:0]\n";
		filterPolicies += ":OUTPUT ACCEPT [0:0]";
		units.add(new FileUnit("iptables_filter_policy", "iptables_dir_created", filterPolicies, "/etc/iptables/iptables.filter.policies.conf"));
		units.add(new CustomFileUnit("iptables_filter_rules_custom", "iptables_dir_created", "/etc/iptables/iptables.filter.custom.conf"));
		units.add(new FileUnit("iptables_filter_rules", "iptables_dir_created", getFilter(), "/etc/iptables/iptables.filter.rules.conf"));

		//This needs child units back.  I should get 'round to fixing that.
		//Hardcode fail for now...
		units.add(new SimpleUnit("iptables_conf_persist", "iptables_running_conf_backup",
				"sudo /etc/iptables/iptables.conf.sh | sudo iptables-restore --test"
				+ " &&"
				+ " sudo /etc/iptables/iptables.conf.sh | sudo iptables-restore;",
				"echo 'This is a hardcoded fail for now. Don\t worry!'", "", "pass"));
		
		return units;
	}
	
	public void addFilterInput(String name, String rule, String comment) {
		add(name, "filter", "INPUT", rule, comment);
	}

	public void addFilterForward(String name, String rule, String comment) {
		add(name, "filter", "FORWARD", rule, comment);
	}

	public void addFilterOutput(String name, String rule, String comment) {
		add(name, "filter", "OUTPUT", rule, comment);
	}

	public void addFilter(String name, String chain, String rule, String comment) {
		add(name, "filter", chain, rule, comment);
	}

	public void addFilter(String name, String chain, int position, String rule, String comment) {
		add(name, "filter", chain, position, rule, comment);
	}
	
	public void addNatPostrouting(String name, String rule, String comment) {
		add(name, "nat", "POSTROUTING", rule, comment);
	}

	public void addNatPrerouting(String name, String rule, String comment) {
		add(name, "nat", "PREROUTING", rule, comment);
	}
	
	public void addMangleForward(String name, String rule, String comment) {
		add(name, "mangle", "FORWARD", rule, comment);
	}

	public SimpleUnit addChain(String name, String table, String chain) {
		chain = StringUtils.stringToAlphaNumeric(chain, "_");
		table = StringUtils.stringToAlphaNumeric(table, "_");
		
		Hashtable<String, Set<String>> tab = tables.get(table);
		
		if (tab == null) {
			tables.put(table, new Hashtable<String, Set<String>>());
		}
		tab = tables.get(table);
		
		Set<String> ch = tab.get(chain);
		if (ch == null) {
			tab.put(chain, new HashSet<String>());
		}
		
		return new SimpleUnit(name + "_chain", "proceed", "echo \\\"handled by model\\\";",
				"cat /etc/iptables/iptables.conf | iptables-xml | xsltproc --stringparam table " + table
						+ " /etc/iptables/iptables.xslt - | " + "grep \":" + chain + " -\"",
				":" + chain + " - [0:0]", "pass");
	}

	private void add(String name, String table, String chain, int position, String rule, String comment) {
		chain = StringUtils.stringToAlphaNumeric(chain, "_");
		table = StringUtils.stringToAlphaNumeric(table, "_");
		
		if (comment.equals("")) {
			comment = "This is probably important, but there is no associated comment - sorry!";
		}
		
		rule += " -m comment --comment \\\"" + comment + "\\\"";
		
		this.getChain(table, chain).add(rule);
	}

	private void add(String name, String table, String chain, String rule, String comment) {
		chain = StringUtils.stringToAlphaNumeric(chain, "_");
		table = StringUtils.stringToAlphaNumeric(table, "_");
		
		int position = this.getChain(table, chain).size();
		
		add(name, table, chain, position, rule, comment);
	}

	private String getNatPostrouting() {
		return getRules("nat", "POSTROUTING");
	}

	private String getMangleForward() {
		return getRules("mangle", "FORWARD");
	}
	
	private String getNatPrerouting() {
		return getRules("nat", "PREROUTING");
	}

	private Set<String> reverseSet(Set<String> set) {
		List<String> ch = new ArrayList<>(set);
		ch.sort(Collections.reverseOrder());
		
		return new HashSet<>(ch);
	}
	
	private String getRules(String table, String chain) {
		chain = StringUtils.stringToAlphaNumeric(chain, "_");
		table = StringUtils.stringToAlphaNumeric(table, "_");
		
		Set<String> rules = reverseSet(this.getChain(table, chain));
		
		String out = "";
		for (String rule : rules) {
			out += "\n-A " + chain +  " " + rule;
		}
		return out;
	}
	
	private String getFilter() {
		Hashtable<String, Set<String>> filterTable = tables.get("filter");

		String policies = "";
		String filters  = "";
		
		for (String policy : filterTable.keySet()) {
			if (!policy.equals("INPUT") && !policy.equals("FORWARD") && !policy.equals("OUTPUT"))
				policies += ":" + policy + " - [0:0]\n";
			
			Set<String> chain = reverseSet(this.getChain("filter", policy));

			for (String filter : chain) {
				filters += "\n-A " + policy +  " " + filter;
			}
		}
		return policies + filters;
	}

	private Set<String> getChain(String table, String chain) {
		chain = StringUtils.stringToAlphaNumeric(chain, "_");
		table = StringUtils.stringToAlphaNumeric(table, "_");
		
		Set<String> ch = tables.get(table).get(chain);
		if (ch == null) {
			addChain("auto_create_chain_" + chain, table, chain);
		}
		
		return tables.get(table).get(chain);
	}

	@Override
	public Set<IUnit> getNetworking() {
		// TODO Auto-generated method stub
		return null;
	}
}
