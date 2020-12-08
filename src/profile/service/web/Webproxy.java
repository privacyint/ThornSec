/*
 * This code is part of the ThornSec project.
 *
 * To learn more, please head to its GitHub repo: @privacyint
 *
 * Pull requests encouraged.
 */
package profile.service.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import core.data.machine.ServerData;
import core.exception.data.InvalidPortException;
import core.exception.data.InvalidPropertyArrayException;
import core.exception.data.InvalidPropertyException;
import core.exception.data.MissingPropertiesException;
import core.exception.data.machine.InvalidMachineException;
import core.exception.data.machine.InvalidServerException;
import core.exception.runtime.InvalidMachineModelException;
import core.iface.IUnit;
import core.model.machine.AMachineModel;
import core.model.machine.ServerModel;
import core.profile.AStructuredProfile;
import core.unit.fs.DirUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import inet.ipaddr.HostName;
import profile.stack.Nginx;

/**
 * This is an NGINX reverse-proxy (load balancer)
 */
public class Webproxy extends AStructuredProfile {

	private final Nginx webserver;
	private FileUnit liveConfig;
	private Set<String> backends;

	public Webproxy(ServerModel me) throws MissingPropertiesException {
		super(me);

		this.webserver = new Nginx(me);
		this.liveConfig = null;

		final ServerData data = getServerModel().getData();

		if (data.getData().containsKey("webproxy")) {
			final JsonObject proxyData = data.getData().getJsonObject("webproxy");
			final JsonArray backends = proxyData.getJsonArray("backends");

			for (final JsonValue backend : backends) {
				putBackend(((JsonString) backend).getString());
			}
		} else {
			throw new MissingPropertiesException("backends");
		}
	}

	@Override
	public Collection<IUnit> getInstalled() throws InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.add(new InstalledUnit("openssl", "proceed", "openssl"));

		units.addAll(this.webserver.getInstalled());

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentConfig()
			throws InvalidPropertyException, InvalidServerException, InvalidMachineModelException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.webserver.getPersistentConfig());

		// Should we pass through real IPs?
		final Boolean passThroughIps = getServerModel().getData().getData().getBoolean("passrealips"); // Defaults false
		// First, build our ssl config
		units.add(new DirUnit("nginx_ssl_include_dir", "proceed", "/etc/nginx/includes"));
		final FileUnit sslConf = new FileUnit("nginx_ssl", "proceed", "/etc/nginx/includes/ssl_params");
		units.add(sslConf);
		sslConf.appendLine("\tssl_session_timeout 5m;");
		sslConf.appendLine("\tssl_session_cache shared:SSL:10m;");
		sslConf.appendLine("\tssl_session_tickets off;");
		sslConf.appendCarriageReturn();
		sslConf.appendLine("\tssl_protocols TLSv1.2 TLSv1.3;");
		sslConf.appendLine(
				"\tssl_ciphers 'TLS13-CHACHA20-POLY1305-SHA256:TLS13-AES-256-GCM-SHA384:EECDH+CHACHA20:EECDH+AESGCM';");
		sslConf.appendLine("\tssl_prefer_server_ciphers on;");
		sslConf.appendLine("\tssl_ecdh_curve X25519:secp521r1:secp384r1;");
		sslConf.appendCarriageReturn();
		sslConf.appendLine("\tadd_header Strict-Transport-Security 'max-age=63072000; includeSubDomains; preload';");
		sslConf.appendCarriageReturn();
		sslConf.appendLine("\tssl_stapling on;");
		sslConf.appendLine("\tssl_stapling_verify on;");

		sslConf.appendText("\tresolver ");
		getNetworkModel().getData()
						 .getUpstreamDNSServers()
						 .ifPresent(upstreams ->
						 	upstreams.forEach(resolver -> {
						 		sslConf.appendText(resolver.asInetAddress() + " ");
						 	}
						 ));
		sslConf.appendLine("valid=300s;");

		sslConf.appendLine("\tresolver_timeout 5s;");

		// Now build our headers file
		final FileUnit headersConf = new FileUnit("nginx_headers", "proceed", "/etc/nginx/includes/header_params");
		headersConf.appendLine("\tadd_header X-Frame-Options                   'SAMEORIGIN' always;");
		headersConf.appendLine("\tadd_header X-Content-Type-Options            'nosniff' always;");
		headersConf.appendLine("\tadd_header X-XSS-Protection                  '1; mode=block' always;");
		headersConf.appendLine("\tadd_header X-Download-Options                'noopen' always;");
		headersConf.appendLine("\tadd_header X-Permitted-Cross-Domain-Policies 'none' always;");
		headersConf.appendLine(
				"    add_header Content-Security-Policy           'upgrade-insecure-requests; block-all-mixed-content; reflected-xss block;' always;");
		headersConf.appendCarriageReturn();
		headersConf.appendLine("\tproxy_set_header Host               \\$host;");
		headersConf.appendLine("\tproxy_set_header X-Forwarded-Host   \\$host:\\$server_port;");
		headersConf.appendLine("\tproxy_set_header X-Forwarded-Server \\$host;");
		headersConf.appendLine("\tproxy_set_header X-Forwarded-Port   \\$server_port;");
		headersConf.appendLine("\tproxy_set_header X-Forwarded-Proto  https;");
		if (passThroughIps) {
			headersConf.appendCarriageReturn();
			headersConf.appendLine(
					"\n#These headers pass real IP addresses to the backend - this may not be desired behaviour");
			headersConf.appendLine("\tproxy_set_header X-Forwarded-For    \\$proxy_add_x_forwarded_for;");
			headersConf.appendLine("\tproxy_set_header X-Real-IP          \\$remote_addr;");
		}
		headersConf.appendCarriageReturn();
		// Just in case, hide any extraneous "powered by" headers
		headersConf.appendLine("\tproxy_hide_header X-Powered-By;");

		// And our forced (301) SSL upgrade
		final FileUnit sslConfig = new FileUnit("nginx_ssl_config", "nginx_installed",
				Nginx.DEFAULT_CONFIG_FILE.toString());
		sslConfig.appendLine("server {");
		sslConfig.appendLine("\tlisten 80 default;");
		sslConfig.appendLine("\treturn 301 https://\\$host\\$request_uri;");
		sslConfig.appendLine("}");

		this.webserver.addLiveConfig(sslConfig);

		return units;
	}

	@Override
	public Collection<IUnit> getLiveConfig() throws InvalidMachineModelException, InvalidPropertyArrayException,
			InvalidMachineException, MissingPropertiesException {
		final Collection<IUnit> units = new ArrayList<>();

		if (this.liveConfig == null) {
			Boolean isDefault = true;

			for (String backendLabel : getBackends()) {
				final AMachineModel backendObj = getNetworkModel().getMachineModel(backendLabel);

				final HostName domain = backendObj.getDomain();
				final String logDir = "/var/log/nginx/" + backendLabel + "." + domain + "/";

				// Generated from
				// https://mozilla.github.io/server-side-tls/ssl-config-generator/
				// (Nginx/Modern) & https://cipherli.st/
				final FileUnit nginxConf = new FileUnit(backendLabel + "_nginx_conf", "nginx_installed",
						Nginx.CONF_D_DIRECTORY + "/" + backendLabel + ".conf");
				nginxConf.appendLine("server {");
				nginxConf.appendText("\tlisten 443 ssl http2");
				if (isDefault) {
					nginxConf.appendText(" default"); // We need this to be here, or it'll crap out :'(
					isDefault = false;
				}
				nginxConf.appendLine(";");

				nginxConf.appendCarriageReturn();

				nginxConf.appendLine("\tserver_name " + backendLabel + "." + domain + ";");

				backendObj.getCNAMEs()
						  .ifPresent((cnames) -> {
							  cnames.forEach((cname) -> {
									nginxConf.appendText("\tserver_name ");
									nginxConf.appendText(
											(cname.equals("") || (cname.equals(".")))
											? ""
											: cname + "."
									);
									nginxConf.appendLine(domain.toNormalizedString() + ";");
						   });
				});

				nginxConf.appendCarriageReturn();

				// Let's separate the logs out...
				nginxConf.appendLine("\taccess_log " + logDir + "access.log;"); // main buffer=16k;");
				nginxConf.appendLine("\terror_log  " + logDir + "error.log;"); // main buffer=16k;");
				nginxConf.appendCarriageReturn();
				// We use the Let's Encrypt naming convention here, in case we want to install
				// it later
				nginxConf.appendLine("\tinclude /etc/nginx/includes/ssl_params;");
				nginxConf.appendLine("\tinclude /etc/nginx/includes/header_params;");
				nginxConf.appendLine("\tssl_certificate /media/data/tls/" + backendLabel + "/fullchain.pem;");
				nginxConf.appendLine("\tssl_certificate_key /media/data/tls/" + backendLabel + "/privkey.pem;");
				nginxConf.appendLine("\tssl_trusted_certificate /media/data/tls/" + backendLabel + "/stapling.pem;");
				nginxConf.appendCarriageReturn();
				nginxConf.appendLine("\tlocation / {");

				nginxConf.appendLine("\t\tproxy_pass              http://" + backendLabel + "/;");
				nginxConf.appendLine("\t\tproxy_request_buffering off;");
				nginxConf.appendLine("\t\tproxy_buffering         off;");
				nginxConf.appendLine("\t\tclient_max_body_size    0;");
				nginxConf.appendLine("\t\tproxy_http_version      1.1;");
				nginxConf.appendLine("\t\tproxy_connect_timeout   3000;");
				nginxConf.appendLine("\t\tproxy_send_timeout      3000;");
				nginxConf.appendLine("\t\tproxy_read_timeout      3000;");
				nginxConf.appendLine("\t\tsend_timeout            3000;");
				nginxConf.appendLine("\t}");
				nginxConf.appendCarriageReturn();
				nginxConf.appendLine("\tinclude /media/data/nginx_custom_blocks/" + backendLabel + ".conf;");
				nginxConf.appendLine("}");

				this.webserver.addLiveConfig(nginxConf);
			}
		} else {
			this.webserver.addLiveConfig(this.liveConfig);
		}

		units.addAll(this.webserver.getLiveConfig());

		return units;
	}

	@Override
	public Collection<IUnit> getPersistentFirewall() throws InvalidMachineModelException, InvalidPortException,
			InvalidPropertyArrayException, InvalidMachineException {
		final Collection<IUnit> units = new ArrayList<>();

		units.addAll(this.webserver.getPersistentFirewall());

		getMachineModel().addListen(443);

		for (final String backend : getBackends()) {
			getMachineModel().addDNAT(getNetworkModel().getMachineModel(backend), 80, 443);
		}

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

	public void setLiveConfig(FileUnit config) {
		this.liveConfig = config;
	}

}
