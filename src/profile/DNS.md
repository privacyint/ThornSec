# DNS.java

This is our unit tests for configuring DNS across our network.

This profile uses unbound for DNS, which has many security, speed, and resources advantages over the more widely-used bind9.

It is split out in this atomic fashion so that you may decide to re-implement DNS with bind9, should you decide this is required for your network.

## getPersistentConfig
This configures unbound using a template configuration from https://calomel.org/unbound_dns.html.

It will only listen on ipv4:53, allowing access from all private address ranges (so is agnostic as to your IP class choice).

It hides various metadata such as its identity and version, and hardens DNSSec calls, as well as using random caps in its returned IDs so as to mitigate MITM.  If ad blocking is set in the configuration, it will null-route the following DNS requests:

- doubleclick.net
- googlesyndication.com
- googleadservices.com
- google-analytics.com
- ads.youtube.com
- adserver.yahoo.com
- ask.com

As you can see, this is a rather blunt instrument for ad blocking on your network, and should be used with caution.  This list may be refined in future to be less blunt.

## getInstalled
This ensures that unbound is installed.

## getPersistentFirewall
This allows our DNS server to listen on TCP:53, TCP:953 && UDP:53 for internal traffic, and to call out to the configured external DNS server on the same ports. All calls to an external DNS server are logged in syslog with the prefix "ipt-dnsd:".

## getLiveConfig
This builds our local zone.  It gives forward and reverse DNS for each device/server defined in our network config, and will handle CNAMEs for a service, if configured.  It is worth noting that due to how unbound works, CNAMEs aren't ***CNAMEs*** in the true sense (as this requires fairly fiddly stub zones to be created, which will affect the readability of the configuration files.  Instead, it will create a forward and reverse A record for each CNAME.
