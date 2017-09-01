package core.model;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Vector;

import core.iface.IUnit;
import core.unit.SimpleUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;

public class FirewallModel extends AModel {

	private Hashtable<String, Hashtable<String, Vector<String>>> tables;

	public FirewallModel(String label) {
		super(label);
	}

	public void init(NetworkModel model) {
		this.tables = new Hashtable<>();

		tables.put("mangle", new Hashtable<String, Vector<String>>());
		Hashtable<String, Vector<String>> tab = tables.get("mangle");
		tab.put("PREROUTING", new Vector<String>());
		tab.put("INPUT", new Vector<String>());
		tab.put("FORWARD", new Vector<String>());
		tab.put("OUTPUT", new Vector<String>());
		tab.put("POSTROUTING", new Vector<String>());
		
		tables.put("nat", new Hashtable<String, Vector<String>>());
		tab = tables.get("nat");
		tab.put("PREROUTING", new Vector<String>());
		tab.put("INPUT", new Vector<String>());
		tab.put("OUTPUT", new Vector<String>());
		tab.put("POSTROUTING", new Vector<String>());

		tables.put("filter", new Hashtable<String, Vector<String>>());
		tab = tables.get("filter");
		tab.put("INPUT", new Vector<String>());
		tab.put("FORWARD", new Vector<String>());
		tab.put("OUTPUT", new Vector<String>());

		this.addFilterInput("iptables_in_tcp", "-p tcp -j ACCEPT");
		this.addFilterInput("iptables_in_udp", "-p udp -j ACCEPT");
		this.addFilterInput("iptables_in_icmp", "-p icmp -j ACCEPT");

		this.addFilterForward("iptables_fwd_tcp", "-p tcp -j ACCEPT");
		this.addFilterForward("iptables_fwd_udp", "-p udp -j ACCEPT");
		this.addFilterForward("iptables_fwd_icmp", "-p icmp -j ACCEPT");

		this.addFilterOutput("iptables_out_tcp", "-p tcp -j ACCEPT");
		this.addFilterOutput("iptables_out_udp", "-p udp -j ACCEPT");
		this.addFilterOutput("iptables_out_icmp", "-p icmp -j ACCEPT");
		
		//We'll now do this on a per-server basis!
		/*
		this.addFilterOutput("base_debian1_out", "-d 78.129.164.123 -p tcp --dport 80 -j ACCEPT");
		this.addFilterOutput("base_debian2_out", "-d 212.211.132.250 -p tcp --dport 80 -j ACCEPT");
		this.addFilterOutput("base_debian3_out", "-d 212.211.132.32 -p tcp --dport 80 -j ACCEPT");
		this.addFilterOutput("base_debian4_out", "-d 195.20.242.89 -p tcp --dport 80 -j ACCEPT");

		this.addFilterInput("base_debian1_in", "-s 78.129.164.123 -p tcp --sport 80 -j ACCEPT");
		this.addFilterInput("base_debian2_in", "-s 212.211.132.250 -p tcp --sport 80 -j ACCEPT");
		this.addFilterInput("base_debian3_in", "-s 212.211.132.32 -p tcp --sport 80 -j ACCEPT");
		this.addFilterInput("base_debian4_in", "-s 195.20.242.89 -p tcp --sport 80 -j ACCEPT");
		*/
	}

	public Vector<IUnit> getUnits() {
		Vector<IUnit> units = new Vector<IUnit>();
		units.addElement(new InstalledUnit("iptables", "iptables"));
		units.addElement(new InstalledUnit("iptables_xsltproc", "xsltproc"));
		units.addElement(new DirUnit("iptables_dir", "proceed", "/etc/iptables"));
		units.addElement(new SimpleUnit("iptables_conf_persist", "iptables_dir_created",
				"echo \"" + getPersistent() + "\" | sudo tee /etc/iptables/iptables.conf; sudo iptables-restore < /etc/iptables/iptables.conf;",
				"cat /etc/iptables/iptables.conf;", getPersistent(), "pass"));
		String iptxslt = "<?xml version=\\\"1.0\\\" encoding=\\\"ISO-8859-1\\\"?>\n";
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
		Hashtable<String, Vector<String>> tab = tables.get(table);
		if (tab == null) {
			tables.put(table, new Hashtable<String, Vector<String>>());
		}
		tab = tables.get(table);
		Vector<String> ch = tab.get(chain);
		if (ch == null) {
			tab.put(chain, new Vector<String>());
		}
		return new SimpleUnit(name, "proceed", "echo \\\"handled by model\\\";",
				"cat /etc/iptables/iptables.conf | iptables-xml | xsltproc --stringparam table " + table
						+ " /etc/iptables/iptables.xslt - | " + "grep \":" + chain + " -\"",
				":" + chain + " - [0:0]", "pass");
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
		ipt += getMangle();
		ipt += "COMMIT\n";
		ipt += "*nat\n";
		ipt += ":PREROUTING ACCEPT [0:0]\n";
		ipt += getForward();
		ipt += ":INPUT ACCEPT [0:0]\n";
		ipt += ":OUTPUT ACCEPT [0:0]\n";
		ipt += ":POSTROUTING ACCEPT [0:0]\n";
		ipt += getNat();
		ipt += "COMMIT\n";
		ipt += "*filter\n";
		ipt += getFilter();
		ipt += "COMMIT";
		return ipt;
	}

	private String getNat() {
		//Vector<String> chain = this.getChain("nat", "POSTROUTING");
		Vector<String> chain = new Vector<String>(new LinkedHashSet<String>(this.getChain("nat", "POSTROUTING")));

		String nat = "";
		for (int i = 0; i < chain.size(); i++) {
			nat += "-A POSTROUTING " + chain.elementAt(chain.size() - 1 - i) + "\n";
		}
		return nat;
	}

	private String getMangle() {
		//Vector<String> chain = this.getChain("nat", "POSTROUTING");
		Vector<String> chain = new Vector<String>(new LinkedHashSet<String>(this.getChain("mangle", "OUTPUT")));

		String nat = "";
		for (int i = 0; i < chain.size(); i++) {
			nat += "-A OUTPUT " + chain.elementAt(chain.size() - 1 - i) + "\n";
		}
		return nat;
	}
	
	private String getForward() {
		//Vector<String> chain = this.getChain("nat", "PREROUTING");
		Vector<String> chain = new Vector<String>(new LinkedHashSet<String>(this.getChain("nat", "PREROUTING")));

		String nat = "";
		for (int i = 0; i < chain.size(); i++) {
			nat += "-A PREROUTING " + chain.elementAt(chain.size() - 1 - i) + "\n";
		}
		return nat;
	}

	private String getFilter() {
		String policy = "";
		String filter = "";
		policy += ":INPUT ACCEPT [0:0]\n";
		policy += ":FORWARD ACCEPT [0:0]\n";
		policy += ":OUTPUT ACCEPT [0:0]\n";
		Hashtable<String, Vector<String>> tab = tables.get("filter");
		Iterator<String> iter = tab.keySet().iterator();
		while (iter.hasNext()) {
			String val = iter.next();
			if (!val.equals("INPUT") && !val.equals("FORWARD") && !val.equals("OUTPUT"))
				policy += ":" + val + " - [0:0]\n";
			
			//Vector<String> chain = this.getChain("filter", val);
			Vector<String> chain = new Vector<String>(new LinkedHashSet<String>(this.getChain("filter", val)));
			for (int j = 0; j < chain.size(); j++) {
				filter += "-A " + val + " " + chain.elementAt(chain.size() - j - 1) + "\n";
			}
		}
		return policy + filter;
	}

	private Vector<String> getChain(String table, String chain) {
		return tables.get(table).get(chain);
	}

}
