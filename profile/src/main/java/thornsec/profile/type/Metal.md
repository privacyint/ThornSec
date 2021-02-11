# metal.java

This configures a Hypervisor ("metal").

## getPersistentConfig
Creates the required ethernet ifaces - either physical or logical bridges depending on if this metal is also a router or not.

Creates our backup shell script, as well as its cron job.

## getInstalled
Installs VirtualBox, genisoimage, bridge-utils, and rsync for creating VMs.
Installs git, qemu-utils, and duplicity for backups.

Downloads John Kaul's iterative backup script from github (https://github.com/JohnKaul/rsync-time-backup.git)

Downloads and checks the specified Debian ISO against the specified sha512 sum.

## getPersistentFirewall

## getLiveConfig
Takes passwords from our local GPG store, and turns them into a crypted string ready for inserting into our preseed file.

If there is no password stored and the configuration file is not set to generate them, it uses the passphrase "secret"

## preseed(String server, String service, NetworkModel model, Boolean expirePasswords)
Spits out our service's Debian preseed file for unattended installs.

Configures:
- SSH public keys for your user
- SSH listening port
- Disables the root account, so it cannot ever be logged into
- If expirePasswords is set, expire your user's password immediately so it is forcibly changed on initial login
- Correct hostname/domain/mirror
- OpenSSH-server && sudo

## buildIso(String server, String service, NetworkModel model, String preseed)
Takes a preseed, inserts it into our Debian ISO.

## buildVm(String server, String service, NetworkModel model, String bridge)
Creates our VM for our service.
