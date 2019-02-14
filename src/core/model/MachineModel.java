package core.model;

import java.net.InetAddress;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import core.data.InterfaceData;
import core.data.NetworkData;
import core.iface.IUnit;

public abstract class MachineModel extends AModel {

	private	NetworkData networkData;
	
	private InterfaceModel networkIfaces;
	
	private Integer firstOctet;
	private Integer secondOctet;
	private Integer thirdOctet;
	
	//Networking stuff
	private Vector<Integer> listen;

	private HashMap<String, Set<Integer>> ingress;
	private HashMap<String, Set<Integer>> egress;
	private HashMap<String, Set<Integer>> forward;
	private HashMap<String, Set<Integer>> dnat;

	MachineModel(String label, NetworkModel networkModel) {
		super(label, networkModel);

		this.networkModel = networkModel;
		
		this.firstOctet = null;
		this.secondOctet = null;
		this.thirdOctet = null;
		
		this.listen = null;
		
		this.setNetworkIfaces(new InterfaceModel(label, this, networkModel));
		this.getNetworkIfaces().init();
		
		this.ingress = new HashMap<String, Set<Integer>>();
		this.egress  = new HashMap<String, Set<Integer>>();
		this.forward = new HashMap<String, Set<Integer>>();
		this.dnat    = new HashMap<String, Set<Integer>>();
		this.listen  = new Vector<Integer>();
		
		for (String source : networkModel.getData().getRequiredIngress(getLabel())) {
			addRequiredIngress(source);
		}
		
		for (String destination : networkModel.getData().getRequiredForward(getLabel())) {
			addRequiredForward(destination);
		}
		
		this.egress.putAll(networkModel.getData().getRequiredEgress(getLabel()));
	}

	public void setData(NetworkData networkData) {
		this.networkData = networkData;
	}
	
	public void setFirstOctet(Integer firstOctet) {
		this.firstOctet = firstOctet;
	}

	public void setSecondOctet(Integer secondOctet) {
		this.secondOctet = secondOctet;
	}

	public void setThirdOctet(Integer thirdOctet) {
		this.thirdOctet = thirdOctet;
	}

	//This *must* be implemented by descendants
	public abstract void getNetworking();
	
	public InetAddress getIP() {
		return this.getNetworkIfaces().getIfaces().elementAt(0).getAddress();
	}
	
	public InterfaceModel getInterfaceModel() {
		return this.getNetworkIfaces();
	}
	
	public Vector<InterfaceData> getInterfaces() {
		return getNetworkIfaces().getIfaces();
	}
	
	public Vector<InetAddress> getAddresses() {
		Vector<InetAddress> addresses = new Vector<InetAddress>();
		
		for (InterfaceData iface : this.getInterfaces()) {
			addresses.add(iface.getAddress());
		}
		
		return addresses;
	}
	
	public Vector<String> getMacs() {
		Vector<String> macs = new Vector<String>();
		
		for (InterfaceData iface : this.getNetworkIfaces().getIfaces()) {
			if (iface.getMac() != null && iface.getGateway() != null) {
				macs.add(iface.getMac());
			}
		}
		
		return macs;
	}
	
	public Vector<InetAddress> getSubnets() {
		Vector<InetAddress> subnets = new Vector<InetAddress>();
		
		for (InterfaceData iface : this.getNetworkIfaces().getIfaces()) {
			if (iface.getSubnet() != null) { //This will only get routed interfaces
				subnets.add(iface.getSubnet());
			}
		}
		
		return subnets;
	}
	
	public Vector<InetAddress> getBroadcasts() {
		Vector<InetAddress> broadcasts = new Vector<InetAddress>();
		
		for (InterfaceData iface : this.getNetworkIfaces().getIfaces()) {
			broadcasts.add(iface.getBroadcast());
		}
		
		return broadcasts;
	}
	
	public Vector<InetAddress> getIPs() {
		Vector<InetAddress> ips = new Vector<InetAddress>();
		
		for (InterfaceData iface : this.getNetworkIfaces().getIfaces()) {
			ips.add(iface.getAddress());
		}
		
		return ips;
	}
	
	public Vector<InetAddress> getGateways() {
		Vector<InetAddress> gateways = new Vector<InetAddress>();
		
		for (InterfaceData iface : this.getNetworkIfaces().getIfaces()) {
			gateways.add(iface.getGateway());
		}
		
		return gateways;
	}
	
	public Integer getFirstOctet() {
		return this.firstOctet;
	}

	public Integer getSecondOctet() {
		return this.secondOctet;
	}

	public Integer getThirdOctet() {
		return this.thirdOctet;
	}
	
	public NetworkData getNetworkData() {
		return this.networkData;
	}

	public String getIngressChain() {
		return getHostname() + "_ingress";
	}
	
	public String getForwardChain() {
		return getHostname() + "_fwd";
	}

	public String getEgressChain() {
		return getHostname() + "_egress";
	}

	public String getHostname() {
		String invalidChars = "[^a-zA-Z0-9-]";
		String safeChars    = "_";
	
		return label.replaceAll(invalidChars, safeChars);
	}
	
	/**
	 * @return the networkIfaces
	 */
	public InterfaceModel getNetworkIfaces() {
		return networkIfaces;
	}

	/**
	 * @param networkIfaces the networkIfaces to set
	 */
	public void setNetworkIfaces(InterfaceModel networkIfaces) {
		this.networkIfaces = networkIfaces;
	}

	private void addRequiredIngress(String uri) {
		addRequiredIngress(uri, new Integer[] { 80, 443 });
	}

	void addRequiredIngress(String uri, Integer[] ports) {
		Set<Integer> extant = this.ingress.get(uri);

		if (extant == null) {
			extant = new HashSet<Integer>();
		}

		extant.addAll(Arrays.asList(ports));
		
		this.ingress.put(uri, extant);
	}
	
	public HashMap<String, Set<Integer>> getRequiredIngress() {
		return this.ingress;
	}

	public void addRequiredListen(Integer[] ports) {
		for (Integer port : ports ) {
			addRequiredListen(port);
		}
	}
	
	public void addRequiredListen(Integer port) { 
		this.listen.add(port);
	}
	
	public Vector<Integer> getRequiredListen() {
		return this.listen;
	}
	
	public void addRequiredEgress(String uri) {
		addRequiredEgress(uri, new Integer[] { 80, 443 });
	}
	
	void addRequiredEgress(String uri, Integer port) {
		addRequiredEgress(uri, new Integer[] {port});
	}

	public void addRequiredEgress(String uri, Integer[] ports) {
		Set<Integer> extant = this.egress.get(uri);
		
		if (extant == null) {
			extant = new HashSet<Integer>();
		}
		
		extant.addAll(Arrays.asList(ports));
		this.egress.put(uri, extant);
	}
	
	public HashMap<String, Set<Integer>> getRequiredEgress() {
		return this.egress;
	}
	
	public void addRequiredDnat(String server) {
		addRequiredDnat(server, new Integer[] { 80, 443 });
	}

	private void addRequiredDnat(String server, Integer[] ports) {
		Set<Integer> extant = this.dnat.get(server);

		if (extant == null) {
			extant = new HashSet<Integer>();
		}
		extant.addAll(Arrays.asList(ports));
		
		this.dnat.put(server, extant);
	}
	
	public HashMap<String, Set<Integer>> getRequiredDnat() {
		return this.dnat;
	}

	private void addRequiredForward(String destinationName) {
		addRequiredForward(destinationName, new Integer[] { 80, 443 });		
	}
	
	public void addRequiredForward(String destinationName, Integer port) {
		addRequiredForward(destinationName, new Integer[] {port});
	}
	
	private void addRequiredForward(String destinationName, Integer[] ports) {
		Set<Integer> extant = this.forward.get(destinationName);
		
		if (extant == null) {
			extant = new HashSet<Integer>();
		}
		extant.addAll(Arrays.asList(ports));
		
		this.forward.put(destinationName, extant);
	}
	
	public HashMap<String, Set<Integer>> getRequiredForward() {
		return this.forward;
	}
	
	protected abstract Vector<IUnit> getUnits();
}
