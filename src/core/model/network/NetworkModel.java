/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.network;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import core.data.machine.AMachineData;
import core.data.machine.AMachineData.MachineType;
import core.data.machine.InternalDeviceData;
import core.data.machine.ExternalDeviceData;
import core.data.machine.HypervisorData;
import core.data.machine.ServerData;
import core.data.machine.ServiceData;
import core.data.machine.UserDeviceData;
import core.data.network.NetworkData;
import core.exception.AThornSecException;
import core.exception.data.InvalidIPAddressException;
import core.exception.data.NoValidUsersException;
import core.exception.runtime.InvalidMachineModelException;
import core.exception.runtime.InvalidServerModelException;
import core.exec.ManageExec;
import core.exec.network.OpenKeePassPassphrase;
import core.iface.IUnit;
import core.model.machine.AMachineModel;
import core.model.machine.ExternalOnlyDeviceModel;
import core.model.machine.HypervisorModel;
import core.model.machine.InternalOnlyDeviceModel;
import core.model.machine.ServerModel;
import core.model.machine.ServiceModel;
import core.model.machine.UserDeviceModel;
import core.model.machine.configuration.networking.MACVLANModel;
import core.model.machine.configuration.networking.NetworkInterfaceModel;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.IncompatibleAddressException;

/**
 * Below the ThornsecModel comes the getNetworkModel().
 *
 * This model represents a given network;
 */
public class NetworkModel {
	private final String label;
	private NetworkData data;
	private Map<String, UserModel> users;
	private Map<String, AMachineModel> machines;
	private Map<String, Collection<IUnit>> networkUnits;

	private Map<MachineType, IPAddress> defaultSubnets;

	NetworkModel(String label) {
		this.label = label;

		this.users = new LinkedHashMap<>();
		
		this.machines = null;
		this.networkUnits = null;
		
		populateNetworkDefaults();
	}

	private void populateNetworkDefaults() {
		buildDefaultSubnets();
	}

	private void buildDefaultSubnets() {
		this.defaultSubnets = new LinkedHashMap<>();

		try {
			this.defaultSubnets.put(MachineType.USER, new IPAddressString("172.16.0.0/16").toAddress());
			this.defaultSubnets.put(MachineType.SERVER, new IPAddressString("10.0.0.0/8").toAddress());
			this.defaultSubnets.put(MachineType.ADMIN, new IPAddressString("172.20.0.0/16").toAddress());
			this.defaultSubnets.put(MachineType.INTERNAL_ONLY, new IPAddressString("172.24.0.0/16").toAddress());
			this.defaultSubnets.put(MachineType.EXTERNAL_ONLY, new IPAddressString("172.28.0.0/16").toAddress());
			this.defaultSubnets.put(MachineType.GUEST, new IPAddressString("172.32.0.0/16").toAddress());
			this.defaultSubnets.put(MachineType.VPN, new IPAddressString("172.36.0.0/16").toAddress());
		} catch (AddressStringException | IncompatibleAddressException e) {
			// Well done, you shouldn't have been able to get here!
			e.printStackTrace();
		}

	}

	final public String getLabel() {
		return this.label;
	}

	final public Map<String, UserModel> getUsers() {
		return this.users;
	}

	/**
	 * Initialises the various models across our network, building and initialising
	 * all of our machines
	 *
	 * @throws AThornSecException
	 */
	void init() throws AThornSecException {

		buildExternalOnlyDevices();
		buildInternalOnlyDevices();
		buildUserDevices();
		buildUsers();
		buildServers();
		
		// Now, step through our devices, initialise them, and run through their units.
		for (final AMachineModel device : getMachines(MachineType.DEVICE).values()) {
			device.init();
			putUnits(label, device.getUnits());
		}

		// We want to initialise the whole network first before we start getting units
		for (final AMachineModel server : getMachines(MachineType.SERVER).values()) {
			server.init();
		}

		// We want to separate the Routers out; they need to be the very last thing, as
		// it relies on everythign else being inited & configured
		for (final AMachineModel server : getMachines(MachineType.SERVER).values()) {
			if (!server.isType(MachineType.ROUTER)) {
				putUnits(server.getLabel(), server.getUnits());
			}
		}

		// Finally, let's build our Routers
		for (final AMachineModel router : getMachines(MachineType.ROUTER).values()) {
			putUnits(router.getLabel(), router.getUnits());
		}
	}

	final public UserModel getConfigUserModel() throws NoValidUsersException {
		return this.users.get(getData().getUser());
	}

	private void buildUsers() throws AThornSecException {
		for (String username : getData().getUsers().keySet()) {
			this.users.put(username, new UserModel(getData().getUsers().get(username)));
		}
	}

	private void buildInternalOnlyDevices() throws AThornSecException {
		final Optional<Map<String, AMachineData>> internals = getData().getMachines(MachineType.INTERNAL_ONLY);
		if (internals.isPresent()) {
			for (String label : internals.get().keySet()) {
				addMachine(new InternalOnlyDeviceModel((InternalDeviceData)getData().getMachineData(label), this));
			}
		}
	}

	private void buildUserDevices() throws AThornSecException {
		final Optional<Map<String, AMachineData>> users = getData().getMachines(MachineType.USER);
		if (users.isPresent()) {
			for (String label : users.get().keySet()) {
				addMachine(new UserDeviceModel((UserDeviceData)getData().getMachineData(label), this));
			}
		}
	}

	private void buildServers() throws AThornSecException {
		final Optional<Map<String, AMachineData>> servers = getData().getMachines(MachineType.SERVER);
		if (servers.isEmpty()) {
			return;
		}

		for (AMachineData serverData : servers.get().values()) {
			if (serverData.isType(MachineType.SERVICE)) {
				addMachine(new ServiceModel((ServiceData)serverData, this));
			}
			else if (serverData.isType(MachineType.HYPERVISOR)) {
				addMachine(new HypervisorModel((HypervisorData)serverData, this));
			}
			else {
				addMachine(new ServerModel((ServerData)serverData, this));
			}
		}



		for (AMachineModel service : getMachines(MachineType.SERVICE).values()) {
			String hypervisorLabel = ((ServiceData)getData().getMachineData(service.getLabel()))
										.getHypervisor()
										.getLabel();
			
			((ServiceModel)service).setHypervisor((HypervisorModel) getMachineModel(hypervisorLabel));
		}
	}

	private void buildExternalOnlyDevices() throws AThornSecException {
		final Optional<Map<String, AMachineData>> externals = getData().getMachines(MachineType.EXTERNAL_ONLY);
		if (externals.isEmpty()) {
			return;
		}

		for (String label : externals.get().keySet()) {
			addMachine(new ExternalOnlyDeviceModel((ExternalDeviceData)getData().getMachineData(label), this));
		}
	}

	private void addMachine(AMachineModel machine) {
		if (this.machines == null) {
			this.machines = new LinkedHashMap<>();
		}
		this.machines.put(machine.getLabel(), machine);
	}

	private void putUnits(String label, Collection<IUnit> units) {
		if (this.networkUnits == null) {
			this.networkUnits = new LinkedHashMap<>();
		}

		this.networkUnits.put(label, units);
	}

	public Collection<NetworkInterfaceModel> getNetworkInterfaces(String machine) throws InvalidMachineModelException {
		Collection<NetworkInterfaceModel> interfaces = getMachineModel(machine).getNetworkInterfaces();

		if (interfaces == null) {
			interfaces = new ArrayList<>();
		}

		return interfaces;
	}

	/**
	 * @return the whole network. Be aware that you will have to cast the values
	 *         from this method; you are far better to use one of the specialised
	 *         methods
	 */
	public final Map<String, AMachineModel> getMachines() {
		return this.machines;
	}

	/**
	 * @param type
	 * @return A map of all machines of a given type
	 */
	public Map<String, AMachineModel> getMachines(MachineType type) {
		Map<String, AMachineModel> machines = getMachines().entrySet()
				.stream()
				.filter(kvp -> kvp.getValue().isType(type))
				.collect(Collectors.toMap(
						kvp -> kvp.getKey(),
						kvp -> kvp.getValue()
				));
	
		return machines;
	}


	/**
	 * @return A specific machine model.
	 */
	public final AMachineModel getMachineModel(String machine) throws InvalidMachineModelException {
		if (getMachines().containsKey(machine)) {
			return machines.get(machine);
		}

		throw new InvalidMachineModelException(machine + " is not a machine on your network");
	}

	public final void auditNonBlock(String server, OutputStream out, InputStream in, boolean quiet) throws InvalidMachineModelException {
		ManageExec exec = null;
		try {
			exec = getManageExec(server, "audit", out, quiet);
		} catch (InvalidServerModelException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (exec != null) {
			exec.manage();
		}
	}

	public final void auditAll(OutputStream out, InputStream in, boolean quiet) throws InvalidMachineModelException {
		for (final String server : getMachines(MachineType.SERVER).keySet()) {
			ManageExec exec = null;
			try {
				exec = getManageExec(server, "audit", out, quiet);
			} catch (InvalidServerModelException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (exec != null) {
				exec.manage();
			}
		}
	}

	public final void configNonBlock(String server, OutputStream out, InputStream in) throws IOException, InvalidMachineModelException {
		final ManageExec exec = getManageExec(server, "config", out, false);
		if (exec != null) {
			exec.manage();
		}
	}

	public final void dryrunNonBlock(String server, OutputStream out, InputStream in) throws IOException, InvalidMachineModelException {
		final ManageExec exec = getManageExec(server, "dryrun", out, false);
		if (exec != null) {
			exec.manage();
		}
	}

	private final ManageExec getManageExec(String server, String action, OutputStream out, boolean quiet) throws IOException, InvalidMachineModelException {
		// need to do a series of local checks eg known_hosts or expected
		// fingerprint
		final OpenKeePassPassphrase pass = new OpenKeePassPassphrase((ServerModel)getMachineModel(server));

		final String audit = getScript(server, action, quiet);

		if (action.equals("dryrun")) {
			try {
				final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
				final String filename = server + "_" + dateFormat.format(new Date()) + ".sh";
				final Writer wr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "UTF8"));
				wr.write(audit);
				wr.flush();
				wr.close();
			} catch (final FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		if (pass.isADefaultPassphrase()) {
			System.out.println("FAIL: no password in keychain for " + server);
			System.out.println("Using the default password instead (this almost certainly won't work!)");
			return null;
		}

		// ManageExec exec = new ManageExec(this.getData().getUser(),
		// pass.getPassphrase(), serverModel.getIP(), this.getData().getSSHPort(server),
		// audit, out);
		final ManageExec exec = new ManageExec(((ServerModel)getMachineModel(server)), this, audit, out);
		return exec;
	}

	private String getScript(String server, String action, boolean quiet) {
		System.out.println("=======================" + getLabel() + ":" + server + "==========================");
		String line = getHeader(server, action) + "\n";
		final Collection<IUnit> units = this.networkUnits.get(server);
		for (final IUnit unit : units) {
			line += "#============ " + unit.getLabel() + " =============\n";
			line += getText(action, unit, quiet) + "\n";
		}
		line += getFooter(server, action);
		return line;
	}

	private String getText(String action, IUnit unit, boolean quiet) {
		String line = "";
		if (action.equals("audit")) {
			line = unit.genAudit(quiet);
		} else if (action.equals("config")) {
			line = unit.genConfig();
		} else if (action.equals("dryrun")) {
			line = unit.genConfig();
			// line = unit.genDryRun();
		}
		return line;
	}

	private String getHeader(String server, String action) {
		String line = "#!/bin/bash\n";
		line += "\n";
		line += "hostname=$(hostname);\n";
		line += "proceed=1;\n";
		line += "\n";
		line += "echo \"Started " + action + " ${hostname} with config label: " + server + "\"\n";
		line += "pass=0; fail=0; fail_string=;";
		return line;
	}

	private String getFooter(String server, String action) {
		String line = "echo \"pass=$pass fail=$fail failed:$fail_string\"\n\n";
		line += "\n";
		line += "echo \"Finished " + action + " ${hostname} with config label: " + server + "\"";
		return line;
	}

	public void setData(NetworkData data) {
		this.data = data;
	}

	public NetworkData getData() {
		return this.data;
	}

	public String getKeePassDBPassphrase() {
		return null;
	}

	public String getKeePassDBPath(String server) throws URISyntaxException {
		return getData().getKeePassDB(server);
	}

	public String getDomain() {
		return getData().getDomain().orElse("lan");
	}

	public Optional<UserModel> getUser(String username) {
		return Optional.ofNullable(this.users.get(username));
	}

	public IPAddress getSubnet(MachineType vlan) throws InvalidIPAddressException {
		try {
			return getData().getSubnet(vlan)
					.orElse(defaultSubnets.get(vlan));
		} catch (IncompatibleAddressException e) {
			throw new InvalidIPAddressException(e.getLocalizedMessage());
		}
	}

	public Map<MachineType, IPAddress> getSubnets() {
		if (getData().getSubnets().isEmpty()) {
			return this.defaultSubnets;
		}
		
		return Stream.of(getData().getSubnets().get(), this.defaultSubnets)
					.flatMap(map -> map.entrySet().stream())
					.collect(Collectors.toMap(
						Map.Entry::getKey,
						Map.Entry::getValue,
						(dataValue, defaultValue) -> dataValue)
					);
	}
}
