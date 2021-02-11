# SSH.java

This is our unit tests for configuring SSHd on a given server/service.

## getPersistentConfig
Creates a secured /etc/ssh/sshd_config file.

Creates a warning banner on SSH in - theoretically, this should never be displayed as login should always be done by keys, not passwords.

Creates a motd banner (adapted from https://nickcharlton.net/posts/debian-ubuntu-dynamic-motd.html) with useful information about the server on login (a la Ubuntu).

Regenerates server-side keys if required, removes weak moduli, and imports public keys for login if required.

## getInstalled
Makes sure the OpenSSH server is installed.

## getPersistentFirewall
Opens the SSH port.

## getLiveConfig
Ensures sshd is running
