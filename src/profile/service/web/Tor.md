# Tor.java

This configures a tor hidden service.  It inherits much of its fuctionality from Webproxy.java.

For now, this is untested, and will probably cause dramas in actual use!  Caveat emptor!*

*Pull requests welcome

## getPersistentConfig
Creates a hidden service proxy, based on Alec Muffet's eotk (https://github.com/alecmuffett/eotk)

## getInstalled
Inherits the proxy's getInstalled, and installs Tor from the mainline repo.

## getLiveConfig
Inherits Webproxy's getLiveConfig, and ensures Tor is running.
