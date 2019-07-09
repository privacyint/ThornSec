package profile;

import java.util.Vector;

import core.iface.IUnit;
import core.model.NetworkModel;
import core.model.ServerModel;
import core.profile.AStructuredProfile;
import core.unit.SimpleUnit;
import core.unit.fs.DirMountedUnit;
import core.unit.fs.DirUnit;
import core.unit.fs.FileAppendUnit;

public class media extends AStructuredProfile {

	public media(ServerModel me, NetworkModel networkModel) {
		super("media", me, networkModel);
	}

	public Vector<IUnit> getNetworking() {
		Vector<IUnit> units = new Vector<IUnit>();
		
		for (ServerModel router : networkModel.getRouterServers()) {
			//DNAT
			router.getFirewallModel().addNatPrerouting("dnat_media_ssh",
					"-d " + networkModel.getData().getExternalIp("media_lb")
					+ " -p tcp"
					+ " --dport 31337"
					+ " -j DNAT --to-destination " + me.getIP().getHostAddress() + ":" + networkModel.getData().getSSHPort(me.getLabel()),
					"Redirect external traffic on :31337 to our Media server");
			
			router.getFirewallModel().addFilter("media_ssh_ingress", "media_ingress",
					"-s 178.238.149.157"
					+ " -d " + networkModel.getServerModel(me.getLabel()).getIP().getHostAddress()
					+ " -p tcp"
					+ " --dport " + networkModel.getData().getSSHPort(me.getLabel())
					+ " -j ACCEPT",
					"Allow inbound SSH");
			router.getFirewallModel().addFilter("media_ssh_egress", "media_egress",
					"-d 178.238.149.157"
					+ " -s " + me.getIP().getHostAddress()
					+ " -p tcp"
					+ " --sport " + networkModel.getData().getSSHPort(me.getLabel())
					+ " -j ACCEPT",
					"");
		}
		
		return units;
	}
	
	protected Vector<IUnit> getPersistentConfig() {
		Vector<IUnit> units = new Vector<IUnit>();

		units.addElement(new SimpleUnit("candc_user", "proceed",
				"sudo useradd -m candc -c 'C&C Music Factory'",
				"id candc 2>&1", "id: ‘candc’: no such user", "fail",
				"The candc user couldn't be added."));
		
		units.addElement(new DirUnit("tc_dir_exists", "is_virtualbox_guest", "/media/data/transcodeDir"));
		
		//Mount /media/data/transcodeDir
		units.addElement(new FileAppendUnit("tc_fstab", "is_virtualbox_guest", "transcodeDir    /media/data/transcodeDir      vboxsf defaults,_netdev,uid=$(id candc -u),gid=$(id candc -g) 0 0", "/etc/fstab",
				"Couldn't create the mount for transcoding at /media/data/transcodeDir"));
		units.addElement(new DirUnit("tc_bindpoint", "is_virtualbox_guest", "/media/data/transcodeDir"));
		units.addElement(new DirMountedUnit("tc", "tc_fstab_appended", "transcodeDir",
				"Couldn't mount the transcoding directory."));
		
		return units;
	}

}
