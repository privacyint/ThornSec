# Webproxy.java

This configures an Nginx reverse caching proxy.  It inherits much of its fuctionality from Nginx.java.

## getPersistentConfig
Creates a default config file, which 301 redirects all clear (:80) traffic to :443.

## getInstalled
Inherits Nginx's getInstalled, and installs openssl.

## getPersistentFirewall
DNATs :80 && :443 for each service it's proxying on the Router, ensuring it doesn't end up in a redirect loop(!)

## getLiveConfig
Creates a default SSL config file with some hardened headers.

Iterates through each service it's to proxy to, and builds their proxy config file (taking into account CNAMEs).  At the same time, creates the folder for the certs && keys.
