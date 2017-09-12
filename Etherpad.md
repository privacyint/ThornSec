#Etherpad.java

Creates an Etherpad-lite instance.  Inherits much functionality from Nginx.java, MariaDB.java && PHP.java.

##getInstalled
Installs Etherpad-lite's dependencies

##getPersistentConfig
Builds nodejs 6.x from mainline.

Installs and configures Etherpad-lite.

Creates an auto-start service, and a reverse proxy to the nodejs instance.
