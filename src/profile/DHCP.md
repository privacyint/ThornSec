# DHCP.java

This is our unit tests for configuring DHCP across our network.

This profile uses the standard isc-dhcp-server for its DHCP.

It is split out in this atomic fashion so that in the future if there is the decision to provide IP addresses through e.g. RADIUS that should be easy enough to do.

## getPersistentConfig
Defines the listening interfaces for your DHCP server.  If you're using a DHCP server on a Metal (standard config for externally hosted servers in e.g. a datacentre) it'll make sure bridge-utils is installed to keep ethernet traffic off the (only) physical ethernet interface.  Otherwise, if it's a dedicated Router machine (as should be default in an office setting) it'll bind these interfaces directly to the internally-facing ethernet card.

This binds to one interface per /30 subnet, which is created in the Router class.

## getInstalled
Ensures isc-dhcp-server is installed.

## getPersistentFirewall
Makes sure your server can listen on :67 on the internally-facing ethernet interface.

## getLiveConfig
Creates /etc/dhcp/dhcpd.conf to give required information for each machine which queries it.

Provides the required subnet broadcast definition, name (wired or wireless), MAC address to assign to, static IP, and router for a given machine.
