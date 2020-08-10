package core.data.machine.configuration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.LinkedHashSet;
import java.util.Set;
import core.exception.data.InvalidPortException;

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
	private String destination;
	private Set<Integer> ports;
	
	public TrafficRule(Encapsulation encapsulation, Table table, String source, String destination, Integer... ports) throws InvalidPortException {
		this.ports = new LinkedHashSet<>();
		
		this.encapsulation = encapsulation;
		this.setTable(table);
		this.setSource(source);
		this.destination = destination;
		this.addPorts(ports);
	}
	
	/**
	 * Deafult traffic rule to destination, on TCP port 443
	 * @param destination
	 * @throws InvalidPortException
	 */
	public TrafficRule(String source, String destination, Table table) throws InvalidPortException {
		this(Encapsulation.TCP, table, source, destination, 443);
	}
	
	public TrafficRule() throws InvalidPortException {
		this(null, null, null, null, (Integer[])null);
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
	
	/**
	 * @param destination the destination to set
	 */
	public void setDestination(String destination) {
		this.destination = destination;
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
	 * @return the destination
	 */
	public String getDestination() {
		//assertNotNull(this.destination);
		
		return this.destination;
	}
	
	public Table getTable() {
		//assertNotNull(this.table);

		return this.table;
	}

	public void setTable(Table table) {
		//assertNotNull(table);
		
		this.table = table;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}
}
