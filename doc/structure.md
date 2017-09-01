# Profile

A profile is a collection of unit tests. These may include other profiles, singletons and base unit tests.

## Compound

A compund profile is a collection of unit tests that may trigger an action if one of the fails. This is generally useful when multiple simple tests may require modification to config files. At the end of this, if one of those has been changed, the service needs to be restarted. Rather than trying to restart the service after each change, the restart occurs at the end of the block of tests that make up the compund profile.

# Singleton 

A singleton is something for which only one instance should be available globally, per network or per server. For example, iptables is a device singleton as many profiles may need to add configuration elements but only one test needs to be performed. The DNS profile is a good example of a profile where there is only one per network. A singleton cannot be created directly but through Class accessors that create the singleton if one doesnt already exist. 

## Network

## Server

# Unit 

The building blocks of the system are Unit tests. They are self contained and can provide an option to configure the system should the audit stage fail. 

## Complex

## Simple

A simple unit follows a very basic principle: run command A and if the output matches the value provided, the test either passes or fails. this is two tests due to the fact that in can be easier to audit for failure text when multiple possible outputs may indicate the test has passed. 

## Child

A child unit test attaches to a parent compound unit test. 

# Model



