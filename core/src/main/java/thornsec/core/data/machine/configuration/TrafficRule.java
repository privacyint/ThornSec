package core.data.machine.configuration;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import core.exception.data.InvalidPortException;
import inet.ipaddr.HostName;

public class TrafficRule {
	public enum Encapsulation {
		UDP, TCP
	}

	public enum Table {
		DNAT, FORWARD, INGRESS, EGRESS
	}

	private Encapsulation encapsulation;
	private Table table;
	private String source;
	private Set<HostName> destinations;
	private Set<Integer> ports;

	public TrafficRule(Encapsulation encapsulation, Table table, String source, Set<HostName> destinations, Set<Integer> ports) throws InvalidPortException {
		this.ports = new LinkedHashSet<>();
		this.destinations = new LinkedHashSet<>();

		this.encapsulation = encapsulation;
		this.setTable(table);
		this.setSource(source);
		this.addDestinations(destinations);
		this.addPorts(ports);
	}

	/**
	 * Deafult traffic rule to destination, on TCP port 443
	 * @param destination
	 * @throws InvalidPortException
	 */
	public TrafficRule(String source, HostName destination, Table table) throws InvalidPortException {
		this(Encapsulation.TCP, table, source, new HashSet<>(Arrays.asList(destination)), new HashSet<>(Arrays.asList(443)));
	}

	public TrafficRule() throws InvalidPortException {
		this(null, null, null, new LinkedHashSet<>(), new LinkedHashSet<>());
	}

	/**
	 * @return the encapsulation
	 */
	public Encapsulation getEncapsulation() {
		return this.encapsulation;
	}

	/**
	 * @param encapsulation the encapsulation to set
	 */
	public void setEncapsulation(Encapsulation encapsulation) {
		this.encapsulation = encapsulation;
	}

	public void addDestinations(Collection<HostName> collection) {
		this.destinations.addAll(collection);
	}

	public void addDestination(HostName destination) {
		this.destinations.add(destination);
	}

	/**
	 * @return the ports
	 */
	public Set<Integer> getPorts() {
		return this.ports;
	}

	/**
	 * @param ports the ports to set
	 * @throws InvalidPortException 
	 */
	public void addPorts(Integer... ports) throws InvalidPortException {
		if (ports == null) {
			return;
		}

		for (Integer port : ports) {
			if (((port < 0)) || ((port > 65535))) {
				throw new InvalidPortException(port);
			}

			this.ports.add(port);
		}
	}

	/**
	 * @param ports the ports to set
	 * @throws InvalidPortException 
	 */
	public void addPorts(Set<Integer> ports) throws InvalidPortException {
		addPorts(ports.toArray(Integer[]::new));
	}

	/**
	 * @return the destinations
	 */
	public Set<HostName> getDestinations() {
		return this.destinations;
	}

	public Table getTable() {
		return this.table;
	}

	public void setTable(Table table) {
		this.table = table;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}
}
