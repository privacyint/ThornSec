/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.mail.internet.InternetAddress;

import core.StringUtils;
import core.data.machine.AMachineData.Encapsulation;
import core.data.machine.configuration.NetworkInterfaceData;
import core.exception.AThornSecException;
import core.exception.data.InvalidPortException;
import core.exception.data.machine.InvalidMachineException;
import core.exception.runtime.InvalidNICException;
import core.iface.IUnit;
import core.model.AModel;
import core.model.machine.configuration.NetworkInterfaceModel;
import core.model.network.NetworkModel;
import inet.ipaddr.HostName;

public abstract class AMachineModel extends AModel {

	private final Hashtable<String, NetworkInterfaceModel> lanIfaces;
	private final Hashtable<String, NetworkInterfaceModel> wanIfaces;

	private final HostName fqdn;
	private final Set<HostName> cnames;

	private InternetAddress emailAddress;

	// Networking stuff
	private Integer firstOctet;
	private Integer secondOctet;
	private Integer thirdOctet;
	private Integer cidr;

	private Boolean throttled;

	private final Hashtable<Encapsulation, Set<HostName>> listens;

	private Set<HostName> forwards;
	private Set<HostName> ingresses;
	private final Set<HostName> egresses;

	private final Hashtable<String, Set<Integer>> dnat;

	AMachineModel(String label, NetworkModel networkModel) throws InvalidMachineException {
		super(label, networkModel);

		this.emailAddress = networkModel.getData().getEmailAddress(label);

		this.fqdn = networkModel.getData().getFQDN(this.getLabel());
		this.cnames = networkModel.getData().getCnames(this.getLabel());

		this.lanIfaces = new Hashtable<>();
		this.wanIfaces = new Hashtable<>();

		this.firstOctet = null;
		this.secondOctet = null;
		this.thirdOctet = null;

		this.throttled = networkModel.getData().getMachineIsThrottled(this.getLabel());

		this.listens = networkModel.getData().getListens(this.getLabel());
		this.ingresses = networkModel.getData().getIngresses(this.getLabel());
		this.egresses = networkModel.getData().getEgresses(this.getLabel());
		this.forwards = networkModel.getData().getForwards(this.getLabel());
		this.dnat = null;
	}

	protected void buildIfaces() throws InvalidNICException, InvalidMachineException {
		if ((this.firstOctet == null) || (this.secondOctet == null) || (this.thirdOctet == null)) {
			throw new InvalidNICException();
		}

		for (final NetworkInterfaceData ifaceData : this.networkModel.getData().getLanIfaces(this.getLabel())) {
			this.addLANInterface(ifaceData.getIface(), ifaceDataToModel(ifaceData));
		}
		for (final NetworkInterfaceData ifaceData : this.networkModel.getData().getWanIfaces(this.getLabel())) {
			this.addWANInterface(ifaceData.getIface(), ifaceDataToModel(ifaceData));
		}
	}

	final private NetworkInterfaceModel ifaceDataToModel(NetworkInterfaceData ifaceData) {
		final NetworkInterfaceModel ifaceModel = new NetworkInterfaceModel(ifaceData.getIface(), this.networkModel);

		ifaceModel.setHost(ifaceData.getHost());
		ifaceModel.setAddress(ifaceData.getAddress());
		ifaceModel.setBridgePorts(ifaceData.getBridgePorts());
		ifaceModel.setBroadcast(ifaceData.getBroadcast());
		ifaceModel.setComment(ifaceData.getComment());
		ifaceModel.setGateway(ifaceData.getGateway());
		ifaceModel.setInet(ifaceData.getInet());
		ifaceModel.setMac(ifaceData.getMAC());
		ifaceModel.setNetmask(ifaceData.getNetmask());
		ifaceModel.setSubnet(ifaceData.getSubnet());

		return ifaceModel;
	}

	public final Integer getFirstOctet() {
		return this.firstOctet;
	}

	public final Integer getSecondOctet() {
		return this.secondOctet;
	}

	public final Integer getThirdOctet() {
		return this.thirdOctet;
	}

	public final void setFirstOctet(Integer firstOctet) {
		this.firstOctet = firstOctet;
	}

	public final void setSecondOctet(Integer secondOctet) {
		this.secondOctet = secondOctet;
	}

	public final void setThirdOctet(Integer thirdOctet) {
		this.thirdOctet = thirdOctet;
	}

	public final Integer getCIDR() {
		return this.cidr;
	}

	public final void setCIDR(Integer cidr) {
		this.cidr = cidr;
	}

	public final void addLANInterface(String iface, NetworkInterfaceModel ifaceModel) {
		this.lanIfaces.put(iface, ifaceModel);
	}

	final protected void addWANInterface(String iface, NetworkInterfaceModel ifaceModel) {
		this.wanIfaces.put(iface, ifaceModel);
	}

	public final NetworkInterfaceModel getLANInterface(String iface) {
		return this.lanIfaces.get(iface);
	}

	public final Hashtable<String, NetworkInterfaceModel> getLANInterfaces() {
		return this.lanIfaces;
	}

	public final Hashtable<String, NetworkInterfaceModel> getWANInterfaces() {
		return this.wanIfaces;
	}

	public final String getIngressChain() {
		return StringUtils.stringToAlphaNumeric(getLabel(), "_") + "_ingress";
	}

	public final String getForwardChain() {
		return StringUtils.stringToAlphaNumeric(getLabel(), "_") + "_fwd";
	}

	public final String getEgressChain() {
		return StringUtils.stringToAlphaNumeric(getLabel(), "_") + "_egress";
	}

	public final void addIngress(String... sources) {
		for (final String source : sources) {
			addIngress(new HostName(source));
		}
	}

	public final void addIngress(HostName... sources) {
		if (this.ingresses == null) {
			this.ingresses = new HashSet<>();
		}

		for (final HostName source : sources) {
			this.ingresses.add(source);
		}
	}

	public final Set<HostName> getIngresses() {
		return this.ingresses;
	}

	public final void addListen(Encapsulation enc, Integer... ports) throws InvalidPortException {
		Set<HostName> listening = this.listens.get(enc);

		if (listening == null) {
			listening = new HashSet<>();
		}

		for (final Integer port : ports) {
			if ((port < 1) || (port > 65535)) {
				throw new InvalidPortException();
			}

			listening.add(new HostName("*:" + port));
		}

		this.listens.put(enc, listening);
	}

	public final Hashtable<Encapsulation, Set<HostName>> getListens() {
		return this.listens;
	}

	public final void addEgress(String egress) {
		addEgress(new HostName(egress));
	}

	public final void addEgress(HostName egress) {
		// @TODO
		// HostName extant = this.egresses.
		// this.egresses.put(destination, new HashSet<Integer>( Arrays.asList(ports) ));
	}

	public final Set<HostName> getEgresses() {
		return this.egresses;
	}

	public final Hashtable<String, Set<Integer>> getRequiredDnat() {
		return this.dnat;
	}

	public final void addForward(HostName destination) {
		if (this.forwards == null) {
			this.forwards = new HashSet<>();
		}

		this.forwards.add(destination);
	}

	public final Set<HostName> getForwards() {
		return this.forwards;
	}

	public final InternetAddress getEmailAddress() {
		return this.emailAddress;
	}

	public final void setEmailAddress(InternetAddress emailAddress) {
		this.emailAddress = emailAddress;
	}

	public final HostName getFQDN() {
		return this.fqdn;
	}

	public final Set<HostName> getCNames() {
		return this.cnames;
	}

	public final void addCName(HostName... cnames) {
		for (final HostName cname : cnames) {
			this.cnames.add(cname);
		}
	}

	public final Boolean getIsThrottled() {
		return this.throttled;
	}

	public final void setIsThrottled(Boolean throttled) {
		this.throttled = throttled;
	}

	protected abstract Set<IUnit> getUnits() throws AThornSecException;

	public String getIP() {
		// TODO Auto-generated method stub
		return null;
	}

	public void addDnat(String backend) {
		// TODO Auto-generated method stub

	}
}
