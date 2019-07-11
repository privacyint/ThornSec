package profile.stack;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import core.data.machine.AMachineData.Encapsulation;

import core.iface.IUnit;

import core.model.network.NetworkModel;

import core.profile.AStructuredProfile;

import core.unit.SimpleUnit;
import core.unit.fs.CustomFileUnit;
import core.unit.fs.FileUnit;
import core.unit.pkg.InstalledUnit;
import core.unit.pkg.RunningUnit;

import core.exception.data.InvalidPortException;
import core.exception.data.machine.InvalidServerException;

import core.exception.runtime.InvalidServerModelException;

public class Nginx extends AStructuredProfile {
	public static final File DEFAULT_CONFIG_FILE = new File("/etc/nginx/conf.d/default.conf");
	
	public static final File CONF_D_DIRECTORY    = new File("/etc/nginx/conf.d/");
	private HashSet<FileUnit> liveConfigs;
	
	public Nginx(String label, NetworkModel networkModel) {
		super(label, networkModel);
		
		this.liveConfigs = null;
	}

	@Override
	public Set<IUnit> getInstalled()
	throws InvalidServerModelException {
		Set<IUnit> units = new HashSet<IUnit>();

		//If we don't give the nginx user a home dir, it can cause problems with npm etc
		units.add(new SimpleUnit("nginx_user", "proceed",
				"sudo useradd -r -d /media/data/www nginx",
				"id nginx 2>&1", "id: ‘nginx’: no such user", "fail",
				"The nginx user couldn't be added.  This will cause all sorts of errors."));

		networkModel.getServerModel(getLabel()).getUserModel().addUsername("nginx");
		
		units.addAll(networkModel.getServerModel(getLabel()).getBindFsModel().addLogBindPoint("nginx", "proceed", "nginx", "0600"));

		units.add(new InstalledUnit("nginx", "nginx_pgp", "nginx"));
		
		return units;
	}
	
	@Override
	public Set<IUnit> getPersistentConfig()
	throws InvalidServerException, InvalidServerModelException {
		Set<IUnit> units =  new HashSet<IUnit>();
		
		units.addAll(networkModel.getServerModel(getLabel()).getBindFsModel().addDataBindPoint("www", "proceed", "nginx", "nginx", "0750"));
		
		units.addAll(networkModel.getServerModel(getLabel()).getBindFsModel().addDataBindPoint("nginx_includes", "nginx_installed", "nginx", "nginx", "0750"));
		units.addAll(networkModel.getServerModel(getLabel()).getBindFsModel().addDataBindPoint("nginx_modules", "nginx_installed", "nginx", "nginx", "0750"));
		
		units.add(new SimpleUnit("nginx_modules_symlink", "nginx_modules_data_bindpoint_created",
				"sudo rm -r /etc/nginx/modules;"
				+ "sudo ln -s /media/data/nginx_modules/ /etc/nginx/modules",
				"readlink /etc/nginx/modules", "/media/data/nginx_modules/", "pass"));
		
		units.add(new CustomFileUnit("nginx_custom_nginx", "nginx_includes_data_bindpoint_created", "/media/data/nginx_includes/customNginxBlockParams"));
		units.add(new CustomFileUnit("nginx_custom_http", "nginx_includes_data_bindpoint_created", "/media/data/nginx_includes/customHttpBlockParams"));

		networkModel.getServerModel(getLabel()).getAptSourcesModel().addAptSource("nginx", "deb http://nginx.org/packages/mainline/debian/ stretch nginx", "keyserver.ubuntu.com", "ABF5BD827BD9BF62");
		
		FileUnit nginxConf = new FileUnit("nginx_conf", "nginx_installed", "/etc/nginx/nginx.conf");
		units.add(nginxConf);
		
		nginxConf.appendLine("user nginx;");
		nginxConf.appendCarriageReturn();
		nginxConf.appendLine("worker_processes " + networkModel.getData().getCpus(getLabel()));
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
	public Set<IUnit> getLiveConfig()
	throws InvalidServerModelException {
		Set<IUnit> units = new HashSet<IUnit>();
		
		networkModel.getServerModel(getLabel()).addProcessString("nginx: master process /usr/sbin/nginx -g daemon on; master_process on;$");
		networkModel.getServerModel(getLabel()).addProcessString("nginx: master process /usr/sbin/nginx -c /etc/nginx/nginx.conf$");
		networkModel.getServerModel(getLabel()).addProcessString("nginx: worker process *$");
		
		units.addAll(networkModel.getServerModel(getLabel()).getBindFsModel().addDataBindPoint("nginx_custom_conf_d", "nginx_installed", "nginx", "nginx", "0750"));
		
		if (! liveConfigs.isEmpty()) {		
			units.addAll(liveConfigs);
		}
		else {
			FileUnit defaultServerBlock = new FileUnit("nginx_default", "nginx_installed", Nginx.CONF_D_DIRECTORY + "default.conf");
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
		liveConfigs.add(config);
	}

	@Override
	public Set<IUnit> getPersistentFirewall()
	throws InvalidServerModelException, InvalidPortException {
		Set<IUnit> units = new HashSet<IUnit>();
		
		networkModel.getServerModel(getLabel()).addListen(Encapsulation.TCP, 80);
		//Allow the server to call out to nginx.org to download mainline
		networkModel.getServerModel(getLabel()).addEgress("nginx.org");
		
		return units;
	}
	
	@Override
	public Set<IUnit> getLiveFirewall() {
		return new HashSet<IUnit>(); //Empty (for now?)
	}
}
