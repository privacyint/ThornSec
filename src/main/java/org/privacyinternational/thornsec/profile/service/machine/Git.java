/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package org.privacyinternational.thornsec.profile.service.machine;

import java.util.ArrayList;
import java.util.Collection;

import org.privacyinternational.thornsec.core.exception.data.InvalidPortException;
import org.privacyinternational.thornsec.core.exception.data.machine.InvalidServerException;
import org.privacyinternational.thornsec.core.exception.runtime.InvalidMachineModelException;
import org.privacyinternational.thornsec.core.iface.IUnit;
import org.privacyinternational.thornsec.core.model.machine.ServerModel;
import org.privacyinternational.thornsec.core.profile.AStructuredProfile;
import org.privacyinternational.thornsec.core.unit.SimpleUnit;
import org.privacyinternational.thornsec.core.unit.fs.FileEditUnit;
import org.privacyinternational.thornsec.core.unit.fs.FileUnit;
import org.privacyinternational.thornsec.core.unit.pkg.InstalledUnit;
import org.privacyinternational.thornsec.core.unit.pkg.RunningUnit;
import inet.ipaddr.HostName;
import org.privacyinternational.thornsec.profile.stack.Nginx;

/**
 * This profile is supposed to create and configure a Git server
 */
public class Git extends AStructuredProfile {

	private final Nginx webserver;

	public Git(ServerModel me) {
		super(me);

		this.webserver = new Nginx(me);
	}

	@Override
	public Collection<IUnit> getInstalled() throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("java", "proceed", "default-jre-headless"));

		units.add(new InstalledUnit("scm_server", "scm_manager_pgp", "scm-server"));

		units.addAll(this.webserver.getInstalled());

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentConfig() throws InvalidServerException, InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new FileEditUnit("scm_server_home", "scm_data_mounted", "export SCM_HOME=/var/lib/scm",
				"export SCM_HOME=/media/data/scm", "/etc/default/scm-server",
				"Couldn't change scm-manager's data directory.  Its data will be stored in the VM only."));

		units.add(new RunningUnit("scm_server", "scm-server", "scm-server"));

		final FileUnit nginxConf = new FileUnit("scm_manager_default_nginx", "scm_server_installed",
				Nginx.CONF_D_DIRECTORY + "default.conf");

		nginxConf.appendLine("server {");
		nginxConf.appendLine("    listen 80;");
		nginxConf.appendLine("    server_name _;");
		nginxConf.appendLine("");
		nginxConf.appendLine("    location / {");
		nginxConf.appendLine("        proxy_pass          http://localhost:8080/scm/;");
		nginxConf.appendLine("        proxy_set_header    Host \\$host;");
		nginxConf.appendLine("        proxy_set_header    X-Real-IP \\$remote_addr;");
		nginxConf.appendLine(
				"        proxy_next_upstream error timeout invalid_header http_500 http_502 http_503 http_504 http_404;");
		nginxConf.appendLine("        proxy_redirect      off;");
		nginxConf.appendLine("        proxy_cache_valid   200 120m;");
		nginxConf.appendLine("        proxy_buffering     on;");
		nginxConf.appendLine("        proxy_set_header    Accept-Encoding \"\";");
		nginxConf.appendLine("    }");
		nginxConf.appendLine("}");

		this.webserver.addLiveConfig(nginxConf);

		units.addAll(this.webserver.getPersistentConfig());

		final FileUnit scmService = new FileUnit("scm_service", "scm_server_installed",
				"/etc/systemd/system/scm.service");
		units.add(scmService);

		scmService.appendLine("[Unit]");
		scmService.appendLine("Description=scm-manager");
		scmService.appendLine("After=network.target auditd.service");
		scmService.appendLine("");
		scmService.appendLine("[Service]");
		scmService.appendLine("ExecStart=/etc/init.d/scm-server start");
		scmService.appendLine("ExecStop=/etc/init.d/scm-server stop");
		scmService.appendLine("Type=forking");
		scmService.appendLine("Restart=always");
		scmService.appendLine("");
		scmService.appendLine("[Install]");
		scmService.appendLine("WantedBy=default.target");

		units.add(new SimpleUnit("scm_service_enabled", "scm_service", "sudo systemctl enable scm.service",
				"systemctl status scm.service 2>&1", "Unit scm.service could not be found.", "fail"));

		return units;
	}

	@Override
	public Collection<IUnit> getLiveConfig() throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.webserver.getLiveConfig());

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws InvalidPortException, InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		//getNetworkModel().getServerModel(getLabel()).getAptSourcesModel().addAptSource("scm_manager",
		//		"deb http://maven.scm-manager.org/nexus/content/repositories/releases ./", "keyserver.ubuntu.com",
		//		"D742B261");
		getMachineModel().addEgress(new HostName("maven.scm-manager.org"));

		units.addAll(this.webserver.getPersistentFirewall());

		return units;
	}

}
