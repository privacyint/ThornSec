/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.stack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import core.exception.data.InvalidPropertyArrayException;
import core.exception.data.machine.InvalidMachineException;
import core.exception.runtime.InvalidMachineModelException;
import core.iface.IUnit;
import core.model.machine.ServerModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.DirOwnUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;

public class OnionBalance extends AStructuredProfile {

	private Set<String> backends;

	public OnionBalance(ServerModel me) {
		super(me);
	}

	@Override
	public Collection<IUnit> getInstalled() throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("tor_keyring", "tor_pgp", "deb.torproject.org-keyring"));
		units.add(new InstalledUnit("tor", "tor_keyring_installed", "tor"));

		units.add(new InstalledUnit("onionbalance", "tor_installed", "onionbalance"));

		getServerModel().getUserModel().addUsername("debian-tor");
		getServerModel().getUserModel().addUsername("onionbalance");

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentConfig() throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(getNetworkModel().getServerModel(getLabel()).getBindFsModel().addDataBindPoint("onionbalance",
				"onionbalance_installed", "onionbalance", "onionbalance", "0700"));
		units.addAll(getNetworkModel().getServerModel(getLabel()).getBindFsModel().addLogBindPoint("onionbalance",
				"onionbalance_installed", "onionbalance", "0750"));
		units.addAll(getNetworkModel().getServerModel(getLabel()).getBindFsModel().addDataBindPoint("tor",
				"tor_installed", "debian-tor", "debian-tor", "0700"));
		units.addAll(getNetworkModel().getServerModel(getLabel()).getBindFsModel().addLogBindPoint("tor",
				"tor_installed", "debian-tor", "0750"));

		units.add(new DirUnit("onionbalance_var_run", "onionbalance_installed", "/var/run/onionbalance"));
		units.add(new DirOwnUnit("onionbalance_var_run", "onionbalance_var_run_created", "/var/run/onionbalance",
				"onionbalance"));

		final FileUnit service = new FileUnit("onionbalance_service", "onionbalance_installed",
				"/lib/systemd/system/onionbalance.service");
		units.add(service);

		service.appendLine("[Unit]");
		service.appendLine("Description=OnionBalance - Tor Onion Service load balancer");
		service.appendLine("Documentation=man:onionbalance");
		service.appendLine("Documentation=file:///usr/share/doc/onionbalance/html/index.html");
		service.appendLine("Documentation=https://github.com/DonnchaC/onionbalance");
		service.appendLine("After=network.target, tor.service");
		service.appendLine("Wants=network-online.target");
		service.appendLine("ConditionPathExists=/etc/onionbalance/config.yaml");
		service.appendCarriageReturn();
		service.appendLine("[Service]");
		service.appendLine("Type=simple");
		service.appendLine("PIDFile=/run/onionbalance.pid");
		service.appendLine("Environment=\"ONIONBALANCE_LOG_LOCATION=/var/log/onionbalance/log\"");
		service.appendLine("ExecStartPre=/bin/chmod o+r /var/run/tor/control.authcookie");
		// service.appendLine("ExecStartPre=/bin/chmod o+r /var/run/tor/control");
		service.appendLine("ExecStartPre=/bin/mkdir -p /var/run/onionbalance");
		service.appendLine("ExecStartPre=/bin/chown -R onionbalance:onionbalance /var/run/onionbalance");
		service.appendLine("ExecStart=/usr/sbin/onionbalance -c /etc/onionbalance/config.yaml");
		service.appendLine("ExecReload=/usr/sbin/onionbalance reload");
		service.appendLine(
				"ExecStop=-/sbin/start-stop-daemon --quiet --stop --retry=TERM/5/KILL/5 --pidfile /run/onionbalance.pid");
		service.appendLine("TimeoutStopSec=5");
		service.appendLine("KillMode=mixed");
		service.appendCarriageReturn();
		service.appendLine("EnvironmentFile=-/etc/default/%p");
		service.appendLine("User=onionbalance");
		service.appendLine("PermissionsStartOnly=true");
		service.appendLine("Restart=always");
		service.appendLine("RestartSec=10s");
		service.appendLine("LimitNOFILE=65536");
		service.appendCarriageReturn();
		service.appendLine("NoNewPrivileges=yes");
		service.appendLine("PrivateDevices=yes");
		service.appendLine("PrivateTmp=yes");
		service.appendLine("ProtectHome=yes");
		service.appendLine("ProtectSystem=full");
		service.appendLine("ReadOnlyDirectories=/");
		service.appendLine("ReadWriteDirectories=-/proc");
		service.appendLine("ReadWriteDirectories=-/var/log/onionbalance");
		service.appendLine("ReadWriteDirectories=-/var/run");
		service.appendCarriageReturn();
		service.appendLine("[Install]");
		service.appendLine("WantedBy=multi-user.target");

		final FileUnit torrc = new FileUnit("torrc", "tor_installed", "/etc/tor/torrc");
		units.add(torrc);

		torrc.appendLine("Datadirectory /var/lib/tor");
		torrc.appendLine("ControlPort 9051");
		torrc.appendLine("CookieAuthentication 1");
		torrc.appendLine("SocksPort 0");
		torrc.appendCarriageReturn();
		torrc.appendLine("RunAsDaemon 1");
		torrc.appendCarriageReturn();
		torrc.appendLine("FascistFirewall 1");

		units.add(new SimpleUnit("tor_service_enabled", "torrc", "sudo systemctl enable tor",
				"sudo systemctl is-enabled tor", "enabled", "pass",
				"Couldn't set tor to auto-start on boot.  You will need to manually start the service (\"sudo service tor start\") on reboot."));

		units.add(new SimpleUnit("onionbalance_service_enabled", "onionbalance_service_config",
				"sudo systemctl enable onionbalance", "sudo systemctl is-enabled onionbalance", "enabled", "pass",
				"Couldn't set onionbalance to auto-start on boot.  You will need to manually start the service (\"sudo service onionbalance start\") on reboot."));

		return units;
	}

	@Override
	public Collection<IUnit> getLiveConfig()
			throws InvalidPropertyArrayException, InvalidMachineException, InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		final FileUnit onionbalanceConfig = new FileUnit("onionbalance", "onionbalance_installed",
				"/etc/onionbalance/config.yaml");
		units.add(onionbalanceConfig);

		onionbalanceConfig.appendLine("REFRESH_INTERVAL: 600");
		onionbalanceConfig.appendLine("services:");
		onionbalanceConfig.appendLine("    - key: /media/data/onionbalance/private_key");
		onionbalanceConfig.appendLine("      instances:");

		final Set<String> backends = getBackends();
		for (final String backend : backends) {
			onionbalanceConfig.appendLine("        - address: " + backend);
		}

		units.add(new RunningUnit("tor", "tor", "/usr/bin/tor"));
		getServerModel().addProcessString(
				"/usr/bin/tor --defaults-torrc /usr/share/tor/tor-service-defaults-torrc -f /etc/tor/torrc --RunAsDaemon 0$");

		return units;
	}

	public void putBackend(String... backends) {
		if (this.backends == null) {
			this.backends = new LinkedHashSet<>();
		}

		for (final String backend : backends) {
			this.backends.add(backend);
		}
	}

	private Set<String> getBackends() {
		return this.backends;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		getNetworkModel().getServerModel(getLabel()).getAptSourcesModel().addAptSource("tor",
				"deb http://deb.torproject.org/torproject.org buster main", "keys.gnupg.net",
				"A3C4F0F979CAA22CDBA8F512EE8CBC9E886DDD89");
		getNetworkModel().getServerModel(getLabel()).addEgress("*"); // Needs to be able to call out to everywhere

		return units;
	}

}
