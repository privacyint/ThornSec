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
		units.addElement(new InstalledUnit("iptables_xsltproc", "xsltproc"));
		units.addElement(new DirUnit("iptables_dir", "proceed", "/etc/iptables"));
		
		units.addElement(new SimpleUnit("iptables_conf_persist", "iptables_dir_created",
				"echo \"" + getPersistent() + "\" | sudo tee /etc/iptables/iptables.conf;"
				+ "sudo iptables-restore < /etc/iptables/iptables.conf;",
				"cat /etc/iptables/iptables.conf;", getPersistent(), "pass"));
		
		String iptxslt = "";
		iptxslt += "<?xml version=\\\"1.0\\\" encoding=\\\"ISO-8859-1\\\"?>\n";
		iptxslt += "<xsl:transform version=\\\"1.0\\\" xmlns:xsl=\\\"http://www.w3.org/1999/XSL/Transform\\\">\n";
		iptxslt += "  <xsl:output method = \\\"text\\\" />\n";
		iptxslt += "  <xsl:strip-space elements=\\\"*\\\" />\n";
		iptxslt += "    <xsl:param name=\\\"table\\\" />\n";
		iptxslt += "\n";
		iptxslt += "  <xsl:template match=\\\"iptables-rules/table/chain/rule/conditions/*\\\">\n";
		iptxslt += "    <xsl:if test=\\\"name() != &quot;match&quot;\\\">\n";
		iptxslt += "      <xsl:text> -m </xsl:text><xsl:value-of select=\\\"name()\\\"/>\n";
		iptxslt += "    </xsl:if>\n";
		iptxslt += "    <xsl:apply-templates select=\\\"node()\\\"/>\n";
		iptxslt += "  </xsl:template>\n";
		iptxslt += "\n";
		iptxslt += "  <xsl:template match=\\\"iptables-rules/table/chain/rule/actions|table/chain/rule/conditions\\\">\n";
		iptxslt += "    <xsl:apply-templates select=\\\"*\\\"/>\n";
		iptxslt += "  </xsl:template>\n";
		iptxslt += "\n";
		iptxslt += "  <xsl:template match=\\\"iptables-rules/table/chain/rule/actions/goto\\\">\n";
		iptxslt += "    <xsl:text> -g </xsl:text>\n";
		iptxslt += "    <xsl:apply-templates select=\\\"*\\\"/>\n";
		iptxslt += "    <xsl:text>&#xA;</xsl:text>\n";
		iptxslt += "  </xsl:template>\n";
		iptxslt += "  <xsl:template match=\\\"iptables-rules/table/chain/rule/actions/call\\\">\n";
		iptxslt += "    <xsl:text> -j </xsl:text>\n";
		iptxslt += "    <xsl:apply-templates select=\\\"*\\\"/>\n";
		iptxslt += "    <xsl:text>&#xA;</xsl:text>\n";
		iptxslt += "  </xsl:template>\n";
		iptxslt += "  <xsl:template match=\\\"iptables-rules/table/chain/rule/actions/*\\\">\n";
		iptxslt += "    <xsl:text> -j </xsl:text><xsl:value-of select=\\\"name()\\\"/>\n";
		iptxslt += "    <xsl:apply-templates select=\\\"*\\\"/>\n";
		iptxslt += "    <xsl:text>&#xA;</xsl:text>\n";
		iptxslt += "  </xsl:template>\n";
		iptxslt += "\n";
		iptxslt += "  <xsl:template match=\\\"iptables-rules/table/chain/rule/actions//*|iptables-rules/table/chain/rule/conditions//*\\\" priority=\\\"0\\\">\n";
		iptxslt += "    <xsl:if test=\\\"@invert=1\\\"><xsl:text> !</xsl:text></xsl:if>\n";
		iptxslt += "    <xsl:text> -</xsl:text>\n";
		iptxslt += "    <xsl:if test=\\\"string-length(name())&gt;1\\\">\n";
		iptxslt += "      <xsl:text>-</xsl:text>\n";
		iptxslt += "    </xsl:if>\n";
		iptxslt += "    <xsl:value-of select=\\\"name()\\\"/>\n";
		iptxslt += "    <xsl:text> </xsl:text>\n";
		iptxslt += "    <xsl:apply-templates select=\\\"node()\\\"/>\n";
		iptxslt += "  </xsl:template>\n";
		iptxslt += "\n";
		iptxslt += "  <xsl:template match=\\\"iptables-rules/table/chain/rule/actions/call/*|iptables-rules/table/chain/rule/actions/goto/*\\\">\n";
		iptxslt += "    <xsl:value-of select=\\\"name()\\\"/>\n";
		iptxslt += "    <xsl:apply-templates select=\\\"node()\\\"/>\n";
		iptxslt += "  </xsl:template>\n";
		iptxslt += "\n";
		iptxslt += "  <xsl:template name=\\\"rule-head\\\">\n";
		iptxslt += "    <xsl:text>-A </xsl:text><!-- a rule must be under a chain -->\n";
		iptxslt += "    <xsl:value-of select=\\\"../@name\\\" />\n";
		iptxslt += "    <xsl:apply-templates select=\\\"conditions\\\"/>\n";
		iptxslt += "  </xsl:template>\n";
		iptxslt += "\n";
		iptxslt += "  <xsl:template match=\\\"iptables-rules/table/chain/rule\\\">\n";
		iptxslt += "    <xsl:choose>\n";
		iptxslt += "      <xsl:when test=\\\"count(actions/*)&gt;0\\\">\n";
		iptxslt += "        <xsl:for-each select=\\\"actions/*\\\">\n";
		iptxslt += "          <xsl:for-each select=\\\"../..\\\">\n";
		iptxslt += "            <xsl:call-template name=\\\"rule-head\\\"/>\n";
		iptxslt += "          </xsl:for-each>\n";
		iptxslt += "          <xsl:apply-templates select=\\\".\\\"/>\n";
		iptxslt += "        </xsl:for-each>\n";
		iptxslt += "      </xsl:when>\n";
		iptxslt += "      <xsl:otherwise>\n";
		iptxslt += "        <xsl:call-template name=\\\"rule-head\\\"/>\n";
		iptxslt += "        <xsl:text>&#xA;</xsl:text>\n";
		iptxslt += "      </xsl:otherwise>\n";
		iptxslt += "    </xsl:choose>\n";
		iptxslt += "  </xsl:template>\n";
		iptxslt += "\n";
		iptxslt += "  <xsl:template match=\\\"iptables-rules/table\\\">\n";
		iptxslt += "    <xsl:if test=\\\"@name=\\$table\\\">\n";
		iptxslt += "    <xsl:for-each select=\\\"chain\\\">\n";
		iptxslt += "    <xsl:text>:</xsl:text>\n";
		iptxslt += "    <xsl:value-of select=\\\"@name\\\"/>\n";
		iptxslt += "    <xsl:text> </xsl:text>\n";
		iptxslt += "    <xsl:choose>\n";
		iptxslt += "    <xsl:when test=\\\"not(string-length(@policy))\\\"><xsl:text>-</xsl:text></xsl:when>\n";
		iptxslt += "      <xsl:otherwise><xsl:value-of select=\\\"@policy\\\"/></xsl:otherwise>\n";
		iptxslt += "     </xsl:choose>\n";
		iptxslt += "      <xsl:text> </xsl:text>\n";
		iptxslt += "       <xsl:call-template name=\\\"counters\\\"><xsl:with-param name=\\\"node\\\" select=\\\".\\\"/></xsl:call-template>\n";
		iptxslt += "       <xsl:text>&#xA;</xsl:text>\n";
		iptxslt += "      </xsl:for-each>\n";
		iptxslt += "            <xsl:apply-templates select=\\\"node()\\\"/>\n";
		iptxslt += "    </xsl:if>\n";
		iptxslt += "  </xsl:template>\n";
		iptxslt += "\n";
		iptxslt += "<xsl:template name=\\\"counters\\\">\n";
		iptxslt += "	<xsl:param name=\\\"node\\\"/>\n";
		iptxslt += "	<xsl:text>[</xsl:text>\n";
		iptxslt += "	<xsl:if test=\\\"string-length(\\$node/@packet-count)\\\"><xsl:value-of select=\\\"\\$node/@packet-count\\\"/></xsl:if>\n";
		iptxslt += "	<xsl:if test=\\\"string-length(\\$node/@packet-count)=0\\\">0</xsl:if>\n";
		iptxslt += "	<xsl:text>:</xsl:text>\n";
		iptxslt += "	<xsl:if test=\\\"string-length(\\$node/@byte-count)\\\"><xsl:value-of select=\\\"\\$node/@byte-count\\\"/></xsl:if>\n";
		iptxslt += "	<xsl:if test=\\\"string-length(\\$node/@byte-count)=0\\\">0</xsl:if>\n";
		iptxslt += "	<xsl:text>]</xsl:text>\n";
		iptxslt += "</xsl:template>\n";
		iptxslt += "\n";
		iptxslt += "  <xsl:template match=\\\"@*|node()\\\">\n";
		iptxslt += "    <xsl:copy>\n";
		iptxslt += "      <xsl:apply-templates select=\\\"@*\\\"/>\n";
		iptxslt += "      <xsl:apply-templates select=\\\"node()\\\"/>\n";
		iptxslt += "    </xsl:copy>\n";
		iptxslt += "  </xsl:template>\n";
		iptxslt += "\n";
		iptxslt += "</xsl:transform>";
		units.addElement(new FileUnit("iptables_xlst", "proceed", iptxslt, "/etc/iptables/iptables.xslt"));

		return units;
	}

	public SimpleUnit addFilterInput(String name, String rule) {
		return add(name, "filter", "INPUT", rule);
	}

	public SimpleUnit addFilterForward(String name, String rule) {
		return add(name, "filter", "FORWARD", rule);
	}

	public SimpleUnit addFilterOutput(String name, String rule) {
		return add(name, "filter", "OUTPUT", rule);
	}

	public SimpleUnit addFilter(String name, String chain, String rule) {
		return add(name, "filter", chain, rule);
	}

	public SimpleUnit addFilter(String name, String chain, int position, String rule) {
		return add(name, "filter", chain, position, rule);
	}
	
	public SimpleUnit addNatPostrouting(String name, String rule) {
		return add(name, "nat", "POSTROUTING", rule);
	}

	public SimpleUnit addNatPrerouting(String name, String rule) {
		return add(name, "nat", "PREROUTING", rule);
	}
	
	public SimpleUnit addMangleForward(String name, String rule) {
		return add(name, "mangle", "FORWARD", rule);
	}

	public SimpleUnit addChain(String name, String table, String chain) {
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


	private SimpleUnit add(String name, String table, String chain, int position, String rule) {
		this.getChain(table, chain).add(position, rule);
		return new SimpleUnit(name, "proceed", "echo \\\"handled by model\\\";",
				"cat /etc/iptables/iptables.conf | iptables-xml | xsltproc --stringparam table " + table
						+ " /etc/iptables/iptables.xslt - | " + "grep \"" + chain + " " + rule.replaceAll("-", "\\\\-")
						+ "\"",
				"-A " + chain + " " + rule, "pass");
	}

	private SimpleUnit add(String name, String table, String chain, String rule) {
		this.getChain(table, chain).add(rule);
		return new SimpleUnit(name, "proceed", "echo \\\"handled by model\\\";",
				"cat /etc/iptables/iptables.conf | iptables-xml | xsltproc --stringparam table " + table
						+ " /etc/iptables/iptables.xslt - | " + "grep \"" + chain + " " + rule.replaceAll("-", "\\\\-")
						+ "\"",
				"-A " + chain + " " + rule, "pass");
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
		Vector<String> chain = new Vector<String>(new LinkedHashSet<String>(this.getChain("nat", "POSTROUTING")));

		String natPostrouting = "";

		for (int i = chain.size() - 1; i > 0; --i) { //Loop through backwards
			natPostrouting += "-A POSTROUTING " + chain.elementAt(i) + "\n";
		}
		
		return natPostrouting;
	}

	private String getMangleForward() {
		Vector<String> chain = new Vector<String>(new LinkedHashSet<String>(this.getChain("mangle", "FORWARD")));

		String mangleForward = "";

		for (int i = chain.size() - 1; i > 0; --i) { //Loop through backwards
			mangleForward += "-A FORWARD " + chain.elementAt(i) + "\n";
		}
		return mangleForward;
	}
	
	private String getNatPrerouting() {
		Vector<String> chain = new Vector<String>(new LinkedHashSet<String>(this.getChain("nat", "PREROUTING")));

		String natPrerouting = "";
		
		for (int i = chain.size() - 1; i > 0; --i) { //Loop through backwards
			natPrerouting += "-A PREROUTING " + chain.elementAt(i) + "\n";
		}
		
		return natPrerouting;
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
			for (int j = 0; j < chain.size(); j++) {
				filters += "-A " + policy + " " + chain.elementAt(chain.size() - j - 1) + "\n";
			}
		}
		return policies + filters;
	}

	private Vector<String> getChain(String table, String chain) {
		Vector<String> ch = iptTables.get(table).get(chain);
		if (ch == null) {
			addChain("auto_create_chain_" + chain, table, chain);
		}
		
		return iptTables.get(table).get(chain);
	}

}
