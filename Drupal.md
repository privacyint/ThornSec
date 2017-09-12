#Drupal.java

This will instantiate/audit a Drupal instance.  Nginx.java, PHP.java, and MariaDB.java to do most of its functionality.

##getPersistentConfig
Adds all units from Nginx/PHP/MariaDB

##getInstalled
Adds all units from Nginx/PHP/MariaDB

Adds Drupal-specific PHP extensions and installs drush.

##getPersistentFirewall
Adds all units from Nginx/PHP/MariaDB

##getLiveConfig
Adds all units from Nginx/PHP/MariaDB

Creates/audits the Drupal-specific Nginx config file, Creates the required databases if needed, and installs Drupal.
