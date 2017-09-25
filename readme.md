# ThornSec Profiles

Whilst ThornSec-Core is the framework which allows ThornSec to produce and run its unit tests, all of the "heavy lifting" is done in its various profiles.

A profile, at its most basic, is a series of unit tests to audit and/or configure a given service.

The profiles are called through reflection, so creating the class with the requisite methods (extending AStructuredProfile) and calling it from the config JSON is enough to get it to run.

There are five available methods.

## Class Instantiation Method
This calls super with its name.  This name must be unique.

## getPersistentConfig(String server, NetworkModel model)
This returns a Vector of unit tests required for the persistent configuration of this service.

This is generally used for configuration files which *should* never change between audits/configurations. 

## getInstalled(String server, NetworkModel model)
This returns a Vector of unit tests which check for/install various packages required for this service, and makes sure they're running.

## getPersistentFirewall(String server, NetworkModel model)
This returns a Vector of unit tests related to various firewalls, assumed to be persistent across different audits.

Since all firewalls on our network are designed to be explicit whitelists with default drop on all other traffic, it is imperitive that if your service is listening on a port, you declare it here.

This method allows you to either configure the firewall for the service, or to pass firewall rules up to the Router.  An example of this can be seen in any of the VPN classes, which DNAT their ports directly in the Router, as well as opening them locally.  This allows for a "zero-config" set of rules, invisible to the end user.

## getLiveConfig(String server, NetworkModel model)
This returns a Vector of unit tests for this service which may change depending on what's set in the configuration JSON.  For instance, this could add a new user to a service, whose definition has been added since the last time this service was audited/configured.

## getLiveFirewall(String server, NetworkModel model)
This returns a Vector of firewall unit tests for this service which may change based on what's set in the configuration JSON.  Whilst this is not used in any of the profiles yet, it is imagined that this could be used to override default listening ports for services based on config.

***

This is not a hard and fast rule, as sometimes it may make more sense to put a given unit test in the "wrong" method.  It may also make sense to have various private methods, where the only lines in the public methods is to call and add their unit tests.  A good example of this is in the Router class, which is doing a lot of atomic configurations which would be even more impenetrable if crowbarred into a single, huge method.

***

These methods are called in the following order:

1. getInstalled
2. getPersistentConfig
3. getPersistentFirewall
4. getLiveConfig
5. getLiveFirewall

So please bear that in mind when writing your tests.
