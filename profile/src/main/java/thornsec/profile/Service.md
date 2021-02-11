# Service.java

This configures a VM ("service").

## getPersistentConfig
Partitions, formats, and mounts the data disk, if required.

Makes sure external shares (log/backup) are in fstab and mounted.

Configures the ethernet iface.

## getInstalled
Installs the VirtualBox Guest Additions if required.

## getPersistentFirewall
Nothing to see here.

## getLiveConfig
