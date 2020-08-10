/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine;

//import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import com.metapossum.utils.scanner.reflect.ClassesInPackageScanner;
import core.StringUtils;
import core.data.machine.AMachineData;
import core.data.machine.AMachineData.MachineType;
import core.data.machine.ServerData;
import core.data.machine.configuration.NetworkInterfaceData;
import core.data.machine.configuration.NetworkInterfaceData.Inet;
import core.data.machine.configuration.TrafficRule;
import core.exception.AThornSecException;
import core.exception.runtime.InvalidProfileException;
import core.iface.IUnit;
import core.model.AModel;
import core.model.machine.configuration.networking.DHCPClientInterfaceModel;
import core.model.machine.configuration.networking.NetworkInterfaceModel;
import core.model.machine.configuration.networking.StaticInterfaceModel;
import core.model.network.NetworkModel;
import core.profile.AProfile;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IncompatibleAddressException;
import inet.ipaddr.MACAddressString;
import inet.ipaddr.mac.MACAddress;
import profile.type.AMachine;
import profile.type.Device;
import profile.type.Server;
import profile.type.Service;

/**
 * This class represents a Machine on our network.
 * 
 * Everything on our network is a descendant from this class, 
 *
 * This is where we stash our various networking rules
 */
public abstract class AMachineModel extends AModel {
	private Set<NetworkInterfaceModel> networkInterfaces;

	private HostName domain;
	private Set<String> cnames;

	private InternetAddress emailAddress;

	private Map<MachineType, AProfile> types;
	
	private Boolean throttled;

	private Set<TrafficRule> firewallRules;
	private Set<IPAddress> externalIPs;

	AMachineModel(AMachineData myData, NetworkModel networkModel) throws AThornSecException {
		super(myData, networkModel);

		setTypesFromData(myData);
		setEmailFromData(myData);
		setNICsFromData(myData);
		setDomainFromData(myData);
		setCNAMEsFromData(myData);
		setExternalIPsFromData(myData);
		setFirewallFromData(myData);
	}

	private void setFirewallFromData(AMachineData myData) {
		this.firewallRules = myData.getTrafficRules();
		
		//assertNotNull(this.firewallRules);
	}
	
	public void addFirewallRule(TrafficRule rule) {
		//assertNotNull(rule);
		
		this.firewallRules.add(rule);
	}

	private void setExternalIPsFromData(AMachineData myData) {
		this.externalIPs = myData.getExternalIPs();	
		
		//assertNotNull(this.externalIPs);
	}

	private void setCNAMEsFromData(AMachineData myData) {
		this.cnames = myData.getCNAMEs().orElse(new LinkedHashSet<>());
	}

	private void setDomainFromData(AMachineData myData) {
		this.domain = myData.getDomain().orElse(new HostName("lan"));
	}

	/**
	 * Set up and initialise our various NICs as set from our Data
	 * @param myData
	 * @throws AThornSecException
	 */
	private void setNICsFromData(AMachineData myData) throws AThornSecException {
		if (myData.getNetworkInterfaces().isEmpty()) {
			return;
		}
		
		for (NetworkInterfaceData nicData : myData.getNetworkInterfaces().get().values()) {
			NetworkInterfaceModel nicModel = buildNICFromData(nicData);
			nicModel.init();
			this.addNetworkInterface(nicModel);
		}
	}

	private NetworkInterfaceModel buildNICFromData(NetworkInterfaceData ifaceData) throws AThornSecException {
		NetworkInterfaceModel nicModel = null;
		switch ((ifaceData.getInet() != null)
					? ifaceData.getInet()
					: Inet.STATIC)
		{
			case STATIC:
				nicModel = new StaticInterfaceModel(ifaceData, getNetworkModel());
				break;
			case DHCP:
				nicModel = new DHCPClientInterfaceModel(ifaceData, getNetworkModel());
				break;
//			case PPP:
//				nicModel = new PPPClientInterfaceModel(label);
//				break;
			default:
				break;
		}

		return nicModel;
	}

	private void setEmailFromData(AMachineData myData) {
		try {
			this.emailAddress = myData.getEmailAddress()
					.orElse(new InternetAddress(getLabel() + "@" + getDomain()));
		} catch (AddressException e) {
			;; // You should not be able to get here. 
		}
	}

	private void setTypesFromData(AMachineData myData) {
		this.types = new LinkedHashMap<>();
		
		myData.getTypes().forEach(type -> {
			addType(type);
		});
	}

	public final void addType(MachineType type) {
//		//assertNotNull(type);
		
		switch (type) {
			case DEVICE:
				addType(type, new Device((ADeviceModel)this));
				break;
			case SERVER:
				addType(type, new Server((ServerModel)this));
				break;
			case SERVICE:
				addType(type, new Service((ServiceModel)this));
				break;
			default:
				break;
		}
	}
	
	public final void addType(MachineType type, AMachine profile) {
		this.types.put(type, profile);
	}

	public final void addNetworkInterface(NetworkInterfaceModel ifaceModel) {
		if (this.networkInterfaces == null) {
			this.networkInterfaces = new LinkedHashSet<>();
		}
		
		this.networkInterfaces.add(ifaceModel);
	}

	public final Collection<NetworkInterfaceModel> getNetworkInterfaces() {
		return this.networkInterfaces;
	}

	public final Collection<IPAddress> getExternalIPs() {
		return this.externalIPs;
	}

	public final InternetAddress getEmailAddress() {
		return this.emailAddress;
	}

	public final void setEmailAddress(InternetAddress emailAddress) {
		//assertNotNull(emailAddress);
		
		this.emailAddress = emailAddress;
	}

	public final Optional<Set<String>> getCNAMEs() {
		return Optional.ofNullable(this.cnames);
	}

	public final void putCNAME(String... cnames) {
		//assertNotNull(cnames);
		
		if (this.cnames == null) {
			this.cnames = new LinkedHashSet<>();
		}
		
		for (final String cname : cnames) {
			this.cnames.add(cname);
		}
	}

	public HostName getDomain() {
		//assertNotNull(this.domain);
		
		return this.domain;
	}

	public String getHostName() {
		return StringUtils.stringToAlphaNumeric(getLabel(), "-");
	}
	
	public final Boolean isThrottled() {
		//assertNotNull(this.throttled);
		
		return this.throttled;
	}

	public final void setIsThrottled(Boolean throttled) {
		//assertNotNull(throttled);
		
		this.throttled = throttled;
	}

	public Collection<IUnit> getUnits() throws AThornSecException {
		final Collection<IUnit> typesUnits = new ArrayList<>();

		for (final AProfile type : getTypes().values()) {
			typesUnits.addAll(type.getUnits());
		}

		return typesUnits;
	}

	public Collection<IPAddress> getIPs() {
		final Collection<IPAddress> ips = new ArrayList<>();

		for (final NetworkInterfaceModel nic : getNetworkInterfaces()) {
			if (nic.getAddresses().isEmpty()) {
				continue;
			}
			
			for (final IPAddress ip : nic.getAddresses().get()) {
				if ((getExternalIPs() != null) && getExternalIPs().contains(ip)) {
					continue;
				}
				
				ips.add(ip);
			}
		}
		
		return ips;
	}

	public MACAddress generateMAC(String iface) {
		final String name = getLabel() + iface;

		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-512");
		} catch (final NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		md.update(name.getBytes());

		final byte byteData[] = md.digest();
		final StringBuffer hashCodeBuffer = new StringBuffer();
		for (final byte element : byteData) {
			hashCodeBuffer.append(Integer.toString((element & 0xff) + 0x100, 16).substring(1));

			if (hashCodeBuffer.length() == 6) {
				break;
			}
		}

		final String address = "080027" + hashCodeBuffer.toString();

		try {
			return new MACAddressString(address).toAddress();
		} catch (final AddressStringException | IncompatibleAddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public final void addType(MachineType type, AProfile profile) {
		if (this.types == null) {
			this.types = new LinkedHashMap<>();
		}

		this.types.put(type, profile);
	}
	
	public final Map<MachineType, AProfile> getTypes() {
		return this.types;
	}

	public final Boolean isType(MachineType type) {
		return getType(type) != null;
	}

	public AProfile getType(MachineType type) {
		return this.types.getOrDefault(type, null);
	}
	
}
