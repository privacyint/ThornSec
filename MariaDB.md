# MariaDB.java
This is our class or creating and configuring a MariaDB instance on a given service.

It's not really designed to be called directly from our config (although it can be, as it's a profile) but is designed to be called as an object from any profile which requires a database back-end.

We use MariaDB over MySQL as MariaDB is better.  Suck it up.

## getInstalled
Creates the mysql user, installs OpenSSL, and downloads MariaDB from the mainline repo.  The one in Debian's repo is too old for many applications.

It then moves the MariaDB data directory to our data directory, and bind-mounts its log directory with the correct permissions for writing.

If MariaDB isn't already installed, it'll create a (random) 32-char hex string for its root password, which is never required to be known outside of the bash variable in which it's generated.b

## createDb(String db, String username, String privileges, Sting passwordVariable)
This, if I do say so myself, is a great way of subverting the mysql model of requiring logins through the power of having r00t on the host machine.  It only happens on *configuration* , and not on auditing.

1. It kills the MariaDB service, and starts it with no user (See https://dev.mysql.com/doc/refman/5.7/en/server-options.html#option_mysqld_skip-grant-tables).
2. It flushes privileges, meaning it's now a standard MariaDB instantiation.
3. It creates its user and database (if the audit failed)
4. It kills the service and restarts as normal.

This will only happen if its audit fails, as with all config tests.  Its audit should pass, if the machine has already been configured.

The audit looks like the following:

`
[[ -z ${" + passwordVariable + "} ]] && echo 0 || mysql -u" + username + " -p${" + passwordVariable + "} -B -N -e \"SELECT EXISTS(SELECT 1 FROM mysql.user WHERE user = '" + username + "')\" 2>&1 | awk '{if ($2 == \"1142\") { print \"1\" } else { print $1 }}'"
`

This works as follows:

- Iff passwordVariable is set 
	- If trying to select your user exists in the user table returns "1142"
		- The user exists (assume the db has been created at the same time) but it doesn't have permissions to query the mysql user table.  Return '1'.
	- OR There are no privileges in the DB, tring to select your user in the mysql user table will return either '1' or '0' depending on if it exists.
- If you've got '1' at this point, your user exists.  '0' means it doesn't.
