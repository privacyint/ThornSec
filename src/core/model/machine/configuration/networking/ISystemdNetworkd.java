/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package core.model.machine.configuration.networking;

import java.util.Collection;

import core.unit.fs.FileUnit;
import inet.ipaddr.IPAddress;

/**
 * Represents a Systemd-networkd network interface.
 *
 * See
 * {@link https://www.freedesktop.org/software/systemd/man/systemd-networkd.service.html}
 *
 */
public interface ISystemdNetworkd {

	/**
	 * Build a Systemd-networkd .network file for this NIC.
	 *
	 * See
	 * {@link https://www.freedesktop.org/software/systemd/man/systemd.network.html}
	 *
	 * @return FileUnit in /etc/systemd/network/
	 */
	FileUnit getNetworkFile();

	/**
	 * Build a Systemd-networkd .netwdev file for this NIC
	 *
	 * See
	 * {@link https://www.freedesktop.org/software/systemd/man/systemd.netdev.html}
	 *
	 * @return FileUnit in /etc/systemd/network/
	 */
	FileUnit getNetDevFile();

	/**
	 * Get all IP Addresses associated with this NIC
	 *
	 * @return
	 */
	Collection<IPAddress> getAddresses();

	IPAddress getGateway();

}