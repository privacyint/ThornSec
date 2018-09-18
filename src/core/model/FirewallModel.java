package core.model;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Vector;

import core.iface.IUnit;
import core.unit.SimpleUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;

public class FirewallModel extends AModel {

	private LinkedHashMap<String, LinkedHashMap<String, Vector<String>>> iptTables;

	public FirewallModel(String label) {
		super(label);
	}

	public void init(NetworkModel model) {
		iptTables = new LinkedHashMap<>();

		iptTables.put("mangle", new LinkedHashMap<String, Vector<String>>());
		iptTables.put("nat", new LinkedHashMap<String, Vector<String>>());
		iptTables.put("filter", new LinkedHashMap<String, Vector<String>>());

		LinkedHashMap<String, Vector<String>> mangleTable = iptTables.get("mangle");
		LinkedHashMap<String, Vector<String>> natTable    = iptTables.get("nat");
		LinkedHashMap<String, Vector<String>> filterTable = iptTables.get("filter");

		mangleTable.put("PREROUTING", new Vector<String>());
		mangleTable.put("INPUT", new Vector<String>());
		mangleTable.put("FORWARD", new Vector<String>());
		mangleTable.put("OUTPUT", new Vector<String>());
		mangleTable.put("POSTROUTING", new Vector<String>());
		
		natTable.put("PREROUTING", new Vector<String>());
		natTable.put("INPUT", new Vector<String>());
		natTable.put("OUTPUT", new Vector<String>());
		natTable.put("POSTROUTING", new Vector<String>());

		filterTable.put("INPUT", new Vector<String>());
		filterTable.put("FORWARD", new Vector<String>());
		filterTable.put("OUTPUT", new Vector<String>());

		this.addFilterInput("iptables_in_tcp", "-p tcp -j ACCEPT");
		this.addFilterInput("iptables_in_udp", "-p udp -j ACCEPT");
		this.addFilterInput("iptables_in_icmp", "-p icmp -j ACCEPT");

		this.addFilterForward("iptables_fwd_tcp", "-p tcp -j ACCEPT");
		this.addFilterForward("iptables_fwd_udp", "-p udp -j ACCEPT");
		this.addFilterForward("iptables_fwd_icmp", "-p icmp -j ACCEPT");

		this.addFilterOutput("iptables_out_tcp", "-p tcp -j ACCEPT");
		this.addFilterOutput("iptables_out_udp", "-p udp -j ACCEPT");
		this.addFilterOutput("iptables_out_icmp", "-p icmp -j ACCEPT");
	}

	public Vector<IUnit> getUnits() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		units.addElement(new InstalledUnit("iptables", "proceed", "iptables", "I was unable to install your firewall. This is bad."));
		units.addElement(new DirUnit("iptables_dir", "proceed", "/etc/iptables"));
		
		units.addElement(new SimpleUnit("iptables_running_conf_backup", "iptables_dir_created",
				"echo 'iptables-save > /etc/iptables/iptables.conf.bak' | sudo bash",
				"find /etc/iptables/iptables.conf.bak -cmin -1", "", "fail",
				"Couldn't take a backup of the currently running iptables rules.  I won't apply the new ones as a precaution."));
		
		units.addElement(new SimpleUnit("iptables_conf_persist", "iptables_running_conf_backup",
				"echo \"" + getPersistent() + "\" | sudo tee /etc/iptables/iptables.conf;"
				+ " sudo iptables-restore --test < /etc/iptables/iptables.conf"
				+ " &&"
				+ " sudo iptables-restore < /etc/iptables/iptables.conf;",
				"cat /etc/iptables/iptables.conf;", getPersistent(), "pass"));
		
		return units;
	}

	public void addFilterInput(String name, String rule) {
		add(name, "filter", "INPUT", rule);
	}

	public void addFilterForward(String name, String rule) {
		add(name, "filter", "FORWARD", rule);
	}

	public void addFilterOutput(String name, String rule) {
		add(name, "filter", "OUTPUT", rule);
	}

	public void addFilter(String name, String chain, String rule) {
		add(name, "filter", chain, rule);
	}

	public void addFilter(String name, String chain, int position, String rule) {
		add(name, "filter", chain, position, rule);
	}
	
	public void addNatPostrouting(String name, String rule) {
		add(name, "nat", "POSTROUTING", rule);
	}

	public void addNatPrerouting(String name, String rule) {
		add(name, "nat", "PREROUTING", rule);
	}
	
	public void addMangleForward(String name, String rule) {
		add(name, "mangle", "FORWARD", rule);
	}

	public SimpleUnit addChain(String name, String table, String chain) {
		chain = cleanString(chain);
		table = cleanString(table);
		
		LinkedHashMap<String, Vector<String>> tab = iptTables.get(table);
		
		if (tab == null) {
			iptTables.put(table, new LinkedHashMap<String, Vector<String>>());
		}
		tab = iptTables.get(table);
		Vector<String> ch = tab.get(chain);
		if (ch == null) {
			tab.put(chain, new Vector<String>());
		}
		return new SimpleUnit(name, "proceed", "echo \\\"handled by model\\\";",
				"cat /etc/iptables/iptables.conf | iptables-xml | xsltproc --stringparam table " + table
						+ " /etc/iptables/iptables.xslt - | " + "grep \":" + chain + " -\"",
				":" + chain + " - [0:0]", "pass");
	}

	private void add(String name, String table, String chain, int position, String rule) {
		chain = cleanString(chain);
		table = cleanString(table);
						
		this.getChain(table, chain).add(position, rule);
	}

	private void add(String name, String table, String chain, String rule) {
		table = cleanString(table);
		chain = cleanString(chain);
		
		int position = this.getChain(table, chain).size();
		
		add(name, table, chain, position, rule);
	}

	private String getPersistent() {
		String ipt = "";
		ipt += "*mangle\n";
		ipt += ":PREROUTING ACCEPT [0:0]\n";
		ipt += ":INPUT ACCEPT [0:0]\n";
		ipt += ":FORWARD ACCEPT [0:0]\n";
		ipt += ":OUTPUT ACCEPT [0:0]\n";
		ipt += ":POSTROUTING ACCEPT [0:0]\n";
		ipt += getMangleForward();
		ipt += "COMMIT\n";
		ipt += "*nat\n";
		ipt += ":PREROUTING ACCEPT [0:0]\n";
		ipt += getNatPrerouting();
		ipt += ":INPUT ACCEPT [0:0]\n";
		ipt += ":OUTPUT ACCEPT [0:0]\n";
		ipt += ":POSTROUTING ACCEPT [0:0]\n";
		ipt += getNatPostrouting();
		ipt += "COMMIT\n";
		ipt += "*filter\n";
		ipt += getFilter();
		ipt += "COMMIT";
		return ipt;
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

	private String getRules(String table, String chain) {
		chain = cleanString(chain);
		table = cleanString(table);
		
		Vector<String> ch = new Vector<String>(new LinkedHashSet<String>(this.getChain(table, chain)));

		String rules = "";
		
		for (int i = ch.size() - 1; i >= 0; --i) { //Loop through backwards
			rules += "-A " + chain +  " " + ch.elementAt(i) + "\n";
		}
		
		return rules;
	}
	
	private String getFilter() {
		LinkedHashMap<String, Vector<String>> filterTable = iptTables.get("filter");

		String policies = "";
		String filters  = "";
		
		policies += ":INPUT ACCEPT [0:0]\n";
		policies += ":FORWARD ACCEPT [0:0]\n";
		policies += ":OUTPUT ACCEPT [0:0]\n";
		
		for (String policy : filterTable.keySet()) {
			if (!Objects.equals(policy, "INPUT") && !Objects.equals(policy, "FORWARD") && !Objects.equals(policy, "OUTPUT"))
				policies += ":" + policy + " - [0:0]\n";
			
			Vector<String> chain = new Vector<String>(new LinkedHashSet<String>(this.getChain("filter", policy)));

			for (int i = chain.size() - 1; i >= 0; --i) { //Loop through backwards
				filters += "-A " + policy +  " " + chain.elementAt(i) + "\n";
			}
		}
		return policies + filters;
	}

	private Vector<String> getChain(String table, String chain) {
		chain = cleanString(chain);
		table = cleanString(table);
		
		Vector<String> ch = iptTables.get(table).get(chain);
		if (ch == null) {
			addChain("auto_create_chain_" + chain, table, chain);
		}
		
		return iptTables.get(table).get(chain);
	}
	
	private String cleanString(String string) {
		String invalidChars = "[^a-zA-Z0-9-]";
		String safeChars    = "_";
		
		return string.replaceAll(invalidChars, safeChars);
	}

}
