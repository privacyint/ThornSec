/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine;

import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.json.stream.JsonParsingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import core.StringUtils;
import core.data.machine.AMachineData.Encapsulation;
import core.data.machine.configuration.NetworkInterfaceData;
import core.exception.AThornSecException;
import core.exception.data.ADataException;
import core.exception.data.InvalidPortException;
import core.iface.IUnit;
import core.model.AModel;
import core.model.machine.configuration.NetworkInterfaceModel;
import core.model.network.NetworkModel;
import inet.ipaddr.HostName;

/**
 * This class represents a Machine on our network.
 *
 * This is where we stash our various networking rules
 */
public abstract class AMachineModel extends AModel {
	private Set<NetworkInterfaceModel> networkInterfaces;

	private final HostName domain;
	private final Set<String> cnames;

	private InternetAddress emailAddress;

	// Networking stuff
	private Integer firstOctet;
	private Integer secondOctet;
	private Integer thirdOctet;
	private Integer cidr;

	private Boolean throttled;

	private final Map<Encapsulation, Set<Integer>> listens;

	private Set<String> forwards;
	private Set<HostName> ingresses;
	private Set<HostName> egresses;

	private final Hashtable<String, Set<Integer>> dnat;

	AMachineModel(String label, NetworkModel networkModel)
			throws AddressException, JsonParsingException, ADataException, IOException {
		super(label, networkModel);

		this.emailAddress = getNetworkModel().getData().getEmailAddress(getLabel());

		this.domain = getNetworkModel().getData().getDomain(getLabel());
		this.cnames = getNetworkModel().getData().getCNAMEs(getLabel());

		this.networkInterfaces = new LinkedHashSet<>();
		if (getNetworkModel().getData().getNetworkInterfaces(getLabel()) != null) {
			for (final NetworkInterfaceData ifaceData : getNetworkModel().getData().getNetworkInterfaces(getLabel())) {
				final NetworkInterfaceModel iface = ifaceDataToModel(ifaceData);
				addNetworkInterface(iface);
			}
		}

		this.firstOctet = 10;
		this.secondOctet = null;
		this.thirdOctet = null;

		this.throttled = getNetworkModel().getData().isThrottled(getLabel());

		this.listens = getNetworkModel().getData().getListens(getLabel());
		this.ingresses = getNetworkModel().getData().getIngresses(getLabel());
		this.egresses = getNetworkModel().getData().getEgresses(getLabel());
		this.forwards = getNetworkModel().getData().getForwards(getLabel());
		this.dnat = new Hashtable<>();
	}

	final private NetworkInterfaceModel ifaceDataToModel(NetworkInterfaceData ifaceData) {
		final NetworkInterfaceModel ifaceModel = new NetworkInterfaceModel(getLabel(), getNetworkModel());

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

	public final void addNetworkInterface(NetworkInterfaceModel ifaceModel) {
		if (this.networkInterfaces == null) {
			this.networkInterfaces = new LinkedHashSet<>();
		}

		this.networkInterfaces.add(ifaceModel);
	}

	public final Set<NetworkInterfaceModel> getNetworkInterfaces() {
		return this.networkInterfaces;
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
		Set<Integer> listening = this.listens.get(enc);

		if (listening == null) {
			listening = new HashSet<>();
		}

		for (final Integer port : ports) {
			if ((port < 1) || (port > 65535)) {
				throw new InvalidPortException();
			}

			listening.add(port);
		}

		this.listens.put(enc, listening);
	}

	public final Map<Encapsulation, Set<Integer>> getListens() {
		return this.listens;
	}

	public final void addEgress(String... egresses) {
		for (final String egress : egresses) {
			addEgress(new HostName(egress));
		}
	}

	public final void addEgress(HostName... egresses) {
		if (this.egresses == null) {
			this.egresses = new LinkedHashSet<>();
		}

		for (final HostName egress : egresses) {
			this.egresses.add(egress);
		}

	}

	public final Set<HostName> getEgresses() {
		return this.egresses;
	}

	public final Hashtable<String, Set<Integer>> getRequiredDnat() {
		return this.dnat;
	}

	public final void addForward(String... destinations) {
		if (this.forwards == null) {
			this.forwards = new HashSet<>();
		}

		for (final String destination : destinations) {
			this.forwards.add(destination);
		}
	}

	public final Set<String> getForwards() {
		return this.forwards;
	}

	public final InternetAddress getEmailAddress() {
		return this.emailAddress;
	}

	public final void setEmailAddress(InternetAddress emailAddress) {
		this.emailAddress = emailAddress;
	}

	public final Set<String> getCNAMEs() {
		return this.cnames;
	}

	public final void putCNAME(String... cnames) {
		for (final String cname : cnames) {
			this.cnames.add(cname);
		}
	}

	public HostName getDomain() {
		return this.domain;
	}

	public final Boolean isThrottled() {
		return this.throttled;
	}

	public final void setIsThrottled(Boolean throttled) {
		this.throttled = throttled;
	}

	public abstract Set<IUnit> getUnits() throws AThornSecException;

	public String getIP() {
		// TODO Auto-generated method stub
		return null;
	}

	public void addDnat(String backend) {
		// TODO Auto-generated method stub

	}
}
