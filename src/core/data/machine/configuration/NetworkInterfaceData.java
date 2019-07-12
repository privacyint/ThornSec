/*
 * This code is part of the ThornSec project.
 * 
 * To learn more, please head to its GitHub repo: @privacyint
 * 
 * Pull requests encouraged.
 */
package core.data.machine.configuration;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.json.JsonObject;
import javax.json.stream.JsonParsingException;

import inet.ipaddr.AddressStringException;
import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.MACAddressString;
import inet.ipaddr.IPAddress.IPVersion;
import inet.ipaddr.mac.MACAddress;

import core.data.AData;

import core.exception.data.ADataException;
import core.exception.data.InvalidIPAddressException;

/**
 * Represents a network interface.
 * Its internal data represents https://wiki.debian.org/NetworkConfiguration#iproute2_method
 * which means we can allow our admins to have fine-grained control over their ifaces! 
 */
public class NetworkInterfaceData extends AData {
	public enum Inet {
		MANUAL("manual"),
		STATIC("static"),
		DHCP("dhcp");
		
		private String inet;
		
		Inet(String inet) {
			this.inet = inet;
		}
		
		public String getInet() {
			return this.inet;
		}
	}
	
	private String      comment;
	private HostName    host;
	private String      iface;
	private String      preUp;
	private String      postDown;
	private Inet        inet;
	private MACAddress  mac;
	private Set<String> bridgePorts;
	private IPAddress   address;
	private IPAddress   gateway;
	private IPAddress   subnet;
	private IPAddress   netmask;
	private IPAddress   broadcast;

	/**
	 * Create a NetworkInterfaceData containing null values.
	 * You should only use this constructor if you intend to call the read() method
	 * otherwise you're wasting electrons!
	 */
	public NetworkInterfaceData() {
		this(null, null, null, null, null, null, null, null, null, null, null, null);
	}

	public NetworkInterfaceData(HostName host, String iface, String preUp, String postDown, MACAddress mac, Inet inet, Set<String> bridgePorts, IPAddress address, IPAddress gateway, IPAddress subnet, IPAddress broadcast, String comment) {
		super(host.getHost() + "_iface_" + iface);
		
		this.host        = host;
		this.iface       = iface;
		this.preUp       = preUp;
		this.postDown    = postDown;
		this.inet        = inet;
		this.bridgePorts = bridgePorts;
		this.address     = address;
		this.gateway     = gateway;
		this.subnet      = subnet;
		this.broadcast   = broadcast;
		this.mac         = mac;
		this.comment     = comment;
	}

	/**
	 * Reads everything except the host. Please set this too!
	 */
	@Override
	public void read(JsonObject data)
	throws ADataException, JsonParsingException, IOException {
		this.iface = data.getString("iface", null);
		this.inet  = Inet.valueOf(data.getString("inet", null));
		try {
			this.address   = new IPAddressString(data.getString("address", null)).toAddress(IPVersion.IPV4);
			this.subnet    = new IPAddressString(data.getString("subnet", null)).toAddress(IPVersion.IPV4);
			this.broadcast = new IPAddressString(data.getString("broadcast", null)).toAddress(IPVersion.IPV4);
			this.gateway   = new IPAddressString(data.getString("gateway", null)).toAddress(IPVersion.IPV4);
		}
		catch(AddressStringException e) {
			throw new InvalidIPAddressException();
		}
		this.mac         = new MACAddressString(data.getString("mac", null)).getAddress();
		this.bridgePorts = new HashSet<String>(Arrays.asList(data.getString("bridgeports", null).split("[^a-z0-9]")));
		this.comment     = data.getString("comment", null);
	}
	
	public final void setHost(HostName host) {
		this.host = host;
	}
	
	final public String getIface() {
		return this.iface;
	}
	
	final public String getComment() {
		return this.comment;
	}
	
	final public IPAddress getAddress() {
		return this.address;
	}
	
	final public IPAddress getGateway() {
		return this.gateway;
	}

	final public IPAddress getSubnet() {
		return this.subnet;
	}
	
	final public IPAddress getBroadcast() {
		return this.broadcast;
	}
	
	final public HostName getHost() {
		return this.host;
	}
	
	final public Set<String> getBridgePorts() {
		return this.bridgePorts;
	}
	
	final public Inet getInet() {
		return this.inet;
	}
	
	final public MACAddress getMAC() {
		return this.mac;
	}
	
	final public String getPreUp() {
		return this.preUp;
	}
	
	final public String getPostDown() {
		return this.postDown;
	}

	final public IPAddress getNetmask() {
		return this.netmask;
	}
}
