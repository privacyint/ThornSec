/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.service.web;

import java.util.HashSet;
import java.util.Set;

import core.data.machine.AMachineData.Encapsulation;
import core.exception.data.InvalidPortException;
import core.exception.data.InvalidPropertyArrayException;
import core.exception.data.InvalidPropertyException;
import core.exception.data.machine.InvalidMachineException;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.InvalidMachineModelException;
import core.exception.runtime.InvalidServerModelException;
import core.iface.IUnit;
import core.model.machine.AMachineModel;
import core.model.network.NetworkModel;
import core.profile.AStructuredProfile;
import core.unit.fs.CustomFileUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;
import profile.stack.Nginx;

/**
 * This is an NGINX reverse-proxy (load balancer)
 */
public class Webproxy extends AStructuredProfile {

	private final Nginx webserver;
	private FileUnit liveConfig;

	public Webproxy(String label, NetworkModel networkModel) {
		super("webproxy", networkModel);

		this.webserver = new Nginx(getLabel(), networkModel);
		this.liveConfig = null;
	}

	@Override
	protected Set<IUnit> getInstalled() throws InvalidServerModelException {
		final Set<IUnit> units = new HashSet<>();

		units.add(new InstalledUnit("openssl", "proceed", "openssl"));

		units.addAll(this.webserver.getInstalled());

		return units;
	}

	@Override
	protected Set<IUnit> getPersistentConfig()
			throws InvalidPropertyException, InvalidServerException, InvalidServerModelException {
		final Set<IUnit> units = new HashSet<>();

		units.addAll(this.webserver.getPersistentConfig());

		// Should we pass through real IPs?
		final Boolean passThroughIps = Boolean
				.parseBoolean(this.networkModel.getData().getProperty(getLabel(), "passrealips", false));

		// First, build our ssl config
		units.add(new DirUnit("nginx_ssl_include_dir", "proceed", "/etc/nginx/includes"));
		final FileUnit sslConf = new FileUnit("nginx_ssl", "proceed", "/etc/nginx/includes/ssl_params");
		units.add(sslConf);
		sslConf.appendLine("    ssl_session_timeout 1d;");
		sslConf.appendLine("    ssl_session_cache shared:SSL:10m;");
		sslConf.appendLine("    ssl_session_tickets off;");
		sslConf.appendCarriageReturn();
		sslConf.appendLine("    ssl_protocols TLSv1.2 TLSv1.3;");
		sslConf.appendLine("    ssl_ciphers 'EECDH+CHACHA20:EECDH+AESGCM:EECDH+AES256';");
		sslConf.appendLine("    ssl_prefer_server_ciphers on;");
		sslConf.appendLine("    ssl_ecdh_curve auto;");
		sslConf.appendCarriageReturn();
		sslConf.appendLine("    add_header Strict-Transport-Security 'max-age=63072000; includeSubDomains; preload';");
		sslConf.appendCarriageReturn();
		sslConf.appendLine("    ssl_stapling on;");
		sslConf.appendLine("    ssl_stapling_verify on;");
		for (final IPAddress resolver : this.networkModel.getData().getUpstreamDNSServers()) {
			sslConf.appendLine("    resolver " + resolver.toFullString() + " valid=300s;");
		}
		sslConf.appendLine("    resolver_timeout 5s;");

		// Now build our headers file
		final FileUnit headersConf = new FileUnit("nginx_headers", "proceed", "/etc/nginx/includes/header_params");
		headersConf.appendLine("    add_header X-Frame-Options                   'SAMEORIGIN' always;");
		headersConf.appendLine("    add_header X-Content-Type-Options            'nosniff' always;");
		headersConf.appendLine("    add_header X-XSS-Protection                  '1; mode=block' always;");
		headersConf.appendLine("    add_header X-Download-Options                'noopen' always;");
		headersConf.appendLine("    add_header X-Permitted-Cross-Domain-Policies 'none' always;");
		headersConf.appendLine(
				"    add_header Content-Security-Policy           'upgrade-insecure-requests; block-all-mixed-content; reflected-xss block;' always;");
		headersConf.appendCarriageReturn();
		headersConf.appendLine("    proxy_set_header Host               \\$host;");
		headersConf.appendLine("    proxy_set_header X-Forwarded-Host   \\$host:\\$server_port;");
		headersConf.appendLine("    proxy_set_header X-Forwarded-Server \\$host;");
		headersConf.appendLine("    proxy_set_header X-Forwarded-Port   \\$server_port;");
		headersConf.appendLine("    proxy_set_header X-Forwarded-Proto  https;");
		if (passThroughIps) {
			headersConf.appendCarriageReturn();
			headersConf.appendLine(
					"\n#These headers pass real IP addresses to the backend - this may not be desired behaviour");
			headersConf.appendLine("    proxy_set_header X-Forwarded-For    \\$proxy_add_x_forwarded_for;");
			headersConf.appendLine("    proxy_set_header X-Real-IP          \\$remote_addr;");
		}

		// And our forced (301) SSL upgrade
		final FileUnit sslConfig = new FileUnit("nginx_ssl_config", "nginx_installed",
				Nginx.DEFAULT_CONFIG_FILE.toString());
		sslConfig.appendLine("server {");
		sslConfig.appendLine("    listen 80 default;");
		sslConfig.appendLine("    return 301 https://\\$host\\$request_uri;");
		sslConfig.appendLine("}");

		this.webserver.addLiveConfig(sslConfig);

		return units;
	}

	@Override
	protected Set<IUnit> getLiveConfig()
			throws InvalidMachineModelException, InvalidPropertyArrayException, InvalidMachineException {
		final Set<IUnit> units = new HashSet<>();

		if (this.liveConfig.equals("")) {
			final Set<String> backends = this.networkModel.getData().getPropertyArray(getLabel(), "proxy");
			Boolean isDefault = true;

			for (final String backend : backends) {
				final AMachineModel backendObj = this.networkModel.getMachineModel(backend);

				final Set<HostName> cnames = this.networkModel.getData().getCnames(backend);
				final HostName domain = this.networkModel.getServerModel(backend).getFQDN();
				final String logDir = "/var/log/nginx/" + backend + "." + domain + "/";

				units.add(new DirUnit(backend + "_log_dir", "proceed", logDir,
						"Could not create the directory for " + backend + "'s logs. Nginx will refuse to start."));
				units.addAll(this.networkModel.getServerModel(getLabel()).getBindFsModel().addBindPoint(
						backend + "_tls_certs", "proceed", "/media/metaldata/tls/" + backend,
						"/media/data/tls/" + backend, "root", "root", "600", "/media/metaldata", false));

				// Generated from
				// https://mozilla.github.io/server-side-tls/ssl-config-generator/
				// (Nginx/Modern) & https://cipherli.st/
				final FileUnit nginxConf = new FileUnit(backend + "_nginx_conf", "nginx_installed",
						Nginx.CONF_D_DIRECTORY + "/" + backend + ".conf");
				nginxConf.appendLine("server {");
				nginxConf.appendLine("    listen 443 ssl http2");
				if (isDefault) {
					nginxConf.appendLine(" default"); // We need this to be here, or it'll crap out :'(
					isDefault = false;
				}
				nginxConf.appendLine(";");

				nginxConf.appendLine("    server_name " + backend + "." + domain);

				for (final HostName cname : cnames) {
					nginxConf.appendLine((cname.equals("")) ? " " : " " + cname + ".");
					nginxConf.appendLine(domain.toNormalizedString());
				}

				nginxConf.appendLine(";");

				// Let's separate the logs out...
				nginxConf.appendLine("    access_log " + logDir + "access.log;"); // main buffer=16k;");
				nginxConf.appendLine("    error_log  " + logDir + "error.log;"); // main buffer=16k;");
				nginxConf.appendCarriageReturn();
				// We use the Let's Encrypt naming convention here, in case we want to install
				// it later
				nginxConf.appendLine("    include /etc/nginx/includes/ssl_params;");
				nginxConf.appendLine("    include /etc/nginx/includes/header_params;");
				nginxConf.appendLine("    ssl_certificate /media/data/tls/" + backend + "/fullchain.pem;");
				nginxConf.appendLine("    ssl_certificate_key /media/data/tls/" + backend + "/privkey.pem;");
				nginxConf.appendLine("    ssl_trusted_certificate /media/data/tls/" + backend + "/stapling.pem;");
				nginxConf.appendCarriageReturn();
				nginxConf.appendLine("    location / {");
				for (final String source : this.networkModel.getData().getPropertyArray(backend, "allow")) {
					nginxConf.appendLine("        allow " + source + ";");
					nginxConf.appendLine("        deny all;");
				}
				nginxConf.appendLine("        proxy_pass              http://" + backendObj.getIP() + ";");
				nginxConf.appendLine("        proxy_request_buffering off;");
				nginxConf.appendLine("        proxy_buffering         off;");
				nginxConf.appendLine("        client_max_body_size    0;");
				nginxConf.appendLine("        proxy_http_version      1.1;");
				nginxConf.appendLine("        proxy_connect_timeout   3000;");
				nginxConf.appendLine("        proxy_send_timeout      3000;");
				nginxConf.appendLine("        proxy_read_timeout      3000;");
				nginxConf.appendLine("        send_timeout            3000;");
				nginxConf.appendLine("    }");
				nginxConf.appendCarriageReturn();
				nginxConf.appendLine("    include /media/data/nginx_custom_blocks/" + backend + ".conf;");
				nginxConf.appendLine("}");

				this.webserver.addLiveConfig(nginxConf);

				units.add(new CustomFileUnit("nginx_custom_block_" + backend,
						"nginx_custom_blocks_data_bindpoint_created",
						"/media/data/nginx_custom_blocks/" + backend + ".conf"));
			}
		} else {
			this.webserver.addLiveConfig(this.liveConfig);
		}

		units.addAll(this.webserver.getLiveConfig());

		return units;
	}

	@Override
	public Set<IUnit> getPersistentFirewall() throws InvalidMachineModelException, InvalidPortException,
			InvalidPropertyArrayException, InvalidMachineException {
		final Set<IUnit> units = new HashSet<>();

		final Set<String> backends = this.networkModel.getData().getPropertyArray(getLabel(), "proxy");

		units.addAll(this.webserver.getPersistentFirewall());

		this.networkModel.getServerModel(getLabel()).addEgress("check.torproject.org");
		this.networkModel.getServerModel(getLabel()).addListen(Encapsulation.TCP, 443);

		for (final String backend : backends) {
			this.networkModel.getMachineModel(getLabel()).addForward(new HostName(backend + ":80"));
			this.networkModel.getMachineModel(getLabel()).addDnat(backend);
		}

		return units;
	}

	public void setLiveConfig(FileUnit config) {
		this.liveConfig = config;
	}

}
