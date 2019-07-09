# Nginx.java
This is our class for creating and configuring an Nginx instance on a given service.

It's not really designed to be called directly from our config (although it can be, as it's a profile) but is designed to be called as an object from any profile which requires a web server front-end.

If you call it directly, it will be a base Nginx config, with no configuration outside of standard.

## getInstalled
Creates our nginx user, and creates its environment using bind-mounts for permissions.

Then installs nginx from the mainline repo - during dev, this was due to the debian repo not supporting http2, however it makes sense from a security standpoint to continue to track mainline.

## getLiveConfig
If Nginx has been called as a method from elsewhere, iterate through its passed configs and write them out.  Otherwise, write out a default config (listening on :80)

## addLiveConfig
If calling from another class, add a config.

## getPersistentFirewall
Allows traffic in on :80 && :443, and allows it out iff it's related.
