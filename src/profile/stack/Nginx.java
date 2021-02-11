/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.stack;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import core.data.machine.configuration.TrafficRule.Encapsulation;
import core.exception.data.InvalidPortException;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.InvalidMachineModelException;
import core.iface.IUnit;
import core.model.machine.ServerModel;
import core.profile.AStructuredProfile;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;
import inet.ipaddr.HostName;

public class Nginx extends AStructuredProfile {
	public static final File DEFAULT_CONFIG_FILE = new File("/etc/nginx/conf.d/default.conf");

	public static final File CONF_D_DIRECTORY = new File("/etc/nginx/conf.d/");
	private Collection<FileUnit> liveConfigs;

	public Nginx(ServerModel me) {
		super(me);

		this.liveConfigs = null;
	}

	@Override
	public Collection<IUnit> getInstalled() throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("nginx", "nginx_pgp", "nginx"));

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentConfig() throws InvalidServerException, InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		final FileUnit nginxConf = new FileUnit("nginx_conf", "nginx_installed", "/etc/nginx/nginx.conf");
		units.add(nginxConf);

		nginxConf.appendLine("user nginx;");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("worker_processes " + ((ServerModel) getMachineModel()).getCPUs() + ";");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("error_log  /var/log/nginx/error.log warn;");
		nginxConf.appendLine("pid        /var/run/nginx.pid;");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("include /media/data/nginx_includes/customNginxBlockParams;");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("events {");
		nginxConf.appendLine("    worker_connections $(ulimit -n);");
		nginxConf.appendLine("    multi_accept       on;");
		nginxConf.appendLine("}");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("http {");
		nginxConf.appendLine("    include       /etc/nginx/mime.types;");
		nginxConf.appendLine("    default_type  application/octet-stream;");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("    log_format  main  '\\$remote_addr - \\$remote_user [\\$time_local] \"\\$request\" '");
		nginxConf.appendLine("                      '\\$status \\$body_bytes_sent \"\\$http_referer\" '");
		nginxConf.appendLine("                      '\"\\$http_user_agent\" \"\\$http_x_forwarded_for\"';");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("    access_log  /var/log/nginx/access.log main buffer=16k;");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("    sendfile on;");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("    keepalive_timeout 65;");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("    server_tokens off;");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("    client_max_body_size 0;");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("    include /media/data/nginx_includes/customHttpBlockParams;");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("    include /etc/nginx/conf.d/*.conf;");
		nginxConf.appendLine("}");

		return units;
	}

	@Override
	public Collection<IUnit> getLiveConfig() throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		getServerModel().addProcessString("nginx: master process /usr/sbin/nginx -g daemon on; master_process on;$");
		getServerModel().addProcessString("nginx: master process /usr/sbin/nginx -c /etc/nginx/nginx.conf$");
		getServerModel().addProcessString("nginx: worker process *$");

		if ((getLiveConfigs() != null) && !getLiveConfigs().isEmpty()) {
			units.addAll(this.liveConfigs);
		} else {
			final FileUnit defaultServerBlock = new FileUnit("nginx_default", "nginx_installed",
					Nginx.CONF_D_DIRECTORY + "/default.conf");
			units.add(defaultServerBlock);

			defaultServerBlock.appendLine("server {");
			defaultServerBlock.appendLine("    listen 80;");
			defaultServerBlock.appendLine("    server_name _;");
			defaultServerBlock.appendCarriageReturn();
			defaultServerBlock.appendLine("    location / {");
			defaultServerBlock.appendLine("        root /media/data/www;");
			defaultServerBlock.appendLine("        index index.html index.htm;");
			defaultServerBlock.appendLine("    }");
			defaultServerBlock.appendCarriageReturn();
			defaultServerBlock.appendLine("    error_page 500 502 503 504 /50x.html;");
			defaultServerBlock.appendLine("    location = /50x.html {");
			defaultServerBlock.appendLine("        root /usr/share/nginx/html;");
			defaultServerBlock.appendLine("    }");
			defaultServerBlock.appendCarriageReturn();
			defaultServerBlock.appendLine("    include /media/data/nginx_custom_conf_d/default.conf;");
			defaultServerBlock.appendLine("}");
		}

		units.add(new RunningUnit("nginx", "nginx", "nginx"));

		return units;
	}

	public final void addLiveConfig(FileUnit config) {
		if (this.liveConfigs == null) {
			this.liveConfigs = new LinkedHashSet<>();
		}

		this.liveConfigs.add(config);
	}

	private final Collection<FileUnit> getLiveConfigs() {
		if (this.liveConfigs == null) {
			this.liveConfigs = new LinkedHashSet<>();
		}

		return this.liveConfigs;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws InvalidMachineModelException, InvalidPortException {
		final Collection<IUnit> units = new ArrayList<>();

		getServerModel().addListen(Encapsulation.TCP, 80);
		// Allow the server to call out to nginx.org to download mainline
		getServerModel().addEgress(new HostName("nginx.org:80"));
		getServerModel().addEgress(new HostName("nginx.org:443"));

		return units;
	}

	@Override
	public Collection<IUnit> getLiveFirewall() {
		return new ArrayList<>(); // Empty (for now?)
	}
}
