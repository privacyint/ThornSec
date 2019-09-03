#!/bin/bash

hostname=$(hostname);
proceed=1;

echo "Started dryrun ${hostname} with config label: router"
pass=0; fail=0; fail_string=;
#============ server_dhcpd_live_config =============
out=$(sudo cat /etc/dhcp/dhcpd.conf.d/server.conf 2>&1;);
test="subnet 10.0.0.1 netmask 255.0.0.0 {}

group server {
	server-name \"server.router.geraghty.london\";
	option routers 10.0.0.1;
	option domain-name-servers 10.0.0.1;
	host nginx_lb-08:00:27:01:01:01 {
		hardware ethernet 08:00:27:01:01:01;
		fixed-address 10.1.1.1;
	}
	host nextcloud-08:00:27:01:02:02 {
		hardware ethernet 08:00:27:01:02:02;
		fixed-address 10.1.2.2;
	host ejg-08:00:27:01:03:03 {
		hardware ethernet 08:00:27:01:03:03;
		fixed-address 10.1.3.3;
	host mail-08:00:27:01:04:04 {
		hardware ethernet 08:00:27:01:04:04;
		fixed-address 10.1.4.4;
	host wedding-08:00:27:01:05:05 {
		hardware ethernet 08:00:27:01:05:05;
		fixed-address 10.1.5.5;
	host media-08:00:27:01:06:06 {
		hardware ethernet 08:00:27:01:06:06;
		fixed-address 10.1.6.6;
	host geraghty-08:00:27:01:07:07 {
		hardware ethernet 08:00:27:01:07:07;
		fixed-address 10.1.7.7;
	host pedigree-chum-44:8a:5b:74:8c:58 {
		hardware ethernet 44:8a:5b:74:8c:58;
		fixed-address 10.1.0.1;
}";
if [ "${out}" != "${test}" ] ; then
	server_dhcpd_live_config=0;
else
	server_dhcpd_live_config=1;
fi ;
if [ "$server_dhcpd_live_config" != "1" ] ; then
if [ "$dhcp_installed" = "1" ] ; then
	echo 'fail server_dhcpd_live_config CONFIGURING'
	sudo [ -f /etc/dhcp/dhcpd.conf.d/server.conf ] || sudo touch /etc/dhcp/dhcpd.conf.d/server.conf;echo "subnet 10.0.0.1 netmask 255.0.0.0 {}

group server {
	server-name \"server.router.geraghty.london\";
	option routers 10.0.0.1;
	option domain-name-servers 10.0.0.1;
	host nginx_lb-08:00:27:01:01:01 {
		hardware ethernet 08:00:27:01:01:01;
		fixed-address 10.1.1.1;
	}
	host nextcloud-08:00:27:01:02:02 {
		hardware ethernet 08:00:27:01:02:02;
		fixed-address 10.1.2.2;
	host ejg-08:00:27:01:03:03 {
		hardware ethernet 08:00:27:01:03:03;
		fixed-address 10.1.3.3;
	host mail-08:00:27:01:04:04 {
		hardware ethernet 08:00:27:01:04:04;
		fixed-address 10.1.4.4;
	host wedding-08:00:27:01:05:05 {
		hardware ethernet 08:00:27:01:05:05;
		fixed-address 10.1.5.5;
	host media-08:00:27:01:06:06 {
		hardware ethernet 08:00:27:01:06:06;
		fixed-address 10.1.6.6;
	host geraghty-08:00:27:01:07:07 {
		hardware ethernet 08:00:27:01:07:07;
		fixed-address 10.1.7.7;
	host pedigree-chum-44:8a:5b:74:8c:58 {
		hardware ethernet 44:8a:5b:74:8c:58;
		fixed-address 10.1.0.1;
}" | sudo tee /etc/dhcp/dhcpd.conf.d/server.conf > /dev/null
	echo 'fail server_dhcpd_live_config RETESTING'
server_dhcpd_live_config=0;
out=$(sudo cat /etc/dhcp/dhcpd.conf.d/server.conf 2>&1;);
test="subnet 10.0.0.1 netmask 255.0.0.0 {}

group server {
	server-name \"server.router.geraghty.london\";
	option routers 10.0.0.1;
	option domain-name-servers 10.0.0.1;
	host nginx_lb-08:00:27:01:01:01 {
		hardware ethernet 08:00:27:01:01:01;
		fixed-address 10.1.1.1;
	}
	host nextcloud-08:00:27:01:02:02 {
		hardware ethernet 08:00:27:01:02:02;
		fixed-address 10.1.2.2;
	host ejg-08:00:27:01:03:03 {
		hardware ethernet 08:00:27:01:03:03;
		fixed-address 10.1.3.3;
	host mail-08:00:27:01:04:04 {
		hardware ethernet 08:00:27:01:04:04;
		fixed-address 10.1.4.4;
	host wedding-08:00:27:01:05:05 {
		hardware ethernet 08:00:27:01:05:05;
		fixed-address 10.1.5.5;
	host media-08:00:27:01:06:06 {
		hardware ethernet 08:00:27:01:06:06;
		fixed-address 10.1.6.6;
	host geraghty-08:00:27:01:07:07 {
		hardware ethernet 08:00:27:01:07:07;
		fixed-address 10.1.7.7;
	host pedigree-chum-44:8a:5b:74:8c:58 {
		hardware ethernet 44:8a:5b:74:8c:58;
		fixed-address 10.1.0.1;
}";
if [ "${out}" != "${test}" ] ; then
	server_dhcpd_live_config=0;
else
	server_dhcpd_live_config=1;
fi ;
if [ "$server_dhcpd_live_config" = "1" ] ; then
	echo pass server_dhcpd_live_config
	((pass++))
else
	echo fail server_dhcpd_live_config
	((fail++))
	fail_string="${fail_string}
server_dhcpd_live_config failed with the message:
\"${out}\"
Couldn't create /etc/dhcp/dhcpd.conf.d/server.conf.  This is a pretty serious problem!
"
fi ;else
	server_dhcpd_live_config=0;
	echo 'fail server_dhcpd_live_config PRECONDITION FAILED dhcp_installed'
fi ;
else
	echo pass server_dhcpd_live_config
	((pass++))
fi ;

#============ rdnssd_uninstalled =============
out=$(dpkg-query --status rdnssd 2>&1 | grep "Status:";);
test="Status: install ok installed";
if [ "${out}" = "${test}" ] ; then
	rdnssd_uninstalled=0;
else
	rdnssd_uninstalled=1;
fi ;
if [ "$rdnssd_uninstalled" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail rdnssd_uninstalled CONFIGURING'
	sudo apt remove rdnssd --purge -y;
	echo 'fail rdnssd_uninstalled RETESTING'
rdnssd_uninstalled=0;
out=$(dpkg-query --status rdnssd 2>&1 | grep "Status:";);
test="Status: install ok installed";
if [ "${out}" = "${test}" ] ; then
	rdnssd_uninstalled=0;
else
	rdnssd_uninstalled=1;
fi ;
if [ "$rdnssd_uninstalled" = "1" ] ; then
	echo pass rdnssd_uninstalled
	((pass++))
else
	echo fail rdnssd_uninstalled
	((fail++))
	fail_string="${fail_string}
rdnssd_uninstalled failed with the message:
\"${out}\"
Couldn't uninstall rdnssd.  This is a package which attempts to be \"clever\" in DNS configuration and just breaks everything instead.
"
fi ;else
	rdnssd_uninstalled=0;
	echo 'fail rdnssd_uninstalled PRECONDITION FAILED proceed'
fi ;
else
	echo pass rdnssd_uninstalled
	((pass++))
fi ;

#============ sshd_ed25519 =============
out=$(sudo ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key | awk '{print $1}');
test="256";
if [ "${out}" != "${test}" ] ; then
	sshd_ed25519=0;
else
	sshd_ed25519=1;
fi ;
if [ "$sshd_ed25519" != "1" ] ; then
if [ "$sshd_config" = "1" ] ; then
	echo 'fail sshd_ed25519 CONFIGURING'
	echo -e "y\n" | sudo ssh-keygen -f /etc/ssh/ssh_host_ed25519_key -N "" -t ed25519
	echo 'fail sshd_ed25519 RETESTING'
sshd_ed25519=0;
out=$(sudo ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key | awk '{print $1}');
test="256";
if [ "${out}" != "${test}" ] ; then
	sshd_ed25519=0;
else
	sshd_ed25519=1;
fi ;
if [ "$sshd_ed25519" = "1" ] ; then
	echo pass sshd_ed25519
	((pass++))
else
	echo fail sshd_ed25519
	((fail++))
	fail_string="${fail_string}
sshd_ed25519 failed with the message:
\"${out}\"
Couldn't generate you a strong ed25519 SSH key.  This isn't too bad, but try re-running the script to get it to work.
"
fi ;else
	sshd_ed25519=0;
	echo 'fail sshd_ed25519 PRECONDITION FAILED sshd_config'
fi ;
else
	echo pass sshd_ed25519
	((pass++))
fi ;

#============ ca_certificates_installed =============
out=$(dpkg-query --status ca-certificates 2>&1 | grep -E "Status: (install|hold) ok installed";);
test="";
if [ "${out}" = "${test}" ] ; then
	ca_certificates_installed=0;
else
	ca_certificates_installed=1;
fi ;
if [ "$ca_certificates_installed" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail ca_certificates_installed CONFIGURING'
	export DEBIAN_FRONTEND=noninteractive; sudo apt-get update;sudo -E apt-get install --assume-yes ca-certificates;
	echo 'fail ca_certificates_installed RETESTING'
ca_certificates_installed=0;
out=$(dpkg-query --status ca-certificates 2>&1 | grep -E "Status: (install|hold) ok installed";);
test="";
if [ "${out}" = "${test}" ] ; then
	ca_certificates_installed=0;
else
	ca_certificates_installed=1;
fi ;
if [ "$ca_certificates_installed" = "1" ] ; then
	echo pass ca_certificates_installed
	((pass++))
else
	echo fail ca_certificates_installed
	((fail++))
	fail_string="${fail_string}
ca_certificates_installed failed with the message:
\"${out}\"
Couldn't install ca-certificates.  This is pretty serious.
"
fi ;else
	ca_certificates_installed=0;
	echo 'fail ca_certificates_installed PRECONDITION FAILED proceed'
fi ;
else
	echo pass ca_certificates_installed
	((pass++))
fi ;

#============ internalonlydevice_netdev =============
out=$(sudo cat /etc/systemd/network/internalonlydevice.netdev 2>&1;);
test="[NetDev]
Name=internalonlydevice
Kind=macvlan

[MACVLAN]
Mode=vepa";
if [ "${out}" != "${test}" ] ; then
	internalonlydevice_netdev=0;
else
	internalonlydevice_netdev=1;
fi ;
if [ "$internalonlydevice_netdev" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail internalonlydevice_netdev CONFIGURING'
	sudo [ -f /etc/systemd/network/internalonlydevice.netdev ] || sudo touch /etc/systemd/network/internalonlydevice.netdev;echo "[NetDev]
Name=internalonlydevice
Kind=macvlan

[MACVLAN]
Mode=vepa" | sudo tee /etc/systemd/network/internalonlydevice.netdev > /dev/null
	echo 'fail internalonlydevice_netdev RETESTING'
internalonlydevice_netdev=0;
out=$(sudo cat /etc/systemd/network/internalonlydevice.netdev 2>&1;);
test="[NetDev]
Name=internalonlydevice
Kind=macvlan

[MACVLAN]
Mode=vepa";
if [ "${out}" != "${test}" ] ; then
	internalonlydevice_netdev=0;
else
	internalonlydevice_netdev=1;
fi ;
if [ "$internalonlydevice_netdev" = "1" ] ; then
	echo pass internalonlydevice_netdev
	((pass++))
else
	echo fail internalonlydevice_netdev
	((fail++))
	fail_string="${fail_string}
internalonlydevice_netdev failed with the message:
\"${out}\"
Couldn't create /etc/systemd/network/internalonlydevice.netdev.  This is a pretty serious problem!
"
fi ;else
	internalonlydevice_netdev=0;
	echo 'fail internalonlydevice_netdev PRECONDITION FAILED proceed'
fi ;
else
	echo pass internalonlydevice_netdev
	((pass++))
fi ;

#============ bindfs_installed =============
out=$(dpkg-query --status bindfs 2>&1 | grep -E "Status: (install|hold) ok installed";);
test="";
if [ "${out}" = "${test}" ] ; then
	bindfs_installed=0;
else
	bindfs_installed=1;
fi ;
if [ "$bindfs_installed" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail bindfs_installed CONFIGURING'
	export DEBIAN_FRONTEND=noninteractive; sudo apt-get update;sudo -E apt-get install --assume-yes bindfs;
	echo 'fail bindfs_installed RETESTING'
bindfs_installed=0;
out=$(dpkg-query --status bindfs 2>&1 | grep -E "Status: (install|hold) ok installed";);
test="";
if [ "${out}" = "${test}" ] ; then
	bindfs_installed=0;
else
	bindfs_installed=1;
fi ;
if [ "$bindfs_installed" = "1" ] ; then
	echo pass bindfs_installed
	((pass++))
else
	echo fail bindfs_installed
	((fail++))
	fail_string="${fail_string}
bindfs_installed failed with the message:
\"${out}\"
Couldn't install bindfs.  This is pretty serious.
"
fi ;else
	bindfs_installed=0;
	echo 'fail bindfs_installed PRECONDITION FAILED proceed'
fi ;
else
	echo pass bindfs_installed
	((pass++))
fi ;

#============ administrator_network =============
out=$(sudo cat /etc/systemd/network/administrator.network 2>&1;);
test="[Match]
Name=administrator

[Network]
IPForward=yes
Address=172.20.0.1/16";
if [ "${out}" != "${test}" ] ; then
	administrator_network=0;
else
	administrator_network=1;
fi ;
if [ "$administrator_network" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail administrator_network CONFIGURING'
	sudo [ -f /etc/systemd/network/administrator.network ] || sudo touch /etc/systemd/network/administrator.network;echo "[Match]
Name=administrator

[Network]
IPForward=yes
Address=172.20.0.1/16" | sudo tee /etc/systemd/network/administrator.network > /dev/null
	echo 'fail administrator_network RETESTING'
administrator_network=0;
out=$(sudo cat /etc/systemd/network/administrator.network 2>&1;);
test="[Match]
Name=administrator

[Network]
IPForward=yes
Address=172.20.0.1/16";
if [ "${out}" != "${test}" ] ; then
	administrator_network=0;
else
	administrator_network=1;
fi ;
if [ "$administrator_network" = "1" ] ; then
	echo pass administrator_network
	((pass++))
else
	echo fail administrator_network
	((fail++))
	fail_string="${fail_string}
administrator_network failed with the message:
\"${out}\"
Couldn't create /etc/systemd/network/administrator.network.  This is a pretty serious problem!
"
fi ;else
	administrator_network=0;
	echo 'fail administrator_network PRECONDITION FAILED proceed'
fi ;
else
	echo pass administrator_network
	((pass++))
fi ;

#============ ssh_dir_ed_chowned =============
out=$(sudo stat -c %U:%G /home/ed/.ssh;);
test="ed:ed";
if [ "${out}" != "${test}" ] ; then
	ssh_dir_ed_chowned=0;
else
	ssh_dir_ed_chowned=1;
fi ;
if [ "$ssh_dir_ed_chowned" != "1" ] ; then
if [ "$ssh_dir_ed_created" = "1" ] ; then
	echo 'fail ssh_dir_ed_chowned CONFIGURING'
	sudo chown -R ed:ed /home/ed/.ssh;
	echo 'fail ssh_dir_ed_chowned RETESTING'
ssh_dir_ed_chowned=0;
out=$(sudo stat -c %U:%G /home/ed/.ssh;);
test="ed:ed";
if [ "${out}" != "${test}" ] ; then
	ssh_dir_ed_chowned=0;
else
	ssh_dir_ed_chowned=1;
fi ;
if [ "$ssh_dir_ed_chowned" = "1" ] ; then
	echo pass ssh_dir_ed_chowned
	((pass++))
else
	echo fail ssh_dir_ed_chowned
	((fail++))
	fail_string="${fail_string}
ssh_dir_ed_chowned failed with the message:
\"${out}\"
Couldn't change the ownership of /home/ed/.ssh to ed:ed
"
fi ;else
	ssh_dir_ed_chowned=0;
	echo 'fail ssh_dir_ed_chowned PRECONDITION FAILED ssh_dir_ed_created'
fi ;
else
	echo pass ssh_dir_ed_chowned
	((pass++))
fi ;

#============ sshd_config =============
out=$(sudo cat /etc/ssh/sshd_config 2>&1;);
test="Port 65422
Protocol 2
HostKey /etc/ssh/ssh_host_rsa_key
HostKey /etc/ssh/ssh_host_ed25519_key
UsePrivilegeSeparation yes
KeyRegenerationInterval 3600
ServerKeyBits 1024
MACs hmac-sha2-512-etm@openssh.com,hmac-sha2-256-etm@openssh.com,hmac-ripemd160-etm@openssh.com,umac-128-etm@openssh.com,hmac-sha2-512,hmac-sha2-256,hmac-ripemd160,umac-128@openssh.com
Ciphers chacha20-poly1305@openssh.com,aes256-gcm@openssh.com,aes128-gcm@openssh.com,aes256-ctr,aes192-ctr,aes128-ctr
KexAlgorithms curve25519-sha256@libssh.org,diffie-hellman-group-exchange-sha256
SyslogFacility AUTH
LogLevel INFO
LoginGraceTime 120
PermitRootLogin no
StrictModes yes
RSAAuthentication yes
PubkeyAuthentication yes
AuthorizedKeysFile %h/.ssh/authorized_keys
IgnoreRhosts yes
RhostsRSAAuthentication no
HostbasedAuthentication no
PermitEmptyPasswords no
PasswordAuthentication no
ChallengeResponseAuthentication no
X11Forwarding yes
X11DisplayOffset 10
PrintMotd no
PrintLastLog yes
TCPKeepAlive yes
AcceptEnv LANG LC_*
Subsystem sftp /usr/lib/openssh/sftp-server
UsePAM yes
Banner /etc/ssh/sshd_banner
MaxSessions 1
UseDNS no";
if [ "${out}" != "${test}" ] ; then
	sshd_config=0;
else
	sshd_config=1;
fi ;
if [ "$sshd_config" != "1" ] ; then
if [ "$sshd_installed" = "1" ] ; then
	echo 'fail sshd_config CONFIGURING'
	sudo [ -f /etc/ssh/sshd_config ] || sudo touch /etc/ssh/sshd_config;echo "Port 65422
Protocol 2
HostKey /etc/ssh/ssh_host_rsa_key
HostKey /etc/ssh/ssh_host_ed25519_key
UsePrivilegeSeparation yes
KeyRegenerationInterval 3600
ServerKeyBits 1024
MACs hmac-sha2-512-etm@openssh.com,hmac-sha2-256-etm@openssh.com,hmac-ripemd160-etm@openssh.com,umac-128-etm@openssh.com,hmac-sha2-512,hmac-sha2-256,hmac-ripemd160,umac-128@openssh.com
Ciphers chacha20-poly1305@openssh.com,aes256-gcm@openssh.com,aes128-gcm@openssh.com,aes256-ctr,aes192-ctr,aes128-ctr
KexAlgorithms curve25519-sha256@libssh.org,diffie-hellman-group-exchange-sha256
SyslogFacility AUTH
LogLevel INFO
LoginGraceTime 120
PermitRootLogin no
StrictModes yes
RSAAuthentication yes
PubkeyAuthentication yes
AuthorizedKeysFile %h/.ssh/authorized_keys
IgnoreRhosts yes
RhostsRSAAuthentication no
HostbasedAuthentication no
PermitEmptyPasswords no
PasswordAuthentication no
ChallengeResponseAuthentication no
X11Forwarding yes
X11DisplayOffset 10
PrintMotd no
PrintLastLog yes
TCPKeepAlive yes
AcceptEnv LANG LC_*
Subsystem sftp /usr/lib/openssh/sftp-server
UsePAM yes
Banner /etc/ssh/sshd_banner
MaxSessions 1
UseDNS no" | sudo tee /etc/ssh/sshd_config > /dev/null
	echo 'fail sshd_config RETESTING'
sshd_config=0;
out=$(sudo cat /etc/ssh/sshd_config 2>&1;);
test="Port 65422
Protocol 2
HostKey /etc/ssh/ssh_host_rsa_key
HostKey /etc/ssh/ssh_host_ed25519_key
UsePrivilegeSeparation yes
KeyRegenerationInterval 3600
ServerKeyBits 1024
MACs hmac-sha2-512-etm@openssh.com,hmac-sha2-256-etm@openssh.com,hmac-ripemd160-etm@openssh.com,umac-128-etm@openssh.com,hmac-sha2-512,hmac-sha2-256,hmac-ripemd160,umac-128@openssh.com
Ciphers chacha20-poly1305@openssh.com,aes256-gcm@openssh.com,aes128-gcm@openssh.com,aes256-ctr,aes192-ctr,aes128-ctr
KexAlgorithms curve25519-sha256@libssh.org,diffie-hellman-group-exchange-sha256
SyslogFacility AUTH
LogLevel INFO
LoginGraceTime 120
PermitRootLogin no
StrictModes yes
RSAAuthentication yes
PubkeyAuthentication yes
AuthorizedKeysFile %h/.ssh/authorized_keys
IgnoreRhosts yes
RhostsRSAAuthentication no
HostbasedAuthentication no
PermitEmptyPasswords no
PasswordAuthentication no
ChallengeResponseAuthentication no
X11Forwarding yes
X11DisplayOffset 10
PrintMotd no
PrintLastLog yes
TCPKeepAlive yes
AcceptEnv LANG LC_*
Subsystem sftp /usr/lib/openssh/sftp-server
UsePAM yes
Banner /etc/ssh/sshd_banner
MaxSessions 1
UseDNS no";
if [ "${out}" != "${test}" ] ; then
	sshd_config=0;
else
	sshd_config=1;
fi ;
if [ "$sshd_config" = "1" ] ; then
	echo pass sshd_config
	((pass++))
else
	echo fail sshd_config
	((fail++))
	fail_string="${fail_string}
sshd_config failed with the message:
\"${out}\"
Couldn't create /etc/ssh/sshd_config.  This is a pretty serious problem!
"
fi ;else
	sshd_config=0;
	echo 'fail sshd_config PRECONDITION FAILED sshd_installed'
fi ;
else
	echo pass sshd_config
	((pass++))
fi ;

#============ sshd_rsa =============
out=$(sudo ssh-keygen -lf /etc/ssh/ssh_host_rsa_key | awk '{print $1}');
test="4096";
if [ "${out}" != "${test}" ] ; then
	sshd_rsa=0;
else
	sshd_rsa=1;
fi ;
if [ "$sshd_rsa" != "1" ] ; then
if [ "$sshd_config" = "1" ] ; then
	echo 'fail sshd_rsa CONFIGURING'
	echo -e "y\n" | sudo ssh-keygen -f /etc/ssh/ssh_host_rsa_key -N "" -t rsa -b 4096
	echo 'fail sshd_rsa RETESTING'
sshd_rsa=0;
out=$(sudo ssh-keygen -lf /etc/ssh/ssh_host_rsa_key | awk '{print $1}');
test="4096";
if [ "${out}" != "${test}" ] ; then
	sshd_rsa=0;
else
	sshd_rsa=1;
fi ;
if [ "$sshd_rsa" = "1" ] ; then
	echo pass sshd_rsa
	((pass++))
else
	echo fail sshd_rsa
	((fail++))
	fail_string="${fail_string}
sshd_rsa failed with the message:
\"${out}\"
Couldn't generate you a new SSH key.  This isn't too bad, but try re-running the script to get it to work.
"
fi ;else
	sshd_rsa=0;
	echo 'fail sshd_rsa PRECONDITION FAILED sshd_config'
fi ;
else
	echo pass sshd_rsa
	((pass++))
fi ;

#============ user_netdev =============
out=$(sudo cat /etc/systemd/network/user.netdev 2>&1;);
test="[NetDev]
Name=user
Kind=macvlan

[MACVLAN]
Mode=vepa";
if [ "${out}" != "${test}" ] ; then
	user_netdev=0;
else
	user_netdev=1;
fi ;
if [ "$user_netdev" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail user_netdev CONFIGURING'
	sudo [ -f /etc/systemd/network/user.netdev ] || sudo touch /etc/systemd/network/user.netdev;echo "[NetDev]
Name=user
Kind=macvlan

[MACVLAN]
Mode=vepa" | sudo tee /etc/systemd/network/user.netdev > /dev/null
	echo 'fail user_netdev RETESTING'
user_netdev=0;
out=$(sudo cat /etc/systemd/network/user.netdev 2>&1;);
test="[NetDev]
Name=user
Kind=macvlan

[MACVLAN]
Mode=vepa";
if [ "${out}" != "${test}" ] ; then
	user_netdev=0;
else
	user_netdev=1;
fi ;
if [ "$user_netdev" = "1" ] ; then
	echo pass user_netdev
	((pass++))
else
	echo fail user_netdev
	((fail++))
	fail_string="${fail_string}
user_netdev failed with the message:
\"${out}\"
Couldn't create /etc/systemd/network/user.netdev.  This is a pretty serious problem!
"
fi ;else
	user_netdev=0;
	echo 'fail user_netdev PRECONDITION FAILED proceed'
fi ;
else
	echo pass user_netdev
	((pass++))
fi ;

#============ guest_network =============
out=$(sudo cat /etc/systemd/network/guest.network 2>&1;);
test="[Match]
Name=guest

[Network]
IPForward=yes
Address=172.31.0.1/16";
if [ "${out}" != "${test}" ] ; then
	guest_network=0;
else
	guest_network=1;
fi ;
if [ "$guest_network" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail guest_network CONFIGURING'
	sudo [ -f /etc/systemd/network/guest.network ] || sudo touch /etc/systemd/network/guest.network;echo "[Match]
Name=guest

[Network]
IPForward=yes
Address=172.31.0.1/16" | sudo tee /etc/systemd/network/guest.network > /dev/null
	echo 'fail guest_network RETESTING'
guest_network=0;
out=$(sudo cat /etc/systemd/network/guest.network 2>&1;);
test="[Match]
Name=guest

[Network]
IPForward=yes
Address=172.31.0.1/16";
if [ "${out}" != "${test}" ] ; then
	guest_network=0;
else
	guest_network=1;
fi ;
if [ "$guest_network" = "1" ] ; then
	echo pass guest_network
	((pass++))
else
	echo fail guest_network
	((fail++))
	fail_string="${fail_string}
guest_network failed with the message:
\"${out}\"
Couldn't create /etc/systemd/network/guest.network.  This is a pretty serious problem!
"
fi ;else
	guest_network=0;
	echo 'fail guest_network PRECONDITION FAILED proceed'
fi ;
else
	echo pass guest_network
	((pass++))
fi ;

#============ sshd_banner =============
out=$(sudo cat /etc/ssh/banner 2>&1;);
test="************************NOTICE***********************
This system is optimised and configured with security and logging as a
priority. All user activity is logged and streamed offsite. Individuals
or groups using this system in excess of their authorisation will have
all access terminated. Illegal access of this system or attempts to
limit or restrict access to authorised users (such as DoS attacks) will
be reported to national and international law enforcement bodies. We
will prosecute to the fullest extent of the law regardless of the funds
required. Anyone using this system consents to these terms and the laws
of the United Kingdom and United States respectively.";
if [ "${out}" != "${test}" ] ; then
	sshd_banner=0;
else
	sshd_banner=1;
fi ;
if [ "$sshd_banner" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail sshd_banner CONFIGURING'
	sudo [ -f /etc/ssh/banner ] || sudo touch /etc/ssh/banner;echo "************************NOTICE***********************
This system is optimised and configured with security and logging as a
priority. All user activity is logged and streamed offsite. Individuals
or groups using this system in excess of their authorisation will have
all access terminated. Illegal access of this system or attempts to
limit or restrict access to authorised users (such as DoS attacks) will
be reported to national and international law enforcement bodies. We
will prosecute to the fullest extent of the law regardless of the funds
required. Anyone using this system consents to these terms and the laws
of the United Kingdom and United States respectively." | sudo tee /etc/ssh/banner > /dev/null
	echo 'fail sshd_banner RETESTING'
sshd_banner=0;
out=$(sudo cat /etc/ssh/banner 2>&1;);
test="************************NOTICE***********************
This system is optimised and configured with security and logging as a
priority. All user activity is logged and streamed offsite. Individuals
or groups using this system in excess of their authorisation will have
all access terminated. Illegal access of this system or attempts to
limit or restrict access to authorised users (such as DoS attacks) will
be reported to national and international law enforcement bodies. We
will prosecute to the fullest extent of the law regardless of the funds
required. Anyone using this system consents to these terms and the laws
of the United Kingdom and United States respectively.";
if [ "${out}" != "${test}" ] ; then
	sshd_banner=0;
else
	sshd_banner=1;
fi ;
if [ "$sshd_banner" = "1" ] ; then
	echo pass sshd_banner
	((pass++))
else
	echo fail sshd_banner
	((fail++))
	fail_string="${fail_string}
sshd_banner failed with the message:
\"${out}\"
Couldn't create /etc/ssh/banner.  This is a pretty serious problem!
"
fi ;else
	sshd_banner=0;
	echo 'fail sshd_banner PRECONDITION FAILED proceed'
fi ;
else
	echo pass sshd_banner
	((pass++))
fi ;

#============ administrator_netdev =============
out=$(sudo cat /etc/systemd/network/administrator.netdev 2>&1;);
test="[NetDev]
Name=administrator
Kind=macvlan

[MACVLAN]
Mode=vepa";
if [ "${out}" != "${test}" ] ; then
	administrator_netdev=0;
else
	administrator_netdev=1;
fi ;
if [ "$administrator_netdev" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail administrator_netdev CONFIGURING'
	sudo [ -f /etc/systemd/network/administrator.netdev ] || sudo touch /etc/systemd/network/administrator.netdev;echo "[NetDev]
Name=administrator
Kind=macvlan

[MACVLAN]
Mode=vepa" | sudo tee /etc/systemd/network/administrator.netdev > /dev/null
	echo 'fail administrator_netdev RETESTING'
administrator_netdev=0;
out=$(sudo cat /etc/systemd/network/administrator.netdev 2>&1;);
test="[NetDev]
Name=administrator
Kind=macvlan

[MACVLAN]
Mode=vepa";
if [ "${out}" != "${test}" ] ; then
	administrator_netdev=0;
else
	administrator_netdev=1;
fi ;
if [ "$administrator_netdev" = "1" ] ; then
	echo pass administrator_netdev
	((pass++))
else
	echo fail administrator_netdev
	((fail++))
	fail_string="${fail_string}
administrator_netdev failed with the message:
\"${out}\"
Couldn't create /etc/systemd/network/administrator.netdev.  This is a pretty serious problem!
"
fi ;else
	administrator_netdev=0;
	echo 'fail administrator_netdev PRECONDITION FAILED proceed'
fi ;
else
	echo pass administrator_netdev
	((pass++))
fi ;

#============ update =============
out=$(sudo apt-get update > /dev/null; sudo apt-get --assume-no upgrade | grep "[0-9] upgraded, [0-9] newly installed, [0-9] to remove and [0-9] not upgraded.";);
test="0 upgraded, 0 newly installed, 0 to remove and 0 not upgraded.";
if [ "${out}" != "${test}" ] ; then
	update=0;
else
	update=1;
fi ;
if [ "$update" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail update CONFIGURING'
	sudo apt-get --assume-yes upgrade;
	echo 'fail update RETESTING'
update=0;
out=$(sudo apt-get update > /dev/null; sudo apt-get --assume-no upgrade | grep "[0-9] upgraded, [0-9] newly installed, [0-9] to remove and [0-9] not upgraded.";);
test="0 upgraded, 0 newly installed, 0 to remove and 0 not upgraded.";
if [ "${out}" != "${test}" ] ; then
	update=0;
else
	update=1;
fi ;
if [ "$update" = "1" ] ; then
	echo pass update
	((pass++))
else
	echo fail update
	((fail++))
	fail_string="${fail_string}
update failed with the message:
\"${out}\"
There are $(sudo apt-get upgrade -s | grep -P '^\d+ upgraded'| cut -d' ' -f1) updates available, of which $(sudo apt-get upgrade -s | grep ^Inst | grep Security | wc -l) are security updates\"
"
fi ;else
	update=0;
	echo 'fail update PRECONDITION FAILED proceed'
fi ;
else
	echo pass update
	((pass++))
fi ;

#============ sysctl_conf =============
out=$(sudo cat /etc/sysctl.conf 2>&1;);
test="net.ipv4.ip_forward=1
net.ipv6.conf.all.disable_ipv6=1
net.ipv6.conf.default.disable_ipv6=1
net.ipv6.conf.lo.disable_ipv6=1";
if [ "${out}" != "${test}" ] ; then
	sysctl_conf=0;
else
	sysctl_conf=1;
fi ;
if [ "$sysctl_conf" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail sysctl_conf CONFIGURING'
	sudo [ -f /etc/sysctl.conf ] || sudo touch /etc/sysctl.conf;echo "net.ipv4.ip_forward=1
net.ipv6.conf.all.disable_ipv6=1
net.ipv6.conf.default.disable_ipv6=1
net.ipv6.conf.lo.disable_ipv6=1" | sudo tee /etc/sysctl.conf > /dev/null
	echo 'fail sysctl_conf RETESTING'
sysctl_conf=0;
out=$(sudo cat /etc/sysctl.conf 2>&1;);
test="net.ipv4.ip_forward=1
net.ipv6.conf.all.disable_ipv6=1
net.ipv6.conf.default.disable_ipv6=1
net.ipv6.conf.lo.disable_ipv6=1";
if [ "${out}" != "${test}" ] ; then
	sysctl_conf=0;
else
	sysctl_conf=1;
fi ;
if [ "$sysctl_conf" = "1" ] ; then
	echo pass sysctl_conf
	((pass++))
else
	echo fail sysctl_conf
	((fail++))
	fail_string="${fail_string}
sysctl_conf failed with the message:
\"${out}\"
Couldn't create /etc/sysctl.conf.  This is a pretty serious problem!
"
fi ;else
	sysctl_conf=0;
	echo 'fail sysctl_conf PRECONDITION FAILED proceed'
fi ;
else
	echo pass sysctl_conf
	((pass++))
fi ;

#============ dns_resolv_conf =============
out=$(sudo cat /etc/resolv.conf 2>&1;);
test="search geraghty.london
nameserver 10.0.0.1";
if [ "${out}" != "${test}" ] ; then
	dns_resolv_conf=0;
else
	dns_resolv_conf=1;
fi ;
if [ "$dns_resolv_conf" != "1" ] ; then
if [ "$dns_running" = "1" ] ; then
	echo 'fail dns_resolv_conf CONFIGURING'
	sudo [ -f /etc/resolv.conf ] || sudo touch /etc/resolv.conf;echo "search geraghty.london
nameserver 10.0.0.1" | sudo tee /etc/resolv.conf > /dev/null
	echo 'fail dns_resolv_conf RETESTING'
dns_resolv_conf=0;
out=$(sudo cat /etc/resolv.conf 2>&1;);
test="search geraghty.london
nameserver 10.0.0.1";
if [ "${out}" != "${test}" ] ; then
	dns_resolv_conf=0;
else
	dns_resolv_conf=1;
fi ;
if [ "$dns_resolv_conf" = "1" ] ; then
	echo pass dns_resolv_conf
	((pass++))
else
	echo fail dns_resolv_conf
	((fail++))
	fail_string="${fail_string}
dns_resolv_conf failed with the message:
\"${out}\"
Unable to change your DNS to point at the local one.  This will probably cause VM building to fail, amongst other problems
"
fi ;else
	dns_resolv_conf=0;
	echo 'fail dns_resolv_conf PRECONDITION FAILED dns_running'
fi ;
else
	echo pass dns_resolv_conf
	((pass++))
fi ;

#============ dhcpd_defiface =============
out=$(sudo cat /etc/default/isc-dhcp-server 2>&1;);
test="INTERFACES=\" server user administrator internalonlydevice externalonlydevice guest\"";
if [ "${out}" != "${test}" ] ; then
	dhcpd_defiface=0;
else
	dhcpd_defiface=1;
fi ;
if [ "$dhcpd_defiface" != "1" ] ; then
if [ "$dhcp_installed" = "1" ] ; then
	echo 'fail dhcpd_defiface CONFIGURING'
	sudo [ -f /etc/default/isc-dhcp-server ] || sudo touch /etc/default/isc-dhcp-server;echo "INTERFACES=\" server user administrator internalonlydevice externalonlydevice guest\"" | sudo tee /etc/default/isc-dhcp-server > /dev/null
	echo 'fail dhcpd_defiface RETESTING'
dhcpd_defiface=0;
out=$(sudo cat /etc/default/isc-dhcp-server 2>&1;);
test="INTERFACES=\" server user administrator internalonlydevice externalonlydevice guest\"";
if [ "${out}" != "${test}" ] ; then
	dhcpd_defiface=0;
else
	dhcpd_defiface=1;
fi ;
if [ "$dhcpd_defiface" = "1" ] ; then
	echo pass dhcpd_defiface
	((pass++))
else
	echo fail dhcpd_defiface
	((fail++))
	fail_string="${fail_string}
dhcpd_defiface failed with the message:
\"${out}\"
Couldn't create /etc/default/isc-dhcp-server.  This is a pretty serious problem!
"
fi ;else
	dhcpd_defiface=0;
	echo 'fail dhcpd_defiface PRECONDITION FAILED dhcp_installed'
fi ;
else
	echo pass dhcpd_defiface
	((pass++))
fi ;

#============ server_network =============
out=$(sudo cat /etc/systemd/network/server.network 2>&1;);
test="[Match]
Name=server

[Network]
IPForward=yes
Address=10.0.0.1/8";
if [ "${out}" != "${test}" ] ; then
	server_network=0;
else
	server_network=1;
fi ;
if [ "$server_network" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail server_network CONFIGURING'
	sudo [ -f /etc/systemd/network/server.network ] || sudo touch /etc/systemd/network/server.network;echo "[Match]
Name=server

[Network]
IPForward=yes
Address=10.0.0.1/8" | sudo tee /etc/systemd/network/server.network > /dev/null
	echo 'fail server_network RETESTING'
server_network=0;
out=$(sudo cat /etc/systemd/network/server.network 2>&1;);
test="[Match]
Name=server

[Network]
IPForward=yes
Address=10.0.0.1/8";
if [ "${out}" != "${test}" ] ; then
	server_network=0;
else
	server_network=1;
fi ;
if [ "$server_network" = "1" ] ; then
	echo pass server_network
	((pass++))
else
	echo fail server_network
	((fail++))
	fail_string="${fail_string}
server_network failed with the message:
\"${out}\"
Couldn't create /etc/systemd/network/server.network.  This is a pretty serious problem!
"
fi ;else
	server_network=0;
	echo 'fail server_network PRECONDITION FAILED proceed'
fi ;
else
	echo pass server_network
	((pass++))
fi ;

#============ shorewall_interfaces =============
out=$(sudo cat /etc/shorewall/interfaces 2>&1;);
test="#Dedicate interfaces to zones
#Please see http://shorewall.net/manpages/shorewall-interfaces.html for more details
#If you're looking for how we are assigning zones, please see /etc/shorewall/hosts
?FORMAT 2
#zone       interface      options
-           servers        detect tcpflags,nosmurfs,routefilter,logmartians
-           users          detect dhcp,tcpflags,nosmurfs,routefilter,logmartians
-           admins         detect tcpflags,nosmurfs,routefilter,logmartians
-           internalOnlys  detect dhcp,tcpflags,nosmurfs,routefilter,logmartians
-           externalOnlys  detect dhcp,tcpflags,nosmurfs,routefilter,logmartians
autoguest   autoguest      detect tcpflags,nosmurfs,routefilter,logmartians";
if [ "${out}" != "${test}" ] ; then
	shorewall_interfaces=0;
else
	shorewall_interfaces=1;
fi ;
if [ "$shorewall_interfaces" != "1" ] ; then
if [ "$shorewall_installed" = "1" ] ; then
	echo 'fail shorewall_interfaces CONFIGURING'
	sudo [ -f /etc/shorewall/interfaces ] || sudo touch /etc/shorewall/interfaces;echo "#Dedicate interfaces to zones
#Please see http://shorewall.net/manpages/shorewall-interfaces.html for more details
#If you're looking for how we are assigning zones, please see /etc/shorewall/hosts
?FORMAT 2
#zone       interface      options
-           servers        detect tcpflags,nosmurfs,routefilter,logmartians
-           users          detect dhcp,tcpflags,nosmurfs,routefilter,logmartians
-           admins         detect tcpflags,nosmurfs,routefilter,logmartians
-           internalOnlys  detect dhcp,tcpflags,nosmurfs,routefilter,logmartians
-           externalOnlys  detect dhcp,tcpflags,nosmurfs,routefilter,logmartians
autoguest   autoguest      detect tcpflags,nosmurfs,routefilter,logmartians" | sudo tee /etc/shorewall/interfaces > /dev/null
	echo 'fail shorewall_interfaces RETESTING'
shorewall_interfaces=0;
out=$(sudo cat /etc/shorewall/interfaces 2>&1;);
test="#Dedicate interfaces to zones
#Please see http://shorewall.net/manpages/shorewall-interfaces.html for more details
#If you're looking for how we are assigning zones, please see /etc/shorewall/hosts
?FORMAT 2
#zone       interface      options
-           servers        detect tcpflags,nosmurfs,routefilter,logmartians
-           users          detect dhcp,tcpflags,nosmurfs,routefilter,logmartians
-           admins         detect tcpflags,nosmurfs,routefilter,logmartians
-           internalOnlys  detect dhcp,tcpflags,nosmurfs,routefilter,logmartians
-           externalOnlys  detect dhcp,tcpflags,nosmurfs,routefilter,logmartians
autoguest   autoguest      detect tcpflags,nosmurfs,routefilter,logmartians";
if [ "${out}" != "${test}" ] ; then
	shorewall_interfaces=0;
else
	shorewall_interfaces=1;
fi ;
if [ "$shorewall_interfaces" = "1" ] ; then
	echo pass shorewall_interfaces
	((pass++))
else
	echo fail shorewall_interfaces
	((fail++))
	fail_string="${fail_string}
shorewall_interfaces failed with the message:
\"${out}\"
Couldn't create /etc/shorewall/interfaces.  This is a pretty serious problem!
"
fi ;else
	shorewall_interfaces=0;
	echo 'fail shorewall_interfaces PRECONDITION FAILED shorewall_installed'
fi ;
else
	echo pass shorewall_interfaces
	((pass++))
fi ;

#============ shorewall_zones =============
out=$(sudo cat /etc/shorewall/zones 2>&1;);
test="#This is the file which creates our various zones
#Please see http://shorewall.net/manpages/shorewall-zones.html for more details
#zone type
fw    firewall
wan   ipv4
servers ipv4
servers:router ipv4
servers:nginx_lb ipv4
servers:nextcloud ipv4
servers:ejg ipv4
servers:mail ipv4
servers:wedding ipv4
servers:media ipv4
servers:geraghty ipv4
servers:pedigree-chum ipv4
users ipv4
users:ed ipv4
users:ed+laptop ipv4
users:ed+phone ipv4
users:luisa+desktop ipv4
users:luisa+phone ipv4
users:luisa+laptop ipv4
users:projector ipv4
admins:users ipv4
internalOnlys ipv4
internalOnlys:printer ipv4
externalOnlys ipv4
externalOnlys:autoguest ipv4";
if [ "${out}" != "${test}" ] ; then
	shorewall_zones=0;
else
	shorewall_zones=1;
fi ;
if [ "$shorewall_zones" != "1" ] ; then
if [ "$shorewall_installed" = "1" ] ; then
	echo 'fail shorewall_zones CONFIGURING'
	sudo [ -f /etc/shorewall/zones ] || sudo touch /etc/shorewall/zones;echo "#This is the file which creates our various zones
#Please see http://shorewall.net/manpages/shorewall-zones.html for more details
#zone type
fw    firewall
wan   ipv4
servers ipv4
servers:router ipv4
servers:nginx_lb ipv4
servers:nextcloud ipv4
servers:ejg ipv4
servers:mail ipv4
servers:wedding ipv4
servers:media ipv4
servers:geraghty ipv4
servers:pedigree-chum ipv4
users ipv4
users:ed ipv4
users:ed+laptop ipv4
users:ed+phone ipv4
users:luisa+desktop ipv4
users:luisa+phone ipv4
users:luisa+laptop ipv4
users:projector ipv4
admins:users ipv4
internalOnlys ipv4
internalOnlys:printer ipv4
externalOnlys ipv4
externalOnlys:autoguest ipv4" | sudo tee /etc/shorewall/zones > /dev/null
	echo 'fail shorewall_zones RETESTING'
shorewall_zones=0;
out=$(sudo cat /etc/shorewall/zones 2>&1;);
test="#This is the file which creates our various zones
#Please see http://shorewall.net/manpages/shorewall-zones.html for more details
#zone type
fw    firewall
wan   ipv4
servers ipv4
servers:router ipv4
servers:nginx_lb ipv4
servers:nextcloud ipv4
servers:ejg ipv4
servers:mail ipv4
servers:wedding ipv4
servers:media ipv4
servers:geraghty ipv4
servers:pedigree-chum ipv4
users ipv4
users:ed ipv4
users:ed+laptop ipv4
users:ed+phone ipv4
users:luisa+desktop ipv4
users:luisa+phone ipv4
users:luisa+laptop ipv4
users:projector ipv4
admins:users ipv4
internalOnlys ipv4
internalOnlys:printer ipv4
externalOnlys ipv4
externalOnlys:autoguest ipv4";
if [ "${out}" != "${test}" ] ; then
	shorewall_zones=0;
else
	shorewall_zones=1;
fi ;
if [ "$shorewall_zones" = "1" ] ; then
	echo pass shorewall_zones
	((pass++))
else
	echo fail shorewall_zones
	((fail++))
	fail_string="${fail_string}
shorewall_zones failed with the message:
\"${out}\"
Couldn't create /etc/shorewall/zones.  This is a pretty serious problem!
"
fi ;else
	shorewall_zones=0;
	echo 'fail shorewall_zones PRECONDITION FAILED shorewall_installed'
fi ;
else
	echo pass shorewall_zones
	((pass++))
fi ;

#============ auto_logout_appended =============
out=$(grep '^TMOUT=7200
readonly TMOUT
export TMOUT' /etc/profile;);
test="";
if [ "${out}" = "${test}" ] ; then
	auto_logout_appended=0;
else
	auto_logout_appended=1;
fi ;
if [ "$auto_logout_appended" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail auto_logout_appended CONFIGURING'
	sudo bash -c 'echo "TMOUT=7200
readonly TMOUT
export TMOUT" >> /etc/profile;';
	echo 'fail auto_logout_appended RETESTING'
auto_logout_appended=0;
out=$(grep '^TMOUT=7200
readonly TMOUT
export TMOUT' /etc/profile;);
test="";
if [ "${out}" = "${test}" ] ; then
	auto_logout_appended=0;
else
	auto_logout_appended=1;
fi ;
if [ "$auto_logout_appended" = "1" ] ; then
	echo pass auto_logout_appended
	((pass++))
else
	echo fail auto_logout_appended
	((fail++))
	fail_string="${fail_string}
auto_logout_appended failed with the message:
\"${out}\"
Couldn't set the serial timeout. This means users who forget to log out won't be auto logged out after two hours.
"
fi ;else
	auto_logout_appended=0;
	echo 'fail auto_logout_appended PRECONDITION FAILED proceed'
fi ;
else
	echo pass auto_logout_appended
	((pass++))
fi ;

#============ ssh_dir_ed_created =============
out=$(sudo [ -d /home/ed/.ssh ] && echo pass || echo fail;);
test="pass";
if [ "${out}" != "${test}" ] ; then
	ssh_dir_ed_created=0;
else
	ssh_dir_ed_created=1;
fi ;
if [ "$ssh_dir_ed_created" != "1" ] ; then
if [ "$sshd_config" = "1" ] ; then
	echo 'fail ssh_dir_ed_created CONFIGURING'
	sudo mkdir -p /home/ed/.ssh;
	echo 'fail ssh_dir_ed_created RETESTING'
ssh_dir_ed_created=0;
out=$(sudo [ -d /home/ed/.ssh ] && echo pass || echo fail;);
test="pass";
if [ "${out}" != "${test}" ] ; then
	ssh_dir_ed_created=0;
else
	ssh_dir_ed_created=1;
fi ;
if [ "$ssh_dir_ed_created" = "1" ] ; then
	echo pass ssh_dir_ed_created
	((pass++))
else
	echo fail ssh_dir_ed_created
	((fail++))
	fail_string="${fail_string}
ssh_dir_ed_created failed with the message:
\"${out}\"
Couldn't create /home/ed/.ssh.  This is pretty serious!
"
fi ;else
	ssh_dir_ed_created=0;
	echo 'fail ssh_dir_ed_created PRECONDITION FAILED sshd_config'
fi ;
else
	echo pass ssh_dir_ed_created
	((pass++))
fi ;

#============ ssh_dir_ed_chmoded =============
out=$(sudo stat -c %a /home/ed/.ssh;);
test="755";
if [ "${out}" != "${test}" ] ; then
	ssh_dir_ed_chmoded=0;
else
	ssh_dir_ed_chmoded=1;
fi ;
if [ "$ssh_dir_ed_chmoded" != "1" ] ; then
if [ "$ssh_dir_ed_chowned" = "1" ] ; then
	echo 'fail ssh_dir_ed_chmoded CONFIGURING'
	sudo chmod -R 755 /home/ed/.ssh;
	echo 'fail ssh_dir_ed_chmoded RETESTING'
ssh_dir_ed_chmoded=0;
out=$(sudo stat -c %a /home/ed/.ssh;);
test="755";
if [ "${out}" != "${test}" ] ; then
	ssh_dir_ed_chmoded=0;
else
	ssh_dir_ed_chmoded=1;
fi ;
if [ "$ssh_dir_ed_chmoded" = "1" ] ; then
	echo pass ssh_dir_ed_chmoded
	((pass++))
else
	echo fail ssh_dir_ed_chmoded
	((fail++))
	fail_string="${fail_string}
ssh_dir_ed_chmoded failed with the message:
\"${out}\"
Couldn't change the permissions of /home/ed/.ssh to 755
"
fi ;else
	ssh_dir_ed_chmoded=0;
	echo 'fail ssh_dir_ed_chmoded PRECONDITION FAILED ssh_dir_ed_chowned'
fi ;
else
	echo pass ssh_dir_ed_chmoded
	((pass++))
fi ;

#============ dns_installed =============
out=$(dpkg-query --status unbound 2>&1 | grep -E "Status: (install|hold) ok installed";);
test="";
if [ "${out}" = "${test}" ] ; then
	dns_installed=0;
else
	dns_installed=1;
fi ;
if [ "$dns_installed" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail dns_installed CONFIGURING'
	export DEBIAN_FRONTEND=noninteractive; sudo apt-get update;sudo -E apt-get install --assume-yes unbound;
	echo 'fail dns_installed RETESTING'
dns_installed=0;
out=$(dpkg-query --status unbound 2>&1 | grep -E "Status: (install|hold) ok installed";);
test="";
if [ "${out}" = "${test}" ] ; then
	dns_installed=0;
else
	dns_installed=1;
fi ;
if [ "$dns_installed" = "1" ] ; then
	echo pass dns_installed
	((pass++))
else
	echo fail dns_installed
	((fail++))
	fail_string="${fail_string}
dns_installed failed with the message:
\"${out}\"
Couldn't install unbound.  This is pretty serious.
"
fi ;else
	dns_installed=0;
	echo 'fail dns_installed PRECONDITION FAILED proceed'
fi ;
else
	echo pass dns_installed
	((pass++))
fi ;

#============ dhcp_installed =============
out=$(dpkg-query --status isc-dhcp-server 2>&1 | grep -E "Status: (install|hold) ok installed";);
test="";
if [ "${out}" = "${test}" ] ; then
	dhcp_installed=0;
else
	dhcp_installed=1;
fi ;
if [ "$dhcp_installed" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail dhcp_installed CONFIGURING'
	export DEBIAN_FRONTEND=noninteractive; sudo apt-get update;sudo -E apt-get install --assume-yes isc-dhcp-server;
	echo 'fail dhcp_installed RETESTING'
dhcp_installed=0;
out=$(dpkg-query --status isc-dhcp-server 2>&1 | grep -E "Status: (install|hold) ok installed";);
test="";
if [ "${out}" = "${test}" ] ; then
	dhcp_installed=0;
else
	dhcp_installed=1;
fi ;
if [ "$dhcp_installed" = "1" ] ; then
	echo pass dhcp_installed
	((pass++))
else
	echo fail dhcp_installed
	((fail++))
	fail_string="${fail_string}
dhcp_installed failed with the message:
\"${out}\"
Couldn't install isc-dhcp-server.  This is pretty serious.
"
fi ;else
	dhcp_installed=0;
	echo 'fail dhcp_installed PRECONDITION FAILED proceed'
fi ;
else
	echo pass dhcp_installed
	((pass++))
fi ;

#============ ssh_key_ed_chmoded =============
out=$(sudo stat -c %a /home/ed/.ssh/authorized_keys;);
test="644";
if [ "${out}" != "${test}" ] ; then
	ssh_key_ed_chmoded=0;
else
	ssh_key_ed_chmoded=1;
fi ;
if [ "$ssh_key_ed_chmoded" != "1" ] ; then
if [ "$ssh_key_ed_chowned" = "1" ] ; then
	echo 'fail ssh_key_ed_chmoded CONFIGURING'
	sudo chmod 644 /home/ed/.ssh/authorized_keys;
	echo 'fail ssh_key_ed_chmoded RETESTING'
ssh_key_ed_chmoded=0;
out=$(sudo stat -c %a /home/ed/.ssh/authorized_keys;);
test="644";
if [ "${out}" != "${test}" ] ; then
	ssh_key_ed_chmoded=0;
else
	ssh_key_ed_chmoded=1;
fi ;
if [ "$ssh_key_ed_chmoded" = "1" ] ; then
	echo pass ssh_key_ed_chmoded
	((pass++))
else
	echo fail ssh_key_ed_chmoded
	((fail++))
	fail_string="${fail_string}
ssh_key_ed_chmoded failed with the message:
\"${out}\"
Couldn't change the permissions of /home/ed/.ssh/authorized_keys to 644
"
fi ;else
	ssh_key_ed_chmoded=0;
	echo 'fail ssh_key_ed_chmoded PRECONDITION FAILED ssh_key_ed_chowned'
fi ;
else
	echo pass ssh_key_ed_chmoded
	((pass++))
fi ;

#============ ssh_key_ed =============
out=$(sudo cat ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIKW15rwHNl1R/VfYxCxVeA5+C7PZKf99ufsXyyLyNbUo 2>&1;);
test="";
if [ "${out}" != "${test}" ] ; then
	ssh_key_ed=0;
else
	ssh_key_ed=1;
fi ;
if [ "$ssh_key_ed" != "1" ] ; then
if [ "$ssh_dir_ed_created" = "1" ] ; then
	echo 'fail ssh_key_ed CONFIGURING'
	null
	echo 'fail ssh_key_ed RETESTING'
ssh_key_ed=0;
out=$(sudo cat ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIKW15rwHNl1R/VfYxCxVeA5+C7PZKf99ufsXyyLyNbUo 2>&1;);
test="";
if [ "${out}" != "${test}" ] ; then
	ssh_key_ed=0;
else
	ssh_key_ed=1;
fi ;
if [ "$ssh_key_ed" = "1" ] ; then
	echo pass ssh_key_ed
	((pass++))
else
	echo fail ssh_key_ed
	((fail++))
	fail_string="${fail_string}
ssh_key_ed failed with the message:
\"${out}\"
/home/ed/.ssh/authorized_keys
"
fi ;else
	ssh_key_ed=0;
	echo 'fail ssh_key_ed PRECONDITION FAILED ssh_dir_ed_created'
fi ;
else
	echo pass ssh_key_ed
	((pass++))
fi ;

#============ guest_dhcpd_live_config =============
out=$(sudo cat /etc/dhcp/dhcpd.conf.d/guest.conf 2>&1;);
test="subnet 172.31.0.1 netmask 255.255.0.0 {}

group guest {
	server-name \"guest.router.geraghty.london\";
	option routers 172.31.0.1;
	option domain-name-servers 172.31.0.1;
}";
if [ "${out}" != "${test}" ] ; then
	guest_dhcpd_live_config=0;
else
	guest_dhcpd_live_config=1;
fi ;
if [ "$guest_dhcpd_live_config" != "1" ] ; then
if [ "$dhcp_installed" = "1" ] ; then
	echo 'fail guest_dhcpd_live_config CONFIGURING'
	sudo [ -f /etc/dhcp/dhcpd.conf.d/guest.conf ] || sudo touch /etc/dhcp/dhcpd.conf.d/guest.conf;echo "subnet 172.31.0.1 netmask 255.255.0.0 {}

group guest {
	server-name \"guest.router.geraghty.london\";
	option routers 172.31.0.1;
	option domain-name-servers 172.31.0.1;
}" | sudo tee /etc/dhcp/dhcpd.conf.d/guest.conf > /dev/null
	echo 'fail guest_dhcpd_live_config RETESTING'
guest_dhcpd_live_config=0;
out=$(sudo cat /etc/dhcp/dhcpd.conf.d/guest.conf 2>&1;);
test="subnet 172.31.0.1 netmask 255.255.0.0 {}

group guest {
	server-name \"guest.router.geraghty.london\";
	option routers 172.31.0.1;
	option domain-name-servers 172.31.0.1;
}";
if [ "${out}" != "${test}" ] ; then
	guest_dhcpd_live_config=0;
else
	guest_dhcpd_live_config=1;
fi ;
if [ "$guest_dhcpd_live_config" = "1" ] ; then
	echo pass guest_dhcpd_live_config
	((pass++))
else
	echo fail guest_dhcpd_live_config
	((fail++))
	fail_string="${fail_string}
guest_dhcpd_live_config failed with the message:
\"${out}\"
Couldn't create /etc/dhcp/dhcpd.conf.d/guest.conf.  This is a pretty serious problem!
"
fi ;else
	guest_dhcpd_live_config=0;
	echo 'fail guest_dhcpd_live_config PRECONDITION FAILED dhcp_installed'
fi ;
else
	echo pass guest_dhcpd_live_config
	((pass++))
fi ;

#============ sshd_running =============
out=$(sudo systemctl status sshd | grep -v grep | grep Active: | awk '{print $2 $3}');
test="active(running)";
if [ "${out}" != "${test}" ] ; then
	sshd_running=0;
else
	sshd_running=1;
fi ;
if [ "$sshd_running" != "1" ] ; then
if [ "$sshd_installed" = "1" ] ; then
	echo 'fail sshd_running CONFIGURING'
	sudo service sshd restart;
	echo 'fail sshd_running RETESTING'
sshd_running=0;
out=$(sudo systemctl status sshd | grep -v grep | grep Active: | awk '{print $2 $3}');
test="active(running)";
if [ "${out}" != "${test}" ] ; then
	sshd_running=0;
else
	sshd_running=1;
fi ;
if [ "$sshd_running" = "1" ] ; then
	echo pass sshd_running
	((pass++))
else
	echo fail sshd_running
	((fail++))
	fail_string="${fail_string}
sshd_running failed with the message:
\"${out}\"
I can't get sshd running.  This could be due to a misconfiguration, or a dependency on something yet to be configured.  Try restarting the service if things aren't working as expected.
"
fi ;else
	sshd_running=0;
	echo 'fail sshd_running PRECONDITION FAILED sshd_installed'
fi ;
else
	echo pass sshd_running
	((pass++))
fi ;

#============ speedtest_cli_installed =============
out=$(dpkg-query --status speedtest-cli 2>&1 | grep -E "Status: (install|hold) ok installed";);
test="";
if [ "${out}" = "${test}" ] ; then
	speedtest_cli_installed=0;
else
	speedtest_cli_installed=1;
fi ;
if [ "$speedtest_cli_installed" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail speedtest_cli_installed CONFIGURING'
	export DEBIAN_FRONTEND=noninteractive; sudo apt-get update;sudo -E apt-get install --assume-yes speedtest-cli;
	echo 'fail speedtest_cli_installed RETESTING'
speedtest_cli_installed=0;
out=$(dpkg-query --status speedtest-cli 2>&1 | grep -E "Status: (install|hold) ok installed";);
test="";
if [ "${out}" = "${test}" ] ; then
	speedtest_cli_installed=0;
else
	speedtest_cli_installed=1;
fi ;
if [ "$speedtest_cli_installed" = "1" ] ; then
	echo pass speedtest_cli_installed
	((pass++))
else
	echo fail speedtest_cli_installed
	((fail++))
	fail_string="${fail_string}
speedtest_cli_installed failed with the message:
\"${out}\"
Couldn't install speedtest-cli.  This is pretty serious.
"
fi ;else
	speedtest_cli_installed=0;
	echo 'fail speedtest_cli_installed PRECONDITION FAILED proceed'
fi ;
else
	echo pass speedtest_cli_installed
	((pass++))
fi ;

#============ shorewall_policies =============
out=$(sudo cat /etc/shorewall/policy 2>&1;);
test="#Default policies to use for intra-zone communication
#For specific rules, please look at /etc/shorewall/rules
#Please see http://shorewall.net/manpages/shorewall-policy.html for more details
#source       destination action
wan           all         DROP
fw            all         REJECT
servers       all         REJECT
users         all         REJECT
admins        all         REJECT
internalOnlys all         REJECT
externalOnlys all         REJECT";
if [ "${out}" != "${test}" ] ; then
	shorewall_policies=0;
else
	shorewall_policies=1;
fi ;
if [ "$shorewall_policies" != "1" ] ; then
if [ "$shorewall_installed" = "1" ] ; then
	echo 'fail shorewall_policies CONFIGURING'
	sudo [ -f /etc/shorewall/policy ] || sudo touch /etc/shorewall/policy;echo "#Default policies to use for intra-zone communication
#For specific rules, please look at /etc/shorewall/rules
#Please see http://shorewall.net/manpages/shorewall-policy.html for more details
#source       destination action
wan           all         DROP
fw            all         REJECT
servers       all         REJECT
users         all         REJECT
admins        all         REJECT
internalOnlys all         REJECT
externalOnlys all         REJECT" | sudo tee /etc/shorewall/policy > /dev/null
	echo 'fail shorewall_policies RETESTING'
shorewall_policies=0;
out=$(sudo cat /etc/shorewall/policy 2>&1;);
test="#Default policies to use for intra-zone communication
#For specific rules, please look at /etc/shorewall/rules
#Please see http://shorewall.net/manpages/shorewall-policy.html for more details
#source       destination action
wan           all         DROP
fw            all         REJECT
servers       all         REJECT
users         all         REJECT
admins        all         REJECT
internalOnlys all         REJECT
externalOnlys all         REJECT";
if [ "${out}" != "${test}" ] ; then
	shorewall_policies=0;
else
	shorewall_policies=1;
fi ;
if [ "$shorewall_policies" = "1" ] ; then
	echo pass shorewall_policies
	((pass++))
else
	echo fail shorewall_policies
	((fail++))
	fail_string="${fail_string}
shorewall_policies failed with the message:
\"${out}\"
Couldn't create /etc/shorewall/policy.  This is a pretty serious problem!
"
fi ;else
	shorewall_policies=0;
	echo 'fail shorewall_policies PRECONDITION FAILED shorewall_installed'
fi ;
else
	echo pass shorewall_policies
	((pass++))
fi ;

#============ no_raw_sockets =============
out=$(sudo lsof | grep RAW | grep -v "dhcpd");
test="";
if [ "${out}" != "${test}" ] ; then
	no_raw_sockets=0;
else
	no_raw_sockets=1;
fi ;
if [ "$no_raw_sockets" != "1" ] ; then
if [ "$lsof_installed" = "1" ] ; then
	echo 'fail no_raw_sockets CONFIGURING'
	
	echo 'fail no_raw_sockets RETESTING'
no_raw_sockets=0;
out=$(sudo lsof | grep RAW | grep -v "dhcpd");
test="";
if [ "${out}" != "${test}" ] ; then
	no_raw_sockets=0;
else
	no_raw_sockets=1;
fi ;
if [ "$no_raw_sockets" = "1" ] ; then
	echo pass no_raw_sockets
	((pass++))
else
	echo fail no_raw_sockets
	((fail++))
	fail_string="${fail_string}
no_raw_sockets failed with the message:
\"${out}\"
There are raw sockets running on this machine.  This is almost certainly a sign of compromise.
"
fi ;else
	no_raw_sockets=0;
	echo 'fail no_raw_sockets PRECONDITION FAILED lsof_installed'
fi ;
else
	echo pass no_raw_sockets
	((pass++))
fi ;

#============ iptables_disabled =============
out=$(systemctl is-enabled iptables);
test="disabled";
if [ "${out}" != "${test}" ] ; then
	iptables_disabled=0;
else
	iptables_disabled=1;
fi ;
if [ "$iptables_disabled" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail iptables_disabled CONFIGURING'
	sudo systemctl disable iptables
	echo 'fail iptables_disabled RETESTING'
iptables_disabled=0;
out=$(systemctl is-enabled iptables);
test="disabled";
if [ "${out}" != "${test}" ] ; then
	iptables_disabled=0;
else
	iptables_disabled=1;
fi ;
if [ "$iptables_disabled" = "1" ] ; then
	echo pass iptables_disabled
	((pass++))
else
	echo fail iptables_disabled
	((fail++))
	fail_string="${fail_string}
iptables_disabled failed with the message:
\"${out}\"
This is a placeholder.  I don't know whether this failure is good, bad, or indifferent.  I'm sorry!
"
fi ;else
	iptables_disabled=0;
	echo 'fail iptables_disabled PRECONDITION FAILED proceed'
fi ;
else
	echo pass iptables_disabled
	((pass++))
fi ;

#============ decrease_apt_timeout =============
out=$(sudo cat /etc/apt/apt.conf.d/99timeout 2>&1;);
test="Acquire::http::Timeout \"3\";
Acquire::ftp::Timeout \"3\";";
if [ "${out}" != "${test}" ] ; then
	decrease_apt_timeout=0;
else
	decrease_apt_timeout=1;
fi ;
if [ "$decrease_apt_timeout" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail decrease_apt_timeout CONFIGURING'
	sudo [ -f /etc/apt/apt.conf.d/99timeout ] || sudo touch /etc/apt/apt.conf.d/99timeout;echo "Acquire::http::Timeout \"3\";
Acquire::ftp::Timeout \"3\";" | sudo tee /etc/apt/apt.conf.d/99timeout > /dev/null
	echo 'fail decrease_apt_timeout RETESTING'
decrease_apt_timeout=0;
out=$(sudo cat /etc/apt/apt.conf.d/99timeout 2>&1;);
test="Acquire::http::Timeout \"3\";
Acquire::ftp::Timeout \"3\";";
if [ "${out}" != "${test}" ] ; then
	decrease_apt_timeout=0;
else
	decrease_apt_timeout=1;
fi ;
if [ "$decrease_apt_timeout" = "1" ] ; then
	echo pass decrease_apt_timeout
	((pass++))
else
	echo fail decrease_apt_timeout
	((fail++))
	fail_string="${fail_string}
decrease_apt_timeout failed with the message:
\"${out}\"
Couldn't decrease the apt timeout. If your network connection is poor, the machine may appear to hang during configuration
"
fi ;else
	decrease_apt_timeout=0;
	echo 'fail decrease_apt_timeout PRECONDITION FAILED proceed'
fi ;
else
	echo pass decrease_apt_timeout
	((pass++))
fi ;

#============ guest_netdev =============
out=$(sudo cat /etc/systemd/network/guest.netdev 2>&1;);
test="[NetDev]
Name=guest
Kind=macvlan

[MACVLAN]
Mode=vepa";
if [ "${out}" != "${test}" ] ; then
	guest_netdev=0;
else
	guest_netdev=1;
fi ;
if [ "$guest_netdev" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail guest_netdev CONFIGURING'
	sudo [ -f /etc/systemd/network/guest.netdev ] || sudo touch /etc/systemd/network/guest.netdev;echo "[NetDev]
Name=guest
Kind=macvlan

[MACVLAN]
Mode=vepa" | sudo tee /etc/systemd/network/guest.netdev > /dev/null
	echo 'fail guest_netdev RETESTING'
guest_netdev=0;
out=$(sudo cat /etc/systemd/network/guest.netdev 2>&1;);
test="[NetDev]
Name=guest
Kind=macvlan

[MACVLAN]
Mode=vepa";
if [ "${out}" != "${test}" ] ; then
	guest_netdev=0;
else
	guest_netdev=1;
fi ;
if [ "$guest_netdev" = "1" ] ; then
	echo pass guest_netdev
	((pass++))
else
	echo fail guest_netdev
	((fail++))
	fail_string="${fail_string}
guest_netdev failed with the message:
\"${out}\"
Couldn't create /etc/systemd/network/guest.netdev.  This is a pretty serious problem!
"
fi ;else
	guest_netdev=0;
	echo 'fail guest_netdev PRECONDITION FAILED proceed'
fi ;
else
	echo pass guest_netdev
	((pass++))
fi ;

#============ externalonlydevice_netdev =============
out=$(sudo cat /etc/systemd/network/externalonlydevice.netdev 2>&1;);
test="[NetDev]
Name=externalonlydevice
Kind=macvlan

[MACVLAN]
Mode=vepa";
if [ "${out}" != "${test}" ] ; then
	externalonlydevice_netdev=0;
else
	externalonlydevice_netdev=1;
fi ;
if [ "$externalonlydevice_netdev" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail externalonlydevice_netdev CONFIGURING'
	sudo [ -f /etc/systemd/network/externalonlydevice.netdev ] || sudo touch /etc/systemd/network/externalonlydevice.netdev;echo "[NetDev]
Name=externalonlydevice
Kind=macvlan

[MACVLAN]
Mode=vepa" | sudo tee /etc/systemd/network/externalonlydevice.netdev > /dev/null
	echo 'fail externalonlydevice_netdev RETESTING'
externalonlydevice_netdev=0;
out=$(sudo cat /etc/systemd/network/externalonlydevice.netdev 2>&1;);
test="[NetDev]
Name=externalonlydevice
Kind=macvlan

[MACVLAN]
Mode=vepa";
if [ "${out}" != "${test}" ] ; then
	externalonlydevice_netdev=0;
else
	externalonlydevice_netdev=1;
fi ;
if [ "$externalonlydevice_netdev" = "1" ] ; then
	echo pass externalonlydevice_netdev
	((pass++))
else
	echo fail externalonlydevice_netdev
	((fail++))
	fail_string="${fail_string}
externalonlydevice_netdev failed with the message:
\"${out}\"
Couldn't create /etc/systemd/network/externalonlydevice.netdev.  This is a pretty serious problem!
"
fi ;else
	externalonlydevice_netdev=0;
	echo 'fail externalonlydevice_netdev PRECONDITION FAILED proceed'
fi ;
else
	echo pass externalonlydevice_netdev
	((pass++))
fi ;

#============ no_additional_ssh_keys =============
out=$(for X in $(cut -f6 -d ':' /etc/passwd |sort |uniq); do   for suffix in "" "2"; do       if sudo [ -s "${X}/.ssh/authorized_keys$suffix" ]; then           cat "${X}/.ssh/authorized_keys$suffix";       fi;   done;done | grep -v "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIKW15rwHNl1R/VfYxCxVeA5+C7PZKf99ufsXyyLyNbUo");
test="";
if [ "${out}" != "${test}" ] ; then
	no_additional_ssh_keys=0;
else
	no_additional_ssh_keys=1;
fi ;
if [ "$no_additional_ssh_keys" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail no_additional_ssh_keys CONFIGURING'
	
	echo 'fail no_additional_ssh_keys RETESTING'
no_additional_ssh_keys=0;
out=$(for X in $(cut -f6 -d ':' /etc/passwd |sort |uniq); do   for suffix in "" "2"; do       if sudo [ -s "${X}/.ssh/authorized_keys$suffix" ]; then           cat "${X}/.ssh/authorized_keys$suffix";       fi;   done;done | grep -v "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIKW15rwHNl1R/VfYxCxVeA5+C7PZKf99ufsXyyLyNbUo");
test="";
if [ "${out}" != "${test}" ] ; then
	no_additional_ssh_keys=0;
else
	no_additional_ssh_keys=1;
fi ;
if [ "$no_additional_ssh_keys" = "1" ] ; then
	echo pass no_additional_ssh_keys
	((pass++))
else
	echo fail no_additional_ssh_keys
	((fail++))
	fail_string="${fail_string}
no_additional_ssh_keys failed with the message:
\"${out}\"
There are unexpected SSH keys on this machine.  This is almost certainly an indicator that this machine has been compromised!
"
fi ;else
	no_additional_ssh_keys=0;
	echo 'fail no_additional_ssh_keys PRECONDITION FAILED proceed'
fi ;
else
	echo pass no_additional_ssh_keys
	((pass++))
fi ;

#============ sshd_moduli_exists =============
out=$(cat /etc/ssh/moduli);
test="";
if [ "${out}" = "${test}" ] ; then
	sshd_moduli_exists=0;
else
	sshd_moduli_exists=1;
fi ;
if [ "$sshd_moduli_exists" != "1" ] ; then
if [ "$sshd_config" = "1" ] ; then
	echo 'fail sshd_moduli_exists CONFIGURING'
	sudo ssh-keygen -G /etc/ssh/moduli.all -b 4096;sudo ssh-keygen -T /etc/ssh/moduli.safe -f /etc/ssh/moduli.all;sudo mv /etc/ssh/moduli.safe /etc/ssh/moduli;sudo rm /etc/ssh/moduli.all
	echo 'fail sshd_moduli_exists RETESTING'
sshd_moduli_exists=0;
out=$(cat /etc/ssh/moduli);
test="";
if [ "${out}" = "${test}" ] ; then
	sshd_moduli_exists=0;
else
	sshd_moduli_exists=1;
fi ;
if [ "$sshd_moduli_exists" = "1" ] ; then
	echo pass sshd_moduli_exists
	((pass++))
else
	echo fail sshd_moduli_exists
	((fail++))
	fail_string="${fail_string}
sshd_moduli_exists failed with the message:
\"${out}\"
Couldn't generate new moduli for your SSH daemon.  This is undesirable, please try re-running the script.
"
fi ;else
	sshd_moduli_exists=0;
	echo 'fail sshd_moduli_exists PRECONDITION FAILED sshd_config'
fi ;
else
	echo pass sshd_moduli_exists
	((pass++))
fi ;

#============ dns_running =============
out=$(sudo systemctl status unbound | grep -v grep | grep Active: | awk '{print $2 $3}');
test="active(running)";
if [ "${out}" != "${test}" ] ; then
	dns_running=0;
else
	dns_running=1;
fi ;
if [ "$dns_running" != "1" ] ; then
if [ "$dns_installed" = "1" ] ; then
	echo 'fail dns_running CONFIGURING'
	sudo service unbound restart;
	echo 'fail dns_running RETESTING'
dns_running=0;
out=$(sudo systemctl status unbound | grep -v grep | grep Active: | awk '{print $2 $3}');
test="active(running)";
if [ "${out}" != "${test}" ] ; then
	dns_running=0;
else
	dns_running=1;
fi ;
if [ "$dns_running" = "1" ] ; then
	echo pass dns_running
	((pass++))
else
	echo fail dns_running
	((fail++))
	fail_string="${fail_string}
dns_running failed with the message:
\"${out}\"
I can't get unbound running.  This could be due to a misconfiguration, or a dependency on something yet to be configured.  Try restarting the service if things aren't working as expected.
"
fi ;else
	dns_running=0;
	echo 'fail dns_running PRECONDITION FAILED dns_installed'
fi ;
else
	echo pass dns_running
	((pass++))
fi ;

#============ user_network =============
out=$(sudo cat /etc/systemd/network/user.network 2>&1;);
test="[Match]
Name=user

[Network]
IPForward=yes
Address=172.16.0.1/16";
if [ "${out}" != "${test}" ] ; then
	user_network=0;
else
	user_network=1;
fi ;
if [ "$user_network" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail user_network CONFIGURING'
	sudo [ -f /etc/systemd/network/user.network ] || sudo touch /etc/systemd/network/user.network;echo "[Match]
Name=user

[Network]
IPForward=yes
Address=172.16.0.1/16" | sudo tee /etc/systemd/network/user.network > /dev/null
	echo 'fail user_network RETESTING'
user_network=0;
out=$(sudo cat /etc/systemd/network/user.network 2>&1;);
test="[Match]
Name=user

[Network]
IPForward=yes
Address=172.16.0.1/16";
if [ "${out}" != "${test}" ] ; then
	user_network=0;
else
	user_network=1;
fi ;
if [ "$user_network" = "1" ] ; then
	echo pass user_network
	((pass++))
else
	echo fail user_network
	((fail++))
	fail_string="${fail_string}
user_network failed with the message:
\"${out}\"
Couldn't create /etc/systemd/network/user.network.  This is a pretty serious problem!
"
fi ;else
	user_network=0;
	echo 'fail user_network PRECONDITION FAILED proceed'
fi ;
else
	echo pass user_network
	((pass++))
fi ;

#============ adblock_up_to_date =============
out=$([ ! -f /etc/unbound/rawhosts ] && echo fail || wget -O - -o /dev/null https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts | cmp /etc/unbound/rawhosts 2>&1);
test="";
if [ "${out}" != "${test}" ] ; then
	adblock_up_to_date=0;
else
	adblock_up_to_date=1;
fi ;
if [ "$adblock_up_to_date" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail adblock_up_to_date CONFIGURING'
	sudo wget -O /etc/unbound/rawhosts https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts && cat /etc/unbound/rawhosts | grep '^0\.0\.0\.0' | awk '{print "local-zone: \""$2"\" redirect\nlocal-data: \""$2" A 0.0.0.0\""}' | sudo tee /etc/unbound/unbound.conf.d/adblock.zone > /dev/null && sudo service unbound restart
	echo 'fail adblock_up_to_date RETESTING'
adblock_up_to_date=0;
out=$([ ! -f /etc/unbound/rawhosts ] && echo fail || wget -O - -o /dev/null https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts | cmp /etc/unbound/rawhosts 2>&1);
test="";
if [ "${out}" != "${test}" ] ; then
	adblock_up_to_date=0;
else
	adblock_up_to_date=1;
fi ;
if [ "$adblock_up_to_date" = "1" ] ; then
	echo pass adblock_up_to_date
	((pass++))
else
	echo fail adblock_up_to_date
	((fail++))
	fail_string="${fail_string}
adblock_up_to_date failed with the message:
\"${out}\"
This is a placeholder.  I don't know whether this failure is good, bad, or indifferent.  I'm sorry!
"
fi ;else
	adblock_up_to_date=0;
	echo 'fail adblock_up_to_date PRECONDITION FAILED proceed'
fi ;
else
	echo pass adblock_up_to_date
	((pass++))
fi ;

#============ sshd_motd =============
out=$(sudo cat /etc/update-motd.d/00-motd 2>&1;);
test="#!/bin/bash
echo \"This machine is a Thornsec configured machine.\"
echo \"_Logging in to configure this machine manually is highly discouraged!_\"
echo \"Please only continue if you know what you're doing!\"
echo
date=\`date\`
load=\`cat /proc/loadavg | awk '{print \$1}'\`
root_usage=\`df -h / | awk '/\// {print \$(NF-1)}'\`
memory_usage=\`free -m | awk '/Mem/ { printf(\"%3.1f%%\", \$3/\$2*100) }'\`
swap_usage=\`free -m | awk '/Swap/ { printf(\"%3.1f%%\", \$3/\$2*100) }'\`
users=\`users | wc -w\`
echo \"System information as of: \${date}\"
printf \"System load:\t%s\tMemory usage:\t%s\n\" \${load} \${memory_usage}
printf \"Usage on /:\t%s\tSwap usage:\t%s\n\" \${root_usage} \${swap_usage}
printf \"Currently logged in users:\t%s\n\" \${users}
echo \"HERE BE DRAGONS.\"
";
if [ "${out}" != "${test}" ] ; then
	sshd_motd=0;
else
	sshd_motd=1;
fi ;
if [ "$sshd_motd" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail sshd_motd CONFIGURING'
	sudo [ -f /etc/update-motd.d/00-motd ] || sudo touch /etc/update-motd.d/00-motd;echo "#!/bin/bash
echo \"This machine is a Thornsec configured machine.\"
echo \"_Logging in to configure this machine manually is highly discouraged!_\"
echo \"Please only continue if you know what you're doing!\"
echo
date=\`date\`
load=\`cat /proc/loadavg | awk '{print \$1}'\`
root_usage=\`df -h / | awk '/\// {print \$(NF-1)}'\`
memory_usage=\`free -m | awk '/Mem/ { printf(\"%3.1f%%\", \$3/\$2*100) }'\`
swap_usage=\`free -m | awk '/Swap/ { printf(\"%3.1f%%\", \$3/\$2*100) }'\`
users=\`users | wc -w\`
echo \"System information as of: \${date}\"
printf \"System load:\t%s\tMemory usage:\t%s\n\" \${load} \${memory_usage}
printf \"Usage on /:\t%s\tSwap usage:\t%s\n\" \${root_usage} \${swap_usage}
printf \"Currently logged in users:\t%s\n\" \${users}
echo \"HERE BE DRAGONS.\"
" | sudo tee /etc/update-motd.d/00-motd > /dev/null
	echo 'fail sshd_motd RETESTING'
sshd_motd=0;
out=$(sudo cat /etc/update-motd.d/00-motd 2>&1;);
test="#!/bin/bash
echo \"This machine is a Thornsec configured machine.\"
echo \"_Logging in to configure this machine manually is highly discouraged!_\"
echo \"Please only continue if you know what you're doing!\"
echo
date=\`date\`
load=\`cat /proc/loadavg | awk '{print \$1}'\`
root_usage=\`df -h / | awk '/\// {print \$(NF-1)}'\`
memory_usage=\`free -m | awk '/Mem/ { printf(\"%3.1f%%\", \$3/\$2*100) }'\`
swap_usage=\`free -m | awk '/Swap/ { printf(\"%3.1f%%\", \$3/\$2*100) }'\`
users=\`users | wc -w\`
echo \"System information as of: \${date}\"
printf \"System load:\t%s\tMemory usage:\t%s\n\" \${load} \${memory_usage}
printf \"Usage on /:\t%s\tSwap usage:\t%s\n\" \${root_usage} \${swap_usage}
printf \"Currently logged in users:\t%s\n\" \${users}
echo \"HERE BE DRAGONS.\"
";
if [ "${out}" != "${test}" ] ; then
	sshd_motd=0;
else
	sshd_motd=1;
fi ;
if [ "$sshd_motd" = "1" ] ; then
	echo pass sshd_motd
	((pass++))
else
	echo fail sshd_motd
	((fail++))
	fail_string="${fail_string}
sshd_motd failed with the message:
\"${out}\"
Couldn't create /etc/update-motd.d/00-motd.  This is a pretty serious problem!
"
fi ;else
	sshd_motd=0;
	echo 'fail sshd_motd PRECONDITION FAILED proceed'
fi ;
else
	echo pass sshd_motd
	((pass++))
fi ;

#============ leave_my_resolv_conf_alone_chmoded =============
out=$(sudo stat -c %a /etc/dhcp/dhclient-enter-hooks.d/leave_my_resolv_conf_alone;);
test="755";
if [ "${out}" != "${test}" ] ; then
	leave_my_resolv_conf_alone_chmoded=0;
else
	leave_my_resolv_conf_alone_chmoded=1;
fi ;
if [ "$leave_my_resolv_conf_alone_chmoded" != "1" ] ; then
if [ "$leave_my_resolv_conf_alone" = "1" ] ; then
	echo 'fail leave_my_resolv_conf_alone_chmoded CONFIGURING'
	sudo chmod 755 /etc/dhcp/dhclient-enter-hooks.d/leave_my_resolv_conf_alone;
	echo 'fail leave_my_resolv_conf_alone_chmoded RETESTING'
leave_my_resolv_conf_alone_chmoded=0;
out=$(sudo stat -c %a /etc/dhcp/dhclient-enter-hooks.d/leave_my_resolv_conf_alone;);
test="755";
if [ "${out}" != "${test}" ] ; then
	leave_my_resolv_conf_alone_chmoded=0;
else
	leave_my_resolv_conf_alone_chmoded=1;
fi ;
if [ "$leave_my_resolv_conf_alone_chmoded" = "1" ] ; then
	echo pass leave_my_resolv_conf_alone_chmoded
	((pass++))
else
	echo fail leave_my_resolv_conf_alone_chmoded
	((fail++))
	fail_string="${fail_string}
leave_my_resolv_conf_alone_chmoded failed with the message:
\"${out}\"
I couldn't stop various systemd services deciding to override your DNS settings. This will cause you intermittent, difficult to diagnose problems as it randomly sets your DNS to wherever it decides. Great for laptops/desktops, atrocious for servers...
"
fi ;else
	leave_my_resolv_conf_alone_chmoded=0;
	echo 'fail leave_my_resolv_conf_alone_chmoded PRECONDITION FAILED leave_my_resolv_conf_alone'
fi ;
else
	echo pass leave_my_resolv_conf_alone_chmoded
	((pass++))
fi ;

#============ internalonlydevice_network =============
out=$(sudo cat /etc/systemd/network/internalonlydevice.network 2>&1;);
test="[Match]
Name=internalonlydevice

[Network]
IPForward=yes
Address=172.24.0.1/16";
if [ "${out}" != "${test}" ] ; then
	internalonlydevice_network=0;
else
	internalonlydevice_network=1;
fi ;
if [ "$internalonlydevice_network" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail internalonlydevice_network CONFIGURING'
	sudo [ -f /etc/systemd/network/internalonlydevice.network ] || sudo touch /etc/systemd/network/internalonlydevice.network;echo "[Match]
Name=internalonlydevice

[Network]
IPForward=yes
Address=172.24.0.1/16" | sudo tee /etc/systemd/network/internalonlydevice.network > /dev/null
	echo 'fail internalonlydevice_network RETESTING'
internalonlydevice_network=0;
out=$(sudo cat /etc/systemd/network/internalonlydevice.network 2>&1;);
test="[Match]
Name=internalonlydevice

[Network]
IPForward=yes
Address=172.24.0.1/16";
if [ "${out}" != "${test}" ] ; then
	internalonlydevice_network=0;
else
	internalonlydevice_network=1;
fi ;
if [ "$internalonlydevice_network" = "1" ] ; then
	echo pass internalonlydevice_network
	((pass++))
else
	echo fail internalonlydevice_network
	((fail++))
	fail_string="${fail_string}
internalonlydevice_network failed with the message:
\"${out}\"
Couldn't create /etc/systemd/network/internalonlydevice.network.  This is a pretty serious problem!
"
fi ;else
	internalonlydevice_network=0;
	echo 'fail internalonlydevice_network PRECONDITION FAILED proceed'
fi ;
else
	echo pass internalonlydevice_network
	((pass++))
fi ;

#============ user_dhcpd_live_config =============
out=$(sudo cat /etc/dhcp/dhcpd.conf.d/user.conf 2>&1;);
test="subnet 172.16.0.1 netmask 255.255.0.0 {}

group user {
	server-name \"user.router.geraghty.london\";
	option routers 172.16.0.1;
	option domain-name-servers 172.16.0.1;
	host ed-bc:5f:f4:b0:91:93 {
		hardware ethernet bc:5f:f4:b0:91:93;
		fixed-address 172.16.0.2/16;
	}
	host ed+laptop-3c:97:0e:c5:b3:0a {
		hardware ethernet 3c:97:0e:c5:b3:0a;
	host ed+laptop-a4:4e:31:63:22:84 {
		hardware ethernet a4:4e:31:63:22:84;
		fixed-address 172.16.0.3/16;
	host ed+phone-cc:73:14:61:df:6b {
		hardware ethernet cc:73:14:61:df:6b;
	host luisa+desktop-08:11:96:7c:65:b8 {
		hardware ethernet 08:11:96:7c:65:b8;
	host luisa+phone-2c:59:8a:6c:3f:5c {
		hardware ethernet 2c:59:8a:6c:3f:5c;
	host luisa+laptop-d0:50:99:2d:30:ca {
		hardware ethernet d0:50:99:2d:30:ca;
	host projector-00:e0:4c:68:6e:1d {
		hardware ethernet 00:e0:4c:68:6e:1d;
}";
if [ "${out}" != "${test}" ] ; then
	user_dhcpd_live_config=0;
else
	user_dhcpd_live_config=1;
fi ;
if [ "$user_dhcpd_live_config" != "1" ] ; then
if [ "$dhcp_installed" = "1" ] ; then
	echo 'fail user_dhcpd_live_config CONFIGURING'
	sudo [ -f /etc/dhcp/dhcpd.conf.d/user.conf ] || sudo touch /etc/dhcp/dhcpd.conf.d/user.conf;echo "subnet 172.16.0.1 netmask 255.255.0.0 {}

group user {
	server-name \"user.router.geraghty.london\";
	option routers 172.16.0.1;
	option domain-name-servers 172.16.0.1;
	host ed-bc:5f:f4:b0:91:93 {
		hardware ethernet bc:5f:f4:b0:91:93;
		fixed-address 172.16.0.2/16;
	}
	host ed+laptop-3c:97:0e:c5:b3:0a {
		hardware ethernet 3c:97:0e:c5:b3:0a;
	host ed+laptop-a4:4e:31:63:22:84 {
		hardware ethernet a4:4e:31:63:22:84;
		fixed-address 172.16.0.3/16;
	host ed+phone-cc:73:14:61:df:6b {
		hardware ethernet cc:73:14:61:df:6b;
	host luisa+desktop-08:11:96:7c:65:b8 {
		hardware ethernet 08:11:96:7c:65:b8;
	host luisa+phone-2c:59:8a:6c:3f:5c {
		hardware ethernet 2c:59:8a:6c:3f:5c;
	host luisa+laptop-d0:50:99:2d:30:ca {
		hardware ethernet d0:50:99:2d:30:ca;
	host projector-00:e0:4c:68:6e:1d {
		hardware ethernet 00:e0:4c:68:6e:1d;
}" | sudo tee /etc/dhcp/dhcpd.conf.d/user.conf > /dev/null
	echo 'fail user_dhcpd_live_config RETESTING'
user_dhcpd_live_config=0;
out=$(sudo cat /etc/dhcp/dhcpd.conf.d/user.conf 2>&1;);
test="subnet 172.16.0.1 netmask 255.255.0.0 {}

group user {
	server-name \"user.router.geraghty.london\";
	option routers 172.16.0.1;
	option domain-name-servers 172.16.0.1;
	host ed-bc:5f:f4:b0:91:93 {
		hardware ethernet bc:5f:f4:b0:91:93;
		fixed-address 172.16.0.2/16;
	}
	host ed+laptop-3c:97:0e:c5:b3:0a {
		hardware ethernet 3c:97:0e:c5:b3:0a;
	host ed+laptop-a4:4e:31:63:22:84 {
		hardware ethernet a4:4e:31:63:22:84;
		fixed-address 172.16.0.3/16;
	host ed+phone-cc:73:14:61:df:6b {
		hardware ethernet cc:73:14:61:df:6b;
	host luisa+desktop-08:11:96:7c:65:b8 {
		hardware ethernet 08:11:96:7c:65:b8;
	host luisa+phone-2c:59:8a:6c:3f:5c {
		hardware ethernet 2c:59:8a:6c:3f:5c;
	host luisa+laptop-d0:50:99:2d:30:ca {
		hardware ethernet d0:50:99:2d:30:ca;
	host projector-00:e0:4c:68:6e:1d {
		hardware ethernet 00:e0:4c:68:6e:1d;
}";
if [ "${out}" != "${test}" ] ; then
	user_dhcpd_live_config=0;
else
	user_dhcpd_live_config=1;
fi ;
if [ "$user_dhcpd_live_config" = "1" ] ; then
	echo pass user_dhcpd_live_config
	((pass++))
else
	echo fail user_dhcpd_live_config
	((fail++))
	fail_string="${fail_string}
user_dhcpd_live_config failed with the message:
\"${out}\"
Couldn't create /etc/dhcp/dhcpd.conf.d/user.conf.  This is a pretty serious problem!
"
fi ;else
	user_dhcpd_live_config=0;
	echo 'fail user_dhcpd_live_config PRECONDITION FAILED dhcp_installed'
fi ;
else
	echo pass user_dhcpd_live_config
	((pass++))
fi ;

#============ dhcpd_confd_dir_created =============
out=$(sudo [ -d /etc/dhcp/dhcpd.conf.d ] && echo pass || echo fail;);
test="pass";
if [ "${out}" != "${test}" ] ; then
	dhcpd_confd_dir_created=0;
else
	dhcpd_confd_dir_created=1;
fi ;
if [ "$dhcpd_confd_dir_created" != "1" ] ; then
if [ "$dhcp_installed" = "1" ] ; then
	echo 'fail dhcpd_confd_dir_created CONFIGURING'
	sudo mkdir -p /etc/dhcp/dhcpd.conf.d;
	echo 'fail dhcpd_confd_dir_created RETESTING'
dhcpd_confd_dir_created=0;
out=$(sudo [ -d /etc/dhcp/dhcpd.conf.d ] && echo pass || echo fail;);
test="pass";
if [ "${out}" != "${test}" ] ; then
	dhcpd_confd_dir_created=0;
else
	dhcpd_confd_dir_created=1;
fi ;
if [ "$dhcpd_confd_dir_created" = "1" ] ; then
	echo pass dhcpd_confd_dir_created
	((pass++))
else
	echo fail dhcpd_confd_dir_created
	((fail++))
	fail_string="${fail_string}
dhcpd_confd_dir_created failed with the message:
\"${out}\"
Couldn't create /etc/dhcp/dhcpd.conf.d.  This is pretty serious!
"
fi ;else
	dhcpd_confd_dir_created=0;
	echo 'fail dhcpd_confd_dir_created PRECONDITION FAILED dhcp_installed'
fi ;
else
	echo pass dhcpd_confd_dir_created
	((pass++))
fi ;

#============ sshd_motd_perms_chmoded =============
out=$(sudo stat -c %a /etc/update-motd.d/00-motd;);
test="755";
if [ "${out}" != "${test}" ] ; then
	sshd_motd_perms_chmoded=0;
else
	sshd_motd_perms_chmoded=1;
fi ;
if [ "$sshd_motd_perms_chmoded" != "1" ] ; then
if [ "$sshd_motd_config" = "1" ] ; then
	echo 'fail sshd_motd_perms_chmoded CONFIGURING'
	sudo chmod 755 /etc/update-motd.d/00-motd;
	echo 'fail sshd_motd_perms_chmoded RETESTING'
sshd_motd_perms_chmoded=0;
out=$(sudo stat -c %a /etc/update-motd.d/00-motd;);
test="755";
if [ "${out}" != "${test}" ] ; then
	sshd_motd_perms_chmoded=0;
else
	sshd_motd_perms_chmoded=1;
fi ;
if [ "$sshd_motd_perms_chmoded" = "1" ] ; then
	echo pass sshd_motd_perms_chmoded
	((pass++))
else
	echo fail sshd_motd_perms_chmoded
	((fail++))
	fail_string="${fail_string}
sshd_motd_perms_chmoded failed with the message:
\"${out}\"
Couldn't change the permissions of /etc/update-motd.d/00-motd to 755
"
fi ;else
	sshd_motd_perms_chmoded=0;
	echo 'fail sshd_motd_perms_chmoded PRECONDITION FAILED sshd_motd_config'
fi ;
else
	echo pass sshd_motd_perms_chmoded
	((pass++))
fi ;

#============ externalonlydevice_dhcpd_live_config =============
out=$(sudo cat /etc/dhcp/dhcpd.conf.d/externalonlydevice.conf 2>&1;);
test="subnet 172.28.0.1 netmask 255.255.0.0 {}

group externalonlydevice {
	server-name \"externalonlydevice.router.geraghty.london\";
	option routers 172.28.0.1;
	option domain-name-servers 172.28.0.1;
}";
if [ "${out}" != "${test}" ] ; then
	externalonlydevice_dhcpd_live_config=0;
else
	externalonlydevice_dhcpd_live_config=1;
fi ;
if [ "$externalonlydevice_dhcpd_live_config" != "1" ] ; then
if [ "$dhcp_installed" = "1" ] ; then
	echo 'fail externalonlydevice_dhcpd_live_config CONFIGURING'
	sudo [ -f /etc/dhcp/dhcpd.conf.d/externalonlydevice.conf ] || sudo touch /etc/dhcp/dhcpd.conf.d/externalonlydevice.conf;echo "subnet 172.28.0.1 netmask 255.255.0.0 {}

group externalonlydevice {
	server-name \"externalonlydevice.router.geraghty.london\";
	option routers 172.28.0.1;
	option domain-name-servers 172.28.0.1;
}" | sudo tee /etc/dhcp/dhcpd.conf.d/externalonlydevice.conf > /dev/null
	echo 'fail externalonlydevice_dhcpd_live_config RETESTING'
externalonlydevice_dhcpd_live_config=0;
out=$(sudo cat /etc/dhcp/dhcpd.conf.d/externalonlydevice.conf 2>&1;);
test="subnet 172.28.0.1 netmask 255.255.0.0 {}

group externalonlydevice {
	server-name \"externalonlydevice.router.geraghty.london\";
	option routers 172.28.0.1;
	option domain-name-servers 172.28.0.1;
}";
if [ "${out}" != "${test}" ] ; then
	externalonlydevice_dhcpd_live_config=0;
else
	externalonlydevice_dhcpd_live_config=1;
fi ;
if [ "$externalonlydevice_dhcpd_live_config" = "1" ] ; then
	echo pass externalonlydevice_dhcpd_live_config
	((pass++))
else
	echo fail externalonlydevice_dhcpd_live_config
	((fail++))
	fail_string="${fail_string}
externalonlydevice_dhcpd_live_config failed with the message:
\"${out}\"
Couldn't create /etc/dhcp/dhcpd.conf.d/externalonlydevice.conf.  This is a pretty serious problem!
"
fi ;else
	externalonlydevice_dhcpd_live_config=0;
	echo 'fail externalonlydevice_dhcpd_live_config PRECONDITION FAILED dhcp_installed'
fi ;
else
	echo pass externalonlydevice_dhcpd_live_config
	((pass++))
fi ;

#============ ssh_key_ed_chowned =============
out=$(sudo stat -c %U:%G /home/ed/.ssh/authorized_keys;);
test="root:root";
if [ "${out}" != "${test}" ] ; then
	ssh_key_ed_chowned=0;
else
	ssh_key_ed_chowned=1;
fi ;
if [ "$ssh_key_ed_chowned" != "1" ] ; then
if [ "$ssh_key_ed" = "1" ] ; then
	echo 'fail ssh_key_ed_chowned CONFIGURING'
	sudo chown root:root /home/ed/.ssh/authorized_keys;
	echo 'fail ssh_key_ed_chowned RETESTING'
ssh_key_ed_chowned=0;
out=$(sudo stat -c %U:%G /home/ed/.ssh/authorized_keys;);
test="root:root";
if [ "${out}" != "${test}" ] ; then
	ssh_key_ed_chowned=0;
else
	ssh_key_ed_chowned=1;
fi ;
if [ "$ssh_key_ed_chowned" = "1" ] ; then
	echo pass ssh_key_ed_chowned
	((pass++))
else
	echo fail ssh_key_ed_chowned
	((fail++))
	fail_string="${fail_string}
ssh_key_ed_chowned failed with the message:
\"${out}\"
Couldn't change the ownership of /home/ed/.ssh/authorized_keys to root:root
"
fi ;else
	ssh_key_ed_chowned=0;
	echo 'fail ssh_key_ed_chowned PRECONDITION FAILED ssh_key_ed'
fi ;
else
	echo pass ssh_key_ed_chowned
	((pass++))
fi ;

#============ shorewall_installed =============
out=$(dpkg-query --status shorewall 2>&1 | grep -E "Status: (install|hold) ok installed";);
test="";
if [ "${out}" = "${test}" ] ; then
	shorewall_installed=0;
else
	shorewall_installed=1;
fi ;
if [ "$shorewall_installed" != "1" ] ; then
if [ "$iptables_disabled" = "1" ] ; then
	echo 'fail shorewall_installed CONFIGURING'
	export DEBIAN_FRONTEND=noninteractive; sudo apt-get update;sudo -E apt-get install --assume-yes shorewall;
	echo 'fail shorewall_installed RETESTING'
shorewall_installed=0;
out=$(dpkg-query --status shorewall 2>&1 | grep -E "Status: (install|hold) ok installed";);
test="";
if [ "${out}" = "${test}" ] ; then
	shorewall_installed=0;
else
	shorewall_installed=1;
fi ;
if [ "$shorewall_installed" = "1" ] ; then
	echo pass shorewall_installed
	((pass++))
else
	echo fail shorewall_installed
	((fail++))
	fail_string="${fail_string}
shorewall_installed failed with the message:
\"${out}\"
Couldn't install shorewall.  This is pretty serious.
"
fi ;else
	shorewall_installed=0;
	echo 'fail shorewall_installed PRECONDITION FAILED iptables_disabled'
fi ;
else
	echo pass shorewall_installed
	((pass++))
fi ;

#============ lsof_installed =============
out=$(dpkg-query --status lsof 2>&1 | grep -E "Status: (install|hold) ok installed";);
test="";
if [ "${out}" = "${test}" ] ; then
	lsof_installed=0;
else
	lsof_installed=1;
fi ;
if [ "$lsof_installed" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail lsof_installed CONFIGURING'
	export DEBIAN_FRONTEND=noninteractive; sudo apt-get update;sudo -E apt-get install --assume-yes lsof;
	echo 'fail lsof_installed RETESTING'
lsof_installed=0;
out=$(dpkg-query --status lsof 2>&1 | grep -E "Status: (install|hold) ok installed";);
test="";
if [ "${out}" = "${test}" ] ; then
	lsof_installed=0;
else
	lsof_installed=1;
fi ;
if [ "$lsof_installed" = "1" ] ; then
	echo pass lsof_installed
	((pass++))
else
	echo fail lsof_installed
	((fail++))
	fail_string="${fail_string}
lsof_installed failed with the message:
\"${out}\"
Couldn't install lsof.  This is pretty serious.
"
fi ;else
	lsof_installed=0;
	echo 'fail lsof_installed PRECONDITION FAILED proceed'
fi ;
else
	echo pass lsof_installed
	((pass++))
fi ;

#============ dhcpd_conf =============
out=$(sudo cat /etc/dhcp/dhcpd.conf 2>&1;);
test="#Options here are set globally across your whole network(s)
#Please see https://www.systutorials.com/docs/linux/man/5-dhcpd.conf/
#for more details
ddns-update-style none;
option domain-name \"geraghty.london\";
option domain-name-servers router geraghty.london;
default-lease-time 600;
max-lease-time 1800;
authoritative;
log-facility local7;

include \"/etc/dhcp/dhcpd.conf.d/server.conf\"
include \"/etc/dhcp/dhcpd.conf.d/user.conf\"
include \"/etc/dhcp/dhcpd.conf.d/administrator.conf\"
include \"/etc/dhcp/dhcpd.conf.d/internalonlydevice.conf\"
include \"/etc/dhcp/dhcpd.conf.d/externalonlydevice.conf\"
include \"/etc/dhcp/dhcpd.conf.d/guest.conf\"";
if [ "${out}" != "${test}" ] ; then
	dhcpd_conf=0;
else
	dhcpd_conf=1;
fi ;
if [ "$dhcpd_conf" != "1" ] ; then
if [ "$dhcp_installed" = "1" ] ; then
	echo 'fail dhcpd_conf CONFIGURING'
	sudo [ -f /etc/dhcp/dhcpd.conf ] || sudo touch /etc/dhcp/dhcpd.conf;echo "#Options here are set globally across your whole network(s)
#Please see https://www.systutorials.com/docs/linux/man/5-dhcpd.conf/
#for more details
ddns-update-style none;
option domain-name \"geraghty.london\";
option domain-name-servers router geraghty.london;
default-lease-time 600;
max-lease-time 1800;
authoritative;
log-facility local7;

include \"/etc/dhcp/dhcpd.conf.d/server.conf\"
include \"/etc/dhcp/dhcpd.conf.d/user.conf\"
include \"/etc/dhcp/dhcpd.conf.d/administrator.conf\"
include \"/etc/dhcp/dhcpd.conf.d/internalonlydevice.conf\"
include \"/etc/dhcp/dhcpd.conf.d/externalonlydevice.conf\"
include \"/etc/dhcp/dhcpd.conf.d/guest.conf\"" | sudo tee /etc/dhcp/dhcpd.conf > /dev/null
	echo 'fail dhcpd_conf RETESTING'
dhcpd_conf=0;
out=$(sudo cat /etc/dhcp/dhcpd.conf 2>&1;);
test="#Options here are set globally across your whole network(s)
#Please see https://www.systutorials.com/docs/linux/man/5-dhcpd.conf/
#for more details
ddns-update-style none;
option domain-name \"geraghty.london\";
option domain-name-servers router geraghty.london;
default-lease-time 600;
max-lease-time 1800;
authoritative;
log-facility local7;

include \"/etc/dhcp/dhcpd.conf.d/server.conf\"
include \"/etc/dhcp/dhcpd.conf.d/user.conf\"
include \"/etc/dhcp/dhcpd.conf.d/administrator.conf\"
include \"/etc/dhcp/dhcpd.conf.d/internalonlydevice.conf\"
include \"/etc/dhcp/dhcpd.conf.d/externalonlydevice.conf\"
include \"/etc/dhcp/dhcpd.conf.d/guest.conf\"";
if [ "${out}" != "${test}" ] ; then
	dhcpd_conf=0;
else
	dhcpd_conf=1;
fi ;
if [ "$dhcpd_conf" = "1" ] ; then
	echo pass dhcpd_conf
	((pass++))
else
	echo fail dhcpd_conf
	((fail++))
	fail_string="${fail_string}
dhcpd_conf failed with the message:
\"${out}\"
Couldn't create /etc/dhcp/dhcpd.conf.  This is a pretty serious problem!
"
fi ;else
	dhcpd_conf=0;
	echo 'fail dhcpd_conf PRECONDITION FAILED dhcp_installed'
fi ;
else
	echo pass dhcpd_conf
	((pass++))
fi ;

#============ htop_installed =============
out=$(dpkg-query --status htop 2>&1 | grep -E "Status: (install|hold) ok installed";);
test="";
if [ "${out}" = "${test}" ] ; then
	htop_installed=0;
else
	htop_installed=1;
fi ;
if [ "$htop_installed" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail htop_installed CONFIGURING'
	export DEBIAN_FRONTEND=noninteractive; sudo apt-get update;sudo -E apt-get install --assume-yes htop;
	echo 'fail htop_installed RETESTING'
htop_installed=0;
out=$(dpkg-query --status htop 2>&1 | grep -E "Status: (install|hold) ok installed";);
test="";
if [ "${out}" = "${test}" ] ; then
	htop_installed=0;
else
	htop_installed=1;
fi ;
if [ "$htop_installed" = "1" ] ; then
	echo pass htop_installed
	((pass++))
else
	echo fail htop_installed
	((fail++))
	fail_string="${fail_string}
htop_installed failed with the message:
\"${out}\"
Couldn't install htop.  This is pretty serious.
"
fi ;else
	htop_installed=0;
	echo 'fail htop_installed PRECONDITION FAILED proceed'
fi ;
else
	echo pass htop_installed
	((pass++))
fi ;

#============ server_netdev =============
out=$(sudo cat /etc/systemd/network/server.netdev 2>&1;);
test="[NetDev]
Name=server
Kind=macvlan

[MACVLAN]
Mode=vepa";
if [ "${out}" != "${test}" ] ; then
	server_netdev=0;
else
	server_netdev=1;
fi ;
if [ "$server_netdev" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail server_netdev CONFIGURING'
	sudo [ -f /etc/systemd/network/server.netdev ] || sudo touch /etc/systemd/network/server.netdev;echo "[NetDev]
Name=server
Kind=macvlan

[MACVLAN]
Mode=vepa" | sudo tee /etc/systemd/network/server.netdev > /dev/null
	echo 'fail server_netdev RETESTING'
server_netdev=0;
out=$(sudo cat /etc/systemd/network/server.netdev 2>&1;);
test="[NetDev]
Name=server
Kind=macvlan

[MACVLAN]
Mode=vepa";
if [ "${out}" != "${test}" ] ; then
	server_netdev=0;
else
	server_netdev=1;
fi ;
if [ "$server_netdev" = "1" ] ; then
	echo pass server_netdev
	((pass++))
else
	echo fail server_netdev
	((fail++))
	fail_string="${fail_string}
server_netdev failed with the message:
\"${out}\"
Couldn't create /etc/systemd/network/server.netdev.  This is a pretty serious problem!
"
fi ;else
	server_netdev=0;
	echo 'fail server_netdev PRECONDITION FAILED proceed'
fi ;
else
	echo pass server_netdev
	((pass++))
fi ;

#============ sysstat_installed =============
out=$(dpkg-query --status sysstat 2>&1 | grep -E "Status: (install|hold) ok installed";);
test="";
if [ "${out}" = "${test}" ] ; then
	sysstat_installed=0;
else
	sysstat_installed=1;
fi ;
if [ "$sysstat_installed" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail sysstat_installed CONFIGURING'
	export DEBIAN_FRONTEND=noninteractive; sudo apt-get update;sudo -E apt-get install --assume-yes sysstat;
	echo 'fail sysstat_installed RETESTING'
sysstat_installed=0;
out=$(dpkg-query --status sysstat 2>&1 | grep -E "Status: (install|hold) ok installed";);
test="";
if [ "${out}" = "${test}" ] ; then
	sysstat_installed=0;
else
	sysstat_installed=1;
fi ;
if [ "$sysstat_installed" = "1" ] ; then
	echo pass sysstat_installed
	((pass++))
else
	echo fail sysstat_installed
	((fail++))
	fail_string="${fail_string}
sysstat_installed failed with the message:
\"${out}\"
Couldn't install sysstat.  This is pretty serious.
"
fi ;else
	sysstat_installed=0;
	echo 'fail sysstat_installed PRECONDITION FAILED proceed'
fi ;
else
	echo pass sysstat_installed
	((pass++))
fi ;

#============ net_tools_installed =============
out=$(dpkg-query --status net-tools 2>&1 | grep -E "Status: (install|hold) ok installed";);
test="";
if [ "${out}" = "${test}" ] ; then
	net_tools_installed=0;
else
	net_tools_installed=1;
fi ;
if [ "$net_tools_installed" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail net_tools_installed CONFIGURING'
	export DEBIAN_FRONTEND=noninteractive; sudo apt-get update;sudo -E apt-get install --assume-yes net-tools;
	echo 'fail net_tools_installed RETESTING'
net_tools_installed=0;
out=$(dpkg-query --status net-tools 2>&1 | grep -E "Status: (install|hold) ok installed";);
test="";
if [ "${out}" = "${test}" ] ; then
	net_tools_installed=0;
else
	net_tools_installed=1;
fi ;
if [ "$net_tools_installed" = "1" ] ; then
	echo pass net_tools_installed
	((pass++))
else
	echo fail net_tools_installed
	((fail++))
	fail_string="${fail_string}
net_tools_installed failed with the message:
\"${out}\"
Couldn't install net-tools.  This is pretty serious.
"
fi ;else
	net_tools_installed=0;
	echo 'fail net_tools_installed PRECONDITION FAILED proceed'
fi ;
else
	echo pass net_tools_installed
	((pass++))
fi ;

#============ dhcp_running_running =============
out=$(sudo systemctl status isc-dhcp-server | grep -v grep | grep Active: | awk '{print $2 $3}');
test="active(running)";
if [ "${out}" != "${test}" ] ; then
	dhcp_running_running=0;
else
	dhcp_running_running=1;
fi ;
if [ "$dhcp_running_running" != "1" ] ; then
if [ "$dhcp_running_installed" = "1" ] ; then
	echo 'fail dhcp_running_running CONFIGURING'
	sudo service isc-dhcp-server restart;
	echo 'fail dhcp_running_running RETESTING'
dhcp_running_running=0;
out=$(sudo systemctl status isc-dhcp-server | grep -v grep | grep Active: | awk '{print $2 $3}');
test="active(running)";
if [ "${out}" != "${test}" ] ; then
	dhcp_running_running=0;
else
	dhcp_running_running=1;
fi ;
if [ "$dhcp_running_running" = "1" ] ; then
	echo pass dhcp_running_running
	((pass++))
else
	echo fail dhcp_running_running
	((fail++))
	fail_string="${fail_string}
dhcp_running_running failed with the message:
\"${out}\"
I can't get isc-dhcp-server running.  This could be due to a misconfiguration, or a dependency on something yet to be configured.  Try restarting the service if things aren't working as expected.
"
fi ;else
	dhcp_running_running=0;
	echo 'fail dhcp_running_running PRECONDITION FAILED dhcp_running_installed'
fi ;
else
	echo pass dhcp_running_running
	((pass++))
fi ;

#============ sshd_moduli_not_weak =============
out=$(awk '$5 <= 2000' /etc/ssh/moduli);
test="";
if [ "${out}" != "${test}" ] ; then
	sshd_moduli_not_weak=0;
else
	sshd_moduli_not_weak=1;
fi ;
if [ "$sshd_moduli_not_weak" != "1" ] ; then
if [ "$sshd_moduli_exists" = "1" ] ; then
	echo 'fail sshd_moduli_not_weak CONFIGURING'
	awk '$5 > 2000' /etc/ssh/moduli > /tmp/moduli;sudo mv /tmp/moduli /etc/ssh/moduli;
	echo 'fail sshd_moduli_not_weak RETESTING'
sshd_moduli_not_weak=0;
out=$(awk '$5 <= 2000' /etc/ssh/moduli);
test="";
if [ "${out}" != "${test}" ] ; then
	sshd_moduli_not_weak=0;
else
	sshd_moduli_not_weak=1;
fi ;
if [ "$sshd_moduli_not_weak" = "1" ] ; then
	echo pass sshd_moduli_not_weak
	((pass++))
else
	echo fail sshd_moduli_not_weak
	((fail++))
	fail_string="${fail_string}
sshd_moduli_not_weak failed with the message:
\"${out}\"
Couldn't remove weak moduli from your SSH daemon.  This is undesirable, as it weakens your security.  Please re-run the script to try and get this to work.
"
fi ;else
	sshd_moduli_not_weak=0;
	echo 'fail sshd_moduli_not_weak PRECONDITION FAILED sshd_moduli_exists'
fi ;
else
	echo pass sshd_moduli_not_weak
	((pass++))
fi ;

#============ no_unexpected_users =============
out=$(awk -F':' '{ print $1 }' /etc/passwd | egrep -v "^backup\$" | egrep -v "^unbound\$" | egrep -v "^lp\$" | egrep -v "^mail\$" | egrep -v "^bin\$" | egrep -v "^systemd-timesync\$" | egrep -v "^gnats\$" | egrep -v "^systemd-network\$" | egrep -v "^_apt\$" | egrep -v "^www-data\$" | egrep -v "^sys\$" | egrep -v "^systemd-resolve\$" | egrep -v "^root\$" | egrep -v "^games\$" | egrep -v "^statd\$" | egrep -v "^man\$" | egrep -v "^irc\$" | egrep -v "^nobody\$" | egrep -v "^ed\$" | egrep -v "^news\$" | egrep -v "^messagebus\$" | egrep -v "^sshd\$" | egrep -v "^list\$" | egrep -v "^sync\$" | egrep -v "^daemon\$" | egrep -v "^proxy\$" | egrep -v "^uucp\$" | egrep -v "^systemd-bus-proxy\$" | tee /dev/stderr | grep -v 'tee /dev/stderr');
test="";
if [ "${out}" != "${test}" ] ; then
	no_unexpected_users=0;
else
	no_unexpected_users=1;
fi ;
if [ "$no_unexpected_users" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail no_unexpected_users CONFIGURING'
	
	echo 'fail no_unexpected_users RETESTING'
no_unexpected_users=0;
out=$(awk -F':' '{ print $1 }' /etc/passwd | egrep -v "^backup\$" | egrep -v "^unbound\$" | egrep -v "^lp\$" | egrep -v "^mail\$" | egrep -v "^bin\$" | egrep -v "^systemd-timesync\$" | egrep -v "^gnats\$" | egrep -v "^systemd-network\$" | egrep -v "^_apt\$" | egrep -v "^www-data\$" | egrep -v "^sys\$" | egrep -v "^systemd-resolve\$" | egrep -v "^root\$" | egrep -v "^games\$" | egrep -v "^statd\$" | egrep -v "^man\$" | egrep -v "^irc\$" | egrep -v "^nobody\$" | egrep -v "^ed\$" | egrep -v "^news\$" | egrep -v "^messagebus\$" | egrep -v "^sshd\$" | egrep -v "^list\$" | egrep -v "^sync\$" | egrep -v "^daemon\$" | egrep -v "^proxy\$" | egrep -v "^uucp\$" | egrep -v "^systemd-bus-proxy\$" | tee /dev/stderr | grep -v 'tee /dev/stderr');
test="";
if [ "${out}" != "${test}" ] ; then
	no_unexpected_users=0;
else
	no_unexpected_users=1;
fi ;
if [ "$no_unexpected_users" = "1" ] ; then
	echo pass no_unexpected_users
	((pass++))
else
	echo fail no_unexpected_users
	((fail++))
	fail_string="${fail_string}
no_unexpected_users failed with the message:
\"${out}\"
There are unexpected user accounts on this machine.  This could be a sign that the machine is compromised, or it could be entirely innocent.  Please check the usernames carefully to see if there's any cause for concern.
"
fi ;else
	no_unexpected_users=0;
	echo 'fail no_unexpected_users PRECONDITION FAILED proceed'
fi ;
else
	echo pass no_unexpected_users
	((pass++))
fi ;

#============ traceroute_installed =============
out=$(dpkg-query --status traceroute 2>&1 | grep -E "Status: (install|hold) ok installed";);
test="";
if [ "${out}" = "${test}" ] ; then
	traceroute_installed=0;
else
	traceroute_installed=1;
fi ;
if [ "$traceroute_installed" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail traceroute_installed CONFIGURING'
	export DEBIAN_FRONTEND=noninteractive; sudo apt-get update;sudo -E apt-get install --assume-yes traceroute;
	echo 'fail traceroute_installed RETESTING'
traceroute_installed=0;
out=$(dpkg-query --status traceroute 2>&1 | grep -E "Status: (install|hold) ok installed";);
test="";
if [ "${out}" = "${test}" ] ; then
	traceroute_installed=0;
else
	traceroute_installed=1;
fi ;
if [ "$traceroute_installed" = "1" ] ; then
	echo pass traceroute_installed
	((pass++))
else
	echo fail traceroute_installed
	((fail++))
	fail_string="${fail_string}
traceroute_installed failed with the message:
\"${out}\"
Couldn't install traceroute.  This is pretty serious.
"
fi ;else
	traceroute_installed=0;
	echo 'fail traceroute_installed PRECONDITION FAILED proceed'
fi ;
else
	echo pass traceroute_installed
	((pass++))
fi ;

#============ motd_created =============
out=$(sudo [ -d /etc/update-motd.d/ ] && echo pass || echo fail;);
test="pass";
if [ "${out}" != "${test}" ] ; then
	motd_created=0;
else
	motd_created=1;
fi ;
if [ "$motd_created" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail motd_created CONFIGURING'
	sudo mkdir -p /etc/update-motd.d/;
	echo 'fail motd_created RETESTING'
motd_created=0;
out=$(sudo [ -d /etc/update-motd.d/ ] && echo pass || echo fail;);
test="pass";
if [ "${out}" != "${test}" ] ; then
	motd_created=0;
else
	motd_created=1;
fi ;
if [ "$motd_created" = "1" ] ; then
	echo pass motd_created
	((pass++))
else
	echo fail motd_created
	((fail++))
	fail_string="${fail_string}
motd_created failed with the message:
\"${out}\"
Couldn't create /etc/update-motd.d/.  This is pretty serious!
"
fi ;else
	motd_created=0;
	echo 'fail motd_created PRECONDITION FAILED proceed'
fi ;
else
	echo pass motd_created
	((pass++))
fi ;

#============ administrator_dhcpd_live_config =============
out=$(sudo cat /etc/dhcp/dhcpd.conf.d/administrator.conf 2>&1;);
test="subnet 172.20.0.1 netmask 255.255.0.0 {}

group administrator {
	server-name \"administrator.router.geraghty.london\";
	option routers 172.20.0.1;
	option domain-name-servers 172.20.0.1;
}";
if [ "${out}" != "${test}" ] ; then
	administrator_dhcpd_live_config=0;
else
	administrator_dhcpd_live_config=1;
fi ;
if [ "$administrator_dhcpd_live_config" != "1" ] ; then
if [ "$dhcp_installed" = "1" ] ; then
	echo 'fail administrator_dhcpd_live_config CONFIGURING'
	sudo [ -f /etc/dhcp/dhcpd.conf.d/administrator.conf ] || sudo touch /etc/dhcp/dhcpd.conf.d/administrator.conf;echo "subnet 172.20.0.1 netmask 255.255.0.0 {}

group administrator {
	server-name \"administrator.router.geraghty.london\";
	option routers 172.20.0.1;
	option domain-name-servers 172.20.0.1;
}" | sudo tee /etc/dhcp/dhcpd.conf.d/administrator.conf > /dev/null
	echo 'fail administrator_dhcpd_live_config RETESTING'
administrator_dhcpd_live_config=0;
out=$(sudo cat /etc/dhcp/dhcpd.conf.d/administrator.conf 2>&1;);
test="subnet 172.20.0.1 netmask 255.255.0.0 {}

group administrator {
	server-name \"administrator.router.geraghty.london\";
	option routers 172.20.0.1;
	option domain-name-servers 172.20.0.1;
}";
if [ "${out}" != "${test}" ] ; then
	administrator_dhcpd_live_config=0;
else
	administrator_dhcpd_live_config=1;
fi ;
if [ "$administrator_dhcpd_live_config" = "1" ] ; then
	echo pass administrator_dhcpd_live_config
	((pass++))
else
	echo fail administrator_dhcpd_live_config
	((fail++))
	fail_string="${fail_string}
administrator_dhcpd_live_config failed with the message:
\"${out}\"
Couldn't create /etc/dhcp/dhcpd.conf.d/administrator.conf.  This is a pretty serious problem!
"
fi ;else
	administrator_dhcpd_live_config=0;
	echo 'fail administrator_dhcpd_live_config PRECONDITION FAILED dhcp_installed'
fi ;
else
	echo pass administrator_dhcpd_live_config
	((pass++))
fi ;

#============ sshd_installed =============
out=$(dpkg-query --status openssh-server 2>&1 | grep -E "Status: (install|hold) ok installed";);
test="";
if [ "${out}" = "${test}" ] ; then
	sshd_installed=0;
else
	sshd_installed=1;
fi ;
if [ "$sshd_installed" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail sshd_installed CONFIGURING'
	export DEBIAN_FRONTEND=noninteractive; sudo apt-get update;sudo -E apt-get install --assume-yes openssh-server;
	echo 'fail sshd_installed RETESTING'
sshd_installed=0;
out=$(dpkg-query --status openssh-server 2>&1 | grep -E "Status: (install|hold) ok installed";);
test="";
if [ "${out}" = "${test}" ] ; then
	sshd_installed=0;
else
	sshd_installed=1;
fi ;
if [ "$sshd_installed" = "1" ] ; then
	echo pass sshd_installed
	((pass++))
else
	echo fail sshd_installed
	((fail++))
	fail_string="${fail_string}
sshd_installed failed with the message:
\"${out}\"
Couldn't install openssh-server.  This is pretty serious.
"
fi ;else
	sshd_installed=0;
	echo 'fail sshd_installed PRECONDITION FAILED proceed'
fi ;
else
	echo pass sshd_installed
	((pass++))
fi ;

#============ leave_my_resolv_conf_alone =============
out=$(sudo cat /etc/dhcp/dhclient-enter-hooks.d/leave_my_resolv_conf_alone 2>&1;);
test="make_resolv_conf() { :; }";
if [ "${out}" != "${test}" ] ; then
	leave_my_resolv_conf_alone=0;
else
	leave_my_resolv_conf_alone=1;
fi ;
if [ "$leave_my_resolv_conf_alone" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail leave_my_resolv_conf_alone CONFIGURING'
	sudo [ -f /etc/dhcp/dhclient-enter-hooks.d/leave_my_resolv_conf_alone ] || sudo touch /etc/dhcp/dhclient-enter-hooks.d/leave_my_resolv_conf_alone;echo "make_resolv_conf() { :; }" | sudo tee /etc/dhcp/dhclient-enter-hooks.d/leave_my_resolv_conf_alone > /dev/null
	echo 'fail leave_my_resolv_conf_alone RETESTING'
leave_my_resolv_conf_alone=0;
out=$(sudo cat /etc/dhcp/dhclient-enter-hooks.d/leave_my_resolv_conf_alone 2>&1;);
test="make_resolv_conf() { :; }";
if [ "${out}" != "${test}" ] ; then
	leave_my_resolv_conf_alone=0;
else
	leave_my_resolv_conf_alone=1;
fi ;
if [ "$leave_my_resolv_conf_alone" = "1" ] ; then
	echo pass leave_my_resolv_conf_alone
	((pass++))
else
	echo fail leave_my_resolv_conf_alone
	((fail++))
	fail_string="${fail_string}
leave_my_resolv_conf_alone failed with the message:
\"${out}\"
Couldn't create /etc/dhcp/dhclient-enter-hooks.d/leave_my_resolv_conf_alone.  This is a pretty serious problem!
"
fi ;else
	leave_my_resolv_conf_alone=0;
	echo 'fail leave_my_resolv_conf_alone PRECONDITION FAILED proceed'
fi ;
else
	echo pass leave_my_resolv_conf_alone
	((pass++))
fi ;

#============ systemd_networkd_enabled =============
out=$(sudo systemctl is-enabled systemd-networkd;);
test="enabled";
if [ "${out}" != "${test}" ] ; then
	systemd_networkd_enabled=0;
else
	systemd_networkd_enabled=1;
fi ;
if [ "$systemd_networkd_enabled" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail systemd_networkd_enabled CONFIGURING'
	sudo systemctl enable systemd-networkd;
	echo 'fail systemd_networkd_enabled RETESTING'
systemd_networkd_enabled=0;
out=$(sudo systemctl is-enabled systemd-networkd;);
test="enabled";
if [ "${out}" != "${test}" ] ; then
	systemd_networkd_enabled=0;
else
	systemd_networkd_enabled=1;
fi ;
if [ "$systemd_networkd_enabled" = "1" ] ; then
	echo pass systemd_networkd_enabled
	((pass++))
else
	echo fail systemd_networkd_enabled
	((fail++))
	fail_string="${fail_string}
systemd_networkd_enabled failed with the message:
\"${out}\"
I was unable to enable the networking service. This is bad!
"
fi ;else
	systemd_networkd_enabled=0;
	echo 'fail systemd_networkd_enabled PRECONDITION FAILED proceed'
fi ;
else
	echo pass systemd_networkd_enabled
	((pass++))
fi ;

#============ externalonlydevice_network =============
out=$(sudo cat /etc/systemd/network/externalonlydevice.network 2>&1;);
test="[Match]
Name=externalonlydevice

[Network]
IPForward=yes
Address=172.28.0.1/16";
if [ "${out}" != "${test}" ] ; then
	externalonlydevice_network=0;
else
	externalonlydevice_network=1;
fi ;
if [ "$externalonlydevice_network" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail externalonlydevice_network CONFIGURING'
	sudo [ -f /etc/systemd/network/externalonlydevice.network ] || sudo touch /etc/systemd/network/externalonlydevice.network;echo "[Match]
Name=externalonlydevice

[Network]
IPForward=yes
Address=172.28.0.1/16" | sudo tee /etc/systemd/network/externalonlydevice.network > /dev/null
	echo 'fail externalonlydevice_network RETESTING'
externalonlydevice_network=0;
out=$(sudo cat /etc/systemd/network/externalonlydevice.network 2>&1;);
test="[Match]
Name=externalonlydevice

[Network]
IPForward=yes
Address=172.28.0.1/16";
if [ "${out}" != "${test}" ] ; then
	externalonlydevice_network=0;
else
	externalonlydevice_network=1;
fi ;
if [ "$externalonlydevice_network" = "1" ] ; then
	echo pass externalonlydevice_network
	((pass++))
else
	echo fail externalonlydevice_network
	((fail++))
	fail_string="${fail_string}
externalonlydevice_network failed with the message:
\"${out}\"
Couldn't create /etc/systemd/network/externalonlydevice.network.  This is a pretty serious problem!
"
fi ;else
	externalonlydevice_network=0;
	echo 'fail externalonlydevice_network PRECONDITION FAILED proceed'
fi ;
else
	echo pass externalonlydevice_network
	((pass++))
fi ;

#============ internalonlydevice_dhcpd_live_config =============
out=$(sudo cat /etc/dhcp/dhcpd.conf.d/internalonlydevice.conf 2>&1;);
test="subnet 172.24.0.1 netmask 255.255.0.0 {}

group internalonlydevice {
	server-name \"internalonlydevice.router.geraghty.london\";
	option routers 172.24.0.1;
	option domain-name-servers 172.24.0.1;
	host printer-00:15:99:b2:c3:40 {
		hardware ethernet 00:15:99:b2:c3:40;
		fixed-address 172.24.0.2/16;
	}
}";
if [ "${out}" != "${test}" ] ; then
	internalonlydevice_dhcpd_live_config=0;
else
	internalonlydevice_dhcpd_live_config=1;
fi ;
if [ "$internalonlydevice_dhcpd_live_config" != "1" ] ; then
if [ "$dhcp_installed" = "1" ] ; then
	echo 'fail internalonlydevice_dhcpd_live_config CONFIGURING'
	sudo [ -f /etc/dhcp/dhcpd.conf.d/internalonlydevice.conf ] || sudo touch /etc/dhcp/dhcpd.conf.d/internalonlydevice.conf;echo "subnet 172.24.0.1 netmask 255.255.0.0 {}

group internalonlydevice {
	server-name \"internalonlydevice.router.geraghty.london\";
	option routers 172.24.0.1;
	option domain-name-servers 172.24.0.1;
	host printer-00:15:99:b2:c3:40 {
		hardware ethernet 00:15:99:b2:c3:40;
		fixed-address 172.24.0.2/16;
	}
}" | sudo tee /etc/dhcp/dhcpd.conf.d/internalonlydevice.conf > /dev/null
	echo 'fail internalonlydevice_dhcpd_live_config RETESTING'
internalonlydevice_dhcpd_live_config=0;
out=$(sudo cat /etc/dhcp/dhcpd.conf.d/internalonlydevice.conf 2>&1;);
test="subnet 172.24.0.1 netmask 255.255.0.0 {}

group internalonlydevice {
	server-name \"internalonlydevice.router.geraghty.london\";
	option routers 172.24.0.1;
	option domain-name-servers 172.24.0.1;
	host printer-00:15:99:b2:c3:40 {
		hardware ethernet 00:15:99:b2:c3:40;
		fixed-address 172.24.0.2/16;
	}
}";
if [ "${out}" != "${test}" ] ; then
	internalonlydevice_dhcpd_live_config=0;
else
	internalonlydevice_dhcpd_live_config=1;
fi ;
if [ "$internalonlydevice_dhcpd_live_config" = "1" ] ; then
	echo pass internalonlydevice_dhcpd_live_config
	((pass++))
else
	echo fail internalonlydevice_dhcpd_live_config
	((fail++))
	fail_string="${fail_string}
internalonlydevice_dhcpd_live_config failed with the message:
\"${out}\"
Couldn't create /etc/dhcp/dhcpd.conf.d/internalonlydevice.conf.  This is a pretty serious problem!
"
fi ;else
	internalonlydevice_dhcpd_live_config=0;
	echo 'fail internalonlydevice_dhcpd_live_config PRECONDITION FAILED dhcp_installed'
fi ;
else
	echo pass internalonlydevice_dhcpd_live_config
	((pass++))
fi ;

#============ no_unexpected_processes =============
out=$(sudo ps -Awwo pid,user,comm,args | grep -v grep | grep -v 'ps -Awwo pid,user,comm,args$' | egrep -v "/usr/sbin/cron -f$" | egrep -v "\[jfsSync\]$" | egrep -v "/usr/sbin/irqbalance --foreground$" | egrep -v "\[kthrotld\]$" | egrep -v "/usr/sbin/acpid$" | egrep -v "/usr/sbin/blkmapd$" | egrep -v "dirmngr --daemon --homedir /tmp/apt-key-gpghome.[a-zA-Z0-9]*$" | egrep -v "\[jfsIO\]$" | egrep -v "\[kcryptd\]$" | egrep -v "\[rcu_sched\]$" | egrep -v "\[vmstat\]$" | egrep -v "/lib/systemd/systemd --system --deserialize [0-9]{1,2}$" | egrep -v "\[bioset\]$" | egrep -v "/usr/sbin/rsyslogd -n$" | egrep -v "\[dio/dm-[0-9]\]$" | egrep -v "\[md[0-9]*_raid[0-9]\]$" | egrep -v "\./script.sh; rm -rf script\.sh; exit;" | egrep -v "/usr/sbin/sshd -D$" | egrep -v "\[kdmflush\]$" | egrep -v "\[ext4-rsv-conver\]$" | egrep -v "\[rpciod\]$" | egrep -v "\[ipv6_addrconf\]$" | egrep -v "\[writeback\]$" | egrep -v "\[rcu_bh\]$" | egrep -v "\[ksmd\]$" | egrep -v "\[crypto\]$" | egrep -v "\[cpuhp/[0-9]+\]$" | egrep -v "\[acpi_thermal_pm\]$" | egrep -v "\[kcryptd_io\]$" | egrep -v "\[i915/signal:[0-9]\]$" | egrep -v "/sbin/mdadm --monitor --scan$" | egrep -v "/sbin/agetty --noclear tty[0-9] linux$" | egrep -v "\[xfs_mru_cache\]$" | egrep -v "/bin/bash \./script.sh$" | egrep -v "\[ksoftirqd/[0-9]{1,2}\]$" | egrep -v "\[netns\]$" | egrep -v "\[hd-audio[0-9]*\]$" | egrep -v "\[scsi_eh_[0-9]{1,2}\]$" | egrep -v "\[khelper\]$" | egrep -v "\[fsnotify_mark\]$" | egrep -v "/lib/systemd/systemd-journald$" | egrep -v "\[kblockd\]$" | egrep -v "/lib/systemd/systemd --user$" | egrep -v "\[md[0-9]_raid[0-9]\]$" | egrep -v "\[watchdogd\]$" | egrep -v "/usr/bin/dbus-daemon --system --address=systemd: --nofork --nopidfile --systemd-activation$" | egrep -v "/usr/sbin/unbound -d$" | egrep -v "\[kdevtmpfs\]$" | egrep -v "/lib/systemd/systemd-udevd$" | egrep -v "\[dmcrypt_write\]$" | egrep -v "\[kthreadd\]$" | egrep -v "\[edac-poller\]$" | egrep -v "\[migration/[0-9]{1,2}\]$" | egrep -v "\[khubd\]$" | egrep -v "\[kworker/[u]{0,1}[0-9]{1,2}\:[0-9]{1,2}[H]{0,1}\]$" | egrep -v "\[irq/36-mei_me\]$" | egrep -v "\[khugepaged\]$" | egrep -v "\[md\]$" | egrep -v "\[led_workqueue\]$" | egrep -v "\[lru-add-drain\]$" | egrep -v "\[dio/sda[0-9]\]$" | egrep -v "\[kpsmoused\]$" | egrep -v "/sbin/lvmetad -f$" | egrep -v "\[deferwq\]$" | egrep -v "/sbin/dhclient -4 -v -pf /run/dhclient.enp[0-9]s[0-9].pid -lf /var/lib/dhcp/dhclient.enp[0-9]s[0-9].leases -I -df /var/lib/dhcp/dhclient6.enp[0-9]s[0-9].leases enp[0-9]s[0-9]$" | egrep -v "\[devfreq_wq\]$" | egrep -v "\[xfsalloc\]$" | egrep -v "\[jfsCommit\]$" | egrep -v "\[kintegrityd\]$" | egrep -v "\[kauditd\]$" | egrep -v "/lib/systemd/systemd --system --deserialize 14$" | egrep -v "/sbin/rdnssd -u rdnssd -H /etc/rdnssd/merge-hook$" | egrep -v "\[ttm_swap\]$" | egrep -v "/usr/lib/packagekit/packagekitd$" | egrep -v "\[ata_sff\]$" | egrep -v "\[dio/dm-0\]$" | egrep -v "/lib/systemd/systemd-timesyncd$" | egrep -v "\[kswapd0\]$" | egrep -v "/sbin/init$" | egrep -v "\[raid[0-9]wq\]$" | egrep -v "/sbin/rpcbind -f -w$" | egrep -v "/usr/sbin/dhcpd -4 -q -cf /etc/dhcp/dhcpd.conf" | egrep -v "\[jbd2/sda1-8\]$" | egrep -v "\[oom_reaper\]$" | egrep -v "\[scsi_tmf_[0-9]{1,2}\]$" | egrep -v "\[watchdog/[0-9]{1,2}\]$" | egrep -v "/usr/lib/policykit-1/polkitd --no-debug$" | egrep -v "\[jbd2/dm-[0-9]{1}-8\]$" | egrep -v "\[khungtaskd\]$" | egrep -v "/lib/systemd/systemd-logind$" | egrep -v "\[kcompactd0\]$" | egrep -v "\[xprtiod\]$" | egrep -v "\[usb-storage\]$" | egrep -v "\(sd-pam\)$" | egrep -v "\[kworker/0:0H\]$" | egrep -v "\[perf\]$" | egrep -v "jbd2/sdb[0-9]-[0-9]$" | tee /dev/stderr | grep -v 'tee /dev/stderr');
test="  PID USER     COMMAND         COMMAND";
if [ "${out}" != "${test}" ] ; then
	no_unexpected_processes=0;
else
	no_unexpected_processes=1;
fi ;
if [ "$no_unexpected_processes" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail no_unexpected_processes CONFIGURING'
	
	echo 'fail no_unexpected_processes RETESTING'
no_unexpected_processes=0;
out=$(sudo ps -Awwo pid,user,comm,args | grep -v grep | grep -v 'ps -Awwo pid,user,comm,args$' | egrep -v "/usr/sbin/cron -f$" | egrep -v "\[jfsSync\]$" | egrep -v "/usr/sbin/irqbalance --foreground$" | egrep -v "\[kthrotld\]$" | egrep -v "/usr/sbin/acpid$" | egrep -v "/usr/sbin/blkmapd$" | egrep -v "dirmngr --daemon --homedir /tmp/apt-key-gpghome.[a-zA-Z0-9]*$" | egrep -v "\[jfsIO\]$" | egrep -v "\[kcryptd\]$" | egrep -v "\[rcu_sched\]$" | egrep -v "\[vmstat\]$" | egrep -v "/lib/systemd/systemd --system --deserialize [0-9]{1,2}$" | egrep -v "\[bioset\]$" | egrep -v "/usr/sbin/rsyslogd -n$" | egrep -v "\[dio/dm-[0-9]\]$" | egrep -v "\[md[0-9]*_raid[0-9]\]$" | egrep -v "\./script.sh; rm -rf script\.sh; exit;" | egrep -v "/usr/sbin/sshd -D$" | egrep -v "\[kdmflush\]$" | egrep -v "\[ext4-rsv-conver\]$" | egrep -v "\[rpciod\]$" | egrep -v "\[ipv6_addrconf\]$" | egrep -v "\[writeback\]$" | egrep -v "\[rcu_bh\]$" | egrep -v "\[ksmd\]$" | egrep -v "\[crypto\]$" | egrep -v "\[cpuhp/[0-9]+\]$" | egrep -v "\[acpi_thermal_pm\]$" | egrep -v "\[kcryptd_io\]$" | egrep -v "\[i915/signal:[0-9]\]$" | egrep -v "/sbin/mdadm --monitor --scan$" | egrep -v "/sbin/agetty --noclear tty[0-9] linux$" | egrep -v "\[xfs_mru_cache\]$" | egrep -v "/bin/bash \./script.sh$" | egrep -v "\[ksoftirqd/[0-9]{1,2}\]$" | egrep -v "\[netns\]$" | egrep -v "\[hd-audio[0-9]*\]$" | egrep -v "\[scsi_eh_[0-9]{1,2}\]$" | egrep -v "\[khelper\]$" | egrep -v "\[fsnotify_mark\]$" | egrep -v "/lib/systemd/systemd-journald$" | egrep -v "\[kblockd\]$" | egrep -v "/lib/systemd/systemd --user$" | egrep -v "\[md[0-9]_raid[0-9]\]$" | egrep -v "\[watchdogd\]$" | egrep -v "/usr/bin/dbus-daemon --system --address=systemd: --nofork --nopidfile --systemd-activation$" | egrep -v "/usr/sbin/unbound -d$" | egrep -v "\[kdevtmpfs\]$" | egrep -v "/lib/systemd/systemd-udevd$" | egrep -v "\[dmcrypt_write\]$" | egrep -v "\[kthreadd\]$" | egrep -v "\[edac-poller\]$" | egrep -v "\[migration/[0-9]{1,2}\]$" | egrep -v "\[khubd\]$" | egrep -v "\[kworker/[u]{0,1}[0-9]{1,2}\:[0-9]{1,2}[H]{0,1}\]$" | egrep -v "\[irq/36-mei_me\]$" | egrep -v "\[khugepaged\]$" | egrep -v "\[md\]$" | egrep -v "\[led_workqueue\]$" | egrep -v "\[lru-add-drain\]$" | egrep -v "\[dio/sda[0-9]\]$" | egrep -v "\[kpsmoused\]$" | egrep -v "/sbin/lvmetad -f$" | egrep -v "\[deferwq\]$" | egrep -v "/sbin/dhclient -4 -v -pf /run/dhclient.enp[0-9]s[0-9].pid -lf /var/lib/dhcp/dhclient.enp[0-9]s[0-9].leases -I -df /var/lib/dhcp/dhclient6.enp[0-9]s[0-9].leases enp[0-9]s[0-9]$" | egrep -v "\[devfreq_wq\]$" | egrep -v "\[xfsalloc\]$" | egrep -v "\[jfsCommit\]$" | egrep -v "\[kintegrityd\]$" | egrep -v "\[kauditd\]$" | egrep -v "/lib/systemd/systemd --system --deserialize 14$" | egrep -v "/sbin/rdnssd -u rdnssd -H /etc/rdnssd/merge-hook$" | egrep -v "\[ttm_swap\]$" | egrep -v "/usr/lib/packagekit/packagekitd$" | egrep -v "\[ata_sff\]$" | egrep -v "\[dio/dm-0\]$" | egrep -v "/lib/systemd/systemd-timesyncd$" | egrep -v "\[kswapd0\]$" | egrep -v "/sbin/init$" | egrep -v "\[raid[0-9]wq\]$" | egrep -v "/sbin/rpcbind -f -w$" | egrep -v "/usr/sbin/dhcpd -4 -q -cf /etc/dhcp/dhcpd.conf" | egrep -v "\[jbd2/sda1-8\]$" | egrep -v "\[oom_reaper\]$" | egrep -v "\[scsi_tmf_[0-9]{1,2}\]$" | egrep -v "\[watchdog/[0-9]{1,2}\]$" | egrep -v "/usr/lib/policykit-1/polkitd --no-debug$" | egrep -v "\[jbd2/dm-[0-9]{1}-8\]$" | egrep -v "\[khungtaskd\]$" | egrep -v "/lib/systemd/systemd-logind$" | egrep -v "\[kcompactd0\]$" | egrep -v "\[xprtiod\]$" | egrep -v "\[usb-storage\]$" | egrep -v "\(sd-pam\)$" | egrep -v "\[kworker/0:0H\]$" | egrep -v "\[perf\]$" | egrep -v "jbd2/sdb[0-9]-[0-9]$" | tee /dev/stderr | grep -v 'tee /dev/stderr');
test="  PID USER     COMMAND         COMMAND";
if [ "${out}" != "${test}" ] ; then
	no_unexpected_processes=0;
else
	no_unexpected_processes=1;
fi ;
if [ "$no_unexpected_processes" = "1" ] ; then
	echo pass no_unexpected_processes
	((pass++))
else
	echo fail no_unexpected_processes
	((fail++))
	fail_string="${fail_string}
no_unexpected_processes failed with the message:
\"${out}\"
There are unexpected processes running on this machine.  This could be a sign that the machine is compromised, or it could be entirely innocent.  Please check the processes carefully to see if there's any cause for concern.  If this machine is a metal, it's not unexpected for there to be processes which haven't been explicitly whitelisted in our base config.
"
fi ;else
	no_unexpected_processes=0;
	echo 'fail no_unexpected_processes PRECONDITION FAILED proceed'
fi ;
else
	echo pass no_unexpected_processes
	((pass++))
fi ;

#============ apt_autoremove =============
out=$(sudo apt-get autoremove --purge --assume-no | grep "0 to remove");
test="";
if [ "${out}" = "${test}" ] ; then
	apt_autoremove=0;
else
	apt_autoremove=1;
fi ;
if [ "$apt_autoremove" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail apt_autoremove CONFIGURING'
	sudo apt-get autoremove --purge --assume-yes
	echo 'fail apt_autoremove RETESTING'
apt_autoremove=0;
out=$(sudo apt-get autoremove --purge --assume-no | grep "0 to remove");
test="";
if [ "${out}" = "${test}" ] ; then
	apt_autoremove=0;
else
	apt_autoremove=1;
fi ;
if [ "$apt_autoremove" = "1" ] ; then
	echo pass apt_autoremove
	((pass++))
else
	echo fail apt_autoremove
	((fail++))
	fail_string="${fail_string}
apt_autoremove failed with the message:
\"${out}\"
This is a placeholder.  I don't know whether this failure is good, bad, or indifferent.  I'm sorry!
"
fi ;else
	apt_autoremove=0;
	echo 'fail apt_autoremove PRECONDITION FAILED proceed'
fi ;
else
	echo pass apt_autoremove
	((pass++))
fi ;

#============ pam_not_tampered =============
out=$(find /lib/$(uname -m)-linux-gnu/security/ | xargs dpkg -S | cut -d ':' -f 1 | uniq | xargs sudo dpkg -V);
test="";
if [ "${out}" != "${test}" ] ; then
	pam_not_tampered=0;
else
	pam_not_tampered=1;
fi ;
if [ "$pam_not_tampered" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail pam_not_tampered CONFIGURING'
	
	echo 'fail pam_not_tampered RETESTING'
pam_not_tampered=0;
out=$(find /lib/$(uname -m)-linux-gnu/security/ | xargs dpkg -S | cut -d ':' -f 1 | uniq | xargs sudo dpkg -V);
test="";
if [ "${out}" != "${test}" ] ; then
	pam_not_tampered=0;
else
	pam_not_tampered=1;
fi ;
if [ "$pam_not_tampered" = "1" ] ; then
	echo pass pam_not_tampered
	((pass++))
else
	echo fail pam_not_tampered
	((fail++))
	fail_string="${fail_string}
pam_not_tampered failed with the message:
\"${out}\"
There are unexpected/tampered PAM modules on this machine.  This is almost certainly an indicator that this machine has been compromised!
"
fi ;else
	pam_not_tampered=0;
	echo 'fail pam_not_tampered PRECONDITION FAILED proceed'
fi ;
else
	echo pass pam_not_tampered
	((pass++))
fi ;

#============ dirmngr_installed =============
out=$(dpkg-query --status dirmngr 2>&1 | grep -E "Status: (install|hold) ok installed";);
test="";
if [ "${out}" = "${test}" ] ; then
	dirmngr_installed=0;
else
	dirmngr_installed=1;
fi ;
if [ "$dirmngr_installed" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail dirmngr_installed CONFIGURING'
	export DEBIAN_FRONTEND=noninteractive; sudo apt-get update;sudo -E apt-get install --assume-yes dirmngr;
	echo 'fail dirmngr_installed RETESTING'
dirmngr_installed=0;
out=$(dpkg-query --status dirmngr 2>&1 | grep -E "Status: (install|hold) ok installed";);
test="";
if [ "${out}" = "${test}" ] ; then
	dirmngr_installed=0;
else
	dirmngr_installed=1;
fi ;
if [ "$dirmngr_installed" = "1" ] ; then
	echo pass dirmngr_installed
	((pass++))
else
	echo fail dirmngr_installed
	((fail++))
	fail_string="${fail_string}
dirmngr_installed failed with the message:
\"${out}\"
Couldn't install dirmngr.  Anything which requires a PGP key to be downloaded and installed won't work. You can possibly fix this by running a configuration again.
"
fi ;else
	dirmngr_installed=0;
	echo 'fail dirmngr_installed PRECONDITION FAILED proceed'
fi ;
else
	echo pass dirmngr_installed
	((pass++))
fi ;

#============ host =============
out=$(sudo -S hostname;);
test="router";
if [ "${out}" != "${test}" ] ; then
	host=0;
else
	host=1;
fi ;
if [ "$host" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail host CONFIGURING'
	printf "ERROR: Configuring with hostname mismatch";
	echo 'fail host RETESTING'
host=0;
out=$(sudo -S hostname;);
test="router";
if [ "${out}" != "${test}" ] ; then
	host=0;
else
	host=1;
fi ;
if [ "$host" = "1" ] ; then
	echo pass host
	((pass++))
else
	echo fail host
	((fail++))
	fail_string="${fail_string}
host failed with the message:
\"${out}\"
This is a placeholder.  I don't know whether this failure is good, bad, or indifferent.  I'm sorry!
"
fi ;else
	host=0;
	echo 'fail host PRECONDITION FAILED proceed'
fi ;
else
	echo pass host
	((pass++))
fi ;

#============ apt_debian_sources =============
out=$(sudo cat /etc/apt/sources.list 2>&1;);
test="deb http://http://mirrorservice.org/sites/ftp.debian.org/debian/ buster main
deb http://security.debian.org/ buster/updates main
deb http://http://mirrorservice.org/sites/ftp.debian.org/debian/ buster-updates main";
if [ "${out}" != "${test}" ] ; then
	apt_debian_sources=0;
else
	apt_debian_sources=1;
fi ;
if [ "$apt_debian_sources" != "1" ] ; then
if [ "$proceed" = "1" ] ; then
	echo 'fail apt_debian_sources CONFIGURING'
	sudo [ -f /etc/apt/sources.list ] || sudo touch /etc/apt/sources.list;echo "deb http://http://mirrorservice.org/sites/ftp.debian.org/debian/ buster main
deb http://security.debian.org/ buster/updates main
deb http://http://mirrorservice.org/sites/ftp.debian.org/debian/ buster-updates main" | sudo tee /etc/apt/sources.list > /dev/null
	echo 'fail apt_debian_sources RETESTING'
apt_debian_sources=0;
out=$(sudo cat /etc/apt/sources.list 2>&1;);
test="deb http://http://mirrorservice.org/sites/ftp.debian.org/debian/ buster main
deb http://security.debian.org/ buster/updates main
deb http://http://mirrorservice.org/sites/ftp.debian.org/debian/ buster-updates main";
if [ "${out}" != "${test}" ] ; then
	apt_debian_sources=0;
else
	apt_debian_sources=1;
fi ;
if [ "$apt_debian_sources" = "1" ] ; then
	echo pass apt_debian_sources
	((pass++))
else
	echo fail apt_debian_sources
	((fail++))
	fail_string="${fail_string}
apt_debian_sources failed with the message:
\"${out}\"
Couldn't create /etc/apt/sources.list.  This is a pretty serious problem!
"
fi ;else
	apt_debian_sources=0;
	echo 'fail apt_debian_sources PRECONDITION FAILED proceed'
fi ;
else
	echo pass apt_debian_sources
	((pass++))
fi ;

echo "pass=$pass fail=$fail failed:$fail_string"


echo "Finished dryrun ${hostname} with config label: router"