#Router.java

This is our unit tests for configuring our Router.

This class pulls in the units from DNS.java && DHCP.java for their respective configurations.

It's a bit of a beast, but that makes sense when you see what it does!

##getPersistentConfig
Diables ipv6 in sysctl, enables ipv4 forwarding in sysctl.

Imports DNS && DHCP persistent config units.

Calls extConnConfigUnits, bandwidthThrottlingUnits, bandwidthThrottlingAlertUnits && dailyBandwidthEmailDigestUnits.

##getInstalled
Installs xsltproc && sendmail.

Imports DNS && DHCP getInstalled units.

##getPersistentFirewall
Imports DNS && DHCP persistent firewall units.

Calls deviceIptUnits && serverIptUnits.

##getLiveConfig
Imports DNS && DHCP live config units.

Calls subnetConfigUnits.


##buildDailyBandwidthEmail(String sender, String recipient, String subject, boolean includeBlurb)
Returns a String containing a sendmail-compatible email which gives a daily digest of in/egress chains on a per-device/server basis.

includeBlurb is for Users to get a more fluffy email.

##dailyBandwidthEmailDigestUnits
Builds our script to be called by our cron.daily.

Calls buildDailyBandwidthEmail for each user, device && server.  Resets each chain to 0 after emailing.

##bandwidthThrottlingAlertUnits
Builds our rsyslogd ommail config for alerting on traffic tagged as throttled by iptables.

##buildThrottledEmailAction
Builds the email stanza for ommail

##bandwidthThrottlingUnits
Builds the instantiation script for tc which actually does the throttling.  For the time being, all functionality is commented out so nothing is throttled.  This can be changed manually.

##subnetConfigUnits
Builds the interfaces for the Router to listen on - one per device on the network.

##serverIptUnits
Builds a chain for each server on our network.

DROPS all attempts to SSH from this server to another to stop lateral movement.

ACCEPTs all traffic to/from our server.

LOGs && ACCEPTs any in/egress traffic.

Everything else for servers is handled in their own firewall, meaning we can be fairly generic up here.

##deviceIptUnits
###Users
Creates a ingress, egress, and forward chain for each user.

ALLOWs any traffic to an internal server/service on :80 && :443.

DROPs any traffic to another user.

LOGs any egress traffic.

Any single TCP session >20MB is MARKed as 4, for further processing by tc/rsyslogd.

###Superusers
Inherits all rules from users, and ACCEPTs SSH traffic to servers/services.

###Servers
ACCEPTs all in/outbound traffic.

DROPs any SSH traffic, unless coming from a superuser.

###Internal-only
ACCEPTs all traffic to internal-only devices.

ACCEPTs ESTABLISHED && RELATED traffic from internal-only to users.

DROPS all else.

###External-only
Any single TCP session >20MB is MARKed as 3, for further processing by tc/rsyslogd.

Otherwise, all in/egress traffic is ACCEPTed with no logging.

##extConnUnits
Configures our ppp/dhcp/static external connection, including building any required ifaces.
